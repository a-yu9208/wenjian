# 个人文件管理系统 — 用户端答辩文档

## 一、项目概述

### 1.1 项目名称
个人文件管理系统（boke）

### 1.2 项目背景与目标
本系统是一个功能完善的个人文件管理网站，支持视频、图片、文档、压缩包、音乐等多种文件类型的上传、管理和在线预览。用户端主要面向普通注册用户，提供文件/视频的上传、浏览、搜索、下载、个人中心管理等功能。

### 1.3 技术栈
| 层级 | 技术选型 |
|------|---------|
| 后端框架 | Django 5.0.3 (Python) |
| 数据库 | MySQL 8.0 |
| 前端 | Bootstrap 5.3 + jQuery |
| 视频处理 | FFmpeg + ffmpeg-python |
| Web服务器 | Nginx + Gunicorn |
| 其他依赖 | Pillow（图片处理）、django-cors-headers（跨域）、djangorestframework |

---

## 二、用户端功能模块详解

### 2.1 用户认证模块（accounts）

#### 2.1.1 用户注册
- 路由：`/register/`
- 校验逻辑：用户名唯一性、邮箱唯一性、密码一致性、密码最少6位
- 防刷机制：基于 Session 的注册限流，60秒内只能注册一次
- 注册成功后跳转登录页

#### 2.1.2 用户登录
- 路由：`/login/`
- 使用 Django 内置 `authenticate()` 进行身份验证
- 登录成功/失败均记录登录日志（`LoginLog`），含 IP 地址
- 已登录用户访问登录页自动跳转首页

#### 2.1.3 用户登出
- 路由：`/logout/`
- 清除 Session 后跳转登录页

#### 2.1.4 个人中心
- 路由：`/profile/`
- 功能：查看和编辑个人信息（邮箱、电话、头像）
- 展示当前用户上传的所有视频和文件（含各状态：待审核/已通过/已驳回）
- 支持批量删除自己的文件和视频

#### 2.1.5 修改密码
- 路由：`/change-password/`（需登录）
- 需验证原密码，新密码至少6位，两次输入需一致
- 修改成功后保持登录状态（Session 自动更新）
- 入口位于个人中心侧边栏

### 2.2 视频模块（videos）

#### 2.2.1 视频首页/列表
- 路由：`/`（网站首页即视频列表）
- 只展示**公开且已通过审核**的视频
- 支持关键词搜索（标题、描述、上传者用户名）
- 支持排序切换：热门（按观看次数）/ 最新（按上传时间）
- 每页20条，Django Paginator 分页

#### 2.2.2 视频详情/播放
- 路由：`/videos/<id>/`
- 视频流播放路由：`/videos/<id>/play/`
- 基于 Session 防刷观看次数（每个 Session 只计一次）
- 视频流通过 Nginx `X-Accel-Redirect` 实现零拷贝发送，支持 HTTP Range 断点续传

#### 2.2.3 视频上传
- 路由：`/videos/upload/`（需登录）
- 支持上传视频文件 + 手动上传封面图
- 未手动上传封面时，系统上传后立即通过后台线程使用 FFmpeg 自动截取封面
- 文件大小限制：1GB
- 上传后根据可见性和用户角色自动确定审核状态：
  - 私密视频 → 自动通过
  - 管理员上传的公开视频 → 自动通过
  - 普通用户上传的公开视频 → 待审核

#### 2.2.4 视频删除
- 单个删除：`/videos/<id>/delete/`
- 批量删除：`/videos/batch-delete/`
- 普通用户只能删除自己上传的视频，同时删除磁盘物理文件

### 2.3 文件模块（files）

#### 2.3.1 文件列表
- 路由：`/files/`
- 只展示**公开且已通过审核**的文件
- 支持按文件类型分类筛选：全部、图片、文档、压缩包、音乐
- 支持关键词搜索（标题、描述）
- 每页20条分页

#### 2.3.2 文件详情（弹窗）
- 在文件列表页点击卡片，通过弹窗展示文件详情（AJAX 请求 `/files/<id>/` 获取 JSON）
- 基于 Session 防刷查看次数
- 预览支持：图片直接展示、文档在线预览前5000字符、音频在线播放
- 操作：下载文件、删除（上传者/管理员）

