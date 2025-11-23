package com.seojin.experiment_tracker.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PageController {
    @GetMapping({"/", "/index"})
    public String home() { return "index"; }

    @GetMapping("/projects")
    public String projects() { return "project/projects"; }

    @GetMapping("/projects/detail")
    public String projectDetail() { return "project/projectDetail"; }

    @GetMapping("/projects/{id}")
    public String projectDetailRestful(@PathVariable String id, Model model) {
        model.addAttribute("id", id);
        return "project/projectDetail";
    }

    @GetMapping("/experiments")
    public String experiments() { return "experiments/experiments"; }

    @GetMapping("/runs")
    public String runs() { return "runs/runs"; }

    @GetMapping("/metrics")
    public String metrics() { return "metrics/metrics"; }

    @GetMapping("/artifacts")
    public String artifacts() { return "artifacts/artifacts"; }
}
