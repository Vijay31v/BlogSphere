package org.example.blogsphere.controller;

import org.example.blogsphere.dto.NotificationDto;
import org.example.blogsphere.entity.Notification;
import org.example.blogsphere.entity.User;
import org.example.blogsphere.service.NotificationService;
import org.example.blogsphere.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @Autowired
    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping
    public String viewNotifications(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    Authentication authentication,
                                    Model model) {
        User currentUser = userService.getUserByUsername(authentication.getName());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Notification> notifications = notificationService.getUserNotifications(currentUser.getId(), pageable);
        model.addAttribute("notifications", notifications);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notifications.getTotalPages());

        return "notifications";
    }

    @PostMapping("/{id}/read")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id, Authentication authentication) {
        User currentUser = userService.getUserByUsername(authentication.getName());
        Map<String, Object> response = new HashMap<>();

        try {
            notificationService.markAsRead(id);
            response.put("success", true);
            response.put("unreadCount", notificationService.countUnreadNotifications(currentUser.getId()));
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/mark-all-read")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAllAsRead(Authentication authentication) {
        User currentUser = userService.getUserByUsername(authentication.getName());
        Map<String, Object> response = new HashMap<>();

        try {
            notificationService.markAllAsRead(currentUser.getId());
            response.put("success", true);
            response.put("unreadCount", 0);
            response.put("message", "All notifications marked as read");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUnreadNotifications(Authentication authentication) {
        User currentUser = userService.getUserByUsername(authentication.getName());
        Map<String, Object> response = new HashMap<>();

        List<Notification> unreadNotifications = notificationService.getUnreadNotifications(currentUser.getId());

        List<NotificationDto> notificationDtos = unreadNotifications.stream()
                .map(notification -> {
                    NotificationDto dto = new NotificationDto();
                    dto.setId(notification.getId());
                    dto.setMessage(notification.getMessage());
                    dto.setLink(notification.getLink());
                    dto.setType(notification.getType().toString());
                    dto.setRead(notification.isRead());
                    dto.setCreatedAt(notification.getCreatedAt());
                    return dto;
                })
                .collect(Collectors.toList());

        response.put("notifications", notificationDtos);
        response.put("unreadCount", notificationDtos.size());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/unread")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAsUnread(@PathVariable Long id, Authentication authentication) {
        User currentUser = userService.getUserByUsername(authentication.getName());
        Map<String, Object> response = new HashMap<>();

        try {
            notificationService.markAsUnread(id);
            response.put("success", true);
            response.put("unreadCount", notificationService.countUnreadNotifications(currentUser.getId()));
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }


    @PostMapping("/{id}/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteNotification(@PathVariable Long id, Authentication authentication) {
        User currentUser = userService.getUserByUsername(authentication.getName());
        Map<String, Object> response = new HashMap<>();

        try {
            notificationService.deleteNotification(id);
            response.put("success", true);
            response.put("unreadCount", notificationService.countUnreadNotifications(currentUser.getId()));
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/delete-all")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteAllNotifications(Authentication authentication) {
        User currentUser = userService.getUserByUsername(authentication.getName());
        Map<String, Object> response = new HashMap<>();

        try {
            notificationService.deleteAllUserNotifications(currentUser.getId());
            response.put("success", true);
            response.put("message", "All notifications deleted successfully");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }
}
