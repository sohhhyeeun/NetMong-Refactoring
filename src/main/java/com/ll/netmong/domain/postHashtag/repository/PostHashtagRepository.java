package com.ll.netmong.domain.postHashtag.repository;

import com.ll.netmong.domain.post.entity.Post;
import com.ll.netmong.domain.postHashtag.entity.PostHashtag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostHashtagRepository extends JpaRepository<PostHashtag, Long> {
    List<PostHashtag> findByPostId(Long postId);
    PostHashtag findByPostAndHashtag_Name(Post post, String name);
}
