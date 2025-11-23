package com.seojin.experiment_tracker.experiment.service;

import com.seojin.experiment_tracker.common.exception.ConflictException;
import com.seojin.experiment_tracker.common.exception.NotFoundException;
import com.seojin.experiment_tracker.experiment.domain.Experiment;
import com.seojin.experiment_tracker.experiment.dto.CreateExperimentRequest;
import com.seojin.experiment_tracker.experiment.dto.UpdateExperimentRequest;
import com.seojin.experiment_tracker.experiment.repository.ExperimentRepository;
import com.seojin.experiment_tracker.project.domain.Project;
import com.seojin.experiment_tracker.project.repository.ProjectRepository;
import com.seojin.experiment_tracker.run.repository.RunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExperimentService {
    private final ExperimentRepository experimentRepository;
    private final ProjectRepository projectRepository;
    private final RunRepository runRepository;

    @Transactional
    public Experiment create(CreateExperimentRequest req) {
        Project project = projectRepository.findById(req.projectId())
                .orElseThrow(() -> new NotFoundException("Project not found: " + req.projectId()));

        if (experimentRepository.existsByExperimentNameAndProject_Id(req.experimentName(), req.projectId())) {
            throw new ConflictException("Experiment name already exists in this project");
        }

        Experiment e = new Experiment();
        e.setExperimentName(req.experimentName());
        e.setPurpose(req.purpose());
        e.setNotes(req.notes());
        e.setProject(project);
        e.setTags(req.tags() != null ? req.tags() : new String[0]);

        return experimentRepository.save(e);
    }

    @Transactional
    public Experiment update(UUID id, UpdateExperimentRequest req) {
        Experiment e = get(id);

        // 프로젝트 변경 허용: 요청의 projectId로 변경할 수 있도록
        Project project = projectRepository.findById(req.projectId())
                .orElseThrow(() -> new NotFoundException("Project not found: " + req.projectId()));

        if (experimentRepository.existsByExperimentNameAndProject_IdAndIdNot(req.experimentName(), req.projectId(), id)) {
            throw new ConflictException("Experiment name already exists in this project");
        }

        e.setExperimentName(req.experimentName());
        e.setPurpose(req.purpose());
        e.setNotes(req.notes());
        e.setProject(project);
        e.setTags(req.tags() != null ? req.tags() : new String[0]);

        return e;
    }

    @Transactional
    public void delete(UUID id) {
        Experiment e = get(id);

        if (runRepository.existsByExperiment_Id(id)) {
            throw new ConflictException("Experiment has runs and cannot be deleted");
        }
        experimentRepository.deleteById(e.getId());
    }

    @Transactional
    public Experiment get(UUID id) {
        return experimentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Experiment not found: " + id));
    }

    @Transactional
    public Page<Experiment> list(UUID projectId, Pageable pageable) {
        if (projectId != null) {
            return experimentRepository.findByProject_Id(projectId, pageable);
        }
        return experimentRepository.findAll(pageable);
    }
}
