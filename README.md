# 个人文件管理系统

一个功能完善的个人文件管理网站，支持视频、图片、文档、压缩包等多种文件类型的上传、管理和在线预览。

## 功能特性

### 用户功能
- ✅ 用户注册和登录
- ✅ 文件上传（视频、图片、文档、压缩包、音乐）
- ✅ 文件上传后需等待管理员审核
- ✅ 文件分类浏览（全部、图片、文档、压缩包、音乐）
- ✅ 文件搜索功能
- ✅ 文件在线预览（文本、图片）
- ✅ 文件下载功能
- ✅ 个人中心，管理自己的文件和视频
- ✅ 批量删除文件/视频

### 视频功能
- ✅ 首页视频列表（按观看次数排序）
- ✅ 视频播放页面
- ✅ 视频封面（用户上传或自动截取）
- ✅ 视频搜索功能
- ✅ 观看次数统计
- ✅ 分页显示

### 管理员功能
- ✅ 自定义网站背景图片
- ✅ 自定义网站主题色
- ✅ 审核/驳回文件上传
- ✅ 删除任意文件
- ✅ 添加驳回理由

### 审核系统
- ✅ 文件上传后待审核状态
- ✅ 管理员批准后显示
- ✅ 驳回时填写理由

## 技术栈

- **后端**: Django 5.0.3
- **数据库**: MySQL 8.0
- **前端**: Bootstrap 5.3 + jQuery
- **视频处理**: FFmpeg
- **Web服务器**: Nginx
- **静态文件**: Bootstrap Icons

## 系统要求

### Linux 服务器需要安装的软件

```bash
# Python 3.8+
python3 --version

# MySQL 8.0
mysql --version

# FFmpeg（用于视频处理）
ffmpeg -version

# Nginx 1.20+
nginx -v

# 其他工具
yum install -y python3-pip python3-devel gcc
```

### 安装 FFmpeg

```bash
# CentOS/RHEL
yum install -y epel-release
yum install -y ffmpeg

# 验证安装
ffmpeg -version
```

## 安装部署

### 1. 克隆项目

```bash
cd /path/to/your/project
# 上传项目文件到此目录
```

### 2. 创建虚拟环境

```bash
python3 -m venv venv
source venv/bin/activate
```

### 3. 安装依赖

```bash
pip install -r requirements.txt
```

**注意**: 如果安装 `mysqlclient` 失败，可能需要先安装 MySQL 开发包：

```bash
yum install -y mysql-devel
```

### 4. 配置数据库

编辑 `config/settings.py`，修改数据库配置：

```python
DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.mysql',
        'NAME': 'boke_db',
        'USER': 'your_mysql_user',
        'PASSWORD': 'your_mysql_password',
        'HOST': 'localhost',
        'PORT': '3306',
    }
}
```

### 5. 创建数据库

```bash
mysql -u root -p
CREATE DATABASE boke_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 6. 初始化数据库

```bash
python manage.py makemigrations
python manage.py migrate
```

### 7. 创建管理员账号

```bash
python manage.py createsuperuser
```

### 8. 收集静态文件

```bash
python manage.py collectstatic --noinput
```

### 9. 测试运行

```bash
python manage.py runserver 0.0.0.0:8000
```

访问 http://your-server-ip:8000 查看效果。

### 10. 配置 Nginx

创建 `/etc/nginx/sites-available/boke`:

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location /static/ {
        alias /path/to/your/project/staticfiles/;
    }

    location /media/ {
        alias /path/to/your/project/media/;
    }

    location / {
        proxy_pass http://127.0.0.1:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

启用配置：

```bash
ln -s /etc/nginx/sites-available/boke /etc/nginx/sites-enabled/
nginx -t
systemctl reload nginx
```

### 11. 使用 systemd 管理服务

创建 `/etc/systemd/system/boke.service`:

```ini
[Unit]
Description=Boke File Management System
After=network.target

[Service]
User=www-data
Group=www-data
WorkingDirectory=/path/to/your/project
Environment="PATH=/path/to/your/project/venv/bin"
ExecStart=/path/to/your/project/venv/bin/gunicorn config.wsgi:application --bind 127.0.0.1:8000

[Install]
WantedBy=multi-user.target
```

启动服务：

```bash
systemctl daemon-reload
systemctl enable boke
systemctl start boke
```

## 目录结构

```
boke/
├── accounts/          # 用户系统
├── files/            # 文件管理
├── videos/           # 视频管理
├── config/           # 项目配置
├── templates/        # HTML 模板
├── static/           # 静态文件（CSS, JS）
├── media/            # 用户上传的文件
├── manage.py         # Django 管理脚本
└── requirements.txt  # Python 依赖

```

## 使用说明

### 用户操作

1. **注册账号**: 访问 `/register/` 注册新用户
2. **上传文件**: 登录后可以上传文件和视频
3. **等待审核**: 上传的文件需要管理员审核后才能显示
4. **浏览内容**: 在首页（视频页面）和文件页面浏览已审核的内容
5. **个人中心**: 查看和管理自己上传的文件

### 管理员操作

1. **审核文件**: 登录管理后台 `/admin/` 审核用户上传的文件
2. **自定义设置**: 在管理后台可以修改网站背景、主题色等
3. **管理用户**: 可以管理所有用户账号

## 开发说明

### 本地开发

```bash
# 安装依赖
pip install -r requirements.txt

# 运行开发服务器
python manage.py runserver
```

### 生产环境注意事项

1. 修改 `SECRET_KEY` 为随机字符串
2. 设置 `DEBUG = False`
3. 配置 `ALLOWED_HOSTS`
4. 使用 HTTPS
5. 定期备份数据库和 media 文件夹
6. 监控服务器资源和磁盘空间

## 常见问题

### Q: 安装 mysqlclient 失败

A: 需要先安装 MySQL 开发包：
```bash
yum install -y mysql-devel
pip install mysqlclient
```

### Q: 视频封面无法自动生成

A: 确保已正确安装 FFmpeg：
```bash
ffmpeg -version
```

### Q: 静态文件无法加载

A: 运行 `python manage.py collectstatic` 收集静态文件

### Q: 权限不足

A: 确保上传目录有写入权限：
```bash
chmod -R 755 media/
chown -R www-data:www-data media/
```

## License

MIT License





