package org.example.blogsphere.controller;

import org.example.blogsphere.entity.Notification;
import org.example.blogsphere.entity.User;
import org.example.blogsphere.service.NotificationService;
import org.example.blogsphere.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @ModelAttribute
    public void addNotificationAttributes(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() &&
                !(authentication instanceof AnonymousAuthenticationToken)) {

            String username = authentication.getName();
            User user = userService.getUserByUsername(username);

            if (user != null) {
                // Add unread notification count
                int unreadCount = notificationService.countUnreadNotifications(user.getId());
                model.addAttribute("unreadNotificationCount", unreadCount);

                // Add recent notifications for dropdown
                List<Notification> recentNotifications =
                        notificationService.getRecentNotifications(user.getId(), 5);
                model.addAttribute("recentNotifications", recentNotifications);
            }
        } else {
            // Set defaults for non-authenticated users
            model.addAttribute("unreadNotificationCount", 0);
            model.addAttribute("recentNotifications", new ArrayList<>());
        }
    }
}

