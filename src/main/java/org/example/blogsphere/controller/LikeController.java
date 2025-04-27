package org.example.blogsphere.controller;

import org.example.blogsphere.entity.User;
import org.example.blogsphere.service.LikeService;
import org.example.blogsphere.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/blogs")
public class LikeController {

    private final LikeService likeService;
    private final UserService userService;

    @Autowired
    public LikeController(LikeService likeService, UserService userService) {
        this.likeService = likeService;
        this.userService = userService;
    }

    @PostMapping("/{id}/like")
    public String likePost(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = userService.getUserByUsername(authentication.getName());

        try {
            likeService.likePost(id, user.getId());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/blogs/" + id;
    }

    @PostMapping("/{id}/unlike")
    public String unlikePost(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = userService.getUserByUsername(authentication.getName());

        try {
            likeService.unlikePost(id, user.getId());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/blogs/" + id;
    }

    // AJAX endpoints for likes
    @PostMapping("/{id}/like-ajax")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> likePostAjax(@PathVariable Long id, Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        Map<String, Object> response = new HashMap<>();

        try {
            likeService.likePost(id, user.getId());
            response.put("success", true);
            response.put("likeCount", likeService.getLikeCount(id));
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/unlike-ajax")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> unlikePostAjax(@PathVariable Long id, Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        Map<String, Object> response = new HashMap<>();

        try {
            likeService.unlikePost(id, user.getId());
            response.put("success", true);
            response.put("likeCount", likeService.getLikeCount(id));
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }
}
