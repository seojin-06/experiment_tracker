package com.seojin.experiment_tracker.project.web;

import com.seojin.experiment_tracker.common.api.ApiResponse;
import com.seojin.experiment_tracker.common.api.PageResponse;
import com.seojin.experiment_tracker.project.domain.Project;
import com.seojin.experiment_tracker.project.dto.CreateProjectRequest;
import com.seojin.experiment_tracker.project.dto.ProjectResponse;
import com.seojin.experiment_tracker.project.dto.UpdateProjectRequest;
import com.seojin.experiment_tracker.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProjectResponse> create(@RequestBody @Valid CreateProjectRequest req) {
        Project p = projectService.create(req);

        return ApiResponse.ok(ProjectResponse.of(p));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectResponse> get(@PathVariable String id){
        Project p = projectService.get(java.util.UUID.fromString(id));
        return ApiResponse.ok(ProjectResponse.of(p));
    }

    @GetMapping
    public ApiResponse<PageResponse<ProjectResponse>> list(@PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
                                                           Pageable pageable){
        Page<Project> page = projectService.list(pageable);
        Page<ProjectResponse> mapped = page.map(ProjectResponse::of);
        return ApiResponse.ok(PageResponse.of(mapped));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProjectResponse> update(@PathVariable UUID id,
                                               @RequestBody @Valid UpdateProjectRequest req) {
        Project p = projectService.update(id, req);
        return ApiResponse.ok(ProjectResponse.of(p));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        projectService.delete(id);
    }


}
