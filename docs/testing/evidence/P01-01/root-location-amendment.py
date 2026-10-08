from pathlib import Path
import hashlib
import json

root = Path('/Users/kimwell/work/pet-platform')
prior = Path('/Users/kimwell/work/enterprise-app-scaffold')
stage = Path('/tmp/enterprise-scaffold-root-correction-path.txt').read_text().strip()
stage = Path(stage)
ev_rel = Path('docs/testing/evidence/P01-01')
required = json.loads((stage / 'conflict-check.json').read_text())['required_files']

def replace_once(text, old, new):
    if text.count(old) != 1:
        raise RuntimeError('Unexpected source text: ' + old[:100])
    return text.replace(old, new, 1)

docs = {name: (prior / name).read_text() for name in required}
docs['AGENTS.md'] = replace_once(docs['AGENTS.md'],
    '本规则只适用于 enterprise-app-scaffold。它是独立工程，',
    '本规则适用于当前项目根目录 `/Users/kimwell/work/pet-platform`。所有工程与文档直接在该根目录创建或更新，不创建 `enterprise-app-scaffold` 子目录，不将现有成果整体移动；修改前检查冲突并保留既有 `ui/`。本项目独立建立工程基线，')
docs['AGENTS.md'] = replace_once(docs['AGENTS.md'],
    '后端基础包暂定 `com.company.scaffold`，未来生成工具必须支持替换 package、项目名和工程标识。',
    '当前后端基础包固定为 `com.pet.platform`，源码目录固定为 `apps/backend/src/main/java/com/pet/platform/`，启动类固定为 `com.pet.platform.Application`。后续模块均位于该基础包下：`com.pet.platform.shared`、`com.pet.platform.platform`、`com.pet.platform.identity`、`com.pet.platform.customeridentity`、`com.pet.platform.attachment`、`com.pet.platform.audit`、`com.pet.platform.notification`、`com.pet.platform.modules`。这些是规划约束，本轮不创建源码目录或启动类；未来初始化工具生成其他项目时可支持显式替换 package、项目名和工程标识，不能改变当前项目的固定包名。')

docs['README.md'] = replace_once(docs['README.md'],
    '工程标识暂定：`enterprise-app-scaffold`。',
    '模板工程标识暂定：`enterprise-app-scaffold`，该标识不代表需要创建同名目录。')
docs['README.md'] = replace_once(docs['README.md'],
    '本轮位置由用户明确选择为 `/Users/kimwell/work/enterprise-app-scaffold`；原目录 `/Users/kimwell/work/pet-platform` 的现有 UI 成果保留。本项目尚未初始化 Git，不存在分支或提交；不会将原项目内容复制成脚手架业务。',
    '项目根目录固定为当前工作目录 `/Users/kimwell/work/pet-platform`；所有工程和文档直接在该根目录创建或更新，不创建 `enterprise-app-scaffold` 子目录。已有 `ui/` 成果保留原位，未整体移动或覆盖。本项目尚未初始化 Git，不存在分支或提交。先前同级目录中的 P01-01 文档属于历史来源，当前有效规范以本根目录文件为准。\n\n后端基础包固定为 `com.pet.platform`，源码目录为 `apps/backend/src/main/java/com/pet/platform/`，启动类为 `com.pet.platform.Application`。模块与三端结构见规划文档；本轮不创建这些工程目录、启动类或业务实现。')

docs['docs/architecture/PROJECT-STRUCTURE.md'] = replace_once(docs['docs/architecture/PROJECT-STRUCTURE.md'],
    '日期：2026-10-07。以下目录是未来规划；当前实际只有根规则、README、docs 和文档证据，不创建这些正式应用或空类。',
    '日期：2026-10-07。项目根目录为 `/Users/kimwell/work/pet-platform`。以下应用目录是未来规划；当前只新增根规则、README、docs 和文档证据，保留已有 `ui/` 及 `.DS_Store`，不创建正式应用或空类，不创建 `enterprise-app-scaffold` 子目录。')
