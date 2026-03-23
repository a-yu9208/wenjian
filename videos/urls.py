"""
videos 模块 - URL 路由配置

路由列表(app_name='videos')：
- /              → 首页视频列表
- /videos/<id>/  → 视频详情页
- /videos/<id>/play/ → 视频流播放(支持Range)
- /videos/upload/    → 上传视频
- /videos/<id>/delete/ → 删除视频
- /videos/batch-delete/ → 批量删除
- /videos/<id>/approve/ → 管理员审核通过
- /videos/<id>/reject/  → 管理员驳回

联动：config.urls 通过 include('videos.urls') 引入
"""
from django.urls import path
from . import views

app_name = 'videos'

urlpatterns = [
    path('', views.index, name='index'),
    path('videos/<int:video_id>/', views.video_detail_view, name='detail'),
    path('videos/<int:video_id>/play/', views.video_stream_view, name='play'),
    path('videos/upload/', views.upload_video_view, name='upload'),
    path('videos/<int:video_id>/delete/', views.delete_video_view, name='delete'),
    path('videos/batch-delete/', views.batch_delete_videos_view, name='batch_delete'),
    path('videos/<int:video_id>/approve/', views.approve_video_view, name='approve'),
    path('videos/<int:video_id>/reject/', views.reject_video_view, name='reject'),
]