#### 2.3.3 文件下载
- 路由：`/files/<id>/download/`
- 私密文件权限校验：需登录且为上传者或管理员
- 通过 Nginx `X-Accel-Redirect` 发送文件，避免占用 Gunicorn Worker
- 每次下载递增下载计数

#### 2.3.4 文件上传
- 路由：`/files/upload/`（需登录）
- 根据文件扩展名自动识别类型：
  - 图片：jpg/jpeg/png/gif/bmp/webp
  - 文档：txt/md/doc/docx/pdf
  - 压缩包：zip/rar/7z/tar/gz
  - 音乐：mp3/wav/flac/aac/ogg
- 审核状态确定逻辑同视频模块

#### 2.3.5 文件删除
- 单个删除：`/files/<id>/delete/`
- 批量删除：`/files/batch-delete/`
- 权限同视频模块，同时删除物理文件

### 2.4 可见性管理
- 用户可在个人中心修改自己文件/视频的可见性（公开↔私密）
- API 路由：
  - `/accounts/api/video/<id>/change-visibility/`
  - `/accounts/api/file/<id>/change-visibility/`
- 核心规则：
  - 公开 → 私密：直接生效，状态自动设为已通过
  - 私密 → 公开：普通用户需重新审核，管理员直接生效

### 2.5 通知系统
- 通知列表：`/notifications/`（展示最近50条通知）
- 标记已读：`/notifications/read/`（支持单条/全部标记）
- 未读计数：`/notifications/count/`（前端导航栏轮询）
- 弹窗公告：`/api/announcement/`（页面加载时获取活跃公告）
- 审核通过/驳回时系统自动发送通知给用户

---

## 三、数据库设计（用户端相关）

### 3.1 核心模型

#### User（自定义用户模型）
| 字段 | 类型 | 说明 |
|------|------|------|
| username | CharField | 用户名（唯一） |
| password | - | 密码（Django 内置加密） |
| email | EmailField | 邮箱 |
| phone | CharField(20) | 电话 |
| role | CharField(10) | 角色：user/admin |
| avatar | ImageField | 头像 |
| created_at | DateTimeField | 创建时间 |

#### Video（视频模型）
| 字段 | 类型 | 说明 |
|------|------|------|
| title | CharField(200) | 标题 |
| description | TextField | 简介 |
| video_file | FileField | 视频文件 |
| thumbnail | ImageField | 封面图片 |
| duration | IntegerField | 时长（秒） |
| file_size | BigIntegerField | 文件大小（字节） |
| uploader | ForeignKey(User) | 上传者 |
| status | CharField(20) | 审核状态：pending/approved/rejected |
| visibility | CharField(10) | 可见性：public/private |
| view_count | IntegerField | 观看次数 |

#### FileItem（文件模型）
| 字段 | 类型 | 说明 |
|------|------|------|
| title | CharField(200) | 标题 |
| description | TextField | 描述 |
| file | FileField | 文件 |
| file_type | CharField(20) | 类型：image/document/archive/audio/other |
| size | BigIntegerField | 文件大小（字节） |
| uploader | ForeignKey(User) | 上传者 |
| status | CharField(20) | 审核状态 |
| visibility | CharField(10) | 可见性 |
| download_count | IntegerField | 下载次数 |
| view_count | IntegerField | 查看次数 |

#### Notification（通知模型）
| 字段 | 类型 | 说明 |
|------|------|------|
| user | ForeignKey(User) | 接收用户 |
| title | CharField(200) | 标题 |
| content | TextField | 内容 |
| is_read | BooleanField | 是否已读 |
| created_at | DateTimeField | 创建时间 |

---

## 四、系统架构与关键技术点

### 4.1 整体架构
```
用户浏览器 → Nginx（反向代理 + 静态文件 + 媒体文件）→ Gunicorn（WSGI）→ Django 应用 → MySQL
```

### 4.2 关键技术实现

#### 审核服务（ReviewService）
- 统一封装在 `core/review_service.py`，视频和文件共用同一套审核逻辑
- 通过策略模式根据用户角色和可见性自动判断初始状态
- 支持单条和批量审核操作

#### 消息服务（MessageService）
- 统一封装在 `core/message_service.py`
- 所有面向用户的提示文案集中管理，便于维护和国际化
- 同时支持 Django messages（页面跳转）和 JSON 响应（AJAX）两种场景

#### 文件传输优化
- 使用 Nginx `X-Accel-Redirect` 实现文件下载和视频流播放
- 避免大文件占用 Gunicorn Worker 进程
- 视频播放支持 HTTP Range 断点续传

