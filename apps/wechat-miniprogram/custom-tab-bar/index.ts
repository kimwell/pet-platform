import { CUSTOMER_TABS } from '../constants/navigation';
import { navigationGeometry } from '../utils/navigation';
import type { CustomerTabKey } from '../types/navigation';
Component({
  data: { tabs: CUSTOMER_TABS, selected: 'home' as CustomerTabKey, safeBottom: 0, switching: false },
  lifetimes: { attached() { this.syncRoute(); } },
  pageLifetimes: { show() { this.syncRoute(); }, resize() { this.syncRoute(); } },
  methods: {
    syncRoute() {
      const route = '/' + (getCurrentPages().slice(-1)[0]?.route ?? '');
      const tab = CUSTOMER_TABS.find(item => item.pagePath === route);
      this.setData({ safeBottom: navigationGeometry().safeBottom, ...(tab ? { selected: tab.key } : {}) });
    },
    switchTab(event: WechatMiniprogram.TouchEvent) {
      const key = event.currentTarget.dataset.key as CustomerTabKey;
      const tab = CUSTOMER_TABS.find(item => item.key === key);
      if (!tab || this.data.switching || key === this.data.selected) return;
      this.setData({ switching: true });
      wx.switchTab({ url: tab.pagePath, success: () => this.syncRoute(),
        fail: () => wx.showToast({ title: '页面切换失败，请重试', icon: 'none' }),
        complete: () => this.setData({ switching: false }) });
    },
    assistant() { wx.showToast({ title: '功能暂未开放', icon: 'none' }); },
  },
});
