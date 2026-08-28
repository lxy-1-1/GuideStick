from django.db import models
from django.contrib.auth.models import AbstractUser

# 自定义用户模型，继承自 Django 内置的 AbstractUser 模型
class User(AbstractUser):
    # 使用唯一的邮箱作为用户的标识之一
    email = models.EmailField(unique=True)

    # 可选的手机号字段，允许为空或为空字符串
    phone = models.CharField(max_length=15, blank=True, null=True)

    # 用户创建时间，自动填充为创建记录时的时间
    created_at = models.DateTimeField(auto_now_add=True)

    # 用户信息最后更新时间，保存记录时自动更新
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        # 数据库中对应的表名
        db_table = 'users'

        # 管理后台显示的名称（单数）
        verbose_name = '用户'

        # 管理后台显示的名称（复数）
        verbose_name_plural = '用户'

    # 返回用户名作为对象的字符串表示
    def __str__(self):
        return self.username
