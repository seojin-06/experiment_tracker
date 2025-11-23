package com.seojin.experiment_tracker.run.web;

import com.seojin.experiment_tracker.common.api.ApiResponse;
import com.seojin.experiment_tracker.common.api.PageResponse;
import com.seojin.experiment_tracker.run.domain.Run;
import com.seojin.experiment_tracker.run.dto.CreateRunRequest;
import com.seojin.experiment_tracker.run.dto.RunResponse;
import com.seojin.experiment_tracker.run.dto.UpdateRunRequest;
import com.seojin.experiment_tracker.run.service.RunService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RunController {
    private final RunService runService;

    // 일반 생성
    @PostMapping("/runs")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RunResponse> create(@Valid @RequestBody CreateRunRequest req) {
        return ApiResponse.ok(RunResponse.of(runService.create(req)));
    }

    // 편의 생성
    @PostMapping("/experiments/{experimentId}/runs")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RunResponse> createUnderExperiment(@PathVariable UUID experimentId,
                                                          @Valid @RequestBody CreateRunRequest req) {
        CreateRunRequest fixed = new CreateRunRequest(
                req.projectId(), experimentId, req.status(), req.seed(),
                req.startedAt(), req.finishedAt(), req.elapsedMs(), req.notes()
        );
        return ApiResponse.ok(RunResponse.of(runService.create(fixed)));
    }

    @PatchMapping("/runs/{id}")
    public ApiResponse<RunResponse> update(@PathVariable UUID id,
                                           @Valid @RequestBody UpdateRunRequest req) {
        return ApiResponse.ok(RunResponse.of(runService.update(id, req)));
    }

    @GetMapping("/runs/{id}")
    public ApiResponse<RunResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(RunResponse.of(runService.get(id)));
    }

    @GetMapping("/runs")
    public ApiResponse<PageResponse<RunResponse>> list(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) UUID experimentId,
            @PageableDefault(size = 20, sort = "startedAt") Pageable pageable
    ) {
        Page<Run> page = runService.list(projectId, experimentId, pageable);
        return ApiResponse.ok(PageResponse.of(page.map(RunResponse::of)));
    }

    @DeleteMapping("/runs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        runService.delete(id);
        return ApiResponse.ok(null);
    }
}
