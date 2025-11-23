package com.seojin.experiment_tracker.project.repository;

import com.seojin.experiment_tracker.project.domain.Project;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    boolean existsByProjectName(String projectName);
    boolean existsByProjectNameAndIdNot(String projectName, UUID id);

    @Query("""
            select (count(e) > 0) from Experiment e where e.project.id = :projectId
            """)
    boolean hasChildExperiments(@Param("projectId") UUID projectId);
}
