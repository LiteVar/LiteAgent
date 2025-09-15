import { defineConfig } from '@hey-api/openapi-ts';

export default defineConfig({
  input: 'http://192.168.2.188:9082/v3/api-docs/liteagent-wechat-adapter', // 后端API文档地址
  output: './src/api',
  client: {
    bundle: true,
    baseUrl: false, // 不生成硬编码的baseUrl
  },
});
