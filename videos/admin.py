from django.contrib import admin
from django.utils.html import format_html, mark_safe
from django.urls import reverse, path
from django.http import JsonResponse
from .models import Video


def approve_videos(modeladmin, request, queryset):
    """批量通过审核"""
    queryset.update(status='approved')
    modeladmin.message_user(request, f'已通过 {queryset.count()} 个视频的审核')
approve_videos.short_description = '通过审核'


def reject_videos(modeladmin, request, queryset):
    """批量驳回"""
    queryset.update(status='rejected', visibility='private')
    modeladmin.message_user(request, f'已驳回 {queryset.count()} 个视频')
reject_videos.short_description = '驳回'


@admin.register(Video)
class VideoAdmin(admin.ModelAdmin):
    list_display = ['title', 'uploader', 'visibility', 'status', 'status_buttons', 'view_count', 'created_at']
    list_filter = ['status', 'visibility', 'created_at']
    search_fields = ['title', 'description']
    readonly_fields = ['view_count', 'created_at', 'updated_at', 'video_preview', 'approve_button', 'reject_button']
    actions = [approve_videos, reject_videos]
    
    change_form_template = 'admin/video_change_form.html'
    
    fieldsets = (
        ('基本信息', {
            'fields': ('title', 'description', 'uploader', 'visibility')
        }),
        ('文件信息', {
            'fields': ('video_file', 'video_preview', 'thumbnail', 'duration', 'file_size')
        }),
        ('统计数据', {
            'fields': ('status', 'view_count')
        }),
        ('审核操作', {
            'fields': ('approve_button', 'reject_button'),
            'classes': ('wide',)
        }),
        ('时间信息', {
            'fields': ('created_at', 'updated_at')
        }),
    )
    
    @mark_safe
    def approve_button(self, obj):
        """审核按钮"""
        if obj and obj.status == 'pending':
            return f'''
            <button onclick="approveVideo({obj.id})" class="approve-btn">
                <i class="bi bi-check-circle"></i> 通过审核
            </button>
            '''
        return ''
    approve_button.short_description = ''
    
    @mark_safe
    def reject_button(self, obj):
        """驳回按钮"""
        if obj and obj.status == 'pending':
            return f'''
            <button onclick="rejectVideo({obj.id})" class="reject-btn">
                <i class="bi bi-x-circle"></i> 驳回
            </button>
            '''
        return ''
    reject_button.short_description = ''
    
    def get_urls(self):
        """添加 AJAX 路由"""
        urls = super().get_urls()
        custom_urls = [
            path('ajax-approve/', self.admin_site.admin_view(self.approve_video_ajax), name='videos_video_ajax_approve'),
            path('ajax-reject/', self.admin_site.admin_view(self.reject_video_ajax), name='videos_video_ajax_reject'),
            path('ajax-make-public/', self.admin_site.admin_view(self.make_public_ajax), name='videos_video_ajax_make_public'),
        ]
        return custom_urls + urls
    
    def approve_video_ajax(self, request):
        """通过审核（AJAX）"""
        if request.method == 'POST':
            video_id = request.POST.get('video_id')
            try:
                video = Video.objects.get(id=video_id)
                video.status = 'approved'
                video.save()
                return JsonResponse({'success': True, 'message': '视频已通过审核'})
            except Video.DoesNotExist:
                return JsonResponse({'error': '视频不存在'}, status=404)
        return JsonResponse({'error': 'Invalid method'}, status=405)
    
    def reject_video_ajax(self, request):
        """驳回（AJAX）"""
        if request.method == 'POST':
            video_id = request.POST.get('video_id')
            try:
                video = Video.objects.get(id=video_id)
                video.status = 'rejected'
                video.visibility = 'private'
                video.save()
                return JsonResponse({'success': True, 'message': '视频已驳回'})
            except Video.DoesNotExist:
                return JsonResponse({'error': '视频不存在'}, status=404)
        return JsonResponse({'error': 'Invalid method'}, status=405)
    
    def make_public_ajax(self, request):
        """转为公开（AJAX）"""
        if request.method == 'POST':
            video_id = request.POST.get('video_id')
            try:
                video = Video.objects.get(id=video_id)
                if video.visibility == 'private':
                    video.visibility = 'public'
                    video.status = 'pending'
                    video.save()
                    return JsonResponse({'success': True, 'message': '视频已转为公开，等待审核'})
                else:
                    return JsonResponse({'error': '视频已经是公开的'}, status=400)
            except Video.DoesNotExist:
                return JsonResponse({'error': '视频不存在'}, status=404)
        return JsonResponse({'error': 'Invalid method'}, status=405)
    
    @mark_safe
    def video_preview(self, obj):
        """显示视频预览"""
        if obj.video_file:
            return f'<video width="320" height="240" controls><source src="{obj.video_file.url}" type="video/mp4">您的浏览器不支持视频播放。</video>'
        return '暂无视频'
    video_preview.short_description = '视频预览'
    
    @mark_safe
    def status_buttons(self, obj):
        """显示审核按钮"""
        if obj.status == 'pending':
            return f'''
            <button onclick="approveVideoById({obj.id})" style="color: green; background: none; border: none; cursor: pointer; margin-right: 5px;">
                ✓ 通过
            </button>
            <button onclick="rejectVideoById({obj.id})" style="color: red; background: none; border: none; cursor: pointer;">
                ✗ 驳回
            </button>
            '''
        elif obj.status == 'approved' and obj.visibility == 'private':
            # 私密视频转为公开时需要审核
            return f'''
            <span style="color: orange;">{obj.get_status_display()}</span>
            <button onclick="makePublicById({obj.id})" style="color: blue; background: none; border: none; cursor: pointer; margin-left: 5px;">
                转为公开
            </button>
            '''
        return obj.get_status_display()
    status_buttons.short_description = '操作'
    
    def save_model(self, request, obj, form, change):
        """保存时检查 URL 参数"""
        from django.contrib import messages
        
        if 'status' in request.GET:
            obj.status = request.GET['status']
            obj.save()
            if obj.status == 'approved':
                messages.success(request, f'视频 "{obj.title}" 已通过审核')
            else:
                messages.success(request, f'视频 "{obj.title}" 已驳回')
        elif 'make_public' in request.GET:
            # 私密转公开时需要审核
            obj.visibility = 'public'
            obj.status = 'pending'
            obj.save()
            messages.success(request, f'视频 "{obj.title}" 已转为公开，等待审核')
        else:
            super().save_model(request, obj, form, change)


