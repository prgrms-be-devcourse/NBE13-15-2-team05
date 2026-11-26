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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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

    @Test
    void 등록하면_저장하고_id와_이름을_돌려준다() {
        given(categoryRepository.existsByName("전자기기")).willReturn(false);
        given(categoryRepository.save(any(Category.class))).willReturn(category(1L, "전자기기"));

        CategoryResponse result = categoryService.create("전자기기");

        assertThat(result).isEqualTo(new CategoryResponse(1L, "전자기기"));
    }

    @Test
    void 같은_이름이_있으면_CATEGORY_NAME_DUPLICATED를_던지고_저장하지_않는다() {
        given(categoryRepository.existsByName("전자기기")).willReturn(true);

        assertThatThrownBy(() -> categoryService.create("전자기기"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CategoryErrorCode.CATEGORY_NAME_DUPLICATED);
        then(categoryRepository).should(never()).save(any());
    }
    @Test
    void 수정하면_이름이_바뀐다() {
        Category category = category(1L, "전자기기");
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(categoryRepository.existsByNameAndIdNot("가전", 1L)).willReturn(false);

        CategoryResponse result = categoryService.update(1L, "가전");

        assertThat(result).isEqualTo(new CategoryResponse(1L, "가전"));
        assertThat(category.getName()).isEqualTo("가전");
    }
    @Test
    void 다른_카테고리가_쓰는_이름이면_CATEGORY_NAME_DUPLICATED를_던지고_이름은_그대로다() {
        Category category = category(1L, "전자기기");
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(categoryRepository.existsByNameAndIdNot("도서", 1L)).willReturn(true);

        assertThatThrownBy(() -> categoryService.update(1L, "도서"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CategoryErrorCode.CATEGORY_NAME_DUPLICATED);
        assertThat(category.getName()).isEqualTo("전자기기");
    }
    @Test
    void 없는_카테고리를_수정하면_CATEGORY_NOT_FOUND를_던진다() {
        given(categoryRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.update(99L, "가전"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CategoryErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    void 경매가_없는_카테고리는_삭제된다() {
        Category category = category(1L, "전자기기");
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(categoryRepository.countAuctionsByCategoryId(1L)).willReturn(0L);

        categoryService.delete(1L);

        then(categoryRepository).should().delete(category);
    }

    @Test
    void 없는_카테고리를_삭제하면_CATEGORY_NOT_FOUND를_던진다() {
        given(categoryRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CategoryErrorCode.CATEGORY_NOT_FOUND);
        then(categoryRepository).should(never()).delete(any());
    }

    @Test
    void 경매가_달린_카테고리를_삭제하면_CATEGORY_IN_USE를_던지고_지우지_않는다() {
        Category category = category(1L, "전자기기");
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(categoryRepository.countAuctionsByCategoryId(1L)).willReturn(1L);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CategoryErrorCode.CATEGORY_IN_USE);
        then(categoryRepository).should(never()).delete(any());
    }
}
