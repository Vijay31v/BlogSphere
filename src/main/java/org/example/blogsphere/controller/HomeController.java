package org.example.blogsphere.controller;

import org.example.blogsphere.entity.Category;
import org.example.blogsphere.entity.User;
import org.example.blogsphere.entity.BlogPost;
import org.example.blogsphere.service.BlogService;
import org.example.blogsphere.service.CategoryService;
import org.example.blogsphere.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final BlogService blogService;
    private final CategoryService categoryService;
    private final UserService userService;

    @Autowired
    public HomeController(BlogService blogService, CategoryService categoryService, UserService userService) {
        this.blogService = blogService;
        this.categoryService = categoryService;
        this.userService = userService;
    }

    @GetMapping("/")
    public String home(Model model) {
        // Fetch featured posts (published posts with the highest likes/comments)
        PageRequest pageRequest = PageRequest.of(0, 4);
        List<BlogPost> featuredPosts = blogService.getMostPopularBlogPosts(pageRequest).getContent();
        List<String> featuredPostSummaries = new ArrayList<>();
        for (int i = 0; i < featuredPosts.size(); i++) {
            int length = (i == 0) ? 150 : 80;
            featuredPostSummaries.add(blogService.getPlainTextSummary(featuredPosts.get(i).getContent(), length));
        }

        model.addAttribute("featuredPosts", featuredPosts);
        model.addAttribute("featuredPostSummaries", featuredPostSummaries);

        // Fetch the latest published blog posts
        PageRequest pageRequest2 = PageRequest.of(0, 6);
        List<BlogPost> latestBlogs = blogService.getLatestPublishedBlogPosts(pageRequest2).getContent();
        List<String> latestBlogSummaries = latestBlogs.stream()
                .map(blog -> blogService.getPlainTextSummary(blog.getContent(), 120))
                .collect(Collectors.toList());
        model.addAttribute("latestBlogs", latestBlogs);
        model.addAttribute("latestBlogSummaries", latestBlogSummaries);

        // Fetch all categories
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);

        // Fetch top bloggers
        List<User> topBloggers = userService.getTopBloggers(4);

        // Add post-counts and follower counts for top bloggers
        Map<Long, Integer> postCounts = new HashMap<>();
        Map<Long, Integer> followerCounts = new HashMap<>();

        for (User blogger : topBloggers) {
            postCounts.put(blogger.getId(), userService.getUserPostCount(blogger.getId()));
            followerCounts.put(blogger.getId(), userService.getUserFollowerCount(blogger.getId()));
        }

        model.addAttribute("topBloggers", topBloggers);
        model.addAttribute("postCounts", postCounts);
        model.addAttribute("followerCounts", followerCounts);

        return "index";
    }
}
