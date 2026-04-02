package com.zorvyn.finance.controller;

import com.zorvyn.finance.dto.DashboardSummary;
import com.zorvyn.finance.service.DashboardService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public DashboardSummary summary() {
        return dashboardService.getSummary();
    }
}
