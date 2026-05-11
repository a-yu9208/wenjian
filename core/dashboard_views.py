"""
管理员仪表盘视图模块 - core.dashboard_views

本模块提供管理员后台的所有功能，包括：

1. dashboard - 仪表盘主页，汇总展示所有管理数据
2. dashboard_action - 统一的 POST 操作入口，根据 type 分发到对应 handler
3. dashboard_upload - 网站设置文件上传（Logo/背景图）

操作 handler 列表：
- _handle_review: 审核视频/文件（通过/驳回/转公开），联动 Notification.send_auto 自动通知
- _handle_send_notification: 发送/群发通知，联动 Notification.send
- _handle_save_template: 编辑通知模板，联动 NotificationTemplate
- _handle_save_announcement: 创建/编辑公告，联动 Announcement
- _handle_delete_announcement: 删除公告
- _handle_toggle_announcement: 启用/停用公告
- _handle_change_status: 直接变更内容审核状态
- _handle_delete_content: 删除视频/文件（含物理文件清理）
- _handle_change_user_role: 修改用户角色（user/admin）
- _handle_reset_user_password: 重置用户密码
- _handle_toggle_user_active: 启用/禁用用户
- _handle_save_site_settings: 修改网站设置（名称/颜色/清除图片）

联动模块：
- core.models: Notification, NotificationTemplate, Announcement, AdminLog, LoginLog
- accounts.models: User（用户管理）, SiteSettings（网站设置）
- videos.models: Video（视频审核/删除/状态变更）
- files.models: FileItem（文件审核/删除/状态变更）

权限控制：所有视图均需登录 + 管理员权限（is_superuser/is_staff/role='admin'）
所有操作均通过 AdminLog.log() 记录审计日志
"""
import json
from django.shortcuts import render
from django.http import JsonResponse
from django.contrib.auth.decorators import login_required, user_passes_test
from django.views.decorators.http import require_POST
from django.db.models import Count, Q
from videos.models import Video
from files.models import FileItem
from accounts.models import User
from core.models import Notification, NotificationTemplate, Announcement, AdminLog, LoginLog


def is_admin(user):
    """管理员权限检查，用于 @user_passes_test 装饰器"""
    return user.is_superuser or user.is_staff or getattr(user, 'role', '') == 'admin'


@login_required(login_url='/accounts/login/')
@user_passes_test(is_admin, login_url='/')
def dashboard(request):
    """
    管理员仪表盘主页

    汇总查询并展示：待审核/已审核/全部内容统计、用户列表、
    通知模板、最近通知、公告列表、操作日志、登录日志、网站设置。
    联动：accounts.models.SiteSettings 获取网站配置。
    """
    from accounts.models import SiteSettings
    pending_videos = Video.objects.filter(status='pending').select_related('uploader')
    pending_files = FileItem.objects.filter(status='pending').select_related('uploader')
    all_videos = Video.objects.all().select_related('uploader')[:50]
    all_files = FileItem.objects.all().select_related('uploader')[:50]

    # 用聚合查询一次拿到所有统计，替代 6 次 COUNT
    video_stats = Video.objects.aggregate(
        pending=Count('id', filter=Q(status='pending')),
        approved=Count('id', filter=Q(status='approved')),
        total=Count('id'),
    )
    file_stats = FileItem.objects.aggregate(
        pending=Count('id', filter=Q(status='pending')),
        approved=Count('id', filter=Q(status='approved')),
        total=Count('id'),
    )
    pending_count = video_stats['pending'] + file_stats['pending']
    approved_count = video_stats['approved'] + file_stats['approved']
    total_count = video_stats['total'] + file_stats['total']

    site_obj, _ = SiteSettings.objects.get_or_create(pk=1)

    return render(request, 'dashboard.html', {
        'pending_videos': pending_videos,
        'pending_files': pending_files,
        'all_videos': all_videos,
        'all_files': all_files,
        'pending_count': pending_count,
        'approved_count': approved_count,
        'total_count': total_count,
        'user_count': User.objects.count(),
        'users': User.objects.all()[:50],
        'notification_templates': NotificationTemplate.objects.all(),
        'recent_notifications': Notification.objects.select_related('user').order_by('-created_at')[:20],
        'announcements': Announcement.objects.all(),
        'admin_logs': AdminLog.objects.select_related('user').all()[:50],
        'login_logs': LoginLog.objects.all()[:50],
        'site_obj': site_obj,
    })


