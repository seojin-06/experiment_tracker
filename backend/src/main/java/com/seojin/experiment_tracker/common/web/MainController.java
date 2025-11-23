package com.seojin.experiment_tracker.common.web;

import com.seojin.experiment_tracker.common.api.ApiResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MainController {
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.ok("OK");
    }
}
