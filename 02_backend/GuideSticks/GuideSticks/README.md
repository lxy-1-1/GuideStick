# 导盲杖项目使用说明

## 项目概述
这是一个基于计算机视觉和深度学习的智能导盲杖系统，旨在为视障人士提供更安全、更智能的出行辅助。

## 功能特性
- 实时物体检测和识别
- 语音提示和警告
- 障碍物距离估算
- 路径规划和导航
- 紧急求助功能

## 硬件方面
- 导盲杖主体
- 摄像头模块
- 超声波传感器
- 语音模块
- 电池模块
- 控制主板

## 软件方面

### 服务器相关

建议选用2核4g以上的配置，不建议在使用更低的配置，yolo还是很吃配置的。系统为ubunt系统（建议为系统安装BT面板）。

#### BT面板

1.需要注意的是宝塔官方给我们的IP是动态的，也就是说如果你关闭服务器后对服务器重启，那么你的宝塔面板的就很有可能改变。如果你发现你的IP不管用了，就远程连接一下服务器，输入：

```bash
bt
```

接着，服务器会给你提示，这时输入14即可得到新的ip。  

2.如果你安装了宝塔面板，宝塔面板对系统的防火墙有了更多的限制。也就是说，加入你的网站的端口为8000，那么也就意味着你不仅要在服务器的官方网站上面的安全管理处（以阿里云服务器为例，阿里云安全组，并找到管理规则即可添加）添加允许任何设备（或者仅仅为你的ip）访问8000端口，还要再宝塔面板的安全处添加管理规则。  

### 环境配置

我使用的python3.9，其他的python版本应该没有问题，python包方面，使用下面的命令：  

```bash
# 创建虚拟环境
python -m venv venv-3.9

# 激活虚拟环境
# Windows:
venv-3.9\Scripts\activate
# Linux/Mac:
source venv-3.9/bin/activate
# 使用pip安装所有依赖
pip install -r requirements.txt
```

这里面的一些包因为是我直接在网上找的下载建议，所以一些包再开发过程中时没有用的，如果你确定包没用的话可以将包删除。（按照我上面的配置文件确实不小，.venv文件直接被我干道5GB了）  

**注意在window与在linux系统创建的虚拟环境不可以混合使用，所以我创建了两个虚拟环境变量文件**  

### 数据库

建立一个名字为guidesticks的数据库，需要注意的是，如果是远程（当然到最后大概率程序是再服务器上面运行的）注意数据库的注册者不要为root，好像是因为安全方面的问题。

## 项目结构
```
GuideSticks/
├── manage.py
├── requirements.txt
├── GuideSticks/          # 项目配置目录
│   ├── __init__.py
│   ├── settings.py
│   ├── urls.py
│   ├── wsgi.py
│   └── asgi.py
├── Android/             # Android应用目录
└── templates/          # 模板文件目录
```

## 部署说明
1. 克隆项目到本地
```bash
git clone git@github.com:timefiy/GuideSticks.git
```

2. 安装依赖
```bash
pip install -r requirements.txt
```

3. 配置数据库
- 创建名为 guidesticks 的数据库
- 修改 settings.py 中的数据库配置

4. 运行数据库迁移
```bash
python manage.py migrate
```

5. 启动开发服务器
```bash
python manage.py runserver
```

## API文档
主要API接口：
- `/android/` - Android应用接口
- `/admin/` - 管理后台接口

## 注意事项
1. 确保服务器防火墙配置正确
2. 定期备份数据库
3. 注意保护敏感配置信息
4. 建议使用HTTPS进行安全通信

## 配置建议（2026-04）

复制 `.env.example` 为 `.env` 后再按环境修改。`.env` 已被 Git 忽略，不能提交：

```bash
cp .env.example .env
```

后端通过环境变量配置关键参数，生产环境请使用独立的密钥和数据库密码：

```bash
# Django
DJANGO_SECRET_KEY=replace-with-long-random-secret
DJANGO_DEBUG=false
DJANGO_ALLOWED_HOSTS=api.example.com,localhost

# Database (MySQL)
DJANGO_DB_ENGINE=django.db.backends.mysql
DJANGO_DB_NAME=guidesticks
DJANGO_DB_USER=guidesticks
DJANGO_DB_PASSWORD=replace-with-real-password
DJANGO_DB_HOST=127.0.0.1
DJANGO_DB_PORT=3306
```

本地开发可启用 SQLite，避免依赖外部 MySQL：

```bash
DJANGO_USE_SQLITE=true
```

运行测试时使用内存 SQLite 设置：

```bash
DJANGO_SETTINGS_MODULE=GuideSticks.test_settings python manage.py test
```
