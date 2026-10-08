import { readFileSync, existsSync } from 'node:fs';
import { resolve } from 'node:path';
import assert from 'node:assert/strict';

const base = resolve('apps/wechat-miniprogram');
const read = (path) => JSON.parse(readFileSync(resolve(base, path), 'utf8'));
const project = read('project.config.json');
const app = read('miniprogram/app.json');
assert.equal(project.compileType, 'miniprogram');
assert.equal(project.miniprogramRoot, 'miniprogram/');
assert.equal(project.libVersion, '3.17.2');
assert.deepEqual(project.setting.useCompilerPlugins, ['typescript']);
assert.equal(project.setting.packNpmManually, true);
assert.deepEqual(project.setting.packNpmRelationList, [{ packageJsonPath: './package.json', miniprogramNpmDistDir: './miniprogram/' }]);
assert.equal(project.setting.urlCheck, true);
assert.equal(read('tsconfig.json').compilerOptions.noEmit, true);
assert.ok(!app.tabBar, '本轮不创建混合身份导航');
for (const page of app.pages) {
  for (const extension of ['ts', 'json', 'wxml', 'wxss']) {
    assert.ok(existsSync(resolve(base, 'miniprogram', `${page}.${extension}`)), `缺少页面文件：${page}.${extension}`);
  }
  for (const component of Object.values(read(`miniprogram/${page}.json`).usingComponents ?? {})) {
    assert.ok(component.startsWith('tdesign-miniprogram/'), '只引用本轮已安装官方组件');
    const relative = component.replace('tdesign-miniprogram/', '');
    assert.ok(existsSync(resolve(base, 'node_modules/tdesign-miniprogram/miniprogram_dist', `${relative}.json`)), `组件不存在：${component}`);
  }
}
read(`miniprogram/${app.sitemapLocation}`);
console.log('原生小程序结构、页面和官方组件引用检查通过；这不是开发者工具编译。');
