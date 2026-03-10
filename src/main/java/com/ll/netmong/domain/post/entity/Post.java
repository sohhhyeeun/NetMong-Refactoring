package com.ll.netmong.domain.post.entity;

import com.ll.netmong.common.BaseEntity;
import com.ll.netmong.domain.image.entity.Image;
import com.ll.netmong.domain.likedPost.entity.LikedPost;
import com.ll.netmong.domain.member.entity.Member;
import com.ll.netmong.domain.post.dto.request.UpdatePostRequest;
import com.ll.netmong.domain.postComment.entity.PostComment;
import com.ll.netmong.domain.postHashtag.entity.PostHashtag;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@SuperBuilder(toBuilder = true)
@SQLDelete(sql = "UPDATE post SET status = 'N', deleted_at = NOW() where id = ?")
@Where(clause = "status = 'Y'")
public class Post extends BaseEntity {
    @Column(length=100)
    private String title;
    private String writer;
    @Column(length=100)
    private String content;

    @Column(name = "deleted_at")
//    private String deleteDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
    private LocalDateTime deleteDate;
    @Builder.Default
    @Column(nullable = false)
    private String status = "Y";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<PostComment> comments = new ArrayList<>();

    public void addComment(PostComment comment) {
        this.comments.add(comment);
        comment.setPost(this);
    }

    @Builder.Default
    @Column(name = "likes_count", nullable = false)
    private Long likesCount = 0L;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<LikedPost> likes = new ArrayList<>();

    public void addLike(LikedPost like) {
        this.likes.add(like);
        like.setPost(this);
        this.likesCount++;  // 좋아요 수 증가
    }

    public void removeLike(LikedPost like) {
        this.likes.remove(like);
        like.setPost(null);
        this.likesCount--;  // 좋아요 수 감소
    }

    @OneToMany(mappedBy = "post")
    private List<PostHashtag> names = new ArrayList<>();

    public void addPostHashtag(PostHashtag postHashtag) {
        this.names.add(postHashtag);
        postHashtag.setPost(this);
    }

    public void removePostHashtag(PostHashtag postHashtag) {
        this.names.remove(postHashtag);
        postHashtag.setPost(null);
    }

    @OneToOne(fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "image_id")
    private Image image;

    public static Image createProductImage(String imageUrl, String s3ImageUrl) {
        return new Image(imageUrl, s3ImageUrl);
    }

    public void addPostImage(Image postImage) {
        this.image = postImage;
    }

    public void updatePost(UpdatePostRequest updatePostRequest) {
        this.title = updatePostRequest.getTitle();
        this.content = updatePostRequest.getContent();
    }
}
