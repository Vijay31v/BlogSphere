package org.example.blogsphere.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.blogsphere.dto.BlogPostDto;
import org.example.blogsphere.entity.*;
import org.example.blogsphere.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;


import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class BlogService {
    private final BlogPostRepository blogPostRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final CategoryRepository categoryRepository;
    private final NotificationService notificationService;

    @Autowired
    public BlogService(BlogPostRepository blogPostRepository,
                       UserRepository userRepository,
                       LikeRepository likeRepository,
                       CommentRepository commentRepository,
                       CategoryRepository categoryRepository,
                       NotificationService notificationService) {
        this.blogPostRepository = blogPostRepository;
        this.userRepository = userRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.categoryRepository = categoryRepository;
        this.notificationService = notificationService;
    }

    public BlogPost createBlogPost(BlogPostDto blogPostDto, Long authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new EntityNotFoundException("Author not found"));

        BlogPost blogPost = new BlogPost();
        blogPost.setTitle(blogPostDto.getTitle());
        blogPost.setContent(blogPostDto.getContent());
        blogPost.setAuthor(author);

        // Handle slug
        if (blogPostDto.getSlug() != null && !blogPostDto.getSlug().isEmpty()) {
            blogPost.setSlug(blogPostDto.getSlug());
        } else {
            blogPost.setSlug(generateSlug(blogPostDto.getTitle()));
        }

        // Handle category
        if (blogPostDto.getCategoryId() != null) {
            Category category = categoryRepository.findById(blogPostDto.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found"));
            blogPost.setCategory(category);
        }

        // Handle tags
        if (blogPostDto.getTags() != null && !blogPostDto.getTags().isEmpty()) {
            Set<String> tags = Arrays.stream(blogPostDto.getTags().split(","))
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty())
                    .collect(Collectors.toSet());
            blogPost.setTags(tags);
        }

        // Handle image URL
        if (blogPostDto.getImageUrl() != null && !blogPostDto.getImageUrl().isEmpty()) {
            blogPost.setImageUrl(blogPostDto.getImageUrl());
        } else{
            blogPost.setImageUrl("/img/placeholder.png");
        }


        // Handle status
        if (blogPostDto.getStatus() != null) {
            System.out.println(blogPostDto.getStatus());
            blogPost.setStatus(BlogPost.PostStatus.valueOf(blogPostDto.getStatus()));
        }

        // Handle comments setting
        blogPost.setAllowComments(blogPostDto.isAllowComments());

        return blogPostRepository.save(blogPost);
    }

    public List<BlogPost> getBlogPostsByUser(Long userId) {
        return blogPostRepository.findByAuthorId(userId);
    }

    public BlogPost getBlogPostById(Long id) throws EntityNotFoundException {
        return blogPostRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Blog post not found"));
    }

    public BlogPost getBlogPostBySlug(String slug) throws EntityNotFoundException{
        return blogPostRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Blog post not found"));
    }

    /**
     * Retrieves only published blog posts for a specific user
     */
    public List<BlogPost> getPublishedBlogPostsByUser(Long userId) {
        List<BlogPost> allPosts = blogPostRepository.findByAuthorId(userId);
        return allPosts.stream()
                .filter(post -> post.getStatus() == BlogPost.PostStatus.PUBLISHED)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves only draft blog posts for a specific user
     */
    public List<BlogPost> getDraftBlogPostsByUser(Long userId) {
        List<BlogPost> allPosts = blogPostRepository.findByAuthorId(userId);
        return allPosts.stream()
                .filter(post -> post.getStatus() == BlogPost.PostStatus.DRAFT)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves blog posts sorted by number of comments (most commented first)
     */
    public Page<BlogPost> getMostCommentedBlogPosts(Pageable pageable) {
        return blogPostRepository.findMostCommented(pageable);
    }

    /**
     * Retrieves popular tags with their counts
     */
    public List<Map<String, Object>> getPopularTags(int limit) {
        // Get all blog posts
        List<BlogPost> allPosts = blogPostRepository.findAll();

        // Count occurrences of each tag
        Map<String, Long> tagCounts = allPosts.stream()
                .flatMap(post -> post.getTags().stream())
                .collect(Collectors.groupingBy(
                        tag -> tag,
                        Collectors.counting()
                ));

        // Convert to a list of maps with name and count
        return tagCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> tagMap = new HashMap<>();
                    tagMap.put("name", entry.getKey());
                    tagMap.put("count", entry.getValue());
                    return tagMap;
                })
                .collect(Collectors.toList());
    }


    public void updateBlogPost(Long id, BlogPostDto blogPostDto) {
        BlogPost existingBlog = getBlogPostById(id);
        existingBlog.setTitle(blogPostDto.getTitle());
        existingBlog.setContent(blogPostDto.getContent());

        // Update slug if provided
        if (blogPostDto.getSlug() != null && !blogPostDto.getSlug().isEmpty()) {
            existingBlog.setSlug(blogPostDto.getSlug());
        }

        // Update category if provided
        if (blogPostDto.getCategoryId() != null) {
            Category category = categoryRepository.findById(blogPostDto.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found"));
            existingBlog.setCategory(category);
        }

        // Update tags if provided
        if (blogPostDto.getTags() != null) {
            Set<String> tags = Arrays.stream(blogPostDto.getTags().split(","))
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty())
                    .collect(Collectors.toSet());
            existingBlog.setTags(tags);
        }

        // Update image URL if provided
        if (blogPostDto.getImageUrl() != null|| !blogPostDto.getImageUrl().isEmpty()) {
            existingBlog.setImageUrl(blogPostDto.getImageUrl());
        }

        // Update status if provided
        if (blogPostDto.getStatus() != null) {
            existingBlog.setStatus(BlogPost.PostStatus.valueOf(blogPostDto.getStatus()));
        }

        // Update comments setting
        existingBlog.setAllowComments(blogPostDto.isAllowComments());

        // Set update time
        existingBlog.setUpdatedAt(LocalDateTime.now());

        blogPostRepository.save(existingBlog);
    }

    public void deleteBlogPost(Long id, String username) {
        BlogPost blogPost = getBlogPostById(id);

        // Check if user is author or admin
        if (!blogPost.getAuthor().getUsername().equals(username) &&
                !userRepository.findByUsername(username).get().getRole().equals(Role.ROLE_ADMIN)) {
            throw new SecurityException("You are not authorized to delete this post");
        }

        blogPostRepository.delete(blogPost);
    }

    public void addCommentToBlog(Long blogPostId, Long userId, String commentText) {
        BlogPost blogPost = blogPostRepository.findById(blogPostId)
                .orElseThrow(() -> new EntityNotFoundException("Blog post not found"));

        // Check if comments are allowed
        if (!blogPost.isAllowComments()) {
            throw new IllegalStateException("Comments are disabled for this post");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Comment comment = new Comment();
        comment.setBlogPost(blogPost);
        comment.setUser(user);
        comment.setCommentText(commentText);
        comment.setCreatedAt(LocalDateTime.now());

        commentRepository.save(comment);

        // Create notification for blog author if it's not the same user
        if (!blogPost.getAuthor().getId().equals(userId)) {
            notificationService.createNotification(
                    blogPost.getAuthor(),
                    Notification.NotificationType.COMMENT,
                    user.getUsername() + " commented on your post: " + blogPost.getTitle(),
                    "/blogs/" + blogPost.getId()
            );
        }
    }
    public String getPlainTextSummary(String html, int maxLength) {
        String text = Jsoup.parse(html).text();
        if (text.length() > maxLength) {
            return text.substring(0, maxLength) + "...";
        }
        return text;
    }

    public String getSafeHtmlSummary(String html, int maxLength) {
        String safeHtml = Jsoup.clean(html, Safelist.basic());
        if (safeHtml.length() > maxLength) {
            return safeHtml.substring(0, maxLength) + "...";
        }
        return safeHtml;
    }


    public Page<BlogPost> getAllBlogPosts(Pageable pageable) {
        return blogPostRepository.findAll(pageable);
    }

    public Long getBlogPostCount() {
        return blogPostRepository.count();
    }

    public Page<BlogPost> getPublishedBlogPosts(Pageable pageable) {
        return blogPostRepository.findPublished(pageable);
    }

    public Page<BlogPost> getLatestPublishedBlogPosts(Pageable pageable){
        return blogPostRepository.findLatestPublished(pageable);
    }

    public Page<BlogPost> getMostPopularBlogPosts(Pageable pageable) {
        return blogPostRepository.findMostPopular(pageable);
    }


    public Page<BlogPost> getBlogPostsByCategory(Long categoryId, Pageable pageable) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
        return blogPostRepository.findByCategory(category, pageable);
    }

    public Page<BlogPost> getBlogPostsByTag(String tag, Pageable pageable) {
        return blogPostRepository.findByTagsContaining(tag, pageable);
    }

    public Page<BlogPost> searchBlogPosts(String keyword, Pageable pageable) {
        return blogPostRepository.search(keyword, pageable);
    }

    public boolean hasUserLikedPost(Long blogPostId, Long userId) {
        return likeRepository.existsByBlogPostIdAndUserId(blogPostId, userId);
    }

    public List<BlogPost> getRelatedPosts(Long blogPostId) {
        BlogPost blogPost = getBlogPostById(blogPostId);
        Set<String> tags = blogPost.getTags();

        if (tags.isEmpty() && blogPost.getCategory() == null) {
            // No tags or category, return recent posts
            return blogPostRepository.findLatestPublished(PageRequest.of(0, 3))
                    .stream().filter(post -> !post.getId().equals(blogPostId))
                    .collect(Collectors.toList());
        }

        // First try to find posts with the same category
        if (blogPost.getCategory() != null) {
            List<BlogPost> categoryPosts = blogPostRepository.findByCategory(
                            blogPost.getCategory(), PageRequest.of(0, 5))
                    .stream().filter(post -> !post.getId().equals(blogPostId))
                    .collect(Collectors.toList());

            if (categoryPosts.size() >= 3) {
                return categoryPosts.subList(0, 3);
            }
        }

        // If not enough category posts, look for tag matches
        Set<BlogPost> relatedPosts = new HashSet<>();

        if (!tags.isEmpty()) {
            for (String tag : tags) {
                if (relatedPosts.size() >= 3) break;

                List<BlogPost> tagPosts = blogPostRepository.findByTagsContaining(
                                tag, PageRequest.of(0, 5))
                        .stream().filter(post -> !post.getId().equals(blogPostId))
                        .toList();

                relatedPosts.addAll(tagPosts);
            }
        }

        if (relatedPosts.size() >= 3) {
            return new ArrayList<>(relatedPosts).subList(0, 3);
        }

        // If still not enough, add some recent posts
        List<BlogPost> recentPosts = blogPostRepository.findLatestPublished(PageRequest.of(0, 5))
                .stream().filter(post -> !post.getId().equals(blogPostId) && !relatedPosts.contains(post))
                .toList();

        relatedPosts.addAll(recentPosts);

        return new ArrayList<>(relatedPosts).subList(0, Math.min(3, relatedPosts.size()));
    }

    // Helper method to generate a slug from a title
    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "") // Remove non-alphanumeric chars except spaces and hyphens
                .replaceAll("\\s+", "-")         // Replace spaces with hyphens
                .replaceAll("-+", "-")           // Replace multiple hyphens with a single one
                .trim();
    }

    public Map<String, Object> getBlogStats(int year, int month) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", blogPostRepository.countByCreatedAtBetween(
                LocalDateTime.of(year, month, 1, 0, 0),
                LocalDateTime.of(year, month, 1, 0, 0).plusMonths(1)
        ));
        stats.put("published", blogPostRepository.countPublishedPosts());
        stats.put("drafts", blogPostRepository.countDraftPosts());
        return stats;
    }

}
