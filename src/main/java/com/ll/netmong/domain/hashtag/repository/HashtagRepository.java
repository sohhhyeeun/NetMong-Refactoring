package com.ll.netmong.domain.hashtag.repository;

import com.ll.netmong.domain.hashtag.entity.Hashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface HashtagRepository extends JpaRepository<Hashtag, Long> {
    @Transactional(readOnly = true)
    Optional<Hashtag> findByName(String name);
}
