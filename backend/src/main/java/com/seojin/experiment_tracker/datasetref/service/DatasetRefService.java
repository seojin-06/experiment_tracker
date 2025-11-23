package com.seojin.experiment_tracker.datasetref.service;

import com.seojin.experiment_tracker.common.exception.NotFoundException;
import com.seojin.experiment_tracker.datasetref.domain.DatasetRef;
import com.seojin.experiment_tracker.datasetref.dto.DatasetRefRequest;
import com.seojin.experiment_tracker.datasetref.repository.DatasetRefRepository;
import com.seojin.experiment_tracker.experiment.domain.Experiment;
import com.seojin.experiment_tracker.experiment.repository.ExperimentRepository;
import com.seojin.experiment_tracker.run.domain.Run;
import com.seojin.experiment_tracker.run.repository.RunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DatasetRefService {
    private final DatasetRefRepository datasetRefRepository;
    private final ExperimentRepository experimentRepository;
    private final RunRepository runRepository;

    @Transactional
    public DatasetRef create(UUID experimentId, DatasetRefRequest req) {
        Experiment exp = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new NotFoundException("Experiment not found"));

        Run run = null;
        if (req.runId() != null) {
            run = runRepository.findById(req.runId())
                    .orElseThrow(() -> new NotFoundException("Run not found"));
        }

        DatasetRef d = DatasetRef.builder()
                .experiment(exp)
                .run(run)
                .name(req.name())
                .version(req.version())
                .uri(req.uri())
                .checksum(req.checksum())
                .sizeBytes(req.sizeBytes())
                .description(req.description())
                .build();

        return datasetRefRepository.save(d);
    }

    @Transactional(readOnly = true)
    public List<DatasetRef> listByExperiment(UUID experimentId) {
        return datasetRefRepository.findByExperiment_IdOrderByCreatedAtDesc(experimentId);
    }

    @Transactional
    public DatasetRef createForRun(UUID runId, DatasetRefRequest req) {
        Run run = runRepository.findById(runId)
                .orElseThrow(() -> new NotFoundException("Run not found"));

        Experiment exp = run.getExperiment();

        DatasetRef d = DatasetRef.builder()
                .experiment(exp)
                .run(run)
                .name(req.name())
                .version(req.version())
                .uri(req.uri())
                .checksum(req.checksum())
                .sizeBytes(req.sizeBytes())
                .description(req.description())
                .build();

        return datasetRefRepository.save(d);
    }

    @Transactional(readOnly = true)
    public List<DatasetRef> listByRun(UUID runId) {
        return datasetRefRepository.findByRun_IdOrderByCreatedAtDesc(runId);
    }
}
