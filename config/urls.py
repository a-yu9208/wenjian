"""
URL 路由总配置 - boke 项目

本文件是所有 URL 路由的入口，负责将请求分发到各个模块：
- /admin/          → 管理员仪表盘（core.dashboard_views）
- /notifications/  → 用户通知系统（core.notification_views）
- /api/announcement/ → 弹窗公告 API
- /                → 视频模块路由（videos.urls）
- /files/          → 文件模块路由（files.urls）
- /accounts/       → 用户模块路由（accounts.urls，含 API 接口）

联动模块：
- core.dashboard_views: 管理员仪表盘视图（审核、用户管理、公告、网站设置）
- core.notification_views: 通知列表、已读标记、未读计数、活跃公告
- videos.urls: 视频首页、详情、播放、上传、删除、审核
- files.urls: 文件列表、详情、下载、上传、删除、审核
- accounts.urls: 注册、登录、登出、个人中心、网站设置 API、可见性修改 API
"""
from django.urls import path, include
from django.conf import settings
from django.conf.urls.static import static
from core.dashboard_views import dashboard, dashboard_action, dashboard_upload
from core.notification_views import notification_list, notification_read, notification_count, active_announcement

urlpatterns = [
    # ---- 管理员仪表盘 ----
    path('admin/', dashboard, name='dashboard'),                    # 仪表盘主页（需管理员权限）
    path('admin/action/', dashboard_action, name='dashboard_action'),  # 仪表盘操作 API（审核/通知/公告/用户管理等）
    path('admin/upload/', dashboard_upload, name='dashboard_upload'),  # 仪表盘文件上传（Logo/背景图）

    # ---- 通知系统 ----
    path('notifications/', notification_list, name='notifications'),       # 通知列表页面
    path('notifications/read/', notification_read, name='notification_read'),  # 标记通知已读
    path('notifications/count/', notification_count, name='notification_count'),  # 未读通知计数 API
    path('api/announcement/', active_announcement, name='active_announcement'),  # 获取活跃弹窗公告

    # ---- 各模块路由 include ----
    path('', include('videos.urls')),      # 视频模块（首页 / 即视频列表）
    path('', include('files.urls')),       # 文件模块
    path('', include('accounts.urls')),    # 用户模块（注册/登录/个人中心/API）
]

# 开发模式下提供媒体文件和静态文件的直接访问
if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
    urlpatterns += static(settings.STATIC_URL, document_root=settings.STATIC_ROOT)