@login_required(login_url='/accounts/login/')
@user_passes_test(is_admin, login_url='/')
@require_POST
def dashboard_action(request):
    """
    仪表盘统一操作入口（POST，JSON body）

    根据请求体中的 type 字段分发到对应的 handler 函数。
    所有操作异常统一捕获并返回 500 错误。
    """
    try:
        data = json.loads(request.body)
        act_type = data.get('type', 'review')

        handlers = {
            'review': _handle_review,
            'send_notification': _handle_send_notification,
            'save_template': _handle_save_template,
            'save_announcement': _handle_save_announcement,
            'delete_announcement': _handle_delete_announcement,
            'toggle_announcement': _handle_toggle_announcement,
            'change_status': _handle_change_status,
            'delete_content': _handle_delete_content,
            'change_user_role': _handle_change_user_role,
            'reset_user_password': _handle_reset_user_password,
            'toggle_user_active': _handle_toggle_user_active,
            'save_site_settings': _handle_save_site_settings,
        }
        handler = handlers.get(act_type)
        if handler:
            return handler(data, request)
        return JsonResponse({'ok': False, 'msg': '无效操作'}, status=400)
    except (Video.DoesNotExist, FileItem.DoesNotExist, User.DoesNotExist,
            NotificationTemplate.DoesNotExist, Announcement.DoesNotExist):
        return JsonResponse({'ok': False, 'msg': '对象不存在'}, status=404)
    except Exception as e:
        return JsonResponse({'ok': False, 'msg': str(e)}, status=500)


def _handle_review(data, request):
    """
    审核操作：通过(approve)/驳回(reject)/转公开(make_public)

    根据 model(video/file) 和 action 执行对应操作，
    审核后通过 Notification.send_auto 自动发送通知给上传者，
    并通过 AdminLog.log 记录操作日志。
    注意：驳回视频时会同时设为私密。
    """
    model = data.get('model')
    act = data.get('action')
    obj_id = data.get('id')

    if model == 'video':
        obj = Video.objects.get(id=obj_id)
        if act == 'approve':
            obj.status = 'approved'; obj.save()
            Notification.send_auto(obj.uploader, 'video_approved', title=obj.title, username=obj.uploader.username)
            AdminLog.log(request, '审核通过视频', f'视频「{obj.title}」(ID:{obj.id})')
            return JsonResponse({'ok': True, 'msg': f'视频「{obj.title}」已通过'})
        elif act == 'reject':
            obj.status = 'rejected'; obj.visibility = 'private'; obj.save()
            Notification.send_auto(obj.uploader, 'video_rejected', title=obj.title, username=obj.uploader.username)
            AdminLog.log(request, '驳回视频', f'视频「{obj.title}」(ID:{obj.id})')
            return JsonResponse({'ok': True, 'msg': f'视频「{obj.title}」已驳回'})
        elif act == 'make_public':
            obj.visibility = 'public'; obj.status = 'pending'; obj.save()
            Notification.send_auto(obj.uploader, 'video_public', title=obj.title, username=obj.uploader.username)
            AdminLog.log(request, '视频转公开', f'视频「{obj.title}」(ID:{obj.id})')
            return JsonResponse({'ok': True, 'msg': f'视频「{obj.title}」已转公开'})
    elif model == 'file':
        obj = FileItem.objects.get(id=obj_id)
        if act == 'approve':
            obj.status = 'approved'; obj.save()
            Notification.send_auto(obj.uploader, 'file_approved', title=obj.title, username=obj.uploader.username)
            AdminLog.log(request, '审核通过文件', f'文件「{obj.title}」(ID:{obj.id})')
            return JsonResponse({'ok': True, 'msg': f'文件「{obj.title}」已通过'})
        elif act == 'reject':
            obj.status = 'rejected'; obj.save()
            Notification.send_auto(obj.uploader, 'file_rejected', title=obj.title, username=obj.uploader.username)
            AdminLog.log(request, '驳回文件', f'文件「{obj.title}」(ID:{obj.id})')
            return JsonResponse({'ok': True, 'msg': f'文件「{obj.title}」已驳回'})
        elif act == 'make_public':
            obj.visibility = 'public'; obj.status = 'pending'; obj.save()
            Notification.send_auto(obj.uploader, 'file_public', title=obj.title, username=obj.uploader.username)
            AdminLog.log(request, '文件转公开', f'文件「{obj.title}」(ID:{obj.id})')
            return JsonResponse({'ok': True, 'msg': f'文件「{obj.title}」已转公开'})
    return JsonResponse({'ok': False, 'msg': '无效操作'}, status=400)


def _handle_send_notification(data, request):
    """
    发送通知：支持单用户发送和群发（user_id='all'）

    联动：Notification.send 创建通知记录，AdminLog.log 记录操作。
    """
    user_id = data.get('user_id')
    title = data.get('title', '').strip()
    content = data.get('content', '').strip()
    if not title or not content:
        return JsonResponse({'ok': False, 'msg': '标题和内容不能为空'}, status=400)
    if user_id == 'all':
        users = User.objects.all()
        for u in users:
            Notification.send(u, title, content)
        AdminLog.log(request, '群发通知', f'标题「{title}」，{users.count()}人')
        return JsonResponse({'ok': True, 'msg': f'已发送给全部 {users.count()} 个用户'})
    else:
        user = User.objects.get(id=user_id)
        Notification.send(user, title, content)
        AdminLog.log(request, '发送通知', f'给{user.username}，标题「{title}」')
        return JsonResponse({'ok': True, 'msg': f'已发送给 {user.username}'})


