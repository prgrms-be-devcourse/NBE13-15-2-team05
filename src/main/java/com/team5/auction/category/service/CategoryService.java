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
@Transactional(readOnly = true) //


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

    /* 운영자 카테고리 등록  같은 이름 있으면 CATEGORY_NAME_DUPLICAATED */
    @Transactional
    public CategoryResponse create(String name) {
        if (categoryRepository.existsByName(name)) {
            throw new BusinessException(CategoryErrorCode.CATEGORY_NAME_DUPLICATED);

        }
        Category category = categoryRepository.save(Category.create(name));
        return CategoryResponse.from(category);
    }

    /* 운영자 카테고리 이름 수정 다른 카테고리가 쓰는 이름이면 CATEGORY_NAME_DUPLICATED */

    @Transactional
    public CategoryResponse update(Long id, String name) {
        Category category = getById(id);
        if (categoryRepository.existsByNameAndIdNot(name, id)) {
            throw new BusinessException(CategoryErrorCode.CATEGORY_NAME_DUPLICATED);
        }
        category.rename(name);
        return CategoryResponse.from(category);
    }
}
