package com.team5.auction.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.team5.auction.common.response.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;

/** 스프링 MVC 표준 예외가 500이 아니라 맞는 상태 코드와 ApiResponse 형식으로 나가는지 */
class GlobalExceptionHandlerTest {

	enum Role { BUYER, SELLER }

	record Body(@NotBlank String name) {
	}

	// @Validated를 붙이면 스프링 기본 메서드 검증이 꺼지고 AOP 검증(ConstraintViolationException)으로 바뀌므로 붙이지 않는다
	@RestController
	static class TestController {

		@GetMapping("/test/enum")
		ApiResponse<Void> enumParam(@RequestParam Role role) {
			return ApiResponse.ok();
		}

		@GetMapping("/test/items/{id}")
		ApiResponse<Void> pathVariable(@PathVariable Long id) {
			return ApiResponse.ok();
		}

		@GetMapping("/test/size")
		ApiResponse<Void> validatedParam(@RequestParam @Max(100) int size) {
			return ApiResponse.ok();
		}

		@PostMapping("/test/body")
		ApiResponse<Void> body(@Valid @RequestBody Body body) {
			return ApiResponse.ok();
		}

		@GetMapping("/test/business")
		ApiResponse<Void> business() {
			throw new BusinessException(CommonErrorCode.FORBIDDEN);
		}

		@GetMapping("/test/optimistic-lock")
		ApiResponse<Void> optimisticLock() {
			throw new ObjectOptimisticLockingFailureException(Object.class, 1L);
		}

		@GetMapping("/test/lock-timeout")
		ApiResponse<Void> lockTimeout() {
			throw new CannotAcquireLockException("lock wait timeout");
		}

		@GetMapping("/test/unexpected")
		ApiResponse<Void> unexpected() {
			throw new IllegalStateException("boom");
		}
	}

	/** 컨트롤러에 @Validated를 붙인 경우. 검증 실패가 ConstraintViolationException으로 나온다 */
	@RestController
	@Validated
	static class ValidatedController {

		@GetMapping("/validated/size")
		ApiResponse<Void> validatedParam(@RequestParam @Max(100) int size) {
			return ApiResponse.ok();
		}
	}

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		// @RequestParam 검증(메서드 검증)은 검증기를 넣어야 켜진다
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		// 실제 앱에서는 MethodValidationPostProcessor가 @Validated 클래스를 프록시로 감싼다. 테스트에서는 직접 감싼다
		ProxyFactory proxyFactory = new ProxyFactory(new ValidatedController());
		proxyFactory.setProxyTargetClass(true);
		proxyFactory.addAdvice(new MethodValidationInterceptor((Validator) validator));
		mockMvc = MockMvcBuilders.standaloneSetup(new TestController(), proxyFactory.getProxy())
			.setControllerAdvice(new GlobalExceptionHandler())
			.setValidator(validator)
			.build();
	}

	@Test
	void enum에_없는_값이면_400_INVALID_INPUT과_가능한_값을_알려준다() throws Exception {
		mockMvc.perform(get("/test/enum").param("role", "abc"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.error.fieldErrors[0].field").value("role"))
			.andExpect(jsonPath("$.error.fieldErrors[0].reason").value("허용되지 않는 값입니다. 가능한 값: BUYER, SELLER"));
	}

	@Test
	void enum_소문자도_변환에_실패해_400() throws Exception {
		mockMvc.perform(get("/test/enum").param("role", "buyer"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
	}

	@Test
	void 필수_파라미터가_없으면_400_INVALID_INPUT() throws Exception {
		mockMvc.perform(get("/test/enum"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.error.fieldErrors[0].field").value("role"));
	}

	@Test
	void 경로의_숫자_자리에_문자가_오면_400_INVALID_INPUT() throws Exception {
		mockMvc.perform(get("/test/items/abc"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.error.fieldErrors[0].field").value("id"))
			.andExpect(jsonPath("$.error.fieldErrors[0].reason").value("형식이 올바르지 않습니다."));
	}

	@Test
	void RequestParam_검증에_실패하면_400_INVALID_INPUT() throws Exception {
		mockMvc.perform(get("/test/size").param("size", "1000"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.error.fieldErrors[0].field").value("size"));
	}

	@Test
	void Validated_컨트롤러의_검증_실패도_400_INVALID_INPUT() throws Exception {
		mockMvc.perform(get("/validated/size").param("size", "1000"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.error.fieldErrors[0].field").value("size"));
	}

	@Test
	void Valid_실패는_기존처럼_400_INVALID_INPUT과_필드별_오류() throws Exception {
		mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.error.fieldErrors[0].field").value("name"));
	}

	@Test
	void 잘못된_JSON은_기존처럼_400_INVALID_INPUT() throws Exception {
		mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{not json"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
	}

	@Test
	void 지원하지_않는_메서드면_405_METHOD_NOT_ALLOWED() throws Exception {
		mockMvc.perform(post("/test/enum"))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
	}

	@Test
	void 지원하지_않는_Content_Type이면_415_UNSUPPORTED_MEDIA_TYPE() throws Exception {
		mockMvc.perform(post("/test/body").contentType(MediaType.TEXT_PLAIN).content("hello"))
			.andExpect(status().isUnsupportedMediaType())
			.andExpect(jsonPath("$.error.code").value("UNSUPPORTED_MEDIA_TYPE"));
	}

	@Test
	void 없는_URL이면_404_NOT_FOUND() throws Exception {
		mockMvc.perform(get("/test/nothing"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
	}

	@Test
	void BusinessException은_기존처럼_에러코드의_상태로_응답() throws Exception {
		mockMvc.perform(get("/test/business"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void 낙관적_락_충돌이면_409_CONCURRENT_UPDATE() throws Exception {
		mockMvc.perform(get("/test/optimistic-lock"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("CONCURRENT_UPDATE"));
	}

	@Test
	void 비관적_락_대기_시간이_초과되면_409_CONCURRENT_UPDATE() throws Exception {
		mockMvc.perform(get("/test/lock-timeout"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("CONCURRENT_UPDATE"));
	}

	@Test
	void 예상_못_한_예외는_기존처럼_500_INTERNAL_ERROR() throws Exception {
		mockMvc.perform(get("/test/unexpected"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"));
	}
}
