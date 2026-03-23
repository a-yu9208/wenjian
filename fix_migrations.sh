#!/bin/bash
# 修复迁移问题的脚本

echo "修复 Django 迁移问题..."

# 创建必要目录
mkdir -p accounts/migrations files/migrations videos/migrations

# 创建 __init__.py 文件
touch accounts/migrations/__init__.py
touch files/migrations/__init__.py
touch videos/migrations/__init__.py

# 尝试生成初始迁移
echo "正在为 accounts 应用创建迁移..."
python manage.py makemigrations accounts --name initial_migration || echo "accounts 迁移创建失败"

echo "正在为 files 应用创建迁移..."
python manage.py makemigrations files --name initial_migration || echo "files 迁移创建失败"

echo "正在为 videos 应用创建迁移..."
python manage.py makemigrations videos --name initial_migration || echo "videos 迁移创建失败"

# 列出所有迁移
echo ""
echo "当前迁移文件："
ls -la */migrations/*.py 2>/dev/null || echo "没有找到迁移文件"

echo ""
echo "如果仍然显示 'No changes detected'，请检查："
echo "1. models.py 文件是否存在并有内容"
echo "2. apps.py 中是否正确定义了 AppConfig"
echo "3. INSTALLED_APPS 中是否包含应用名称"





