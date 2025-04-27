package org.example.blogsphere.repository;

import org.example.blogsphere.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT COUNT(f) FROM User u JOIN u.followers f WHERE u.id = :userId")
    int countFollowersByUserId(Long userId);

    @Query(value = """
    SELECT u.* FROM users u
    LEFT JOIN user_follows uf ON u.id = uf.following_id
    LEFT JOIN blog_posts bp ON u.id = bp.author_id
    GROUP BY u.id
    ORDER BY COUNT(uf.follower_id) DESC, COUNT(bp.id) DESC
    """, nativeQuery = true)
    List<User> findTopBloggers(Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt BETWEEN :start AND :end")
    Long countByCreatedAtBetween(@Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(u) FROM User u WHERE u.active = true")
    Long countActiveUsers();


    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= ?1")
    Long countNewUsers(LocalDateTime since);

}
