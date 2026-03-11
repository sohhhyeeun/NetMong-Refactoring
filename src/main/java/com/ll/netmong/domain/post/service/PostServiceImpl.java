package com.ll.netmong.domain.post.service;

import com.ll.netmong.common.PermissionDeniedException;
import com.ll.netmong.domain.image.service.ImageService;
import com.ll.netmong.domain.likedPost.repository.LikedPostRepository;
import com.ll.netmong.domain.member.entity.Member;
import com.ll.netmong.domain.member.repository.MemberRepository;
import com.ll.netmong.domain.post.dto.request.PostRequest;
import com.ll.netmong.domain.post.dto.request.UpdatePostRequest;
import com.ll.netmong.domain.post.dto.response.PostResponse;
import com.ll.netmong.domain.post.entity.Post;
import com.ll.netmong.domain.post.repository.PostRepository;
import com.ll.netmong.domain.postComment.exception.DataNotFoundException;
import com.ll.netmong.domain.postHashtag.service.PostHashtagService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;
    private final LikedPostRepository likedPostRepository;
    private final MemberRepository memberRepository;
    private final ImageService imageService;
    private final PostHashtagService postHashtagService;

    @Override
    public Page<PostResponse> searchPostsByHashtag (String hashtag, Pageable pageable) {
        Page<Post> postsHashtag = postRepository.findByHashtagName(hashtag, pageable);
        return postsHashtag.map(PostResponse::postsView);
    }

    @Override
    public Page<PostResponse> searchPostsByCategory(String category, String searchWord, Pageable pageable) {
        Map<String, BiFunction<String, Pageable, Page<Post>>> searchByCategory = new HashMap<>(); //BiFunction<String, Pageable, Page<Post>> - String, Pageable 매개변수를 받아 Page<Post> 반환
        searchByCategory.put("작성자", (word, page) -> postRepository.findByWriterContaining(word, page));
        searchByCategory.put("내용", (word, page) -> postRepository.findByContentContaining(word, page));

        BiFunction<String, Pageable, Page<Post>> searchingPosts = searchByCategory.getOrDefault(category, (word, page) -> Page.empty());
        Page<Post> posts = searchingPosts.apply(searchWord, pageable);

        return posts.map(PostResponse::postsView);
    }

    @Override
    public Page<PostResponse> viewPostsByPage(Pageable pageable) {
//        Page<Post> postsPage = postRepository.findAllWithImage(pageable);

//        return postsPage.map(PostResponse::postsView);
        Page<Long> postIdsPage = postRepository.findPostIdsByPage(pageable);

        if (postIdsPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> postIds = postIdsPage.getContent();

        List<Post> posts = postRepository.findAllWithImageAndHashtagsByIdIn(postIds);

        Map<Long, Post> postMap = posts.stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));

        List<PostResponse> content = postIds.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .map(PostResponse::postsView)
                .toList();

        return new PageImpl<>(content, pageable, postIdsPage.getTotalElements());
    }

    private Post uploadPost(PostRequest postRequest, Member foundMember) {
        return postRepository.save(Post.builder()
                .title(postRequest.getTitle())
                .member(foundMember)
                .writer(foundMember.getUsername())
                .content(postRequest.getContent())
                .build());
    }

    @Override
    @Transactional
    public Post uploadPostWithImage(PostRequest postRequest, MultipartFile image, Member foundMember) throws IOException {
        if (Objects.isNull(image) || image.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 없습니다.");
        }

        Post post = uploadPost(postRequest, foundMember);
        post.addPostImage(imageService.uploadImage(post, image).orElseThrow());

        return post;
    }

    @Override
    public PostResponse getDetail(Long id, UserDetails userDetails) {
        Post originPost = postRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("포스트를 찾을 수 없습니다."));

        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new DataNotFoundException("사용자를 찾을 수 없습니다."));
        boolean isLiked = likedPostRepository.existsByMemberAndPost(member, originPost);

        List<String> hashtags = originPost.getNames().stream()
                .map(postHashtag -> postHashtag.getHashtag().getName())
                .collect(Collectors.toList());

        PostResponse postResponse = new PostResponse(originPost);
        postResponse.setIsLiked(isLiked);
        postResponse.setHashtags(hashtags);

        return postResponse;
    }

    @Override
    @Transactional
    public void deletePost(Long postId, String foundUsername) {
        Post originPost = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("포스트를 찾을 수 없습니다."));

        if (!originPost.getMember().getUsername().equals(foundUsername)) {
            throw new PermissionDeniedException("해당 포스트에 대한 삭제 권한이 없습니다.");
        }

        postHashtagService.deleteHashtag(postId);
        postRepository.deleteById(postId);
    }

    private Post updatePost(Long id, UpdatePostRequest updatePostRequest) {
        Post originPost = postRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("포스트를 찾을 수 없습니다."));

        if (originPost.getMember().getUsername().equals(updatePostRequest.getFoundUsername())) {
            originPost.updatePost(updatePostRequest);
            postRepository.save(originPost);
        } else {
            throw new PermissionDeniedException("해당 포스트에 대한 수정 권한이 없습니다.");
        }

        return originPost;
    }

    @Override
    @Transactional
    public void updatePostWithImage(Long id, UpdatePostRequest updatePostRequest, MultipartFile image) throws IOException {
        if (Objects.isNull(image) || image.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 없습니다.");
        }

        Post post = updatePost(id, updatePostRequest);
        post.addPostImage(imageService.uploadImage(post, image).orElseThrow());
    }

    @Override
    public Page<PostResponse> viewPostsByMemberId(Long memberId, Pageable pageable) {
        Page<Post> posts = postRepository.findByMemberIdAndDeleteDateIsNullOrderByCreateDateDesc(memberId, pageable);

        return posts.map(PostResponse::new);
    }

    @Override
    public Post findByPostId(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new DataNotFoundException("해당하는 게시물을 찾을 수 없습니다."));
    }

}
