"""
files 模块 - URL 路由配置

app_name='files'，提供以下路由：
- 列表/详情/下载：公开访问（详情和下载内部做权限校验）
- 上传/删除/批量删除：需登录
- 审核通过/驳回：需登录且为管理员（在视图层校验）

联动关系：
- 所有路由映射到 files.views 中对应的视图函数
"""
from django.urls import path
from . import views

app_name = 'files'

urlpatterns = [
    path('files/', views.file_list_view, name='list'),                          # 文件列表
    path('files/<int:file_id>/', views.file_detail_view, name='detail'),        # 文件详情
    path('files/<int:file_id>/download/', views.file_download_view, name='download'),  # 文件下载
    path('files/upload/', views.upload_file_view, name='upload'),               # 文件上传
    path('files/<int:file_id>/delete/', views.delete_file_view, name='delete'), # 删除文件
    path('files/batch-delete/', views.batch_delete_files_view, name='batch_delete'),   # 批量删除
    path('files/<int:file_id>/approve/', views.approve_file_view, name='approve'),     # 审核通过
    path('files/<int:file_id>/reject/', views.reject_file_view, name='reject'),        # 审核驳回
]