#### 安全措施
- CSRF 防护（Django 内置中间件）
- CORS 跨域控制（白名单机制）
- 点击劫持防护（X-Frame-Options: SAMEORIGIN）
- 注册限流（Session 级别，60秒间隔）
- 登录日志记录（含 IP 地址）
- 私密文件权限校验

### 4.3 项目目录结构
```
boke/
├── accounts/          # 用户模块（注册/登录/个人中心/可见性API）
├── files/             # 文件模块（上传/下载/列表/详情/删除）
├── videos/            # 视频模块（上传/播放/列表/详情/删除）
├── core/              # 核心模块（审核服务/消息服务/通知/公告/日志）
├── config/            # 项目配置（settings/urls/wsgi）
├── templates/         # HTML 模板
├── static/            # 静态文件（CSS/JS）
├── media/             # 用户上传文件存储
├── manage.py          # Django 管理脚本
└── requirements.txt   # Python 依赖
```

---

## 五、用户端操作流程

### 5.1 注册 → 登录 → 上传 → 审核 → 展示
```
用户注册 → 登录系统 → 上传文件/视频（选择公开/私密）
                                    ↓
                        公开内容 → 待审核状态 → 管理员审核通过 → 在列表页展示
                        私密内容 → 自动通过 → 仅自己可见（个人中心）
```

### 5.2 可见性切换流程
```
公开 → 私密：直接生效
私密 → 公开：进入待审核 → 管理员审核通过后公开展示
```

---

## 六、答辩常见问题及参考回答

### Q1：为什么选择 Django 作为后端框架？
Django 是 Python 生态中最成熟的全栈 Web 框架，内置 ORM、用户认证、Admin 后台、表单处理、CSRF 防护等功能，开发效率高。本项目需要用户系统、文件管理、审核流程等功能，Django 的"batteries included"理念非常契合需求，减少了大量重复开发工作。

### Q2：用户认证是怎么实现的？
继承 Django 的 `AbstractUser` 扩展了自定义用户模型，增加了 `role`（角色）、`avatar`（头像）、`phone`（电话）等字段。认证使用 Django 内置的 `authenticate()` 和 `login()` 函数，密码通过 Django 的 PBKDF2 算法加密存储，不存储明文密码。通过 `is_admin_role` 属性统一判断管理员权限。

### Q3：文件上传的审核流程是怎样的？
系统实现了统一的审核服务（`ReviewService`）：
1. 用户上传文件/视频时选择公开或私密
2. 私密内容自动通过审核，仅上传者可见
3. 公开内容进入"待审核"状态，管理员审核通过后才在列表页展示
4. 管理员可以通过或驳回，驳回时必须填写理由
5. 审核结果通过通知系统自动告知用户

### Q4：视频播放是怎么实现的？
视频播放采用 Nginx `X-Accel-Redirect` 机制：Django 只负责权限校验和路由，实际文件传输由 Nginx 直接完成（零拷贝），支持 HTTP Range 断点续传，用户可以拖动进度条。这样避免了大文件传输占用 Python 进程，提高了并发性能。

### Q5：如何防止恶意刷浏览量/下载量？
- 观看次数和查看次数使用基于 Session 的防刷机制，每个 Session 对同一内容只计一次
- 注册接口有60秒限流
- 登录失败会记录日志（含 IP），便于发现异常

### Q6：文件类型是怎么识别的？
上传时根据文件扩展名自动判断类型，映射规则：
- 图片：jpg/jpeg/png/gif/bmp/webp
- 文档：txt/md/doc/docx/pdf
- 压缩包：zip/rar/7z/tar/gz
- 音乐：mp3/wav/flac/aac/ogg
- 其他：不在上述范围的文件

### Q7：系统的安全性是如何保障的？
1. **CSRF 防护**：Django 内置 CsrfViewMiddleware，所有 POST 请求需携带 CSRF Token
2. **CORS 控制**：通过 django-cors-headers 白名单限制跨域请求
3. **密码安全**：Django PBKDF2 加密，不存储明文
4. **权限控制**：`@login_required` 装饰器 + `is_admin_role` 属性双重校验
5. **私密文件保护**：下载时校验用户身份，非上传者和非管理员无法下载
6. **点击劫持防护**：X-Frame-Options 设为 SAMEORIGIN
7. **登录审计**：所有登录尝试记录日志，含 IP 地址

