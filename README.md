# 视觉感知与云端大模型智能导盲辅助系统

> 团队：智途 · 口号：智行无碍，途达万家

面向视障人士户外出行场景的 AIoT 导盲辅助原型。系统将视觉识别、距离感知、规则安全控制、移动端交互与紧急求助串联为闭环：设备采集道路环境，云端识别交通信号及道路目标，Android 客户端提供识别展示、出行情景解释和设备管理，终端以语音与灯光反馈关键结果。

## 系统能力

- **视觉感知**：摄像头采集图像并上传至 Django 服务，由 YOLOv5 进行红绿灯及道路目标识别。
- **距离与安全提示**：测距结果与视觉状态共同用于生成 `STOP`、`PROCEED`、`CAUTION` 等确定性安全提示。
- **移动端交互**：Android 客户端支持登录注册、识别展示、出行助手、设备查找、紧急联系人与告警接收。
- **场景解释**：DeepSeek 仅接收后端提供的结构化场景状态和用户问题，用于生成简短解释；不读取原始图像，也不直接下发设备控制或紧急求助指令。
- **离线兜底**：网络不可用时，设备仍可基于距离阈值进行本地避障预警。

## 工作流程

```text
摄像头 / 测距传感器
        ↓
ESP32 系列设备采集、联网与本地提示
        ↓ HTTP/HTTPS
Django REST 服务 → YOLOv5 视觉识别 → 规则安全决策
        ↓                              ↓
Android 客户端 ← 场景状态与交互结果 ← DeepSeek 场景解释（受限）
        ↓
语音、灯光、告警与求助反馈
```

## 仓库结构

| 目录 | 内容 |
| --- | --- |
| [`01_hardware`](01_hardware) | ESP32 端固件与硬件相关素材 |
| [`02_backend`](02_backend) | Django/DRF 后端、YOLOv5 推理与数据库脚本 |
| [`03_android_client`](03_android_client) | Android Studio 客户端工程 |
| [`04_docs`](04_docs) | 项目结构、赛道作品信息等说明 |
| [`docs`](docs) | 真机验收清单与设计记录 |
| [`output`](output) | 已生成的设计文档与展示材料 |

## 快速开始

### 后端

```powershell
cd 02_backend\GuideSticks\GuideSticks
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
Copy-Item .env.example .env
python manage.py migrate
python manage.py runserver
```

`.env.example` 默认启用 SQLite，适合本地体验；生产环境请改用独立 MySQL 并配置 Django 密钥、数据库和 DeepSeek 服务。不要把任何密钥写入代码或提交到仓库。详细部署说明见 [后端 README](02_backend/GuideSticks/GuideSticks/README.md)。

### Android 客户端

使用 Android Studio 打开 [`03_android_client/GuideSticks_android`](03_android_client/GuideSticks_android)，在真机或模拟器上完成构建。真机联调前，请确认客户端、后端与设备端的服务地址一致。

### 设备端

使用 Arduino IDE 打开 [`01_hardware/daomangzhang/daomangzhang.ino`](01_hardware/daomangzhang/daomangzhang.ino)，根据实际硬件填写 Wi-Fi 与服务端地址后烧录。进行 ESP32-S3 与 ToF 测距迁移时，应逐边界验证摄像头采集、联网、上传、测距和终端反馈。

## 文档入口

- [项目结构说明](04_docs/项目结构说明.md)
- [乐鑫赛道作品信息](04_docs/乐鑫赛道作品信息填写稿.md)
- [Android 客户端真机验收清单](docs/android-client-manual-checklist.md)

## 安全与仓库约定

- `.env`、Python 虚拟环境、Gradle 缓存、调试签名和模型运行产物已排除，不会上传。
- 模型权重、云端地址与密钥应由部署环境单独管理。
- 设备安全控制由可审计的确定性规则负责；大模型仅作为场景解释与交互辅助。

## 许可与使用说明

本仓库包含项目集成代码以及第三方 YOLOv5 组件。使用、分发或二次开发时，请分别遵守各组件随附的许可证与相关服务条款。
