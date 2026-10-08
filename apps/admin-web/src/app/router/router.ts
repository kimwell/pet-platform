import { createRootRoute, createRoute, createRouter } from '@tanstack/react-router';
import { SystemLayout } from '../layout/SystemLayout';
import { SystemEntry } from '../../shared/SystemEntry';
import { SystemError } from '../../shared/SystemError';
import { SystemNotFound } from '../../shared/SystemNotFound';

const rootRoute = createRootRoute({ component: SystemLayout, errorComponent: SystemError, notFoundComponent: SystemNotFound });
const entryRoute = createRoute({ getParentRoute: () => rootRoute, path: '/', component: SystemEntry });
export const routeTree = rootRoute.addChildren([entryRoute]);
export const router = createRouter({ routeTree });

declare module '@tanstack/react-router' {
  interface Register { router: typeof router }
}
