package org.example.blogsphere.repository;

import org.example.blogsphere.entity.BlogPost;
import org.example.blogsphere.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    List<BlogPost> findByAuthorId(Long authorId);
    @Query("SELECT b FROM BlogPost b WHERE b.status = 'PUBLISHED' ORDER BY b.createdAt ASC")
    Page<BlogPost> findAll(Pageable pageable);

    int countByAuthorId(Long authorId);

    Optional<BlogPost> findBySlug(String slug);
    Page<BlogPost> findByCategory(Category category, Pageable pageable);
    Page<BlogPost> findByTagsContaining(String tag, Pageable pageable);

    @Query("SELECT b FROM BlogPost b WHERE b.status = 'PUBLISHED'")
    Page<BlogPost> findPublished(Pageable pageable);

    @Query("SELECT b FROM BlogPost b WHERE b.status = 'PUBLISHED' ORDER BY b.createdAt DESC")
    Page<BlogPost> findLatestPublished(Pageable pageable);
    @Query("SELECT COUNT(b) FROM BlogPost b WHERE b.createdAt BETWEEN :start AND :end")
    Long countByCreatedAtBetween(@Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(b) FROM BlogPost b WHERE b.status = 'PUBLISHED'")
    Long countPublishedPosts();
    @Query("SELECT COUNT(b) FROM BlogPost b WHERE b.status = 'DRAFT'")
    Long countDraftPosts();
    @Query("SELECT COUNT(b) FROM BlogPost b WHERE b.status = 'PUBLISHED' AND SIZE(b.comments) > 0")
    Long countPostsWithComments();
    @Query("SELECT b FROM BlogPost b WHERE b.status = 'PUBLISHED' ORDER BY SIZE(b.likes) DESC")
    Page<BlogPost> findMostPopular(Pageable pageable);


    @Query("SELECT b FROM BlogPost b WHERE b.status = 'PUBLISHED' ORDER BY SIZE(b.comments) DESC")
    Page<BlogPost> findMostCommented(Pageable pageable);

    @Query("SELECT b FROM BlogPost b " +
            "LEFT JOIN b.tags t " +
            "WHERE LOWER(CAST(b.title AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(CAST(b.content AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(t) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<BlogPost> search(String keyword, Pageable pageable);

}
