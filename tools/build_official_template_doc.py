from copy import deepcopy
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile

from lxml import etree


SOURCE = Path(r"A:\4928bfc9-c8a1-484f-848e-9dcb9f15ba7a (1).docx")
OUTPUT = Path(
    r"E:\GuideStick\output\documents\视觉感知与云端大模型智能导盲辅助系统-官方模板最终版.docx"
)
TITLE = "视觉感知与云端大模型智能导盲辅助系统"
TEAM = "智途"

W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
CP = "http://schemas.openxmlformats.org/package/2006/metadata/core-properties"
DC = "http://purl.org/dc/elements/1.1/"
NS = {"w": W, "r": R, "cp": CP, "dc": DC}


ABSTRACT = (
    "面向视障人士独立出行时难以及时获取交通信号灯、位置和设备状态等信息的问题，"
    "本作品设计了一套融合视觉感知、云端大模型与无障碍交互的智能导盲辅助系统。"
    "系统以乐鑫ESP32-S3为核心主控，连接OV2640摄像头、实体按键、LED和LU-ASR PRO"
    "语音模块，通过2.4 GHz WiFi将路口图像上传至Django云端服务，由YOLOv5完成红绿灯"
    "识别。服务端进一步融合识别结果、手机定位、设备在线状态和用户问题，调用DeepSeek"
    "生成简短、谨慎的出行场景解释，并通过STOP、PROCEED、CAUTION等受限指令形成"
    "大模型到设备的下行交互。Android客户端提供红绿灯识别、出行助手、手机定位地图、"
    "当前位置播报、紧急联系人短信与电话求助、寻找导盲杖以及服务器和硬件在线状态监测，"
    "同时适配TalkBack、大字体、语音输入、TTS播报和振动反馈。作品采用“端侧采集—无线"
    "传输—云端识别与理解—多模态反馈”的端云协同架构，在控制成本的同时提升交通视觉"
    "信息的可达性。系统定位为传统白手杖的数字化信息补充，不替代物理探路、交通规则和"
    "使用者的现场安全判断。"
)


