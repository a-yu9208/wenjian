"""
核心数据模型 - core 模块

本文件定义了系统级的数据模型，为整个项目提供基础支撑：

1. NotificationTemplate - 自动通知模板
   管理员可编辑的通知模板，用于审核通过/驳回时自动发送通知给用户。
   联动：dashboard_views（管理员编辑模板）、Notification.send_auto（触发发送）

2. Notification - 用户通知
   站内通知消息，支持手动发送和模板自动发送。
   联动：notification_views（用户查看/标记已读）、dashboard_views（管理员发送/群发）

3. AdminLog - 管理员操作日志
   记录管理员的所有操作（审核、删除、修改设置等），用于审计追踪。
   联动：dashboard_views（所有管理操作都会记录日志）

4. LoginLog - 登录日志
   记录所有登录尝试（成功/失败），含 IP 地址。
   联动：accounts.views.login_view（登录时记录）

5. Announcement - 弹窗公告
   全站弹窗公告，支持启用/停用。
   联动：notification_views.active_announcement（前端获取）、dashboard_views（管理员管理）
"""
from django.db import models
from django.conf import settings


class NotificationTemplate(models.Model):
    """
    自动通知模板

    管理员可在仪表盘中编辑模板内容，当视频/文件审核状态变更时，
    系统会根据模板自动生成通知发送给对应用户。

    支持的模板变量：{title}（内容标题）、{username}（用户名）

    联动：
    - dashboard_views._handle_save_template: 管理员编辑模板
    - dashboard_views._handle_review: 审核时通过 Notification.send_auto 触发
    """
    KEY_CHOICES = [
        ('video_approved', '视频审核通过'),
        ('video_rejected', '视频审核驳回'),
        ('file_approved', '文件审核通过'),
        ('file_rejected', '文件审核驳回'),
        ('video_public', '视频转为公开'),
        ('file_public', '文件转为公开'),
    ]
    key = models.CharField(max_length=30, choices=KEY_CHOICES, unique=True, verbose_name='模板标识')
    title = models.CharField(max_length=200, verbose_name='标题模板')
    content = models.TextField(verbose_name='内容模板', help_text='可用变量: {title}, {username}')

    class Meta:
        verbose_name = '通知模板'
        verbose_name_plural = '通知模板'

    def __str__(self):
        return self.get_key_display()

    @classmethod
    def get_defaults(cls):
        """获取所有模板的默认值，用于初始化或模板不存在时的降级"""
        return {
            'video_approved': ('您的视频已通过审核', '您上传的视频「{title}」已通过审核，现在可以公开访问了。'),
            'video_rejected': ('您的视频未通过审核', '您上传的视频「{title}」未通过审核，已转为私密。'),
            'file_approved': ('您的文件已通过审核', '您上传的文件「{title}」已通过审核，现在可以公开访问了。'),
            'file_rejected': ('您的文件未通过审核', '您上传的文件「{title}」未通过审核。'),
            'video_public': ('您的视频已转为公开', '您的视频「{title}」已转为公开状态，等待审核。'),
            'file_public': ('您的文件已转为公开', '您的文件「{title}」已转为公开状态，等待审核。'),
        }

    @classmethod
    def ensure_defaults(cls):
        """确保所有默认模板存在（项目初始化时调用）"""
        for key, (title, content) in cls.get_defaults().items():
            cls.objects.get_or_create(key=key, defaults={'title': title, 'content': content})


class Notification(models.Model):
    """
    用户通知

    支持两种发送方式：
    1. send() - 直接发送，指定标题和内容（管理员手动发送/群发）
    2. send_auto() - 根据模板自动发送（审核状态变更时触发）

    联动：
    - notification_views: 用户查看通知列表、标记已读、获取未读计数
    - dashboard_views._handle_send_notification: 管理员手动发送/群发
    - dashboard_views._handle_review: 审核操作触发自动通知
    """
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='notifications', verbose_name='接收用户')
    title = models.CharField(max_length=200, verbose_name='标题')
    content = models.TextField(verbose_name='内容')
    is_read = models.BooleanField(default=False, verbose_name='已读')
    created_at = models.DateTimeField(auto_now_add=True, verbose_name='创建时间')

    class Meta:
        verbose_name = '通知'
        verbose_name_plural = '通知'
        ordering = ['-created_at']

    def __str__(self):
        return f'{self.user.username}: {self.title}'

    @classmethod
    def send(cls, user, title, content):
        """直接发送通知给指定用户"""
        return cls.objects.create(user=user, title=title, content=content)

    @classmethod
    def send_auto(cls, user, template_key, **kwargs):
        """
        根据模板发送自动通知

        优先使用数据库中的模板，若模板不存在则使用内置默认值。
        kwargs 中的参数会被格式化到模板的 title 和 content 中。
        """
        try:
            tpl = NotificationTemplate.objects.get(key=template_key)
            title = tpl.title.format(**kwargs)
            content = tpl.content.format(**kwargs)
        except NotificationTemplate.DoesNotExist:
            defaults = NotificationTemplate.get_defaults().get(template_key)
            if defaults:
                title = defaults[0].format(**kwargs)
                content = defaults[1].format(**kwargs)
            else:
                return None
        return cls.send(user, title, content)


