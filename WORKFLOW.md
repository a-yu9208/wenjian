# 月月烧烤项目 — 开发工作流程文档

## 项目概述
烧烤店商家管理系统，包含 Android APP（商家端）、H5（顾客点餐端）、Python 后端。

## 代码仓库
- 地址：https://github.com/a-yu9208/wenjian.git
- 分支：shaokaoAPP
- 本地路径：/root/wenjian

## 项目结构

### Android 前端（/root/wenjian/）
- 语言：Kotlin + Jetpack Compose + Material3
- 包名：com.example.yueyeushaokaojiaoziguan
- 构建：Gradle，由用户在本地打包 APK
- 关键目录：
  - app/src/main/java/.../merchant/ — 数据层（ViewModel、API、模型）
  - app/src/main/java/.../screens/ — UI 页面
  - app/src/main/java/.../ui/theme/ — 主题配色

### H5 顾客端（/root/wenjian/h5/）
- 纯 HTML/CSS/JS 单页应用
- app.js — 全部逻辑
- style.css — 样式

### Python 后端（/shaokao/backend/）
- 框架：FastAPI + SQLAlchemy 2.0 + MySQL
- 入口：app/main.py
- 端口：8888（nginx 反代到 https://api.teselx.cn）
- 数据库连接：.env 中的 DATABASE_URL
- 关键文件：
  - app/merchant.py — 商家端 API
  - app/customer.py — 顾客端 API
  - app/models.py — 数据库模型
  - app/schemas.py — 请求模型
  - AI_CONTEXT.md — AI 助手的规则和项目上下文文档
  - apk_watcher.py — APK 更新监控脚本
  - uploads/ — 图片和 APK 上传目录
  - uploads/changelog.txt — 当前版本更新日志
  - uploads/.apk_state.json — APK 版本状态

## 我的工作流程

### 1. 修改代码
- 前端代码在 /root/wenjian/ 下修改
- 后端代码在 /shaokao/backend/ 下修改
- 修改后检查括号匹配：`grep -o '{' file | wc -l` 对比 `}`

### 2. 推送前端代码
```bash
cd /root/wenjian
git add -A
git commit -m "提交信息"
git push origin
```

### 3. 更新 changelog
每次推送代码后，更新 /shaokao/backend/uploads/changelog.txt，写明本次更新内容。
这个文件会在用户上传新 APK 时被监控脚本读取，显示在 APP 的更新弹窗中。

### 4. 修改后端后重启
```bash
kill $(lsof -ti:8888) 2>/dev/null; sleep 1
cd /shaokao/backend && nohup python3 -m uvicorn app.main:app --host 127.0.0.1 --port 8888 > /tmp/shaokao.log 2>&1 &
```
如果遇到 pyc 缓存问题：
```bash
find /shaokao/backend -name "*.pyc" -delete
```

### 5. 确保监控脚本在运行
```bash
# 检查
ps aux | grep apk_watcher | grep -v grep
# 如果没运行，启动它
nohup python3 -u /shaokao/backend/apk_watcher.py > /tmp/apk_watcher.log 2>&1 &
```

## 版本发布流程
1. 我修改代码并推送到 GitHub（shaokaoAPP 分支）
2. 我更新 /shaokao/backend/uploads/changelog.txt
3. 用户在本地拉代码、打包 APK
4. 用户通过 SSH 上传 APK 到 /shaokao/backend/uploads/app-release.apk
5. apk_watcher.py 自动检测到文件变化：
   - 递增 versionCode（如 205 → 206）
   - 递增 versionName（如 2.0.5 → 2.0.6）
   - 从 changelog.txt 读取更新日志
   - 更新 merchant.py 中的版本信息
   - 自动重启后端
6. 用户手机上的 APP 检测到新版本，弹窗提示更新

## AI 烤烤助理
- 后端 /merchant/ai-chat 接口
- 调用 /root/.local/bin/kiro-cli 的无交互模式生成回复
- 上下文文档：/shaokao/backend/AI_CONTEXT.md（包含规则、能力范围、项目架构）
- 每次请求注入实时店铺数据（菜品、库存、订单、营业额等）
- 只做查询和操作指引，不执行增删改操作
- 如果消息中包含手机号，自动补充积分数据

## 注意事项
- 前端代码在 git 仓库中（/root/wenjian），后端代码不在仓库中（/shaokao/backend）
- 后端修改直接在服务器生效，重启即可
- SQLAlchemy 必须 >= 2.0，不要安装会降级它的包
- build.gradle.kts 中的 versionCode 保持为 1，实际版本由 CI/用户打包时决定
- MerchantApiConfig.baseApiUrl = "https://api.teselx.cn"
