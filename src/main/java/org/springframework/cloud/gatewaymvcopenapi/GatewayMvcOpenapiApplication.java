package org.springframework.cloud.gatewaymvcopenapi;

import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.webmvc.core.fn.SpringdocRouteBuilder.route;
import static org.springframework.cloud.gateway.server.mvc.filter.AfterFilterFunctions.addResponseHeader;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.routeId;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.setPath;
import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.version;

@SpringBootApplication
public class GatewayMvcOpenapiApplication {

	@Autowired
	LoadBalancerClient lbClient;

	// http :8080/v3/api-docs

	// http :8080/users X-API-Version:2.0
	// http :8080/users X-API-Version:1.0
	// http :8080/users X-API-Version:0.9
	@Bean
	public RouterFunction<ServerResponse> gatewayRouterFunction() {
		// @formatter:off
		return route()
			.GET("/userdocs", http(), ops -> ops.operationId("getUserDocs"))
				.before(unary(routeId("userapidocs")))
				.before(unary(setPath("/v3/api-docs")))
				.filter(lb("users"))
				.build().and(route()
			.GET("/users", version("2.0"), http(),
					ops -> ops.operationId("usersv2").response(responseBuilder()
							.ref(getRef("/paths/~1api~12.0~1users/get/responses"))))
				.before(unary(routeId("usersv2")))
				.before(unary(setPath("/api/2.0/users")))
				.filter(lb("users"))
				.after(addResponseHeader("X-Version", "2.0"))
				.build().and(route()
			.GET("/users", version("1.0"), http(),
					ops -> ops.operationId("usersv1").response(responseBuilder()
							.ref(getRef("/paths/~1api~1v1.0~1users/get/responses"))))
				.before(unary(routeId("usersv1")))
				.before(unary(setPath("/api/v1.0/users")))
				.filter(lb("users"))
				.after(addResponseHeader("X-Version", "1.0"))
				.build()));
		// @formatter:on
	}

	private String getRef(String pathsegment) {
		ServiceInstance instance = lbClient.choose("users");
		Map<String, String> metadata = instance.getMetadata();
		String path = metadata.get("response-ref-path");
		String ref = UriComponentsBuilder.fromUriString("http://localhost:8080").path(path + pathsegment).build().toString();
		return ref;
	}

	static UnaryOperator<ServerRequest> unary(Function<ServerRequest, ServerRequest> function) {
		return function::apply;
	}

	public static void main(String[] args) {
		SpringApplication.run(GatewayMvcOpenapiApplication.class, args);
	}

}
