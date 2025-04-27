/**
 * BlogSphere - Main JavaScript
 * Contains common functionality across the site
 */

document.addEventListener('DOMContentLoaded', function() {
    // Initialize Bootstrap tooltips
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function(tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });

    // Initialize Bootstrap popovers
    const popoverTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="popover"]'));
    popoverTriggerList.map(function(popoverTriggerEl) {
        return new bootstrap.Popover(popoverTriggerEl);
    });

    // Copy link functionality
    document.querySelectorAll('.copy-link').forEach(button => {
        button.addEventListener('click', function() {
            const url = this.getAttribute('data-url');
            navigator.clipboard.writeText(url).then(() => {
                // Change button text temporarily to show success
                const originalText = this.innerHTML;
                this.innerHTML = '<i class="fas fa-check"></i> Copied!';
                setTimeout(() => {
                    this.innerHTML = originalText;
                }, 2000);
            }, (err) => {
                console.error('Could not copy text: ', err);
            });
        });
    });

    // Comment deletion confirmation
    document.querySelectorAll('.delete-comment-btn').forEach(button => {
        button.addEventListener('click', function(e) {
            if (!confirm('Are you sure you want to delete this comment?')) {
                e.preventDefault();
            }
        });
    });

    // Blog post deletion confirmation
    document.querySelectorAll('.delete-post-btn').forEach(button => {
        button.addEventListener('click', function(e) {
            if (!confirm('Are you sure you want to delete this post? This action cannot be undone.')) {
                e.preventDefault();
            }
        });
    });

    // Mobile navigation menu toggle
    const navbarToggler = document.querySelector('.navbar-toggler');
    if (navbarToggler) {
        navbarToggler.addEventListener('click', function() {
            document.querySelector('.navbar-collapse').classList.toggle('show');
        });
    }

    // Handle alert dismissal
    document.querySelectorAll('.alert .btn-close').forEach(button => {
        button.addEventListener('click', function() {
            this.parentElement.style.display = 'none';
        });
    });

    // Auto-hide alerts after 5 seconds
    setTimeout(function() {
        document.querySelectorAll('.alert:not(.alert-permanent)').forEach(alert => {
            // Create and trigger a fade-out effect
            alert.style.transition = 'opacity 1s';
            alert.style.opacity = '0';

            // Remove after fade completes
            setTimeout(function() {
                if (alert.parentElement) {
                    alert.parentElement.removeChild(alert);
                }
            }, 1000);
        });
    }, 5000);

    // Handle like/unlike toggle without page refresh
    document.querySelectorAll('.like-toggle-btn').forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault();

            const postId = this.getAttribute('data-post-id');
            const isLiked = this.getAttribute('data-is-liked') === 'true';
            const likeCountEl = document.getElementById('like-count-' + postId);
            const likeUrl = isLiked ? `/blogs/${postId}/unlike` : `/blogs/${postId}/like`;

            fetch(likeUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').getAttribute('content')
                }
            })
                .then(response => {
                    if (response.ok) {
                        return response.json();
                    }
                    throw new Error('Network response was not ok');
                })
                .then(data => {
                    // Update like count
                    if (likeCountEl) {
                        likeCountEl.textContent = data.likeCount;
                    }

                    // Toggle button state
                    this.setAttribute('data-is-liked', !isLiked);

                    if (isLiked) {
                        this.innerHTML = '<i class="far fa-heart"></i> Like';
                        this.classList.replace('btn-danger', 'btn-outline-danger');
                    } else {
                        this.innerHTML = '<i class="fas fa-heart"></i> Liked';
                        this.classList.replace('btn-outline-danger', 'btn-danger');
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                });
        });
    });

    // Image lazy loading with fade-in effect
    document.querySelectorAll('img[loading="lazy"]').forEach(img => {
        img.style.opacity = 0;
        img.style.transition = 'opacity 0.5s';
        img.addEventListener('load', function() {
            img.style.opacity = 1;
        });
    });
});
