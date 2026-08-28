from pathlib import Path
import sys

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_CELL_VERTICAL_ALIGNMENT, WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK, WD_LINE_SPACING
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "output" / "documents" / "智途-作品设计文档-完成版.docx"
SKILL_SCRIPTS = Path(
    r"A:\.codex\plugins\cache\openai-primary-runtime\documents\26.715.12143\skills\documents\scripts"
)
sys.path.insert(0, str(SKILL_SCRIPTS))
from table_geometry import apply_table_geometry, column_widths_from_weights  # noqa: E402


TITLE = "智途——融合视觉感知与云端大模型的智能导盲辅助系统"
ACCENT = "0B6E69"
ACCENT_LIGHT = "E8F4F2"
TEXT = "202827"
MUTED = "5F6B69"
OUTLINE = "9CB8B4"


def set_font(run, name="宋体", size=Pt(10.5), bold=False, color=TEXT):
    run.font.name = name
    run._element.get_or_add_rPr().rFonts.set(qn("w:eastAsia"), name)
    run._element.get_or_add_rPr().rFonts.set(qn("w:ascii"), "Times New Roman")
    run._element.get_or_add_rPr().rFonts.set(qn("w:hAnsi"), "Times New Roman")
    run.font.size = size
    run.font.bold = bold
    run.font.color.rgb = RGBColor.from_string(color)


def shade_cell(cell, fill):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = tc_pr.find(qn("w:shd"))
    if shd is None:
        shd = OxmlElement("w:shd")
        tc_pr.append(shd)
    shd.set(qn("w:fill"), fill)


def set_cell_border(cell, color=OUTLINE, size="6"):
    tc_pr = cell._tc.get_or_add_tcPr()
    borders = tc_pr.find(qn("w:tcBorders"))
    if borders is None:
        borders = OxmlElement("w:tcBorders")
        tc_pr.append(borders)
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        tag = qn(f"w:{edge}")
        node = borders.find(tag)
        if node is None:
            node = OxmlElement(f"w:{edge}")
            borders.append(node)
        node.set(qn("w:val"), "single")
        node.set(qn("w:sz"), size)
        node.set(qn("w:color"), color)


def set_repeat_table_header(row):
    tr_pr = row._tr.get_or_add_trPr()
    header = OxmlElement("w:tblHeader")
    header.set(qn("w:val"), "true")
    tr_pr.append(header)


def style_table(table, widths, header=True, font_size=Pt(9.5)):
    total = 9360
    dxa = column_widths_from_weights(widths, total)
    apply_table_geometry(
        table,
        dxa,
        table_width_dxa=total,
        indent_dxa=120,
        cell_margins_dxa={"top": 100, "bottom": 100, "start": 120, "end": 120},
    )
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    for row_idx, row in enumerate(table.rows):
        for cell in row.cells:
            cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
            set_cell_border(cell)
            if header and row_idx == 0:
                shade_cell(cell, ACCENT)
            for paragraph in cell.paragraphs:
                paragraph.paragraph_format.space_after = Pt(0)
                paragraph.paragraph_format.line_spacing = 1.15
                for run in paragraph.runs:
                    set_font(
                        run,
                        "黑体" if header and row_idx == 0 else "宋体",
                        font_size,
                        bold=header and row_idx == 0,
                        color="FFFFFF" if header and row_idx == 0 else TEXT,
                    )
    if header and table.rows:
        set_repeat_table_header(table.rows[0])


def add_table(doc, headers, rows, weights, font_size=Pt(9.5)):
    table = doc.add_table(rows=1, cols=len(headers))
    for idx, text in enumerate(headers):
        table.rows[0].cells[idx].text = text
    for values in rows:
        cells = table.add_row().cells
        for idx, text in enumerate(values):
            cells[idx].text = str(text)
    style_table(table, weights, header=True, font_size=font_size)
    doc.add_paragraph().paragraph_format.space_after = Pt(0)
    return table


def add_body(doc, text, bold_prefix=None):
    p = doc.add_paragraph(style="Normal")
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    p.paragraph_format.first_line_indent = Cm(0.74)
    p.paragraph_format.line_spacing_rule = WD_LINE_SPACING.ONE_POINT_FIVE
    p.paragraph_format.space_after = Pt(6)
    if bold_prefix and text.startswith(bold_prefix):
        r1 = p.add_run(bold_prefix)
        set_font(r1, "黑体", Pt(10.5), True)
        r2 = p.add_run(text[len(bold_prefix):])
        set_font(r2)
    else:
        r = p.add_run(text)
        set_font(r)
    return p


