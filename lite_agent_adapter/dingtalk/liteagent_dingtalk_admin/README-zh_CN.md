# LiteAgent DingTalk Admin - 钉钉轻量级代理管理系统

中文.[English](README.md)

LiteAgent DingTalk Admin 是一个专为钉钉集成设计的轻量级代理管理后台系统，基于 **React 19 + TypeScript + Tailwind CSS** （TailAdmin React.js） 构建，
为开发者提供完整的钉钉机器人和用户管理解决方案。

### 版本
0.1.0

## 🚀 功能特性

### 核心功能模块

#### 🤖 机器人管理
- **机器人绑定管理** - 钉钉机器人与 LiteAgent Agent 的绑定关系管理
- **权限精细控制** - 支持机器人权限的创建、编辑、删除和详情查看
- **智能搜索分页** - 高效的机器人搜索和分页浏览功能

## 🛠️ 技术栈

### 核心技术
- **前端框架**: React 19 + TypeScript
- **路由管理**: React Router v7.1.5
- **样式框架**: Tailwind CSS v4.0.8
- **构建工具**: Vite v6.1.0

### 开发工具
- **API 客户端**: OpenAPI 自动生成
- **状态管理**: React Hooks
- **UI 组件**: 自定义组件库
- **主题系统**: 深色/浅色模式切换

### 特色功能
- ✅ 响应式设计，支持移动端适配
- ✅ TypeScript 全覆盖，类型安全
- ✅ 钉钉企业应用深度集成
- ✅ 现代化 UI 设计和交互体验

## 🗺️ 路由架构

### 主应用路由 (需要认证)
这些路由通过 `AppLayout` 包装，要求用户已通过钉钉认证：

| 路由路径 | 组件 | 功能说明 |
|---------|------|----------|
| `/` | Home | 仪表盘首页 - 展示系统概览和关键指标 |
| `/robots` | RobotManagement | 机器人管理 - 钉钉机器人与 LiteAgent Agent 绑定管理 |
| `/users` | UserManagement | 用户管理 - 钉钉用户信息和部门结构管理 |
| `/system` | SystemInfo | 系统信息 - 系统状态监控和配置管理 |

### 错误处理
| 路由路径 | 组件 | 功能说明 |
|---------|------|----------|
| `*` | NotFound | 404 页面 - 处理未匹配的路由请求 |

### 认证保护机制
- **全局认证检查**: `AppLayout` 组件检查 `localStorage` 中的 Token
- **自动重定向**: 未认证用户自动重定向到 `/signin`
- **Token 管理**: 登录成功后自动设置 API 客户端认证头
- **状态监听**: 监听 `localStorage` 变化，支持多标签页同步

## 📦 安装和快速开始

### 环境要求

开始使用之前，请确保您的开发环境满足以下要求：

- **Node.js**: 18.x 或更高版本 (推荐使用 Node.js 20.x+)
- **包管理器**: npm、yarn 或 pnpm
- **钉钉开放平台账号**: 用于创建企业应用和获取 OAuth2 配置

### 快速开始

#### 1. 克隆项目

```bash
git clone git@gitlab.litevar.com:litevar/lite-agent/lite-agent-dingtalk-admin.git
cd lite-agent-dingTalk-admin
```

#### 2. 安装依赖

```bash
# 使用 npm
npm install

# 使用 yarn
yarn install

# 使用 pnpm
pnpm install
```

> 💡 如果遇到依赖安装问题，可以尝试使用 `--legacy-peer-deps` 参数

#### 3. 启动开发服务器

```bash
# 使用 npm
npm run dev

# 使用 yarn  
yarn dev

# 使用 pnpm
pnpm dev
```

项目将在 `http://localhost:5173` 启动。首次访问会自动跳转到钉钉登录页面。

## 💻 开发指南

### 项目结构
```
src/
├── api/                 # API 客户端和类型定义
├── components/          # React 组件
│   ├── auth/           # 认证相关组件
│   ├── robot/          # 机器人管理组件
│   └── ui/             # 通用 UI 组件
├── hooks/              # 自定义 React Hooks
├── layout/             # 布局组件
├── pages/              # 页面组件
└── utils/              # 工具函数
```

### API 使用
项目使用 OpenAPI 自动生成的类型安全客户端：

```typescript
import { client } from '../api';

// 自动包含认证头
const response = await client.getRobots();
```

### 组件开发规范
- 使用 TypeScript 编写所有组件
- 遵循 React Hooks 模式
- 使用 Tailwind CSS 进行样式开发
- 保持组件的单一职责原则

## 🚀 部署说明

### 生产环境构建
```bash
# 安装依赖
npm install

# 构建生产版本
npm run build
```

### 环境变量配置
创建 `.env.production` 文件：
```env
VITE_API_BASE_URL=https://your-api-domain.com
```

### 静态文件部署
将 `dist` 目录部署到您的静态文件服务器：
- Nginx
- Apache
- CDN (如阿里云 OSS、腾讯云 COS)

### docker部署
```
docker compose up -d
```

