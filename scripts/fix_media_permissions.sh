#!/usr/bin/env bash
# 修复「首页能打开但 /media/、视频播放、文件下载一律 403」——Nginx worker 读不了媒体目录。
# 在服务器上以 root 执行；MEDIA_ROOT 须与 nginx 里 alias 路径一致（默认 /boke/media）。

set -euo pipefail

MEDIA_ROOT="${1:-/boke/media}"

if [[ ! -d "$MEDIA_ROOT" ]]; then
  echo "目录不存在: $MEDIA_ROOT" >&2
  exit 1
fi

echo "==> 递归开放读与目录遍历: $MEDIA_ROOT"
chmod -R a+rX "$MEDIA_ROOT"

if command -v getenforce >/dev/null 2>&1 && [[ "$(getenforce 2>/dev/null)" == "Enforcing" ]]; then
  if command -v chcon >/dev/null 2>&1; then
    echo "==> SELinux Enforcing：尝试打 httpd 可读标签（若失败可改用关闭 SELinux 或定制策略）"
    chcon -R -t httpd_sys_content_t "$MEDIA_ROOT" || true
  fi
fi

echo "完成。请执行 nginx -t && systemctl reload nginx（或你的 reload 方式）后重试访问 /media/ 下文件。"
