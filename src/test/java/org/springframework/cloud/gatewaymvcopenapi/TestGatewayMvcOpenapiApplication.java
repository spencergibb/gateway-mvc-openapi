package org.springframework.cloud.gatewaymvcopenapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration(proxyBeanMethods = false)
public class TestGatewayMvcOpenapiApplication {

	public static void main(String[] args) {
		SpringApplication.from(GatewayMvcOpenapiApplication::main)
				.with(HttpbinConfiguration.class).run(args);
	}
}
