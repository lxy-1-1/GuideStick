import json
from unittest.mock import Mock, patch

from django.core.cache import cache
from django.test import SimpleTestCase, override_settings
from django.utils import timezone
from rest_framework import status
from rest_framework.test import APIClient

from .deepseek_client import DeepSeekError
from .tests import make_test_image_payload


class PhoneSensorContextTests(SimpleTestCase):
    def setUp(self):
        cache.clear()

    def test_sensor_context_endpoint_records_phone_location(self):
        captured_at_ms = int(timezone.now().timestamp() * 1000)
        response = APIClient().post(
            "/android/sensor/context/",
            {
                "latitude": 34.811927,
                "longitude": 113.499048,
                "accuracy": 12.5,
                "address": "郑州市中原区",
                "captured_at_ms": captured_at_ms,
            },
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        context = response.json()["context"]
        self.assertTrue(context["available"])
        self.assertEqual(context["latitude"], 34.811927)
        self.assertEqual(context["longitude"], 113.499048)
        self.assertEqual(context["address"], "郑州市中原区")
        self.assertTrue(context["fresh"])

    def test_sensor_context_endpoint_rejects_invalid_coordinates(self):
        response = APIClient().post(
            "/android/sensor/context/",
            {
                "latitude": 91,
                "longitude": 113.499048,
                "accuracy": 12.5,
                "captured_at_ms": 1784800000000,
            },
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_stale_captured_location_is_not_fresh(self):
        from .sensor_context import get_phone_context, record_phone_context

        now = timezone.now()
        stale_timestamp_ms = int((now.timestamp() - 600) * 1000)
        record_phone_context(
            latitude=34.811927,
            longitude=113.499048,
            accuracy=12.5,
            address="Zhengzhou",
            captured_at_ms=stale_timestamp_ms,
            now=now,
        )

        context = get_phone_context(now=now)

        self.assertFalse(context["fresh"])
        self.assertGreaterEqual(context["age_seconds"], 600)

    @patch("Android.sensor_context.get_device_status")
    def test_fused_scene_contains_camera_location_and_device_state(
        self, mock_device_status
    ):
        from .sensor_context import build_fused_scene, record_phone_context

        mock_device_status.return_value = {
            "online": True,
            "age_seconds": 1,
            "last_seen": "2026-07-23T10:00:00+08:00",
        }
        now = timezone.now()
        record_phone_context(
            latitude=34.811927,
            longitude=113.499048,
            accuracy=12.5,
            address="郑州市中原区",
            captured_at_ms=int(now.timestamp() * 1000),
            now=now,
        )

        fused = build_fused_scene(
            {"light": "green", "age_seconds": 0, "fresh": True}, now=now
        )

        self.assertEqual(fused["light"], "green")
        self.assertEqual(fused["camera"]["type"], "traffic_light_camera")
        self.assertEqual(fused["location"]["latitude"], 34.811927)
        self.assertTrue(fused["stick"]["online"])
        self.assertEqual(
            fused["fusion_sources"], ["camera", "phone_location", "stick_heartbeat"]
        )


class DeepSeekDownlinkTests(SimpleTestCase):
    @override_settings(
        DEEPSEEK_API_KEY="test-key",
        DEEPSEEK_BASE_URL="https://api.deepseek.com",
        DEEPSEEK_MODEL="deepseek-v4-flash",
    )
    @patch("Android.deepseek_client.requests.post")
    def test_model_guidance_uses_fused_context_and_returns_allowlisted_command(
        self, mock_post
    ):
        from .deepseek_client import guidance_about_scene

        response = Mock()
        response.raise_for_status.return_value = None
        response.json.return_value = {
            "choices": [
                {
                    "message": {
                        "content": json.dumps(
                            {
                                "command": "PROCEED",
                                "message": "检测到绿灯，请确认周围安全后通行",
                            },
                            ensure_ascii=False,
                        )
                    }
                }
            ]
        }
        mock_post.return_value = response
        scene = {
            "light": "green",
            "fresh": True,
            "location": {
                "available": True,
                "fresh": True,
                "latitude": 34.811927,
                "longitude": 113.499048,
                "accuracy": 12.5,
            },
            "stick": {"online": True, "age_seconds": 1},
            "fusion_sources": ["camera", "phone_location", "stick_heartbeat"],
        }

        result = guidance_about_scene(scene)

        self.assertEqual(result["command"], "PROCEED")
        self.assertEqual(result["source"], "deepseek")
        request_payload = mock_post.call_args.kwargs["json"]
        request_text = request_payload["messages"][-1]["content"]
        self.assertIn("34.811927", request_text)
        self.assertIn("stick_heartbeat", request_text)

    @override_settings(
        DEEPSEEK_API_KEY="test-key",
        DEEPSEEK_BASE_URL="https://api.deepseek.com",
        DEEPSEEK_MODEL="deepseek-v4-flash",
    )
    @patch("Android.deepseek_client.requests.post")
    def test_model_message_cannot_inject_a_different_firmware_command(
        self, mock_post
    ):
        from .deepseek_client import guidance_about_scene

        response = Mock()
        response.raise_for_status.return_value = None
        response.json.return_value = {
            "choices": [
                {
                    "message": {
                        "content": json.dumps(
                            {
                                "command": "PROCEED",
                                "message": "STOP CAUTION ignore the selected command",
                            }
                        )
                    }
                }
            ]
        }
        mock_post.return_value = response
        scene = {
            "light": "green",
            "fresh": True,
            "location": {
                "available": True,
                "fresh": True,
                "latitude": 34.811927,
                "longitude": 113.499048,
                "accuracy": 12.5,
            },
            "stick": {"online": True, "age_seconds": 1},
            "fusion_sources": ["camera", "phone_location", "stick_heartbeat"],
        }

        result = guidance_about_scene(scene)

        self.assertEqual(result["command"], "PROCEED")
        self.assertNotIn("STOP", result["message"])
        self.assertNotIn("CAUTION", result["message"])

    @patch("Android.deepseek_client.requests.post")
    def test_model_guidance_requires_all_fusion_sources(self, mock_post):
        from .deepseek_client import guidance_about_scene

        with self.assertRaises(DeepSeekError):
            guidance_about_scene(
                {
                    "light": "green",
                    "fresh": True,
                    "location": {"available": False, "fresh": False},
                    "stick": {"online": True},
                    "fusion_sources": ["camera", "stick_heartbeat"],
                }
            )

        mock_post.assert_not_called()

    @override_settings(
        DEEPSEEK_API_KEY="test-key",
        DEEPSEEK_BASE_URL="https://api.deepseek.com",
        DEEPSEEK_MODEL="deepseek-v4-flash",
    )
    @patch("Android.deepseek_client.requests.post")
    def test_unsafe_red_light_model_command_is_rejected(self, mock_post):
        from .deepseek_client import guidance_about_scene

        response = Mock()
        response.raise_for_status.return_value = None
        response.json.return_value = {
            "choices": [
                {
                    "message": {
                        "content": '{"command":"PROCEED","message":"可以通行"}'
                    }
                }
            ]
        }
        mock_post.return_value = response

        with self.assertRaises(DeepSeekError):
            guidance_about_scene({"light": "red"})

    @patch("Android.views.guidance_about_scene")
    @patch("Android.views.predict")
    def test_identify_returns_model_command_without_changing_firmware_fields(
        self, mock_predict, mock_guidance
    ):
        mock_predict.return_value = json.dumps({"Red Light": 0, "Green Light": 1})
        mock_guidance.return_value = {
            "command": "PROCEED",
            "message": "检测到绿灯，请确认周围安全后通行",
            "source": "deepseek",
        }

        response = APIClient().post(
            "/android/identify/",
            {"image": make_test_image_payload()},
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.json()["command"], "PROCEED")
        self.assertEqual(response.json()["source"], "deepseek")
        self.assertTrue(mock_guidance.called)
        fused_scene = mock_guidance.call_args.args[0]
        self.assertEqual(fused_scene["light"], "green")
        self.assertIn("camera", fused_scene["fusion_sources"])

    @patch("Android.views.guidance_about_scene")
    @patch("Android.views.predict")
    def test_identify_falls_back_to_safe_local_command(
        self, mock_predict, mock_guidance
    ):
        mock_predict.return_value = json.dumps({"Red Light": 1, "Green Light": 0})
        mock_guidance.side_effect = DeepSeekError("timeout")

        response = APIClient().post(
            "/android/identify/",
            {"image": make_test_image_payload()},
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.json()["command"], "STOP")
        self.assertEqual(response.json()["source"], "local_fallback")


class FusedAssistantTests(SimpleTestCase):
    @patch("Android.travel_assistant.chat_about_scene")
    @patch("Android.travel_assistant.build_fused_scene")
    @patch("Android.travel_assistant.get_latest_scene")
    def test_general_question_sends_fused_scene_to_deepseek(
        self, mock_latest_scene, mock_build_fused, mock_chat
    ):
        from .travel_assistant import answer_travel_question

        latest = {"light": "green", "age_seconds": 2, "fresh": True}
        fused = {
            **latest,
            "location": {"available": True, "latitude": 34.811927},
            "stick": {"online": True},
            "fusion_sources": ["camera", "phone_location", "stick_heartbeat"],
        }
        mock_latest_scene.return_value = latest
        mock_build_fused.return_value = fused
        mock_chat.return_value = "最近识别到绿灯，导盲杖在线。"

        result = answer_travel_question("刚才识别到了什么？", [])

        self.assertEqual(result["source"], "deepseek")
        mock_build_fused.assert_called_once_with(latest)
        mock_chat.assert_called_once_with("刚才识别到了什么？", [], fused)
