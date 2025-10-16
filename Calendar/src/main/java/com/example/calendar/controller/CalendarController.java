package com.example.calendar.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CalendarController {

    @GetMapping("/api/calendar")
    public String getCalendar() {
        return "Your Keycloak-protected calendar data!";
    }
}
