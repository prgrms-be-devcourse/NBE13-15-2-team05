package com.team5.auction.category.service;
import com.team5.auction.category.domain.Category;
import com.team5.auction.category.exception.CategoryErrorCode;
import com.team5.auction.category.repository.CategoryRepository;
import com.team5.auction.common.exception.BusinessException;
import lombok.RequiredArgsConstructor ;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List ;
import com.team5.auction.category.dto.CategoryResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) //


public class CategoryService {

    private final CategoryRepository categoryRepository ;

    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(CategoryErrorCode.CATEGORY_NOT_FOUND)) ;
    }
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll(Sort.by("id")).stream()
                .map(CategoryResponse::from)
                .toList();

    }
}
