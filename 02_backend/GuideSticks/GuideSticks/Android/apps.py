from django.apps import AppConfig

class AndroidConfig(AppConfig): 
    # 自动主键字段类型，默认使用 BigAutoField（64位自增主键）
    default_auto_field = 'django.db.models.BigAutoField'

    # app 的 Python 路径（必须匹配 app 文件夹名称或其导入路径）
    name = 'Android'

    # 后台显示的 app 名称（可读性更强，用于 admin 后台）
    verbose_name = 'Android应用'
