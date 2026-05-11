# 安装部署指南

## 快速开始

### 方式一：使用部署脚本（推荐）

```bash
# 1. 上传项目到服务器
scp -r boke/ user@your-server:/path/to/

# 2. SSH 登录服务器
ssh user@your-server

# 3. 进入项目目录
cd /path/to/boke

# 4. 运行部署脚本
bash deploy.sh

# 5. 按照提示完成配置
```

### 方式二：手动安装

## 详细步骤

### 1. 环境准备

确保服务器已安装以下软件：

```bash
# CentOS/RHEL
yum update -y
yum install -y python3 python3-pip python3-devel gcc mysql-devel
yum install -y epel-release
yum install -y ffmpeg
yum install -y nginx
yum install -y mysql-server
```

### 2. 安装项目依赖

```bash
# 创建虚拟环境
python3 -m venv venv

# 激活虚拟环境
source venv/bin/activate

# 安装依赖
pip install --upgrade pip
pip install -r requirements.txt
```

### 3. 配置数据库

```bash
# 启动 MySQL
systemctl start mysqld
systemctl enable mysqld

# 登录 MySQL
mysql -u root -p

# 在 MySQL 中执行
CREATE DATABASE boke_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'boke_user'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON boke_db.* TO 'boke_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

编辑 `config/settings.py`:

```python
DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.mysql',
        'NAME': 'boke_db',
        'USER': 'boke_user',
        'PASSWORD': 'your_secure_password',
        'HOST': 'localhost',
        'PORT': '3306',
    }
}
```

### 4. 初始化数据库

```bash
python manage.py makemigrations
python manage.py migrate

# 创建超级管理员
python manage.py createsuperuser
```

### 5. 收集静态文件

```bash
python manage.py collectstatic --noinput
```

### 6. 配置 Nginx

```bash
# 复制配置文件
cp nginx.conf.example /etc/nginx/sites-available/boke

# 编辑配置文件，修改路径和域名
nano /etc/nginx/sites-available/boke

# 创建符号链接
ln -s /etc/nginx/sites-available/boke /etc/nginx/sites-enabled/

# 测试配置
nginx -t

# 重启 Nginx
systemctl restart nginx
```

### 7. 配置 Systemd 服务

```bash
# 创建日志目录
sudo mkdir -p /var/log/boke

# 复制服务文件
cp systemd.service.example /etc/systemd/system/boke.service

# 编辑服务文件，修改路径
sudo nano /etc/systemd/system/boke.service

# 重载 systemd
sudo systemctl daemon-reload

# 启用并启动服务
sudo systemctl enable boke
sudo systemctl start boke

# 查看服务状态
sudo systemctl status boke
```

### 8. 测试

访问 http://your-domain.com 查看网站是否正常运行。

## 生产环境优化

### 1. 安全设置

编辑 `config/settings.py`:

```python
DEBUG = False
ALLOWED_HOSTS = ['your-domain.com', 'www.your-domain.com']
SECRET_KEY = 'your-random-secret-key-here'  # 使用复杂随机字符串
```

### 2. 数据库优化

在 MySQL 中执行：

```sql
ALTER DATABASE boke_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. 文件权限

```bash
# 确保上传目录有正确的权限
chown -R www-data:www-data media/
chmod -R 755 media/
```

### 4. 定期备份

创建备份脚本 `backup.sh`:

```bash
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backup/boke"
mkdir -p $BACKUP_DIR

# 备份数据库
mysqldump -u boke_user -p boke_db > $BACKUP_DIR/db_$DATE.sql

# 备份媒体文件
tar -czf $BACKUP_DIR/media_$DATE.tar.gz media/

# 删除 7 天前的备份
find $BACKUP_DIR -name "*.sql" -mtime +7 -delete
find $BACKUP_DIR -name "*.tar.gz" -mtime +7 -delete
```

设置定时任务：

```bash
crontab -e
# 添加以下行，每天凌晨 2 点执行
0 2 * * * /path/to/backup.sh
```

## 常见问题排查

### 1. 服务无法启动

```bash
# 查看日志
sudo journalctl -u boke -n 50

# 查看错误日志
tail -f /var/log/boke/gunicorn-error.log
```

### 2. 静态文件无法加载

```bash
# 重新收集静态文件
python manage.py collectstatic --noinput

# 检查 Nginx 配置
nginx -t
```

### 3. 权限问题

```bash
# 修复权限
sudo chown -R www-data:www-data /path/to/project
sudo chmod -R 755 /path/to/project
```

### 4. 数据库连接失败

检查：
- MySQL 服务是否运行
- 用户名密码是否正确
- 数据库是否存在
- 防火墙是否开放 3306 端口

## 维护命令

```bash
# 查看服务状态
sudo systemctl status boke

# 重启服务
sudo systemctl restart boke

# 查看日志
sudo journalctl -u boke -f

# 更新代码
git pull
source venv/bin/activate
pip install -r requirements.txt
python manage.py migrate
python manage.py collectstatic --noinput
sudo systemctl restart boke
```





