#!/usr/bin/env python3
"""
测试脚本：检查环境是否正确配置
"""

import sys
import os

def check_python_version():
    """检查 Python 版本"""
    version = sys.version_info
    print(f"✓ Python 版本: {version.major}.{version.minor}.{version.micro}")
    if version.major < 3 or (version.major == 3 and version.minor < 8):
        print("✗ 错误: 需要 Python 3.8 或更高版本")
        return False
    return True

def check_virtual_env():
    """检查虚拟环境"""
    if hasattr(sys, 'real_prefix') or (hasattr(sys, 'base_prefix') and sys.base_prefix != sys.prefix):
        print("✓ 虚拟环境已激活")
        print(f"  虚拟环境路径: {sys.prefix}")
        return True
    else:
        print("⚠ 警告: 虚拟环境未激活")
        print("  请运行: source venv/bin/activate")
        return False

def check_installed_modules():
    """检查已安装的模块"""
    modules = {
        'django': 'Django',
        'PIL': 'Pillow',
        'mysqlclient': 'mysqlclient',
        'ffmpeg_python': 'ffmpeg-python',
    }
    
    results = {}
    for module, name in modules.items():
        try:
            __import__(module)
            version = ''
            if module == 'django':
                import django
                version = django.get_version()
            print(f"✓ {name} 已安装" + (f" (版本: {version})" if version else ""))
            results[module] = True
        except ImportError:
            print(f"✗ {name} 未安装")
            results[module] = False
    
    return results

def check_file_structure():
    """检查文件结构"""
    required_dirs = ['config', 'accounts', 'files', 'videos', 'templates', 'static']
    print("\n检查项目结构...")
    for dir_name in required_dirs:
        if os.path.exists(dir_name):
            print(f"✓ {dir_name}/")
        else:
            print(f"✗ {dir_name}/ 不存在")
            return False
    return True

def main():
    print("=" * 50)
    print("环境检查脚本")
    print("=" * 50)
    
    print("\n1. 检查 Python 版本...")
    python_ok = check_python_version()
    
    print("\n2. 检查虚拟环境...")
    venv_ok = check_virtual_env()
    
    print("\n3. 检查已安装的模块...")
    modules = check_installed_modules()
    all_modules_ok = all(modules.values())
    
    print("\n4. 检查项目结构...")
    structure_ok = check_file_structure()
    
    print("\n" + "=" * 50)
    print("检查结果总结:")
    print("=" * 50)
    
    results = {
        'Python 版本': python_ok,
        '虚拟环境': venv_ok,
        '依赖模块': all_modules_ok,
        '项目结构': structure_ok,
    }
    
    for name, result in results.items():
        status = "✓" if result else "✗"
        print(f"{status} {name}")
    
    all_ok = all(results.values())
    
    if all_ok:
        print("\n✓ 所有检查通过！环境配置正确。")
        print("\n下一步：")
        print("1. 配置数据库：编辑 config/settings.py")
        print("2. 运行：python manage.py migrate")
        print("3. 运行：python manage.py createsuperuser")
        print("4. 运行：python manage.py runserver")
    else:
        print("\n✗ 环境配置存在问题，请根据上述检查结果进行修复。")
        print("\n建议操作：")
        if not python_ok:
            print("- 升级 Python 到 3.8+")
        if not venv_ok:
            print("- 激活虚拟环境: source venv/bin/activate")
        if not all_modules_ok:
            print("- 安装依赖: pip install -r requirements.txt")
        if not structure_ok:
            print("- 检查项目文件是否完整")
    
    return all_ok

if __name__ == '__main__':
    success = main()
    sys.exit(0 if success else 1)





