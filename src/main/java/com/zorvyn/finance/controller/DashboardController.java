package com.zorvyn.finance.controller;

import com.zorvyn.finance.dto.DashboardSummary;
import com.zorvyn.finance.service.DashboardService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummary summary() {
        return dashboardService.getSummary();
    }
}
