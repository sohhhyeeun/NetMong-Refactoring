package com.ll.netmong.domain.hashtag.service;

import com.ll.netmong.domain.hashtag.entity.Hashtag;
import com.ll.netmong.domain.member.entity.Member;
import com.ll.netmong.domain.member.repository.MemberRepository;
import com.ll.netmong.domain.post.entity.Post;
import com.ll.netmong.domain.post.repository.PostRepository;
import com.ll.netmong.domain.postHashtag.service.PostHashtagService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
class HashtagServiceImplConcurrencyTest {
    @Autowired
    private HashtagService hashtagService;
    @Autowired
    private PostHashtagService postHashtagService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String tagName;
    private Member member;
    private List<Post> posts;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        tagName = "동시성" + suffix;

        member = memberRepository.save(Member.builder().username("username" + suffix).email("user" + suffix + "@naver.com").build());

        posts = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            posts.add(postRepository.save(Post.builder()
                    .title("테스트" + i)
                    .member(member)
                    .writer(member.getUsername())
                    .content("#" + tagName)
                    .build()));
        }
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM post_hashtag WHERE post_id IN (SELECT id FROM post WHERE member_id = ?)", member.getId());
        jdbcTemplate.update("DELETE FROM post WHERE member_id = ?", member.getId());
        jdbcTemplate.update("DELETE FROM hashtag WHERE name = ?", tagName);
        jdbcTemplate.update("DELETE FROM member WHERE id = ?", member.getId());
    }

    @Test
    @DisplayName("여러 스레드에서 동시에 같은 새 해시태그로 연결해도 해시태그는 1개만 저장되고 모든 게시물에 연결된다.")
    void testGetOrCreateHashtagsConcurrently() throws InterruptedException {
        //given
        int threadCount = posts.size();
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger errors = new AtomicInteger();

        //when
        for (Post post : posts) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    List<Hashtag> hashtags = hashtagService.getOrCreateHashtags(post.getContent());
                    postHashtagService.linkHashtags(post, hashtags);
                } catch (Exception e) {
                    errors.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        //then
        Integer hashtagRows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hashtag WHERE name = ?", Integer.class, tagName);
        List<Integer> postHashtagRows = new ArrayList<>();
        for (Post post : posts) {
            postHashtagRows.add(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM post_hashtag ph JOIN hashtag h ON ph.hashtag_id = h.id WHERE ph.post_id = ? AND h.name = ?",
                    Integer.class, post.getId(), tagName));
        }

        Assertions.assertThat(completed).isTrue();
        Assertions.assertThat(errors.get()).isEqualTo(0);
        Assertions.assertThat(hashtagRows).isEqualTo(1);
        Assertions.assertThat(postHashtagRows).containsOnly(1);
    }
}
