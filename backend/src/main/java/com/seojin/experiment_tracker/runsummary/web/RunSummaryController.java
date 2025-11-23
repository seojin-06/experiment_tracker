package com.seojin.experiment_tracker.runsummary.web;


import com.seojin.experiment_tracker.runsummary.domain.RunSummary;
import com.seojin.experiment_tracker.runsummary.dto.RunSummaryResponse;
import com.seojin.experiment_tracker.runsummary.dto.UpdateRunSummaryRequest;
import com.seojin.experiment_tracker.runsummary.service.RunSummaryService;
import com.seojin.experiment_tracker.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RunSummaryController {
    private final RunSummaryService runSummaryService;

    @GetMapping("/runs/{runId}/summary")
    public ApiResponse<RunSummaryResponse> get(@PathVariable UUID runId) {
        RunSummary s = runSummaryService.recompute(runId); // 저장까지 포함
        return ApiResponse.ok(RunSummaryResponse.of(s));
    }

    @PostMapping("/runs/{runId}/summary/recompute")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<RunSummaryResponse> recompute(@PathVariable UUID runId) {
        var s = runSummaryService.recompute(runId);
        return ApiResponse.ok(RunSummaryResponse.of(s));
    }

    @PatchMapping("/runs/{runId}/summary")
    public ApiResponse<RunSummaryResponse> patchNotes(@PathVariable UUID runId,
                                                      @RequestBody UpdateRunSummaryRequest req) {
        var s = runSummaryService.patchNotes(runId, req);
        return ApiResponse.ok(RunSummaryResponse.of(s));
    }
}
