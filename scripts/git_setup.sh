#!/usr/bin/env bash
# 一键初始化本地 git 仓库并提交（不含 push，push 单独一步）
set -e
cd /c/Users/LEnovo/ModsWebsite || exit 1

git init -b main
git config user.name "Barca-Xin"
git config user.email "18814053090@163.com"
git add -A
git commit -m "init mods website"
git remote add origin https://github.com/Barca-Xin/my-mods.git

echo "=== 本地仓库就绪 ==="
git log --oneline -1
git remote -v
echo "=== 下一步：执行  git push -u origin main  ==="
