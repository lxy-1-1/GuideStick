from collections.abc import Mapping

from .deepseek_client import DeepSeekError, chat_about_scene
from .scene_state import get_latest_scene
from .sensor_context import build_fused_scene

SAFETY_PHRASES = ("能不能走", "可以走吗", "能走吗", "能过马路吗", "可以过吗", "安全吗")
PASSAGE_TERMS = ("过马路", "过街", "横穿", "通行", "通过", "过路")
PERMISSION_TERMS = ("能", "可以", "可否", "是否")
CROSSING_ACTION_TERMS = ("过", "走", "通行", "通过", "横穿", "过街")
HELP_PHRASES = ("救命", "紧急", "联系家人", "帮我求助", "呼叫联系人")
FAMILY_TERMS = (
    "家人",
    "家里人",
    "家里",
    "家属",
    "亲人",
    "妈妈",
    "爸爸",
    "父母",
    "儿子",
    "女儿",
)
CONTACT_ACTION_TERMS = ("联系", "拨打", "打给", "呼叫", "叫", "找")
GREEN_BOUNDARY = "当前仅确认交通灯状态，尚未确认车辆和路面，请确认周围安全后通行。"
GREEN_SAFETY_ANSWER = f"检测到绿灯。{GREEN_BOUNDARY}"

HELP_ANSWER = "请使用首页的紧急呼叫功能联系预设联系人。"
MISSING_SCENE_ANSWER = "当前没有有效的环境信息，请重新识别后再判断。"
RED_LIGHT_ANSWER = "检测到红灯，请停止通行。"
NO_LIGHT_ANSWER = "未识别到交通灯，无法判断是否可以通行。"
UNKNOWN_LIGHT_ANSWER = "当前交通灯状态未知，无法判断是否可以通行，请重新识别后再判断。"
DEEPSEEK_FALLBACK_ANSWER = "暂时无法连接出行助手，请稍后重试。"


def normalize_history(value):
    if not isinstance(value, list):
        return []

    cleaned = []
    for item in value:
        if not isinstance(item, dict) or item.get("role") not in {"user", "assistant"}:
            continue
        content = item.get("content")
        if not isinstance(content, str):
            continue
        content = content.strip()[:500]
        if content:
            cleaned.append({"role": item["role"], "content": content})
    return cleaned[-6:]


def _scene_summary(scene):
    if not isinstance(scene, Mapping):
        return None

    light = scene.get("light")
    age_seconds = scene.get("age_seconds")
    fresh = scene.get("fresh")
    if (
        not isinstance(light, str)
        or not isinstance(age_seconds, int)
        or isinstance(age_seconds, bool)
        or age_seconds < 0
        or not isinstance(fresh, bool)
    ):
        return None

    return {"light": light, "age_seconds": age_seconds, "fresh": fresh}


def _is_help_query(question):
    if any(phrase in question for phrase in HELP_PHRASES):
        return True
    has_family_target = any(term in question for term in FAMILY_TERMS)
    has_contact_action = any(term in question for term in CONTACT_ACTION_TERMS) or (
        "打" in question and "电话" in question
    )
    return has_family_target and has_contact_action


def _is_safety_query(question):
    if any(phrase in question for phrase in SAFETY_PHRASES) or "安全" in question:
        return True
    if any(term in question for term in PASSAGE_TERMS):
        return True
    return any(term in question for term in PERMISSION_TERMS) and any(
        term in question for term in CROSSING_ACTION_TERMS
    )


def _response(answer, intent, scene, source):
    return {"answer": answer, "intent": intent, "scene": scene, "source": source}


def _safety_answer(scene):
    if not scene or not scene["fresh"]:
        return MISSING_SCENE_ANSWER
    if scene["light"] == "red":
        return RED_LIGHT_ANSWER
    if scene["light"] == "no":
        return NO_LIGHT_ANSWER
    if scene["light"] == "green":
        return GREEN_SAFETY_ANSWER
    return UNKNOWN_LIGHT_ANSWER


def answer_travel_question(question: str, history: list[dict]) -> dict:
    question_text = question.strip() if isinstance(question, str) else ""
    normalized_question = "".join(question_text.split())
    scene_summary = _scene_summary(get_latest_scene())
    fused_scene = build_fused_scene(scene_summary)

    if _is_help_query(normalized_question):
        return _response(HELP_ANSWER, "HELP_QUERY", scene_summary, "rules")
    if _is_safety_query(normalized_question):
        return _response(
            _safety_answer(scene_summary), "SAFETY_QUERY", scene_summary, "rules"
        )

    try:
        answer = chat_about_scene(
            question_text, normalize_history(history), fused_scene
        )
        source = "deepseek"
    except DeepSeekError:
        answer = DEEPSEEK_FALLBACK_ANSWER
        source = "fallback"
    return _response(answer, "GENERAL", scene_summary, source)
