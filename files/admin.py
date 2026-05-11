from django.contrib import admin
from django.utils.html import format_html, mark_safe
from django.urls import reverse, path
from django.http import JsonResponse
from .models import FileCategory, FileItem


def approve_files(modeladmin, request, queryset):
    """批量通过审核"""
    queryset.update(status='approved')
    modeladmin.message_user(request, f'已通过 {queryset.count()} 个文件的审核')
approve_files.short_description = '通过审核'


def reject_files(modeladmin, request, queryset):
    """批量驳回"""
    queryset.update(status='rejected')
    modeladmin.message_user(request, f'已驳回 {queryset.count()} 个文件')
reject_files.short_description = '驳回'


@admin.register(FileCategory)
class FileCategoryAdmin(admin.ModelAdmin):
    list_display = ['name', 'icon']


@admin.register(FileItem)
class FileItemAdmin(admin.ModelAdmin):
    list_display = ['title', 'uploader', 'file_type', 'visibility', 'status', 'status_buttons', 'download_count', 'created_at']
    list_filter = ['file_type', 'status', 'visibility', 'created_at']
    search_fields = ['title', 'description']
    readonly_fields = ['download_count', 'view_count', 'created_at', 'updated_at', 'file_preview', 'approve_button', 'reject_button']
    actions = [approve_files, reject_files]
    
    change_form_template = 'admin/fileitem_change_form.html'
    
    fieldsets = (
        ('基本信息', {
            'fields': ('title', 'description', 'uploader', 'visibility')
        }),
        ('文件信息', {
            'fields': ('file', 'file_preview', 'file_type', 'size')
        }),
        ('统计数据', {
            'fields': ('status', 'download_count', 'view_count')
        }),
        ('审核操作', {
            'fields': ('approve_button', 'reject_button'),
            'classes': ('wide',)
        }),
        ('时间信息', {
            'fields': ('created_at', 'updated_at')
        }),
    )
    
    def get_urls(self):
        """添加 AJAX 路由"""
        urls = super().get_urls()
        custom_urls = [
            path('ajax-approve/', self.admin_site.admin_view(self.approve_file_ajax), name='files_fileitem_ajax_approve'),
            path('ajax-reject/', self.admin_site.admin_view(self.reject_file_ajax), name='files_fileitem_ajax_reject'),
            path('ajax-make-public/', self.admin_site.admin_view(self.make_public_ajax), name='files_fileitem_ajax_make_public'),
        ]
        return custom_urls + urls
    
    def approve_file_ajax(self, request):
        """通过审核（AJAX）"""
        if request.method == 'POST':
            file_id = request.POST.get('file_id')
            try:
                file_item = FileItem.objects.get(id=file_id)
                file_item.status = 'approved'
                file_item.save()
                return JsonResponse({'success': True, 'message': '文件已通过审核'})
            except FileItem.DoesNotExist:
                return JsonResponse({'error': '文件不存在'}, status=404)
        return JsonResponse({'error': 'Invalid method'}, status=405)
    
    def reject_file_ajax(self, request):
        """驳回（AJAX）"""
        if request.method == 'POST':
            file_id = request.POST.get('file_id')
            try:
                file_item = FileItem.objects.get(id=file_id)
                file_item.status = 'rejected'
                file_item.save()
                return JsonResponse({'success': True, 'message': '文件已驳回'})
            except FileItem.DoesNotExist:
                return JsonResponse({'error': '文件不存在'}, status=404)
        return JsonResponse({'error': 'Invalid method'}, status=405)
    
    def make_public_ajax(self, request):
        """转为公开（AJAX）"""
        if request.method == 'POST':
            file_id = request.POST.get('file_id')
            try:
                file_item = FileItem.objects.get(id=file_id)
                if file_item.visibility == 'private':
                    file_item.visibility = 'public'
                    file_item.status = 'pending'
                    file_item.save()
                    return JsonResponse({'success': True, 'message': '文件已转为公开，等待审核'})
                else:
                    return JsonResponse({'error': '文件已经是公开的'}, status=400)
            except FileItem.DoesNotExist:
                return JsonResponse({'error': '文件不存在'}, status=404)
        return JsonResponse({'error': 'Invalid method'}, status=405)
    
    @mark_safe
    def file_preview(self, obj):
        """显示文件预览"""
        if obj.file and obj.file_type == 'image':
            return f'<img src="{obj.file.url}" style="max-width: 300px; max-height: 200px;" />'
        elif obj.file:
            return f'<a href="{obj.file.url}" target="_blank">查看文件</a>'
        return '暂无文件'
    file_preview.short_description = '文件预览'
    
    @mark_safe
    def status_buttons(self, obj):
        """显示审核按钮"""
        if obj.status == 'pending':
            return f'''
            <button onclick="approveFileById({obj.id})" style="color: green; background: none; border: none; cursor: pointer; margin-right: 5px;">
                ✓ 通过
            </button>
            <button onclick="rejectFileById({obj.id})" style="color: red; background: none; border: none; cursor: pointer;">
                ✗ 驳回
            </button>
            '''
        elif obj.status == 'approved' and obj.visibility == 'private':
            return f'''
            <span style="color: orange;">{obj.get_status_display()}</span>
            <button onclick="makeFilePublicById({obj.id})" style="color: blue; background: none; border: none; cursor: pointer; margin-left: 5px;">
                转为公开
            </button>
            '''
        return obj.get_status_display()
    status_buttons.short_description = '操作'
    
    @mark_safe
    def approve_button(self, obj):
        """审核按钮"""
        if obj and obj.status == 'pending':
            return f'''
            <button onclick="approveFile({obj.id})" class="approve-btn">
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
            <button onclick="rejectFile({obj.id})" class="reject-btn">
                <i class="bi bi-x-circle"></i> 驳回
            </button>
            '''
        return ''
    reject_button.short_description = ''
    
    def save_model(self, request, obj, form, change):
        """保存时检查 URL 参数"""
        from django.contrib import messages
        
        if 'status' in request.GET:
            obj.status = request.GET['status']
            obj.save()
            if obj.status == 'approved':
                messages.success(request, f'文件 "{obj.title}" 已通过审核')
            else:
                messages.success(request, f'文件 "{obj.title}" 已驳回')
        elif 'make_public' in request.GET:
            # 私密转公开时需要审核
            obj.visibility = 'public'
            obj.status = 'pending'
            obj.save()
            messages.success(request, f'文件 "{obj.title}" 已转为公开，等待审核')
        else:
            super().save_model(request, obj, form, change)


