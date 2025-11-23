package com.seojin.experiment_tracker.run.service;

import com.seojin.experiment_tracker.common.exception.ConflictException;
import com.seojin.experiment_tracker.common.exception.NotFoundException;
import com.seojin.experiment_tracker.experiment.domain.Experiment;
import com.seojin.experiment_tracker.experiment.repository.ExperimentRepository;
import com.seojin.experiment_tracker.project.domain.Project;
import com.seojin.experiment_tracker.project.repository.ProjectRepository;
import com.seojin.experiment_tracker.run.domain.Run;
import com.seojin.experiment_tracker.run.dto.CreateRunRequest;
import com.seojin.experiment_tracker.run.dto.UpdateRunRequest;
import com.seojin.experiment_tracker.run.enums.RunStatus;
import com.seojin.experiment_tracker.run.repository.RunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RunService {
    private final RunRepository runRepository;
    private final ProjectRepository projectRepository;
    private final ExperimentRepository experimentRepository;

    @Transactional
    public Run create(CreateRunRequest req) {
        Project project = projectRepository.findById(req.projectId())
                .orElseThrow(() -> new NotFoundException("Project not found: " + req.projectId()));

        Experiment experiment = experimentRepository.findById(req.experimentId())
                .orElseThrow(() -> new NotFoundException("Experiment not found: " + req.experimentId()));

        // 실험-프로젝트 정합성 체크
        if (!experiment.getProject().getId().equals(project.getId())) {
            throw new ConflictException("Experiment does not belong to the given project");
        }

        Run r = new Run();
        r.setProject(project);
        r.setExperiment(experiment);
        r.setStatus(req.status() != null ? req.status() : RunStatus.PENDING);
        r.setSeed(req.seed());
        r.setNotes(req.notes());

        if (req.startedAt() != null) r.setStartedAt(req.startedAt());
        if (req.finishedAt() != null) r.setFinishedAt(req.finishedAt());
        if (req.elapsedMs() != null) r.setElapsedMs(req.elapsedMs());

        // RUNNING인데 startedAt이 없으면 지금으로
        if (r.getStatus() == RunStatus.RUNNING && r.getStartedAt() == null) {
            r.setStartedAt(OffsetDateTime.now());
        }
        // 완료 상태면 finishedAt/elapsedMs 보정
        if (isTerminal(r.getStatus())) {
            if (r.getFinishedAt() == null) r.setFinishedAt(OffsetDateTime.now());
            if (r.getStartedAt() != null && r.getElapsedMs() == null) {
                r.setElapsedMs(r.getFinishedAt().toInstant().toEpochMilli() - r.getStartedAt().toInstant().toEpochMilli());
            }
        }
        return runRepository.save(r);
    }

    @Transactional
    public Run update(UUID id, UpdateRunRequest req) {
        Run r = get(id);

        if (req.seed() != null) r.setSeed(req.seed());
        if (req.notes() != null) r.setNotes(req.notes());
        if (req.startedAt() != null) r.setStartedAt(req.startedAt());
        if (req.finishedAt() != null) r.setFinishedAt(req.finishedAt());
        if (req.elapsedMs() != null) r.setElapsedMs(req.elapsedMs());

        if (req.status() != null) {
            RunStatus prev = r.getStatus();
            r.setStatus(req.status());

            if (prev != RunStatus.RUNNING && r.getStatus() == RunStatus.RUNNING && r.getStartedAt() == null) {
                r.setStartedAt(OffsetDateTime.now());
            }
            if (isTerminal(r.getStatus())) {
                if (r.getFinishedAt() == null) r.setFinishedAt(OffsetDateTime.now());
                if (r.getStartedAt() != null && r.getElapsedMs() == null) {
                    r.setElapsedMs(r.getFinishedAt().toInstant().toEpochMilli() - r.getStartedAt().toInstant().toEpochMilli());
                }
            }
        }
        return r;
    }

    @Transactional
    public void delete(UUID id) {
        Run r = get(id);
        runRepository.deleteById(r.getId());
    }

    @Transactional
    public Run get(UUID id) {
        return runRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Run not found: " + id));
    }

    @Transactional
    public Page<Run> list(UUID projectId, UUID experimentId, Pageable pageable) {
        if (experimentId != null) return runRepository.findByExperiment_Id(experimentId, pageable);
        if (projectId != null) return runRepository.findByProject_Id(projectId, pageable);
        return runRepository.findAll(pageable);
    }

    private boolean isTerminal(RunStatus s){
        return s == RunStatus.SUCCEEDED || s == RunStatus.FAILED || s == RunStatus.CANCELED;
    }
}
