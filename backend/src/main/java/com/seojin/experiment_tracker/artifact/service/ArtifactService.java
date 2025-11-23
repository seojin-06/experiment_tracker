package com.seojin.experiment_tracker.artifact.service;

import com.seojin.experiment_tracker.artifact.domain.Artifact;
import com.seojin.experiment_tracker.artifact.enums.ArtifactType;
import com.seojin.experiment_tracker.artifact.repository.ArtifactRepository;
import com.seojin.experiment_tracker.common.exception.NotFoundException;
import com.seojin.experiment_tracker.run.domain.Run;
import com.seojin.experiment_tracker.run.repository.RunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArtifactService {
    private final ArtifactRepository artifactRepository;
    private final RunRepository runRepository;
    private final ArtifactStorage storage;

    @Transactional
    public Artifact upload(UUID runId, ArtifactType type, MultipartFile file) throws IOException, NoSuchAlgorithmException {
        Run run = runRepository.findById(runId)
                .orElseThrow(() -> new NotFoundException("Run not found"));

        String filename = file.getOriginalFilename();
        InputStream in = file.getInputStream();

        var stored = storage.store(runId, filename, in);

        Artifact artifact = Artifact.builder()
                .run(run)
                .type(type != null ? type : ArtifactType.OTHER)
                .uri(stored.uri())
                .sizeBytes(stored.sizeBytes())
                .checksum(stored.checksum())
                .uploadedAt(OffsetDateTime.now())
                .build();

        return artifactRepository.save(artifact);
    }

    @Transactional(readOnly = true)
    public Page<Artifact> list(UUID runId, Pageable pageable) {
        return artifactRepository.findByRun_Id(runId, pageable);
    }

    @Transactional(readOnly = true)
    public Artifact get(UUID id) {
        return artifactRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Artifact not found"));
    }

    @Transactional
    public void delete(UUID id) throws IOException {
        Artifact a = get(id);
        storage.delete(a.getUri());
        artifactRepository.delete(a);
    }
}
