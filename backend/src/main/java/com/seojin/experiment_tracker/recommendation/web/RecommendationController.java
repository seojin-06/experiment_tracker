package com.seojin.experiment_tracker.ai.recommendation.web;

import com.seojin.experiment_tracker.ai.recommendation.domain.Recommendation;
import com.seojin.experiment_tracker.ai.recommendation.repository.RecommendationRepository;
import com.seojin.experiment_tracker.ai.recommendation.service.RecommendationService;
import com.seojin.experiment_tracker.common.api.ApiResponse;
import com.seojin.experiment_tracker.common.api.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/experiments/{experimentId}/recommendations")
@RequiredArgsConstructor
public class RecommendationController {
    private final RecommendationRepository recommendationRepository;
    private final RecommendationService recommendationService;
    @GetMapping
    public ApiResponse<PageResponse<Recommendation>> list(@PathVariable UUID experimentId,
                                                          @PageableDefault(size=50, sort="createdAt", direction= Sort.Direction.DESC) Pageable pageable) {
        var page = recommendationRepository.findByExperiment_Id(experimentId, pageable);
        return ApiResponse.ok(PageResponse.of(page.map(r -> r)));
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Integer> refresh(@PathVariable UUID experimentId){
        var list = recommendationService.refresh(experimentId);
        return ApiResponse.ok(list.size());
    }
}
