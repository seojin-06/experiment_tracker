package com.seojin.experiment_tracker.metric.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.seojin.experiment_tracker.common.api.ApiResponse;
import com.seojin.experiment_tracker.common.api.PageResponse;
import com.seojin.experiment_tracker.metric.domain.Metric;
import com.seojin.experiment_tracker.metric.dto.LogMetricsRequest;
import com.seojin.experiment_tracker.metric.service.MetricService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MetricController {
    private final MetricService metricService;

    @PostMapping(value = "/runs/{runId}/metrics", consumes = "application/json")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> log(@PathVariable UUID runId,
                                 @Valid @RequestBody List<LogMetricsRequest> body) {
        metricService.log(runId, body);
        return ApiResponse.ok(null);
    }

    @GetMapping("/runs/{runId}/metrics")
    public ApiResponse<PageResponse<MetricResponse>> list(@PathVariable UUID runId,
                                                          @RequestParam(required = false) String key,
                                                          @PageableDefault(size = 200, sort = {"step","recordedAt"}) Pageable pageable) {
        Page<Metric> page = metricService.list(runId, key, pageable);
        return ApiResponse.ok(PageResponse.of(page.map(MetricResponse::of)));
    }

    @GetMapping("/runs/{runId}/metrics/last")
    public ApiResponse<MetricResponse> last(@PathVariable UUID runId,
                                            @RequestParam String key) {
        Metric m = metricService.last(runId, key);
        return ApiResponse.ok(m != null ? MetricResponse.of(m) : null);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record MetricResponse(
            String id, String runId, String key,
            long step, Double value, String recordedAt
    ) {
        static MetricResponse of(Metric m){
            return new MetricResponse(
                    m.getId()!=null? m.getId().toString(): null,
                    m.getRun()!=null? m.getRun().getId().toString(): null,
                    m.getKey(),
                    m.getStep(),
                    m.getValue(),
                    m.getRecordedAt()!=null? m.getRecordedAt().toString(): null
            );
        }
    }
}