CHAPTERS = [
    {
        "title": "设计需求分析",
        "break": 67,
        "items": [
            ("h2", "项目背景与问题"),
            ("h3", "视障出行中的信息缺口"),
            (
                "p",
                "交通信号灯、路口环境、当前位置和紧急联系信息主要通过视觉界面呈现，"
                "视障人士独立出行时难以同步获取。传统白手杖能够完成近距离物理探路，"
                "但无法识别红绿灯，也不能主动说明设备、网络和位置状态；部分电子导盲"
                "产品仅依赖测距或预设语音，难以形成从环境感知到场景解释的完整闭环。",
            ),
            (
                "p",
                "本项目聚焦“交通视觉信息不可达、求助链路分散、硬件状态不可知、移动端"
                "操作不友好”四类问题，将ESP32-S3感知终端、云端视觉识别与大模型、"
                "Android无障碍客户端组合为统一系统，使用户能够通过实体按键、触屏或"
                "语音完成关键操作，并获得短句式、多通道反馈。",
            ),
            ("h2", "用户需求与使用场景"),
            ("h3", "核心使用场景"),
            (
                "table",
                [
                    ["使用场景", "核心需求", "系统处理", "反馈方式"],
                    ["路口等候", "了解前方信号灯状态", "拍照并由云端模型识别", "语音、文字和安全指令"],
                    ["日常出行", "获取位置和场景提示", "融合手机定位与识别结果", "地图、位置播报和对话"],
                    ["紧急情况", "快速联系预设联系人", "生成含位置链接的求助信息", "短信与电话呼叫"],
                    ["设备遗失", "在附近寻找导盲杖", "客户端下发一次性寻杖指令", "导盲杖语音提示"],
                    ["系统异常", "判断服务端和硬件状态", "分别检查服务和设备心跳", "在线状态与最近通信时间"],
                ],
                [1.1, 1.5, 1.7, 1.5],
            ),
            ("h2", "设计目标与约束"),
            ("h3", "设计目标"),
            (
                "p",
                "（1）以ESP32-S3作为核心主控，完成摄像头、按键、语音和网络通信接入；"
                "（2）形成一次可演示的“采集—上传—识别—解释—反馈”闭环；"
                "（3）将摄像头识别、手机位置、设备状态和用户问题融合为出行场景上下文；"
                "（4）通过TalkBack、大字体、语音和振动降低视障用户的操作门槛[1-3]。",
            ),
            ("h3", "安全与工程约束"),
            (
                "p",
                "模型回答必须简短、非绝对化，无法确认时提示用户谨慎等待；自由文本不得"
                "直接控制硬件，所有下行命令必须经过白名单校验。系统优先采用成熟开源"
                "框架和通用模块，云端API密钥仅保存在服务器环境变量中。网络异常时，"
                "本地按键、照明和固定语音等基础功能不应被永久阻塞。",
            ),
        ],
    },
    {
        "title": "特色与创新",
        "break": 98,
        "items": [
            ("h2", "端云协同的视觉导盲闭环"),
            ("h3", "轻量化端侧与云端专用识别"),
            (
                "p",
                "ESP32-S3承担图像采集、按键交互、网络连接、设备心跳和受限指令执行，"
                "计算量较大的YOLOv5推理部署在云端。该分工降低手持终端的算力与存储"
                "压力，同时保留设备侧主动感知和指令响应能力。与仅在手机端识别的方案"
                "相比，导盲杖本体可以独立触发采集；与固定播报设备相比，云端模型能够"
                "持续优化[2-3,6-7]。",
            ),
            ("h2", "多源感知信息融合"),
            ("h3", "视觉、位置与设备状态联合建模"),
            (
                "p",
                "系统不把摄像头、定位和设备状态割裂展示，而是将红绿灯识别结果、手机"
                "经纬度与地址、导盲杖最近通信时间以及用户当前问题组织为结构化场景上下文。"
                "大模型基于这些感知信息生成与当前位置和设备状态相关的解释，使视觉识别"
                "结果能够服务于位置播报、紧急求助和状态提醒等连续任务。",
            ),
            ("h2", "云端大模型解释与受限指令下行"),
            ("h3", "可解释但不越权的交互机制"),
            (
                "p",
                "DeepSeek被定位为“出行场景对话与解释助手”，不替代YOLOv5完成红绿灯"
                "分类，也不直接给出绝对安全结论。服务端只允许模型从STOP、PROCEED、"
                "CAUTION等有限指令集中选择，经规则校验后写入设备队列，ESP32-S3轮询"
                "并执行，从而满足大模型下行交互要求并保留审计边界[11]。",
            ),
            ("h2", "面向视障用户的多模态无障碍交互"),
            ("h3", "多入口与多反馈协同"),
            (
                "p",
                "系统同时提供实体按键、离线固定语音、手机语音输入、TTS播报、振动和"
                "大字体界面。Android客户端为控件设置可读语义，聊天记录区分“你说”和"
                "“出行助手说”，状态轮询采用去重播报，避免TalkBack重复朗读。紧急求助"
                "使用高对比样式和较大触控区域，兼顾全盲、低视力用户及陪护人员[10]。",
            ),
            (
                "table",
                [
                    ["对比维度", "常见电子导盲方案", "本作品"],
                    ["环境感知", "单一测距或固定提醒", "交通灯视觉识别与场景信息融合"],
                    ["智能能力", "预设语音或普通问答", "识别结果驱动的大模型解释与受限指令"],
                    ["交互方式", "单一声音或触屏", "按键、语音、TTS、振动和TalkBack协同"],
                    ["设备管理", "仅判断服务器是否可达", "服务器与导盲杖在线状态分离判断"],
                    ["紧急链路", "单独拨号", "位置短信、地图链接和电话呼叫组合"],
                ],
                [1.2, 2.0, 2.5],
            ),
        ],
    },
    {
        "title": "功能设计",
        "break": 108,
        "items": [
            ("h2", "红绿灯视觉识别"),
            ("h3", "采集与反馈流程"),
            (
                "p",
                "用户按下拍照键后，ESP32-S3驱动OV2640获取前方图像，经JPEG压缩和"
                "Base64编码后通过HTTP上传。服务器完成解码与YOLOv5推理，将结果归一化"
                "为红灯、绿灯或无法确认，并同步产生STOP、PROCEED或CAUTION安全指令。"
                "图像质量不足、网络超时或模型置信度不足时，系统统一提示谨慎等待[4,6-7]。",
            ),
            ("h2", "出行场景对话助手"),
            ("h3", "语音和文字问答"),
            (
                "p",
                "Android客户端支持键盘和系统语音识别输入。用户可询问“刚才识别到了"
                "什么”“当前设备是否在线”等问题，也可进行缓解紧张情绪的陪伴式对话。"
                "客户端提交有限对话历史和最近场景信息，后端调用DeepSeek生成简短回答；"
                "网络失败时保留问题并提供重试提示[11]。",
            ),
            ("h2", "语音、按键与照明"),
            ("h3", "设备侧基础交互"),
            (
                "p",
                "导盲杖设置拍照识别、紧急求助和夜间照明三个实体按键。LU-ASR PRO承担"
                "离线唤醒和固定播报，ESP32-S3通过串口发送经过校验的指令码；Android端"
                "使用系统TTS播报识别、大模型回答、位置和状态。双通道设计保证用户未打开"
                "手机界面时仍能完成基础操作[12]。",
            ),
            ("h2", "定位与紧急求助"),
            ("h3", "位置短信和电话呼叫"),
            (
                "p",
                "定位功能调用手机定位能力获取经纬度、精度、更新时间和解析地址，在地图"
                "中显示当前位置，并提供位置播报与外部地图入口。用户预先设置紧急联系人；"
                "触发求助后，客户端生成包含地址、坐标和地图链接的短信，再进入电话呼叫"
                "流程。权限、SIM卡或定位不可用时分别给出降级提示。",
            ),
            ("h2", "寻找导盲杖与设备状态"),
            ("h3", "一次性指令与心跳复用"),
            (
                "p",
                "客户端点击“寻找导盲杖”后，服务器写入一次性寻杖状态；ESP32-S3周期"
                "轮询时读取并清除该状态，驱动语音模块发出定位提示。相同轮询同时作为"
                "硬件心跳，服务器记录最近通信时间并进行超时判断。首页分别显示服务器"
                "和导盲杖状态，避免服务器在线时误认为硬件也已连接。",
            ),
        ],
    },
    {
        "title": "系统实现",
        "break": 118,
        "items": [
            ("h2", "总体技术架构"),
            ("h3", "分层架构与数据流"),
            (
                "p",
                "系统由感知层、传输层、控制层、智能层和交互层组成。ESP32-S3终端负责"
                "摄像头、按键、语音模块和设备状态；Django提供统一REST服务并协调YOLOv5"
                "与DeepSeek；Android客户端负责认证、识别入口、对话、定位、求助、寻杖"
                "和无障碍反馈。数据流为“设备采集—WiFi上传—云端识别—场景融合—大模型"
                "解释—客户端或设备反馈”。",
            ),
            (
                "table",
                [
                    ["层级", "主要组件", "核心职责"],
                    ["感知层", "ESP32-S3、OV2640、按键、手机定位", "采集视觉、位置和用户操作信息"],
                    ["传输层", "2.4 GHz WiFi、HTTP、JSON、Base64", "上传图像和状态，下发结果与指令"],
                    ["控制层", "ESP32-S3固件、Django、Android逻辑", "状态机、权限和任务编排"],
                    ["智能层", "YOLOv5、DeepSeek", "红绿灯识别、场景解释和受限指令生成"],
                    ["交互层", "LU-ASR PRO、TTS、TalkBack、振动", "多模态反馈和无障碍操作"],
                ],
                [1.0, 2.1, 2.6],
            ),
            ("h2", "感知层与硬件实现"),
            ("h3", "ESP32-S3视觉终端"),
            (
                "p",
                "最终硬件以ESP32-S3系列模组为核心并连接OV2640摄像头。板卡选型和PCB"
                "设计需确认摄像头接口、PSRAM、GPIO和瞬时供电能力；按键避开启动配置相关"
                "引脚，天线区域保持净空。自制PCB丝印层标注加工时间，且不出现学校、教师"
                "或学生姓名等敏感信息[2-4]。",
            ),
            ("h3", "语音和实体交互"),
            (
                "p",
                "LU-ASR PRO连接麦克风和扬声器，承担离线唤醒、固定指令识别与提示音播放。"
                "ESP32-S3通过串口或GPIO与语音模块、按键和LED协同。当前未实现的超声波"
                "测距和IMU仅列为后续扩展，不在本作品已实现功能中描述[12]。",
            ),
            ("h2", "传输层与控制层实现"),
            ("h3", "网络通信"),
            (
                "p",
                "ESP32-S3以STA模式连接2.4 GHz WiFi。图像采用JPEG压缩、Base64编码并"
                "封装为JSON，HTTP请求设置连接和读取超时。Android客户端使用Retrofit与"
                "OkHttp访问后端，服务器地址可在设置页修改，便于在校园局域网、手机热点"
                "和云服务器之间切换[3]。",
            ),
            ("h3", "Arduino C++固件"),
            (
                "p",
                "设备固件采用Arduino框架和C++开发，主要任务包括摄像头初始化、按键消抖、"
                "LED控制、WiFi管理、HTTP请求、设备心跳和串口指令输出。正式迁移至所选"
                "ESP32-S3摄像头板时，需要按实际板卡修改摄像头引脚和初始化配置，并完成"
                "供电、启动、PSRAM、联网和连续采集验收[5]。",
            ),
            ("h2", "云端服务与人工智能实现"),
            ("h3", "Django与YOLOv5"),
            (
                "p",
                "后端使用Python、Django 4.1.7和Django REST Framework构建。识别接口"
                "接收Base64图像，调用PyTorch版YOLOv5自定义权重，返回识别类别、提示"
                "文本和安全指令。认证模块采用JWT，识别、寻杖、心跳和对话功能通过独立"
                "接口组织，便于测试与扩展[6-9]。",
            ),
            ("h3", "DeepSeek云端大模型"),
            (
                "p",
                "服务端从环境变量读取API密钥，通过兼容接口调用deepseek-v4-flash。系统"
                "提示词限定其为出行场景解释助手，输入包括用户问题、最近识别结果、位置"
                "摘要和设备状态；输出限制长度、语气和安全边界。模型不可访问密钥，也不"
                "允许生成白名单外的设备控制指令[11]。",
            ),
            ("h2", "Android无障碍客户端实现"),
            ("h3", "界面和辅助功能"),
            (
                "p",
                "客户端使用Java和Material Components构建高对比、低层级界面。首页提供"
                "识别、出行助手、寻杖和紧急求助入口；底部导航包含首页、位置和我的。"
                "关键控件设置内容描述和状态语义，大字体、自动语音播报与触觉反馈可在设置"
                "页调整。地图加载失败时仍保留地址、坐标、播报和外部地图入口[10]。",
            ),
            ("h2", "测试与验收"),
            ("h3", "软件回归与硬件验收边界"),
            (
                "p",
                "当前软件回归覆盖认证、视觉接口、大模型调用与降级、寻杖状态、设备心跳、"
                "服务器地址、地图链接、紧急消息和对话存储等核心逻辑。Django端35项测试"
                "通过，Android单元测试、Lint和Debug APK构建通过。上述结果只证明软件"
                "逻辑，不替代ESP32-S3实物验收；提交视频前仍需完成硬件冷启动、联网、"
                "连续采集、视觉闭环、大模型下行和语音反馈实测。",
            ),
        ],
    },
    {
        "title": "其他内容",
        "break": 128,
        "items": [
            ("h2", "工业设计与结构"),
            ("h3", "手柄布局与可维护性"),
            (
                "p",
                "摄像头布置于手柄前端，拍摄方向与行进方向一致；三个实体按键通过位置、"
                "间距和触感区分；扬声器开孔朝向使用者；电池、降压模块和主控固定在内部"
                "支架上。ESP32-S3天线区域与电池、金属件及PCB大面积铺铜保持距离，外壳"
                "边缘采用圆角，线束增加应力释放。",
            ),
            ("h2", "成本估算"),
            ("h3", "原型成本"),
            (
                "table",
                [
                    ["部件", "数量", "参考成本（元）", "说明"],
                    ["ESP32-S3摄像头开发板或模组", "1", "50—90", "取决于Flash、PSRAM和摄像头配置"],
                    ["LU-ASR PRO及扬声器", "1套", "30—60", "离线唤醒与固定语音反馈"],
                    ["电池与电源管理", "1套", "25—45", "含电芯、充电保护与稳压"],
                    ["按键、LED和连接器", "1批", "10—20", "基础交互与装配辅料"],
                    ["PCB及结构件", "1套", "50—100", "取决于打样数量和外壳工艺"],
                    ["合计", "—", "约165—315", "不含开发工具和云服务器成本"],
                ],
                [2.0, 0.8, 1.2, 2.0],
            ),
            ("h2", "隐私、伦理与安全说明"),
            ("h3", "权限和数据边界"),
            (
                "p",
                "系统只在用户主动触发识别、定位、语音或紧急求助时申请相应权限。交通"
                "图像用于即时识别，设计上不作为身份档案；DeepSeek密钥仅保存在服务端。"
                "红绿灯识别和大模型回答均可能受到遮挡、逆光、网络和模型误差影响，因此"
                "界面与播报强调结合现场声音、盲道、交通规则和必要的人力协助进行判断。",
            ),
            ("h2", "可扩展方向"),
            ("h3", "后续升级"),
            (
                "p",
                "后续可增加ToF或超声波测距模块，对低矮和悬空障碍进行距离提醒；增加IMU"
                "识别跌倒、长时间静止和异常姿态；将轻量视觉模型迁移至ESP32-S3端侧以"
                "降低网络依赖；使用MQTT与设备身份认证替代演示阶段轮询；进一步融合公交、"
                "校园道路和无障碍设施数据，形成更完整的路线辅助能力。",
            ),
            ("h2", "团队信息"),
            (
                "p",
                "团队名称为“智途”，口号为“智行无碍，途达万家”。团队工作覆盖嵌入式"
                "固件、硬件集成、云端服务、人工智能模型和Android无障碍客户端。",
            ),
        ],
    },
]


