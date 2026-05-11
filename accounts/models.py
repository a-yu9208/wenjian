"""
accounts 模块 - 用户与网站设置模型

本模块定义了系统的用户模型和网站设置模型：
- User: 扩展 Django AbstractUser，增加角色、头像、电话等字段
- SiteSettings: 网站全局配置单例模型

联动关系：
- User 被 videos.models.Video、files.models.FileItem 的 uploader 字段外键关联
- User.is_admin_role 属性在 accounts.views、files.views、videos.views 中用于权限判断
- SiteSettings 在 accounts.views 中通过 API 提供给前端读取和更新
"""
from django.contrib.auth.models import AbstractUser
from django.db import models


class User(AbstractUser):
    """
    扩展用户模型

    在 Django 内置用户基础上增加 role（角色）、avatar（头像）、phone（电话）字段。
    通过 is_admin_role 属性统一判断管理员权限（含 superuser）。

    联动：
    - videos.models.Video.uploader -> 本模型（视频上传者）
    - files.models.FileItem.uploader -> 本模型（文件上传者）
    - core.models.LoginLog 记录本模型用户的登录日志
    """
    ROLE_CHOICES = [
        ('user', '普通用户'),
        ('admin', '管理员'),
    ]
    
    role = models.CharField(max_length=10, choices=ROLE_CHOICES, default='user', verbose_name='角色')
    avatar = models.ImageField(upload_to='avatars/', null=True, blank=True, verbose_name='头像')
    phone = models.CharField(max_length=20, blank=True, verbose_name='电话')
    created_at = models.DateTimeField(auto_now_add=True, verbose_name='创建时间')
    updated_at = models.DateTimeField(auto_now=True, verbose_name='更新时间')
    
    class Meta:
        verbose_name = '用户'
        verbose_name_plural = '用户'
    
    def __str__(self):
        return self.username
    
    @property
    def is_admin_role(self):
        """判断是否为管理员（role='admin' 或 superuser 均视为管理员）"""
        return self.role == 'admin' or self.is_superuser


class SiteSettings(models.Model):
    """
    网站设置（单例模型）

    存储网站名称、背景图片、主题色、Logo 等全局配置。
    通过 save() 方法确保数据库中只保留一条记录。

    联动：
    - accounts.views.get_site_settings: 以 JSON 返回设置供前端使用
    - accounts.views.update_site_settings: 管理员更新设置
    """
    site_name = models.CharField(max_length=100, default='我的文件管理系统', verbose_name='网站名称')
    background_image = models.ImageField(upload_to='settings/', null=True, blank=True, verbose_name='背景图片')
    primary_color = models.CharField(max_length=7, default='#1890ff', verbose_name='主题色')
    secondary_color = models.CharField(max_length=7, default='#52c41a', verbose_name='辅助色')
    logo = models.ImageField(upload_to='settings/', null=True, blank=True, verbose_name='Logo')
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    
    class Meta:
        verbose_name = '网站设置'
        verbose_name_plural = '网站设置'
    
    def __str__(self):
        return self.site_name
    
    def save(self, *args, **kwargs):
        # 强制 pk=1 实现单例，避免并发 delete+create 丢数据
        self.pk = 1
        super().save(*args, **kwargs)
