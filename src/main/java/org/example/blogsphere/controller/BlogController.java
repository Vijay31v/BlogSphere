package org.example.blogsphere.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.example.blogsphere.dto.BlogPostDto;
import org.example.blogsphere.dto.CommentDto;
import org.example.blogsphere.entity.*;
import org.example.blogsphere.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/blogs")
public class BlogController {

    private final BlogService blogService;
    private final UserService userService;
    private final CategoryService categoryService;
    private final NotificationService notificationService;

    @Autowired
    public BlogController(BlogService blogService, UserService userService, CategoryService categoryService, NotificationService notificationService) {
        this.blogService = blogService;
        this.userService = userService;
        this.categoryService = categoryService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public String getAllBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "newest") String sort,
            Model model) {

        Page<BlogPost> blogs;
        Pageable pageable;

        if (search != null && !search.isEmpty()) {
            pageable = PageRequest.of(page, size);
            blogs = blogService.searchBlogPosts(search, pageable);
            model.addAttribute("searchQuery", search);
        } else if (category != null) {
            pageable = PageRequest.of(page, size);
            blogs = blogService.getBlogPostsByCategory(category, pageable);
            Category categoryObj = categoryService.getCategoryById(category);
            model.addAttribute("selectedCategory", categoryObj);
        } else if (tag != null && !tag.isEmpty()) {
            pageable = PageRequest.of(page, size);
            blogs = blogService.getBlogPostsByTag(tag, pageable);
            model.addAttribute("selectedTag", tag);
        } else {
            blogs = switch (sort) {
                case "oldest" -> {
                    pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
                    yield blogService.getPublishedBlogPosts(pageable);
                }
                case "popular" -> {
                    pageable = PageRequest.of(page, size);
                    yield blogService.getMostPopularBlogPosts(pageable);
                }
                case "comments" -> {
                    pageable = PageRequest.of(page, size);
                    yield blogService.getMostCommentedBlogPosts(pageable);
                }
                default -> {
                    pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                    yield blogService.getPublishedBlogPosts(pageable);
                }
            };
        }
        List<String> blogSummaries = blogs.getContent().stream()
                .map(blog -> blogService.getSafeHtmlSummary(blog.getContent(), 200))
                .collect(Collectors.toList());


        model.addAttribute("blogs", blogs);
        model.addAttribute("blogSummaries", blogSummaries);
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);

        // Add categories
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);

        // Add popular tags
        List<Map<String, Object>> popularTags = blogService.getPopularTags(10);
        model.addAttribute("popularTags", popularTags);

        // Add top bloggers
        List<User> topBloggers = userService.getTopBloggers(4);

        // Create maps to store counts
        Map<Long, Integer> postCounts = new HashMap<>();
        Map<Long, Integer> followerCounts = new HashMap<>();

        // Populate the maps
        for (User blogger : topBloggers) {
            postCounts.put(blogger.getId(), userService.getUserPostCount(blogger.getId()));
            followerCounts.put(blogger.getId(), userService.getUserFollowerCount(blogger.getId()));
        }

        model.addAttribute("topBloggers", topBloggers);
        model.addAttribute("postCounts", postCounts);
        model.addAttribute("followerCounts", followerCounts);

        return "blogs/list";
    }

    @ModelAttribute("currentUrl")
    public String currentUrl(HttpServletRequest request) {
        return request.getRequestURL().toString();
    }

    @GetMapping("/{id}")
    public String getBlogDetail(@PathVariable Long id, Model model, Authentication authentication) {
        try {
            BlogPost blog = blogService.getBlogPostById(id);

            model.addAttribute("blog", blog);

            // Check if current user has liked the post
            boolean hasLiked = false;
            if (authentication != null && authentication.isAuthenticated()) {
                User currentUser = userService.getUserByUsername(authentication.getName());
                hasLiked = blogService.hasUserLikedPost(id, currentUser.getId());
                // Also check if user is following the author
                boolean isFollowing = userService.isFollowing(currentUser.getId(), blog.getAuthor().getId());
                model.addAttribute("isFollowing", isFollowing);
            }
            model.addAttribute("hasLiked", hasLiked);


            // Add related posts
            List<BlogPost> relatedPosts = blogService.getRelatedPosts(id);
            model.addAttribute("relatedPosts", relatedPosts);

            // Add popular tags
            List<Map<String, Object>> popularTags = blogService.getPopularTags(10);
            model.addAttribute("popularTags", popularTags);
            CommentDto commentDto = new CommentDto();
            commentDto.setBlogPostId(id);
            // Prepare comment form
            model.addAttribute("commentDto",commentDto);

            return "blogs/detail";
        }
        catch (EntityNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "error";
        }
    }

    @GetMapping("/slug/{slug}")
    public String getBlogBySlug(@PathVariable String slug, Model model, Authentication authentication) {
        try {
            BlogPost blog = blogService.getBlogPostBySlug(slug);
            return getBlogDetail(blog.getId(), model, authentication);
        }catch(EntityNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "error";
        }
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("blogPost", new BlogPostDto());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "blogs/create";
    }

    @PostMapping("/create")
    public String createBlog(@Valid @ModelAttribute("blogPost") BlogPostDto blogPostDto,
                             BindingResult bindingResult,
                             Authentication authentication,
                             Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllCategories());
            return "blogs/create";
        }

        User user = userService.getUserByUsername(authentication.getName());
        BlogPost createdBlog = blogService.createBlogPost(blogPostDto, user.getId());

        List<User> followers = List.copyOf(userService.getFollowers(user.getId()));
        String message = user.getUsername() + " published a new post: " + createdBlog.getTitle();
        String link = "/blogs/" + createdBlog.getId();

        notificationService.createBatchNotifications(
                followers,
                Notification.NotificationType.POST,
                message,
                link
        );



        return "redirect:/blogs/" + createdBlog.getId();
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, Authentication authentication) {
        BlogPost blog = blogService.getBlogPostById(id);
        String author = blog.getAuthor().getUsername();

        // Check if user is author
        if (!author.equals(authentication.getName())) {
            return "redirect:/blogs/" + id;
        }

        BlogPostDto blogPostDto = new BlogPostDto();
        blogPostDto.setId(blog.getId());
        blogPostDto.setTitle(blog.getTitle());
        blogPostDto.setContent(blog.getContent());
        blogPostDto.setSlug(blog.getSlug());

        if (blog.getCategory() != null) {
            System.out.println(blog.getCategory().getId());
            blogPostDto.setCategoryId(blog.getCategory().getId());
        }

        if (blog.getTags() != null && !blog.getTags().isEmpty()) {
            blogPostDto.setTags(String.join(", ", blog.getTags()));
        }

        blogPostDto.setImageUrl(blog.getImageUrl());
        blogPostDto.setStatus(blog.getStatus().toString());
        blogPostDto.setAllowComments(blog.isAllowComments());
        blogPostDto.setCreatedAt(blog.getCreatedAt());
        blogPostDto.setUpdatedAt(blog.getUpdatedAt());

        model.addAttribute("blogPost", blogPostDto);
        model.addAttribute("author", author);
        model.addAttribute("categories", categoryService.getAllCategories());

        return "blogs/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateBlog(@PathVariable Long id,
                             @Valid @ModelAttribute("blogPost") BlogPostDto blogPostDto,
                             BindingResult bindingResult,
                             Authentication authentication,
                             Model model) {
        BlogPost blog = blogService.getBlogPostById(id);
        System.out.println(" blog "+ blogPostDto.toString());
        System.out.println("tags "+ blogPostDto.getTags());

        // Check if user is author
        if (!blog.getAuthor().getUsername().equals(authentication.getName())) {
            return "redirect:/blogs/" + id;
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllCategories());
            return "blogs/edit";
        }

        blogService.updateBlogPost(id, blogPostDto);
        return "redirect:/blogs/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteBlog(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            blogService.deleteBlogPost(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Blog post deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/tag/{tag}")
    public String getBlogsByTag(@PathVariable String tag,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BlogPost> blogs = blogService.getBlogPostsByTag(tag, pageable);
        List<String> blogSummaries = blogs.getContent().stream()
                .map(blog -> blogService.getSafeHtmlSummary(blog.getContent(), 200))
                .collect(Collectors.toList());


        model.addAttribute("blogs", blogs);
        model.addAttribute("blogSummaries", blogSummaries);
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("selectedTag", tag);

        // Add categories and popular tags
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("popularTags", blogService.getPopularTags(10));

        // Add top bloggers
        List<User> topBloggers = userService.getTopBloggers(5);

        // Create maps to store counts
        Map<Long, Integer> postCounts = new HashMap<>();
        Map<Long, Integer> followerCounts = new HashMap<>();

        // Populate the maps
        for (User blogger : topBloggers) {
            postCounts.put(blogger.getId(), userService.getUserPostCount(blogger.getId()));
            followerCounts.put(blogger.getId(), userService.getUserFollowerCount(blogger.getId()));
        }
        model.addAttribute("topBloggers", topBloggers);
        model.addAttribute("postCounts", postCounts);
        model.addAttribute("followerCounts", followerCounts);


        return "blogs/list";
    }

    @GetMapping("/category/{id}")
    public String getBlogsByCategory(@PathVariable Long id,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Category category = categoryService.getCategoryById(id);
        Page<BlogPost> blogs = blogService.getBlogPostsByCategory(id, pageable);
        List<String> blogSummaries = blogs.getContent().stream()
                .map(blog -> blogService.getSafeHtmlSummary(blog.getContent(), 200))
                .collect(Collectors.toList());


        model.addAttribute("blogs", blogs);
        model.addAttribute("blogSummaries", blogSummaries);
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("selectedCategory", category);

        // Add categories and popular tags
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("popularTags", blogService.getPopularTags(10));

        return "blogs/list";
    }

    @GetMapping("/search")
    public String searchBlogs(@RequestParam String query,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BlogPost> blogs = blogService.searchBlogPosts(query, pageable);
        List<String> blogSummaries = blogs.getContent().stream()
                .map(blog -> blogService.getSafeHtmlSummary(blog.getContent(), 200))
                .collect(Collectors.toList());


        model.addAttribute("blogs", blogs);
        model.addAttribute("blogSummaries", blogSummaries);
        model.addAttribute("query", query);
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);

        // Add popular tags
        model.addAttribute("popularTags", blogService.getPopularTags(10));

        return "blogs/search-results";
    }
}
