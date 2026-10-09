"""只读核对本轮浏览器验收的容器、端口和临时凭据文件，不停止或删除其他资源。"""
import datetime
import json
import pathlib
import re
import socket
import subprocess

root = pathlib.Path(__file__).resolve().parents[4]
evidence = pathlib.Path(__file__).resolve().parent
containers, ports = [], {18098}
for name in ("browser-backend-runtime", "browser-draft-runtime"):
    body = (evidence / (name + ".log")).read_text()
    for container_id in re.findall(r"Container .* is starting: ([0-9a-f]{64})", body):
        result = subprocess.run(
            ["docker", "inspect", "--format", "{{.State.Running}}", container_id],
            capture_output=True, text=True,
        )
        containers.append({
            "runtime": name, "containerId": container_id,
            "removed": result.returncode != 0 and "no such" in result.stderr.lower(),
            "running": result.stdout.strip() == "true", "inspectionExit": result.returncode,
        })
    ports.update(int(p) for p in re.findall(r"Tomcat started on port (\d+)", body))
listeners = []
for port in sorted(ports):
    with socket.socket() as connection:
        connection.settimeout(1)
        listeners.append({"port": port, "acceptsConnection": connection.connect_ex(("127.0.0.1", port)) == 0})
private_files = [p.name for p in (root / ".local-data/b01").glob("*") if "credential" in p.name or "token" in p.name]
result = {
    "checkedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
    "containers": containers, "ports": listeners,
    "privateCredentialFilesRemaining": private_files,
    "actions": "只读检查专用ID与端口；本轮没有停机、删除或重建既有卷/进程",
}
result["result"] = "PASS" if all(c["removed"] for c in containers) and not any(p["acceptsConnection"] for p in listeners) and not private_files else "FAIL"
(evidence / "cleanup.json").write_text(json.dumps(result, ensure_ascii=False, indent=2))
print(json.dumps(result, ensure_ascii=False))
assert result["result"] == "PASS"
