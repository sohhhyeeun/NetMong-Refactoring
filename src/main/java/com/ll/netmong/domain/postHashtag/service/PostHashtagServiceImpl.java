package com.ll.netmong.domain.postHashtag.service;

import com.ll.netmong.domain.hashtag.entity.Hashtag;
import com.ll.netmong.domain.hashtag.repository.HashtagRepository;
import com.ll.netmong.domain.post.entity.Post;
import com.ll.netmong.domain.postHashtag.entity.PostHashtag;
import com.ll.netmong.domain.postHashtag.repository.PostHashtagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostHashtagServiceImpl implements PostHashtagService {
    private final PostHashtagRepository postHashtagRepository;
    private final HashtagRepository hashtagRepository;

    @Override
    @Transactional
    public void deleteHashtag(Long postId) {
        List<PostHashtag> postHashtags = postHashtagRepository.findByPostId(postId);

        for (PostHashtag postHashtag : postHashtags) {
            postHashtagRepository.delete(postHashtag);
        }
    }

    @Override
    @Transactional
    public void linkHashtags(Post post, List<Hashtag> hashtags) {
        for (Long hashtagId : toHashtagIds(hashtags)) {
            addPostHashtag(post, hashtagId);
        }
    }

    @Override
    @Transactional
    public void updateHashtags(Post post, List<Hashtag> hashtags) {
        Set<Long> newHashtagIds = toHashtagIds(hashtags);
        Set<Long> keptHashtagIds = new HashSet<>();

        //사라지거나 중복된 해시태그(postHashtag)를 삭제
        for (PostHashtag postHashtag : postHashtagRepository.findByPostId(post.getId())) {
            Long hashtagId = postHashtag.getHashtag().getId();

            if (!newHashtagIds.contains(hashtagId) || keptHashtagIds.contains(hashtagId)) {
                post.removePostHashtag(postHashtag);
                postHashtagRepository.delete(postHashtag);
            } else {
                keptHashtagIds.add(hashtagId);
            }
        }

        //새로운 해시태그(hashtagId)를 추가
        for (Long hashtagId : newHashtagIds) {
            if (!keptHashtagIds.contains(hashtagId)) {
                addPostHashtag(post, hashtagId);
            }
        }
    }

    private Set<Long> toHashtagIds(List<Hashtag> hashtags) {
        Set<Long> hashtagIds = new LinkedHashSet<>();

        for (Hashtag hashtag : hashtags) {
            hashtagIds.add(hashtag.getId());
        }

        return hashtagIds;
    }

    private void addPostHashtag(Post post, Long hashtagId) {
        PostHashtag postHashtag = postHashtagRepository.save(new PostHashtag(post, hashtagRepository.getReferenceById(hashtagId)));
        post.addPostHashtag(postHashtag);
    }
}
