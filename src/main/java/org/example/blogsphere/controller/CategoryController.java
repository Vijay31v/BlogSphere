package org.example.blogsphere.controller;

import jakarta.validation.Valid;
import org.example.blogsphere.dto.CategoryDto;
import org.example.blogsphere.entity.Category;
import org.example.blogsphere.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class CategoryController {

    private final CategoryService categoryService;

    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String listCategories(Model model) {
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);
        model.addAttribute("categoryDto", new CategoryDto());
        return "admin/categories";
    }

    @PostMapping
    public String createCategory(@Valid @ModelAttribute("categoryDto") CategoryDto categoryDto,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllCategories());
            return "admin/categories";
        }

        try {
            categoryService.createCategory(categoryDto.getName(), categoryDto.getDescription(), categoryDto.getIcon());
            redirectAttributes.addFlashAttribute("successMessage", "Category created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/categories";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Category category = categoryService.getCategoryById(id);

        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setId(category.getId());
        categoryDto.setName(category.getName());
        categoryDto.setDescription(category.getDescription());
        categoryDto.setIcon(category.getIcon());

        model.addAttribute("categoryDto", categoryDto);
        return "admin/edit-category";
    }

    @PostMapping("/{id}/edit")
    public String updateCategory(@PathVariable Long id,
                                 @Valid @ModelAttribute("categoryDto") CategoryDto categoryDto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/edit-category";
        }

        try {
            categoryService.updateCategory(id, categoryDto.getName(), categoryDto.getDescription(), categoryDto.getIcon());
            redirectAttributes.addFlashAttribute("successMessage", "Category updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("successMessage", "Category deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/categories";
    }
}
