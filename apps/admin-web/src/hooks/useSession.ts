import { useSyncExternalStore } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useRouter } from '@tanstack/react-router';
import type { AuthSpace } from '../utils/auth/spaces';

export function useSession(space: AuthSpace, enabled = true) {
  const { auth } = useRouter().options.context!;
  useSyncExternalStore(auth.runtime.subscribe, () => auth.runtime.snapshot(space), () => auth.runtime.snapshot(space));
  const query = useQuery({ ...auth.meOptions(space), enabled: enabled && !auth.runtime.isBusy(space) });
  return { ...query, auth, busy: auth.runtime.isBusy(space), notice: auth.runtime.notice(space) };
}
