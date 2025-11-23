package com.seojin.experiment_tracker.project.repository;

import com.seojin.experiment_tracker.project.domain.Project;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ProjectRepositoryTest {

    @Autowired
    ProjectRepository projectRepository;

    @Test
    @DisplayName("existByProjectName 동작 확인")
    void existsByProjectName() {
        projectRepository.save(Project.builder().projectName("P1").description("d").build());
        assertThat(projectRepository.existsByProjectName("P1")).isTrue();
        assertThat(projectRepository.existsByProjectName("P2")).isFalse();
    }

    @Test
    @DisplayName("existsByProjectNameAndIdNot 동작 확인")
    void existsByProjectNameAndIdNot() {
        Project p1 = projectRepository.save(Project.builder().projectName("Same").build());
        Project p2 = projectRepository.save(Project.builder().projectName("Other").build());

        assertThat(projectRepository.existsByProjectNameAndIdNot("Same", p1.getId())).isFalse();
        assertThat(projectRepository.existsByProjectNameAndIdNot("Same", p2.getId())).isTrue();
    }
}