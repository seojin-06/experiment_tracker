package com.seojin.experiment_tracker.project.service;

import com.seojin.experiment_tracker.common.exception.ConflictException;
import com.seojin.experiment_tracker.common.exception.NotFoundException;
import com.seojin.experiment_tracker.project.domain.Project;
import com.seojin.experiment_tracker.project.dto.CreateProjectRequest;
import com.seojin.experiment_tracker.project.dto.UpdateProjectRequest;
import com.seojin.experiment_tracker.project.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {
    private final ProjectRepository projectRepository;

    public Project create(CreateProjectRequest req) {
        if(projectRepository.existsByProjectName(req.projectName())) {
            throw new ConflictException("Project name already exists");
        }

        Project p = Project.builder()
                .projectName(req.projectName())
                .description(req.description())
                .build();

        try {
            return projectRepository.save(p);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Project name already exists");
        }
    }

    @Transactional(readOnly = true)
    public Project get(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project not found: " + id));
    }

    @Transactional
    public Project update(UUID id, UpdateProjectRequest req) {
        Project p = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        if (projectRepository.existsByProjectNameAndIdNot(req.projectName(), id)) {
            throw new ConflictException("Project name already exists");
        }

        p.setProjectName(req.projectName());
        p.setDescription(req.description());

        try {
            return p;
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Project name already exists");
        }
    }

    @Transactional
    public void delete(UUID id) {
        Project p = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        if(projectRepository.hasChildExperiments(id)) {
            throw new ConflictException("Project has experiments and cannot be deleted");
        }

        projectRepository.deleteById(id);
    }

    public Page<Project> list(Pageable pageable) {
        return projectRepository.findAll(pageable);
    }
}
