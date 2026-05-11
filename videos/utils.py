"""
videos 模块 - 视频处理工具函数

使用 ffmpeg (python-ffmpeg) 和 Pillow 处理视频文件：
- extract_thumbnail: 从视频截取指定时间点的帧作为封面，缩放到640x360
- get_video_duration: 获取视频时长（秒）
- get_video_info: 获取视频详细信息（宽高、时长、码率）

联动：videos.views.upload_video_view 上传后在后台线程中调用这些函数
依赖：ffmpeg-python、Pillow、系统安装的 ffmpeg
"""
import subprocess
import os
from PIL import Image
import ffmpeg


def extract_thumbnail(video_path, output_path, time_second=1):
    """
    从视频中提取封面
    :param video_path: 视频文件路径
    :param output_path: 输出图片路径
    :param time_second: 截取时间点（秒）
    """
    try:
        # 使用 ffmpeg 截取第一帧作为封面
        stream = ffmpeg.input(video_path, ss=time_second)
        stream = ffmpeg.output(stream, output_path, vframes=1)
        ffmpeg.run(stream, overwrite_output=True, quiet=True)
        
        # 如果成功生成图片，创建缩略图
        if os.path.exists(output_path):
            img = Image.open(output_path)
            img.thumbnail((640, 360))  # 缩放到B站风格的尺寸
            img.save(output_path)
            return True
    except Exception as e:
        print(f"提取封面失败: {e}")
        return False
    
    return False


def get_video_duration(video_path):
    """获取视频时长（秒）"""
    try:
        probe = ffmpeg.probe(video_path)
        duration = float(probe['format']['duration'])
        return int(duration)
    except Exception as e:
        print(f"获取视频时长失败: {e}")
        return 0


def get_video_info(video_path):
    """获取视频信息"""
    try:
        probe = ffmpeg.probe(video_path)
        video_stream = next((stream for stream in probe['streams'] if stream['codec_type'] == 'video'), None)
        
        if video_stream:
            return {
                'width': video_stream.get('width', 0),
                'height': video_stream.get('height', 0),
                'duration': float(probe['format']['duration']),
                'bitrate': int(probe['format'].get('bit_rate', 0))
            }
    except Exception as e:
        print(f"获取视频信息失败: {e}")
    
    return {}





