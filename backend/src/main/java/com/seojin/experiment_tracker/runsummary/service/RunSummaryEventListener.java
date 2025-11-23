package com.seojin.experiment_tracker.runsummary.service;

import com.seojin.experiment_tracker.runsummary.dto.MetricsAppendedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RunSummaryEventListener {
    private final RunSummaryService runSummaryService;

    @TransactionalEventListener
    public void onMetricsAppended(MetricsAppendedEvent ev) {
        runSummaryService.recompute(ev.runId());
    }
}
