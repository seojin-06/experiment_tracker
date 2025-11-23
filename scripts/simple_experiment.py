import time
import math
import requests
from etlogger import ETLogger

# -----------------------------
# 기본 설정
# -----------------------------
API_BASE = "http://localhost:8080/api"
AUTH = None   # BasicAuth 쓰면 ("user", "pass") 로 설정

PROJECT_NAME   = "DemoProject"
EXPERIMENT_NAME = "simple-ai-experiment"

def log_dataset_ref(run_id: str):

    url = f"{API_BASE}/runs/{run_id}/datasets"

    payload = {
        "name": "CIFAR-10",
        "version": "v1",
        "uri": "file:///datasets/cifar10",
        "checksum": None,
        "sizeBytes": None,
        "description": "Demo dataset reference from simple_experiment.py"
    }
    
    print(f"[INFO] posting DatasetRef to {url}")
    
    r = requests.post(url, json=payload, auth=AUTH)
    if r.status_code >= 400:
        print("[WARN] dataset ref logging failed:", r.status_code, r.text)
    else:
        print("[INFO] dataset ref logged OK:", r.json())

def main():
    # 1) 로거 초기화
    logger = ETLogger(api_base=API_BASE, auth=AUTH)

    # 2) Run 생성
    run_name = f"demo-run-{int(time.time())}"
    run_id = logger.start_run(PROJECT_NAME, EXPERIMENT_NAME, run_name=run_name)
    print(f"[INFO] created run_id = {run_id}")

    # 3) 하이퍼파라미터 로깅
    hparams = {
        "model": "MLP",
        "lr": 0.001,
        "batch_size": 64,
        "epochs": 20,
        "optimizer": "Adam",
    }
    logger.log_hparams(hparams)
    print("[INFO] logged hyperparameters:", hparams)
    
    # 4) DatasetRef 로깅 (데이터셋 참조 정보)
    try:
        log_dataset_ref(run_id)
    except Exception as e:
        print("[WARN] dataset ref logging failed with exception:", e)

    # 5) 환경 스냅샷 자동 수집 & 전송
    try:
        logger.auto_capture_and_log_env(
            include_libs=["torch", "numpy"],
            env_whitelist=["CUDA_VISIBLE_DEVICES", "GPU"],
            commit=None,       # git commit 있으면 문자열 넣어도 됨
            gpu=None           # GPU 이름 직접 넘기거나 env에서 읽게 둘 수 있음
        )
        print("[INFO] logged environment snapshot (auto capture)")
    except Exception as e:
        print("[WARN] env snapshot logging failed:", e)

    # 6) 가짜 학습 루프 돌면서 메트릭 로깅
    print("[INFO] start fake training loop...")
    base_val_acc = 0.70

    for epoch in range(1, 21):
        # 가짜 loss/acc 생성
        train_loss = 1.0 / math.sqrt(epoch) + 0.02 * math.sin(epoch)
        val_acc = base_val_acc + 0.02 * math.log(epoch + 1)

        metrics = {
            "train/loss": float(f"{train_loss:.5f}"),
            "val/acc":    float(f"{val_acc:.5f}")
        }

        logger.log(metrics, step=epoch)
        print(f"[METRIC] epoch={epoch}  train/loss={metrics['train/loss']:.5f}  val/acc={metrics['val/acc']:.5f}")

        time.sleep(0.2)  # 너무 빨리 끝나면 재미없으니까 살짝 딜레이

    print("[INFO] training loop finished.")

    # 7) Run 종료(mark finish)
    try:
        # etlogger에 finish_run 이 있다고 가정
        logger.finish_run(status="SUCCEEDED")
        print("[INFO] run marked as SUCCEEDED.")
    except AttributeError:
        # 만약 finish_run 메서드가 없다면, 그냥 경고만 찍고 넘어감
        print("[WARN] ETLogger.finish_run(...) is not implemented in this version.")
    except Exception as e:
        logger.finish_run(status="FAILED")
        print("[WARN] finish_run failed:", e)

if __name__ == "__main__":
    main()
