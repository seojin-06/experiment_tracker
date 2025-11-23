package com.seojin.experiment_tracker.artifact.repository;

import com.seojin.experiment_tracker.artifact.domain.Artifact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ArtifactRepository extends JpaRepository<Artifact, UUID> {
    Page<Artifact> findByRun_Id(UUID runId, Pageable pageable);
}
