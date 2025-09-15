FROM node:20-alpine

# 设置工作目录
WORKDIR /app

# 复制 package 文件
COPY package*.json ./
COPY pnpm-lock.yaml ./

# 安装 pnpm
RUN npm install -g pnpm

# 安装依赖
RUN pnpm install --frozen-lockfile

# 复制源代码
COPY . .

# 设置生产环境并构建应用
ENV NODE_ENV=production
RUN pnpm run build

# 暴露端口
EXPOSE 3000

# 使用 vite preview 预览构建结果
CMD ["pnpm", "preview", "--host", "0.0.0.0", "--port", "3000"]