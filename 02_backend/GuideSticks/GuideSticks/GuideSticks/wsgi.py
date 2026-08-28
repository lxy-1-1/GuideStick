"""
WSGI config for GuideSticks project.

It exposes the WSGI callable as a module-level variable named ``application``.

For more information on this file, see
https://docs.djangoproject.com/en/4.1/howto/deployment/wsgi/
"""

# 导入操作系统相关模块
import os

# 导入Django的WSGI应用获取函数
from django.core.wsgi import get_wsgi_application

# 设置Django的环境变量，指定使用GuideSticks项目的settings.py作为配置文件
# 这个设置告诉Django在哪里找到项目的设置
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'GuideSticks.settings')

# 获取WSGI应用实例
# 这个application变量是WSGI服务器（如Gunicorn、uWSGI等）的入口点
# 它会被WSGI服务器调用以处理HTTP请求
application = get_wsgi_application()
