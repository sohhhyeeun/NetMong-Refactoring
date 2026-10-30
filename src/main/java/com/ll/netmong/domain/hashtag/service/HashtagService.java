package com.ll.netmong.domain.hashtag.service;

import com.ll.netmong.domain.hashtag.entity.Hashtag;

import java.util.List;

public interface HashtagService {
    List<String> extractHashtagNames(String content);
    List<Hashtag> getOrCreateHashtags(String content);
}
