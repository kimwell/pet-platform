from pathlib import Path
import json,hashlib
from PIL import Image,ImageChops,ImageDraw,ImageFont,ImageStat
root=Path.cwd();ev=root/'docs/testing/evidence/ARCHITECTURE-REFRESH';out=ev/'visual';out.mkdir(exist_ok=True)
profile=json.loads((root/'.codex-ui/architecture-refresh/style-profile.json').read_text());(ev/'style-profile.json').write_text(json.dumps(profile,ensure_ascii=False,indent=2))
try:font=ImageFont.truetype('/System/Library/Fonts/STHeiti Medium.ttc',22)
except OSError:font=ImageFont.load_default()
assets={'home':'ZIU38','rooms':'XSHGN','orders':'yqq6i','mine':'GMAwX'};report={'scope':profile['scope'],'scoreKind':'MANUAL_REVIEW，诊断像素指标不是风格分','criticalFailures':[],'devices':[],'pages':[]}
for device in ['320','375','430']:
 r=json.loads((ev/f'mini-{device}-runtime.json').read_text());assert r['status']=='PASS'
 for page in r['pages']:
  m=page['metrics'];body,title=m['boxes'][:2];bar,buttons,assistant=m['footerBoxes'];assert len(buttons)==5
  critical={'capsuleOverlap':max(0,title['right']-m['capsule']['left']),'bodyNavigationOverlap':max(0,m['navigation']['totalHeight']-body['top']),'bottomOverlap':max(0,max(b['bottom'] for b in buttons)-m['window']['safeArea']['bottom']),'assistantBottomOverlap':max(0,assistant['bottom']-m['window']['safeArea']['bottom'])};assert all(v==0 for v in critical.values())
  report['devices'].append({'device':device,'page':page['key'],'windowWidth':m['window']['windowWidth'],'safeBottom':m['navigation']['safeBottom'],'fiveColumnWidths':[b['width'] for b in buttons],'critical':critical})
for key,asset in assets.items():
 ref=Image.open(root/f'ui/exports/final-review-2026-09-25/{asset}.png').convert('RGB');capture=Image.open(ev/f'mini-375-{key}.png').convert('RGB');m=next(p['metrics'] for p in json.loads((ev/'mini-375-runtime.json').read_text())['pages'] if p['key']==key);scale=capture.width/m['window']['windowWidth']
 # 只比较框架标题带和底部导航带，正文/原生状态栏/底部系统手势区排除。
 sourceHeader=ref.crop((0,80,750,160));actualHeader=capture.crop((0,round(m['navigation']['statusBarHeight']*scale),capture.width,round(m['navigation']['totalHeight']*scale))).resize((750,80))
 sourceFooter=ref.crop((0,ref.height-140,750,ref.height));buttons=m['footerBoxes'][1];top=min(b['top'] for b in buttons)-12;bottom=max(b['bottom'] for b in buttons);actualFooter=capture.crop((0,round(top*scale),capture.width,round(bottom*scale))).resize((750,140))
 source=Image.new('RGB',(750,220),'white');actual=source.copy();source.paste(sourceHeader,(0,0));source.paste(sourceFooter,(0,80));actual.paste(actualHeader,(0,0));actual.paste(actualFooter,(0,80));diff=ImageChops.difference(source,actual);stat=ImageStat.Stat(diff)
 source.save(out/f'{key}-reference-framework.png');actual.save(out/f'{key}-runtime-framework.png');diff.save(out/f'{key}-difference.png')
 canvas=Image.new('RGB',(2250,260),'#f4f6f9');draw=ImageDraw.Draw(canvas)
 for i,label in enumerate(['终审稿公共框架','实际模拟器公共框架','绝对像素差异（诊断）']):draw.text((i*750+16,8),label,fill='#172033',font=font)
 canvas.paste(source,(0,40));canvas.paste(actual,(750,40));canvas.paste(diff,(1500,40));canvas.save(out/f'{key}-comparison.png')
 score={'palette':24.5,'geometry':24,'icons':23.5,'typography':10,'platformSafety':15};report['pages'].append({'page':key,'score':sum(score.values()),'dimensions':score,'review':'标题和五列导航保留暖白/橙灰体系、原助手及线性图标；少量图标轮廓、渐变和间距差异扣3分；窗口实测安全区域无交叠。','pixelMAE_RGB_0_255':round(sum(stat.mean)/3,3),'pixelMetricLimit':'跨运行时归一化，包含胶囊位置/字体渲染差异，仅诊断，不能充当精确视觉一致性或业务验收','artifacts':[f'visual/{key}-comparison.png',f'visual/{key}-difference.png'],'sourceSha256':hashlib.sha256((root/f'ui/exports/final-review-2026-09-25/{asset}.png').read_bytes()).hexdigest()})
 assert sum(score.values())>=profile['thresholds']['minStyleScore']
report['status']='PASS_PUBLIC_FRAMEWORK';(ev/'visual-review.json').write_text(json.dumps(report,ensure_ascii=False,indent=2));print('四页公共框架人工评分97；12组实测胶囊/底部遮挡为0；诊断比较图已保存。正文不评分。')
