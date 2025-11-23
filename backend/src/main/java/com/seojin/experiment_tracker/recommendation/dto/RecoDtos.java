package com.seojin.experiment_tracker.ai.recommendation.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RecoDtos {
    public record MetricPoint(long step, double value) {}
    public record RunSeries(String runId, List<MetricPoint> valAcc, List<MetricPoint> trainLoss) {}
    public record Request(String experimentId, List<RunSeries> runs) {}

    public record Suggestion(
            String type,
            Map<String, Object> params,
            Double predictedScore,
            Map<String, Object> explanations,
            Map<String, Object> context
    ){}

    public record Response(List<Suggestion> suggestions){}
}
