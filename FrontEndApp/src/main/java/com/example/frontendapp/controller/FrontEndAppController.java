package com.example.frontendapp.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Controller
public class FrontEndAppController {

	private final WebClient calendarWebClient;

	public FrontEndAppController(WebClient calendarWebClient) {
		this.calendarWebClient = calendarWebClient;
	}

	@GetMapping("/")
	public String index(Model model, @AuthenticationPrincipal OidcUser oidcUser) {
		if (oidcUser != null) {
			model.addAttribute("username", oidcUser.getPreferredUsername());
			model.addAttribute("claims", oidcUser.getClaims());
		}
		return "home";
	}

	// This endpoint is protected by SecurityConfig to require ROLE_my-role
	@GetMapping("/calendar-data")
	public String calendarData(Model model,
			@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient authorizedClient) {
		// Using WebClient that automatically sends bearer token via oauth2 filter
		Mono<String> json = calendarWebClient.get().uri("/api/calendar").retrieve().bodyToMono(String.class);

		String result = json.block(); // for simplicity — this call is short-lived
		model.addAttribute("calendarJson", result);
		return "home";
	}

}
