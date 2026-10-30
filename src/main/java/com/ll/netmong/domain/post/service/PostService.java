package com.ll.netmong.domain.post.service;

import com.ll.netmong.domain.hashtag.entity.Hashtag;
import com.ll.netmong.domain.member.entity.Member;
import com.ll.netmong.domain.post.dto.request.PostRequest;
import com.ll.netmong.domain.post.dto.request.UpdatePostRequest;
import com.ll.netmong.domain.post.dto.response.PostResponse;
import com.ll.netmong.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface PostService {
    Page<PostResponse> searchPostsByHashtag (String hashtag, Pageable pageable);
    Page<PostResponse> searchPostsByCategory(String category, String searchWord, Pageable pageable);
    Page<PostResponse> viewPostsByPage(Pageable pageable);
    Post uploadPostWithImage(PostRequest postRequest, MultipartFile image, Member foundMember, List<Hashtag> hashtags) throws IOException;
    PostResponse getDetail(Long id, UserDetails userDetails);
    void updatePostWithImage(Long id, UpdatePostRequest updatePostRequest, MultipartFile image, List<Hashtag> hashtags) throws IOException;
    void deletePost(Long postId, String foundUsername);

    Post findByPostId(Long postId);
    Page<PostResponse> viewPostsByMemberId(Long memberId, Pageable pageable);
}
