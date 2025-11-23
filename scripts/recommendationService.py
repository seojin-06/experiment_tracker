# ai_reco_service_xgb.py (또는 recommendationService.py)
print("### LOADED AI RECO SERVICE v3 ###")

from fastapi import FastAPI
from pydantic import BaseModel
from typing import List, Optional, Dict, Tuple
import numpy as np
import warnings

# XGBoost
from xgboost import XGBRegressor

app = FastAPI()

# ---------- Pydantic models (계약 동일) ----------

class MetricPoint(BaseModel):
    step: int
    value: float

class RunSeries(BaseModel):
    runId: str
    valAcc: List[MetricPoint] = []
    trainLoss: List[MetricPoint] = []

class Request(BaseModel):
    experimentId: str
    runs: List[RunSeries]

class Suggestion(BaseModel):
    type: str
    params: Dict[str, object]
    predictedScore: Optional[float] = None
    explanations: Optional[Dict[str, object]] = None
    context: Optional[Dict[str, object]] = None

class Response(BaseModel):
    suggestions: List[Suggestion]

# ---------- 유틸 ----------

def to_xy(points: List[MetricPoint]) -> Optional[Tuple[np.ndarray, np.ndarray]]:
    """
    (지금은 직접 X, y를 만드는 방식으로 바꿔서 안 쓰지만,
    필요하면 feature를 늘릴 때 참고용으로 남겨둔 함수)
    """
    if len(points) < 3:
        return None
    pts = sorted(points, key=lambda p: p.step)
    x = np.array([p.step for p in pts], dtype=float).reshape(-1, 1)
    y = np.array([p.value for p in pts], dtype=float)
    return x, y

def xgb_predict_next(points: List[MetricPoint], *, next_offset: int = 1) -> Optional[Dict[str, float]]:
    """
    XGBoost로 다음 스텝의 값을 예측.
    - 입력: MetricPoint(step, value) 리스트
    - 출력: dict {y_last, y_pred, delta, next_step}
      * y_last: 마지막 관측값
      * y_pred: 다음 step에서의 예측값
      * delta : y_pred - y_last
      * next_step: 예측한 step 번호
    - 포인트가 3개 미만이면 XGBoost 학습하지 않고 y_pred = y_last 로 fallback.
    """
    if not points:
        return None

    pts = sorted(points, key=lambda p: p.step)
    last = pts[-1]
    y_last = float(last.value)
    max_step = int(last.step)
    next_step = max_step + next_offset

    # 데이터가 너무 적으면 (3개 미만) → 그냥 '지금 값 유지'라고 가정
    if len(pts) < 3:
        return {
            "y_last": y_last,
            "y_pred": y_last,
            "delta": 0.0,
            "next_step": next_step
        }

    # 충분한 데이터 → XGBoost 회귀
    X = np.array([p.step for p in pts], dtype=float).reshape(-1, 1)
    y = np.array([p.value for p in pts], dtype=float)
    x_next = np.array([[next_step]], dtype=float)

    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        model = XGBRegressor(
            n_estimators=80,
            max_depth=3,
            learning_rate=0.15,
            subsample=0.9,
            colsample_bytree=0.9,
            reg_lambda=1.0,
            objective="reg:squarederror",
            random_state=42,
        )
        model.fit(X, y)
        y_pred = float(model.predict(x_next)[0])

    return {
        "y_last": y_last,
        "y_pred": y_pred,
        "delta": y_pred - y_last,
        "next_step": next_step
    }

def safe_last(points: List[MetricPoint]) -> Optional[float]:
    if not points:
        return None
    pts = sorted(points, key=lambda p: p.step)
    return float(pts[-1].value)

# ---------- 메인 로직 ----------

