"""
WSGI 应用入口 - boke 项目

本模块是 Web 服务器（Gunicorn / Nginx）与 Django 之间的桥梁，
提供符合 WSGI 协议的 application 可调用对象。

联动模块：
- config.settings: 通过环境变量 DJANGO_SETTINGS_MODULE 指定配置文件
- gunicorn_config.py: Gunicorn 启动时加载本模块中的 application 对象
"""

import os

from django.core.wsgi import get_wsgi_application

# 指定 Django 配置模块，Gunicorn 启动时据此加载 settings
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings')

# WSGI 应用对象，Gunicorn 通过 config.wsgi:application 引用
application = get_wsgi_application()
