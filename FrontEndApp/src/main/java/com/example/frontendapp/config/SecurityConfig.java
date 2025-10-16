package com.example.frontendapp.config;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/home", "/css/**", "/js/**").permitAll()
                .requestMatchers("/calendar-data").hasRole("my-role")
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2.userInfoEndpoint(
                userInfo -> userInfo.oidcUserService(this.oidcUserService())
            ))
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll()
            );

        return http.build();
    }

    private OidcUserService oidcUserService() {
        OidcUserService delegate = new OidcUserService();

        return new OidcUserService() {
            @Override
            public OidcUser loadUser(OidcUserRequest userRequest) {
                OidcUser oidcUser = delegate.loadUser(userRequest);

                Map<String, Object> resourceAccess = oidcUser.getClaims().containsKey("resource_access")
                        ? (Map<String, Object>) oidcUser.getClaims().get("resource_access")
                        : Collections.emptyMap();

                Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get("frontend-app");
                List<String> clientRoles = clientAccess != null
                        ? (List<String>) clientAccess.get("roles")
                        : Collections.emptyList();

                // Map roles to ROLE_ prefix for Spring Security
                List<GrantedAuthority> mappedAuthorities = clientRoles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());

                // Merge the default authorities (SCOPE_*, OIDC_USER) + custom roles
                mappedAuthorities.addAll(oidcUser.getAuthorities());

                return new org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser(
                        mappedAuthorities,
                        oidcUser.getIdToken(),
                        oidcUser.getUserInfo()
                );
            }
		};
	}
}
