package com.seojin.experiment_tracker.hyperparam.domain;

import com.seojin.experiment_tracker.hyperparam.dto.HyperparamUpsertRequest;
import com.seojin.experiment_tracker.hyperparam.enums.Source;
import com.seojin.experiment_tracker.hyperparam.enums.ValueType;
import com.seojin.experiment_tracker.common.jpa.BaseEntity;
import com.seojin.experiment_tracker.run.domain.Run;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "hyperparam",
        uniqueConstraints = @UniqueConstraint(name = "uk_hparam_run_key", columnNames = {"run_id","key"}),
        indexes = {
                @Index(name = "idx_hparam_run", columnList = "run_id"),
                @Index(name = "idx_hparam_run_key", columnList = "run_id,key")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Hyperparam extends BaseEntity { //학습에 사용된 하이퍼파라미터(Key-Value) 기록
    @Id @GeneratedValue @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private Run run;

    @Column(length = 100, nullable = false)
    private String key;

    @Column(name = "value_string", nullable = true)
    private String valueString;

    @Column(name = "value_numeric", nullable = true)
    private Double valueNumeric;

    @Column(name = "value_boolean", nullable = true)
    private Boolean valueBoolean;

    @Column(name = "value_json", columnDefinition = "text", nullable = true)
    private String valueJson;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private ValueType valueType;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Source source;

    public static Hyperparam of(UUID runId, HyperparamUpsertRequest r) {
        Run runRef = new Run(); runRef.setId(runId);
        Hyperparam hp = Hyperparam.builder()
                .run(runRef)
                .key(r.key())
                .valueType(r.valueType())
                .source(r.source() != null ? r.source() : Source.CONFIG)
                .build();
        // 값 채우기
        switch (hp.getValueType()) {
            case STRING -> hp.setValueString(r.valueString());
            case NUMBER -> hp.setValueNumeric(r.valueNumeric());
            case BOOLEAN -> hp.setValueBoolean(r.valueBoolean());
            case JSON -> hp.setValueJson(r.valueJson());
        }
        return hp;
    }

    public void apply(HyperparamUpsertRequest r) {
        setValueType(r.valueType());
        setSource(r.source() != null ? r.source() : Source.CONFIG);
        setValueString(null); setValueNumeric(null); setValueBoolean(null); setValueJson(null);
        switch (r.valueType()) {
            case STRING -> setValueString(r.valueString());
            case NUMBER -> setValueNumeric(r.valueNumeric());
            case BOOLEAN -> setValueBoolean(r.valueBoolean());
            case JSON -> setValueJson(r.valueJson());
        }
    }
}
