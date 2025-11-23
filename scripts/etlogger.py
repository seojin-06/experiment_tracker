# etlogger.py — robust client (2025-11-11 mark-start + env/hparams)
from __future__ import annotations

__version__ = "2025-11-11-mark-start"

import os
import platform
from typing import Any, Dict, List, Optional, Tuple, Union

import requests
from datetime import datetime, timezone


class ETLogger:
    """AI Experiment Tracker Python client (Jupyter/Script friendly)."""

    def __init__(
        self,
        api_base: str,
        auth: Optional[Tuple[str, str]] = None,
        token: Optional[str] = None,
        timeout: int = 15,
        extra_headers: Optional[Dict[str, str]] = None,
        verify_ssl: Union[bool, str] = True,
    ) -> None:
        self.api_base   = api_base.rstrip("/")
        self.timeout    = timeout
        self.verify_ssl = verify_ssl

        self.session = requests.Session()
        if auth:
            self.session.auth = auth
        self.session.headers.update({"Accept": "application/json"})
        if token:
            self.session.headers.update({"Authorization": f"Bearer {token}"})
        if extra_headers:
            self.session.headers.update(extra_headers)

        self.project: Optional[Dict[str, Any]]    = None
        self.experiment: Optional[Dict[str, Any]] = None
        self.run: Optional[Dict[str, Any]]        = None
        self.run_id: Optional[str]                = None

    # --------------------------
    # Public high-level methods
    # --------------------------
    def start_run(
        self,
        project_name: str,
        experiment_name: str,
        run_name: Optional[str] = None,
        run_id: Optional[str] = None,
        run_meta: Optional[Dict[str, Any]] = None,
        *, 
        mark_started: bool = True,
        status_when_start: str = "RUNNING",
        hparams: Optional[Dict[str, Any]] = None,
        env_snapshot: Optional[Dict[str, Any]] = None,
    ) -> str:
        """Ensure project/experiment exist, then create (or attach) a run.
        If *mark_started* is True (default), PATCH the run as RUNNING with startedAt=now.
        Optionally send *hparams* and *env_snapshot* immediately after start.
        Returns the active run_id.
        """
        proj_obj = _unwrap(self._ensure_project(project_name))
        project_id = _pick_id(proj_obj)

        exp_obj = _unwrap(self._ensure_experiment(experiment_name, project_id))
        experiment_id = _pick_id(exp_obj)

        if run_id:  # attach to existing run
            self.project, self.experiment = proj_obj, exp_obj
            self.run_id = run_id
            self.run = {"id": run_id, "projectId": project_id, "experimentId": experiment_id}
        else:
            run_obj = self._try_create_run(project_id, experiment_id, run_name, (run_meta or {}))
            if not run_obj:
                raise RuntimeError("Run creation endpoint not available or returned unexpected payload.\n→ Create a run in UI/API first and call start_run(..., run_id='...').")
            run_obj = _unwrap(run_obj)
            self.project, self.experiment, self.run = proj_obj, exp_obj, run_obj
            self.run_id = _pick_id(run_obj)

        # Mark start (status RUNNING + startedAt)
        if mark_started:
            self.mark_run_started(status=status_when_start)

        # Optional: send hparams / env snapshot right away
        if hparams:
            self.log_hparams(hparams)
        if env_snapshot:
            self.log_env_snapshot(**env_snapshot)

        return self.run_id

    def start_run_by_ids(
        self,
        project_id: str,
        experiment_id: str,
        run_name: Optional[str] = None,
        run_id: Optional[str] = None,
        run_meta: Optional[Dict[str, Any]] = None,
        *, 
        mark_started: bool = True,
        status_when_start: str = "RUNNING",
        hparams: Optional[Dict[str, Any]] = None,
        env_snapshot: Optional[Dict[str, Any]] = None,
    ) -> str:
        if run_id:
            self.run_id = run_id
            self.run = {"id": run_id, "projectId": project_id, "experimentId": experiment_id}
        else:
            run_obj = self._try_create_run(project_id, experiment_id, run_name, (run_meta or {}))
            if not run_obj:
                raise RuntimeError("Run creation endpoint not available or failed.")
            run_obj = _unwrap(run_obj)
            self.run = run_obj
            self.run_id = _pick_id(run_obj)

        if mark_started:
            self.mark_run_started(status=status_when_start)
        if hparams:
            self.log_hparams(hparams)
        if env_snapshot:
            self.log_env_snapshot(**env_snapshot)
        return self.run_id

    # ---- lifecycle helpers ----
    def mark_run_started(self, *, status: str = "RUNNING", started_at: Optional[str] = None, notes: Optional[str] = None) -> None:
        """PATCH the run to reflect it has started (status RUNNING + startedAt)."""
        _require(self.run_id, "No active run_id. Call start_run(...) first.")
        raw = (status or "").strip().upper()
        allowed = {"SUCCEEDED", "FAILED", "RUNNING", "CANCELED", "PENDING"}
        if raw not in allowed:
            raw = "RUNNING"
        body = {
            "status": raw,
            "startedAt": _iso_now() if started_at is None else started_at,
        }
        if notes:
            body["notes"] = notes
        url = f"{self.api_base}/runs/{self.run_id}"
        r = self.session.patch(url, json=body, timeout=self.timeout, verify=self.verify_ssl)
        _raise_for_status(r, "mark_run_started failed")
        try:
            j = r.json()
            data = j.get("data") if isinstance(j, dict) else None
            if isinstance(data, dict):
                self.run = data
        except Exception:
            pass

    def finish_run(
        self,
        *,
        status: str = "SUCCEEDED",
        finished_at: Optional[str] = None,
        notes: Optional[str] = None,
        elapsed_ms: Optional[int] = None,
    ) -> None:
        _require(self.run_id, "No active run_id. Call start_run(...) first.")
        end_iso = _iso_now() if finished_at is None else finished_at

        raw = (status or "").strip().upper()
        aliases = {
            "FINISHED": "SUCCEEDED",
            "SUCCESS": "SUCCEEDED",
            "OK": "SUCCEEDED",
            "DONE": "SUCCEEDED",
            "ABORTED": "CANCELED",
            "CANCELLED": "CANCELED",
        }
        status_enum = aliases.get(raw, raw)
        allowed = {"SUCCEEDED", "FAILED", "RUNNING", "CANCELED", "PENDING"}
        if status_enum not in allowed:
            raise ValueError(f"Invalid status '{status}'. Allowed: {sorted(allowed)}")

        ms = elapsed_ms
        try:
            started_iso = None
            if isinstance(self.run, dict):
                started_iso = self.run.get("startedAt") or self.run.get("startTime") or self.run.get("started_at")
            if ms is None:
                if not started_iso:
                    rr = self.session.get(f"{self.api_base}/runs/{self.run_id}",
                                          timeout=self.timeout, verify=self.verify_ssl,
                                          headers={"Accept": "application/json", **self.session.headers})
                    if 200 <= rr.status_code < 300:
                        j = _unwrap_json(rr)
                        if isinstance(j, dict):
                            started_iso = j.get("startedAt") or j.get("startTime")
                ms = _ms_between(started_iso, end_iso)
        except Exception:
            pass

        body: Dict[str, Any] = {"status": status_enum, "finishedAt": end_iso}
        if ms is not None:
            body["elapsedMs"] = ms
        if notes:
            body["notes"] = notes

        url = f"{self.api_base}/runs/{self.run_id}"
        r = self.session.patch(url, json=body, timeout=self.timeout, verify=self.verify_ssl)
        _raise_for_status(r, "finish_run failed")
        try:
            j = r.json()
            data = j.get("data") if isinstance(j, dict) else None
            if isinstance(data, dict):
                self.run = data
        except Exception:
            pass

    # --------------------------
    # Logging
    # --------------------------
    def log(self, metrics: Dict[str, Union[int, float]], step: int, recorded_at: Optional[str] = None) -> None:
        _require(self.run_id, "No active run_id. Call start_run(...) first.")
        rows: List[Dict[str, Any]] = []
        for k, v in metrics.items():
            item: Dict[str, Any] = {"key": str(k), "step": int(step), "value": float(v)}
            if recorded_at:
                item["recordedAt"] = recorded_at
            rows.append(item)
        self._post_metrics(self.run_id, rows)

    def log_batch(self, rows: List[Dict[str, Any]]) -> None:
        _require(self.run_id, "No active run_id. Call start_run(...) first.")
        for r in rows:
            if not all(k in r for k in ("key", "step", "value")):
                raise ValueError(f"Invalid row (needs key/step/value): {r}")
        casted = [{"key": str(r["key"]), "step": int(r["step"]), "value": float(r["value"])} | ({k: r[k] for k in ("recordedAt",) if k in r}) for r in rows]
        self._post_metrics(self.run_id, casted)

    def log_hparams(self, params: dict, *, source: str = "CLI") -> None:
        _require(self.run_id, "No active run_id. Call start_run(...) first.")
        url = f"{self.api_base}/runs/{self.run_id}/hyperparams"

        def infer_type(v):
            if isinstance(v, bool): return "BOOL"
            if isinstance(v, (int, float)): return "NUMBER"
            if isinstance(v, (dict, list)): return "JSON"
            return "STRING"

        items = []
        for k, v in params.items():
            items.append({
                "key": str(k),
                "valueType": infer_type(v),
                "value": str(v),
                "source": source
            })

        r = self.session.post(url, json=items, timeout=self.timeout, verify=self.verify_ssl)
        _raise_for_status(r, "log_hparams failed")

    def log_env_snapshot(self,
                         *,
                         os_name: str = None,
                         os_version: str = None,
                         python: str = None,
                         commit: str = None,
                         gpu: str = None,
                         libraries: Optional[Dict[str, str]] = None,
                         env_vars: Optional[Dict[str, str]] = None,
                         ) -> None:
        _require(self.run_id, "No active run_id. Call start_run(...) first.")
        url = f"{self.api_base}/runs/{self.run_id}/env"

        body: Dict[str, Any] = {}
        if os_name is not None:
            body["osName"] = os_name
        if os_version is not None:
            body["osVersion"] = os_version
        if python is not None:
            body["pythonVersion"] = python
        if commit is not None:
            body["commitHash"] = commit

        env_payload: Dict[str, str] = {}
        if gpu is not None:
            env_payload["GPU"] = str(gpu)

        if env_vars:
            for k, v in env_vars.items():
                env_payload[str(k)] = str(v)

        if env_payload:
            body["envVars"] = env_payload

        if libraries:
            body["libraries"] = {str(k): str(v) for k, v in libraries.items()}

        r = self.session.post(url, json=body, timeout=self.timeout, verify=self.verify_ssl)
        _raise_for_status(r, "log_env_snapshot failed")

    # convenience: capture simple env automatically
    def auto_capture_and_log_env(self, *, include_libs: Optional[List[str]] = None, env_whitelist: Optional[List[str]] = None, commit: Optional[str] = None, gpu: Optional[str] = None) -> None:
        libs: Dict[str, str] = {}
        if include_libs:
            for mod in include_libs:
                try:
                    m = __import__(mod)
                    v = getattr(m, "__version__", None)
                    if v:
                        libs[mod] = str(v)
                except Exception:
                    pass
        envs: Dict[str, str] = {}
        if env_whitelist:
            for k in env_whitelist:
                if k in os.environ:
                    envs[k] = os.environ[k]

        self.log_env_snapshot(
            os_name=platform.system(),          # ✅ 이름/버전 분리
            os_version=platform.release(),
            python=platform.python_version(),
            commit=commit,
            gpu=gpu or os.environ.get("GPU"),
            libraries=libs if libs else None,
            env_vars=envs if envs else None,
        )

    def upload_artifact(
        self,
        path: str,
        artifact_type: str = "OTHER",
        field_name: str = "file",
        extra_fields: Optional[Dict[str, str]] = None,
    ) -> Dict[str, Any]:
        _require(self.run_id, "No active run_id. Call start_run(...) first.")
        url = f"{self.api_base}/runs/{self.run_id}/artifacts"
        fields = {"type": artifact_type}
        if extra_fields:
            fields.update({str(k): str(v) for k, v in extra_fields.items()})
        with open(path, "rb") as f:
            files = {field_name: (os.path.basename(path), f)}
            r = self.session.post(url, files=files, data=fields, timeout=self.timeout, verify=self.verify_ssl)
        _raise_for_status(r, "Artifact upload failed")
        return _unwrap_json(r)

    # --------------------------
    # Internals: Project/Experiment
    # --------------------------
    def _ensure_project(self, project_name: str) -> Dict[str, Any]:
        body = {"projectName": project_name, "description": "Auto-created via ETLogger"}
        r = self.session.post(f"{self.api_base}/projects", json=body, timeout=self.timeout, verify=self.verify_ssl)
        if 200 <= r.status_code < 300:
            return _unwrap_json(r)
        if r.status_code == 409:
            found = _deep_find_by_name(self.session, self.api_base, "projects", project_name, timeout=self.timeout, verify=self.verify_ssl)
            if found:
                return found
            raise RuntimeError(f"Project exists but not listed by name: '{project_name}'.")
        _raise_for_status(r, "Project create failed")

    def _ensure_experiment(self, experiment_name: str, project_id: str) -> Dict[str, Any]:
        body = {"experimentName": experiment_name, "projectId": project_id}
        r = self.session.post(f"{self.api_base}/experiments", json=body, timeout=self.timeout, verify=self.verify_ssl)
        if 200 <= r.status_code < 300:
            return _unwrap_json(r)
        if r.status_code == 409:
            for url in (f"{self.api_base}/experiments?projectId={project_id}&size=1000", f"{self.api_base}/experiments?projectId={project_id}"):
                rr = self.session.get(url, timeout=self.timeout, verify=self.verify_ssl)
                if rr.ok:
                    items = _as_list_json(rr)
                    name_l = experiment_name.lower()
                    for obj in items:
                        nm = obj.get("experimentName") or obj.get("name")
                        if isinstance(nm, str) and nm.lower() == name_l:
                            return obj
            found = _deep_find_by_name(self.session, self.api_base, "experiments", experiment_name, timeout=self.timeout, verify=self.verify_ssl)
            if found:
                return found
            raise RuntimeError(f"Experiment exists but not listed by name: '{experiment_name}'.")
        _raise_for_status(r, "Experiment create failed")

    # --------------------------
    # Internals: Run & Metrics
    # --------------------------
    def _try_create_run(self, project_id: str, experiment_id: str, run_name: Optional[str], run_meta: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        url = f"{self.api_base}/runs"
        body: Dict[str, Any] = {"projectId": project_id, "experimentId": experiment_id}
        if run_name:
            body["runName"] = run_name
        if run_meta:
            body.update(run_meta)
        r = self.session.post(url, json=body, timeout=self.timeout, verify=self.verify_ssl)
        if 200 <= r.status_code < 300:
            return _unwrap_json(r)
        if r.status_code in (400, 403, 404, 405):
            return None
        return None

    def _post_metrics(self, run_id: str, payload: List[Dict[str, Any]]) -> None:
        url  = f"{self.api_base}/runs/{run_id}/metrics"
        sess = self.session
        kw   = dict(timeout=self.timeout, verify=self.verify_ssl)

        grouped: Dict[int, Dict[str, float]] = {}
        for r in payload:
            step = int(r["step"])
            key  = str(r["key"]).replace("/", ".")
            val  = float(r["value"])
            bucket = grouped.setdefault(step, {})
            bucket[key] = val

        body = [{"step": s, "metrics": m} for s, m in sorted(grouped.items(), key=lambda x: x[0])]
        r = sess.post(url, json=body, **kw)
        _raise_for_status(r, "Metrics log failed")
        try:
            j = r.json()
            if isinstance(j, dict) and "success" in j and j.get("success") is not True:
                raise RuntimeError(f"Metrics 2xx but success=false: {j}")
        except Exception:
            pass


# --------------------------
# Helpers
# --------------------------
def _require(cond: Any, msg: str) -> None:
    if not cond:
        raise RuntimeError(msg)

def _safe_json(resp: requests.Response) -> Any:
    try:
        return resp.json()
    except Exception:
        return resp.text

def _unwrap(obj: Any) -> Any:
    if isinstance(obj, dict) and "data" in obj and ("success" in obj or "error" in obj):
        return obj.get("data")
    return obj

def _unwrap_json(resp: requests.Response) -> Any:
    return _unwrap(_safe_json(resp))

def _as_list(obj: Any) -> List[Any]:
    obj = _unwrap(obj)
    if isinstance(obj, list):
        return obj
    if isinstance(obj, dict):
        for key in ("content", "items", "results", "list", "projects", "records", "data"):
            if isinstance(obj.get(key), list):
                return obj[key]
    return []

def _as_list_json(resp: requests.Response) -> List[Any]:
    return _as_list(_safe_json(resp))

def _maybe_id(d: Any) -> Optional[str]:
    if not isinstance(d, dict):
        return None
    for k in ("id", "runId", "projectId", "experimentId"):
        if k in d and d[k]:
            return str(d[k])
    return None

def _pick_id(d: Dict[str, Any]) -> str:
    v = _maybe_id(d)
    if not v:
        raise KeyError(f"id not found in object (keys: {list(d.keys()) if isinstance(d, dict) else type(d)})")
    return v

def _raise_for_status(resp: requests.Response, msg: str) -> None:
    if resp is None:
        raise RuntimeError(f"{msg}: no response")
    if 200 <= resp.status_code < 300:
        return
    body = _safe_json(resp)
    raise RuntimeError(f"{msg}: [{resp.status_code}] {body}")

def _deep_find_by_name(session: requests.Session, base_url: str, resource: str, name: str,
                       timeout: int = 15, verify: Union[bool, str] = True) -> Optional[Dict[str, Any]]:
    urls = [
        f"{base_url}/{resource}?name={name}",
        f"{base_url}/{resource}?q={name}",
        f"{base_url}/{resource}?keyword={name}",
        f"{base_url}/{resource}?search={name}",
        f"{base_url}/{resource}?page=0&size=1000",
        f"{base_url}/{resource}",
    ]
    target = (name or "").lower()

    def scan(obj: Any) -> Optional[Dict[str, Any]]:
        obj = _unwrap(obj)
        if isinstance(obj, dict):
            nm = obj.get("projectName") or obj.get("experimentName") or obj.get("name")
            if isinstance(nm, str) and nm.lower() == target:
                return obj
            for v in obj.values():
                found = scan(v)
                if found:
                    return found
        elif isinstance(obj, list):
            for it in obj:
                found = scan(it)
                if found:
                    return found
        return None

    for url in urls:
        r = session.get(url, timeout=timeout, verify=verify)
        if r.status_code >= 400:
            continue
        found = scan(_safe_json(r))
        if found:
            return found
    return None

# --------------------------
# Time helpers
# --------------------------
def _iso_now() -> str:
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")

def _parse_iso(ts: str) -> Optional[datetime]:
    if not ts:
        return None
    try:
        if ts.endswith("Z"):
            ts = ts[:-1] + "+00:00"
        return datetime.fromisoformat(ts)
    except Exception:
        return None

def _ms_between(start_iso: Optional[str], end_iso: Optional[str]) -> Optional[int]:
    s = _parse_iso(start_iso) if start_iso else None
    e = _parse_iso(end_iso) if end_iso else None
    if not s or not e:
        return None
    return int((e - s).total_seconds() * 1000)