docs['docs/architecture/PROJECT-STRUCTURE.md'] = replace_once(docs['docs/architecture/PROJECT-STRUCTURE.md'],
    'enterprise-app-scaffold/', '当前项目根目录/  # /Users/kimwell/work/pet-platform')
docs['docs/architecture/PROJECT-STRUCTURE.md'] = replace_once(docs['docs/architecture/PROJECT-STRUCTURE.md'],
    '基础包暂定 `com.company.scaffold`；生成工具须替换基础包及源码路径，不让业务实现依赖固定公司名称。',
    '当前基础包固定为 `com.pet.platform`，对应源码目录为 `apps/backend/src/main/java/com/pet/platform/`，启动类为 `com.pet.platform.Application`（对应 `apps/backend/src/main/java/com/pet/platform/Application.java`）。本轮仅记录约束，不创建源码路径或启动类。未来 P11 生成其他新项目时支持显式替换 package 和源码路径；当前项目始终使用已确定的包名。')
for module in ['shared','platform','identity','customeridentity','attachment','audit','notification','modules']:
    docs['docs/architecture/PROJECT-STRUCTURE.md'] = docs['docs/architecture/PROJECT-STRUCTURE.md'].replace('| ' + module + ' |', '| `com.pet.platform.' + module + '` |')
docs['docs/architecture/PROJECT-STRUCTURE.md'] = replace_once(docs['docs/architecture/PROJECT-STRUCTURE.md'],
    'shared 规划 api、exception、config、security、tenancy、persistence、redis、messaging、storage、wechat、observability。',
    '`com.pet.platform.shared` 下规划 api、exception、config、security、tenancy、persistence、redis、messaging、storage、wechat、observability。')
docs['docs/architecture/PROJECT-STRUCTURE.md'] = replace_once(docs['docs/architecture/PROJECT-STRUCTURE.md'],
    '实际文件列表由 [验证报告](../testing/P01-01-VERIFICATION.md) 记录。',
    '当前根目录实际包含 `.DS_Store`、既有 `ui/`、新增 `AGENTS.md`、`README.md` 和 `docs/`。上方规划树只描述目标工程结构，`ui/` 保留原位。实际文件列表由 [验证报告](../testing/P01-01-VERIFICATION.md) 记录。')

docs['docs/architecture/TECHNICAL-BASELINE.md'] = replace_once(docs['docs/architecture/TECHNICAL-BASELINE.md'],
    '## 后端\n\n',
    '## 后端\n\n项目位置固定为当前根目录 `/Users/kimwell/work/pet-platform`。后端基础包为 `com.pet.platform`，源码目录为 `apps/backend/src/main/java/com/pet/platform/`，启动类为 `com.pet.platform.Application`；模块包边界见 [工程结构](PROJECT-STRUCTURE.md)。位置和包名已确定只构成 P01-01 文档约束，正式目录及源码留待 P02。\n\n')

docs['docs/product/SCAFFOLD-SCOPE.md'] = replace_once(docs['docs/product/SCAFFOLD-SCOPE.md'],
    '日期：2026-10-07。定位为企业应用工程模板；当前只执行 P01-01。',
    '日期：2026-10-07。定位为企业应用工程模板；当前只执行 P01-01。项目位置为当前根目录 `/Users/kimwell/work/pet-platform`，现有 `ui/` 成果保留原位；位置和包名确定不扩大本轮执行范围。')

docs['docs/development/ROADMAP.md'] = replace_once(docs['docs/development/ROADMAP.md'],
    '| 独立目录、明确固定技术方向 |', '| 当前根目录与既有成果边界明确、固定技术方向 |')
docs['docs/development/ROADMAP.md'] = replace_once(docs['docs/development/ROADMAP.md'],
    'P01-01已完成：只检查事实、确定并验证版本、建立当前文档。',
    'P01-01已完成：只检查事实、确定并验证版本、建立当前文档。位置固定为当前根目录 `/Users/kimwell/work/pet-platform`，后端基础包固定为 `com.pet.platform`、启动类为 `com.pet.platform.Application`；P02 必须在该根目录按规划结构初始化，本轮不创建任何正式三端工程。')

