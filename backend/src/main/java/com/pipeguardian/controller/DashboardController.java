package com.pipeguardian.controller;

import com.pipeguardian.service.InspectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final InspectionService inspectionService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("alerts", inspectionService.getAlerts());
        return "dashboard";
    }
}
