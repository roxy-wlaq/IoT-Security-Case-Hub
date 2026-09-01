import React from 'react';
import { createRoot } from 'react-dom/client';
import App from '@/App';
import { bootstrapCsrf } from '@/shared/api/csrf';
import '@/styles/global.css';

// 应用启动即让后端下发 CSRF Token Cookie（XSRF-TOKEN，HttpOnly=false），
// 供后续状态修改请求通过 X-XSRF-TOKEN 头回传。
bootstrapCsrf();

const container = document.getElementById('root');
if (!container) {
  throw new Error('Root container #root not found');
}

createRoot(container).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
