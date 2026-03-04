package org.springframework.cloud.gatewaymvcopenapi;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.annotations.StringToClassMapItem;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.fn.builders.apiresponse.Builder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.ProxyExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.arrayschema.Builder.arraySchemaBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;
import static org.springdoc.webmvc.core.fn.SpringdocRouteBuilder.route;
import static org.springframework.cloud.gateway.server.mvc.filter.AfterFilterFunctions.addResponseHeader;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.routeId;
import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;
//import static org.springframework.web.servlet.function.RequestPredicates.version;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.version;

@SpringBootApplication
public class GatewayMvcOpenapiApplication {

	// http :8080/v3/api-docs

	// http :8080/anything/versioned X-API-Version:2.0.1
	// http :8080/anything/versioned X-API-Version:1.0.0
	// http :8080/anything/versioned X-API-Version:0.9.0
	@Bean
	public RouterFunction<ServerResponse> version20() {
		// @formatter:off
		return route()
				.GET("/anything/versioned", version("2.0+"), http(),
						ops -> ops.operationId("versioned") //.beanClass(Response.class).beanMethod("anything"))
								.response(responseBuilder().responseCode("200").ref("http://localhost:8080/httpbin-spec.json#/paths/anything/{anything}/get/responses/200")))
								// .response(responseBuilder().responseCode("200").implementationArray(UserDTOv1.class)))
								//.parameter(parameterBuilder().in(ParameterIn.HEADER).name("X-API-Version")
								//		.schema(schemaBuilder().type("string").defaultValue("1.0").allowableValues(new String[]{"1.0", "2.0"}))))
								/*.response(responseBuilder().responseCode("200")
										.content(contentBuilder().array(arraySchemaBuilder().schema(
												schemaBuilder().name("UserDTOv1").type("object")
														.properties(new StringToClassMapItem[] {
																mapItem("headers", Map.class),
																mapItem("name", String.class),
																mapItem("email", String.class)
														}))))))*/
								// .response(responseBuilder().responseCode("200").description("This is normal response description")))
				.before(unary(routeId("versionroute20")))
				// .before(unary(new HttpbinUriResolver()))
				.filter(lb("httpbin"))
				.after(addResponseHeader("X-Version", "2.0+"))
				.build();
		// @formatter:on
	}

	@Bean
	public RouterFunction<ServerResponse> version10() {
		// @formatter:off
		return route()
				.GET("/anything/versioned", version("1.0+"), http(),
						ops -> ops.operationId("versioned") //.beanClass(Response.class).beanMethod("anything"))
								.response(responseBuilder().responseCode("200").ref("http://localhost:8080/httpbin-spec.json#/paths/anything/{anything}/get/responses/200")))
				// .response(responseBuilder().responseCode("200").implementationArray(UserDTOv2.class)))
				// 				.parameter(parameterBuilder().in(ParameterIn.HEADER).name("X-API-Version")
				// 						.schema(schemaBuilder().type("string").defaultValue("1.0").allowableValues(new String[]{"1.0", "2.0"})))
								/*.response(responseBuilder().responseCode("200")
										.content(contentBuilder().array(arraySchemaBuilder().schema(
												schemaBuilder().name("UserDTOv2").type("object")
														.properties(new StringToClassMapItem[] {
																mapItem("id", Integer.class),
																mapItem("firstName", String.class),
																mapItem("lastName", String.class),
																mapItem("email", String.class)
														}))))))*/
								//.response(responseBuilder().responseCode("200").description("This is normal response description")))

				.before(unary(routeId("versionroute10")))
				.before(unary(new HttpbinUriResolver()))
				.after(addResponseHeader("X-Version", "1.0+"))
				.build();
		// @formatter:on
	}

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI().components(new Components()
				.addSchemas("UserDTOv1", getSchemaWithDifferentDescription(UserDTOv1.class, "work Address" )));
	}

	private Schema getSchemaWithDifferentDescription(Class className, String description){
		Map<String, Schema> read = ModelConverters.getInstance().read(new AnnotatedType(className));
		return read.values().iterator().next();
		/*ResolvedSchema resolvedSchema = ModelConverters.getInstance()
				.resolveAsResolvedSchema(
						new AnnotatedType(className).resolveAsRef(true));
		return resolvedSchema.schema.description(description);*/
	}

	static UnaryOperator<ServerRequest> unary(Function<ServerRequest, ServerRequest> function) {
		return function::apply;
	}

	public record UserDTOv1(Integer id, String name, String email) {
	}

	public record UserDTOv2(Integer id, String firstName, String lastName, String email) {
	}

	@Component
	static class Response {
		Map<String, Object> anything() {
			return Collections.emptyMap();
		}
	}

	private StringToClassMapItem mapItem(String key, Class<?> clazz) {
		return new StringToClassMapItem() {
			@Override
			public Class<? extends Annotation> annotationType() {
				return StringToClassMapItem.class;
			}

			@Override
			public String key() {
				return key;
			}

			@Override
			public Class<?> value() {
				return clazz;
			}
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(GatewayMvcOpenapiApplication.class, args);
	}

}