docs['docs/development/DECISION-LOG.md'] = replace_once(docs['docs/development/DECISION-LOG.md'],
    '| D01 | 独立目录 enterprise-app-scaffold | 原目录有 UI 成果；用户明确选择同级新目录 | 不覆盖、不复制原行业业务；不初始化 Git/正式应用 |',
    '| D01 | 当前根目录 `/Users/kimwell/work/pet-platform` 是项目位置 | 用户最新指令替代先前同级目录选择；重新检查无文件冲突 | 所有工程/文档直接位于当前根，不创建 enterprise-app-scaffold 子目录，不整体移动既有成果；只新增本轮文档 |')
docs['docs/development/DECISION-LOG.md'] = replace_once(docs['docs/development/DECISION-LOG.md'],
    '| D15 | 当前只执行 P01-01，P01-02 为下一任务 | 用户明确范围 | 无自动阶段推进，无提交/推送/部署，无 Product Delivery OS |',
    '| D15 | 当前只执行 P01-01，P01-02 为下一任务 | 用户明确范围 | 无自动阶段推进，无提交/推送/部署，无 Product Delivery OS |\n| D18 | 当前基础包 `com.pet.platform`、源码 `apps/backend/src/main/java/com/pet/platform/`、启动类 `com.pet.platform.Application` | 用户最新明确约束；结构与根规则同步更新 | 所有模块位于此包下；只记录规划，不提前创建启动类/正式三端工程 |')
docs['docs/development/DECISION-LOG.md'] = replace_once(docs['docs/development/DECISION-LOG.md'],
    '## 尚未细化（不作为已完成实现）',
    '## 位置修订记录\n\n先前同级目录选择及 `com.company.scaffold` 占位包名已被本次用户指令替代，不再是当前规范。旧目录文件保留为历史成果；当前有效事实源为本项目根目录中的文档。原始探针命令、日志和归档保留原工作目录及技术测试包，不能据此声称当前启动类已建立或运行。\n\n## 尚未细化（不作为已完成实现）')

report = docs['docs/testing/P01-01-VERIFICATION.md']
start = report.index('最初 cwd 为')
end = report.index('## 2. 本机环境与目标环境')
report = report[:start] + '''当前项目根目录固定为 `/Users/kimwell/work/pet-platform`。本次修订写入前，根目录只有 `.DS_Store` 和 `ui/`，273个既有文件；没有 AGENTS.md、根 README、docs、package.json、pom.xml、依赖锁文件或正式三端工程。当前根及四级父目录的 AGENTS.md 均不存在，没有与10份指定产物或证据目录重名的文件：[冲突检查](evidence/P01-01/conflict-check.json)。

本次重新执行 rev-parse、branch、status，均退出128并提示 not a git repository；当前不是 Git 仓库，分支及 tracked/dirty/untracked 分类不适用：[当前根与环境命令](evidence/P01-01/current-root-checks.json)。已检查既有 UI README；其视觉成果保留原位，不能作为通用模板业务或完整业务验收。

用户最新位置指令替代先前同级新目录选择：所有工程和文档直接在当前根创建或更新，不创建 `enterprise-app-scaffold` 子目录，不整体移动既有成果。只将前轮已产生的10份 P01-01 文档和相关验证证据复制并修订到当前根；先前同级目录未被修改或删除，不再是当前有效规范位置。证据中的原工作目录、命令、测试包和失败结果保留原样，明确作为历史执行记录；当前根的规则、结构、决策及本报告为修订后的事实源。

本次写入前的273个既有文件逐个与写入后核对路径、大小、SHA-256，均保持一致：[本次保全结果](evidence/P01-01/root-preservation.json)、[本次初始清单](evidence/P01-01/root-inventory-before.json)。前轮的[保全结果](evidence/P01-01/source-preservation.json)及清单亦作为原始证据保留。本轮未读取或更新 Product Delivery OS 状态、未引入其框架。

后端规划约束固定为基础包 `com.pet.platform`，源码目录 `apps/backend/src/main/java/com/pet/platform/`，启动类 `com.pet.platform.Application`。模块为 shared、platform、identity、customeridentity、attachment、audit、notification、modules，均位于该基础包下。本轮只更新这些规范，不创建源码目录或启动类。

''' + report[end:]
report = replace_once(report,
    '环境原始命令、工作目录、退出码和输出见 [environment.json]',
    '本次重新核对默认 Java、Node、pnpm、系统 Maven、Docker CLI/daemon及Compose，均与前轮记录相同，命令/退出码见 [当前环境核对](evidence/P01-01/current-root-checks.json)。前轮完整环境原始命令、工作目录、退出码和输出见 [environment.json]')