class AdminLog(models.Model):
    """
    管理员操作日志

    记录管理员在仪表盘中的所有操作，包括：
    审核通过/驳回、发送通知、修改模板、管理公告、删除内容、
    修改用户角色、重置密码、启用/禁用用户、修改网站设置等。

    联动：
    - dashboard_views: 所有管理操作都通过 AdminLog.log() 记录
    """
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.SET_NULL, null=True, verbose_name='操作人')
    action = models.CharField(max_length=200, verbose_name='操作')
    detail = models.TextField(blank=True, verbose_name='详情')
    ip = models.GenericIPAddressField(null=True, blank=True, verbose_name='IP')
    created_at = models.DateTimeField(auto_now_add=True, verbose_name='时间')

    class Meta:
        verbose_name = '操作日志'
        verbose_name_plural = '操作日志'
        ordering = ['-created_at']

    def __str__(self):
        return f'{self.user}: {self.action}'

    @classmethod
    def log(cls, request, action, detail=''):
        """
        记录一条操作日志

        自动从请求中提取操作人和 IP 地址（支持 X-Forwarded-For 代理头）
        """
        ip = request.META.get('HTTP_X_FORWARDED_FOR', '').split(',')[0].strip() or request.META.get('REMOTE_ADDR')
        return cls.objects.create(user=request.user, action=action, detail=detail, ip=ip)


class LoginLog(models.Model):
    """
    登录日志

    记录所有登录尝试，包括成功和失败的登录。
    失败登录时 user 字段为 null，但 username 字段会记录尝试的用户名。

    联动：
    - accounts.views.login_view: 登录成功/失败时调用 LoginLog.record()
    """
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.SET_NULL, null=True, verbose_name='用户')
    username = models.CharField(max_length=150, verbose_name='用户名')
    ip = models.GenericIPAddressField(null=True, blank=True, verbose_name='IP')
    success = models.BooleanField(default=True, verbose_name='是否成功')
    created_at = models.DateTimeField(auto_now_add=True, verbose_name='时间')

    class Meta:
        verbose_name = '登录日志'
        verbose_name_plural = '登录日志'
        ordering = ['-created_at']

    def __str__(self):
        return f'{self.username} {"✓" if self.success else "✗"}'

    @classmethod
    def record(cls, request, user, username, success=True):
        """记录一条登录日志，自动提取 IP 地址"""
        ip = request.META.get('HTTP_X_FORWARDED_FOR', '').split(',')[0].strip() or request.META.get('REMOTE_ADDR')
        return cls.objects.create(user=user if success else None, username=username, ip=ip, success=success)


class Announcement(models.Model):
    """
    弹窗公告

    全站弹窗公告，前端页面加载时会请求活跃公告并弹窗展示。
    支持启用/停用切换，同一时间只展示最新的一条活跃公告。

    联动：
    - notification_views.active_announcement: 前端获取活跃公告的 API
    - dashboard_views._handle_save_announcement: 管理员创建/编辑公告
    - dashboard_views._handle_delete_announcement: 管理员删除公告
    - dashboard_views._handle_toggle_announcement: 管理员启用/停用公告
    """
    title = models.CharField(max_length=200, verbose_name='标题')
    content = models.TextField(verbose_name='内容')
    is_active = models.BooleanField(default=True, verbose_name='启用')
    created_at = models.DateTimeField(auto_now_add=True, verbose_name='创建时间')
    updated_at = models.DateTimeField(auto_now=True, verbose_name='更新时间')

    class Meta:
        verbose_name = '弹窗公告'
        verbose_name_plural = '弹窗公告'
        ordering = ['-updated_at']

    def __str__(self):
        return self.title
