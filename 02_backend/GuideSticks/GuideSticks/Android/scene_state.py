from django.core.cache import cache
from django.utils import timezone

SCENE_CACHE_KEY = "guidestick:latest_scene"
SCENE_FRESH_SECONDS = 30


def record_scene(light, command, message, source, now=None):
    captured_at = now or timezone.now()
    state = {
        "light": light,
        "command": command,
        "message": message,
        "source": source,
        "captured_at": captured_at,
    }
    cache.set(SCENE_CACHE_KEY, state, timeout=None)
    return dict(state)


def get_latest_scene(now=None):
    state = cache.get(SCENE_CACHE_KEY)
    if not state:
        return None
    current_time = now or timezone.now()
    elapsed_seconds = max(0, (current_time - state["captured_at"]).total_seconds())
    age_seconds = int(elapsed_seconds)
    return {
        **state,
        "age_seconds": age_seconds,
        "fresh": elapsed_seconds <= SCENE_FRESH_SECONDS,
    }
