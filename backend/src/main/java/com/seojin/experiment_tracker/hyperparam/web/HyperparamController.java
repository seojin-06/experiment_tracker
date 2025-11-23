package com.seojin.experiment_tracker.hyperparam.web;

import com.seojin.experiment_tracker.hyperparam.dto.HyperparamResponse;
import com.seojin.experiment_tracker.hyperparam.dto.HyperparamUpsertRequest;
import com.seojin.experiment_tracker.hyperparam.service.HyperparamService;
import com.seojin.experiment_tracker.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/runs/{runId}/hyperparams")
@RequiredArgsConstructor
public class HyperparamController {
    private final HyperparamService hyperparamService;

    @GetMapping
    public ApiResponse<List<HyperparamResponse>> list(@PathVariable UUID runId) {
        var rows = hyperparamService.list(runId).stream()
                .map(HyperparamResponse::from).toList();
        return ApiResponse.ok(rows);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<HyperparamResponse>> upsertAll( @PathVariable UUID runId,
                                                            @RequestBody List<HyperparamUpsertRequest> body) {
        var saved = hyperparamService.upsertAll(runId, body)
                .stream().map(HyperparamResponse::from).toList();
        return ApiResponse.ok(saved);
    }
}