def add_bullets(doc, items):
    for item in items:
        p = doc.add_paragraph(style="List Bullet")
        p.paragraph_format.left_indent = Cm(0.74)
        p.paragraph_format.first_line_indent = Cm(-0.35)
        p.paragraph_format.line_spacing = 1.3
        p.paragraph_format.space_after = Pt(3)
        set_font(p.add_run(item))


def add_heading(doc, text, level=1):
    p = doc.add_paragraph(style=f"Heading {level}")
    p.paragraph_format.keep_with_next = True
    p.paragraph_format.space_before = Pt(12 if level == 1 else 9)
    p.paragraph_format.space_after = Pt(6)
    if level == 1:
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run(text)
    set_font(
        r,
        "黑体",
        Pt(16 if level == 1 else (13 if level == 2 else 11)),
        True,
        ACCENT if level <= 2 else TEXT,
    )
    return p


def add_page_number(paragraph):
    paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = paragraph.add_run()
    fld_char1 = OxmlElement("w:fldChar")
    fld_char1.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = " PAGE "
    fld_char2 = OxmlElement("w:fldChar")
    fld_char2.set(qn("w:fldCharType"), "end")
    run._r.append(fld_char1)
    run._r.append(instr)
    run._r.append(fld_char2)
    set_font(run, "宋体", Pt(9), False, MUTED)


def add_static_toc(doc):
    entries = [
        ("1 设计需求分析", 0),
        ("1.1 项目背景与问题", 1),
        ("1.2 用户需求与使用场景", 1),
        ("1.3 设计目标与约束", 1),
        ("2 特色与创新", 0),
        ("2.1 端云协同的视觉导盲闭环", 1),
        ("2.2 大模型场景解释与指令下行", 1),
        ("2.3 面向视障用户的多模态无障碍交互", 1),
        ("3 功能设计", 0),
        ("3.1 红绿灯视觉识别", 1),
        ("3.2 出行场景对话助手", 1),
        ("3.3 定位、求助、寻杖与状态监测", 1),
        ("4 系统实现", 0),
        ("4.1 总体架构", 1),
        ("4.2 感知层、传输层与控制层", 1),
        ("4.3 云端服务与Android客户端", 1),
        ("4.4 测试与验收", 1),
        ("5 其他内容", 0),
        ("参考文献", 0),
    ]
    for text, indent in entries:
        p = doc.add_paragraph()
        p.paragraph_format.left_indent = Cm(0.74 * indent)
        p.paragraph_format.space_after = Pt(4)
        set_font(p.add_run(text), "黑体" if indent == 0 else "宋体", Pt(10.5), indent == 0)


