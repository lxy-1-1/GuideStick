from rest_framework import serializers
from .models import User

# 用户信息序列化器：用于展示用户详情信息
class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = User  # 指定使用的模型
        fields = ('id', 'username', 'email', 'phone', 'created_at')  # 序列化和反序列化的字段列表
        read_only_fields = ('id', 'created_at')  # 只读字段，客户端提交时会被忽略


# 用户注册序列化器：用于用户注册时输入和校验数据
class UserRegisterSerializer(serializers.ModelSerializer):
    # 显式声明密码和确认密码字段，并设为仅写入
    password = serializers.CharField(write_only=True)
    confirm_password = serializers.CharField(write_only=True)

    class Meta:
        model = User  # 使用自定义的 User 模型
        fields = ('username', 'email', 'password', 'confirm_password')  # 注册需要填写的字段

    # 校验方法：确保两次密码一致
    def validate(self, data):
        if data['password'] != data['confirm_password']:
            raise serializers.ValidationError("两次输入的密码不一致")  # 抛出验证错误
        return data

    # 创建用户的方法：去掉 confirm_password 字段，并调用 create_user 创建用户
    def create(self, validated_data):
        validated_data.pop('confirm_password')  # 不需要存入数据库的字段移除
        user = User.objects.create_user(  # 使用 Django 提供的 create_user 方法创建用户
            username=validated_data['username'],
            email=validated_data['email'],
            password=validated_data['password']
        )
        return user

# 用户登录序列化器：用于接收登录所需的用户名和密码
class UserLoginSerializer(serializers.Serializer):
    username = serializers.CharField()  # 用户名字段
    password = serializers.CharField()  # 密码字段