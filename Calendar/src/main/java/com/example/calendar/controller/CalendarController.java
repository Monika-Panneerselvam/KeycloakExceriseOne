package com.example.calendar.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CalendarController {

	// optional: restrict to callers that have 'my-role' as an authority in token
	@GetMapping("/calendar")
	public Map<String, Object> getCalendar(@AuthenticationPrincipal Jwt jwt) {
		// Optionally verify the presence of my-role in the token
		// But resource-server configuration already validates token; this checks role
		// membership
		Object roles = null;
		if (jwt.containsClaim("realm_access")) {
			Object realmAccess = jwt.getClaim("realm_access");
			// naive extraction — real code should parse safe types
			// ... omitted for brevity
		}

		List<Map<String, Object>> items = List.of(
				Map.of("date", LocalDate.now().toString(), "title", "Meeting with team"),
				Map.of("date", LocalDate.now().plusDays(1).toString(), "title", "Prepare demo"));

		return Map.of("user", jwt.getSubject(), "items", items);
	}
}
