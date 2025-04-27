package org.example.blogsphere.controller;

import jakarta.validation.Valid;
import org.example.blogsphere.dto.CommentDto;
import org.example.blogsphere.entity.User;
import org.example.blogsphere.service.CommentService;
import org.example.blogsphere.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;

    @Autowired
    public CommentController(CommentService commentService, UserService userService) {
        this.commentService = commentService;
        this.userService = userService;
    }

    @PostMapping
    public String addComment(@Valid @ModelAttribute("commentDto") CommentDto commentDto,
                             BindingResult bindingResult,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Comment must not be empty and less than 1000 characters");
            return "redirect:/blogs/" + commentDto.getBlogPostId();
        }

        User user = userService.getUserByUsername(authentication.getName());
        try {
            commentService.addComment(commentDto.getBlogPostId(), user, commentDto.getCommentText());
            redirectAttributes.addFlashAttribute("successMessage", "Comment added successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/blogs/" + commentDto.getBlogPostId().toString();
    }

    @PostMapping("/{id}/delete")
    public String deleteComment(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        User currentUser = userService.getUserByUsername(authentication.getName());

        try {
            // Get blog post ID before deleting for redirect
            Long blogPostId = commentService.getBlogPostIdByCommentId(id);

            commentService.deleteComment(id, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Comment deleted successfully");

            return "redirect:/blogs/" + blogPostId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/dashboard";
        }
    }
}