def build():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    doc = Document()
    section = doc.sections[0]
    section.page_width = Cm(21)
    section.page_height = Cm(29.7)
    section.top_margin = Cm(2.5)
    section.bottom_margin = Cm(2.3)
    section.left_margin = Cm(2.5)
    section.right_margin = Cm(2.5)

    styles = doc.styles
    normal = styles["Normal"]
    normal.font.name = "宋体"
    normal._element.rPr.rFonts.set(qn("w:eastAsia"), "宋体")
    normal.font.size = Pt(10.5)
    normal.paragraph_format.line_spacing = 1.5
    normal.paragraph_format.space_after = Pt(6)

    for style_name, font_name, size in [
        ("Heading 1", "黑体", 16),
        ("Heading 2", "黑体", 13),
        ("Heading 3", "黑体", 11),
    ]:
        style = styles[style_name]
        style.font.name = font_name
        style._element.rPr.rFonts.set(qn("w:eastAsia"), font_name)
        style.font.size = Pt(size)
        style.font.bold = True
        style.font.color.rgb = RGBColor.from_string(ACCENT if style_name != "Heading 3" else TEXT)

    doc.core_properties.title = TITLE
    doc.core_properties.subject = "全国大学生物联网设计竞赛乐鑫赛道作品设计文档"
    doc.core_properties.author = "智途团队"
    doc.core_properties.keywords = "ESP32-S3, 智能导盲, 视觉识别, 云端大模型, 无障碍交互"

    # Cover
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(12)
    set_font(p.add_run("全国大学生物联网设计竞赛"), "黑体", Pt(22), True, ACCENT)

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(26)
    set_font(p.add_run("作品设计文档"), "黑体", Pt(28), True)

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(34)
    p.paragraph_format.space_after = Pt(28)
    set_font(p.add_run(TITLE), "黑体", Pt(20), True, ACCENT)

    cover = doc.add_table(rows=7, cols=2)
    cover_data = [
        ("项目", "信息"),
        ("学校名称", "（按报名信息填写）"),
        ("团队名称", "智途"),
        ("参赛赛道", "乐鑫科技赛道"),
        ("队长", "（按报名信息填写）"),
        ("队员", "（按报名信息填写）"),
        ("团队口号", "智行无碍，途达万家"),
    ]
    for row, values in zip(cover.rows, cover_data):
        row.cells[0].text, row.cells[1].text = values
    style_table(cover, [1.3, 3.7], header=True, font_size=Pt(11))
    for row in cover.rows[1:]:
        shade_cell(row.cells[0], ACCENT_LIGHT)
        for run in row.cells[0].paragraphs[0].runs:
            set_font(run, "黑体", Pt(11), True, ACCENT)

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(42)
    set_font(p.add_run("全国大学生物联网设计竞赛组委会"), "宋体", Pt(12))
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    set_font(p.add_run("2026年7月"), "宋体", Pt(12))

    doc.add_page_break()

    # Abstract
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    set_font(p.add_run(TITLE), "黑体", Pt(18), True, ACCENT)
    add_heading(doc, "摘要", 1)
    abstract = (
        "面向视障人士独立出行过程中难以及时获取交通信号灯、位置与设备状态等信息的问题，"
        "本作品设计了一套融合视觉感知、云端智能与无障碍交互的智能导盲辅助系统。系统以乐鑫"
        "ESP32-S3为核心主控，外接OV2640摄像头、实体按键、LED与LU-ASR PRO语音模块，"
        "通过WiFi将交通信号灯图像上传至Django云端服务，由YOLOv5模型完成红绿灯识别，"
        "再以语音、灯光和Android客户端向用户反馈。云端接入DeepSeek大模型，将识别结果、"
        "设备状态和用户问题组织为出行场景上下文，生成简短、明确的安全解释，并预留STOP、"
        "PROCEED、CAUTION等安全指令的设备下行通道。Android客户端面向视障用户提供大字体、"
        "TalkBack语义、语音输入与自动播报，并集成手机定位地图、当前位置播报、紧急联系人短信"
        "与电话求助、寻找导盲杖、服务器与导盲杖在线状态监测等功能。作品采用“端侧采集—无线"
        "传输—云端推理—多模态反馈”的轻量化架构，兼顾低成本、可扩展性与安全边界，可作为"
        "传统导盲杖的数字化信息补充，但不替代物理探路、交通规则和使用者安全判断。"
    )
    add_body(doc, abstract)
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(10)
    r = p.add_run("关键词：")
    set_font(r, "黑体", Pt(10.5), True)
    set_font(p.add_run("ESP32-S3；视觉感知；云端大模型；端云协同；无障碍交互；智能导盲"))

    doc.add_page_break()
    add_heading(doc, "目  录", 1)
    add_static_toc(doc)

    # Chapter 1
    doc.add_page_break()
    add_heading(doc, "1 设计需求分析", 1)
    add_heading(doc, "1.1 项目背景与问题", 2)
    add_body(
        doc,
        "交通信号灯、路口环境、位置与紧急联系信息主要通过视觉界面呈现，视障人士在独立出行时往往难以同步获取。"
        "传统白手杖擅长近距离物理探路，但无法识别红绿灯，也不能主动解释当前场景；部分电子导盲设备仅提供超声波"
        "避障或固定语音，缺乏云端认知能力、远程服务和面向视障用户的完整移动端体验。",
    )
    add_body(
        doc,
        "本项目不以替代白手杖为目标，而是针对“视觉信息不可达、求助链路分散、设备状态不可知、交互入口不友好”"
        "四类痛点提供数字化补充。系统将ESP32-S3感知终端、云端视觉识别与大模型、Android无障碍客户端组合为"
        "闭环，使用户能够通过实体按键或语音完成关键操作，并获得短句式、多通道反馈。",
    )

    add_heading(doc, "1.2 用户需求与使用场景", 2)
    add_table(
        doc,
        ["使用场景", "核心需求", "系统响应"],
        [
            ("路口等候", "了解前方交通信号灯状态", "拍摄图像，云端识别并语音反馈红灯、绿灯或无法确认"),
            ("日常出行", "获得当前位置与简明安全提示", "手机定位、地图显示、位置播报和大模型场景问答"),
            ("紧急情况", "快速联系预设联系人并说明位置", "发送包含位置链接的短信并跳转电话呼叫"),
            ("设备遗失", "快速定位附近导盲杖", "客户端下发寻杖指令，导盲杖通过语音提示响应"),
            ("系统异常", "区分服务器与硬件是否在线", "分别显示服务器状态、导盲杖心跳状态和最近通信时间"),
        ],
        [1.2, 1.8, 3.0],
    )

    add_heading(doc, "1.3 设计目标与约束", 2)
    add_bullets(
        doc,
        [
            "主控合规：最终实物采用乐鑫ESP32-S3系列芯片作为唯一核心主控，并为摄像头、语音和按键预留稳定接口。",
            "闭环完整：至少形成一次“感知数据采集—云端分析—结果解释—设备或客户端反馈”的完整流程。",
            "无障碍优先：关键按钮具备明确语义、足够触控面积、语音和振动反馈，支持TalkBack与大字体。",
            "安全可解释：模型输出使用短句和受限指令集；无法确认时返回谨慎提示，不将模型结果描述为绝对安全结论。",
            "低成本与可维护：采用成熟开源框架和通用模块，硬件可拆换，服务端密钥仅保存在服务器环境变量中。",
        ],
    )

    # Chapter 2
    doc.add_page_break()
    add_heading(doc, "2 特色与创新", 1)
    add_heading(doc, "2.1 端云协同的视觉导盲闭环", 2)
    add_body(
        doc,
        "作品将摄像头作为主要环境感知入口，ESP32-S3负责可靠采集与网络连接，计算量较大的YOLOv5推理部署在云端。"
        "这种分工降低了手持终端的算力与存储压力，同时保留了设备侧的实体交互、状态心跳和指令执行能力。与只在手机"
        "中运行识别的方案相比，导盲杖本体能够独立触发感知；与只进行固定播报的电子导盲杖相比，云端服务能够持续更新。",
    )

    add_heading(doc, "2.2 大模型场景解释与指令下行", 2)
    add_body(
        doc,
        "DeepSeek不承担红绿灯最终分类，而是作为“出行场景对话与解释助手”：接收结构化识别结果和用户问题，输出简短的"
        "安全解释、注意事项和陪伴式回复。为避免自由文本直接控制硬件，系统设计STOP、PROCEED、CAUTION等有限指令集，"
        "服务端完成校验后再写入设备指令队列，由ESP32-S3轮询并通过LU-ASR PRO或LED执行，形成可审计的下行交互。",
    )

    add_heading(doc, "2.3 面向视障用户的多模态无障碍交互", 2)
    add_body(
        doc,
        "系统同时提供实体按键、固定语音、手机语音输入、TTS播报、振动和大字体界面。Android客户端为控件设置内容描述、"
        "标题语义和状态播报，区分“你说”和“出行助手说”，并避免状态定时刷新造成重复朗读。关键操作均设置明确反馈，"
        "紧急求助使用高对比视觉样式和较大触控区域，兼顾全盲、低视力和陪护人员的使用需求。",
    )

    add_heading(doc, "2.4 创新点对比", 2)
    add_table(
        doc,
        ["对比维度", "常见方案", "智途方案"],
        [
            ("环境感知", "单一测距或固定提醒", "摄像头采集交通信号灯，云端专用模型识别"),
            ("智能能力", "预设语音或手机普通问答", "识别结果作为场景上下文，大模型负责解释与受限指令"),
            ("交互方式", "单一声音或手机触屏", "实体按键、语音、播报、振动、TalkBack和大字体协同"),
            ("设备管理", "只判断服务器是否可达", "服务器与导盲杖状态分离，基于心跳判断硬件在线"),
            ("紧急链路", "单独拨号", "预设联系人、位置短信、地图链接与电话呼叫组合"),
        ],
        [1.2, 2.1, 2.7],
    )

    # Chapter 3
    doc.add_page_break()
    add_heading(doc, "3 功能设计", 1)
    add_heading(doc, "3.1 红绿灯视觉识别", 2)
    add_body(
        doc,
        "用户按下拍照键后，ESP32-S3驱动OV2640获取前方图像，经JPEG压缩和Base64编码后通过HTTP上传。服务器完成"
        "解码与YOLOv5推理，将结果归一化为red、green或unknown，并同步生成STOP、PROCEED或CAUTION安全指令。"
        "设备端与Android端均以中文短句反馈；当图像质量不足、网络超时或模型无法确认时，统一提示用户谨慎等待。",
    )

    add_heading(doc, "3.2 出行场景对话助手", 2)
    add_body(
        doc,
        "Android客户端支持键盘和系统语音识别输入，可询问“刚才识别到了什么”“现在适合过马路吗”等场景问题，也可进行"
        "缓解紧张情绪的陪伴式对话。客户端将最近识别结果与有限对话历史提交给Django，后端构建系统提示词并调用DeepSeek。"
        "回答长度和语气受到约束，优先输出可执行、安全、非绝对化的提示；网络失败时保留问题并显示可重试状态。",
    )

    add_heading(doc, "3.3 语音、按键与照明", 2)
    add_body(
        doc,
        "导盲杖设置拍照识别、紧急求助和夜间照明三个实体按键。LU-ASR PRO承担离线唤醒和固定播报，ESP32-S3通过串口"
        "发送经过校验的指令码；Android端使用系统TTS播报识别、大模型回答、位置与状态。双通道设计使用户在未打开手机"
        "界面时仍能完成基础操作，同时保留手机端更丰富的交互能力。",
    )

    add_heading(doc, "3.4 定位与紧急求助", 2)
    add_body(
        doc,
        "定位功能使用手机定位能力获取经纬度、精度和更新时间，在地图中显示当前位置，并提供位置播报与外部地图打开入口。"
        "用户可预先设置紧急联系人；触发求助后，客户端优先发送包含地址、坐标和地图链接的短信，再进入电话呼叫流程。"
        "若权限、SIM卡或定位不可用，界面分别给出降级提示，避免将未执行的动作误报为成功。",
    )

    add_heading(doc, "3.5 寻找导盲杖与设备状态", 2)
    add_body(
        doc,
        "客户端点击“寻找导盲杖”后，服务器写入一次性寻杖状态；ESP32-S3周期轮询时读取并清除该状态，驱动语音模块发出"
        "定位提示。相同轮询同时作为硬件心跳：服务器记录最近通信时间，超过阈值后将导盲杖标记为离线。首页分别展示"
        "“服务器在线/离线”和“导盲杖在线/离线”，避免服务器正常时误认为硬件也已连接。",
    )

    add_heading(doc, "3.6 功能模块汇总", 2)
    add_table(
        doc,
        ["模块", "输入", "处理", "输出"],
        [
            ("红绿灯识别", "摄像头图像", "YOLOv5分类与安全规则", "语音、文字和安全指令"),
            ("出行助手", "问题、对话历史、场景结果", "DeepSeek场景解释", "简短回答和受限设备指令"),
            ("手机定位", "手机定位数据", "地址解析与地图展示", "地址、坐标、精度和位置播报"),
            ("紧急求助", "按键或客户端操作", "权限检查、短信与拨号", "求助短信、地图链接和电话"),
            ("寻找导盲杖", "客户端寻杖命令", "服务器状态队列与设备轮询", "导盲杖语音提示"),
            ("在线监测", "设备周期心跳", "超时判断", "在线状态和最近通信时间"),
        ],
        [1.1, 1.6, 2.0, 1.7],
    )

    # Chapter 4
    doc.add_page_break()
    add_heading(doc, "4 系统实现", 1)
    add_heading(doc, "4.1 总体架构", 2)
    add_body(
        doc,
        "系统由感知终端、网络传输、云端服务和Android交互四部分组成。ESP32-S3终端负责传感器接入、实体按键、摄像头"
        "采集、设备心跳与安全指令执行；Django提供统一REST API，协调YOLOv5与DeepSeek；Android客户端负责账号、"
        "识别入口、对话、定位、求助、寻杖和无障碍反馈。各层通过结构化接口解耦，便于独立测试与后续替换。",
    )
    add_table(
        doc,
        ["层级", "主要组件", "职责"],
        [
            ("感知层", "ESP32-S3、OV2640、按键、LU-ASR PRO、手机定位", "采集视觉、位置和用户操作信息"),
            ("传输层", "2.4GHz WiFi、HTTP、JSON、Base64", "上传图像与状态，下发识别结果和控制指令"),
            ("控制层", "ESP32-S3固件、Django业务服务、Android逻辑", "状态机、权限、任务编排和异常降级"),
            ("智能层", "YOLOv5、DeepSeek", "红绿灯识别、场景解释与受限指令生成"),
            ("交互层", "LU-ASR PRO、Android UI、TTS、TalkBack、振动", "多模态反馈与无障碍操作"),
        ],
        [1.0, 2.3, 2.7],
    )

    add_heading(doc, "4.2 感知层与硬件实现", 2)
    add_body(
        doc,
        "最终硬件以ESP32-S3系列模组为核心，外接OV2640摄像头。选型时需确认摄像头接口、PSRAM容量、板级引脚和电源"
        "能力，固件应依据所选ESP32-S3板卡重新配置摄像头引脚。按键应避开ESP32-S3启动配置相关引脚，天线区域下方保持净空，"
        "电源设计需覆盖摄像头和WiFi发射的瞬时负载。自制PCB丝印层标注加工时间，且不出现学校或个人敏感信息。",
    )
    add_body(
        doc,
        "摄像头是主要环境传感器，负责获取路口视觉信息；手机定位为移动端位置感知来源。两类信息在应用层以场景上下文"
        "形式组织：识别结果用于说明当前信号灯状态，位置用于位置播报和求助消息。系统不虚构超声波测距或IMU功能，后续"
        "可通过I2C、UART或GPIO接口扩展测距和姿态模块。",
    )

    add_heading(doc, "4.3 传输层实现", 2)
    add_body(
        doc,
        "ESP32-S3以STA模式连接2.4GHz WiFi。图像使用JPEG压缩后Base64编码并封装为JSON，请求设置连接和读取超时。"
        "Android客户端使用Retrofit与OkHttp访问REST接口，服务器地址可在设置页修改，便于在校园局域网、手机热点和"
        "云服务器之间切换。设备状态轮询既承担寻杖指令获取，也承担在线心跳，客户端每5秒刷新一次状态。",
    )

    add_heading(doc, "4.4 控制层与固件实现", 2)
    add_body(
        doc,
        "ESP32-S3固件采用Arduino框架和C++开发，主要任务包括摄像头初始化、按键消抖、LED控制、WiFi管理、HTTP请求、"
        "心跳轮询和串口指令输出。为避免网络故障阻塞全部功能，正式迁移时将连接过程改为带超时的非阻塞状态机，基础"
        "按键和照明在断网时仍可使用。服务端下发命令必须经过白名单校验，未知命令仅记录而不执行。",
    )

    add_heading(doc, "4.5 云端服务与人工智能实现", 2)
    add_body(
        doc,
        "后端使用Python 3.10、Django 4.1.7和Django REST Framework构建。识别接口接收Base64图像，调用PyTorch版"
        "YOLOv5自定义权重，返回light、command和message字段。大模型客户端从环境变量读取API密钥，通过官方兼容接口"
        "调用DeepSeek；系统提示词限定其角色、回答长度和安全边界。注册登录采用JWT，密钥不下发至ESP32-S3或Android。",
    )

    add_heading(doc, "4.6 Android无障碍客户端实现", 2)
    add_body(
        doc,
        "客户端使用Java开发，Material Components构建高对比、低层级的操作界面。首页提供识别、出行助手、寻杖和紧急"
        "求助四个主要入口；底部导航包含首页、位置和我的。TalkBack描述不照搬可见文字，而是说明控件目的和执行结果；"
        "大字体、自动语音播报与触觉反馈均可在设置页调整。地图优先展示手机位置，加载失败时仍保留地址、坐标和外部地图入口。",
    )

    add_heading(doc, "4.7 核心接口", 2)
    add_table(
        doc,
        ["接口", "方法", "作用"],
        [
            ("/android/identify/", "POST", "上传图像并返回红绿灯识别结果"),
            ("/android/assistant/chat/", "POST", "提交场景问题并返回大模型回答"),
            ("/android/find_stick/", "GET", "写入一次性寻杖指令"),
            ("/Android/find_stick_status/", "GET", "设备轮询寻杖状态并上报心跳"),
            ("/android/device/status/", "GET", "查询导盲杖在线状态与最近通信时间"),
            ("/android/login/、register/", "POST", "用户注册登录与身份认证"),
        ],
        [2.5, 0.8, 2.7],
    )

    add_heading(doc, "4.8 异常处理与安全边界", 2)
    add_bullets(
        doc,
        [
            "识别无法确认时返回CAUTION，不输出“可以绝对安全通行”等结论。",
            "服务器在线与硬件在线分别判断；服务不可达时显示“状态不可获取”，不误判为导盲杖离线。",
            "紧急求助执行前检查联系人、定位、短信、电话和权限状态，并对部分成功情况分别提示。",
            "DeepSeek API密钥仅保存在服务器环境变量中，客户端不保存第三方模型密钥。",
            "定位、相机、麦克风、电话和短信权限只在相关功能触发时申请，并向用户说明用途。",
        ],
    )

    add_heading(doc, "4.9 软件测试与硬件验收", 2)
    add_body(
        doc,
        "当前软件回归已覆盖认证、视觉接口、大模型调用与降级、寻杖状态、设备心跳、无障碍文本、服务器地址、地图链接、"
        "紧急消息和对话存储等核心逻辑。后端Android应用测试35项通过；Android客户端单元测试35项通过，Lint检查0个错误。"
        "上述结果用于验证软件逻辑，不替代ESP32-S3实物验收。",
    )
    add_table(
        doc,
        ["验收对象", "验收内容", "通过标准"],
        [
            ("ESP32-S3启动", "供电、复位、PSRAM、摄像头初始化", "连续冷启动无异常重启，摄像头可稳定采集"),
            ("网络通信", "热点连接、接口访问、超时恢复", "断网后可恢复连接，基础本地功能不被永久阻塞"),
            ("视觉闭环", "拍照、上传、识别、反馈", "完成一次端到端识别并输出对应语音或文字"),
            ("大模型下行", "命令生成、白名单、设备执行", "仅执行STOP、PROCEED、CAUTION等允许指令"),
            ("无障碍体验", "TalkBack、大字体、语音与振动", "关键流程可在不依赖视觉的情况下完成"),
            ("紧急求助", "定位短信与拨号", "不同权限和设备能力下均给出准确结果提示"),
        ],
        [1.2, 2.2, 2.6],
    )

    # Chapter 5
    doc.add_page_break()
    add_heading(doc, "5 其他内容", 1)
    add_heading(doc, "5.1 工业设计与结构", 2)
    add_body(
        doc,
        "导盲杖终端采用可维护的模块化结构：摄像头布置于手柄前端，拍摄方向与行进方向一致；三个实体按键通过位置、间距"
        "和触感区分；扬声器开孔朝向使用者；电池、降压模块和主控固定在内部支架上。ESP32-S3天线区域与电池、金属件及"
        "PCB大面积铺铜保持距离。外壳边缘采用圆角，线束增加应力释放，充电口和维护口具备明确标识。",
    )

    add_heading(doc, "5.2 成本估算", 2)
    add_table(
        doc,
        ["部件", "参考数量", "参考成本（元）", "说明"],
        [
            ("ESP32-S3摄像头开发板/模组", "1", "50—90", "具体价格取决于Flash、PSRAM与摄像头配置"),
            ("LU-ASR PRO及扬声器", "1套", "30—60", "用于离线唤醒和固定语音反馈"),
            ("电池与电源管理", "1套", "25—45", "含电芯、充电保护与稳压"),
            ("按键、LED及连接器", "1批", "10—20", "基础交互与装配辅料"),
            ("PCB及结构件", "1套", "50—100", "与打样数量、工艺和外壳方式有关"),
            ("合计", "—", "约165—315", "不含开发工具、云服务器和批量制造成本"),
        ],
        [2.0, 0.8, 1.2, 2.0],
    )

    add_heading(doc, "5.3 隐私、伦理与安全说明", 2)
    add_body(
        doc,
        "系统只在用户主动触发识别、定位、语音或紧急求助时申请对应权限。交通图像用于即时识别，设计上不将其作为用户"
        "身份档案；DeepSeek密钥保存在服务端。大模型回答属于辅助提示，红绿灯识别也可能受到遮挡、逆光、网络和模型"
        "误差影响，因此所有界面与播报均强调结合现场声音、盲道、交通规则及必要的人力协助进行判断。",
    )

    add_heading(doc, "5.4 可扩展方向", 2)
    add_bullets(
        doc,
        [
            "增加ToF或超声波测距模块，对低矮障碍和悬空障碍进行距离提醒。",
            "增加IMU，识别跌倒、长时间静止和导盲杖异常姿态。",
            "将部分轻量视觉模型迁移至ESP32-S3端侧，降低网络依赖和响应延迟。",
            "使用MQTT与设备身份认证替代演示阶段的轮询状态，支持多设备管理。",
            "与公交、校园道路和无障碍设施数据结合，提供更完整的路线辅助。",
        ],
    )

    add_heading(doc, "5.5 团队信息", 2)
    add_body(
        doc,
        "团队名称为“智途”，口号为“智行无碍，途达万家”。团队工作覆盖嵌入式固件、硬件集成、云端服务、人工智能模型和"
        "Android无障碍客户端。本文档不填写学校、教师或学生姓名，提交前由队长依据竞赛报名信息补充封面对应字段。",
    )

    # References
    doc.add_page_break()
    add_heading(doc, "参考文献", 1)
    refs = [
        "[1] 乐鑫信息科技. 乐鑫命题—2026年全国大学生物联网设计竞赛[EB/OL]. https://www.espressif.com/zh-hans/ecosystem/education/competition/iot, 2026-07-22.",
        "[2] Espressif Systems. ESP32-S3 Series Datasheet[EB/OL]. https://www.espressif.com/sites/default/files/documentation/esp32-s3_datasheet_en.pdf, 2026-07-22.",
        "[3] Espressif Systems. ESP-IDF Programming Guide: ESP32-S3[EB/OL]. https://docs.espressif.com/projects/esp-idf/en/latest/esp32s3/, 2026-07-22.",
        "[4] Espressif Systems. Arduino-ESP32 Documentation[EB/OL]. https://docs.espressif.com/projects/arduino-esp32/en/latest/, 2026-07-22.",
        "[5] Ultralytics. YOLOv5[CP/OL]. https://github.com/ultralytics/yolov5, 2026-07-22.",
        "[6] Django Software Foundation. Django 4.1 Documentation[EB/OL]. https://docs.djangoproject.com/en/4.1/, 2026-07-22.",
        "[7] Google. Make apps more accessible[EB/OL]. https://developer.android.com/guide/topics/ui/accessibility/apps, 2026-07-22.",
        "[8] DeepSeek. DeepSeek API Docs[EB/OL]. https://api-docs.deepseek.com/, 2026-07-22.",
    ]
    for ref in refs:
        p = doc.add_paragraph()
        p.paragraph_format.left_indent = Cm(0.74)
        p.paragraph_format.first_line_indent = Cm(-0.74)
        p.paragraph_format.line_spacing = 1.3
        p.paragraph_format.space_after = Pt(6)
        set_font(p.add_run(ref), "宋体", Pt(9.5))

    # Header/footer and document defaults
    for sec in doc.sections:
        sec.header_distance = Cm(1.2)
        sec.footer_distance = Cm(1.2)
        header_p = sec.header.paragraphs[0]
        header_p.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        set_font(header_p.add_run("智途 · 作品设计文档"), "宋体", Pt(8.5), False, MUTED)
        add_page_number(sec.footer.paragraphs[0])

    settings = doc.settings.element
    update_fields = settings.find(qn("w:updateFields"))
    if update_fields is None:
        update_fields = OxmlElement("w:updateFields")
        settings.append(update_fields)
    update_fields.set(qn("w:val"), "true")

    doc.save(OUTPUT)
    print(OUTPUT)


if __name__ == "__main__":
    build()
