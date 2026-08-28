from django.urls import path
from . import views

urlpatterns = [
    path("assistant/chat/", views.AssistantChatView.as_view(), name="assistant_chat"),
    path(
        "sensor/context/",
        views.SensorContextView.as_view(),
        name="sensor_context",
    ),
    path("login/", views.LoginView.as_view(), name="login"),
    path("register/", views.RegisterView.as_view(), name="register"),
    path("identify/", views.Identify.as_view(), name="identify"),
    path("iscall/", views.IsCallView.as_view(), name="iscall"),
    path("emergency/", views.EmergencyCall.as_view(), name="emergency"),
    path("find_stick/", views.FindStickView.as_view(), name="find_stick"),
    path(
        "find_stick_status/",
        views.FindStickStatusView.as_view(),
        name="find_stick_status",
    ),
    path("device/status/", views.DeviceStatusView.as_view(), name="device_status"),
    path("receive/", views.ReceiveData.as_view(), name="receive"),
    path("base64/", views.Base64.as_view(), name="base64"),
    path("test/", views.test_page, name="test_page"),
]
