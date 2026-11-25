package com.team5.auction.category.service;

import com.team5.auction.category.domain.Category;
import com.team5.auction.category.dto.CategoryResponse;
import com.team5.auction.category.exception.CategoryErrorCode;
import com.team5.auction.category.repository.CategoryRepository;
import com.team5.auction.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {



    @Mock
    CategoryRepository categoryRepository;

    @InjectMocks
    CategoryService categoryService;

    @Test
    void 목록은_id순으로_id와_이름을_돌려준다() {
        // given: 저장소가 id순으로 카테고리 2개를 돌려준다고 정해 둔다
        given(categoryRepository.findAll(Sort.by("id")))
                .willReturn(List.of(category(1L, "전자기기"), category(2L, "도서")));

        // when
        List<CategoryResponse> result = categoryService.getAll();

        // then
        assertThat(result).containsExactly(
                new CategoryResponse(1L, "전자기기"),
                new CategoryResponse(2L, "도서"));
    }

    @Test
    void 있는_id면_카테고리를_돌려준다() {
        Category category = category(1L, "전자기기");
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));

        Category result = categoryService.getById(1L);

        assertThat(result).isSameAs(category);
    }

    @Test
    void 없는_id면_CATEGORY_NOT_FOUND를_던진다() {
        given(categoryRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CategoryErrorCode.CATEGORY_NOT_FOUND);
    }

    /** 테스트용 카테고리. id는 원래 DB가 채우는 값이라 직접 넣어 준다. */
    private Category category(Long id, String name) {
        Category category = Category.create(name);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }
}
