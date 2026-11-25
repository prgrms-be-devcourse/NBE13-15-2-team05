package com.team5.auction.category.controller;

import java.util.List ;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController ;
import com.team5.auction.category.dto.CategoryResponse;
import com.team5.auction.category.service.CategoryService;
import com.team5.auction.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor

public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<CategoryResponse>> getCategories(){
        return ApiResponse.ok(categoryService.getAll());
    }
}
