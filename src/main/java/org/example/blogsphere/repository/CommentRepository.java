package org.example.blogsphere.repository;

import org.example.blogsphere.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByBlogPostId(Long blogPostId);
    Page<Comment> findByBlogPostId(Long blogPostId, Pageable pageable);
    List<Comment> findByUserId(Long userId);
    List<Comment> findByBlogPostIdInOrderByCreatedAtDesc(List<Long> blogPostIds, Pageable pageable);


    @Query("SELECT c FROM Comment c ORDER BY c.createdAt DESC")
    Page<Comment> findLatest(Pageable pageable);

    @Query("SELECT c.blogPost.id FROM Comment c WHERE c.id = :commentId")
    Long findBlogPostIdByCommentId(Long commentId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.createdAt BETWEEN :start AND :end")
    Long countByCreatedAtBetween(@Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);


}
