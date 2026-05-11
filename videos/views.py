"""
videos 模块 - 视图层

提供视频相关的所有页面和API接口：
- index: 首页视频列表（支持搜索、排序、分页）
- video_detail_view: 视频详情页（含session防刷观看次数）
- video_stream_view: 视频流播放（支持HTTP Range断点续传）
- upload_video_view: 视频上传（后台线程自动提取封面和时长）
- delete_video_view / batch_delete_videos_view: 单个/批量删除
- approve_video_view / reject_video_view: 管理员审核通过/驳回

联动模块：
- core.review_service: 审核逻辑（确定初始状态、通过、驳回）
- core.message_service: 统一消息响应（json_success/json_error）
- videos.utils: ffmpeg视频处理（封面提取、时长获取）
- videos.models.Video: 视频数据模型
"""
from django.shortcuts import render, get_object_or_404, redirect
from django.contrib.auth.decorators import login_required
from django.http import JsonResponse, FileResponse, StreamingHttpResponse
from django.core.paginator import Paginator
from django.db.models import Q, F
from django.db import transaction
from django.conf import settings
from .models import Video
from .utils import extract_thumbnail, get_video_duration, get_video_info
from core.review_service import ReviewService, ReviewStatus, Visibility
from core.message_service import MessageService, json_success, json_error
import os
import mimetypes
from urllib.parse import quote


def index(request):
    """首页 - 视频列表"""
    search_query = request.GET.get('search', '')
    sort = request.GET.get('sort', 'hot')
    page_number = request.GET.get('page', 1)
    
    videos = Video.objects.filter(status='approved', visibility='public')
    
    if search_query:
        videos = videos.filter(
            Q(title__icontains=search_query) |
            Q(description__icontains=search_query) |
            Q(uploader__username__icontains=search_query)
        )
    
    if sort == 'new':
        videos = videos.order_by('-created_at')
    else:
        videos = videos.order_by('-view_count', '-created_at')
    
    paginator = Paginator(videos, 20)
    page_obj = paginator.get_page(page_number)
    
    context = {
        'videos': page_obj,
        'search_query': search_query,
        'current_sort': sort,
    }
    return render(request, 'videos/index.html', context)


def video_detail_view(request, video_id):
    """视频详情页"""
    video = get_object_or_404(Video, id=video_id)
    
    # 基于 session 防刷观看次数
    viewed_key = f'viewed_video_{video_id}'
    if not request.session.get(viewed_key):
        Video.objects.filter(id=video_id).update(view_count=F('view_count') + 1)
        request.session[viewed_key] = True
    
    context = {
        'video': video,
        'is_uploader': request.user == video.uploader if request.user.is_authenticated else False,
        'is_admin': request.user.is_admin_role if request.user.is_authenticated else False,
    }
    return render(request, 'videos/detail.html', context)


def video_stream_view(request, video_id):
    """视频流播放 - 通过 Nginx X-Accel-Redirect 直接发送文件"""
    video = get_object_or_404(Video, id=video_id)
    video_path = video.video_file.path
    
    if not os.path.exists(video_path):
        return JsonResponse({'error': '视频文件不存在'}, status=404)
    
    content_type, _ = mimetypes.guess_type(video_path)
    if content_type is None:
        content_type = 'video/mp4'
    
    # Django 只做权限校验，文件由 Nginx 直接发送（sendfile 零拷贝 + 原生 Range 支持）
    response = StreamingHttpResponse(content_type=content_type)
    response['X-Accel-Redirect'] = f'/protected-media/{quote(video.video_file.name)}'
    response['X-Accel-Buffering'] = 'no'
    response['Accept-Ranges'] = 'bytes'
    response['Cache-Control'] = 'public, max-age=3600'
    response['Content-Disposition'] = 'inline'
    
    return response


