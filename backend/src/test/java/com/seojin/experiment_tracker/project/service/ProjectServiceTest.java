package com.seojin.experiment_tracker.project.service;

import com.seojin.experiment_tracker.common.exception.ConflictException;
import com.seojin.experiment_tracker.common.exception.NotFoundException;
import com.seojin.experiment_tracker.project.domain.Project;
import com.seojin.experiment_tracker.project.dto.CreateProjectRequest;
import com.seojin.experiment_tracker.project.dto.UpdateProjectRequest;
import com.seojin.experiment_tracker.project.repository.ProjectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ProjectServiceTest {

    ProjectRepository repo = mock(ProjectRepository.class);
    ProjectService sut = new ProjectService(repo);

    @Test @DisplayName("create: 중복 이름이면 ConflictException")
    void create_conflict() {
        given(repo.existsByProjectName("P1")).willReturn(true);
        assertThatThrownBy(() ->
                sut.create(new CreateProjectRequest("P1", "d")))
                .isInstanceOf(ConflictException.class);
    }

    @Test @DisplayName("create: 정상 저장")
    void create_ok() {
        given(repo.existsByProjectName("P1")).willReturn(false);
        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        given(repo.save(captor.capture())).willAnswer(inv -> inv.getArgument(0));

        Project saved = sut.create(new CreateProjectRequest("P1", "d"));
        assertThat(saved.getProjectName()).isEqualTo("P1");
        assertThat(saved.getDescription()).isEqualTo("d");
    }

    @Test @DisplayName("update: 없으면 NotFoundException")
    void update_notfound() {
        UUID id = UUID.randomUUID();
        given(repo.findById(id)).willReturn(Optional.empty());
        assertThatThrownBy(() ->
                sut.update(id, new UpdateProjectRequest("X","Y")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test @DisplayName("update: 같은 이름이면 ConflictException")
    void update_conflict() {
        UUID id = UUID.randomUUID();
        Project p = Project.builder().id(id).projectName("Old").description("D").build();
        given(repo.findById(id)).willReturn(Optional.of(p));
        given(repo.existsByProjectNameAndIdNot("New", id)).willReturn(true);

        assertThatThrownBy(() ->
                sut.update(id, new UpdateProjectRequest("New","D2")))
                .isInstanceOf(ConflictException.class);
    }

    @Test @DisplayName("update: 정상 갱신")
    void update_ok() {
        UUID id = UUID.randomUUID();
        Project p = Project.builder().id(id).projectName("Old").description("D").build();
        given(repo.findById(id)).willReturn(Optional.of(p));
        given(repo.existsByProjectNameAndIdNot("New", id)).willReturn(false);

        Project res = sut.update(id, new UpdateProjectRequest("New", "D2"));
        assertThat(res.getProjectName()).isEqualTo("New");
        assertThat(res.getDescription()).isEqualTo("D2");
    }
}