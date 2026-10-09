import { expect, it, vi } from 'vitest';
import { onlineManager } from '@tanstack/react-query';
import { createServices } from './AuthService';

it('显式离线写入立即反馈失败，恢复网络不自动排队重放（技术网络夹具）', async () => {
  const { queryClient } = createServices();
  const wasOnline = onlineManager.isOnline();
  const write = vi.fn(async () => { throw new Error('受控离线传输失败'); });
  try {
    onlineManager.setOnline(false);
    const mutation = queryClient.getMutationCache().build(queryClient, { mutationFn: write });
    await expect(mutation.execute(undefined)).rejects.toThrow('受控离线传输失败');
    expect(mutation.state.isPaused).toBe(false);
    onlineManager.setOnline(true);
    await queryClient.resumePausedMutations();
    expect(write).toHaveBeenCalledTimes(1);
  } finally {
    onlineManager.setOnline(wasOnline);
    queryClient.clear();
  }
});
