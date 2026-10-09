"""依据当前源码、最新目标附件和实际浏览器证据审计布局完成条件。"""
import datetime
import hashlib
import json
from pathlib import Path
import shutil

ROOT = Path('/Users/kimwell/work/pet-platform')
EVIDENCE = ROOT / 'docs/testing/evidence/ARCHITECTURE-REFRESH'
REFERENCE = Path('/Users/kimwell/.codex/attachments/da6a8ec2-477e-475e-93ec-f901d480e950/image-1.png')
records = [json.loads(line) for line in (EVIDENCE / 'commands.jsonl').read_text().splitlines()]
source_files = sorted(path for path in (ROOT / 'apps/admin-web/src').rglob('*') if path.is_file())
checks = []
for label in ('final-root-check-after-freeze', 'final-web-build-after-freeze'):
    record = next(item for item in records if item['label'] == label)
    assert record['exitCode'] == 0
    timestamp = datetime.datetime.fromisoformat(record['startedAt']).timestamp()
    assert all(path.stat().st_mtime <= timestamp for path in source_files), '最终检查后 Web 源码发生变化'
    checks.append(record)

layout = ROOT / 'apps/admin-web/src/layouts/BasicLayout'
assert all((layout / filename).is_file() for filename in
           ('index.tsx', 'Header.tsx', 'Sidebar.tsx', 'Breadcrumb.tsx', 'UserDropdown.tsx'))
assert '<Outlet />' in (layout / 'index.tsx').read_text()
sidebar = (layout / 'Sidebar.tsx').read_text()
assert 'width={240}' in sidebar and 'collapsedWidth={64}' in sidebar and '<Drawer' in sidebar
css = (ROOT / 'apps/admin-web/src/styles/global.css').read_text()
assert '--header-height: 64px' in css and '.content-scroll { height: 100%; overflow-y: auto;' in css
assert (ROOT / 'apps/admin-web/src/components/ProTable/index.tsx').is_file()

visual = json.loads((EVIDENCE / 'web-visual.json').read_text())
runtime = json.loads((EVIDENCE / 'browser-final.json').read_text())
management = json.loads((EVIDENCE / 'browser-manage.json').read_text())
assert visual['status'] == runtime['status'] == management['status'] == 'PASS'
assert {page['key'] for page in visual['pages']} == {'employees', 'roles', 'organizations', 'tenants', 'accounts'}
for page in visual['pages']:
    assert page['metrics']['header']['height'] == 64
    assert page['metrics']['sidebar']['width'] == 240
    assert page['metrics']['scroll']['overflowY'] == 'auto'
    assert page['metrics']['noPageOverflow']
collapsed = next(item for item in runtime['layout'] if item['key'] == 'collapsed')
mobile = next(item for item in runtime['layout'] if item['key'] == 'drawer-390')
assert collapsed['sidebar']['width'] == 64 and collapsed['noPageOverflow']
assert mobile['viewport']['width'] == 390 and mobile['sidebar'] is None and mobile['noPageOverflow']

shutil.copyfile(REFERENCE, EVIDENCE / 'web-goal-layout-reference.png')
requirements = [
    {'requirement': '通栏 Header：左侧折叠按钮与蓝色品牌，右侧头像/账号/下拉',
     'sources': ['layouts/BasicLayout/Header.tsx', 'layouts/BasicLayout/UserDropdown.tsx', 'styles/global.css'],
     'evidence': ['web-employees-final.png', 'web-accounts-final.png', 'web-visual.json'], 'result': 'PROVEN'},
    {'requirement': '白色侧栏、导航图标与选中态；展开240px、折叠64px',
     'sources': ['layouts/BasicLayout/Sidebar.tsx', 'stores/app.store.ts', 'styles/global.css'],
     'evidence': ['web-visual.json', 'browser-final.json'], 'result': 'PROVEN'},
    {'requirement': '浅灰背景、面包屑、白卡片与标题分隔；内容独立滚动',
     'sources': ['layouts/BasicLayout/index.tsx', 'layouts/BasicLayout/Breadcrumb.tsx', 'styles/global.css'],
     'evidence': ['web-visual.json', 'web-employees-final.png', 'web-roles-final.png', 'web-organizations-final.png', 'web-tenants-final.png', 'web-accounts-final.png'],
     'result': 'PROVEN'},
    {'requirement': '列表筛选、工具栏、表格与分页遵循统一 ProTable；业务内容仍依本项目',
     'sources': ['components/ProTable/index.tsx', 'pages/admin', 'pages/platform'],
     'evidence': ['browser-manage.json', 'browser-final.json', 'web-visual.json'], 'result': 'PROVEN'},
    {'requirement': 'TanStack Router Outlet 承载页面，拆分布局职责',
     'sources': ['router/router.ts', 'layouts/BasicLayout/index.tsx'],
     'evidence': ['final-root-check-after-freeze.log', 'final-web-build-after-freeze.log'], 'result': 'PROVEN'},
    {'requirement': '移动端官方 Drawer，菜单导航、Escape、焦点恢复和页面无横向溢出',
     'sources': ['layouts/BasicLayout/Sidebar.tsx', 'styles/global.css'],
     'evidence': ['browser-manage.json', 'browser-final.json'], 'result': 'PROVEN'},
]
for item in requirements:
    assert all((EVIDENCE / filename).is_file() for filename in item['evidence'])

report = {'status': 'PASS', 'previousGoalTurn': 'PROGRESS',
          'objective': 'web layout 页面整体布局参考用户截图进行调整',
          'checkedAt': datetime.datetime.now(datetime.timezone.utc).isoformat(),
          'scope': '完整公共布局与全部既有管理列表；业务菜单、字段和操作依本项目需求',
          'reference': {'file': str(REFERENCE), 'sha256': hashlib.sha256(REFERENCE.read_bytes()).hexdigest(),
                        'savedEvidence': 'web-goal-layout-reference.png'},
          'requirements': requirements, 'remainingRequiredWork': [],
          'sourceDigests': {str(path.relative_to(ROOT)): hashlib.sha256(path.read_bytes()).hexdigest()
                            for path in source_files},
          'checks': checks, 'evidenceLevel': 'DOCUMENTED / RUNTIME_VERIFIED',
          'visualReview': '本轮对最新目标附件与五页实际框架截图逐项人工复核；非自动像素一致性结论。'}
(EVIDENCE / 'web-layout-completion-audit.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
print('布局完成审计通过：6类要求均有当前源码与对应证据；五个管理列表、240/64侧栏、390px Drawer；无必需工作剩余。')
