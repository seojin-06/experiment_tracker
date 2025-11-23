import argparse, time, math, random, requests
from datetime import datetime, timezone, timedelta

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--api", required=True)
    p.add_argument("--run", required=True)
    p.add_argument("--steps", type=int, default=100)
    args = p.parse_args()

    rows = []
    for step in range(1, args.steps + 1):
        loss = round(1.0 / (step ** 0.5) + random.uniform(-0.01, 0.01), 5)
        metrics = {"train/loss": loss}
        if step % 10 == 0:
            acc = round(0.60 + step * 0.003 + random.uniform(-0.005, 0.005), 5)
            metrics["val/acc"] = acc

        rows.append({"step": step, "metrics": metrics})

    url = f"{args.api.rstrip('/')}/runs/{args.run}/metrics"
    r = requests.post(url, json=rows, headers={"Content-Type": "application/json"})  # 인증 없이
    if not r.ok:
        print("Status:", r.status_code)
        print("Body  :", r.text[:1000])
    r.raise_for_status()
    print("OK:", len(rows), "rows")

if __name__ == "__main__":
    main()