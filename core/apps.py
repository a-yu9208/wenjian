"""
Core 应用配置 - boke 项目

本模块定义 core 应用的 Django AppConfig，用于应用注册和元信息声明。
core 是项目的核心模块，包含：
- 通知系统（notification_views）
- 管理员仪表盘（dashboard_views）：审核、用户管理、公告、网站设置
- 弹窗公告功能

联动模块：
- config.settings INSTALLED_APPS: 通过 'core' 注册本应用
- accounts: 用户权限校验（管理员判断）
- files / videos: 审核流程由仪表盘统一管理
"""
from django.apps import AppConfig


class CoreConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'  # 主键使用 BigAutoField
    name = 'core'              # 应用模块名，需与目录名一致
    verbose_name = '核心'      # 后台管理界面显示名称
