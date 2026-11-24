package com.team5.auction.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/** Swagger UI: http://localhost:8080/swagger-ui.html */
@Configuration
public class SwaggerConfig {

	private static final String BEARER = "bearerAuth";

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
			.info(new Info().title("경매장 API (5팀)").version("v1"))
			.components(new Components().addSecuritySchemes(BEARER,
				new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
			.addSecurityItem(new SecurityRequirement().addList(BEARER));
	}
}
