# AI Experiment Tracker
## 1. 프로젝트 개요
AI Experiment Tracker는 머신러닝 실험 과정에서 생성되는 지표(Metrics), 하이퍼파라미터, 환경 정보, 모델 파일(Artifacts) 등을
자동으로 수집·저장하고 시각화하는 경량 자가 호스팅 실험 관리 플랫폼이다. Python 학습 코드와 직접 연동되는 전용 SDK(etlogger), FastAPI 기반 AI 분석 기능, Spring Boot + PostgreSQL 기반 백엔드를 통해
실험 자동화 + 로그 수집 + 분석 + 추천을 제공한다.

## 2. 주요 기능
### 1) 실험 관리 (Project → Experiment → Run)
- Python SDK로 프로젝트/실험/런 자동 생성
- Run 상태 관리(Start/Running/Finish)
- Seed·Notes 설정
### 2) Metrics 실시간 수집·시각화
- Bulk Logging API 지원
- Step 기반 로그 저장 (train/loss, val/acc 등)
- 학습 곡선(Line Chart) 및 테이블 시각화
### 3) Hyperparameters 자동 기록
- key/type/value/source 단위로 저장
- CLI/config/autotuner 출처 구분
### 4) Environment Snapshot
- OS, Python, CUDA, 라이브러리 버전 수집
### 5) Run Summary
- Best Accuracy / Best Epoch
- Last Step / Epoch
- Predicted Final Accuracy
- Early Stop Epoch
### 6) Artifacts 관리
- 모델 파일 업로드/다운로드/삭제
- 파일 크기, 체크섬, 업로드 시각 기록
### 7) AI 기반 실험 분석 기능
- RUN_SELECTION: 가장 유망한 Run 추천
- EARLY_STOP_HINT: 학습 조기 종료 시점 예측
- HYPERPARAM_SUGGESTION: 다음 실험 추천 파라미터
