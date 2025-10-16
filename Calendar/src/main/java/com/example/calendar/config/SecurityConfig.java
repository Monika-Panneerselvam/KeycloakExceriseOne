package com.example.calendar.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(
				auth -> auth.requestMatchers("/", "/actuator/health").permitAll().anyRequest().hasRole("my-role"))
				.oauth2ResourceServer(
						oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

		return http.build();
	}

	private JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

		converter.setJwtGrantedAuthoritiesConverter(jwt -> {
			Collection<String> roles = extractClientRoles(jwt, "frontend-app");

			return roles.stream().map(role -> "ROLE_" + role)
					.map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
					.collect(Collectors.toList());
		});

		return converter;
	}

	@SuppressWarnings("unchecked")
	private Collection<String> extractClientRoles(Jwt jwt, String clientId) {
		Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
		if (resourceAccess == null)
			return List.of();

		Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(clientId);
		if (clientAccess == null)
			return List.of();

		Object roles = clientAccess.get("roles");
		if (roles instanceof Collection<?>) {
			return ((Collection<?>) roles).stream().map(Object::toString).collect(Collectors.toList());
		}

		return List.of();
	}

	@Bean
	public JwtDecoder jwtDecoder() {
		return JwtDecoders.fromIssuerLocation("http://localhost:8082/realms/myrealm");
	}
}
