package com.seojin.experiment_tracker.artifact.service;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public interface ArtifactStorage {
    StoredFile store(UUID runId, String filename, InputStream in) throws IOException, NoSuchAlgorithmException;
    InputStream load(String uri) throws IOException;
    void delete(String uri) throws IOException;

    record StoredFile(String uri, long sizeBytes, String checksum) {}
}
