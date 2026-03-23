"""
accounts 模块 - 视图层

本模块处理用户认证、个人中心、网站设置及资源可见性变更：
- register_view: 用户注册
- login_view: 用户登录（记录登录日志）
- logout_view: 用户登出
- profile_view: 个人中心（展示用户视频和文件）
- get_site_settings / update_site_settings: 网站设置读取与更新
- change_video_visibility / change_file_visibility: 修改视频/文件可见性

联动关系：
- 调用 core.message_service 进行统一消息提示和 JSON 响应
- 调用 core.review_service.ReviewService 处理可见性变更的审核流程
- 调用 core.models.LoginLog 记录登录成功/失败日志
- 读取 videos.models.Video 和 files.models.FileItem 展示用户资源
"""
from django.shortcuts import render, redirect
from django.contrib.auth import login, authenticate
from django.contrib.auth.decorators import login_required
from django.http import JsonResponse
from django.views.decorators.http import require_http_methods
from django.conf import settings
from .models import User, SiteSettings
from videos.models import Video
from files.models import FileItem
from core.review_service import ReviewService
from core.message_service import MessageService, msg_success, msg_error
import json


def register_view(request):
    """
    用户注册视图

    POST: 校验用户名/密码/邮箱唯一性后创建用户，成功后跳转登录页。
    GET: 渲染注册页面。
    """
    if request.method == 'POST':
        # 基于 session 的简单注册限流：60秒内只能注册一次
        import time
        last_register = request.session.get('last_register_time', 0)
        if time.time() - last_register < 60:
            msg_error(request, 'user.register.too_frequent')
            return render(request, 'accounts/register.html')
        
        username = request.POST.get('username')
        password = request.POST.get('password')
        password2 = request.POST.get('password2')
        email = request.POST.get('email')
        phone = request.POST.get('phone', '')
        
        # 密码一致性校验
        if password != password2:
            msg_error(request, 'user.register.password_mismatch')
            return render(request, 'accounts/register.html')
        
        # 密码长度校验
        if len(password) < 6:
            msg_error(request, 'user.register.password_too_short')
            return render(request, 'accounts/register.html')
        
        # 用户名唯一性校验
        if User.objects.filter(username=username).exists():
            msg_error(request, 'user.register.username_exists')
            return render(request, 'accounts/register.html')
        
        # 邮箱唯一性校验
        if email and User.objects.filter(email=email).exists():
            msg_error(request, 'user.register.email_exists')
            return render(request, 'accounts/register.html')
        
        user = User.objects.create_user(
            username=username,
            password=password,
            email=email,
            phone=phone,
            role='user'
        )
        request.session['last_register_time'] = time.time()
        msg_success(request, 'user.register.success')
        return redirect('accounts:login')
    
    return render(request, 'accounts/register.html')


def login_view(request):
    """
    用户登录视图

    已登录用户直接跳转首页。
    POST: 验证凭据，成功则登录并记录日志（联动 core.models.LoginLog），失败也记录日志。
    GET: 渲染登录页面。
    """
    if request.user.is_authenticated:
        return redirect('videos:index')
    
    if request.method == 'POST':
        username = request.POST.get('username')
        password = request.POST.get('password')
        
        user = authenticate(username=username, password=password)
        if user:
            login(request, user)
            # 联动 core.models.LoginLog：记录登录成功
            from core.models import LoginLog
            LoginLog.record(request, user, username, success=True)
            return redirect('videos:index')
        else:
            # 联动 core.models.LoginLog：记录登录失败
            from core.models import LoginLog
            LoginLog.record(request, None, username, success=False)
            msg_error(request, 'user.login.failed')
    
    return render(request, 'accounts/login.html')


def logout_view(request):
    """用户登出，清除会话后跳转登录页"""
    from django.contrib.auth import logout
    logout(request)
    msg_success(request, 'user.logout.success')
    return redirect('accounts:login')


