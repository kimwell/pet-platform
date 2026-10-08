import js from '@eslint/js';
import tseslint from 'typescript-eslint';
import hooks from 'eslint-plugin-react-hooks';
import refresh from 'eslint-plugin-react-refresh';

export default tseslint.config(
  { ignores: ['**/node_modules/**', '**/dist/**', '**/miniprogram_npm/**', 'docs/**', 'ui/**', '.local-data/**'] },
  { ...js.configs.recommended, files: ['scripts/**/*.mjs'], languageOptions: { globals: { process: 'readonly', console: 'readonly', Buffer: 'readonly' } } },
  ...tseslint.configs.recommended,
  {
    files: ['apps/admin-web/**/*.{ts,tsx}'],
    plugins: { 'react-hooks': hooks, 'react-refresh': refresh },
    rules: { ...hooks.configs.recommended.rules, 'react-refresh/only-export-components': ['error', { allowConstantExport: true }] },
    languageOptions: { globals: { document: 'readonly', window: 'readonly' } }
  },
  {
    files: ['apps/wechat-miniprogram/**/*.ts'],
    languageOptions: { globals: { App: 'readonly', Page: 'readonly', wx: 'readonly' } }
  }
);
