package com.ll.netmong.domain.hashtag.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest
@Transactional
class HashtagServiceImplTest {
    @Autowired
    private HashtagService hashtagService;

    @Test
    @DisplayName("content를 받아 중복을 제거한 해시태그 이름을 순서대로 추출한다.")
    void testExtractHashtagNames() {
        //given
        String content = "#a #a #b";

        //when
        List<String> names = hashtagService.extractHashtagNames(content);

        //then
        Assertions.assertThat(names).containsExactly("a", "b");
    }
}
