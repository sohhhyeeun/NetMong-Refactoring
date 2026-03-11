package com.ll.netmong.domain.postHashtag.entity;

import com.ll.netmong.common.BaseEntity;
import com.ll.netmong.domain.hashtag.entity.Hashtag;
import com.ll.netmong.domain.post.entity.Post;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PostHashtag extends BaseEntity {
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "hashtag_id")
    private Hashtag hashtag;

    public PostHashtag(Post post, Hashtag hashtag) {
        this.post = post;
        this.hashtag = hashtag;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public void setHashtag(Hashtag hashtag) {
        this.hashtag = hashtag;
    }
}
