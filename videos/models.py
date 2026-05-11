"""
videos 模块 - 视频模型

本模块定义视频管理的数据模型：
- Video: 视频模型，包含审核状态、可见性、时长、封面、观看计数等

联动关系：
- Video.uploader 外键关联 accounts.models.User（上传者）
- Video.status 由 core.review_service.ReviewService 管理审核流程
- Video.visibility 可通过 accounts.views.change_video_visibility 变更
- videos.views 中的上传/删除/审核/播放视图操作本模块模型
- videos.utils 提供封面提取和时长获取工具函数
"""
from django.db import models
from django.conf import settings
import os


class Video(models.Model):
    """
    视频模型

    核心字段说明：
    - status: 审核状态（pending/approved/rejected），由 core.review_service 控制
    - visibility: 可见性（public/private），影响列表页展示
    - duration: 视频时长（秒），上传后由后台线程通过 videos.utils.get_video_duration 获取
    - thumbnail: 封面图片，可手动上传或由 videos.utils.extract_thumbnail 自动生成
    - uploader: 外键关联 accounts.models.User

    联动：
    - videos.views.upload_video_view 创建实例，后台线程处理时长和封面
    - videos.views.approve_video_view / reject_video_view 通过 ReviewService 变更状态
    - accounts.views.change_video_visibility 通过 ReviewService 变更可见性
    - accounts.views.profile_view 查询当前用户的视频列表
    """
    STATUS_CHOICES = [
        ('pending', '待审核'),
        ('approved', '已通过'),
        ('rejected', '已驳回'),
    ]
    
    VISIBILITY_CHOICES = [
        ('public', '公开'),
        ('private', '私密'),
    ]
    
    title = models.CharField(max_length=200, verbose_name='标题')
    description = models.TextField(blank=True, verbose_name='简介')
    video_file = models.FileField(upload_to='uploads/videos/', verbose_name='视频文件')
    thumbnail = models.ImageField(upload_to='uploads/videos/thumbnails/', null=True, blank=True, verbose_name='封面图片')
    duration = models.IntegerField(default=0, verbose_name='时长（秒）')
    file_size = models.BigIntegerField(verbose_name='文件大小（字节）')
    
    uploader = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, verbose_name='上传者')
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='pending', verbose_name='状态', db_index=True)
    visibility = models.CharField(max_length=10, choices=VISIBILITY_CHOICES, default='public', verbose_name='可见性', db_index=True)
    
    view_count = models.IntegerField(default=0, verbose_name='观看次数')
    needs_processing = models.BooleanField(default=False, verbose_name='待处理封面/时长')
    
    created_at = models.DateTimeField(auto_now_add=True, verbose_name='上传时间')
    updated_at = models.DateTimeField(auto_now=True, verbose_name='更新时间')
    
    class Meta:
        verbose_name = '视频'
        verbose_name_plural = '视频'
        ordering = ['-view_count', '-created_at']
    
    def __str__(self):
        return self.title
    
    def get_file_size_mb(self):
        """获取文件大小（MB），保留两位小数"""
        return round(self.file_size / (1024 * 1024), 2)
    
    def get_duration_formatted(self):
        """格式化时长为 MM:SS 格式"""
        minutes = self.duration // 60
        seconds = self.duration % 60
        return f"{minutes:02d}:{seconds:02d}"