@app.post("/reco", response_model=Response)
def reco(req: Request):
    print("### RECO CALLED v3 ###")

    stats = []
    print("DEBUG PY RUNS:", len(req.runs))
    if req.runs:
        print("DEBUG PY first run valAcc:", len(req.runs[0].valAcc),
              "trainLoss:", len(req.runs[0].trainLoss))

    for r in req.runs:
        acc_pred = xgb_predict_next(r.valAcc)       # 다음 step의 valAcc 예측 (또는 fallback)
        loss_pred = xgb_predict_next(r.trainLoss)   # 다음 step의 trainLoss 예측 (또는 fallback)
        last_acc = safe_last(r.valAcc)
        last_loss = safe_last(r.trainLoss)

        stats.append({
            "runId": r.runId,
            "acc_pred": acc_pred,     # None or dict
            "loss_pred": loss_pred,   # None or dict
            "last_acc": last_acc,     # None or float
            "last_loss": last_loss,   # None or float
        })

    suggestions: List[Suggestion] = []

    # 1) RUN_SELECTION: "예측된 valAcc"가 가장 높은 run 추천 (fallback: 최근값)
    def acc_score_for_selection(s):
        if s["acc_pred"] is not None:
            return s["acc_pred"]["y_pred"]
        if s["last_acc"] is not None:
            return s["last_acc"]
        return -1.0

    best = None
    for s in stats:
        sc = acc_score_for_selection(s)
        if best is None or sc > acc_score_for_selection(best):
            best = s

    if best and (best["acc_pred"] is not None or best["last_acc"] is not None):
        y_last = best["acc_pred"]["y_last"] if best["acc_pred"] else best["last_acc"]
        y_pred = best["acc_pred"]["y_pred"] if best["acc_pred"] else best["last_acc"]
        suggestions.append(Suggestion(
            type="RUN_SELECTION",
            params={"bestRunId": best["runId"], "predValAcc": y_pred},
            predictedScore=0.95,
            explanations={
                "basis": "XGBoost next-step prediction" if best["acc_pred"] else "fallback_last_acc",
                "y_last": y_last,
                "y_pred": y_pred
            },
            context={"window": "all_points", "model": "XGBRegressor+fallback"}
        ))

    # 2) EARLY_STOP_HINT: "loss 예측은 감소(좋음)인데 acc 예측은 개선이 거의 없음/하락" → 일반화 이슈 힌트
    for s in stats:
        acc_ok = s["acc_pred"] is not None
        loss_ok = s["loss_pred"] is not None
        if acc_ok and loss_ok:
            acc_delta = s["acc_pred"]["delta"]
            loss_delta = s["loss_pred"]["delta"]
            # loss가 줄어들(음수)고, acc가 거의 증가하지 않거나(<=ε) 하락(<=0)
            if loss_delta < -1e-4 and acc_delta <= 1e-4:
                score = min(
                    1.0,
                    float(abs(loss_delta)) * 10.0 + float(max(0.0, -acc_delta)) * 10.0
                )
                suggestions.append(Suggestion(
                    type="EARLY_STOP_HINT",
                    params={
                        "runId": s["runId"],
                        "hints": [
                            "EarlyStopping(patience=3~5)",
                            "Increase Dropout",
                            "Increase Weight Decay",
                            "Stronger Augmentation"
                        ]
                    },
                    predictedScore=round(score, 2),
                    explanations={
                        "loss_next_delta": loss_delta,
                        "acc_next_delta": acc_delta,
                        "reason": "Loss expected to drop while valAcc stagnates or decreases"
                    },
                    context={"model": "XGBRegressor+fallback", "predict_next_step": True}
                ))

    # 3) HYPERPARAM_SUGGESTION: 전체적으로 acc 예측 향상이 미미/음수 → 탐색 제안
    deltas = [s["acc_pred"]["delta"] for s in stats if s["acc_pred"] is not None]
    mean_delta = float(np.mean(deltas)) if deltas else 0.0

    # 전체 마지막 정확도 평균
    last_accs = [s["last_acc"] for s in stats if s["last_acc"] is not None]
    avg_acc = float(np.mean(last_accs)) if last_accs else 0.0

    # 초기 기본 그리드
    grid = {
        "lr": [0.1, 0.03, 0.01, 0.003, 0.001],
        "batch_size": [32, 64, 128],
        "epochs": [30, 50, 80]
    }

    reason = "Default grid"

    # =========================
    # 상황별로 grid 변경
    # =========================

    # ① 성능 정체(mean_delta <= 0) → lr 높이는 방향
    if mean_delta <= 0:
        grid["lr"] = [0.1, 0.05, 0.03]
        reason = "Accuracy trend flat/declining → try higher LR"

    # ② 이미 정확도 0.90 이상인데 더 안 오르면 → 더 작은 lr
    if avg_acc >= 0.90 and mean_delta <= 0:
        grid["lr"] = [0.01, 0.003, 0.001]
        reason = "High accuracy but plateau → lower LR for fine-tuning"

    # ③ 오버피팅 패턴(loss↓ acc→ 정체)
    for s in stats:
        if s["acc_pred"] and s["loss_pred"]:
            if s["loss_pred"]["delta"] < 0 and s["acc_pred"]["delta"] <= 0:
                grid["batch_size"] = [64, 128]
                grid["epochs"] = [20, 30]
                reason = "Overfitting pattern detected → larger batch & fewer epochs"
                break

    # ④ underfitting(loss 안 줄고 acc 낮음)
    if avg_acc < 0.6:
        grid["epochs"] = [50, 80, 120]
        reason = "Underfitting → train longer epochs"

    suggestions.append(Suggestion(
        type="HYPERPARAM_SUGGESTION",
        params={"grid": grid},
        predictedScore=0.6,
        explanations={
            "acc_next_delta_mean": mean_delta,
            "avg_acc": avg_acc,
            "reason": reason
        },
        context={"model": "XGBRegressor_dynamic_grid"}
    ))

    return Response(suggestions=suggestions)

# 선택: 헬스체크
@app.get("/healthz")
def healthz():
    return {"ok": True}