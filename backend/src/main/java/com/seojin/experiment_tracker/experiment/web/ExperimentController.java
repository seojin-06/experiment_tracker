package com.seojin.experiment_tracker.experiment.web;


import com.seojin.experiment_tracker.common.api.ApiResponse;
import com.seojin.experiment_tracker.common.api.PageResponse;
import com.seojin.experiment_tracker.experiment.domain.Experiment;
import com.seojin.experiment_tracker.experiment.dto.CreateExperimentRequest;
import com.seojin.experiment_tracker.experiment.dto.ExperimentResponse;
import com.seojin.experiment_tracker.experiment.dto.UpdateExperimentRequest;
import com.seojin.experiment_tracker.experiment.service.ExperimentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/experiments")
@RequiredArgsConstructor
public class ExperimentController {
    private final ExperimentService experimentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExperimentResponse> create(@Valid @RequestBody CreateExperimentRequest req) {
        return ApiResponse.ok(ExperimentResponse.of(experimentService.create(req)));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ExperimentResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody UpdateExperimentRequest req) {
        return ApiResponse.ok(ExperimentResponse.of(experimentService.update(id, req)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ExperimentResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(ExperimentResponse.of(experimentService.get(id)));
    }

    @GetMapping
    public ApiResponse<PageResponse<ExperimentResponse>> list(
            @RequestParam(required = false) UUID projectId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        Page<Experiment> page = experimentService.list(projectId, pageable);
        return ApiResponse.ok(PageResponse.of(page.map(ExperimentResponse::of)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        experimentService.delete(id);
        return ApiResponse.ok(null);
    }
}
