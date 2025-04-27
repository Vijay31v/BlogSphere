package org.example.blogsphere.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.blogsphere.entity.Category;
import org.example.blogsphere.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryService {
    private final CategoryRepository categoryRepository;

    @Autowired
    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
    }

    public Category getCategoryByName(String name) {
        return categoryRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
    }

    public Category createCategory(String name, String description, String icon) {
        if (categoryRepository.existsByName(name)) {
            throw new IllegalArgumentException("Category with this name already exists");
        }

        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setIcon(icon);

        return categoryRepository.save(category);
    }

    public Category updateCategory(Long id, String name, String description, String icon) {
        Category category = getCategoryById(id);

        // Check if name is being changed and if new name is already taken
        if (!category.getName().equals(name) && categoryRepository.existsByName(name)) {
            throw new IllegalArgumentException("Category with this name already exists");
        }

        category.setName(name);
        category.setDescription(description);
        category.setIcon(icon);

        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        Category category = getCategoryById(id);

        // Check if category has blog posts
        if (!category.getBlogPosts().isEmpty()) {
            throw new IllegalStateException("Cannot delete category with associated blog posts");
        }

        categoryRepository.delete(category);
    }
}
