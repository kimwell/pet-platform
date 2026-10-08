"""复核本轮源文件、原成果保全、链接与秘密特征，不作为业务验收。"""
from pathlib import Path
import hashlib
import json
import os
import re
import subprocess
import sys

root = Path(__file__).resolve().parents[4]
evidence = root / "docs/testing/evidence/P02-01"
# 首次运行先建立待计算的输出文件，避免引用审计与自身产物循环依赖。
for output in ["final-audit.json", "change-summary.json"]:
    path = evidence / output
    if not path.exists():
        path.write_text(json.dumps({"status": "NOT_EXECUTED", "reason": "本次审计尚未完成"}, ensure_ascii=False) + "\n")
baseline = json.loads((evidence / "inventory-before.json").read_text())
before = {row["path"]: row for row in baseline}
allowed = {
    "AGENTS.md", "README.md", "docs/architecture/PROJECT-STRUCTURE.md",
    "docs/architecture/TECHNICAL-BASELINE.md", "docs/architecture/CONFIGURATION.md",
    "docs/development/VERSION-MATRIX.md", "docs/development/ROADMAP.md",
    "docs/development/DECISION-LOG.md", "docs/testing/ACCEPTANCE-MATRIX.md",
}
skipped = {".git", "node_modules", "dist", "target", "coverage", ".local-data", "miniprogram_npm", ".cache", ".vite"}
current = {}
for directory, dirs, files in os.walk(root):
    dirs[:] = sorted(d for d in dirs if d not in skipped and not (Path(directory) / d).is_symlink())
    for name in sorted(files):
        path = Path(directory) / name
        if path.is_symlink():
            continue
        rel = str(path.relative_to(root))
        current[rel] = {"path": rel, "size": path.stat().st_size, "sha256": hashlib.sha256(path.read_bytes()).hexdigest()}
deleted = sorted(set(before) - set(current))
changed = sorted(path for path in before if path in current and before[path]["sha256"] != current[path]["sha256"])
assert not deleted, f"既有文件缺失：{deleted}"
assert set(changed) <= allowed, f"非授权文件发生变化：{set(changed) - allowed}"
ui = [row for row in baseline if row["path"].startswith("ui/")]
assert all(current[row["path"]]["sha256"] == row["sha256"] for row in ui)
history = [row for row in baseline if row["path"].startswith("docs/testing/evidence/P01-")]
assert all(current[row["path"]]["sha256"] == row["sha256"] for row in history)
assert not (root / "enterprise-app-scaffold").exists()
assert not (root / ".delivery-os").exists()
assert not (root / "apps/backend/src/main/resources/db").exists()

bad_links = []
for path in [root / "README.md", root / "AGENTS.md", *root.glob("docs/**/*.md"), root / "infra/local/README.md", root / "packages/api-contracts/README.md"]:
    content = path.read_text()
    for link in re.findall(r"\]\(([^)]+)\)", content):
        if re.match(r"(?:https?://|#|mailto:)", link):
            continue
        target = link.split("#")[0].strip("<>")
        if target and not (path.parent / target).exists():
            bad_links.append({"path": str(path.relative_to(root)), "target": target})
assert not bad_links, f"失效本地引用：{bad_links}"

new = sorted(set(current) - set(before))
findings = []
patterns = [
    re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----"),
    re.compile(r"\bAKIA[0-9A-Z]{16}\b"),
    re.compile(r"\bgh[pousr]_[A-Za-z0-9]{30,}\b"),
    re.compile(r"\bsk-(?:proj-)?[A-Za-z0-9_-]{30,}\b"),
]
for rel in new + changed:
    if rel.endswith((".jpg", ".png", ".gz")) or rel.endswith(("audit-reproduction.py", "check-repository.mjs")):
        continue
    content = (root / rel).read_text(errors="replace")
    if any(pattern.search(content) for pattern in patterns):
        findings.append(rel)
assert not findings, f"发现秘密特征，仅列文件不传播值：{findings}"
for rel in ["apps/backend/.env.example", "apps/admin-web/.env.example", "infra/local/.env.example"]:
    assert (root / rel).exists()
    for line in (root / rel).read_text().splitlines():
        if "PASSWORD=" in line or "DEFAULT_PASS=" in line:
            assert not line.split("=", 1)[1], f"示例密码必须留空：{rel}"
assert json.loads((root / "apps/wechat-miniprogram/project.config.json").read_text())["appid"] == ""
assert json.loads((root / "apps/wechat-miniprogram/project.private.config.json.example").read_text())["appid"] == ""

whitespace = []
for rel in new + changed:
    if rel.startswith("docs/testing/evidence/") or rel.endswith("mvnw.cmd"):
        continue
    result = subprocess.run(["git", "diff", "--no-index", "--check", "--", "/dev/null", rel], cwd=root, capture_output=True, text=True)
    if result.stdout or result.stderr:
        whitespace.append(rel)
assert not whitespace, f"文件差异存在空白问题：{whitespace}"

summary = {
    "cwd": str(root), "command": [sys.executable, str(Path(__file__).resolve())],
    "exit_code": 0, "status": "PASS", "level": "DOCUMENTED",
    "baseline_files": len(before), "unchanged_baseline_files": len(before) - len(changed),
    "ui_files_unchanged": len(ui), "p01_evidence_files_unchanged": len(history),
    "changed_baseline_files": changed, "deleted_files": deleted,
    "invalid_local_links": bad_links, "secret_feature_findings": findings,
    "new_file_count": len(new), "business_scope": "无正式业务Controller/CRUD/迁移/会话/附件/Outbox/生成假DTO",
    "boundary": "源文件及文档审计，不替代运行或业务验收；秘密检查结合人工增量核对。",
}
(evidence / "change-summary.json").write_text(json.dumps({
    "modified": [current[path] for path in changed],
    "added": [current[path] for path in new if not path.startswith("docs/testing/evidence/P02-01/")],
    "evidence_added": [path for path in new if path.startswith("docs/testing/evidence/P02-01/")],
    "evidence_hashes": "EVIDENCE-MANIFEST.json 逐文件记录；避免递归自引用的过期哈希",
    "deleted": deleted,
}, ensure_ascii=False, indent=2) + "\n")
(evidence / "final-audit.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n")
print(json.dumps(summary, ensure_ascii=False))
