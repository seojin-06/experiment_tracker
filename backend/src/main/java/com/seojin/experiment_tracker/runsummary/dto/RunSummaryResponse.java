package com.seojin.experiment_tracker.runsummary.dto;

import com.seojin.experiment_tracker.runsummary.domain.RunSummary;

public record RunSummaryResponse(
        String runId,
        Double bestAccuracy,
        Long bestEpoch,
        Integer lastEpoch,
        Integer lastStep,
        Double predictedFinalAccuracy,
        Long earlyStopEpoch,
        String updatedAt,
        String notes
) {
    public static RunSummaryResponse of(RunSummary s) {
        return new RunSummaryResponse(
                s.getRun()!=null ? s.getRun().getId().toString() : null,
                s.getBestAccuracy(),
                s.getBestEpoch(),
                s.getLastEpoch(),
                s.getLastStep(),
                s.getPredictedFinalAccuracy(),
                s.getEarlyStopEpoch(),
                s.getUpdatedAt()!=null ? s.getUpdatedAt().toString() : null,
                s.getNotes()
        );
    }
}
