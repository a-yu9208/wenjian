"""
files 模块 - 视图层

本模块处理文件的列表展示、详情查看、下载、上传、删除及审核：
- file_list_view: 文件列表（分类筛选 + 搜索 + 分页）
- file_detail_view: 文件详情（支持 AJAX JSON 和页面渲染）
- file_download_view: 文件下载（含私密文件权限校验）
- upload_file_view: 文件上传（自动识别类型，联动审核服务）
- delete_file_view / batch_delete_files_view: 单个/批量删除
- approve_file_view / reject_file_view: 管理员审核

联动关系：
- 调用 core.review_service.ReviewService 确定上传初始状态、审核通过/驳回
- 调用 core.message_service 返回统一消息和 JSON 响应
- 操作 files.models.FileItem 和 FileCategory
"""
from django.shortcuts import render, get_object_or_404, redirect
from django.contrib.auth.decorators import login_required
from django.http import JsonResponse, FileResponse, Http404, HttpResponse
from django.core.paginator import Paginator
from django.db.models import Q, F
from django.db import transaction
from django.conf import settings
from .models import FileItem, FileCategory
from core.review_service import ReviewService, ReviewStatus, Visibility
from core.message_service import MessageService, json_success, json_error
import os
import mimetypes
from urllib.parse import quote


def file_list_view(request):
    """
    文件列表页面

    只展示公开且已通过审核的文件，支持按 file_type 分类筛选和关键词搜索。
    每页 20 条，使用 Django Paginator 分页。
    """
    file_type = request.GET.get('type', 'all')
    search_query = request.GET.get('search', '')
    page_number = request.GET.get('page', 1)
    
    # 基础查询集 - 只显示公开且已通过审核的文件
    files = FileItem.objects.filter(status='approved', visibility='public')
    
    # 分类筛选
    if file_type != 'all':
        files = files.filter(file_type=file_type)
    
    # 搜索功能（标题和描述模糊匹配）
    if search_query:
        files = files.filter(
            Q(title__icontains=search_query) |
            Q(description__icontains=search_query)
        )
    
    # 分页
    paginator = Paginator(files, 20)
    page_obj = paginator.get_page(page_number)
    
    context = {
        'files': page_obj,
        'file_type': file_type,
        'search_query': search_query,
    }
    return render(request, 'files/list.html', context)


def file_detail_view(request, file_id):
    """
    文件详情视图

    基于 session 防刷查看次数（每个 session 只计一次）。
    文档类文件尝试读取前 5000 字符用于预览。
    支持 AJAX 请求返回 JSON，也支持渲染完整页面。
    """
    file_item = get_object_or_404(FileItem, id=file_id)
    
    # 基于 session 防刷查看次数
    viewed_key = f'viewed_file_{file_id}'
    if not request.session.get(viewed_key):
        FileItem.objects.filter(id=file_id).update(view_count=F('view_count') + 1)
        request.session[viewed_key] = True
    
    # 获取文件内容（仅文本类文件可预览）
    file_content = ''
    if file_item.file_type == 'document':
        try:
            with open(file_item.file.path, 'r', encoding='utf-8') as f:
                file_content = f.read()[:5000]
        except:
            file_content = '无法预览此文件'
    
    # AJAX 请求返回 JSON
    if request.headers.get('X-Requested-With') == 'XMLHttpRequest' or request.GET.get('format') == 'json':
        return JsonResponse({
            'title': file_item.title,
            'uploader': file_item.uploader.username,
            'created_at': file_item.created_at.strftime('%Y-%m-%d %H:%M'),
            'download_count': file_item.download_count,
            'file_size_mb': file_item.get_file_size_mb(),
            'description': file_item.description,
            'file_content': file_content,
            'file_url': file_item.file.url,
            'file_type': file_item.file_type,
            'is_uploader': request.user == file_item.uploader if request.user.is_authenticated else False,
            'is_admin': request.user.is_admin_role if request.user.is_authenticated else False,
        })
    
    # 渲染完整页面
    context = {
        'file_item': file_item,
        'file_content': file_content,
        'is_uploader': request.user == file_item.uploader if request.user.is_authenticated else False,
        'is_admin': request.user.is_admin_role if request.user.is_authenticated else False,
    }
    return render(request, 'files/detail.html', context)


def file_download_view(request, file_id):
    """
    文件下载视图

    私密文件需登录且为上传者或管理员才可下载。
    每次下载递增 download_count 计数。
    """
    file_item = get_object_or_404(FileItem, id=file_id)
    
    # 私密文件权限校验
    if file_item.visibility == 'private':
        if not request.user.is_authenticated:
            return JsonResponse({'error': '请先登录'}, status=403)
        if file_item.uploader != request.user and not request.user.is_admin_role:
            return JsonResponse({'error': '无权下载此文件'}, status=403)
    
    # 增加下载次数
    FileItem.objects.filter(id=file_id).update(download_count=F('download_count') + 1)
    
    file_path = file_item.file.path
    if not os.path.exists(file_path):
        raise Http404('文件不存在')
    
    # 通过 Nginx X-Accel-Redirect 发送文件，避免占用 Worker
    response = HttpResponse()
    response['X-Accel-Redirect'] = f'/protected-media/{quote(file_item.file.name)}'
    response['Content-Type'] = 'application/octet-stream'
    filename = f'{file_item.title}{os.path.splitext(file_item.file.name)[1]}'
    response['Content-Disposition'] = f"attachment; filename*=UTF-8''{quote(filename)}"
    return response