def _handle_save_template(data, request):
    """编辑通知模板的标题和内容，联动 NotificationTemplate 模型"""
    tpl_id = data.get('id')
    title = data.get('title', '').strip()
    content = data.get('content', '').strip()
    if not title or not content:
        return JsonResponse({'ok': False, 'msg': '标题和内容不能为空'}, status=400)
    tpl = NotificationTemplate.objects.get(id=tpl_id)
    tpl.title = title; tpl.content = content; tpl.save()
    AdminLog.log(request, '修改通知模板', f'模板「{tpl.get_key_display()}」')
    return JsonResponse({'ok': True, 'msg': f'模板「{tpl.get_key_display()}」已保存'})


def _handle_save_announcement(data, request):
    """创建或编辑弹窗公告，有 id 则更新，无 id 则新建"""
    ann_id = data.get('id')
    title = data.get('title', '').strip()
    content = data.get('content', '').strip()
    if not title or not content:
        return JsonResponse({'ok': False, 'msg': '标题和内容不能为空'}, status=400)
    if ann_id:
        ann = Announcement.objects.get(id=ann_id)
        ann.title = title; ann.content = content; ann.save()
        AdminLog.log(request, '更新公告', f'公告「{title}」(ID:{ann_id})')
        return JsonResponse({'ok': True, 'msg': '公告已更新'})
    else:
        Announcement.objects.create(title=title, content=content, is_active=True)
        AdminLog.log(request, '创建公告', f'公告「{title}」')
        return JsonResponse({'ok': True, 'msg': '公告已创建'})


def _handle_delete_announcement(data, request):
    """删除弹窗公告"""
    ann = Announcement.objects.filter(id=data.get('id')).first()
    title = ann.title if ann else '未知'
    Announcement.objects.filter(id=data.get('id')).delete()
    AdminLog.log(request, '删除公告', f'公告「{title}」')
    return JsonResponse({'ok': True, 'msg': '公告已删除'})


def _handle_toggle_announcement(data, request):
    """切换公告启用/停用状态"""
    ann = Announcement.objects.get(id=data.get('id'))
    ann.is_active = not ann.is_active; ann.save()
    status = '启用' if ann.is_active else '停用'
    AdminLog.log(request, f'{status}公告', f'公告「{ann.title}」')
    return JsonResponse({'ok': True, 'msg': f'公告已{status}'})


def _handle_change_status(data, request):
    """直接变更视频/文件的审核状态（pending/approved/rejected）"""
    model = data.get('model')
    obj_id = data.get('id')
    new_status = data.get('status')
    if new_status not in ('pending', 'approved', 'rejected'):
        return JsonResponse({'ok': False, 'msg': '无效状态'}, status=400)
    if model == 'video':
        obj = Video.objects.get(id=obj_id)
        obj.status = new_status; obj.save()
        AdminLog.log(request, '变更视频状态', f'视频「{obj.title}」→{obj.get_status_display()}')
        return JsonResponse({'ok': True, 'msg': f'视频「{obj.title}」状态已改为{obj.get_status_display()}'})
    elif model == 'file':
        obj = FileItem.objects.get(id=obj_id)
        obj.status = new_status; obj.save()
        AdminLog.log(request, '变更文件状态', f'文件「{obj.title}」→{obj.get_status_display()}')
        return JsonResponse({'ok': True, 'msg': f'文件「{obj.title}」状态已改为{obj.get_status_display()}'})
    return JsonResponse({'ok': False, 'msg': '无效操作'}, status=400)


def _handle_delete_content(data, request):
    """
    删除视频/文件，同时清理关联的物理文件（视频文件、缩略图、附件）
    """
    model = data.get('model')
    obj_id = data.get('id')
    if model == 'video':
        obj = Video.objects.get(id=obj_id)
        title = obj.title
        if obj.video_file:
            obj.video_file.delete(save=False)
        if obj.thumbnail:
            obj.thumbnail.delete(save=False)
        obj.delete()
        AdminLog.log(request, '删除视频', f'视频「{title}」(ID:{obj_id})')
        return JsonResponse({'ok': True, 'msg': f'视频「{title}」已删除'})
    elif model == 'file':
        obj = FileItem.objects.get(id=obj_id)
        title = obj.title
        if obj.file:
            obj.file.delete(save=False)
        obj.delete()
        AdminLog.log(request, '删除文件', f'文件「{title}」(ID:{obj_id})')
        return JsonResponse({'ok': True, 'msg': f'文件「{title}」已删除'})
    return JsonResponse({'ok': False, 'msg': '无效操作'}, status=400)


