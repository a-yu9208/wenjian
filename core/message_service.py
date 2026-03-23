"""
消息服务模块 - core.message_service

本模块统一管理系统中所有的用户提示消息和 JSON 响应，提供：

1. MessageService - 系统消息服务
   - 预定义消息模板（用户、视频、文件、审核、可见性、通用）
   - Django messages 框架集成（页面跳转场景）
   - 标准化 JSON 响应（AJAX 场景）

2. 便捷函数
   - msg_success/msg_error: 快速添加 Django 消息
   - json_success/json_error: 快速创建 JSON 响应

联动模块：
- accounts.views: 注册/登录/个人中心使用 msg_success/msg_error
- videos.views: 视频上传/删除/审核使用 json_success/json_error
- files.views: 文件上传/删除/审核使用 json_success/json_error
- accounts.views: 可见性修改使用 MessageService 响应方法
"""
from django.contrib import messages
from django.http import JsonResponse
from typing import Optional, Dict, Any


class MessageLevel:
    """消息级别常量，对应 Django messages 框架的级别"""
    SUCCESS = 'success'
    ERROR = 'error'
    WARNING = 'warning'
    INFO = 'info'
    DEBUG = 'debug'


class MessageService:
    """
    消息服务类 - 统一处理系统消息

    所有面向用户的提示文案都集中在此类的 MESSAGES 字典中管理，
    便于统一修改和国际化。支持两种使用场景：
    1. 页面跳转：通过 Django messages 框架显示 flash 消息
    2. AJAX 请求：返回标准化的 JSON 响应
    """

    # 预定义消息模板，按业务域分组
    MESSAGES = {
        # ---- 用户相关 ----
        'user.register.success': '注册成功，请登录',
        'user.register.username_exists': '用户名已存在，请更换',
        'user.register.email_exists': '邮箱已被使用，请更换',
        'user.register.password_mismatch': '两次输入的密码不一致',
        'user.register.password_too_short': '密码至少需要6位',
        'user.login.failed': '用户名或密码错误',
        'user.logout.success': '已退出登录',
        'user.profile.updated': '个人资料更新成功',
        'user.permission.denied': '权限不足',

        # ---- 视频相关 ----
        'video.upload.success': '视频上传成功',
        'video.upload.success_private': '私密视频上传成功，只有您能看到此视频',
        'video.upload.pending': '视频上传成功，等待管理员审核',
        'video.upload.failed': '视频上传失败',
        'video.upload.size_limit': '文件大小不能超过1GB',
        'video.upload.no_file': '请选择视频文件',
        'video.delete.success': '视频已删除',
        'video.delete.failed': '删除失败',
        'video.batch_delete.success': '已删除 {count} 个视频',
        'video.not_found': '视频不存在',

        # ---- 文件相关 ----
        'file.upload.success': '文件上传成功',
        'file.upload.success_private': '私密文件上传成功，只有您能看到此文件',
        'file.upload.pending': '文件上传成功，等待管理员审核',
        'file.upload.failed': '文件上传失败',
        'file.upload.no_file': '请选择文件',
        'file.delete.success': '文件已删除',
        'file.delete.failed': '删除失败',
        'file.batch_delete.success': '已删除 {count} 个文件',
        'file.not_found': '文件不存在',

        # ---- 审核相关 ----
        'review.approve.success': '{type}已通过审核',
        'review.reject.success': '{type}已驳回',
        'review.reject.no_reason': '请填写驳回理由',
        'review.batch_approve.success': '已通过 {count} 个内容的审核',
        'review.batch_reject.success': '已驳回 {count} 个内容',

        # ---- 可见性相关 ----
        'visibility.change.to_public': '已转为公开',
        'visibility.change.to_public_pending': '已转为公开，等待管理员审核',
        'visibility.change.to_private': '已设为私密',
        'visibility.change.invalid': '无效的可见性值',
        'visibility.change.no_change': '可见性未改变',

        # ---- 通用消息 ----
        'common.operation.success': '操作成功',
        'common.operation.failed': '操作失败',
        'common.invalid_method': '无效的请求方法',
        'common.invalid_params': '参数错误',
        'common.server_error': '服务器错误，请稍后重试',
    }

    @staticmethod
    def get_message(key: str, **kwargs) -> str:
        """根据消息键获取格式化后的消息文本，支持 {param} 占位符"""
        template = MessageService.MESSAGES.get(key, key)
        try:
            return template.format(**kwargs)
        except KeyError:
            return template

    @staticmethod
    def add_message(request, level: str, key: str, **kwargs):
        """添加 Django flash 消息（用于页面跳转后显示提示）"""
        message_text = MessageService.get_message(key, **kwargs)
        level_map = {
            MessageLevel.SUCCESS: messages.success,
            MessageLevel.ERROR: messages.error,
            MessageLevel.WARNING: messages.warning,
            MessageLevel.INFO: messages.info,
            MessageLevel.DEBUG: messages.debug,
        }
        handler = level_map.get(level, messages.info)
        handler(request, message_text)

    @staticmethod
    def success(request, key: str, **kwargs):
        """添加成功消息"""
        MessageService.add_message(request, MessageLevel.SUCCESS, key, **kwargs)

    @staticmethod
    def error(request, key: str, **kwargs):
        """添加错误消息"""
        MessageService.add_message(request, MessageLevel.ERROR, key, **kwargs)

    @staticmethod
    def warning(request, key: str, **kwargs):
        """添加警告消息"""
        MessageService.add_message(request, MessageLevel.WARNING, key, **kwargs)

    @staticmethod
    def info(request, key: str, **kwargs):
        """添加信息消息"""
        MessageService.add_message(request, MessageLevel.INFO, key, **kwargs)

    @staticmethod
    def json_response(
        success: bool,
        message_key: str = None,
        data: Dict[str, Any] = None,
        status: int = None,
        **kwargs
    ) -> JsonResponse:
        """
        创建标准化的 JSON 响应（用于 AJAX 场景）

        响应格式：{"success": bool, "message": str, ...extra_data}
        """
        response_data = {'success': success}
        if message_key:
            response_data['message'] = MessageService.get_message(message_key, **kwargs)
        if data:
            response_data.update(data)
        if status is None:
            status = 200 if success else 400
        return JsonResponse(response_data, status=status)

    @staticmethod
    def success_response(message_key: str, data: Dict[str, Any] = None, **kwargs) -> JsonResponse:
        """创建成功 JSON 响应（HTTP 200）"""
        return MessageService.json_response(True, message_key, data, 200, **kwargs)

    @staticmethod
    def error_response(message_key: str, status: int = 400, data: Dict[str, Any] = None, **kwargs) -> JsonResponse:
        """创建错误 JSON 响应"""
        return MessageService.json_response(False, message_key, data, status, **kwargs)

    @staticmethod
    def permission_denied_response() -> JsonResponse:
        """创建权限不足响应（HTTP 403）"""
        return MessageService.error_response('user.permission.denied', status=403)

    @staticmethod
    def not_found_response(resource_type: str = 'resource') -> JsonResponse:
        """创建资源不存在响应（HTTP 404），根据资源类型返回对应消息"""
        key_map = {'video': 'video.not_found', 'file': 'file.not_found'}
        return MessageService.error_response(key_map.get(resource_type, 'common.operation.failed'), status=404)

    @staticmethod
    def invalid_method_response() -> JsonResponse:
        """创建无效请求方法响应（HTTP 405）"""
        return MessageService.error_response('common.invalid_method', status=405)

    @staticmethod
    def server_error_response() -> JsonResponse:
        """创建服务器错误响应（HTTP 500）"""
        return MessageService.error_response('common.server_error', status=500)


# ============================================================
# 便捷函数 - 简化调用
# ============================================================

def msg_success(request, key: str, **kwargs):
    """快捷方式：添加成功消息（联动 accounts.views 等页面跳转场景）"""
    MessageService.success(request, key, **kwargs)


def msg_error(request, key: str, **kwargs):
    """快捷方式：添加错误消息"""
    MessageService.error(request, key, **kwargs)


def json_success(message_key: str, data: Dict[str, Any] = None, **kwargs) -> JsonResponse:
    """快捷方式：创建成功 JSON 响应（联动 videos.views / files.views 等 AJAX 场景）"""
    return MessageService.success_response(message_key, data, **kwargs)


def json_error(message_key: str, status: int = 400, **kwargs) -> JsonResponse:
    """快捷方式：创建错误 JSON 响应"""
    return MessageService.error_response(message_key, status, **kwargs)
