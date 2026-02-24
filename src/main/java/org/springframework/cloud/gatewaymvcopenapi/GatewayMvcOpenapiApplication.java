package org.springframework.cloud.gatewaymvcopenapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.AfterFilterFunctions.addResponseHeader;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.version;

@SpringBootApplication
public class GatewayMvcOpenapiApplication {

	// http :8080/anything/versioned X-API-Version:2.0.1
	// http :8080/anything/versioned X-API-Version:1.0.0
	// http :8080/anything/versioned X-API-Version:0.9.0
	@Bean
	public RouterFunction<ServerResponse> gatewayRouterFunctionVersions() {
		// @formatter:off
		return route("versionroute20")
				.GET(path("/anything/versioned").and(version("2.0+")), http())
				.before(new HttpbinUriResolver())
				.after(addResponseHeader("X-Version", "2.0+"))
				.build()
				.and(route("versionroute10")
						.GET(path("/anything/versioned").and(version("1.0+")), http())
						.before(new HttpbinUriResolver())
						.after(addResponseHeader("X-Version", "1.0+"))
						.build());
		// @formatter:on
	}

	public static void main(String[] args) {
		SpringApplication.run(GatewayMvcOpenapiApplication.class, args);
	}

}
