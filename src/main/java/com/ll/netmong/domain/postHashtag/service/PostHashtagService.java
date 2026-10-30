package com.ll.netmong.domain.postHashtag.service;

import com.ll.netmong.domain.hashtag.entity.Hashtag;
import com.ll.netmong.domain.post.entity.Post;

import java.util.List;

public interface PostHashtagService {
    void deleteHashtag(Long postId);
    void linkHashtags(Post post, List<Hashtag> hashtags);
    void updateHashtags(Post post, List<Hashtag> hashtags);
}
