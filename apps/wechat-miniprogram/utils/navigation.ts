import { CUSTOMER_HOME } from '../constants/navigation';
import type { CustomerTabBar, NavigationGeometry } from '../types/navigation';
/** 以窗口与胶囊实测值安排标题，异常胶囊使用保守留白。单位均为px。 */
export function navigationGeometry(): NavigationGeometry {
  const window = wx.getWindowInfo();
  const statusBarHeight = Math.max(0, window.statusBarHeight || 0);
  const capsule = wx.getMenuButtonBoundingClientRect();
  const valid = capsule.height > 0 && capsule.width > 0 && capsule.top >= statusBarHeight && capsule.left > 0 && capsule.right <= window.windowWidth;
  const gap = valid ? Math.max(0, capsule.top - statusBarHeight) : 6;
  const barHeight = valid ? capsule.height + gap * 2 : 44;
  return { statusBarHeight, barHeight, totalHeight: statusBarHeight + barHeight,
    contentRight: valid ? window.windowWidth - capsule.left + 12 : 108,
    safeBottom: Math.max(0, window.screenHeight - (window.safeArea?.bottom ?? window.screenHeight)), windowWidth: window.windowWidth };
}
export function syncCustomerTab(): void {
  const pages = getCurrentPages();
  const page = pages[pages.length - 1];
  (page?.getTabBar?.() as unknown as CustomerTabBar | undefined)?.syncRoute();
}
export function backOrHome(): void {
  if (getCurrentPages().length > 1) wx.navigateBack({ delta: 1, fail: () => wx.switchTab({ url: CUSTOMER_HOME }) });
  else wx.switchTab({ url: CUSTOMER_HOME });
}