report = replace_once(report,
    '## 5. 实际命令、退出码和结果\n\n',
    '## 5. 实际命令、退出码和结果\n\n以下兼容探针均为前轮真实执行记录，原始日志保持不变。本次修改项目位置和规划包名，不变更依赖、探针源码或工具链；没有重复执行兼容构建，不将前轮结果描述为本次重新运行。当前根环境核对及文档审计在本次实际执行。中性探针使用测试包 `probe`，不代表 `com.pet.platform.Application` 已创建或通过启动验收。\n\n')
report = replace_once(report,
    '| 正式三端初始化、业务开发、提交/发布/部署 | NOT_EXECUTED | 用户明确禁止，本轮未自动执行 |',
    '| 正式三端初始化、当前启动类创建/启动、业务开发、提交/发布/部署 | NOT_EXECUTED | 用户明确禁止；包名/位置仅作为规划记录，本轮未自动执行 |')
report = replace_once(report, '已确定：独立目录、固定技术体系、', '已确定：当前根目录、固定基础包/源码路径/启动类、固定技术体系、')
report = replace_once(report,
    '新增以下10个指定产物；没有修改任何既有其他项目文件：',
    '在当前根目录新增以下10个指定产物，内容由前轮文档修订位置及包名；没有覆盖或移动任何既有文件：')
report = replace_once(report,
    '| G01 位置/已有成果检查 | PASS | cwd、Git128、已有README/配置清单、273文件SHA一致、用户指定新目录 |',
    '| G01 位置/已有成果检查 | PASS | 当前根重新检查、Git128、冲突检查、273个既有文件SHA一致、用户最新根目录指令 |')
report = replace_once(report,
    '| G07 规划工程结构明确 | PASS | PROJECT-STRUCTURE区分规划与实际，单Module、包边界及可替换package |',
    '| G07 规划工程结构明确 | PASS | PROJECT-STRUCTURE区分规划/实际；当前根、单Module、com.pet.platform、源码路径/启动类及8个模块明确 |')
report = replace_once(report,
    '| G11 无提前正式业务实现 | PASS | 正式目录仅规则/docs；测试源码仅临时及证据归档 |',
    '| G11 无提前正式业务实现 | PASS | 本轮仅新增规则/docs，既有ui保留；apps/packages/templates/scripts/infra未创建，测试源码仅临时/证据归档 |')
report = replace_once(report,
    '最终文档审计命令：`python3 /tmp/enterprise-scaffold-p01-audit.py`，cwd为原工作目录，退出0；检查10个指定文件、全部本地引用、精确版本/发布时间、原成果SHA、无正式业务目录、秘密标识及探针归档边界：[审计结果](evidence/P01-01/document-audit.json)。所有证据文件的SHA-256见 [证据清单](evidence/P01-01/EVIDENCE-MANIFEST.json)。',
    '本次文档审计命令：`python3 docs/testing/evidence/P01-01/audit-reproduction.py`，cwd为 `/Users/kimwell/work/pet-platform`，退出0；检查10个指定文件及新增证据内全部本地引用、精确版本/发布时间、273个既有文件SHA、当前根/固定包名一致、无正式工程或额外脚手架目录、秘密标识及探针归档边界：[本次审计](evidence/P01-01/document-audit.json)。前次审计另存为 `document-audit-prior-location.json`，只代表前次位置，不能替代本次检查。[位置修订说明](evidence/P01-01/ROOT-LOCATION-AMENDMENT.md)记录复用边界；所有当前文档和证据SHA-256见 [证据清单](evidence/P01-01/EVIDENCE-MANIFEST.json)。')
