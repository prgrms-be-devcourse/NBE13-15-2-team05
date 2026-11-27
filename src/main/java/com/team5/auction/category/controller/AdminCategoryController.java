package com.team5.auction.category.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.team5.auction.category.dto.CategoryRequest;
import com.team5.auction.category.dto.CategoryResponse;
import com.team5.auction.category.service.CategoryService;
import com.team5.auction.common.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "운영자 - 카테고리")
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor

public class AdminCategoryController {
    private final CategoryService categoryService;
    @Operation(summary = "카테고리 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(categoryService.create(request.name()));
    }

    @Operation(summary = "카테고리 이름 수정")
    @PatchMapping("/{id}")
    public ApiResponse<CategoryResponse> updateCategory(@PathVariable Long id,
                                                        @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(categoryService.update(id, request.name()));
    }

    @Operation(summary = "카테고리 삭제", description = "경매가 하나라도 달린 카테고리는 삭제할 수 없습니다. (R-64)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.ok();
    }
}

