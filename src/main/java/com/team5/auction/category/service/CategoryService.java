package com.team5.auction.category.service;

import com.team5.auction.category.domain.Category;
import com.team5.auction.category.dto.CategoryResponse;
import com.team5.auction.category.exception.CategoryErrorCode;
import com.team5.auction.category.repository.CategoryRepository;
import com.team5.auction.common.exception.BusinessException;
import lombok.RequiredArgsConstructor ;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List ;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(CategoryErrorCode.CATEGORY_NOT_FOUND));
    }

    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll(Sort.by("id")).stream()
                .map(CategoryResponse::from)
                .toList();

    }

    /* 운영자 카테고리 등록같은 이름 있으면 CATEGORY_NAME_DUPLICATED */
    @Transactional
    public CategoryResponse create(String name) {
        String trimmed = name.strip();
        if (categoryRepository.existsByName(trimmed)) {
            throw new BusinessException(CategoryErrorCode.CATEGORY_NAME_DUPLICATED);
        }
        Category category = categoryRepository.save(Category.create(trimmed));
        return CategoryResponse.from(category);
    }

    /* 운영자 카테고리 이름 수정. 이름 앞뒤 공백을 지우고 다른 카테고리가 쓰는 이름이면 CATEGORY_NAME_DUPLICATED */
    @Transactional
    public CategoryResponse update(Long id, String name) {
        String trimmed = name.strip();
        Category category = getById(id);
        if (categoryRepository.existsByNameAndIdNot(trimmed, id)) {
            throw new BusinessException(CategoryErrorCode.CATEGORY_NAME_DUPLICATED);
        }
        category.rename(trimmed);
        return CategoryResponse.from(category);
    }
}
