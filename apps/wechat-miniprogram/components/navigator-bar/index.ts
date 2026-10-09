import { backOrHome, navigationGeometry } from '../../utils/navigation';
Component({
  options: { multipleSlots: true },
  properties: { title: { type: String, value: '' }, back: { type: Boolean, value: false }, placeholder: { type: Boolean, value: true } },
  data: { geometry: { statusBarHeight: 0, barHeight: 44, totalHeight: 44, contentRight: 108 } },
  lifetimes: { attached() { this.measure(); } },
  pageLifetimes: { show() { this.measure(); }, resize() { this.measure(); } },
  methods: { measure() { this.setData({ geometry: navigationGeometry() }); },
    goBack() { this.triggerEvent('back'); backOrHome(); } },
});
