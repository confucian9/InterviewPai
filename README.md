# 面经派（InterviewPai）

> ⚠️ **声明**：本项目为 **AI Coding 练手项目**，用于学习和实践 AI 辅助编程技术。项目中的代码由 AI 辅助生成，仅供学习参考，不建议直接用于生产环境。

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vue-3.4.0-brightgreen" alt="Vue">
  <img src="https://img.shields.io/badge/TypeScript-5.3.3-blue" alt="TypeScript">
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License">
</p>

## 项目简介

面经派是一个基于 AI 的面试经验管理平台，能够将面试录音自动转换为文字，并通过 AI 技术识别其中的问题与答案，形成结构化的面试知识库，帮助用户进行复习与总结。

## 主要功能

- 🎤 **音频上传**：支持 mp3、wav、m4a 格式的面试录音上传
- 🤖 **语音识别**：使用阿里百炼 fun-asr 模型进行语音转文字
- 💡 **智能问答提取**：使用 DeepSeek LLM 自动提取面试问题与答案
- 📝 **AI 总结**：自动生成面试总结和关键词
- 🔍 **搜索功能**：支持关键词和标签搜索
- 📚 **复习模式**：刷题模式帮助复习面试问题
- 🏷️ **标签系统**：自动生成标签，支持手动编辑

## 技术栈

### 后端

- Spring Boot 3.2.0
- Spring Security + JWT
- MyBatis Plus
- MySQL
- Redis
- MinIO
- 阿里百炼 ASR
- DeepSeek LLM

### 前端

- Vue 3
- TypeScript
- Vite
- Pinia
- Element Plus
- Axios

## 项目结构

```
InterviewPai/
├── backend/                    # 后端项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/interviewpai/
│   │   │   │   ├── config/     # 配置类
│   │   │   │   ├── controller/ # 控制器
│   │   │   │   ├── dto/        # 数据传输对象
│   │   │   │   ├── entity/     # 实体类
│   │   │   │   ├── mapper/     # MyBatis Mapper
│   │   │   │   ├── security/   # 安全相关
│   │   │   │   └── service/    # 服务层
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/init.sql
│   │   └── test/
│   └── pom.xml
├── frontend/                   # 前端项目
│   ├── src/
│   │   ├── api/               # API 接口
│   │   ├── layouts/           # 布局组件
│   │   ├── router/            # 路由配置
│   │   ├── stores/            # Pinia 状态管理
│   │   ├── styles/            # 样式文件
│   │   ├── utils/             # 工具函数
│   │   └── views/             # 页面组件
│   ├── index.html
│   ├── package.json
│   ├── tsconfig.json
│   └── vite.config.ts
└── README.md
```

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+
- MinIO

### 环境变量配置

```bash
# MySQL
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_DB=interviewpai
export MYSQL_USER=root
export MYSQL_PASSWORD=123456

# Redis
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=

# MinIO
export MINIO_ENDPOINT=http://localhost:9000
export MINIO_ACCESS_KEY=admin
export MINIO_SECRET_KEY=12345678

# JWT
export JWT_SECRET=your-jwt-secret-key

# DeepSeek
export DEEPSEEK_KEY=your-deepseek-api-key

# 阿里百炼
export DASHSCOPE_API_KEY=your-dashscope-api-key
```

### 后端启动

```bash
cd backend

# 创建数据库并初始化
mysql -u root -p < src/main/resources/db/init.sql

# 启动项目
mvn spring-boot:run
```

### 前端启动

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

### 访问应用

- 前端地址：http://localhost:3000
- 后端地址：http://localhost:8080

## API 接口

### 认证相关

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/auth/register | 用户注册 |
| POST | /api/auth/login | 用户登录 |
| GET | /api/auth/me | 获取当前用户信息 |
| PUT | /api/auth/me | 更新用户信息 |

### 音频相关

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/audio/upload | 上传音频 |
| GET | /api/audio/list | 获取音频列表 |
| GET | /api/audio/{id} | 获取音频详情 |
| DELETE | /api/audio/{id} | 删除音频 |

### 面经相关

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/interview/list | 获取面经列表 |
| GET | /api/interview/{id} | 获取面经详情 |
| PUT | /api/interview/qa/{id} | 更新问答 |
| DELETE | /api/interview/qa/{id} | 删除问答 |

### 处理相关

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/process/audio/{id} | 开始处理音频 |

### 标签相关

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/tags | 获取所有标签 |
| GET | /api/tags/search | 关键词搜索 |
| GET | /api/tags/search/{tag} | 标签搜索 |

### 复习相关

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/review/random | 获取随机问题 |
| GET | /api/review/tag/{tag} | 按标签获取问题 |
| GET | /api/review/all | 获取所有问题 |

## 数据库设计

### 用户表 (user)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 用户ID |
| username | VARCHAR(50) | 用户名 |
| password | VARCHAR(255) | 密码 |
| nickname | VARCHAR(50) | 昵称 |
| avatar | VARCHAR(500) | 头像 |
| create_time | DATETIME | 创建时间 |

### 音频记录表 (audio_record)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 音频ID |
| user_id | BIGINT | 用户ID |
| file_name | VARCHAR(255) | 文件名 |
| file_url | VARCHAR(500) | 文件地址 |
| duration | BIGINT | 音频时长 |
| status | VARCHAR(20) | 处理状态 |

### 问答记录表 (qa_record)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | ID |
| audio_id | BIGINT | 音频ID |
| question | TEXT | 问题 |
| answer | TEXT | 答案 |
| tags | VARCHAR(500) | 标签 |
| confidence | DECIMAL(5,2) | 置信度 |

## 部署说明

### Docker 部署

```bash
# 构建后端镜像
cd backend
docker build -t interviewpai-backend .

# 构建前端镜像
cd frontend
docker build -t interviewpai-frontend .

# 使用 docker-compose 启动
docker-compose up -d
```

### 生产环境配置

1. 修改 `application.yml` 中的生产环境配置
2. 配置 HTTPS
3. 配置 MinIO 公开访问
4. 配置 Redis 密码
5. 配置 MySQL 主从复制

## 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 联系方式

项目地址：[https://github.com/confucian9/InterviewPai](https://github.com/confucian9/InterviewPai)
