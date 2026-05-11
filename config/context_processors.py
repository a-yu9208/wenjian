"""
全局模板上下文处理器 - boke 项目

本模块为所有模板注入全局上下文变量，当前提供：
- site_settings: 网站基础配置（站名、主题色、Logo、背景图）

联动模块：
- accounts.models.SiteSettings: 网站设置数据模型，由管理员在仪表盘中维护
- config.settings TEMPLATES.context_processors: 在模板引擎中注册本处理器
- core.dashboard_views: 管理员通过仪表盘修改 SiteSettings 记录
"""
from accounts.models import SiteSettings


def site_settings(request):
    """
    将网站设置注入模板上下文，所有模板可直接使用 {{ site_settings.site_name }} 等变量。
    若数据库查询失败或无记录，返回内置默认值以保证页面正常渲染。
    """
    try:
        settings = SiteSettings.objects.first()  # 取第一条记录（全站仅一条配置）
        if settings:
            return {
                'site_settings': settings
            }
    except:
        pass
    
    # 数据库无记录或异常时的兜底默认设置
    return {
        'site_settings': {
            'site_name': '我的文件管理系统',
            'primary_color': '#1890ff',
            'secondary_color': '#52c41a',
            'background_image': None,
            'logo': None,
        }
    }
