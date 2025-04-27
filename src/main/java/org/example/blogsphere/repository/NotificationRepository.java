package org.example.blogsphere.repository;

import org.example.blogsphere.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdAndReadFalse(Long userId);
    Page<Notification> findByUserId(Long userId, Pageable pageable);
    int countByUserIdAndReadFalse(Long userId);
    void deleteByUserId(Long userId);
}
