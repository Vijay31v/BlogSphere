package org.example.blogsphere.controller;

import org.example.blogsphere.entity.BlogPost;
import org.example.blogsphere.entity.Comment;
import org.example.blogsphere.entity.Like;
import org.example.blogsphere.entity.User;
import org.example.blogsphere.service.BlogService;
import org.example.blogsphere.service.CommentService;
import org.example.blogsphere.service.LikeService;
import org.example.blogsphere.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final BlogService blogService;
    private final UserService userService;
    private final CommentService commentService;
    private final LikeService likeService;

    @Autowired
    public DashboardController(BlogService blogService, UserService userService,
                               CommentService commentService, LikeService likeService) {
        this.blogService = blogService;
        this.userService = userService;
        this.commentService = commentService;
        this.likeService = likeService;
    }

    @GetMapping
    public String showDashboard(Authentication authentication,
                                @RequestParam(required = false) String filter,
                                Model model) {
        User user = userService.getUserByUsername(authentication.getName());

        // Get user's posts with optional filtering
        List<BlogPost> userPosts;
        if ("published".equals(filter)) {
            userPosts = blogService.getPublishedBlogPostsByUser(user.getId());
        } else if ("draft".equals(filter)) {
            userPosts = blogService.getDraftBlogPostsByUser(user.getId());
        } else {
            userPosts = blogService.getBlogPostsByUser(user.getId());
        }
        List<String> blogSummaries = userPosts.stream()
                .map(blog -> blogService.getPlainTextSummary(blog.getContent(), 150))
                .collect(Collectors.toList());

        model.addAttribute("userPosts", userPosts);
        model.addAttribute("blogSummaries", blogSummaries);
        model.addAttribute("filter", filter);

        // Get user stats
        int postCount = userPosts.size();

        List<Comment> userComments = commentService.getCommentsByUser(user.getId());
        int commentCount = userComments.size();

        List<Like> userLikes = likeService.getLikesByUser(user.getId());
        int likeCount = userLikes.size();

        // Count received likes on user's posts
        int receivedLikes = 0;
        for (BlogPost post : userPosts) {
            receivedLikes += post.getLikes().size();
        }

        model.addAttribute("postCount", postCount);
        model.addAttribute("commentCount", commentCount);
        model.addAttribute("likeCount", likeCount);
        model.addAttribute("receivedLikes", receivedLikes);

        // Get follower and following counts
        model.addAttribute("followerCount", user.getFollowers().size());
        model.addAttribute("followingCount", user.getFollowing().size());

        // Get recent activity (comments on user's posts)
        List<Comment> recentComments = commentService.getRecentCommentsOnUserPosts(user.getId(), 5);
        model.addAttribute("recentComments", recentComments);

        // Get unread notification count
        model.addAttribute("unreadNotificationsCount", userService.getUnreadNotificationsCount(user.getId()));

        return "dashboard";
    }
}
