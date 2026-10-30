package com.ll.netmong.domain.postHashtag.service;

import com.ll.netmong.domain.hashtag.entity.Hashtag;
import com.ll.netmong.domain.hashtag.repository.HashtagRepository;
import com.ll.netmong.domain.hashtag.service.HashtagService;
import com.ll.netmong.domain.member.entity.Member;
import com.ll.netmong.domain.member.repository.MemberRepository;
import com.ll.netmong.domain.member.service.MemberService;
import com.ll.netmong.domain.post.entity.Post;
import com.ll.netmong.domain.post.repository.PostRepository;
import com.ll.netmong.domain.post.service.PostService;
import com.ll.netmong.domain.postHashtag.entity.PostHashtag;
import com.ll.netmong.domain.postHashtag.repository.PostHashtagRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SpringBootTest
@Transactional
class PostHashtagServiceImplTest {
    String username;
    Member member;
    Post post;
    MockMultipartFile image;

    @Autowired
    PostService postService;
    @Autowired
    private MemberService memberService;
    @Autowired
    private  PostHashtagService postHashtagService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private HashtagService hashtagService;
    @Autowired
    private HashtagRepository hashtagRepository;
    @Autowired
    private PostHashtagRepository postHashtagRepository;
    @Autowired
    EntityManager em;

//    @BeforeEach
//    void setUp() {
//        username = "username150";
//
//        JoinRequest joinRequest = new JoinRequest();
//        joinRequest.setUsername(username);
//        joinRequest.setPassword("password150");
//        joinRequest.setRealname("realname150");
//        joinRequest.setEmail("name150@naver.com");
//        member = memberService.createMember(joinRequest);
//        memberRepository.save(member);
//
//        PostRequest postRequest = new PostRequest();
//        postRequest.setTitle("테스트");
//        postRequest.setContent("#테스트 작성 중 #실행");
//
//        image = new MockMultipartFile("file", "test.txt", "text/plain", "test file".getBytes(StandardCharsets.UTF_8) );
//    }
//
//    @Test
//    @DisplayName("postId를 받아 해당 게시물의 해시태그를 삭제한다.")
//    void testDeleteHashtag() {
//        //given
//        Post savedPost = postRepository.save(Post.builder()
//                .title("테스트")
//                .member(member)
//                .writer(username)
//                .content("#테스트 작성 중")
//                .build());
//
//        Post foundPost = postRepository.findById(post.getId())
//                .orElseThrow(() -> new EntityNotFoundException("포스트를 찾을 수 없습니다."));
//
//        //when
//        postHashtagService.deleteHashtag(savedPost.getId());
//
//        //then
//        Assertions.assertThat(foundPost.getStatus()).isEqualTo("N");
//    }
//
//    @Test
//    @DisplayName("postId와 postRequest를 받아 해당 게시물의 해시태그를 수정(추가, 삭제)한다.")
//    void testUpdateHashtag() {
//        //given
//        Post post = Post.builder()
//                .title("테스트")
//                .member(member)
//                .writer(username)
//                .content("#테스트 작성 중")
//                .build();
//        List<String> originNames = post.getNames().stream()
//                .map(postHashtag -> postHashtag.getHashtag().getName())
//                .collect(Collectors.toList());
//
//        UpdatePostRequest updatedPostRequest = new UpdatePostRequest();
//        updatedPostRequest.setTitle("멍멍");
//        updatedPostRequest.setContent("#오늘 강아지 #멍멍");
//
//        //when
//        postHashtagService.updateHashtag(post.getId(), updatedPostRequest);
//
//        //then
//        Assertions.assertThat(originNames.get(0)).isEqualTo("오늘");
//        Assertions.assertThat(originNames.get(1)).isEqualTo("멍멍");
//    }

    @Test
    @DisplayName("post와 hashtags를 받아 같은 해시태그는 1개만 연결한다.")
    void testLinkHashtagsWithDuplicateTag() {
        //given
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Post savedPost = savePost("#멍멍" + suffix + " #산책" + suffix + " #멍멍" + suffix);
        List<Hashtag> hashtags = saveHashtags(savedPost.getContent());

        //when
        postHashtagService.linkHashtags(savedPost, hashtags);
        em.flush();
        em.clear();

        //then
        List<String> names = postHashtagRepository.findByPostId(savedPost.getId()).stream()
                .map(postHashtag -> postHashtag.getHashtag().getName())
                .toList();
        Assertions.assertThat(names).containsExactlyInAnyOrder("멍멍" + suffix, "산책" + suffix);
    }

    @Test
    @DisplayName("post와 hashtags를 받아 중복 연결이 있는 게시물의 해시태그를 수정(추가, 삭제)한다.")
    void testUpdateHashtagsWithDuplicateRows() {
        //given
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Post savedPost = savePost("#멍멍" + suffix + " #산책" + suffix);
        List<Hashtag> originHashtags = saveHashtags(savedPost.getContent());
        postHashtagRepository.save(new PostHashtag(savedPost, originHashtags.get(0)));
        postHashtagRepository.save(new PostHashtag(savedPost, originHashtags.get(0)));
        postHashtagRepository.save(new PostHashtag(savedPost, originHashtags.get(1)));
        em.flush();
        em.clear();

        Post foundPost = postRepository.findById(savedPost.getId())
                .orElseThrow(() -> new EntityNotFoundException("포스트를 찾을 수 없습니다."));
        Hashtag keptHashtag = hashtagRepository.findByName("멍멍" + suffix).orElseThrow();
        Hashtag newHashtag = hashtagRepository.save(new Hashtag("오늘" + suffix));

        //when
        postHashtagService.updateHashtags(foundPost, List.of(keptHashtag, newHashtag));
        em.flush();
        em.clear();

        //then
        List<String> names = postHashtagRepository.findByPostId(savedPost.getId()).stream()
                .map(postHashtag -> postHashtag.getHashtag().getName())
                .toList();
        Assertions.assertThat(names).containsExactlyInAnyOrder("멍멍" + suffix, "오늘" + suffix);
    }

    private Post savePost(String content) {
        String memberSuffix = UUID.randomUUID().toString().substring(0, 8);
        Member savedMember = memberRepository.save(Member.builder().username("username" + memberSuffix).email("user" + memberSuffix + "@naver.com").build());

        return postRepository.save(Post.builder()
                .title("테스트")
                .member(savedMember)
                .writer(savedMember.getUsername())
                .content(content)
                .build());
    }

    private List<Hashtag> saveHashtags(String content) {
        List<Hashtag> hashtags = new ArrayList<>();

        for (String name : hashtagService.extractHashtagNames(content)) {
            hashtags.add(hashtagRepository.save(new Hashtag(name)));
        }

        return hashtags;
    }
}