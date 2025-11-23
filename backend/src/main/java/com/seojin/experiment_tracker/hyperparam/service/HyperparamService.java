package com.seojin.experiment_tracker.hyperparam.service;

import com.seojin.experiment_tracker.hyperparam.domain.Hyperparam;
import com.seojin.experiment_tracker.hyperparam.dto.HyperparamUpsertRequest;
import com.seojin.experiment_tracker.hyperparam.repository.HyperparamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HyperparamService {
    private final HyperparamRepository hyperparamRepository;

    @Transactional
    public List<Hyperparam> upsertAll(UUID runId, List<HyperparamUpsertRequest> reqs) {
        List<Hyperparam> result = new ArrayList<>(reqs.size());
        for(HyperparamUpsertRequest r : reqs) {
            Hyperparam h = hyperparamRepository.findByRunIdAndKey(runId, r.key())
                    .map(existing -> { existing.apply(r); return existing; })
                    .orElseGet(() -> Hyperparam.of(runId, r));

            result.add(h);
        }
        return hyperparamRepository.saveAll(result);
    }

    @Transactional(readOnly = true)
    public List<Hyperparam> list(UUID runId) {
        return hyperparamRepository.findByRunIdOrderByKeyAsc(runId);
    }
}
