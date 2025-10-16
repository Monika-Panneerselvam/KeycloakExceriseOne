package com.example.frontendapp.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.function.client.WebClient;

@Controller
public class FrontEndAppController {

	private final WebClient webClient;

	@Value("${calendar.base-url}")
	private String calendarBaseUrl;

	public FrontEndAppController(WebClient webClient) {
		this.webClient = webClient;
	}

	@GetMapping("/")
	public String home() {
		return "home";
	}

	@GetMapping("/calendar-data")
	public String getCalendarData(Model model) {
		String data = webClient.get().uri(calendarBaseUrl + "/api/calendar").retrieve().bodyToMono(String.class)
				.block();

		model.addAttribute("calendarData", data);
		return "calendar";
	}
}
