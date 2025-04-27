package org.example.blogsphere.controller;

import jakarta.validation.Valid;
import org.example.blogsphere.dto.PasswordChangeDto;
import org.example.blogsphere.dto.UserPreferenceDto;
import org.example.blogsphere.dto.UserProfileDto;
import org.example.blogsphere.entity.User;
import org.example.blogsphere.entity.UserPreference;
import org.example.blogsphere.service.BlogService;
import org.example.blogsphere.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
public class ProfileController {

    private final UserService userService;
    private final BlogService blogService;

    @Autowired
    public ProfileController(UserService userService, BlogService blogService) {
        this.userService = userService;
        this.blogService = blogService;
    }

    @GetMapping("/profile/{username}")
    public String viewProfile(@PathVariable String username,
                              Authentication authentication,
                              Model model) {
        User profileUser = userService.getUserByUsername(username);
        model.addAttribute("profileUser", profileUser);

        // Check if this is the current user's profile
        boolean isCurrentUser = authentication != null &&
                authentication.getName().equals(username);
        model.addAttribute("isCurrentUser", isCurrentUser);

        // Check if current user is following this profile
        boolean isFollowing = false;
        if (authentication != null && !isCurrentUser) {
            User currentUser = userService.getUserByUsername(authentication.getName());
            isFollowing = userService.isFollowing(currentUser.getId(), profileUser.getId());
        }
        model.addAttribute("isFollowing", isFollowing);

        // Get user's blog posts
        model.addAttribute("blogs", blogService.getBlogPostsByUser(profileUser.getId()));

        return "profile";
    }

    @GetMapping("/profile/edit")
    public String showEditProfileForm(Authentication authentication, Model model) {
        User user = userService.getUserByUsername(authentication.getName());
        UserPreference preferences = userService.getUserPreferences(user.getId());

        UserProfileDto profileDto = new UserProfileDto();
        UserPreferenceDto preferencesDto = new UserPreferenceDto();
        profileDto.setFullName(user.getFullName());
        profileDto.setBio(user.getBio());
        profileDto.setLocation(user.getLocation());
        profileDto.setWebsite(user.getWebsite());
        profileDto.setProfileImage(user.getProfileImage());
        profileDto.setTwitterUsername(user.getTwitterUsername());

        preferencesDto.setEmailNewFollower(preferences.isEmailNewFollower());
        preferencesDto.setEmailNewComment(preferences.isEmailNewComment());
        preferencesDto.setEmailNewLike(preferences.isEmailNewLike());
        preferencesDto.setEmailNewsletter(preferences.isEmailNewsletter());

        model.addAttribute("user", user);
        model.addAttribute("profile", profileDto);
        model.addAttribute("preferences", preferencesDto);

        return "profile-edit";
    }

    @PostMapping("/profile/edit")
    public String updateProfile(@Valid @ModelAttribute("profileDto") UserProfileDto profileDto,
                                @Valid @ModelAttribute("preferencesDto") UserPreferenceDto preferencesDto,
                                BindingResult bindingResult,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "profile-edit";
        }

        User user = userService.getUserByUsername(authentication.getName());


        Map<String, String> profileData = getStringStringMap(profileDto);

        userService.updateUserProfile(user.getId(), profileData);

        Map<String, Object> preferences = new HashMap<>();
        preferences.put("emailNewFollower", preferencesDto.isEmailNewFollower());
        preferences.put("emailNewComment", preferencesDto.isEmailNewComment());
        preferences.put("emailNewLike", preferencesDto.isEmailNewLike());
        preferences.put("emailNewsletter", preferencesDto.isEmailNewsletter());

        userService.updateUserPreferences(user.getId(), preferences);

        redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully");
        return "redirect:/profile/" + authentication.getName();
    }

    private static Map<String, String> getStringStringMap(UserProfileDto profileDto) {
        Map<String, String> profileData = new HashMap<>();
        profileData.put("fullName", profileDto.getFullName());
        profileData.put("bio", profileDto.getBio());
        profileData.put("location", profileDto.getLocation());
        profileData.put("website", profileDto.getWebsite());
        if (profileDto.getProfileImage()!=null || !profileDto.getProfileImage().isBlank()) {
            profileData.put("profileImage", profileDto.getProfileImage());
        }
        profileData.put("twitterUsername", profileDto.getTwitterUsername());
        return profileData;
    }

    @GetMapping("/profile/preferences")
    public String showPreferencesForm(Authentication authentication, Model model) {
        User user = userService.getUserByUsername(authentication.getName());
        UserPreference preferences = user.getPreferences();

        UserPreferenceDto preferencesDto = new UserPreferenceDto();
        if (preferences != null) {
            preferencesDto.setEmailNewFollower(preferences.isEmailNewFollower());
            preferencesDto.setEmailNewComment(preferences.isEmailNewComment());
            preferencesDto.setEmailNewLike(preferences.isEmailNewLike());
            preferencesDto.setEmailNewsletter(preferences.isEmailNewsletter());
        }

        model.addAttribute("preferences", preferencesDto);

        return "profile-edit";
    }

    @PostMapping("/profile/preferences")
    public String updatePreferences(@ModelAttribute("preferences") UserPreferenceDto preferencesDto,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        User user = userService.getUserByUsername(authentication.getName());

        Map<String, Object> preferences = new HashMap<>();
        preferences.put("emailNewFollower", preferencesDto.isEmailNewFollower());
        preferences.put("emailNewComment", preferencesDto.isEmailNewComment());
        preferences.put("emailNewLike", preferencesDto.isEmailNewLike());
        preferences.put("emailNewsletter", preferencesDto.isEmailNewsletter());

        userService.updateUserPreferences(user.getId(), preferences);

        redirectAttributes.addFlashAttribute("successMessage", "Preferences updated successfully");
        return "redirect:/profile/edit";
    }

    @GetMapping("/profile/change-password")
    public String showChangePasswordForm(Model model) {
        model.addAttribute("passwordChangeDto", new PasswordChangeDto());
        return "profile-edit";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@Valid @ModelAttribute("passwordChangeDto") PasswordChangeDto passwordChangeDto,
                                 BindingResult bindingResult,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        // Check if passwords match
        if (!passwordChangeDto.getNewPassword().equals(passwordChangeDto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.passwordChange", "Passwords do not match");
        }

        if (bindingResult.hasErrors()) {
            return "profile-edit";
        }

        User user = userService.getUserByUsername(authentication.getName());
        boolean success = userService.changePassword(
                user.getId(),
                passwordChangeDto.getCurrentPassword(),
                passwordChangeDto.getNewPassword()
        );

        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Current password is incorrect");
            return "redirect:/profile/change-password";
        }

        return "redirect:/profile/" + authentication.getName();
    }
}
