package org.example.blogsphere.controller;

import org.example.blogsphere.entity.BlogPost;
import org.example.blogsphere.entity.Comment;
import org.example.blogsphere.entity.Role;
import org.example.blogsphere.entity.User;
import org.example.blogsphere.service.BlogService;
import org.example.blogsphere.service.CommentService;
import org.example.blogsphere.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final BlogService blogService;
    private final CommentService commentService;

    @Autowired
    public AdminController(UserService userService, BlogService blogService, CommentService commentService) {
        this.userService = userService;
        this.blogService = blogService;
        this.commentService = commentService;
    }

    @GetMapping
    public String showAdminDashboard(Model model) {
        // User stats
        Long totalUsers = userService.getUsersCount();
        model.addAttribute("totalUsers", totalUsers);

        // Blog stats
        Long totalBlogs = blogService.getBlogPostCount();
        model.addAttribute("totalBlogs", totalBlogs);

        // Comment stats
        Long totalComments = commentService.getCommentCount();
        model.addAttribute("totalComments",totalComments);

        Long newUsers = userService.getUsersCreatedInLast30Days();
        model.addAttribute("newUsers", newUsers);

        return "admin/dashboard";
    }

    @GetMapping("/dashboard-data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDashboardData(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();

        // Validate filters
        if(year == null || year < 2025 || year > currentYear) year = currentYear;
        if(month == null || month < 1 || month > 12) month = currentMonth;

        Map<String, Object> data = new HashMap<>();

        try {

            data.put("userStats", userService.getUserStats(year, month));
            data.put("blogStats", blogService.getBlogStats(year, month));
            data.put("commentStats", commentService.getCommentStats(year, month));
            System.out.println(data);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/users")
    public String manageUsers(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Model model) {
        Page<User> users = userService.getAllUsers(PageRequest.of(page, size));

        model.addAttribute("users", users);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", users.getTotalPages());

        return "admin/user-management";
    }

    @PostMapping("/users/{id}/toggle-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> requestBody) {

        Map<String, Object> response = new HashMap<>();
        boolean active = requestBody.get("active");

        try {
            if (active) {
                userService.activateUser(id);
            } else {
                userService.deactivateUser(id);
            }
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserById(id);
            if (user.getRole() == Role.ROLE_ADMIN) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete an admin user");
                return "redirect:/admin/users";
            }

            // Deactivate for now (soft delete)
            userService.deactivateUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "User has been deactivated");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/users/create")
    public String createUser(@RequestParam String username,
                             @RequestParam String email,
                             @RequestParam String password,
                             @RequestParam String _role,
                             RedirectAttributes redirectAttributes) {
        try {
            Role role = Role.valueOf(_role);
            userService.registerUser(username, email, password, role);
            redirectAttributes.addFlashAttribute("successMessage", "User created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating user: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    @GetMapping("/users/{id}/edit")
    public String showEditUserForm(@PathVariable Long id, Model model) {
        try {
            User user = userService.getUserById(id);
            model.addAttribute("user", user);
            model.addAttribute("roles", Role.values());
            return "admin/user-edit";
        } catch (Exception e) {
            return "redirect:/admin/users";
        }
    }

    @PostMapping("/users/{id}/update")
    public String updateUser(@PathVariable Long id,
                             @RequestParam String username,
                             @RequestParam String email,
                             @RequestParam(required = false) String fullName,
                             @RequestParam String role,
                             @RequestParam(defaultValue = "true") boolean active,
                             RedirectAttributes redirectAttributes) {
        try {

            User user = userService.getUserById(id);

            // Update user details
            user.setUsername(username);
            user.setEmail(email);
            user.setFullName(fullName);
            user.setRole(Role.valueOf(role));
            user.setActive(active);

            // Save updated user
            userService.updateUser(user);

            redirectAttributes.addFlashAttribute("successMessage", "User updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating user: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    @GetMapping("/blogs")
    public String manageBlogs(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BlogPost> blogs = blogService.getAllBlogPosts(pageable);

        model.addAttribute("blogs", blogs);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", blogs.getTotalPages());

        return "admin/blog-management";
    }

    @PostMapping("/blogs/{id}/delete")
    public String deleteBlog(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            blogService.deleteBlogPost(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Blog post deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/blogs";
    }

    @GetMapping("/comments")
    public String manageComments(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 Model model) {
        Page<Comment> comments = commentService.getLatestComments(page, size);

        model.addAttribute("comments", comments);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", comments.getTotalPages());

        return "admin/comment-management";
    }

    @PostMapping("/comments/{id}/delete")
    public String deleteComment(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User adminUser = userService.getUserByUsername(authentication.getName());
            commentService.deleteComment(id, adminUser);
            redirectAttributes.addFlashAttribute("successMessage", "Comment deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/comments";
    }
    
}
