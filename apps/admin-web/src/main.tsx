import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { RouterProvider } from '@tanstack/react-router';
import { AppProviders } from './config/AppProviders';
import { router, services } from './router/router';
import 'antd/dist/reset.css';
import './styles/global.css';

const root = document.getElementById('root');
if (!root) throw new Error('缺少应用入口节点');
createRoot(root).render(
  <StrictMode>
    <AppProviders services={services}><RouterProvider router={router} /></AppProviders>
  </StrictMode>
);
