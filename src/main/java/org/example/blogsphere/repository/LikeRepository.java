package org.example.blogsphere.repository;

import org.example.blogsphere.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    boolean existsByBlogPostIdAndUserId(Long blogPostId, Long userId);
    void deleteByBlogPostIdAndUserId(Long blogPostId, Long userId);
    long countByBlogPostId(Long blogPostId);

    List<Like> findByUserId(Long userId);
    List<Like> findByBlogPostId(Long blogPostId);
}
