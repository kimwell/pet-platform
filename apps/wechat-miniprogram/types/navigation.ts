export type CustomerTabKey = 'home' | 'rooms' | 'orders' | 'mine';
export interface CustomerTab { key: CustomerTabKey; text: string; pagePath: string; icon: string; selectedIcon: string }
export interface NavigationGeometry { statusBarHeight: number; barHeight: number; totalHeight: number; contentRight: number; safeBottom: number; windowWidth: number }
export interface CustomerTabBar { syncRoute: () => void }