@login_required
def upload_video_view(request):
    """上传视频"""
    if request.method == 'POST':
        video_file = request.FILES.get('video')
        thumbnail = request.FILES.get('thumbnail')
        title = request.POST.get('title', video_file.name if video_file else '')
        description = request.POST.get('description', '')
        
        if not video_file:
            return json_error('video.upload.no_file')
        
        # 检查文件大小（1GB限制）
        if video_file.size > 1073741824:  # 1GB
            return json_error('video.upload.size_limit', status=413)
        
        # 获取可见性设置
        visibility = request.POST.get('visibility', Visibility.PUBLIC)
        
        # 使用审核服务确定初始状态
        initial_status = ReviewService.determine_initial_status(request.user, visibility)
        
        # 创建视频对象，标记待处理
        video = Video.objects.create(
            title=title or video_file.name,
            description=description,
            video_file=video_file,
            thumbnail=thumbnail,
            file_size=video_file.size,
            uploader=request.user,
            status=initial_status,
            visibility=visibility,
            duration=0,
            needs_processing=not thumbnail,  # 有手动封面则不需要处理
        )
        
        # 后台线程提取封面和时长
        if video.needs_processing:
            import threading
            def _process(vid):
                try:
                    v = Video.objects.get(id=vid)
                    path = v.video_file.path
                    if not os.path.exists(path):
                        return
                    v.duration = get_video_duration(path)
                    if not v.thumbnail:
                        thumb_dir = os.path.join(settings.MEDIA_ROOT, 'uploads/videos/thumbnails')
                        os.makedirs(thumb_dir, exist_ok=True)
                        thumb_path = os.path.join(thumb_dir, f'thumb_{vid}.jpg')
                        if extract_thumbnail(path, thumb_path):
                            v.thumbnail = f'uploads/videos/thumbnails/thumb_{vid}.jpg'
                    v.needs_processing = False
                    v.save(update_fields=['duration', 'thumbnail', 'needs_processing'])
                except Exception:
                    pass
            threading.Thread(target=_process, args=(video.id,), daemon=True).start()

        # 根据状态返回不同的消息
        if visibility == Visibility.PRIVATE:
            message_key = 'video.upload.success_private'
        elif initial_status == ReviewStatus.APPROVED:
            message_key = 'video.upload.success'
        else:
            message_key = 'video.upload.pending'
        
        return json_success(message_key, data={'video_id': video.id})
    
    return render(request, 'videos/upload.html')


@login_required
def delete_video_view(request, video_id):
    """删除视频"""
    video = get_object_or_404(Video, id=video_id)
    
    # 权限检查：管理员可以删除所有，普通用户只能删除自己的
    if not request.user.is_admin_role and video.uploader != request.user:
        return MessageService.permission_denied_response()
    
    # 删除物理文件
    if video.video_file and os.path.exists(video.video_file.path):
        os.remove(video.video_file.path)
    if video.thumbnail and os.path.exists(video.thumbnail.path):
        os.remove(video.thumbnail.path)
    
    video.delete()
    return json_success('video.delete.success')


@login_required
def batch_delete_videos_view(request):
    """批量删除视频"""
    if request.method == 'POST':
        video_ids = request.POST.getlist('ids[]') or request.POST.getlist('ids') or request.POST.getlist('video_ids')
        deleted_count = 0
        
        with transaction.atomic():
            for video_id in video_ids:
                try:
                    video = Video.objects.get(id=video_id)
                    if request.user.is_admin_role or video.uploader == request.user:
                        if video.video_file and os.path.exists(video.video_file.path):
                            os.remove(video.video_file.path)
                        if video.thumbnail and os.path.exists(video.thumbnail.path):
                            os.remove(video.thumbnail.path)
                        video.delete()
                        deleted_count += 1
                except Video.DoesNotExist:
                    continue
        
        return json_success('video.batch_delete.success', data={'count': deleted_count})
    
    return MessageService.invalid_method_response()


@login_required
def approve_video_view(request, video_id):
    """批准视频（管理员）"""
    video = get_object_or_404(Video, id=video_id)
    comment = request.POST.get('comment', '')
    
    result = ReviewService.approve_content(video, request.user, comment)
    
    if result['success']:
        return json_success('review.approve.success', type='视频')
    else:
        return MessageService.permission_denied_response()


@login_required
def reject_video_view(request, video_id):
    """驳回视频（管理员）"""
    video = get_object_or_404(Video, id=video_id)
    reason = request.POST.get('reason') or request.POST.get('comment', '')
    
    if not reason:
        return json_error('review.reject.no_reason')
    
    result = ReviewService.reject_content(video, request.user, reason, set_private=True)
    
    if result['success']:
        return json_success('review.reject.success', type='视频')
    else:
        return MessageService.permission_denied_response()


