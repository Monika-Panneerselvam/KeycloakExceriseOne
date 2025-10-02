package com.example.frontendapp.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(authorize -> authorize.requestMatchers("/", "/login", "/error").permitAll()
				// only allow users who have ROLE_my-role to access /calendar-data
				.requestMatchers("/calendar-data").hasRole("my-role").anyRequest().authenticated())
				.oauth2Login(
						oauth2 -> oauth2.userInfoEndpoint(userInfo -> userInfo.oidcUserService(this.oidcUserService())))
				.logout(logout -> logout.logoutSuccessUrl("/").permitAll());

		return http.build();
	}

	/**
	 * Map Keycloak roles from ID token (or userinfo) to Spring authorities with
	 * prefix ROLE_. This implementation reads `realm_access.roles` and
	 * `resource_access` roles and converts them into
	 * SimpleGrantedAuthority("ROLE_<role>").
	 */
	private OidcUserService oidcUserService() {
		OidcUserService delegate = new OidcUserService();
		return userRequest -> {
			OidcUser oidcUser = delegate.loadUser(userRequest);
			Map<String, Object> claims = oidcUser.getClaims();

			// Collect roles from realm_access and resource_access
			Collection<GrantedAuthority> mappedAuthorities = extractRolesFromClaims(claims).stream()
					.map(r -> new SimpleGrantedAuthority("ROLE_" + r)).collect(Collectors.toList());

			// Include existing authorities and the mapped roles
			mappedAuthorities.addAll(oidcUser.getAuthorities());

			return oidcUser; // NOTE: We could return a new DefaultOidcUser(mappedAuthorities,
								// oidcUser.getIdToken(), oidcUser.getUserInfo())
		};
	}

	private List<String> extractRolesFromClaims(Map<String, Object> claims) {
		// realm_access.roles
		List<String> roles = List.of();
		if (claims.containsKey("realm_access")) {
			Object realmAccess = claims.get("realm_access");
			if (realmAccess instanceof Map) {
				Object r = ((Map<?, ?>) realmAccess).get("roles");
				if (r instanceof Iterable) {
					roles = ((Collection<String>) r).stream().map(Object::toString).collect(Collectors.toList());
				}
			}
		}

		// resource_access.<client>.roles or check 'roles' claim
		if (claims.containsKey("resource_access")) {
			Object resourceAccess = claims.get("resource_access");
			if (resourceAccess instanceof Map) {
				for (Object clientEntry : ((Map<?, ?>) resourceAccess).values()) {
					if (clientEntry instanceof Map) {
						Object rr = ((Map<?, ?>) clientEntry).get("roles");
						if (rr instanceof Iterable) {
							roles = Stream.concat(roles.stream(), ((Collection<String>) rr).stream().map(Object::toString))
									.collect(Collectors.toList());
						}
					}
				}
			}
		}

		// fallback: roles claim directly
		if (claims.containsKey("roles") && claims.get("roles") instanceof Iterable) {
			roles = Stream.concat(roles.stream(), ((Collection<String>) claims.get("roles")).stream().map(Object::toString))
					.collect(Collectors.toList());
		}

		return roles.stream().distinct().collect(Collectors.toList());
	}
}
