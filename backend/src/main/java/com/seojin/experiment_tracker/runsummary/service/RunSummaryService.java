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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@Slf4j
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
        Map<Integer, Double> accByEpoch = new LinkedHashMap<>();

        List<String> accKeys = List.of(
                "val/acc", "val.acc", "val_acc", "valAccuracy",
                "accuracy", "acc", "val_accuracy",
                "train.acc", "train_acc", "trainAccuracy", "train/acc"
        );

        String selectedAccKey = null;
        for (String cand : accKeys) {
            boolean exists = all.stream().anyMatch(m -> {
                String k = m.getKey();
                return k != null && k.equalsIgnoreCase(cand);
            });
            if (exists) {
                selectedAccKey = cand;
                break;
            }
        }

        // 🔹 3-2) 선택된 키로 accuracy 시계열 뽑기
        String finalSelectedAccKey = selectedAccKey;
        List<Metric> accSeries = (selectedAccKey == null)
                ? List.of()
                : all.stream()
                .filter(m -> {
                    String k = m.getKey();
                    return k != null && k.equalsIgnoreCase(finalSelectedAccKey);
                })
                .sorted(Comparator.comparingLong(Metric::getStep))
                .toList();

        // 🔹 3-3) epoch / acc 매핑
        if (!epochSeries.isEmpty()) {
            // epoch 메트릭이 있는 경우 → 마지막 epoch는 epochSeries 기준
            lastEpoch = safeToInt(epochSeries.get(epochSeries.size() - 1).getValue());
            // accSeries의 순서를 epoch 1,2,3... 으로 매핑 (epoch 값과 1:1이 아니어도 대략적으로)
            for (int i = 0; i < accSeries.size(); i++) {
                int ep = (i + 1);
                accByEpoch.put(ep, accSeries.get(i).getValue());
            }
        } else {
            // epoch 메트릭이 없으면 acc 개수로 epoch 추정
            lastEpoch = accSeries.isEmpty() ? null : accSeries.size();
            for (int i = 0; i < accSeries.size(); i++) {
                accByEpoch.put(i + 1, accSeries.get(i).getValue());
            }
        }

        // 4) bestAccuracy / bestEpoch
        Double bestAcc = null;
        Long bestEpoch = null;
        for (var e : accByEpoch.entrySet()) {
            Double v = e.getValue();
            if (v == null) continue;
            if (bestAcc == null || v > bestAcc) {
                bestAcc = v;
                bestEpoch = e.getKey().longValue();
            }
        }

        /*// 5) predictedFinalAccuracy (최근 3개 선형 외삽)
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
        }*/

        // 7) 저장/업서트
        RunSummary s = runSummaryRepository.findByRun_Id(runId)
                .orElseGet(() -> RunSummary.builder().run(run).build());

        s.setBestAccuracy(bestAcc);
        s.setBestEpoch(bestEpoch);
        s.setLastEpoch(lastEpoch);
        s.setLastStep((int) lastStep);

        return runSummaryRepository.save(s);
    }

    @Transactional
    public RunSummary applyAiPrediction(
            UUID runId,
            Double predictedFinalAccuracy,
            Long earlyStopEpoch
    ) {
        Run run = runRepository.findById(runId)
                .orElseThrow(() -> new NotFoundException("Run not found: " + runId));

        RunSummary s = runSummaryRepository.findByRun_Id(runId)
                .orElseGet(() -> RunSummary.builder().run(run).build());

        if (predictedFinalAccuracy != null) {
            s.setPredictedFinalAccuracy(predictedFinalAccuracy);
        }
        if (earlyStopEpoch != null) {
            s.setEarlyStopEpoch(earlyStopEpoch);
        }

        log.info("[RunSummary] applyAiPrediction runId={}, predictedFinalAccuracy={}, earlyStopEpoch={}",
                runId, s.getPredictedFinalAccuracy(), s.getEarlyStopEpoch());


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
