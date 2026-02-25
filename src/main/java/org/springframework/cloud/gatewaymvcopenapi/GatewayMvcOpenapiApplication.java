package org.springframework.cloud.gatewaymvcopenapi;

import java.util.function.Function;
import java.util.function.UnaryOperator;

import io.swagger.v3.oas.annotations.enums.ParameterIn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;
import static org.springdoc.webmvc.core.fn.SpringdocRouteBuilder.route;
import static org.springframework.cloud.gateway.server.mvc.filter.AfterFilterFunctions.addResponseHeader;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.routeId;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;
import static org.springframework.web.servlet.function.RequestPredicates.version;

@SpringBootApplication
public class GatewayMvcOpenapiApplication {

	// http :8080/anything/versioned X-API-Version:2.0.1
	// http :8080/anything/versioned X-API-Version:1.0.0
	// http :8080/anything/versioned X-API-Version:0.9.0
	@Bean
	public RouterFunction<ServerResponse> version20() {
		// @formatter:off
		return route()
				.GET(path("/anything/versioned").and(version("2.0+")), http(),
						ops -> ops.operationId("versioned20")
							.parameter(parameterBuilder().in(ParameterIn.HEADER).name("X-Version")
								.schema(schemaBuilder().type("string").defaultValue("1.0").allowableValues(new String[]{"1.0", "2.0"}))))
				.before(unary(routeId("versionroute20")))
				.before(unary(new HttpbinUriResolver()))
				.after(addResponseHeader("X-Version", "2.0+"))
				.build();
		// @formatter:on
	}
	@Bean
	public RouterFunction<ServerResponse> version10() {
		// @formatter:off
		return route()
				.GET(path("/anything/versioned").and(version("1.0+")), http(),
						ops -> ops.operationId("versioned10"))
				.before(unary(routeId("versionroute10")))
				.before(unary(new HttpbinUriResolver()))
				.after(addResponseHeader("X-Version", "1.0+"))
				.build();
		// @formatter:on
	}

	static UnaryOperator<ServerRequest> unary(Function<ServerRequest, ServerRequest> function) {
		return function::apply;
	}

	public static void main(String[] args) {
		SpringApplication.run(GatewayMvcOpenapiApplication.class, args);
	}

}