REFERENCES = [
    "[1] 全国大学生物联网设计竞赛组委会. 乐鑫命题和开发板类型情况说明[Z]. 2026.",
    "[2] ESPRESSIF SYSTEMS. ESP32-S3 Series Datasheet[EB/OL]. [2026-07-23]. https://www.espressif.com/sites/default/files/documentation/esp32-s3_datasheet_en.pdf.",
    "[3] ESPRESSIF SYSTEMS. ESP-IDF Programming Guide: ESP32-S3[EB/OL]. [2026-07-23]. https://docs.espressif.com/projects/esp-idf/en/latest/esp32s3/.",
    "[4] OMNIVISION TECHNOLOGIES. OV2640 Color CMOS UXGA CameraChip Sensor Datasheet[EB/OL]. [2026-07-23]. https://files.waveshare.com/wiki/common/OV2640DS_en.pdf.",
    "[5] ESPRESSIF SYSTEMS. Arduino-ESP32 Documentation[EB/OL]. [2026-07-23]. https://docs.espressif.com/projects/arduino-esp32/en/latest/.",
    "[6] JOCHER G, CHAURASIA A, QIU J. Ultralytics YOLOv5[CP/OL]. [2026-07-23]. https://github.com/ultralytics/yolov5.",
    "[7] PYTORCH FOUNDATION. PyTorch Documentation[EB/OL]. [2026-07-23]. https://docs.pytorch.org/docs/stable/index.html.",
    "[8] DJANGO SOFTWARE FOUNDATION. Django 4.1 Documentation[EB/OL]. [2026-07-23]. https://docs.djangoproject.com/en/4.1/.",
    "[9] ENCODE OSS LTD. Django REST Framework Documentation[EB/OL]. [2026-07-23]. https://www.django-rest-framework.org/.",
    "[10] GOOGLE. Make Apps More Accessible[EB/OL]. [2026-07-23]. https://developer.android.com/guide/topics/ui/accessibility/apps.",
    "[11] DEEPSEEK. DeepSeek API Docs[EB/OL]. [2026-07-23]. https://api-docs.deepseek.com/.",
    "[12] LU-ASR PRO. LU-ASR PRO V1.0语音识别模块说明书[Z]. 随模块提供的技术资料.",
]


