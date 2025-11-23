package com.seojin.experiment_tracker.ai.recommendation.service;

import com.seojin.experiment_tracker.ai.recommendation.dto.RecoDtos;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;

@Component
public class RecommendationClient {
    private static final WebClient web = WebClient.create();
    private static final String aiUrl = System.getProperty("ai.reco.url",
            System.getenv().getOrDefault("AI_RECO_URL", "http://localhost:5001/reco"));

    public static RecoDtos.Response analyze(RecoDtos.Request req) {
        return  web.post().uri(aiUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .bodyToMono(RecoDtos.Response.class)
                .block();
    }
}
