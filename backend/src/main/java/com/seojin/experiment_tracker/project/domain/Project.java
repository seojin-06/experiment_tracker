package com.seojin.experiment_tracker.project.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "project")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Project {
    @Id @GeneratedValue @UuidGenerator
    private UUID id;

    @Column(length = 120, nullable = false, unique = true)
    private String projectName;

    @Column(columnDefinition = "text")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
