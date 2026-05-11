"""
accounts 模块 - URL 路由配置

app_name='accounts'，提供以下路由：
- 页面路由：注册、登录、登出、个人中心
- API 路由：网站设置读取/更新、视频/文件可见性变更

联动关系：
- 所有路由映射到 accounts.views 中对应的视图函数
- change_video_visibility / change_file_visibility 分别操作 videos 和 files 模块的模型
"""
from django.urls import path
from . import views

app_name = 'accounts'

urlpatterns = [
    # 用户认证相关页面
    path('register/', views.register_view, name='register'),
    path('login/', views.login_view, name='login'),
    path('logout/', views.logout_view, name='logout'),
    path('profile/', views.profile_view, name='profile'),
    path('change-password/', views.change_password_view, name='change_password'),
    # 网站设置 API
    path('api/site-settings/', views.get_site_settings, name='api_site_settings'),
    path('api/update-site-settings/', views.update_site_settings, name='api_update_site_settings'),
    # 资源可见性变更 API（联动 videos / files 模块）
    path('api/video/<int:video_id>/change-visibility/', views.change_video_visibility, name='change_video_visibility'),
    path('api/file/<int:file_id>/change-visibility/', views.change_file_visibility, name='change_file_visibility'),
]