@login_required
def upload_file_view(request):
    """
    文件上传视图

    POST: 根据文件扩展名自动判断 file_type，联动 ReviewService.determine_initial_status
    确定初始审核状态（管理员直接通过，普通用户公开文件需审核）。
    GET: 渲染上传页面。
    """
    if request.method == 'POST':
        file = request.FILES.get('file')
        title = request.POST.get('title', file.name if file else '')
        description = request.POST.get('description', '')
        file_type = request.POST.get('file_type', 'other')
        
        if not file:
            return json_error('file.upload.no_file')
        
        # 根据扩展名自动判断文件类型
        filename = file.name.lower()
        if filename.endswith(('.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp')):
            file_type = 'image'
        elif filename.endswith(('.txt', '.md', '.doc', '.docx', '.pdf')):
            file_type = 'document'
        elif filename.endswith(('.zip', '.rar', '.7z', '.tar', '.gz')):
            file_type = 'archive'
        elif filename.endswith(('.mp3', '.wav', '.flac', '.aac', '.ogg')):
            file_type = 'audio'
        
        # 获取可见性设置
        visibility = request.POST.get('visibility', Visibility.PUBLIC)
        
        # 联动审核服务确定初始状态
        initial_status = ReviewService.determine_initial_status(request.user, visibility)
        
        file_item = FileItem.objects.create(
            title=title or file.name,
            description=description,
            file=file,
            file_type=file_type,
            size=file.size,
            uploader=request.user,
            status=initial_status,
            visibility=visibility
        )
        
        # 根据可见性和审核状态返回不同提示消息
        if visibility == Visibility.PRIVATE:
            message_key = 'file.upload.success_private'
        elif initial_status == ReviewStatus.APPROVED:
            message_key = 'file.upload.success'
        else:
            message_key = 'file.upload.pending'
        
        return json_success(message_key, data={'file_id': file_item.id})
    
    return render(request, 'files/upload.html')


@login_required
def delete_file_view(request, file_id):
    """
    删除文件

    权限：管理员可删除所有文件，普通用户只能删除自己上传的。
    同时删除磁盘上的物理文件。
    """
    file_item = get_object_or_404(FileItem, id=file_id)
    
    # 权限检查：管理员可以删除所有，普通用户只能删除自己的
    if not request.user.is_admin_role and file_item.uploader != request.user:
        return MessageService.permission_denied_response()
    
    # 删除物理文件
    if file_item.file and os.path.exists(file_item.file.path):
        os.remove(file_item.file.path)
    
    file_item.delete()
    return json_success('file.delete.success')


@login_required
def batch_delete_files_view(request):
    """
    批量删除文件

    POST 接收 file_ids 列表，逐个校验权限后删除（含物理文件），返回成功删除数量。
    """
    if request.method == 'POST':
        file_ids = request.POST.getlist('file_ids')
        deleted_count = 0
        
        with transaction.atomic():
            for file_id in file_ids:
                try:
                    file_item = FileItem.objects.get(id=file_id)
                    if request.user.is_admin_role or file_item.uploader == request.user:
                        if file_item.file and os.path.exists(file_item.file.path):
                            os.remove(file_item.file.path)
                        file_item.delete()
                        deleted_count += 1
                except FileItem.DoesNotExist:
                    continue
        
        return json_success('file.batch_delete.success', count=deleted_count)
    
    return MessageService.invalid_method_response()


@login_required
def approve_file_view(request, file_id):
    """
    审核通过文件（管理员）

    联动 core.review_service.ReviewService.approve_content 将文件状态设为 approved。
    """
    file_item = get_object_or_404(FileItem, id=file_id)
    comment = request.POST.get('comment', '')
    
    result = ReviewService.approve_content(file_item, request.user, comment)
    
    if result['success']:
        return json_success('review.approve.success', type='文件')
    else:
        return MessageService.permission_denied_response()


@login_required
def reject_file_view(request, file_id):
    """
    审核驳回文件（管理员）

    必须提供驳回理由。联动 core.review_service.ReviewService.reject_content 将文件状态设为 rejected。
    """
    file_item = get_object_or_404(FileItem, id=file_id)
    reason = request.POST.get('reason') or request.POST.get('comment', '')
    
    if not reason:
        return json_error('review.reject.no_reason')
    
    result = ReviewService.reject_content(file_item, request.user, reason, set_private=False)
    
    if result['success']:
        return json_success('review.reject.success', type='文件')
    else:
        return MessageService.permission_denied_response()
