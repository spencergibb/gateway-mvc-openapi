package org.springframework.cloud.gatewaymvcopenapi;

import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

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
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.version;

@SpringBootApplication
public class GatewayMvcOpenapiApplication {

	// http :8080/v3/api-docs

	// http :8080/anything/versioned X-API-Version:2.0.1
	// http :8080/anything/versioned X-API-Version:1.0.0
	// http :8080/anything/versioned X-API-Version:0.9.0
	@Bean
	public RouterFunction<ServerResponse> version20(LoadBalancerClient lbClient) {
		// @formatter:off
		return route()
				.GET("/anything/versioned", version("2.0+"), http(),
						ops -> ops.operationId("versioned").response(responseBuilder().ref(getRef(lbClient))))
				.before(unary(routeId("versionroute20")))
				.before(unary(new HttpbinUriResolver()))
				// .filter(lb("httpbin"))
				.after(addResponseHeader("X-Version", "2.0+"))
			.GET("/anything/versioned", version("1.0+"), http(),
					ops -> ops.operationId("versioned").response(responseBuilder().ref(getRef(lbClient))))
				.before(unary(routeId("versionroute10")))
				.before(unary(new HttpbinUriResolver()))
				.after(addResponseHeader("X-Version", "1.0+"))
				.build();
		// @formatter:on
	}

	private static String getRef(LoadBalancerClient lbClient) {
		ServiceInstance httpbin = lbClient.choose("httpbin");
		Map<String, String> metadata = httpbin.getMetadata();
		String path = metadata.get("response-ref-path");
		String ref = UriComponentsBuilder.fromUri(httpbin.getUri()).path(path + "/get/responses").build().toString();
		return ref;
	}

	static UnaryOperator<ServerRequest> unary(Function<ServerRequest, ServerRequest> function) {
		return function::apply;
	}

	public static void main(String[] args) {
		SpringApplication.run(GatewayMvcOpenapiApplication.class, args);
	}

}