def qn(local):
    return f"{{{W}}}{local}"


def text_nodes(element):
    return element.xpath(".//w:t", namespaces=NS)


def set_text(element, value):
    nodes = text_nodes(element)
    if not nodes:
        paragraphs = element.xpath(".//w:p", namespaces=NS)
        paragraph = paragraphs[0] if paragraphs else element
        run = etree.SubElement(paragraph, qn("r"))
        node = etree.SubElement(run, qn("t"))
        nodes = [node]
    nodes[0].text = value
    if value.startswith(" ") or value.endswith(" "):
        nodes[0].set("{http://www.w3.org/XML/1998/namespace}space", "preserve")
    for node in nodes[1:]:
        node.text = ""


def replace_text_across_nodes(element, old, new):
    nodes = text_nodes(element)
    combined = "".join(node.text or "" for node in nodes)
    start = combined.find(old)
    if start < 0:
        return False
    end = start + len(old)

    positions = []
    cursor = 0
    for node in nodes:
        value = node.text or ""
        positions.append((cursor, cursor + len(value)))
        cursor += len(value)

    start_index = next(
        index for index, (_, node_end) in enumerate(positions) if start < node_end
    )
    end_index = next(
        index
        for index, (node_start, node_end) in enumerate(positions)
        if node_start < end <= node_end
    )
    start_offset = start - positions[start_index][0]
    end_offset = end - positions[end_index][0]

    if start_index == end_index:
        value = nodes[start_index].text or ""
        nodes[start_index].text = value[:start_offset] + new + value[end_offset:]
        return True

    start_value = nodes[start_index].text or ""
    end_value = nodes[end_index].text or ""
    nodes[start_index].text = start_value[:start_offset] + new
    for index in range(start_index + 1, end_index):
        nodes[index].text = ""
    nodes[end_index].text = end_value[end_offset:]
    return True


