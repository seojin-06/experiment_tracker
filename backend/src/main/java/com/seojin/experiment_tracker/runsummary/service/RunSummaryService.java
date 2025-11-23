package com.seojin.experiment_tracker.runsummary.service;

import com.seojin.experiment_tracker.runsummary.domain.RunSummary;
import com.seojin.experiment_tracker.runsummary.dto.UpdateRunSummaryRequest;
import com.seojin.experiment_tracker.runsummary.repository.RunSummaryRepository;
import com.seojin.experiment_tracker.common.exception.NotFoundException;
import com.seojin.experiment_tracker.metric.domain.Metric;
import com.seojin.experiment_tracker.metric.repository.MetricRepository;
import com.seojin.experiment_tracker.run.domain.Run;
import com.seojin.experiment_tracker.run.repository.RunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RunSummaryService {
    private final RunRepository runRepository;
    private final RunSummaryRepository runSummaryRepository;
    private final MetricRepository metricRepository;

    @Transactional(readOnly = true)
    public RunSummary getOrThrow(UUID runId) {
        return runSummaryRepository.findByRun_Id(runId)
                .orElseThrow(() -> new NotFoundException("RunSummary not found for run: " + runId));
    }

    @Transactional(readOnly = true)
    public Optional<RunSummary> getOptional(UUID runId) {
        return runSummaryRepository.findByRun_Id(runId);
    }

    @Transactional
    public RunSummary patchNotes(UUID runId, UpdateRunSummaryRequest req) {
        RunSummary s = runSummaryRepository.findByRun_Id(runId)
                .orElseGet(() -> {
                    Run r = runRepository.findById(runId)
                            .orElseThrow(() -> new NotFoundException("Run not found: " + runId));
                    return RunSummary.builder().run(r).build();
                });
        s.setNotes(req.notes());
        return runSummaryRepository.save(s);
    }

    @Transactional
    public RunSummary recompute(UUID runId) {
        Run run = runRepository.findById(runId)
                .orElseThrow(() -> new NotFoundException("Run not found: " + runId));

        // 1) 메트릭 불러오기
        List<Metric> all = metricRepository.findByRun_IdOrderByStepAsc(runId);
        if (all.isEmpty()) {
            return runSummaryRepository.save(
                    runSummaryRepository.findByRun_Id(runId).orElseGet(() -> RunSummary.builder().run(run).build())
            );
        }

        long lastStep = all.stream().mapToLong(Metric::getStep).max().orElse(0L);

        // 3) epoch 후보들
        List<Metric> epochSeries = all.stream()
                .filter(m -> {
                    String k = m.getKey().toLowerCase(Locale.ROOT);
                    return k.equals("epoch") || k.endsWith("/epoch");
                })
                .sorted(Comparator.comparingLong(Metric::getStep))
                .toList();

        Integer lastEpoch = null;
        Map<Integer, Double> valAccByEpoch = new LinkedHashMap<>();

        // val/acc만 추출
        List<Metric> valAcc = all.stream()
                .filter(m -> "val/acc".equalsIgnoreCase(m.getKey()))
                .sorted(Comparator.comparingLong(Metric::getStep))
                .toList();

        if (!epochSeries.isEmpty()) {
            lastEpoch = safeToInt(epochSeries.get(epochSeries.size()-1).getValue());
            // val/acc에 epoch가 같이 있지 않다면, val/acc의 순서를 epoch로 매핑
            for (int i=0;i<valAcc.size();i++) {
                int ep = (i+1);
                valAccByEpoch.put(ep, valAcc.get(i).getValue());
            }
        } else {
            // epoch이 없으면 val/acc “개수”로 추정
            lastEpoch = valAcc.isEmpty() ? null : valAcc.size();
            for (int i=0;i<valAcc.size();i++) {
                valAccByEpoch.put(i+1, valAcc.get(i).getValue());
            }
        }

        // 4) bestAccuracy / bestEpoch
        Double bestAcc = null;
        Long bestEpoch = null;
        for (var e : valAccByEpoch.entrySet()) {
            if (e.getValue() == null) continue;
            if (bestAcc == null || e.getValue() > bestAcc) {
                bestAcc = e.getValue();
                bestEpoch = e.getKey().longValue();
            }
        }

        // 5) predictedFinalAccuracy (최근 3개 선형 외삽)
        Double predicted = null;
        if (valAcc.size() >= 2) {
            List<Double> ys = valAcc.stream().map(Metric::getValue).filter(Objects::nonNull).toList();
            int n = ys.size();
            if (n >= 2) {
                double y1 = ys.get(n-2);
                double y2 = ys.get(n-1);
                double slope = (y2 - y1); // epoch 간격 1 가정
                predicted = y2 + slope;   // 다음 epoch 예상
                if (predicted != null) {
                    predicted = Math.min(1.0, Math.max(predicted, bestAcc!=null?bestAcc:predicted));
                }
            }
        }

        // 6) earlyStopEpoch (patience=3, epsilon=0.001)
        Long early = null;
        final double epsilon = 0.001;
        final int patience = 3;
        if (valAcc.size() >= patience + 1) {
            List<Double> ys = valAcc.stream().map(Metric::getValue).filter(Objects::nonNull).toList();
            int n = ys.size();
            double recentBest = ys.subList(0, n).stream().max(Double::compareTo).orElse(Double.NaN);
            double lastBestInWindow = ys.subList(n - patience - 1, n).stream().max(Double::compareTo).orElse(Double.NaN);
            if (!Double.isNaN(recentBest) && !Double.isNaN(lastBestInWindow)) {
                if (recentBest - lastBestInWindow < epsilon && bestEpoch != null) {
                    early = bestEpoch;
                }
            }
        }

        // 7) 저장/업서트
        RunSummary s = runSummaryRepository.findByRun_Id(runId)
                .orElseGet(() -> RunSummary.builder().run(run).build());

        s.setBestAccuracy(bestAcc);
        s.setBestEpoch(bestEpoch);
        s.setLastEpoch(lastEpoch);
        s.setLastStep((int) lastStep);
        s.setPredictedFinalAccuracy(predicted);
        s.setEarlyStopEpoch(early);

        return runSummaryRepository.save(s);
    }

    private Integer safeToInt(Double v) {
        if (v == null) return null;
        try {
            return new BigDecimal(v.toString()).setScale(0, java.math.RoundingMode.HALF_UP).intValue();
        } catch (Exception e) {
            return v.intValue();
        }
    }
}
