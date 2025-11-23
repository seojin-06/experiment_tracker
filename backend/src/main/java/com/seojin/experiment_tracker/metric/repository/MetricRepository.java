package com.seojin.experiment_tracker.metric.repository;

import com.seojin.experiment_tracker.metric.domain.Metric;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface MetricRepository extends JpaRepository<Metric, UUID> {
    Page<Metric> findByRun_Id(UUID runId, Pageable pageable);

    Page<Metric> findByRun_IdAndKey(UUID runId, String key, Pageable pageable);

    Metric findTop1ByRun_IdAndKeyOrderByStepDescRecordedAtDesc(UUID runId, String key);

    List<Metric> findByRun_IdOrderByStepAsc(UUID runId);

    @Query("""
    select m from Metric m
    where m.run.id = :runId and m.key = :key
    order by m.step desc
    """)
    List<Metric> findByRun_IdAndKeyOrderByStepDesc(UUID runId, String key, Pageable pageable);
}
