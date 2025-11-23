package com.seojin.experiment_tracker.envsnapshot.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seojin.experiment_tracker.envsnapshot.dto.EnvSnapshotRequest;
import com.seojin.experiment_tracker.envsnapshot.dto.EnvSnapshotResponse;
import com.seojin.experiment_tracker.envsnapshot.service.EnvSnapshotService;
import com.seojin.experiment_tracker.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/runs/{runId}/env")
@RequiredArgsConstructor
public class EnvSnapshotController {
    private final EnvSnapshotService envSnapshotService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ApiResponse<EnvSnapshotResponse> get(@PathVariable UUID runId) {
        var env = envSnapshotService.get(runId)
                .map(e -> EnvSnapshotResponse.from(e, objectMapper))
                .orElse(null);
        return ApiResponse.ok(env);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EnvSnapshotResponse> upsert(@PathVariable UUID runId, @RequestBody EnvSnapshotRequest body) {
        var saved = envSnapshotService.saveOrUpdate(runId, body);
        return ApiResponse.ok(EnvSnapshotResponse.from(saved, objectMapper));
    }
}