def strip_bookmarks(element):
    for node in element.xpath(".//w:bookmarkStart | .//w:bookmarkEnd", namespaces=NS):
        parent = node.getparent()
        if parent is not None:
            parent.remove(node)


def clone_paragraph(source_children, index, value):
    paragraph = deepcopy(source_children[index])
    strip_bookmarks(paragraph)
    set_text(paragraph, value)
    return paragraph


def set_cell_text(cell, value):
    set_text(cell, value)


def make_table(template, rows, weights):
    table = deepcopy(template)
    grid = table.find(qn("tblGrid"))
    grid_columns = list(grid) if grid is not None else []
    total_width = sum(int(col.get(qn("w"), "0")) for col in grid_columns) or 9000
    total_weight = sum(weights)
    widths = [max(1, round(total_width * weight / total_weight)) for weight in weights]
    widths[-1] += total_width - sum(widths)

    source_rows = table.findall(qn("tr"))
    header_template = deepcopy(source_rows[0])
    body_template = deepcopy(source_rows[1] if len(source_rows) > 1 else source_rows[0])
    for row in source_rows:
        table.remove(row)

    if grid is not None:
        for col in list(grid):
            grid.remove(col)
        for width in widths:
            col = etree.SubElement(grid, qn("gridCol"))
            col.set(qn("w"), str(width))

    for row_index, values in enumerate(rows):
        row = deepcopy(header_template if row_index == 0 else body_template)
        if row_index == 0:
            row_properties = row.find(qn("trPr"))
            if row_properties is None:
                row_properties = etree.Element(qn("trPr"))
                row.insert(0, row_properties)
            table_header = row_properties.find(qn("tblHeader"))
            if table_header is None:
                table_header = etree.SubElement(row_properties, qn("tblHeader"))
            table_header.set(qn("val"), "true")
        cells = row.findall(qn("tc"))
        while len(cells) > len(values):
            row.remove(cells.pop())
        while len(cells) < len(values):
            new_cell = deepcopy(cells[-1])
            row.append(new_cell)
            cells.append(new_cell)
        for index, (cell, value) in enumerate(zip(cells, values)):
            set_cell_text(cell, value)
            width_nodes = cell.xpath("./w:tcPr/w:tcW", namespaces=NS)
            if width_nodes:
                width_nodes[0].set(qn("w"), str(widths[index]))
                width_nodes[0].set(qn("type"), "dxa")
        table.append(row)
    return table


