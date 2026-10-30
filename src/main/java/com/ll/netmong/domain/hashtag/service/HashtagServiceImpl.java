package com.ll.netmong.domain.hashtag.service;

import com.ll.netmong.domain.hashtag.entity.Hashtag;
import com.ll.netmong.domain.hashtag.repository.HashtagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HashtagServiceImpl implements HashtagService {
    private final HashtagRepository hashtagRepository;

    private static final Pattern CONTENT_PATTERN = Pattern.compile("#(\\S+)");

    @Override
    public List<String> extractHashtagNames(String content) {
        Matcher matcher = CONTENT_PATTERN.matcher(content);
        Set<String> tags = new LinkedHashSet<>();

        while (matcher.find()) {
            tags.add(matcher.group(1));
        }

        return new ArrayList<>(tags);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<Hashtag> getOrCreateHashtags(String content) {
        List<Hashtag> hashtags = new ArrayList<>();

        for (String name : extractHashtagNames(content)) {
            Hashtag hashtag = hashtagRepository.findByName(name)
                    .orElseGet(() -> createHashtag(name));

            hashtags.add(hashtag);
        }

        return hashtags;
    }

    private Hashtag createHashtag(String name) {
        try {
            return hashtagRepository.save(new Hashtag(name));
        } catch (DataIntegrityViolationException e) {
            //다른 요청이 먼저 저장한 해시태그(name)를 조회
            return hashtagRepository.findByName(name).orElseThrow(() -> e);
        }
    }
}
