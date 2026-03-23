"""
审核服务模块 - core.review_service

本模块统一管理视频（Video）和文件（FileItem）的审核逻辑，提供：

1. ReviewStatus / Visibility - 审核状态和可见性常量
2. ReviewService - 审核服务类
   - determine_initial_status: 根据用户角色和可见性确定初始审核状态
   - approve_content / reject_content: 单条内容通过/驳回
   - change_visibility: 修改可见性（私密↔公开），自动处理审核状态
   - batch_approve / batch_reject: 批量审核操作
   - get_review_statistics: 获取审核统计数据

联动模块：
- videos.views: 视频上传时调用 determine_initial_status 确定初始状态
- files.views: 文件上传时调用 determine_initial_status 确定初始状态
- accounts.views: 可见性修改时调用 change_visibility
- dashboard_views: 管理员审核操作（dashboard_views 有自己的直接实现，
  本服务主要供前台 views 使用）
"""
from django.db import transaction
from django.utils import timezone
from typing import Optional, Dict, Any


class ReviewStatus:
    """审核状态常量，与 Video/FileItem 模型的 status 字段值对应"""
    PENDING = 'pending'      # 待审核
    APPROVED = 'approved'    # 已通过
    REJECTED = 'rejected'    # 已驳回


class Visibility:
    """可见性常量，与 Video/FileItem 模型的 visibility 字段值对应"""
    PUBLIC = 'public'        # 公开
    PRIVATE = 'private'      # 私密


