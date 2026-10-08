// 使用官方 Action 的真实分发包，检查解析和匹配；不执行安装或 GitHub 缓存操作。
const assert = require('node:assert/strict');
const fs = require('node:fs');
const Module = require('node:module');
const path = require('node:path');
const [bundlePath, releasePath] = process.argv.slice(2);
const source = fs.readFileSync(bundlePath, 'utf8');
const entry = 'var __webpack_exports__ = __nccwpck_require__(90471);';
assert.ok(source.includes(entry), '官方分发包入口已变化，需要重新核对');
const probeModule = new Module(bundlePath, module);
probeModule.filename = bundlePath;
probeModule.paths = Module._nodeModulePaths(path.dirname(bundlePath));
probeModule._compile(source.replace(entry, 'var __webpack_exports__ = { load: __nccwpck_require__ };'), bundlePath);
const load = probeModule.exports.load;
const { JavaBase } = load(79935);
const { TemurinDistribution, TemurinImplementation } = load(91986);
const { isVersionSatisfies } = load(54527);
const release = JSON.parse(fs.readFileSync(releasePath, 'utf8')).release;
const expectedVersion = release.version_data.semver;
assert.throws(() => JavaBase.prototype.normalizeVersion('21.0.12.1+1'), /not valid SemVer/);
console.log('已复现：官方 v5.7.0 仍拒绝原四段版本输入，单纯升级 Action 无法修复。');
const normalized = JavaBase.prototype.normalizeVersion(expectedVersion);
assert.equal(normalized.version, expectedVersion);
assert.equal(normalized.stable, true);
assert.equal(isVersionSatisfies(expectedVersion, expectedVersion), true);
for (const differentBuild of ['21.0.12+101.1.LTS', '21.0.12+100.0.LTS', '21.0.12+1.LTS']) {
  assert.equal(isVersionSatisfies(expectedVersion, differentBuild), false);
}
const installer = new TemurinDistribution({
  version: expectedVersion, architecture: 'x64', packageType: 'jdk',
  checkLatest: false, verifySignature: false
}, TemurinImplementation.Hotspot);
// 只替换网络读取，输入是本次刚取得的 Adoptium 官方元数据快照。
installer.getAvailableVersions = async () => [release];
(async () => {
  const resolved = await installer.findPackageForDownload(expectedVersion);
  assert.equal(resolved.version, expectedVersion);
  assert.equal(resolved.url, release.binaries[0].package.link);
  assert.equal(release.version_data.openjdk_version, '21.0.12.1+1-LTS');
  assert.ok(resolved.url.includes('jdk-21.0.12.1%2B1/'));
  console.log(JSON.stringify({ status: 'PASS', input: expectedVersion, runtimeVersion: release.version_data.openjdk_version, linuxX64Package: resolved.url }, null, 2));
  console.log('范围：真实 Action 的解析及元数据匹配；没有执行 Linux JDK 下载、安装或 GitHub CI。');
})().catch((error) => { console.error(error); process.exitCode = 1; });
