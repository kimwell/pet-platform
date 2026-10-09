const automator=require(process.cwd()+'/.local-data/architecture/tools/node_modules/miniprogram-automator');
const fs=require('node:fs');
const root=process.cwd(),ev=root+'/docs/testing/evidence/ARCHITECTURE-REFRESH',profile=process.argv[2]||'iphone12';
(async()=>{
 const mini=await automator.launch({projectPath:root+'/apps/wechat-miniprogram',cliPath:'/Users/kimwell/Applications/WechatDevTools-2.02.2608080.app/Contents/MacOS/cli',trustProject:true});
 const result={profile,status:'IN_PROGRESS',pages:[],exceptions:[],logs:[]};
 mini.on('exception',()=>result.exceptions.push('运行时异常'));
 try {
  for (const key of ['home','rooms','orders','mine']) {
   const route='/pages/customer/'+key+'/index';let page; for(let attempt=0;attempt<3;attempt++){try{page=await mini.switchTab(route);break;}catch(error){result.logs.push({key,attempt,automationError:String(error.message)});if(attempt===2)throw error;await new Promise(r=>setTimeout(r,1500));}}await page.waitFor(400);
   const metrics=await mini.evaluate(()=>new Promise(resolve=>{
    const page=getCurrentPages().slice(-1)[0];const nav=page.selectComponent('#navigator');
    const query=wx.createSelectorQuery();query.select('.customer-body').boundingClientRect();
    query.in(nav).select('.navigator-title').boundingClientRect();query.select('.navigator').boundingClientRect();
    query.exec(boxes=>{const tab=page.getTabBar();const footer=wx.createSelectorQuery().in(tab);footer.select('.customer-tab-bar').boundingClientRect();footer.selectAll('.tab-item').boundingClientRect();footer.select('.assistant-bump').boundingClientRect();footer.exec(footerBoxes=>resolve({route:page.route,window:wx.getWindowInfo(),capsule:wx.getMenuButtonBoundingClientRect(),navigation:nav.data.geometry,tab:tab.data,boxes,footerBoxes}));});
   }));
   await mini.screenshot({path:ev+'/mini-'+profile+'-'+key+'.png'});result.pages.push({key,metrics});
   if(metrics.tab.selected!==key)throw Error('选中项与路由不一致');const [body,title]=metrics.boxes,[bar,buttons,assistant]=metrics.footerBoxes;const capsule=metrics.capsule;if(title.right>capsule.left||body.top<metrics.navigation.totalHeight)throw Error('胶囊/导航遮挡');if(buttons.length!==5||buttons.some(b=>Math.abs(b.width-metrics.window.windowWidth/5)>1||b.bottom>metrics.window.safeArea.bottom+1)||assistant.bottom>metrics.window.safeArea.bottom+1||bar.bottom>metrics.window.windowHeight+1)throw Error('底部安全区或五列布局错误');
  }
  await mini.evaluate(()=>{getCurrentPages().slice(-1)[0].getTabBar().assistant();});
  await mini.screenshot({path:ev+'/mini-'+profile+'-assistant.png'});
  // 旧入口是真实路由跳转，未替代微信API。
  await mini.callWxMethod('reLaunch',{url:'/pages/system/entry/index'});await new Promise(r=>setTimeout(r,500));
  let legacy; for(let i=0;i<12;i++){legacy=(await mini.currentPage())?.path;if(legacy==='pages/customer/home/index')break;await new Promise(r=>setTimeout(r,500));}
  result.legacyObserved=legacy;if(legacy.replace(/^\//,'')!=='pages/customer/home/index')throw Error('旧入口未回到首页');
  result.legacyEntry='PASS';result.status='PASS';
 }catch(error){result.status='FAIL';result.error=String(error.message);throw error;}
 finally{fs.writeFileSync(ev+'/mini-'+profile+'-runtime.json',JSON.stringify(result,null,2));mini.disconnect();}
})().catch(e=>{console.error(e.message);process.exitCode=1;});
