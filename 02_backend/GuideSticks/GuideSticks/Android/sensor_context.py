from collections.abc import Mapping

from django.core.cache import cache
from django.utils import timezone

from .device_state import get_status as get_device_status


PHONE_CONTEXT_CACHE_KEY = "guidestick:phone_sensor_context"
PHONE_CONTEXT_FRESH_SECONDS = 300
PHONE_CONTEXT_MAX_FUTURE_SECONDS = 30
VALID_LIGHTS = {"red", "green", "no"}


def record_phone_context(
    latitude,
    longitude,
    accuracy,
    address,
    captured_at_ms,
    now=None,
):
    received_at = now or timezone.now()
    state = {
        "available": True,
        "latitude": round(float(latitude), 6),
        "longitude": round(float(longitude), 6),
        "accuracy": round(float(accuracy), 1),
        "address": (address or "").strip()[:300],
        "captured_at_ms": int(captured_at_ms),
        "received_at": received_at,
    }
    cache.set(PHONE_CONTEXT_CACHE_KEY, state, timeout=None)
    return _phone_context_payload(state, received_at)


def get_phone_context(now=None):
    state = cache.get(PHONE_CONTEXT_CACHE_KEY)
    if not state:
        return {"available": False, "fresh": False, "age_seconds": None}
    return _phone_context_payload(state, now or timezone.now())


def _phone_context_payload(state, now):
    received_age_seconds = max(0, (now - state["received_at"]).total_seconds())
    captured_age_seconds = now.timestamp() - (state["captured_at_ms"] / 1000)
    age_seconds = max(0, int(captured_age_seconds))
    captured_time_valid = (
        -PHONE_CONTEXT_MAX_FUTURE_SECONDS
        <= captured_age_seconds
        <= PHONE_CONTEXT_FRESH_SECONDS
    )
    return {
        key: value
        for key, value in state.items()
        if key != "received_at"
    } | {
        "fresh": (
            received_age_seconds <= PHONE_CONTEXT_FRESH_SECONDS
            and captured_time_valid
        ),
        "age_seconds": age_seconds,
    }


def build_fused_scene(scene, now=None):
    current_time = now or timezone.now()
    source_scene = scene if isinstance(scene, Mapping) else {}
    light = source_scene.get("light")
    if light not in VALID_LIGHTS:
        light = None

    age_seconds = source_scene.get("age_seconds")
    if not isinstance(age_seconds, int) or isinstance(age_seconds, bool):
        age_seconds = None
    fresh = source_scene.get("fresh") is True and light is not None

    location = get_phone_context(now=current_time)
    device = get_device_status(now=current_time)
    stick = {
        "online": bool(device.get("online")),
        "age_seconds": device.get("age_seconds"),
        "last_seen": device.get("last_seen"),
    }

    fusion_sources = []
    if light is not None:
        fusion_sources.append("camera")
    if location.get("available") and location.get("fresh"):
        fusion_sources.append("phone_location")
    if stick["online"]:
        fusion_sources.append("stick_heartbeat")

    fusion_ready = (
        fresh
        and location.get("available") is True
        and location.get("fresh") is True
        and stick["online"]
    )
    return {
        "light": light,
        "age_seconds": age_seconds,
        "fresh": fresh,
        "camera": {
            "type": "traffic_light_camera",
            "light": light,
            "fresh": fresh,
            "age_seconds": age_seconds,
        },
        "location": location,
        "stick": stick,
        "fusion_sources": fusion_sources,
        "fusion_ready": fusion_ready,
    }
