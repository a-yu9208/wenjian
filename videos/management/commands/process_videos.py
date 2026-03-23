"""
管理命令：处理待处理的视频（提取封面、获取时长）

用法：
  python manage.py process_videos          # 处理所有待处理视频
  crontab: */2 * * * * cd /boke && python manage.py process_videos

联动：
- videos.models.Video.needs_processing 标记
- videos.utils.extract_thumbnail / get_video_duration
"""
import os
from django.core.management.base import BaseCommand
from django.conf import settings
from videos.models import Video
from videos.utils import extract_thumbnail, get_video_duration


class Command(BaseCommand):
    help = '处理待处理的视频（封面提取、时长获取）'

    def handle(self, *args, **options):
        videos = Video.objects.filter(needs_processing=True)
        if not videos.exists():
            return

        for video in videos:
            video_path = video.video_file.path
            if not os.path.exists(video_path):
                video.needs_processing = False
                video.save(update_fields=['needs_processing'])
                continue

            # 获取时长
            try:
                video.duration = get_video_duration(video_path)
            except Exception as e:
                self.stderr.write(f"获取时长失败 [{video.id}]: {e}")

            # 生成封面（如果没有）
            if not video.thumbnail:
                thumb_path = os.path.join(
                    settings.MEDIA_ROOT,
                    'uploads/videos/thumbnails',
                    f'thumb_{video.id}.jpg'
                )
                os.makedirs(os.path.dirname(thumb_path), exist_ok=True)
                try:
                    if extract_thumbnail(video_path, thumb_path):
                        video.thumbnail = f'uploads/videos/thumbnails/thumb_{video.id}.jpg'
                except Exception as e:
                    self.stderr.write(f"生成封面失败 [{video.id}]: {e}")

            video.needs_processing = False
            video.save(update_fields=['duration', 'thumbnail', 'needs_processing'])
            self.stdout.write(f"已处理: {video.title} (ID:{video.id})")
