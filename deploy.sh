#!/bin/bash

# 个人文件管理系统部署脚本

set -e  # 遇到错误立即退出

echo "========================================="
echo "  个人文件管理系统 - 部署脚本"
echo "========================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查是否为 root 用户
if [ "$EUID" -eq 0 ]; then 
   echo -e "${RED}请不要使用 root 用户运行此脚本${NC}"
   exit 1
fi

echo -e "${GREEN}步骤 1: 检查系统环境...${NC}"

# 检查 Python
if ! command -v python3 &> /dev/null; then
    echo -e "${RED}错误: 未找到 Python 3${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Python 版本: $(python3 --version)${NC}"

# 检查 MySQL
if ! command -v mysql &> /dev/null; then
    echo -e "${YELLOW}警告: 未找到 MySQL 客户端${NC}"
fi

# 检查 FFmpeg
if ! command -v ffmpeg &> /dev/null; then
    echo -e "${RED}错误: 未找到 FFmpeg，请先安装: yum install -y ffmpeg${NC}"
    exit 1
fi
echo -e "${GREEN}✓ FFmpeg 版本: $(ffmpeg -version | head -n 1)${NC}"

# 检查 Nginx
if ! command -v nginx &> /dev/null; then
    echo -e "${YELLOW}警告: 未找到 Nginx${NC}"
fi

echo -e "\n${GREEN}步骤 2: 创建虚拟环境...${NC}"
if [ ! -d "venv" ]; then
    python3 -m venv venv
    echo -e "${GREEN}✓ 虚拟环境已创建${NC}"
else
    echo -e "${YELLOW}虚拟环境已存在，跳过${NC}"
fi

echo -e "\n${GREEN}步骤 3: 激活虚拟环境并安装依赖...${NC}"
source venv/bin/activate || {
    echo -e "${RED}错误: 无法激活虚拟环境${NC}"
    exit 1
}

# 升级 pip
echo "正在升级 pip..."
pip install --upgrade pip --quiet

# 安装依赖
echo "正在安装依赖包..."
if ! pip install -r requirements.txt; then
    echo -e "${RED}错误: 依赖安装失败${NC}"
    exit 1
fi

# 验证 Django 安装
echo "验证 Django 安装..."
if ! python -c "import django; print(f'Django {django.get_version()}')"; then
    echo -e "${RED}错误: Django 未正确安装${NC}"
    exit 1
fi

echo -e "${GREEN}✓ 依赖安装完成${NC}"

echo -e "\n${YELLOW}注意: 步骤 4 需要先配置数据库${NC}"
echo -e "${YELLOW}请在 config/settings.py 中配置数据库连接信息${NC}"
read -p "数据库已配置完成？(y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${RED}请先配置数据库后再运行此脚本${NC}"
    exit 1
fi

echo -e "\n${GREEN}步骤 4: 初始化数据库...${NC}"
echo "正在生成迁移文件..."
python manage.py makemigrations || {
    echo -e "${YELLOW}警告: 可能没有新的迁移需要生成${NC}"
}
echo "正在执行数据库迁移..."
python manage.py migrate || {
    echo -e "${RED}错误: 数据库迁移失败，请检查数据库配置${NC}"
    echo -e "${RED}确保 MySQL 服务已启动，数据库已创建${NC}"
    exit 1
}
echo -e "${GREEN}✓ 数据库迁移完成${NC}"

echo -e "\n${GREEN}步骤 5: 收集静态文件...${NC}"
python manage.py collectstatic --noinput || {
    echo -e "${YELLOW}警告: 静态文件收集出现问题${NC}"
}
echo -e "${GREEN}✓ 静态文件收集完成${NC}"

echo -e "\n${YELLOW}步骤 6: 创建超级管理员${NC}"
echo "请输入超级管理员信息（如果已存在管理员可跳过，按 Ctrl+C）"
python manage.py createsuperuser || {
    echo -e "${YELLOW}未创建新管理员（可能已存在）${NC}"
}

echo -e "\n${GREEN}步骤 7: 检查目录权限...${NC}"
mkdir -p media/uploads files temp
chmod -R 755 media/ || echo -e "${YELLOW}权限设置可能需要 sudo${NC}"
echo -e "${GREEN}✓ 目录结构已创建${NC}"

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}部署完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "下一步操作："
echo "1. 修改 config/settings.py 中的数据库配置"
echo "2. 创建 Nginx 配置文件"
echo "3. 创建 systemd 服务文件"
echo "4. 启动服务: sudo systemctl start boke"
echo ""
echo "测试运行:"
echo "  source venv/bin/activate"
echo "  python manage.py runserver 0.0.0.0:8000"
echo ""

