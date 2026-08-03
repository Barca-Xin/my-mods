# 用 GitHub Pages 免费部署（先用起来）

网站会得到一个永久公开网址：`https://你的用户名.github.io/你的仓库名/`，模组 jar 也由 GitHub 免费分发，别人可以直接下载。

> ⚠️ 免费版的限制：GitHub Pages 是纯静态托管，跑不了 Java 后端，所以**后台管理界面和下载计数暂时用不了**。发布新版本改为「改数据 → 推代码」的方式（见下方「更新内容」）。以后买了云服务器，切回完整后端即可恢复。

## 一次性准备

### 1. 在 GitHub 上建一个空仓库

1. 打开 https://github.com/new
2. Repository name 填一个名字，比如 `my-mods`
3. **Public**（公开，别人才能访问）
4. 什么都不要勾选（不要初始化 README / .gitignore / license），点 Create repository

### 2. 在本地初始化并推送

在项目根目录 `C:\Users\LEnovo\ModsWebsite` 打开终端（Git Bash），执行：

```bash
git init
git add .
git commit -m "init: mods website static version"
git branch -M main
git remote add origin https://github.com/你的用户名/my-mods.git
git push -u origin main
```

> 推送时会弹出 GitHub 登录窗口（或要求输入用户名 + Personal Access Token），按提示完成。

### 3. 开启 GitHub Pages

1. 打开仓库页面 → **Settings**（设置）
2. 左侧 **Pages**
3. **Build and deployment** → Source 选 **GitHub Actions**
4. 完成

### 4. 等自动部署

每次推 `main` 分支，GitHub Actions 会自动构建并部署（仓库顶部 **Actions** 标签页可看进度，约 1~2 分钟）。完成后：

- 网站地址：`https://你的用户名.github.io/my-mods/`
- 首页点卡片进详情，下载 jar 正常

---

## 之后每次发布新版本（更新内容）

免费版没有后台界面，发布流程改为：

1. 先在本地跑后端，用后台把新版本传好（或者直接改数据库）
2. 重新生成静态数据：
   ```bash
   python scripts/export_data.py
   ```
   （自动更新 `frontend/public/data.json` 并把 jar 拷到 `frontend/public/files/`）
3. 提交推送，网站自动更新：
   ```bash
   git add .
   git commit -m "发布 xxx 新版本"
   git push
   ```

## 遇到问题

| 现象 | 处理 |
|---|---|
| 网址打开是 404 | 确认仓库是 Public；Settings→Pages→Source 选的是 GitHub Actions；等 Actions 跑完 |
| 下载的 jar 打开报错 | 重新跑 `export_data.py` 确认文件齐全后再次推送 |
| 想恢复后台管理/下载计数 | 买云服务器跑完整后端（`README.md` 里的本地启动方式），Nginx 部署 |
