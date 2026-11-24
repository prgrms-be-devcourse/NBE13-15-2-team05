package com.team5.auction.common.response;

import java.util.List;

import org.springframework.data.domain.Page;

/** 목록 응답. 스프링 Page를 그대로 내보내지 않고 이 형식으로 감쌉니다. */
public record PageResponse<T>(List<T> content, int page, int size,
							  long totalElements, int totalPages, boolean hasNext) {

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
			page.getTotalElements(), page.getTotalPages(), page.hasNext());
	}
}
