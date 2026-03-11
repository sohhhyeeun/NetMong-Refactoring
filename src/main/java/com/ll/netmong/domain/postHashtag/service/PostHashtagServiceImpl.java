package com.ll.netmong.domain.postHashtag.service;

import com.ll.netmong.domain.hashtag.entity.Hashtag;
import com.ll.netmong.domain.hashtag.repository.HashtagRepository;
import com.ll.netmong.domain.post.dto.request.UpdatePostRequest;
import com.ll.netmong.domain.post.entity.Post;
import com.ll.netmong.domain.post.repository.PostRepository;
import com.ll.netmong.domain.postHashtag.entity.PostHashtag;
import com.ll.netmong.domain.postHashtag.repository.PostHashtagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostHashtagServiceImpl implements PostHashtagService {
    private final PostHashtagRepository postHashtagRepository;
    private final PostRepository postRepository;
    private final HashtagRepository hashtagRepository;
    private static final Pattern CONTENT_PATTERN = Pattern.compile("#(\\S+)");

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
    public void updateHashtag(Long postId, UpdatePostRequest updatePostRequest) {
        Post originPost = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));

        List<String> originNames = originPost.getNames().stream()
                .map(postHashtag -> postHashtag.getHashtag().getName())
                .collect(Collectors.toList());

        List<String> names = parsingContent(updatePostRequest.getContent());

        //새로운 해시태그(name)를 추가
        for (String name : names) {
            if (!originNames.contains(name)) {
                Hashtag hashtag = hashtagRepository.findByName(name).orElseGet(() -> hashtagRepository.save(new Hashtag(name)));
                postHashtagRepository.save(new PostHashtag(originPost, hashtag));
            }
        }

        //사라진 해시태그(originName)를 삭제
        for (String originName : originNames) {
            if (!names.contains(originName)) {
                PostHashtag postHashtag = postHashtagRepository.findByPostAndHashtag_Name(originPost, originName);
                postHashtagRepository.delete(postHashtag);
            }
        }
    }

    public List<String> parsingContent(String content) {
        Matcher matcher = CONTENT_PATTERN.matcher(content);
        List<String> tags = new ArrayList<>();

        while (matcher.find()) {
            tags.add(matcher.group(1));
        }

        return tags;
    }
}
