package com.seojin.experiment_tracker.run.repository;

import com.seojin.experiment_tracker.run.domain.Run;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RunRepository extends JpaRepository<Run, UUID> {
    boolean existsByExperiment_Id(UUID experimentId);
    Page<Run> findByExperiment_Id(UUID experimentId, Pageable pageable);
    List<Run> findByExperiment_Id(UUID experimentId);
    Page<Run> findByProject_Id(UUID projectId, Pageable pageable);
    long countByExperiment_Id(UUID experimentId);
}
