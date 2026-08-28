#!/usr/bin/env python
"""
Django的命令行工具,用于执行各种管理任务。
这是Django项目的标准入口点,用于运行各种管理命令。
"""

import os  # 导入操作系统相关功能
import sys  # 导入系统相关功能


def main():
    """
    执行Django管理任务的主函数。
    设置Django环境并执行命令行参数指定的命令。
    """
    # 设置Django的配置文件路径
    # 这里指定使用GuideSticks项目中的settings.py作为配置文件
    os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'GuideSticks.settings')
    
    try:
        # 尝试导入Django的管理命令执行器
        from django.core.management import execute_from_command_line
    except ImportError as exc:
        # 如果导入失败，抛出错误提示
        # 可能的原因包括：Django未安装、环境变量未正确设置、虚拟环境未激活等
        raise ImportError(
            "无法导入Django。请确认Django是否已安装，"
            "并且PYTHONPATH环境变量是否正确设置？"
            "是否忘记激活虚拟环境？"
        ) from exc
    
    # 执行从命令行传入的命令
    # sys.argv包含了所有命令行参数
    execute_from_command_line(sys.argv)


# 当脚本直接运行时（而不是被导入时），执行main()函数
if __name__ == '__main__':
    main()
