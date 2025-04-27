package org.example.blogsphere.controller;

import jakarta.validation.Valid;
import org.example.blogsphere.dto.PasswordChangeDto;
import org.example.blogsphere.dto.UserRegistrationDto;
import org.example.blogsphere.entity.Role;
import org.example.blogsphere.service.EmailService;
import org.example.blogsphere.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;
    private final EmailService emailSevice;

    @Autowired
    public AuthController(UserService userService, EmailService emailSevice) {
        this.userService = userService;
        this.emailSevice = emailSevice;
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new UserRegistrationDto());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") UserRegistrationDto userDto,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        // Check if passwords match
        if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.user", "Passwords do not match");
        }

        // Check if username is available
        if (userDto.getUsername() != null && !userService.isUsernameAvailable(userDto.getUsername())) {
            bindingResult.rejectValue("username", "error.user", "Username is already taken");
        }

        // Check if email is available
        if (userDto.getEmail() != null && !userService.isEmailAvailable(userDto.getEmail())) {
            bindingResult.rejectValue("email", "error.user", "Email is already registered");
        }

        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            userService.registerUser(userDto.getUsername(), userDto.getEmail(), userDto.getPassword(), Role.ROLE_BLOGGER);
            emailSevice.sendWelcomeEmail(userDto.getEmail(), userDto.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please login.");
            return "redirect:/login?registered";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/password/request-reset")
    public String showPasswordResetRequestForm() {
        return "password-reset";
    }

    @PostMapping("/password/request-reset")
    public String requestPasswordReset(@RequestParam String email, RedirectAttributes redirectAttributes) {
        try {
            userService.requestPasswordReset(email);
            redirectAttributes.addFlashAttribute("resetRequested", true);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/password/request-reset";
    }

    @GetMapping("/password/reset")
    public String showPasswordResetForm(@RequestParam String token, Model model, RedirectAttributes redirectAttributes) {
        if (!userService.isValidPasswordResetToken(token)) {
            redirectAttributes.addFlashAttribute("error", "Invalid or expired password reset token.");
            return "redirect:/password/request-reset";
        }

        model.addAttribute("token", token);
        model.addAttribute("passwordChangeDto", new PasswordChangeDto());

        return "password-reset";
    }

    @PostMapping("/password/reset")
    public String resetPassword(@RequestParam String token,
                                @Valid @ModelAttribute("passwordChangeDto") PasswordChangeDto passwordChangeDto,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (!userService.isValidPasswordResetToken(token)) {
            redirectAttributes.addFlashAttribute("error", "Invalid or expired password reset token.");
            return "redirect:/password/request-reset";
        }

        // Only validate new password and confirm password
        if (!passwordChangeDto.getNewPassword().equals(passwordChangeDto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.passwordReset", "Passwords do not match");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("token", token);
            return "password-reset";
        }

        try {
            userService.resetPassword(token, passwordChangeDto.getNewPassword());
            redirectAttributes.addFlashAttribute("successMessage", "Your password has been reset successfully. Please login with your new password.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("token", token);
            return "password-reset";
        }
    }
}
