"""
files 模块 - 文件管理模型

本模块定义文件管理系统的数据模型：
- FileCategory: 文件分类（图标+名称）
- FileItem: 文件项，包含审核状态、可见性、文件类型、下载/查看计数等

联动关系：
- FileItem.uploader 外键关联 accounts.models.User（上传者）
- FileItem.status 由 core.review_service.ReviewService 管理审核流程
- FileItem.visibility 可通过 accounts.views.change_file_visibility 变更
- files.views 中的上传/删除/审核视图操作本模块模型
"""
from django.db import models
from django.conf import settings


class FileCategory(models.Model):
    """文件分类，用于前端展示分类筛选（图标 + 名称）"""
    name = models.CharField(max_length=20, verbose_name='分类名称')
    icon = models.CharField(max_length=50, verbose_name='图标类名')
    
    class Meta:
        verbose_name = '文件分类'
        verbose_name_plural = '文件分类'
    
    def __str__(self):
        return self.name


class FileItem(models.Model):
    """
    文件项模型

    核心字段说明：
    - status: 审核状态（pending/approved/rejected），由 core.review_service 控制
    - visibility: 可见性（public/private），影响列表页展示和下载权限
    - file_type: 文件类型，上传时根据扩展名自动判断
    - uploader: 外键关联 accounts.models.User

    联动：
    - files.views.upload_file_view 创建实例，调用 ReviewService 确定初始状态
    - files.views.approve_file_view / reject_file_view 通过 ReviewService 变更状态
    - accounts.views.change_file_visibility 通过 ReviewService 变更可见性
    - accounts.views.profile_view 查询当前用户的文件列表
    """
    STATUS_CHOICES = [
        ('pending', '待审核'),
        ('approved', '已通过'),
        ('rejected', '已驳回'),
    ]
    
    FILE_TYPE_CHOICES = [
        ('image', '图片'),
        ('document', '文档'),
        ('archive', '压缩包'),
        ('audio', '音乐'),
        ('other', '其他'),
    ]
    
    VISIBILITY_CHOICES = [
        ('public', '公开'),
        ('private', '私密'),
    ]
    
    title = models.CharField(max_length=200, verbose_name='标题')
    description = models.TextField(blank=True, verbose_name='描述')
    file = models.FileField(upload_to='uploads/files/', verbose_name='文件')
    file_type = models.CharField(max_length=20, choices=FILE_TYPE_CHOICES, verbose_name='文件类型')
    size = models.BigIntegerField(verbose_name='文件大小（字节）')
    uploader = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, verbose_name='上传者')
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='pending', verbose_name='状态', db_index=True)
    visibility = models.CharField(max_length=10, choices=VISIBILITY_CHOICES, default='public', verbose_name='可见性', db_index=True)
    download_count = models.IntegerField(default=0, verbose_name='下载次数')
    view_count = models.IntegerField(default=0, verbose_name='查看次数')
    created_at = models.DateTimeField(auto_now_add=True, verbose_name='上传时间')
    updated_at = models.DateTimeField(auto_now=True, verbose_name='更新时间')
    
    class Meta:
        verbose_name = '文件'
        verbose_name_plural = '文件'
        ordering = ['-created_at']
    
    def __str__(self):
        return self.title
    
    def get_file_size_mb(self):
        """获取文件大小（MB），保留两位小数"""
        return round(self.size / (1024 * 1024), 2)
    
    def get_file_extension(self):
        """获取文件扩展名（小写）"""
        return self.file.name.split('.')[-1].lower()
