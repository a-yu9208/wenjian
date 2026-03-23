#!/bin/bash
# 修复数据库迁移冲突

echo "修复数据库迁移状态..."

# 方案 1: 标记迁移为已完成（不实际创建表）
echo "标记 Django 内置迁移为已完成..."
python manage.py migrate --fake contenttypes.0001_initial
python manage.py migrate --fake auth.0001_initial
python manage.py migrate --fake sessions.0001_initial

# 应用其余迁移
echo "应用剩余迁移..."
python manage.py migrate

# 如果还有问题，重置迁移
echo ""
echo "如果仍然出错，可以尝试："
echo "python manage.py migrate --fake"
echo ""
echo "或者重置数据库："
echo "DROP DATABASE boke_db;"
echo "CREATE DATABASE boke_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
echo "python manage.py migrate"




