import json

import requests
from django.conf import settings


class DeepSeekError(RuntimeError):
    pass


LOCAL_GUIDANCE = {
    "red": {"command": "STOP", "message": "检测到红灯，请停止通行"},
    "green": {"command": "PROCEED", "message": "检测到绿灯，确认安全后通行"},
    "no": {"command": "CAUTION", "message": "未识别到交通灯，请注意周围环境"},
}

ALLOWED_COMMANDS_BY_LIGHT = {
    "red": {"STOP"},
    "green": {"PROCEED", "CAUTION"},
    "no": {"CAUTION"},
}
REQUIRED_FUSION_SOURCES = {"camera", "phone_location", "stick_heartbeat"}
SAFE_GUIDANCE_MESSAGES = {
    "STOP": "检测到红灯，请停止通行",
    "PROCEED": "检测到绿灯，请确认车辆、路面和周围声音后再通行",
    "CAUTION": "环境信息不确定，请减速并注意周围情况",
}

TRAVEL_ASSISTANT_PROMPT = """
你是面向视障人士的出行场景解释助手。你只能依据输入中的结构化场景信息回答，不能声称看到了未提供的车辆、路面或障碍物。回答使用简短、明确的中文，不超过120个汉字。绿灯只代表交通灯状态，不能据此保证可以安全通行。只输出JSON：{"answer":"回答内容"}。
""".strip()

GUIDANCE_PROMPT = """
你是智能导盲设备的出行场景决策助手。输入是由摄像头红绿灯状态、手机定位和导盲杖在线状态组成的结构化感知信息。只能输出JSON：
{"command":"STOP|PROCEED|CAUTION","message":"不超过60个汉字的中文提示"}。
规则：红灯只能STOP；未识别到交通灯只能CAUTION；绿灯可PROCEED或CAUTION，但必须提醒确认车辆、路面和周围声音后再行动。不得输出其他指令，不得声称绝对安全。
""".strip()


def local_guidance(light):
    try:
        return dict(LOCAL_GUIDANCE[light])
    except KeyError as exc:
        raise ValueError(f"Unsupported traffic-light value: {light}") from exc


def _request_json(messages, max_tokens, timeout):
    api_key = settings.DEEPSEEK_API_KEY
    if not api_key:
        raise DeepSeekError("DEEPSEEK_API_KEY is not configured")

    payload = {
        "model": settings.DEEPSEEK_MODEL,
        "messages": messages,
        "thinking": {"type": "disabled"},
        "response_format": {"type": "json_object"},
        "max_tokens": max_tokens,
        "stream": False,
    }
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json",
    }
    try:
        response = requests.post(
            f"{settings.DEEPSEEK_BASE_URL.rstrip('/')}/chat/completions",
            headers=headers,
            json=payload,
            timeout=timeout,
        )
        response.raise_for_status()
        content = response.json()["choices"][0]["message"]["content"]
        result = json.loads(content)
        if not isinstance(result, dict):
            raise ValueError("DeepSeek answer is not a JSON object")
        return result
    except (
        requests.RequestException,
        KeyError,
        IndexError,
        TypeError,
        ValueError,
    ) as exc:
        raise DeepSeekError("DeepSeek request failed") from exc


def _cloud_scene(scene):
    location = scene.get("location")
    if not isinstance(location, dict):
        location = {"available": False}
    elif location.get("available"):
        location = {
            "available": True,
            "fresh": location.get("fresh") is True,
            "latitude": round(float(location.get("latitude")), 6),
            "longitude": round(float(location.get("longitude")), 6),
            "accuracy": round(float(location.get("accuracy")), 1),
            "address": str(location.get("address") or "")[:120],
            "age_seconds": location.get("age_seconds"),
        }
    else:
        location = {"available": False, "fresh": False}

    stick = scene.get("stick")
    if not isinstance(stick, dict):
        stick = {"online": False}
    else:
        stick = {
            "online": bool(stick.get("online")),
            "age_seconds": stick.get("age_seconds"),
        }

    return {
        "traffic_light": scene.get("light"),
        "phone_location": location,
        "stick": stick,
        "fusion_sources": scene.get("fusion_sources", []),
    }


def guidance_about_scene(scene):
    if not isinstance(scene, dict):
        raise DeepSeekError("Fused scene is missing")
    light = scene.get("light")
    allowed_commands = ALLOWED_COMMANDS_BY_LIGHT.get(light)
    if not allowed_commands:
        raise DeepSeekError("Traffic-light state is invalid")
    sources = set(scene.get("fusion_sources") or [])
    location = scene.get("location") or {}
    stick = scene.get("stick") or {}
    if (
        scene.get("fresh") is not True
        or not REQUIRED_FUSION_SOURCES.issubset(sources)
        or location.get("available") is not True
        or location.get("fresh") is not True
        or stick.get("online") is not True
    ):
        raise DeepSeekError("Required fused sensor context is unavailable")

    result = _request_json(
        [
            {"role": "system", "content": GUIDANCE_PROMPT},
            {
                "role": "user",
                "content": json.dumps(_cloud_scene(scene), ensure_ascii=False),
            },
        ],
        max_tokens=120,
        timeout=6,
    )
    command = result.get("command")
    if command not in allowed_commands:
        raise DeepSeekError("DeepSeek command failed the safety policy")
    return {
        "command": command,
        # The old firmware scans the whole response for command tokens.
        # Never forward model-authored text that could contain another token.
        "message": SAFE_GUIDANCE_MESSAGES[command],
        "source": "deepseek",
    }


def chat_about_scene(question: str, history: list[dict], scene: dict | None) -> str:
    messages = [{"role": "system", "content": TRAVEL_ASSISTANT_PROMPT}]
    messages.extend(history)
    messages.append(
        {
            "role": "user",
            "content": json.dumps(
                {"question": question, "latest_scene": scene},
                ensure_ascii=False,
            ),
        }
    )
    answer_payload = _request_json(messages, max_tokens=180, timeout=15)
    answer = answer_payload.get("answer")
    if not isinstance(answer, str):
        raise DeepSeekError("DeepSeek answer is not a string")
    answer = answer.strip()

    if not answer:
        raise DeepSeekError("DeepSeek returned an empty answer")
    return answer[:120]
