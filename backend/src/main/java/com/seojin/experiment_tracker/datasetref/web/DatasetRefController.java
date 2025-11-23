package com.seojin.experiment_tracker.datasetref.web;


import com.seojin.experiment_tracker.common.api.ApiResponse;
import com.seojin.experiment_tracker.datasetref.domain.DatasetRef;
import com.seojin.experiment_tracker.datasetref.dto.DatasetRefRequest;
import com.seojin.experiment_tracker.datasetref.dto.DatasetRefResponse;
import com.seojin.experiment_tracker.datasetref.service.DatasetRefService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DatasetRefController {
    private final DatasetRefService datasetRefService;

    @GetMapping("/experiments/{experimentId}/datasets")
    public ApiResponse<List<DatasetRefResponse>> listByExperiment(@PathVariable UUID experimentId) {
        List<DatasetRef> list = datasetRefService.listByExperiment(experimentId);
        return ApiResponse.ok(list.stream().map(DatasetRefResponse::of).toList());
    }

    @PostMapping("/experiments/{experimentId}/datasets")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DatasetRefResponse> create(@PathVariable UUID experimentId,
                                                  @Valid @RequestBody DatasetRefRequest req) {
        DatasetRef saved = datasetRefService.create(experimentId, req);
        return ApiResponse.ok(DatasetRefResponse.of(saved));
    }

    @GetMapping("/runs/{runId}/datasets")
    public ApiResponse<List<DatasetRefResponse>> listByRun(@PathVariable UUID runId) {
        List<DatasetRef> list = datasetRefService.listByRun(runId);
        return ApiResponse.ok(list.stream().map(DatasetRefResponse::of).toList());
    }

    @PostMapping("/runs/{runId}/datasets")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DatasetRefResponse> createForRun(@PathVariable UUID runId,
                                                        @Valid @RequestBody DatasetRefRequest req) {
        DatasetRef saved = datasetRefService.createForRun(runId, req);
        return ApiResponse.ok(DatasetRefResponse.of(saved));
    }
}
