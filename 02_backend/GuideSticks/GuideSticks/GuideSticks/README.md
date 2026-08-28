# GuideSticks 项目配置

## 目录结构
```
GuideSticks/
├── __pycache__/     # Python字节码缓存

├── settings.py       # 项目配置文件
├── urls.py          # URL路由配置
├── asgi.py          # 异步服务器接口
├── wsgi.py          # 同步服务器接口
└── __init__.py      # Python包标识
```

## 配置文件说明
1. `settings.py`
   - 数据库配置
   - 应用配置
   - 中间件设置
   - 静态文件设置
   - 安全设置等

2. `urls.py`
   - 项目URL路由配置
   - API端点映射
   - 包含以下路由：
     - `/admin/`: Django管理后台
     - `/android/`: Android应用API

3. `asgi.py`
   - 异步服务器网关接口
   - 支持WebSocket等实时功能
   - 适用于现代异步Web应用

4. `wsgi.py`
   - Web服务器网关接口
   - 用于生产环境部署
   - 处理同步Web请求

## 项目配置
1. 数据库配置
   - 使用MySQL数据库
   - 数据库名: guidesticks
   - 主机: 127.0.0.1
   - 端口: 3306

2. 已安装应用
   - Django管理后台
   - Django认证系统
   - Django会话管理
   - Django消息框架
   - Django静态文件
   - Android应用
   - REST framework

## 部署说明
1. 开发环境
   - 使用`python manage.py runserver`启动开发服务器
   - 默认端口: 8000

2. 生产环境
   - 使用`wsgi.py`或`asgi.py`进行部署
   - 建议使用Nginx + Gunicorn/Uvicorn
   - 配置SSL证书
   - 设置适当的CORS策略

## 注意事项
- 生产环境需要修改`settings.py`中的安全设置
- 确保数据库配置正确
- 定期备份数据库
- 监控服务器性能
- 保持依赖包更新 
