package org.springframework.cloud.gatewaymvcopenapi;

import org.springframework.web.accept.SemanticApiVersionParser;

public class ApiVersionParser extends SemanticApiVersionParser {

    // allows us to use /api/v2/users instead of /api/2.0/users
    @Override
    public Version parseVersion(String version) {
        // Remove "v" prefix if it exists (v1 becomes 1, v2 becomes 2)
        if (version.startsWith("v") || version.startsWith("V")) {
            version = version.substring(1);
        }

		if (version.endsWith("+")) {
			version = version.substring(0, version.length() - 1);
		}
		
        return super.parseVersion(version);
    }
}
