import { readFileSync, existsSync, readdirSync } from 'node:fs';
import { resolve, dirname, relative, extname } from 'node:path';
import assert from 'node:assert/strict';

const base = resolve('apps/wechat-miniprogram');
const read = (path) => JSON.parse(readFileSync(resolve(base, path), 'utf8'));
const project = read('project.config.json');
const app = read('app.json');
assert.equal(project.compileType, 'miniprogram');
assert.equal(project.miniprogramRoot, './');
assert.equal(project.libVersion, '3.17.2');
assert.deepEqual(project.setting.useCompilerPlugins, ['typescript']);
assert.equal(project.setting.packNpmManually, true);
assert.deepEqual(project.setting.packNpmRelationList, [{ packageJsonPath: './package.json', miniprogramNpmDistDir: './' }]);
assert.equal(project.setting.urlCheck, true);
assert.equal(read('tsconfig.json').compilerOptions.noEmit, true);
const source = base;
const inside = path => { const rel = relative(source, path); assert.ok(!rel.startsWith('..') && !rel.startsWith('/'), `引用越出源码：${path}`); return path; };
const local = (owner, target) => inside(target.startsWith('/') ? resolve(source, '.' + target) : resolve(dirname(owner), target));
const extensions = ['ts', 'json', 'wxml', 'wxss'];
const componentFiles = (entry, label) => { for (const ext of extensions) assert.ok(existsSync(`${entry}.${ext}`), `缺少${label}文件：${entry}.${ext}`); };
assert.equal(app.window.navigationStyle, 'custom');
assert.equal(app.tabBar.custom, true);
assert.deepEqual(app.tabBar.list.map(tab => tab.pagePath), ['home', 'rooms', 'orders', 'mine'].map(name => `pages/customer/${name}/index`));
assert.deepEqual(app.tabBar.list.map(tab => tab.text), ['首页', '找房', '订单', '我的']);
assert.ok(new Set(app.pages).size === app.pages.length, '页面不可重复');
for (const page of app.pages) { assert.ok(!page.includes('..') && !page.startsWith('/')); componentFiles(inside(resolve(source, page)), '页面'); }
componentFiles(resolve(source, 'custom-tab-bar/index'), '自定义TabBar');
function walk(dir) { return readdirSync(dir, { withFileTypes: true }).flatMap(item => ['miniprogram_npm', 'node_modules'].includes(item.name) ? [] : item.isDirectory() ? walk(resolve(dir, item.name)) : [resolve(dir, item.name)]); }
for (const file of walk(source)) {
  if (extname(file) === '.json') {
    const config = JSON.parse(readFileSync(file, 'utf8'));
    for (const target of Object.values(config.usingComponents ?? {})) {
      if (target.startsWith('tdesign-miniprogram/')) {
        const entry = target.replace('tdesign-miniprogram/', '');
        assert.ok(existsSync(resolve(base, 'node_modules/tdesign-miniprogram/miniprogram_dist', `${entry}.json`)), `官方组件不存在：${target}`);
      } else {
        const entry = local(file, target); componentFiles(entry, '自定义组件');
        assert.equal(JSON.parse(readFileSync(`${entry}.json`, 'utf8')).component, true, `组件未声明component：${target}`);
      }
    }
  }
  if (['.ts', '.wxml', '.wxss'].includes(extname(file))) {
    const text = readFileSync(file, 'utf8');
    for (const match of text.matchAll(/(?:src=["']|["'])(\/assets\/[^"'{}]+)["']/g)) assert.ok(existsSync(local(file, match[1])), `素材不存在：${match[1]}`);
    if (extname(file) === '.wxss') for (const match of text.matchAll(/@import\s+["']([^"']+)["']/g)) assert.ok(existsSync(local(file, match[1])), `样式不存在：${match[1]}`);
  }
}
read(app.sitemapLocation);
console.log('原生小程序页面、自定义导航/TabBar、官方及本地组件、素材和样式引用检查通过；这不是开发者工具编译。');
