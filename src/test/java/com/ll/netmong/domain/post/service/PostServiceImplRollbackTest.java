package com.ll.netmong.domain.post.service;

import com.ll.netmong.domain.hashtag.entity.Hashtag;
import com.ll.netmong.domain.hashtag.service.HashtagService;
import com.ll.netmong.domain.member.entity.Member;
import com.ll.netmong.domain.member.repository.MemberRepository;
import com.ll.netmong.domain.post.dto.request.PostRequest;
import com.ll.netmong.domain.postHashtag.entity.PostHashtag;
import com.ll.netmong.domain.postHashtag.repository.PostHashtagRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class PostServiceImplRollbackTest {
    @Autowired
    private HashtagService hashtagService;
    @Autowired
    private PostService postService;
    @Autowired
    private MemberRepository memberRepository;
    @SpyBean
    private PostHashtagRepository postHashtagRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String tagName;
    private Member member;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        tagName = "롤백" + suffix;

        member = memberRepository.save(Member.builder().username("username" + suffix).email("user" + suffix + "@naver.com").build());
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM post_hashtag WHERE post_id IN (SELECT id FROM post WHERE member_id = ?)", member.getId());
        jdbcTemplate.update("DELETE FROM post WHERE member_id = ?", member.getId());
        jdbcTemplate.update("DELETE FROM hashtag WHERE name = ?", tagName);
        jdbcTemplate.update("DELETE FROM member WHERE id = ?", member.getId());
    }

    @Test
    @DisplayName("해시태그 연결 저장이 실패하면 게시물 저장도 함께 취소된다.")
    void testUploadPostWithImageRollback() {
        //given
        PostRequest postRequest = new PostRequest();
        postRequest.setTitle("테스트");
        postRequest.setContent("#" + tagName);
        MockMultipartFile image = new MockMultipartFile("file", "test.txt", "text/plain", "test file".getBytes(StandardCharsets.UTF_8));

        List<Hashtag> hashtags = hashtagService.getOrCreateHashtags(postRequest.getContent());
        Integer beforePostCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM post WHERE member_id = ?", Integer.class, member.getId());

        doThrow(new IllegalStateException("해시태그 연결 저장 실패")).when(postHashtagRepository).save(any(PostHashtag.class));

        //when, then
        Assertions.assertThatThrownBy(() -> postService.uploadPostWithImage(postRequest, image, member, hashtags))
                .isInstanceOf(IllegalStateException.class);

        Integer afterPostCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM post WHERE member_id = ?", Integer.class, member.getId());
        Assertions.assertThat(afterPostCount).isEqualTo(beforePostCount);
    }
}
