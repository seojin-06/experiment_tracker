package com.seojin.experiment_tracker.artifact.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalArtifactStorage implements ArtifactStorage {

    @Value("${app.storage.artifacts-root:./data/artifacts}")
    private String rootDir;

    @Override
    public StoredFile store(UUID runId, String filename, InputStream in) throws IOException, NoSuchAlgorithmException {
        Path dir = Path.of(rootDir, runId.toString());
        Files.createDirectories(dir);
        Path dst = dir.resolve(filename);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        long size = 0;

        try (OutputStream out = Files.newOutputStream(dst);
             DigestInputStream dis = new DigestInputStream(in, digest)) {
            size = dis.transferTo(out);
        }
        String checksum = HexFormat.of().formatHex(digest.digest());
        return new StoredFile(dst.toAbsolutePath().toString(), size, checksum);
    }

    @Override
    public InputStream load(String uri) throws IOException {
        return Files.newInputStream(Path.of(uri));
    }

    @Override
    public void delete(String uri) throws IOException {
        Files.deleteIfExists(Path.of(uri));
    }
}
