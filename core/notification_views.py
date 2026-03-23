"""
通知视图模块 - core.notification_views

本模块提供用户通知和弹窗公告的前端 API 接口：

1. notification_list - 通知列表页面（需登录）
2. notification_read - 标记通知已读（支持单条/全部，需登录）
3. notification_count - 获取未读通知数量（导航栏轮询用，需登录）
4. active_announcement - 获取当前活跃弹窗公告（无需登录）

联动模块：
- core.models.Notification: 通知数据的读取和更新
- core.models.Announcement: 弹窗公告数据的读取
- dashboard_views: 管理员通过仪表盘发送通知、管理公告
- 前端模板 notifications.html: 通知列表页面渲染
- 前端 JS: 轮询 notification_count 更新导航栏未读角标，
  页面加载时请求 active_announcement 弹窗展示
"""
from django.shortcuts import render
from django.http import JsonResponse
from django.contrib.auth.decorators import login_required
from django.views.decorators.http import require_POST
from .models import Notification


@login_required
def notification_list(request):
    """
    通知列表页面

    展示当前用户最近50条通知及未读计数。
    联动：模板 notifications.html 渲染通知列表。
    """
    notifications = Notification.objects.filter(user=request.user)
    unread = notifications.filter(is_read=False).count()
    return render(request, 'notifications.html', {
        'notifications': notifications[:50],
        'unread_count': unread,
    })


@login_required
@require_POST
def notification_read(request):
    """
    标记通知已读（POST）

    参数 id='all' 时标记全部已读，否则按 id 标记单条已读。
    仅操作当前用户自己的通知，防止越权。
    """
    nid = request.POST.get('id')
    if nid == 'all':
        Notification.objects.filter(user=request.user, is_read=False).update(is_read=True)
    else:
        Notification.objects.filter(id=nid, user=request.user).update(is_read=True)
    return JsonResponse({'ok': True})


@login_required
def notification_count(request):
    """
    获取未读通知数量（JSON API）

    供前端导航栏轮询调用，返回 {"count": N}。
    """
    count = Notification.objects.filter(user=request.user, is_read=False).count()
    return JsonResponse({'count': count})


def active_announcement(request):
    """
    获取当前活跃弹窗公告（JSON API，无需登录）

    返回最新一条 is_active=True 的公告，若无则返回 {"id": null}。
    联动：core.models.Announcement，前端页面加载时请求并弹窗展示。
    """
    from .models import Announcement
    ann = Announcement.objects.filter(is_active=True).first()
    if ann:
        return JsonResponse({'id': ann.id, 'title': ann.title, 'content': ann.content, 'updated': ann.updated_at.isoformat()})
    return JsonResponse({'id': None})