def append_chapter(body, source_children, chapter, templates):
    body.append(clone_paragraph(source_children, templates["h1"], chapter["title"]))
    for item in chapter["items"]:
        kind = item[0]
        if kind in {"h2", "h3", "p"}:
            body.append(clone_paragraph(source_children, templates[kind], item[1]))
        elif kind == "table":
            body.append(make_table(templates["table"], item[1], item[2]))
        else:
            raise ValueError(f"Unsupported content kind: {kind}")
    body.append(deepcopy(source_children[chapter["break"]]))


def update_settings(xml_bytes):
    root = etree.fromstring(xml_bytes)
    node = root.find(qn("updateFields"))
    if node is None:
        node = etree.SubElement(root, qn("updateFields"))
    node.set(qn("val"), "true")
    return etree.tostring(root, xml_declaration=True, encoding="UTF-8", standalone=True)


def update_core(xml_bytes):
    root = etree.fromstring(xml_bytes)
    title_nodes = root.xpath("./dc:title", namespaces=NS)
    if title_nodes:
        title_nodes[0].text = TITLE
    creator_nodes = root.xpath("./dc:creator", namespaces=NS)
    if creator_nodes:
        creator_nodes[0].text = TEAM
    return etree.tostring(root, xml_declaration=True, encoding="UTF-8", standalone=True)


