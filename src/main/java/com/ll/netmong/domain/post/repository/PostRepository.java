package com.ll.netmong.domain.post.repository;

import com.ll.netmong.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findById(Long id);

    Page<Post> findByMemberIdAndDeleteDateIsNullOrderByCreateDateDesc(@Param("memberId") Long memberId, Pageable pageable);

    Page<Post> findByWriterContaining(String searchWord, Pageable pageable);

    Page<Post> findByContentContaining(String searchWord, Pageable pageable);
  
    Long countByMemberIdAndDeleteDateIsNull(@Param("memberId") Long memberId);

//    @Query("SELECT p FROM Post p JOIN p.names ph WHERE ph.name = :hashtag")
//    Page<Post> findByHashtagName(@Param("hashtag") String hashtag, Pageable pageable);
    @Query("SELECT p FROM Post p JOIN p.names ph JOIN ph.hashtag h WHERE h.name = :hashtag")
    Page<Post> findByHashtagName(@Param("hashtag") String hashtag, Pageable pageable);

//    @Query("SELECT p FROM Post p JOIN FETCH p.image")
//    Page<Post> findAllWithImage(Pageable pageable);

    @Query(
            value = """
                    SELECT p.id
                    FROM Post p
                    ORDER BY p.modifyDate DESC
                    """,
            countQuery = """
                    SELECT count(p)
                    FROM Post p
                    """
    )
    Page<Long> findPostIdsByPage(Pageable pageable);

    @Query("""
            SELECT DISTINCT p
            FROM Post p
            JOIN FETCH p.image
            LEFT JOIN FETCH p.names ph
            LEFT JOIN FETCH ph.hashtag
            WHERE p.id IN :postIds
            """)
    List<Post> findAllWithImageAndHashtagsByIdIn(@Param("postIds") List<Long> postIds);
}
