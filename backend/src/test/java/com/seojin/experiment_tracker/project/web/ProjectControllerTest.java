package com.seojin.experiment_tracker.project.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seojin.experiment_tracker.project.dto.CreateProjectRequest;
import com.seojin.experiment_tracker.project.dto.UpdateProjectRequest;
import com.seojin.experiment_tracker.project.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;



@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
@WithMockUser
class ProjectControllerTest {

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @BeforeEach
    void clean() {
        projectRepository.deleteAll(); // 다른 테이블 FK 있으면 deleteAll 순서/ cascade 주의
    }

    @Test @DisplayName("POST /api/projects: 생성 성공")
    void create_ok() throws Exception {
        var req = new CreateProjectRequest("P1", "desc");
        mvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectName").value("P1"))
                .andExpect(jsonPath("$.data.id", not(emptyString())));
    }

    @Test @DisplayName("POST /api/projects: 중복 이름 409")
    void create_conflict() throws Exception {
        var req = new CreateProjectRequest("Dup", null);
        mvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code", anyOf(is("CONFLICT"), is("DUPLICATE"))));
    }

    @Test @DisplayName("GET /api/projects/{id}: 조회 성공 & 404")
    void get_by_id() throws Exception {
        String id = mvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new CreateProjectRequest("P1", null))))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String createdId = om.readTree(id).path("data").path("id").asText();

        mvc.perform(get("/api/projects/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectName").value("P1"));

        mvc.perform(get("/api/projects/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test @DisplayName("PATCH /api/projects/{id}: 수정 성공 & 중복 409")
    void update_ok_and_conflict() throws Exception {
        String p1 = mvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new CreateProjectRequest("A", null))))
                .andReturn().getResponse().getContentAsString();
        String p2 = mvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new CreateProjectRequest("B", null))))
                .andReturn().getResponse().getContentAsString();

        String id1 = om.readTree(p1).path("data").path("id").asText();
        String id2 = om.readTree(p2).path("data").path("id").asText();

        // 정상 수정
        mvc.perform(put("/api/projects/{id}", id1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new UpdateProjectRequest("A2", "d2"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectName").value("A2"));

        // 중복 이름으로 수정 → 409
        mvc.perform(put("/api/projects/{id}", id1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new UpdateProjectRequest("B", null))))
                .andExpect(status().isConflict());
    }

    @Test @DisplayName("GET /api/projects: 페이지 리스트")
    void list_paging() throws Exception {
        for (int i = 0; i < 25; i++) {
            mvc.perform(post("/api/projects")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(new CreateProjectRequest("P"+i, null))))
                    .andExpect(status().isCreated());
        }

        mvc.perform(get("/api/projects?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(20)))
                .andExpect(jsonPath("$.data.totalElements").value(25));

        mvc.perform(get("/api/projects?page=1&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(5)));
    }

    @Test @DisplayName("DELETE /api/projects/{id}: 삭제 성공 & 404")
    void delete_ok_and_404() throws Exception {
        String body = mvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new CreateProjectRequest("DEL", null))))
                .andReturn().getResponse().getContentAsString();
        String id = om.readTree(body).path("data").path("id").asText();

        mvc.perform(delete("/api/projects/{id}", id))
                .andExpect(status().isNoContent());

        mvc.perform(delete("/api/projects/{id}", id))
                .andExpect(status().isNotFound());
    }
}