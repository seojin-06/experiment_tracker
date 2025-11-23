package com.seojin.experiment_tracker.metric.service;

import com.seojin.experiment_tracker.common.exception.NotFoundException;
import com.seojin.experiment_tracker.metric.domain.Metric;
import com.seojin.experiment_tracker.metric.dto.LogMetricsRequest;
import com.seojin.experiment_tracker.metric.repository.MetricRepository;
import com.seojin.experiment_tracker.run.domain.Run;
import com.seojin.experiment_tracker.run.repository.RunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MetricService {
    private final MetricRepository metricRepository;
    private final RunRepository runRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void log(UUID runId, List<LogMetricsRequest> body) {
        Run run = runRepository.findById(runId)
                .orElseThrow(() -> new NotFoundException("Run not found: " + runId));

        List<Metric> rows = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();

        for (LogMetricsRequest item : body) {
            long step = item.step();
            Map<String, Double> map = item.metrics();
            if (map == null || map.isEmpty()) continue;

            for (Map.Entry<String, Double> e : map.entrySet()) {
                Metric m = new Metric();
                m.setRun(run);
                m.setKey(e.getKey());
                m.setValue(e.getValue());
                m.setStep(step);
                m.setRecordedAt(now);
                rows.add(m);
            }
        }
        if (!rows.isEmpty()) {
            metricRepository.saveAll(rows);
        }
    }

    @Transactional
    public Page<Metric> list(UUID runId, String key, Pageable pageable) {
        if (key != null && !key.isBlank()) {
            return metricRepository.findByRun_IdAndKey(runId, key, pageable);
        }
        return metricRepository.findByRun_Id(runId, pageable);
    }

    @Transactional
    public Metric last(UUID runId, String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key is required");
        }
        return metricRepository.findTop1ByRun_IdAndKeyOrderByStepDescRecordedAtDesc(runId, key);
    }

    /*@Transactional
    public List<Metric> appendAll(UUID runId, List<LogMetricsRequest> reqs) {
        List<Metric> saved = metricRepository.saveAll(
                reqs.stream().map(r -> Metric.of(runId, r)).toList()
        );
        eventPublisher.publishEvent(new MetricsAppendedEvent(runId)); // ★ 이거
        return saved;
    }

    @Transactional
    public Metric appendOne(UUID runId, LogMetricsRequest req) {
        Metric m = metricRepository.save(Metric.of(runId, req));
        eventPublisher.publishEvent(new MetricsAppendedEvent(runId)); // ★ 이것도
        return m;
    }*/
}
