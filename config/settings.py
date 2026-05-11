"""
Django 项目配置文件 - boke 博客/文件管理系统

本文件是整个项目的核心配置，定义了：
- 数据库连接（MySQL）
- 已安装应用（accounts、files、videos、core）
- 中间件链（含 CORS 跨域支持）
- 模板引擎配置
- 静态文件和媒体文件路径
- 用户认证模型（自定义 User）
- 文件上传大小限制（1GB）
- Session 会话配置

联动模块：
- accounts: 自定义用户模型 AUTH_USER_MODEL
- core: 核心业务逻辑（通知、审核、仪表盘）
- files: 文件管理模块
- videos: 视频管理模块
- config.context_processors: 全局模板上下文（网站设置）
"""

from pathlib import Path
import os

# 项目根目录，所有相对路径基于此
BASE_DIR = Path(__file__).resolve().parent.parent

# ============================================================
# 安全配置
# ============================================================

# Django 密钥，生产环境应通过环境变量注入
SECRET_KEY = os.environ.get('DJANGO_SECRET_KEY', '<django-secret-key-placeholder>')

# 调试模式开关，生产环境必须为 False
DEBUG = os.environ.get('DJANGO_DEBUG', 'False').lower() == 'true'

# 允许访问的域名列表
ALLOWED_HOSTS = ['www.teselx.cn', 'teselx.cn', '127.0.0.1', 'localhost']

# ============================================================
# 应用注册
# ============================================================

INSTALLED_APPS = [
    'django.contrib.auth',           # Django 认证框架
    'django.contrib.contenttypes',   # 内容类型框架
    'django.contrib.sessions',       # Session 会话框架
    'django.contrib.messages',       # 消息框架（flash messages）
    'django.contrib.staticfiles',    # 静态文件管理
    'corsheaders',                   # 第三方：CORS 跨域支持
    'core',                          # 核心模块：通知、审核、仪表盘、公告
    'accounts',                      # 用户模块：注册、登录、个人中心、网站设置
    'files',                         # 文件模块：文件上传、下载、列表、审核
    'videos',                        # 视频模块：视频上传、播放、列表、审核
]

# ============================================================
# 中间件配置（按执行顺序排列）
# ============================================================

MIDDLEWARE = [
    'django.middleware.security.SecurityMiddleware',          # 安全头部
    'django.contrib.sessions.middleware.SessionMiddleware',   # Session 处理
    'corsheaders.middleware.CorsMiddleware',                  # CORS 跨域（需在 CommonMiddleware 之前）
    'django.middleware.common.CommonMiddleware',              # 通用请求处理
    'django.middleware.csrf.CsrfViewMiddleware',             # CSRF 防护
    'django.contrib.auth.middleware.AuthenticationMiddleware',# 用户认证
    'django.contrib.messages.middleware.MessageMiddleware',   # 消息中间件
    'django.middleware.clickjacking.XFrameOptionsMiddleware', # 点击劫持防护
]

# CORS 跨域设置：允许携带 Cookie
CORS_ALLOW_CREDENTIALS = True

# URL 路由入口
ROOT_URLCONF = 'config.urls'

# ============================================================
# 模板引擎配置
# ============================================================

TEMPLATES = [
    {
        'BACKEND': 'django.template.backends.django.DjangoTemplates',
        'DIRS': [BASE_DIR / 'templates'],  # 全局模板目录
        'APP_DIRS': True,                   # 自动搜索各 app 的 templates 目录
        'OPTIONS': {
            'context_processors': [
                'django.template.context_processors.debug',
                'django.template.context_processors.request',
                'django.contrib.auth.context_processors.auth',
                'django.contrib.messages.context_processors.messages',
                'config.context_processors.site_settings',  # 自定义：注入网站设置到所有模板
            ],
        },
    },
]

# WSGI 应用入口
WSGI_APPLICATION = 'config.wsgi.application'

# ============================================================
# 数据库配置（MySQL）
# ============================================================

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.mysql',
        'NAME': 'boke_db',
        'USER': 'root',
        'PASSWORD': '852741963.0Emmm',
        'HOST': 'localhost',
        'PORT': '3306',
        'OPTIONS': {
            'charset': 'utf8mb4',                                    # 支持 emoji 等4字节字符
            'init_command': "SET sql_mode='STRICT_TRANS_TABLES'",    # 严格模式
        },
    }
}

# ============================================================
# 密码验证器
# ============================================================

AUTH_PASSWORD_VALIDATORS = [
    {'NAME': 'django.contrib.auth.password_validation.UserAttributeSimilarityValidator'},
    {'NAME': 'django.contrib.auth.password_validation.MinimumLengthValidator'},
    {'NAME': 'django.contrib.auth.password_validation.CommonPasswordValidator'},
    {'NAME': 'django.contrib.auth.password_validation.NumericPasswordValidator'},
]

# ============================================================
# 国际化配置
# ============================================================

LANGUAGE_CODE = 'zh-hans'       # 简体中文
TIME_ZONE = 'Asia/Shanghai'     # 上海时区
USE_I18N = True
USE_TZ = True

# ============================================================
# 静态文件与媒体文件
# ============================================================

STATIC_URL = '/static/'                        # 静态文件 URL 前缀
STATIC_ROOT = BASE_DIR / 'staticfiles'         # collectstatic 收集目标
STATICFILES_DIRS = [BASE_DIR / 'static']       # 开发时额外的静态文件目录

MEDIA_URL = '/media/'                          # 用户上传文件 URL 前缀
MEDIA_ROOT = BASE_DIR / 'media'                # 用户上传文件存储目录

# 生产环境 Nginx 与 Gunicorn 进程用户不同，默认 umask 可能导致 uploads 仅属主可读，
# Nginx 用 alias 读 /media/ 会 403。固定为 644/755 可避免新上传文件再次出现「页面能开、文件全 403」。
FILE_UPLOAD_PERMISSIONS = 0o644
FILE_UPLOAD_DIRECTORY_PERMISSIONS = 0o755

# ============================================================
# 其他配置
# ============================================================

# 默认主键类型
DEFAULT_AUTO_FIELD = 'django.db.models.BigAutoField'

# 自定义用户模型（联动 accounts.models.User）
AUTH_USER_MODEL = 'accounts.User'

# CORS 允许的源站
CORS_ALLOWED_ORIGINS = [
    'https://www.teselx.cn',
    'https://teselx.cn',
]

# 文件上传大小限制：1GB（联动 videos/files 上传功能）
FILE_UPLOAD_MAX_MEMORY_SIZE = 1073741824
DATA_UPLOAD_MAX_MEMORY_SIZE = 1073741824
DATA_UPLOAD_MAX_NUMBER_FIELDS = 10240

# Session 配置：24小时过期
SESSION_COOKIE_AGE = 86400
SESSION_SAVE_EVERY_REQUEST = False

# 允许同源 iframe 嵌入（视频播放器需要）
X_FRAME_OPTIONS = 'SAMEORIGIN'
