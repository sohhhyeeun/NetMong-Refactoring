package com.ll.netmong.domain.post.service;

import com.ll.netmong.domain.member.entity.Member;
import com.ll.netmong.domain.member.repository.MemberRepository;
import com.ll.netmong.domain.member.service.MemberService;
import com.ll.netmong.domain.post.dto.request.PostRequest;
import com.ll.netmong.domain.post.dto.request.UpdatePostRequest;
import com.ll.netmong.domain.post.dto.response.PostResponse;
import com.ll.netmong.domain.post.entity.Post;
import com.ll.netmong.domain.post.repository.PostRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
//@Rollback(value = false)
@Transactional
class PostServiceImplTest {
    String username;
    String email;
    Member member;
    UserDetails userDetails;
    Post post;
    PostRequest postRequest;
    MockMultipartFile image;


    @Autowired
    private MemberService memberService;
    @Autowired
    private PostService postService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    EntityManager em;

    @BeforeEach
    void setUp() {
        username = "username150";
        email = "user150@naver.com";

        member = Member.builder().username(username).email(email).build();
        memberRepository.save(member);
        userDetails = User.withUsername(email).password("password150").authorities("USER").build();

        postRequest = new PostRequest();
        postRequest.setTitle("테스트1");
        postRequest.setContent("#테스트 1번 #실행");

        post = postRepository.save(Post.builder()
                .title("테스트1")
                .member(member)
                .writer(username)
                .content("#테스트 1번 #실행")
                .build());

        image = new MockMultipartFile("file", "test.txt", "text/plain", "test file".getBytes(StandardCharsets.UTF_8) );
    }

    @Test
    @DisplayName("postRequest, foundMember, foundUsername를 받아 post를 업로드 후 값을 저장한다.")
    public void testUploadPostWithImage() throws IOException {
        //given

        //when
        Post post = postService.uploadPostWithImage(postRequest, image, member, List.of());

        //then
        Assertions.assertThat(post.getTitle()).isEqualTo("테스트1");
        Assertions.assertThat(post.getContent()).isEqualTo("#테스트 1번 #실행");
    }

    @Test
    @DisplayName("id, userDetails를 받아 post 상세 값을 보여준다.")
    public void testGetDetail() throws IOException {
        //given
        Post post = postService.uploadPostWithImage(postRequest, image, member, List.of());

        // when
        PostResponse postDetail = postService.getDetail(post.getId(), userDetails);

        //then
        Assertions.assertThat(postDetail.getTitle()).isEqualTo("테스트1");
        Assertions.assertThat(postDetail.getContent()).isEqualTo("#테스트 1번 #실행");
    }

    @Test
    @DisplayName("postId, foundUsername 받아 해당 post를 삭제한다.")
    public void testDeletePost() throws Exception {
        // given
        Member foundMember = memberService.findByEmail(userDetails.getUsername());
        String foundUsername = foundMember.getUsername();

        // when
        postService.deletePost(post.getId(), foundUsername);
        em.flush();
        em.clear();

        // then
        Post deletedPost = (Post) em.createNativeQuery("SELECT * FROM post WHERE id = :id", Post.class)
                .setParameter("id", post.getId())
                .getSingleResult();

        assertNotNull(deletedPost);
        Assertions.assertThat(deletedPost.getStatus()).isEqualTo("N");
    }

    @Test
    @DisplayName("postId, postRequest, username을 받아 해당 post를 수정한다.")
    public void testUpdatePostWithImage() throws IOException {
        //given
        UpdatePostRequest updatePostRequest = new UpdatePostRequest();
        updatePostRequest.setTitle("테스트2");
        updatePostRequest.setContent("#테스트 2번 #실행실행");
        updatePostRequest.setFoundUsername("username150");

        //when
        postService.updatePostWithImage(post.getId(), updatePostRequest, image, List.of());

        //then
        Post updatedPost = postRepository.findById(post.getId())
                .orElseThrow(() -> new EntityNotFoundException("포스트를 찾을 수 없습니다."));
        Assertions.assertThat(updatedPost.getTitle()).isEqualTo("테스트2");
        Assertions.assertThat(updatedPost.getContent()).isEqualTo("#테스트 2번 #실행실행");
    }

//    @Test
//    @DisplayName("pageable 받아 전체 post를 조회한다.")
//    public void testViewPostsByPage() throws IOException {
//        //given
//        for (int i = 0; i < 7; i++) {
//            Post foundPost = postService.uploadPostWithImage(postRequest, image, member);
//        }
//
//        Pageable pageable = PageRequest.of(1, 5);
//
//        //when
//        Page<PostResponse> postsResponse = postService.viewPostsByPage(pageable);
//
//        //then
//        Assertions.assertThat(postsResponse.getTotalElements()).isEqualTo(7);
//    }
}