package com.seojin.experiment_tracker.ai.recommendation.service;

import com.seojin.experiment_tracker.ai.recommendation.domain.Recommendation;
import com.seojin.experiment_tracker.ai.recommendation.dto.RecoDtos;
import com.seojin.experiment_tracker.ai.recommendation.enums.RecommendationType;
import com.seojin.experiment_tracker.ai.recommendation.repository.RecommendationRepository;
import com.seojin.experiment_tracker.common.exception.NotFoundException;
import com.seojin.experiment_tracker.experiment.domain.Experiment;
import com.seojin.experiment_tracker.experiment.repository.ExperimentRepository;
import com.seojin.experiment_tracker.metric.domain.Metric;
import com.seojin.experiment_tracker.metric.repository.MetricRepository;
import com.seojin.experiment_tracker.run.domain.Run;
import com.seojin.experiment_tracker.run.repository.RunRepository;
import com.seojin.experiment_tracker.runsummary.service.RunSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {
    private final ExperimentRepository experimentRepo;
    private final RunRepository runRepo;
    private final MetricRepository metricRepo;
    private final RecommendationRepository recoRepo;
    private final RecommendationClient recoClient;
    private final RunSummaryService runSummaryService;

    @Transactional
    public List<Recommendation> refresh(UUID experimentId) {
        Experiment exp = experimentRepo.findById(experimentId)
                .orElseThrow(() -> new NotFoundException("Expriment not found"));

        final int N = 200;

        List<String> accKeys = List.of(
                "val/acc", "val.acc", "val_acc", "valAccuracy",
                "accuracy", "acc", "val_accuracy",
                "train.acc", "train_acc", "trainAccuracy", "train/acc"
        );
        List<String> lossKeys = List.of(
                "train/loss", "train.loss", "train_loss",
                "loss", "loss_value",
                "val/loss", "val.loss", "val_loss"
        );

        List<Run> runs = runRepo.findByExperiment_Id(experimentId);
        List<RecoDtos.RunSeries> series = new ArrayList<>();

        for(Run r : runs) {
            List<Metric> acc = new ArrayList<>();
            for (String k : accKeys) {
                acc.addAll(metricRepo.findByRun_IdAndKeyOrderByStepDesc(
                        r.getId(), k, PageRequest.of(0, N)
                ));
            }

            List<Metric> loss = new ArrayList<>();
            for (String k : lossKeys) {
                loss.addAll(metricRepo.findByRun_IdAndKeyOrderByStepDesc(
                        r.getId(), k, PageRequest.of(0, N)
                ));
            }

            acc.sort(Comparator.comparingLong(Metric::getStep));
            loss.sort(Comparator.comparingLong(Metric::getStep));

            var valAccPoints = acc.stream()
                    .map(m -> new RecoDtos.MetricPoint(m.getStep(), m.getValue()))
                    .toList();

            var trainLossPoints = loss.stream()
                    .map(m -> new RecoDtos.MetricPoint(m.getStep(), m.getValue()))
                    .toList();

            series.add(new RecoDtos.RunSeries(
                    r.getId().toString(),
                    valAccPoints,
                    trainLossPoints
            ));
        }

        log.info("AI RECO REQUEST runs={}, firstRun valAcc={}, trainLoss={}",
                series.size(),
                series.isEmpty() ? 0 : series.get(0).valAcc().size(),
                series.isEmpty() ? 0 : series.get(0).trainLoss().size()
        );

        recoRepo.deleteByExperiment_Id(experimentId);

        var req = new RecoDtos.Request(experimentId.toString(), series);
        var res = RecommendationClient.analyze(req);

        List<Recommendation> saved = new ArrayList<>();
        if (res != null && res.suggestions() != null) {
            for (var s : res.suggestions()) {
                var entity = Recommendation.builder()
                        .experiment(exp)
                        .type(RecommendationType.valueOf(s.type()))   // enum 변환
                        .paramsJson(s.params() == null ? Map.of() : s.params())
                        .predictedScore(s.predictedScore())
                        .explanationsJson(s.explanations())
                        .contextJson(s.context())
                        .build();
                saved.add(recoRepo.save(entity));

                if ("EARLY_STOP_HINT".equalsIgnoreCase(s.type())) {
                    Map<String, Object> params = (s.params() != null) ? s.params() : Map.of();
                    Map<String, Object> explanations = (s.explanations() != null) ? s.explanations() : Map.of();

                    Object runIdRaw = params.get("runId");
                    if (runIdRaw == null) {
                        continue;
                    }
                    UUID runId = UUID.fromString(runIdRaw.toString());

                    // predFinalAcc
                    Double predFinalAcc = null;
                    Object predRaw = params.get("predFinalAcc");
                    if (predRaw instanceof Number n) {
                        predFinalAcc = n.doubleValue();
                    } else if (predRaw != null) {
                        try {
                            predFinalAcc = Double.parseDouble(predRaw.toString());
                        } catch (Exception ignored) {}
                    }

                    // earlyStopEpoch
                    Long earlyStopEpoch = null;
                    Object earlyRaw = params.get("earlyStopEpoch");
                    if (earlyRaw instanceof Number n) {
                        earlyStopEpoch = n.longValue();
                    } else if (earlyRaw != null) {
                        try {
                            earlyStopEpoch = Long.parseLong(earlyRaw.toString());
                        } catch (Exception ignored) {}
                    }

                    log.info("[AI] EARLY_STOP_HINT for run {}: predFinalAcc={}, earlyStopEpoch={}, reason={}",
                            runId, predFinalAcc, earlyStopEpoch);

                    // RunSummary에 AI 결과 반영
                    runSummaryService.applyAiPrediction(runId, predFinalAcc, earlyStopEpoch);
                }
            }
        }
        return saved;
    }
}
