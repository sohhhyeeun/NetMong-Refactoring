package com.ll.netmong.domain.post.controller;

import com.ll.netmong.domain.member.entity.Member;
import com.ll.netmong.domain.member.repository.MemberRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

@SpringBootTest
@AutoConfigureMockMvc
class PostControllerConcurrencyTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String tagName;
    private List<Member> members;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        tagName = "동시성HTTP" + suffix;

        members = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            members.add(memberRepository.save(Member.builder().username("username" + suffix + i).email("user" + suffix + i + "@naver.com").build()));
        }
    }

    @AfterEach
    void tearDown() {
        for (Member member : members) {
            List<Long> imageIds = jdbcTemplate.queryForList("SELECT image_id FROM post WHERE member_id = ? AND image_id IS NOT NULL", Long.class, member.getId());
            jdbcTemplate.update("DELETE FROM post_hashtag WHERE post_id IN (SELECT id FROM post WHERE member_id = ?)", member.getId());
            jdbcTemplate.update("DELETE FROM post WHERE member_id = ?", member.getId());
            for (Long imageId : imageIds) {
                jdbcTemplate.update("DELETE FROM image WHERE id = ?", imageId);
            }
        }
        jdbcTemplate.update("DELETE FROM hashtag WHERE name = ?", tagName);
        for (Member member : members) {
            jdbcTemplate.update("DELETE FROM member WHERE id = ?", member.getId());
        }
    }

    @Test
    @DisplayName("여러 스레드에서 동시에 같은 새 해시태그로 게시물을 업로드해도 해시태그는 1개만 저장되고 모든 게시물에 연결된다.")
    void testPostUploadConcurrently() throws InterruptedException {
        //given
        int threadCount = members.size();
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger errors = new AtomicInteger();
        List<Integer> statuses = new CopyOnWriteArrayList<>();

        //when
        for (Member member : members) {
            executorService.submit(() -> {
                try {
                    MockMultipartFile image = new MockMultipartFile("image", "test.txt", "text/plain", "test file".getBytes(StandardCharsets.UTF_8));
                    startLatch.await();
                    int status = mockMvc.perform(multipart("/api/v1/post/upload")
                                    .file(image)
                                    .param("title", "테스트")
                                    .param("content", "#" + tagName)
                                    .with(user(member.getEmail())))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                    statuses.add(status);
                } catch (Exception e) {
                    errors.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        boolean completed = doneLatch.await(60, TimeUnit.SECONDS);
        executorService.shutdown();

        //then
        Integer hashtagRows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hashtag WHERE name = ?", Integer.class, tagName);
        List<Long> postIds = new ArrayList<>();
        for (Member member : members) {
            postIds.addAll(jdbcTemplate.queryForList("SELECT id FROM post WHERE member_id = ?", Long.class, member.getId()));
        }
        List<Integer> postHashtagRows = new ArrayList<>();
        for (Long postId : postIds) {
            postHashtagRows.add(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM post_hashtag ph JOIN hashtag h ON ph.hashtag_id = h.id WHERE ph.post_id = ? AND h.name = ?",
                    Integer.class, postId, tagName));
        }

        Assertions.assertThat(completed).isTrue();
        Assertions.assertThat(errors.get()).isEqualTo(0);
        Assertions.assertThat(statuses).hasSize(threadCount).containsOnly(201);
        Assertions.assertThat(postIds).hasSize(threadCount);
        Assertions.assertThat(hashtagRows).isEqualTo(1);
        Assertions.assertThat(postHashtagRows).containsOnly(1);
    }
}
