import { existsSync } from 'node:fs';
import { resolve } from 'node:path';
import { spawnSync } from 'node:child_process';

// 只包装本项目 Compose 命令与停止结果，不解析或打印秘密配置。
const [action, ...options] = process.argv.slice(2);
let envFile = 'infra/local/.env';
let project;
for (let i = 0; i < options.length; i += 2) {
  if (options[i] === '--env-file' && options[i + 1]) envFile = options[i + 1];
  else if (options[i] === '--project' && /^[a-z0-9][a-z0-9_-]*$/.test(options[i + 1] ?? '')) project = options[i + 1];
  else throw new Error('参数必须为 --env-file <本地配置文件> 或 --project <本项目验证名称>');
}
if (!['check', 'start', 'stop'].includes(action)) throw new Error('操作必须为 check、start 或 stop');
if (!existsSync(resolve(envFile))) {
  console.error('缺少本地基础设施环境文件；请复制 infra/local/.env.example 为 infra/local/.env 并填写专用凭据，或指定 --env-file。');
  process.exit(1);
}
const compose = ['compose', '--env-file', envFile, ...(project ? ['-p', project] : []), '-f', 'infra/local/docker-compose.yml'];
function docker(args, capture = false, explainPorts = false) {
  const result = spawnSync('docker', args, { encoding: 'utf8', stdio: explainPorts ? 'pipe' : capture ? ['ignore', 'pipe', 'inherit'] : 'inherit' });
  if (result.error) {
    console.error('Docker 命令无法执行；请检查 Docker 安装与可用性。');
    process.exit(1);
  }
  if (result.status !== 0) {
    if (explainPorts) {
      if (/invalid.*(?:hostPort|port)/i.test(result.stderr ?? '')) {
        const defaults = { POSTGRES_PORT: '15432', REDIS_PORT: '16379', RABBITMQ_PORT: '15672', RABBITMQ_MANAGEMENT_PORT: '15673' };
        // 用 Compose 自身解析定位端口，不自行解析 dotenv，不输出完整配置。
        for (const key of Object.keys(defaults)) {
          const otherPorts = Object.fromEntries(Object.entries(defaults).filter(([name]) => name !== key));
          const probe = spawnSync('docker', args, { encoding: 'utf8', env: { ...process.env, ...otherPorts }, stdio: 'pipe' });
          if (probe.status !== 0) console.error(`${key} 格式错误；请设置合法宿主端口。`);
        }
      } else console.error(result.stderr?.trim() || 'Compose 配置校验失败。');
    }
    process.exit(result.status ?? 1);
  }
  return result.stdout?.trim() ?? '';
}
docker([...compose, 'config', '--quiet'], false, true);
if (action === 'start') docker([...compose, 'up', '-d', '--wait', '--wait-timeout', '180']);
if (action === 'stop') {
  docker([...compose, 'stop']);
  const ids = docker([...compose, 'ps', '-a', '-q'], true).split(/\s+/).filter(Boolean);
  if (ids.length) {
    const states = docker(['inspect', '--format', '{{.Name}} {{.State.Status}} {{.State.ExitCode}} {{.State.OOMKilled}}', ...ids], true);
    console.log(states);
    if (states.split('\n').some((line) => {
      const [, status, exitCode, oom] = line.trim().split(/\s+/);
      return status !== 'exited' || exitCode !== '0' || oom !== 'false';
    })) {
      console.error('基础设施停止未正常完成；请核对上述容器退出码及日志，数据卷已保留。');
      process.exit(1);
    }
  } else console.log('本项目尚无容器；没有停止任何资源。');
}