def _handle_change_user_role(data, request):
    """修改用户角色（user/admin），同步更新 is_staff 字段，禁止修改超级管理员"""
    uid = data.get('id')
    role = data.get('role')
    if role not in ('user', 'admin'):
        return JsonResponse({'ok': False, 'msg': '无效角色'}, status=400)
    u = User.objects.get(id=uid)
    if u.is_superuser:
        return JsonResponse({'ok': False, 'msg': '不能修改超级管理员角色'}, status=403)
    u.role = role
    u.is_staff = (role == 'admin')
    u.save()
    AdminLog.log(request, '变更用户角色', f'{u.username} → {u.get_role_display()}')
    return JsonResponse({'ok': True, 'msg': f'{u.username} 角色已改为{u.get_role_display()}'})


def _handle_reset_user_password(data, request):
    """重置用户密码（最少6位），禁止通过此方式修改超级管理员密码"""
    uid = data.get('id')
    new_pwd = data.get('password', '').strip()
    if len(new_pwd) < 6:
        return JsonResponse({'ok': False, 'msg': '密码至少6位'}, status=400)
    u = User.objects.get(id=uid)
    if u.is_superuser:
        return JsonResponse({'ok': False, 'msg': '不能通过此方式修改超级管理员密码'}, status=403)
    u.set_password(new_pwd)
    u.save()
    AdminLog.log(request, '重置用户密码', f'用户{u.username}')
    return JsonResponse({'ok': True, 'msg': f'{u.username} 密码已重置'})


def _handle_toggle_user_active(data, request):
    """切换用户启用/禁用状态，禁止禁用超级管理员"""
    uid = data.get('id')
    u = User.objects.get(id=uid)
    if u.is_superuser:
        return JsonResponse({'ok': False, 'msg': '不能禁用超级管理员'}, status=403)
    u.is_active = not u.is_active
    u.save()
    status = '启用' if u.is_active else '禁用'
    AdminLog.log(request, f'{status}用户', f'用户{u.username}')
    return JsonResponse({'ok': True, 'msg': f'{u.username} 已{status}'})


def _handle_save_site_settings(data, request):
    """
    修改网站设置（名称/主色/副色），支持清除背景图和 Logo

    联动：accounts.models.SiteSettings（单例模式，pk=1）
    """
    from accounts.models import SiteSettings
    site_obj, _ = SiteSettings.objects.get_or_create(pk=1)
    fields_changed = []
    for field in ('site_name', 'primary_color', 'secondary_color'):
        val = data.get(field, '').strip()
        if val and val != getattr(site_obj, field):
            setattr(site_obj, field, val)
            fields_changed.append(field)
    # 处理清除背景图/Logo
    if data.get('clear_background'):
        if site_obj.background_image:
            site_obj.background_image.delete(save=False)
            site_obj.background_image = None
            fields_changed.append('清除背景图')
    if data.get('clear_logo'):
        if site_obj.logo:
            site_obj.logo.delete(save=False)
            site_obj.logo = None
            fields_changed.append('清除Logo')
    site_obj.save()
    AdminLog.log(request, '修改网站设置', '、'.join(fields_changed) if fields_changed else '保存设置')
    return JsonResponse({'ok': True, 'msg': '网站设置已保存'})


@login_required(login_url='/accounts/login/')
@user_passes_test(is_admin, login_url='/')
@require_POST
def dashboard_upload(request):
    """
    处理网站设置的文件上传（Logo、背景图）

    上传前会删除旧文件以避免存储泄漏。
    联动：accounts.models.SiteSettings
    """
    from accounts.models import SiteSettings
    site_obj, _ = SiteSettings.objects.get_or_create(pk=1)
    field = request.POST.get('field', '')
    if field == 'logo' and 'file' in request.FILES:
        if site_obj.logo:
            site_obj.logo.delete(save=False)
        site_obj.logo = request.FILES['file']
        site_obj.save()
        AdminLog.log(request, '上传Logo')
        return JsonResponse({'ok': True, 'msg': 'Logo已更新', 'url': site_obj.logo.url})
    elif field == 'background_image' and 'file' in request.FILES:
        if site_obj.background_image:
            site_obj.background_image.delete(save=False)
        site_obj.background_image = request.FILES['file']
        site_obj.save()
        AdminLog.log(request, '上传背景图')
        return JsonResponse({'ok': True, 'msg': '背景图已更新', 'url': site_obj.background_image.url})
    return JsonResponse({'ok': False, 'msg': '无效上传'}, status=400)