### Q8：前后端是如何交互的？
系统采用混合渲染模式：
- **页面级操作**（注册/登录/列表浏览）：Django 模板渲染，服务端返回完整 HTML
- **交互操作**（上传/删除/审核/可见性切换）：AJAX 请求，返回标准化 JSON 响应
- 统一消息服务（`MessageService`）同时支持两种场景，保证用户体验一致

### Q9：为什么要把审核逻辑抽成独立的 ReviewService？
遵循**单一职责原则**和**DRY 原则**。视频和文件的审核逻辑完全一致（初始状态判断、通过、驳回、可见性切换），如果分别在各自的 views 中实现会导致大量重复代码。抽成独立服务后：
- 代码复用，修改审核规则只需改一处
- 便于单元测试
- 视图层更简洁，只关注请求处理

### Q10：数据库为什么选择 MySQL？
MySQL 是最流行的关系型数据库之一，成熟稳定，社区资源丰富。本项目的数据模型（用户、文件、视频、通知）之间有明确的外键关联关系，适合关系型数据库。配置使用 utf8mb4 字符集以支持 emoji 等4字节字符，开启严格模式保证数据完整性。

### Q11：如果文件很大，上传会不会有问题？
系统设置了 1GB 的上传大小限制（`FILE_UPLOAD_MAX_MEMORY_SIZE` 和 `DATA_UPLOAD_MAX_MEMORY_SIZE`）。大文件上传时 Django 会自动使用临时文件而非内存缓存。下载和视频播放通过 Nginx `X-Accel-Redirect` 直接发送，不经过 Python 进程，避免内存溢出。

### Q12：通知系统是怎么实现的？
通知系统基于数据库存储（`Notification` 模型），支持两种发送方式：
1. **自动通知**：审核状态变更时，根据预定义的通知模板（`NotificationTemplate`）自动生成并发送
2. **手动通知**：管理员可在仪表盘手动发送/群发通知
前端通过轮询 `/notifications/count/` 接口获取未读数量，在导航栏显示角标。

### Q13：项目的部署方案是什么？
采用 Nginx + Gunicorn + Django 的经典部署方案：
- **Nginx**：反向代理、静态文件服务、媒体文件服务（X-Accel-Redirect）、HTTPS 终端
- **Gunicorn**：WSGI 服务器，多 Worker 进程处理并发请求
- **Systemd**：管理 Gunicorn 进程的启停和自动重启
- 域名：teselx.cn

### Q14：你在项目中负责了哪些具体工作？
我负责用户端的全部功能开发，包括：
1. 用户认证系统（注册/登录/登出/个人中心）
2. 文件模块（上传/下载/列表/详情/删除/分类筛选/搜索）
3. 视频模块（上传/播放/列表/详情/删除/搜索/排序）
4. 可见性管理功能
5. 通知系统的用户端展示
6. 前端页面的交互逻辑（AJAX 请求、分页、搜索等）

### Q15：项目开发过程中遇到了什么困难？如何解决的？
1. **大文件传输性能问题**：最初视频播放通过 Django FileResponse 直接发送，大文件会长时间占用 Worker。后来改用 Nginx X-Accel-Redirect，由 Nginx 直接发送文件，性能大幅提升。
2. **审核逻辑复杂度**：视频和文件的审核、可见性切换逻辑交叉复杂，最初代码分散在各个 view 中难以维护。通过抽取 ReviewService 统一管理，代码结构清晰了很多。
3. **防刷机制**：浏览量容易被恶意刷高，采用基于 Session 的防刷策略，同一 Session 对同一内容只计一次。

---

## 七、项目亮点总结

1. **统一审核服务**：ReviewService 封装了完整的审核生命周期，视频和文件共用，代码复用率高
2. **统一消息服务**：MessageService 集中管理所有提示文案，支持 Django messages 和 JSON 双模式
3. **Nginx 文件传输优化**：X-Accel-Redirect 零拷贝发送，支持断点续传，高效处理大文件
4. **完善的权限体系**：公开/私密可见性 + 审核状态双维度控制内容访问
5. **自动通知机制**：审核状态变更自动触发通知，模板可由管理员自定义
6. **安全防护全面**：CSRF、CORS、登录审计、注册限流、私密文件保护等多层安全措施
