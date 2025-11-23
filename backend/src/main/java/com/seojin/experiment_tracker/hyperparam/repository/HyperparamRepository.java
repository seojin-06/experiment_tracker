package com.seojin.experiment_tracker.hyperparam.repository;

import com.seojin.experiment_tracker.hyperparam.domain.Hyperparam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HyperparamRepository extends JpaRepository<Hyperparam, UUID> {
    List<Hyperparam> findByRunIdOrderByKeyAsc(UUID runId);
    Optional<Hyperparam> findByRunIdAndKey(UUID runId, String key);
}
