package com.seojin.experiment_tracker.artifact.web;

import com.seojin.experiment_tracker.artifact.domain.Artifact;
import com.seojin.experiment_tracker.artifact.dto.ArtifactResponse;
import com.seojin.experiment_tracker.artifact.enums.ArtifactType;
import com.seojin.experiment_tracker.artifact.service.ArtifactService;
import com.seojin.experiment_tracker.common.api.ApiResponse;
import com.seojin.experiment_tracker.common.api.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ArtifactController {
    private final ArtifactService artifactService;

    @PostMapping(value = "/runs/{runId}/artifacts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ArtifactResponse> upload(@PathVariable UUID runId,
                                                @RequestParam(defaultValue = "OTHER") ArtifactType type,
                                                @RequestPart("file") MultipartFile file) throws IOException, NoSuchAlgorithmException {
        Artifact a = artifactService.upload(runId, type, file);
        return ApiResponse.ok(ArtifactResponse.of(a));
    }

    @GetMapping("/runs/{runId}/artifacts")
    public ApiResponse<PageResponse<ArtifactResponse>> list(@PathVariable UUID runId,
                                                            @PageableDefault(size = 100, sort = "uploadedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        var page = artifactService.list(runId, pageable).map(ArtifactResponse::of);
        return ApiResponse.ok(PageResponse.of(page));
    }

    @GetMapping("/artifacts/{id}")
    public ApiResponse<ArtifactResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(ArtifactResponse.of(artifactService.get(id)));
    }

    @GetMapping("/artifacts/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id) throws IOException {
        Artifact a = artifactService.get(id);
        InputStreamResource res = new InputStreamResource(Files.newInputStream(Path.of(a.getUri())));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + Path.of(a.getUri()).getFileName() + "\"")
                .contentLength(a.getSizeBytes() != null ? a.getSizeBytes() : -1)
                .body((Resource) res);
    }

    @DeleteMapping("/artifacts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) throws IOException {
        artifactService.delete(id);
    }

}