def build_document(document_xml):
    root = etree.fromstring(document_xml)
    body = root.find(qn("body"))
    source_children = list(body)
    final_sect_pr = deepcopy(source_children[-1])

    for child in list(body):
        body.remove(child)

    # Cover: preserve the official template layout and fill only its slots.
    cover = [deepcopy(source_children[index]) for index in range(0, 15)]
    set_text(cover[4], TITLE)
    cover_cells = cover[10].xpath(".//w:tc", namespaces=NS)
    values = {
        1: "郑州轻工业大学",
        3: TEAM,
        7: "白奥涵",
        9: "胡春昊",
        11: "郭奕冰",
        13: "无",
    }
    for index, value in values.items():
        set_cell_text(cover_cells[index], value)
    set_text(cover[14], "2026年7月")
    for element in cover:
        body.append(element)

    # The instruction page is intentionally omitted. Its final section boundary
    # remains so the abstract keeps the official header/footer relationship.
    body.append(deepcopy(source_children[24]))

    # Abstract.
    body.append(deepcopy(source_children[25]))
    body.append(clone_paragraph(source_children, 26, TITLE))
    body.append(deepcopy(source_children[27]))
    body.append(clone_paragraph(source_children, 28, "摘要"))
    body.append(clone_paragraph(source_children, 52, ABSTRACT))
    body.append(deepcopy(source_children[41]))
    body.append(
        clone_paragraph(
            source_children,
            42,
            "关键词：ESP32-S3；视觉感知；传感器信息融合；云端大模型；端云协同；无障碍交互",
        )
    )
    body.append(deepcopy(source_children[43]))
    body.append(deepcopy(source_children[44]))

    # Keep the official TOC field/content control and update its cached labels.
    body.append(deepcopy(source_children[45]))
    toc = deepcopy(source_children[46])
    for replacement in [
        "项目背景与问题",
        "端云协同的视觉导盲闭环",
        "红绿灯视觉识别",
        "总体技术架构",
        "工业设计与结构",
    ]:
        replace_text_across_nodes(toc, "二级标题", replacement)
    for replacement in [
        "视障出行中的信息缺口",
        "轻量化端侧与云端专用识别",
        "采集与反馈流程",
        "分层架构与数据流",
        "手柄布局与可维护性",
    ]:
        replace_text_across_nodes(toc, "三级标题", replacement)
    body.append(toc)
    for index in range(47, 51):
        body.append(deepcopy(source_children[index]))

    templates = {
        "h1": 51,
        "h2": 53,
        "h3": 56,
        "p": 52,
        "table": source_children[65],
    }
    for chapter in CHAPTERS:
        append_chapter(body, source_children, chapter, templates)

    # References occupy the ninth official section.
    body.append(clone_paragraph(source_children, 129, "参考文献"))
    for reference in REFERENCES:
        body.append(clone_paragraph(source_children, 132, reference))
    body.append(final_sect_pr)

    return etree.tostring(root, xml_declaration=True, encoding="UTF-8", standalone=True)


def build():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(SOURCE, "r") as source:
        document_xml = build_document(source.read("word/document.xml"))
        with ZipFile(OUTPUT, "w", compression=ZIP_DEFLATED) as target:
            for info in source.infolist():
                data = source.read(info.filename)
                if info.filename == "word/document.xml":
                    data = document_xml
                elif info.filename.startswith("word/header") and info.filename.endswith(".xml"):
                    header = etree.fromstring(data)
                    if replace_text_across_nodes(header, "[作品名称]", TITLE):
                        data = etree.tostring(
                            header,
                            xml_declaration=True,
                            encoding="UTF-8",
                            standalone=True,
                        )
                elif info.filename == "word/settings.xml":
                    data = update_settings(data)
                elif info.filename == "docProps/core.xml":
                    data = update_core(data)
                target.writestr(info, data)
    print(OUTPUT)


if __name__ == "__main__":
    build()
