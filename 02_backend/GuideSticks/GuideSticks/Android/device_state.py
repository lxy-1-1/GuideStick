from threading import Lock

from django.utils import timezone


ONLINE_TIMEOUT_SECONDS = 5

_lock = Lock()
_last_seen = None


def record_heartbeat(now=None):
    global _last_seen
    heartbeat_time = now or timezone.now()
    with _lock:
        _last_seen = heartbeat_time
    return heartbeat_time


def get_status(now=None):
    current_time = now or timezone.now()
    with _lock:
        last_seen = _last_seen

    if last_seen is None:
        return {
            "online": False,
            "last_seen": None,
            "last_seen_epoch_ms": None,
            "age_seconds": None,
            "timeout_seconds": ONLINE_TIMEOUT_SECONDS,
        }

    age_seconds = max(0, int((current_time - last_seen).total_seconds()))
    return {
        "online": age_seconds <= ONLINE_TIMEOUT_SECONDS,
        "last_seen": last_seen.isoformat(),
        "last_seen_epoch_ms": int(last_seen.timestamp() * 1000),
        "age_seconds": age_seconds,
        "timeout_seconds": ONLINE_TIMEOUT_SECONDS,
    }
