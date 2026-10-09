"""从原始命令记录生成证据索引，不修改原退出码。"""
import json
from pathlib import Path
import shlex

ROOT = Path('/Users/kimwell/work/pet-platform')
EVIDENCE = ROOT / 'docs/testing/evidence/ARCHITECTURE-REFRESH'
records = [json.loads(line) for line in (EVIDENCE / 'commands.jsonl').read_text().splitlines()]


def level(record):
    label = record['label']
    if record['exitCode'] != 0:
        return 'NOT_VERIFIED', 'FAIL（保留原失败）'
    if label == 'mini-unit':
        return 'UNIT_FIXTURE', 'PASS（技术夹具）'
    if label.startswith(('browser-', 'web-manage-', 'web-supplement-', 'web-final-runtime',
                         'mini-flat-', 'mini-final-', 'mini-navigation-real-',
                         'final-runtime-', 'backend-final-verify', 'backend-verify-')):
        return 'RUNTIME_VERIFIED', 'PASS（对应范围）'
    if label == 'web-visual':
        return 'RUNTIME_VERIFIED', 'PASS（真实公共框架布局）'
    if label in ('visual-review', 'final-visual-review'):
        return 'DOCUMENTED / RUNTIME_VERIFIED', 'PASS（人工评分 / 真实安全指标）'
    if label.startswith(('final-frozen-install', 'mini-automator-')):
        return 'RESOLVED', 'PASS（依赖安装，不等于运行验收）'
    if label in ('final-source-audit', 'final-whitespace-check', 'final-document-check', 'web-layout-completion-audit'):
        return 'DOCUMENTED', 'PASS（保全或文档检查）'
    if label == 'mini-cli-auto-first':
        return 'RESOLVED', 'PASS（打开自动化连接，不等于页面验证）'
    return 'COMPILED', 'PASS（编译或技术检查）'


def cell(value):
    return str(value).replace('|', '\\|').replace('\n', '<br>')


lines = [
    '# 架构改造检查命令与证据分级', '',
    '工作根目录：`/Users/kimwell/work/pet-platform`。按实际执行顺序记录；早期 PASS 仅代表当时对应范围，最终结论以主验收报告的最终轮为准。', '',
    '退出码非 0 全部保留为 FAIL；后续修复不改写原记录。COMPILED 的测试夹具不替代真实服务验收。视觉人工评分与真实布局指标分别标注。', '',
    '原始记录：[commands.jsonl](commands.jsonl)。汇总：[主验收报告](../../ARCHITECTURE-REFRESH-VERIFICATION.md)。', '',
    '| 轮次 | 工作目录 | 实际命令 | 退出码 | 证据等级 | 结果 / 日志 |',
    '| --- | --- | --- | --- | --- | --- |',
]
graded = []
for record in records:
    evidence_level, result = level(record)
    command = shlex.join(record['command'])
    lines.append('| ' + ' | '.join([cell(record['label']), '`' + cell(record['cwd']) + '`',
                                  '`' + cell(command) + '`', str(record['exitCode']),
                                  evidence_level, result + ' [' + record['log'] + '](' + record['log'] + ')']) + ' |')
    graded.append({**record, 'evidenceLevel': evidence_level, 'result': result})

lines += [
    '', '## 其他原始证据', '',
    '| 操作 | 工作目录 / 操作方式 | 退出码 | 等级 / 证据 |',
    '| --- | --- | --- | --- |',
    '| 临时正式后端与受限 PostgreSQL/Redis 初始化 | 根目录；各条实际命令在 JSON 中 | 逐条原值 | RUNTIME_VERIFIED；[runtime-setup.json](runtime-setup.json)，只用于隔离验收 |',
    '| STAFF 验收租户正式初始化 | 根目录；正式初始化及 API 命令在 JSON 中 | 逐条原值 | RUNTIME_VERIFIED；[staff-runtime-bootstrap.json](staff-runtime-bootstrap.json)，不放宽门店权限政策 |',
    '| 最终微信源码编译 | 冻结 GUI 普通编译，当前 iPhone 14 Pro Max；不是 shell 命令 | 不适用 | RUNTIME_VERIFIED；[AX](devtools-final-compile-ax.txt) / [截图](devtools-final-compile.png)，Errors=0；工具 Warnings=9 保留 |',
    '| 依赖实际解析 | backend verify 和冻结 pnpm install | 对应日志原值 | RESOLVED / COMPILED；[版本与保全](preservation-final.json)，不改变版本事实源 |',
    '| 未执行边界 | 本轮没有真机、生产跨源/TLS、多 OS、远程 CI、完整使用端正文或微信 code 交换 | NOT_EXECUTED | NOT_VERIFIED；原因及范围见主报告 |',
    '| 本轮收尾 | 根目录；只停止归属已校验的本轮进程组与临时 tmpfs 容器 | 最终 0 | RUNTIME_VERIFIED；[cleanup.json](cleanup.json)，四个回环端口释放、既有三容器状态保全、凭据副本删除 |',
    '', '清理早期失败来自 Docker 的 --rm 容器停止后自动移除，以及缺失对象错误文本大小写。原失败日志保留；按真实容器状态修正幂等检查后完成，未终止用户原有容器。', '',
    'SDK 的 switchTab 故障注入属于工具故障边界。最终真实 API 导航已通过，失败回调/有栈返回由独立 UNIT_FIXTURE 验证；不将夹具描述为原生微信故障复现。', '',
]
(EVIDENCE / 'CHECKS.md').write_text('\n'.join(lines))
(EVIDENCE / 'evidence-levels.json').write_text(json.dumps(graded, ensure_ascii=False, indent=2) + '\n')
print(f'已生成 {len(records)} 条原始命令索引；保留 {sum(r["exitCode"] != 0 for r in records)} 条非零退出记录。')
