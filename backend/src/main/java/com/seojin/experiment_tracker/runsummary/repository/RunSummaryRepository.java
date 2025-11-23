package com.seojin.experiment_tracker.runsummary.repository;

import com.seojin.experiment_tracker.runsummary.domain.RunSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RunSummaryRepository extends JpaRepository<RunSummary, UUID> {
    Optional<RunSummary> findByRun_Id(UUID runId);
    boolean existsByRun_Id(UUID runId);
    void deleteByRun_Id(UUID runId);
}
