package com.seojin.experiment_tracker.ai.recommendation.repository;

import com.seojin.experiment_tracker.ai.recommendation.domain.Recommendation;
import com.seojin.experiment_tracker.run.domain.Run;
import org.aspectj.weaver.ast.Var;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {
    void deleteByExperiment_Id(UUID experimentId);
    Page<Recommendation> findByExperiment_Id(UUID experimentId, Pageable pageable);
}