class ReviewService:
    """
    审核服务类 - 统一处理审核逻辑

    核心审核规则：
    - 私密内容自动通过审核，无需管理员介入
    - 管理员上传的公开内容自动通过
    - 普通用户上传的公开内容需要管理员审核
    - 驳回视频时自动转为私密，驳回文件时不改变可见性
    - 私密转公开时需要重新审核（管理员操作除外）
    """
    
    @staticmethod
    def determine_initial_status(user, visibility: str) -> str:
        """
        确定内容的初始审核状态
        
        规则：
        1. 私密内容 → 自动通过
        2. 管理员上传的公开内容 → 自动通过
        3. 普通用户上传的公开内容 → 待审核
        
        Args:
            user: 上传用户对象
            visibility: 可见性（public/private）
            
        Returns:
            str: 审核状态（pending/approved）
        """
        if visibility == Visibility.PRIVATE:
            return ReviewStatus.APPROVED
        
        if hasattr(user, 'is_admin_role') and user.is_admin_role:
            return ReviewStatus.APPROVED
        
        return ReviewStatus.PENDING
    
    @staticmethod
    def can_approve(user) -> bool:
        """
        检查用户是否有审核权限
        
        Args:
            user: 用户对象
            
        Returns:
            bool: 是否有审核权限
        """
        return hasattr(user, 'is_admin_role') and user.is_admin_role
    
    @staticmethod
    @transaction.atomic
    def approve_content(content, reviewer, comment: Optional[str] = None) -> Dict[str, Any]:
        """
        通过审核
        
        Args:
            content: 内容对象（Video 或 FileItem）
            reviewer: 审核人
            comment: 审核备注（可选）
            
        Returns:
            dict: 操作结果
        """
        if not ReviewService.can_approve(reviewer):
            return {
                'success': False,
                'error': '权限不足：只有管理员可以审核内容'
            }
        
        # 更新状态
        content.status = ReviewStatus.APPROVED
        content.save(update_fields=['status', 'updated_at'])
        
        return {
            'success': True,
            'message': f'{content.__class__.__name__} 已通过审核',
            'status': ReviewStatus.APPROVED
        }
    
    @staticmethod
    @transaction.atomic
    def reject_content(content, reviewer, reason: str, set_private: bool = False) -> Dict[str, Any]:
        """
        驳回审核
        
        Args:
            content: 内容对象（Video 或 FileItem）
            reviewer: 审核人
            reason: 驳回理由（必填）
            set_private: 是否同时设置为私密（视频默认为True）
            
        Returns:
            dict: 操作结果
        """
        if not ReviewService.can_approve(reviewer):
            return {
                'success': False,
                'error': '权限不足：只有管理员可以审核内容'
            }
        
        if not reason or not reason.strip():
            return {
                'success': False,
                'error': '驳回理由不能为空'
            }
        
        # 更新状态
        content.status = ReviewStatus.REJECTED
        
        # 视频驳回时自动设为私密
        if set_private and hasattr(content, 'visibility'):
            content.visibility = Visibility.PRIVATE
            content.save(update_fields=['status', 'visibility', 'updated_at'])
        else:
            content.save(update_fields=['status', 'updated_at'])
        
        return {
            'success': True,
            'message': f'{content.__class__.__name__} 已驳回',
            'status': ReviewStatus.REJECTED,
            'reason': reason
        }
    
    @staticmethod
    @transaction.atomic
    def change_visibility(content, new_visibility: str, user) -> Dict[str, Any]:
        """
        修改可见性
        
        规则：
        - 私密 → 公开：需要重新审核（除非是管理员）
        - 公开 → 私密：直接修改，无需审核
        
        Args:
            content: 内容对象
            new_visibility: 新的可见性
            user: 操作用户
            
        Returns:
            dict: 操作结果
        """
        old_visibility = content.visibility
        
        # 检查权限：只能修改自己的内容
        if content.uploader != user and not ReviewService.can_approve(user):
            return {
                'success': False,
                'error': '权限不足：只能修改自己的内容'
            }
        
        # 如果没有变化，直接返回
        if old_visibility == new_visibility:
            return {
                'success': True,
                'message': '可见性未改变',
                'visibility': new_visibility
            }
        
        # 私密 → 公开：需要重新审核
        if old_visibility == Visibility.PRIVATE and new_visibility == Visibility.PUBLIC:
            content.visibility = Visibility.PUBLIC
            
            # 管理员操作：直接通过
            if ReviewService.can_approve(user):
                content.status = ReviewStatus.APPROVED
                message = '已转为公开'
            else:
                content.status = ReviewStatus.PENDING
                message = '已转为公开，等待管理员审核'
            
            content.save(update_fields=['visibility', 'status', 'updated_at'])
            
            return {
                'success': True,
                'message': message,
                'visibility': new_visibility,
                'status': content.status
            }
        
        # 公开 → 私密：直接修改
        if old_visibility == Visibility.PUBLIC and new_visibility == Visibility.PRIVATE:
            content.visibility = Visibility.PRIVATE
            content.status = ReviewStatus.APPROVED  # 私密内容自动通过
            content.save(update_fields=['visibility', 'status', 'updated_at'])
            
            return {
                'success': True,
                'message': '已设为私密',
                'visibility': new_visibility,
                'status': ReviewStatus.APPROVED
            }
        
        return {
            'success': False,
            'error': '无效的可见性值'
        }
    
    @staticmethod
    def get_review_statistics(user) -> Dict[str, int]:
        """
        获取审核统计信息（管理员专用）
        
        Args:
            user: 用户对象
            
        Returns:
            dict: 统计信息
        """
        if not ReviewService.can_approve(user):
            return {}
        
        from videos.models import Video
        from files.models import FileItem
        
        return {
            'pending_videos': Video.objects.filter(
                status=ReviewStatus.PENDING,
                visibility=Visibility.PUBLIC
            ).count(),
            'pending_files': FileItem.objects.filter(
                status=ReviewStatus.PENDING,
                visibility=Visibility.PUBLIC
            ).count(),
            'total_videos': Video.objects.count(),
            'total_files': FileItem.objects.count(),
            'approved_videos': Video.objects.filter(
                status=ReviewStatus.APPROVED
            ).count(),
            'approved_files': FileItem.objects.filter(
                status=ReviewStatus.APPROVED
            ).count(),
        }
    
    @staticmethod
    def batch_approve(content_list, reviewer) -> Dict[str, Any]:
        """
        批量通过审核
        
        Args:
            content_list: 内容对象列表
            reviewer: 审核人
            
        Returns:
            dict: 操作结果
        """
        if not ReviewService.can_approve(reviewer):
            return {
                'success': False,
                'error': '权限不足'
            }
        
        success_count = 0
        for content in content_list:
            result = ReviewService.approve_content(content, reviewer)
            if result['success']:
                success_count += 1
        
        return {
            'success': True,
            'message': f'已通过 {success_count}/{len(content_list)} 个内容的审核',
            'count': success_count
        }
    
    @staticmethod
    def batch_reject(content_list, reviewer, reason: str) -> Dict[str, Any]:
        """
        批量驳回
        
        Args:
            content_list: 内容对象列表
            reviewer: 审核人
            reason: 驳回理由
            
        Returns:
            dict: 操作结果
        """
        if not ReviewService.can_approve(reviewer):
            return {
                'success': False,
                'error': '权限不足'
            }
        
        if not reason or not reason.strip():
            return {
                'success': False,
                'error': '驳回理由不能为空'
            }
        
        success_count = 0
        for content in content_list:
            # 判断是否需要设置为私密
            set_private = content.__class__.__name__ == 'Video'
            result = ReviewService.reject_content(content, reviewer, reason, set_private)
            if result['success']:
                success_count += 1
        
        return {
            'success': True,
            'message': f'已驳回 {success_count}/{len(content_list)} 个内容',
            'count': success_count
        }
