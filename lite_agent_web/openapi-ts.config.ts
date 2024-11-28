import { defineConfig } from '@hey-api/openapi-ts';

export default defineConfig({
  client: '@hey-api/client-fetch',
  input: './lib/lite_agent_openapi.json',
  output: {
    format: 'prettier',
    path: './src/client',
  },
  types: {
    dates: 'types+transform',
    enums: 'typescript',
  },
  plugins: ['@tanstack/react-query'],
});
