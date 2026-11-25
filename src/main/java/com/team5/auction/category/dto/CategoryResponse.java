package com.team5.auction.category.dto;
import com.team5.auction.category.domain.Category;


public record CategoryResponse(Long id, String name) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