@login_required
def profile_view(request):
    """
    个人中心视图

    POST: 更新用户邮箱、电话、头像等个人信息。
    GET: 查询当前用户上传的视频（联动 videos.models.Video）和文件（联动 files.models.FileItem），渲染个人中心页面。
    """
    if request.method == 'POST':
        user = request.user
        user.email = request.POST.get('email', user.email)
        user.phone = request.POST.get('phone', user.phone)
        if 'avatar' in request.FILES:
            user.avatar = request.FILES['avatar']
        user.save()
        msg_success(request, 'user.profile.updated')
        return redirect('accounts:profile')
    
    # 获取用户的视频和文件
    user_videos = Video.objects.filter(uploader=request.user).order_by('-created_at')
    user_files = FileItem.objects.filter(uploader=request.user).order_by('-created_at')
    
    context = {
        'user_videos': user_videos,
        'user_files': user_files,
    }
    return render(request, 'accounts/profile.html', context)


@login_required
def get_site_settings(request):
    """获取网站设置（JSON 接口，供前端使用），联动 SiteSettings 单例模型"""
    settings_obj, _ = SiteSettings.objects.get_or_create(pk=1)
    return JsonResponse({
        'site_name': settings_obj.site_name,
        'background_image': settings_obj.background_image.url if settings_obj.background_image else '',
        'primary_color': settings_obj.primary_color,
        'secondary_color': settings_obj.secondary_color,
        'logo': settings_obj.logo.url if settings_obj.logo else '',
    })


@login_required
def update_site_settings(request):
    """
    更新网站设置（管理员专用）

    仅 is_admin_role 用户可操作，支持更新网站名称、主题色、背景图片、Logo。
    联动 core.message_service.MessageService 返回统一 JSON 响应。
    """
    if not request.user.is_admin_role:
        return MessageService.permission_denied_response()
    
    if request.method == 'POST':
        settings_obj, _ = SiteSettings.objects.get_or_create(pk=1)
        
        settings_obj.site_name = request.POST.get('site_name', settings_obj.site_name)
        settings_obj.primary_color = request.POST.get('primary_color', settings_obj.primary_color)
        settings_obj.secondary_color = request.POST.get('secondary_color', settings_obj.secondary_color)
        
        if 'background_image' in request.FILES:
            settings_obj.background_image = request.FILES['background_image']
        if 'logo' in request.FILES:
            settings_obj.logo = request.FILES['logo']
        
        settings_obj.save()
        return MessageService.success_response('common.operation.success')
    
    return MessageService.invalid_method_response()


@login_required
def change_video_visibility(request, video_id):
    """
    修改视频可见性

    联动 core.review_service.ReviewService.change_visibility 处理审核流程：
    - 公开转私密：直接生效
    - 私密转公开：普通用户需审核，管理员直接生效
    """
    try:
        video = Video.objects.get(id=video_id)
        new_visibility = request.POST.get('visibility')
        
        if not new_visibility or new_visibility not in ['public', 'private']:
            return MessageService.error_response('visibility.change.invalid')
        
        result = ReviewService.change_visibility(video, new_visibility, request.user)
        
        if result['success']:
            return MessageService.success_response(
                'visibility.change.to_public_pending' if result.get('status') == 'pending' else 'visibility.change.to_public' if new_visibility == 'public' else 'visibility.change.to_private',
                data={'visibility': result['visibility'], 'status': result.get('status')}
            )
        else:
            status = 403 if '权限' in result['error'] else 400
            return MessageService.error_response('user.permission.denied' if status == 403 else 'common.operation.failed', status=status)
            
    except Video.DoesNotExist:
        return MessageService.not_found_response('video')


@login_required
def change_file_visibility(request, file_id):
    """
    修改文件可见性

    联动 core.review_service.ReviewService.change_visibility 处理审核流程：
    - 公开转私密：直接生效
    - 私密转公开：普通用户需审核，管理员直接生效
    """
    try:
        file_item = FileItem.objects.get(id=file_id)
        new_visibility = request.POST.get('visibility')
        
        if not new_visibility or new_visibility not in ['public', 'private']:
            return MessageService.error_response('visibility.change.invalid')
        
        result = ReviewService.change_visibility(file_item, new_visibility, request.user)
        
        if result['success']:
            return MessageService.success_response(
                'visibility.change.to_public_pending' if result.get('status') == 'pending' else 'visibility.change.to_public' if new_visibility == 'public' else 'visibility.change.to_private',
                data={'visibility': result['visibility'], 'status': result.get('status')}
            )
        else:
            status = 403 if '权限' in result['error'] else 400
            return MessageService.error_response('user.permission.denied' if status == 403 else 'common.operation.failed', status=status)
            
    except FileItem.DoesNotExist:
        return MessageService.not_found_response('file')
