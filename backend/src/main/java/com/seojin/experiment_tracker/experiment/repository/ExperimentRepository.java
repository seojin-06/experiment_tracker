package com.seojin.experiment_tracker.experiment.repository;

import com.seojin.experiment_tracker.experiment.domain.Experiment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExperimentRepository extends JpaRepository<Experiment, UUID> {
    Page<Experiment> findByProject_Id(UUID projectId, Pageable pageable);

    boolean existsByExperimentNameAndProject_Id(String experimentName, UUID projectId);

    boolean existsByExperimentNameAndProject_IdAndIdNot(String experimentName, UUID projectId, UUID id);
}