docs['docs/testing/P01-01-VERIFICATION.md'] = report

changes = '''# P01-01 位置与后端包名修订

日期：2026-10-07（Asia/Shanghai）。用户最新指令明确当前工作目录 `/Users/kimwell/work/pet-platform` 为项目根。本修订仍只属于 P01-01，不执行 P01-02/P02。

当前规范：所有工程与文档直接位于当前根；不创建 enterprise-app-scaffold 子目录，不整体移动现有文件。基础包固定为 `com.pet.platform`，源码规划为 `apps/backend/src/main/java/com/pet/platform/`，启动类规划为 `com.pet.platform.Application`。八个模块包由根规则和工程结构明确记录。

写入前检查10个指定文件、证据目录、适用规则及仓库状态，无冲突。273个既有文件保留原位，写入后逐文件核对 SHA-256。根环境11条命令真实执行，Git三条退出128明确表示当前非Git；其余环境查询退出0。

前轮同级目录仅作为读取来源，未修改或删除。其10份文档与证据有选择地复制到当前根；原始日志、命令记录、依赖树、版本矩阵数据、探针归档均不改写。前次文档审计和清单另存为 prior-location 文件，当前有效审计与SHA清单重新生成，避免混淆位置事实。

本次没有重新执行兼容构建，因为依赖、工具链和中性探针源码未改动。前轮已执行结果与失败/未验证边界在当前验证报告中保留；中性测试包 probe 不能证明当前规划启动类已建立或已运行。本次实际执行的是环境复核、文件保全和文档一致性审计。

重现文档审计：在当前根执行 `python3 docs/testing/evidence/P01-01/audit-reproduction.py`。重现兼容探针必须按 PROBE-REPRODUCTION.md 创建新的临时目录，不把归档解压到正式 apps/。
'''

planned = {Path(name): body.encode() for name, body in docs.items()}
for path in (prior / ev_rel).rglob('*'):
    if path.is_file():
        name = path.name
        if name == 'document-audit.json': name = 'document-audit-prior-location.json'
        if name == 'EVIDENCE-MANIFEST.json': name = 'EVIDENCE-MANIFEST-prior-location.json'
        planned[ev_rel / name] = path.read_bytes()
planned[ev_rel / 'ROOT-LOCATION-AMENDMENT.md'] = changes.encode()
for name in ['root-inventory-before.json','current-root-checks.json','conflict-check.json','prior-location-inventory-before.json']:
    planned[ev_rel / name] = (stage / name).read_bytes()
planned[ev_rel / 'root-location-amendment.py'] = Path(__file__).read_bytes()
repro_rel = ev_rel / 'PROBE-REPRODUCTION.md'
planned[repro_rel] = (planned[repro_rel].decode() + '\n当前正式文档根为 `/Users/kimwell/work/pet-platform`；原始证据中的同级目录属于历史来源。当前基础包/启动类只在文档冻结，中性探针包不替代正式工程初始化或启动验收。\n').encode()

before = json.loads((stage / 'root-inventory-before.json').read_text())
for item in before:
    path = root / item['path']
    if not path.is_file() or path.stat().st_size != item['bytes'] or hashlib.sha256(path.read_bytes()).hexdigest() != item['sha256']:
        raise RuntimeError('Existing file changed before write: ' + item['path'])
conflicts = [str(name) for name in planned if (root / name).exists()]
if conflicts: raise RuntimeError('Write conflict: ' + repr(conflicts))
for name, body in planned.items():
    target = root / name
    target.parent.mkdir(parents=True, exist_ok=True)
    with target.open('xb') as stream: stream.write(body)
print(json.dumps({'root':str(root),'created_files':len(planned),'required_docs':len(docs),'existing_files_preserved':len(before),'formal_projects_created':False},ensure_ascii=False))
