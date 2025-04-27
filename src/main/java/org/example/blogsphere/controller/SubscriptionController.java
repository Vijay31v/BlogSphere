package org.example.blogsphere.controller;

import org.example.blogsphere.entity.User;
import org.example.blogsphere.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final UserService userService;

    @Autowired
    public SubscriptionController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String viewSubscriptions(Authentication authentication, Model model) {
        User currentUser = userService.getUserByUsername(authentication.getName());

        model.addAttribute("following", userService.getFollowing(currentUser.getId()));
        model.addAttribute("followers", userService.getFollowers(currentUser.getId()));

        return "subscriptions";
    }


    @PostMapping("/{id}")
    public String followUser(@PathVariable Long id,
                             Authentication authentication,
                             @RequestParam(required = false) String redirect,
                             RedirectAttributes redirectAttributes) {
        User currentUser = userService.getUserByUsername(authentication.getName());

        try {
            userService.followUser(currentUser.getId(), id);
            redirectAttributes.addFlashAttribute("successMessage", "You are now following this user");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        // Redirect back to the page the user was on
        if (redirect != null && !redirect.isEmpty()) {
            return "redirect:" + redirect;
        }

        // Default redirect to the user's profile that was followed
        User followedUser = userService.getUserById(id);
        return "redirect:/profile/" + followedUser.getUsername();
    }

    @PostMapping("/{id}/unfollow")
    public String unfollowUser(@PathVariable Long id,
                               Authentication authentication,
                               @RequestParam(required = false) String redirect,
                               RedirectAttributes redirectAttributes) {
        User currentUser = userService.getUserByUsername(authentication.getName());

        try {
            userService.unfollowUser(currentUser.getId(), id);
            redirectAttributes.addFlashAttribute("successMessage", "You have unfollowed this user");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        // Redirect back to the page the user was on
        if (redirect != null && !redirect.isEmpty()) {
            return "redirect:" + redirect;
        }

        // Default redirect to the user's profile that was unfollowed
        User unfollowedUser = userService.getUserById(id);
        return "redirect:/profile/" + unfollowedUser.getUsername();
    }
}
