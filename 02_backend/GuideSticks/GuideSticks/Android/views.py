import base64
import json
import logging
import os
from datetime import datetime
from io import BytesIO
from threading import Timer

from PIL import Image
from django.contrib.auth import authenticate
from django.http import JsonResponse
from django.shortcuts import render
from rest_framework import status
from rest_framework.permissions import AllowAny
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework_simplejwt.tokens import RefreshToken

from Android.yolov5.inference import predict
from .assistant_serializers import AssistantChatRequestSerializer, SensorContextSerializer
from .deepseek_client import DeepSeekError, guidance_about_scene, local_guidance
from .device_state import get_status as get_device_status
from .device_state import record_heartbeat
from .models import User
from .scene_state import record_scene
from .sensor_context import build_fused_scene, record_phone_context
from .serializers import UserLoginSerializer, UserRegisterSerializer, UserSerializer
from .travel_assistant import answer_travel_question

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
log_dir = os.path.join(PROJECT_ROOT, "logs")
os.makedirs(log_dir, exist_ok=True)

log_file = os.path.join(log_dir, "traffic_light.log")
logging.basicConfig(
    filename=log_file,
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
)

if not any(
    isinstance(h, logging.StreamHandler) for h in logging.getLogger("").handlers
):
    console_handler = logging.StreamHandler()
    console_handler.setLevel(logging.INFO)
    console_handler.setFormatter(
        logging.Formatter("%(asctime)s - %(levelname)s - %(message)s")
    )
    logging.getLogger("").addHandler(console_handler)

logging.info("Logging initialized")

isCall = 0
isFindStick = 0


class AssistantChatView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = AssistantChatRequestSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        data = serializer.validated_data
        result = answer_travel_question(data["question"], data["history"])
        return Response(result, status=status.HTTP_200_OK)


class SensorContextView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = SensorContextSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        context = record_phone_context(**serializer.validated_data)
        return Response(
            {"status": "accepted", "context": context},
            status=status.HTTP_200_OK,
        )


class Base64(APIView):
    permission_classes = [AllowAny]

    def get(self, request):
        image_path = os.path.join(PROJECT_ROOT, "Android", "red_or_green", "red.jpg")
        with open(image_path, "rb") as img_file:
            return Response(base64.b64encode(img_file.read()))


class Identify(APIView):
    permission_classes = [AllowAny]

    def get(self, request):
        global isCall
        if isCall:
            isCall = 0
            return Response(1)
        return Response(0)

    def post(self, request):
        try:
            base64_str = request.data["image"]
            base64_str = (
                base64_str.replace("data:image/jpeg;base64,", "")
                .replace("data:image/png;base64,", "")
                .replace("data:image/jpg;base64,", "")
            )
            image_data = base64.b64decode(base64_str)
            image = Image.open(BytesIO(image_data))
            logging.info(f"Received image: {image}")

            results = predict(image)
            image.close()
            logging.info(f"Prediction raw result: {results}")

            results_dict = json.loads(results)
            if "error" in results_dict:
                logging.error(f"Model inference failed: {results_dict}")
                return JsonResponse(
                    {
                        "error": results_dict.get("error", "Prediction failed"),
                        "type": results_dict.get("type", "PredictionError"),
                    },
                    status=500,
                )

            if results_dict.get("Red Light", 0) > 0:
                light = "red"
            elif results_dict.get("Green Light", 0) > 0:
                light = "green"
            else:
                light = "no"

            fused_scene = build_fused_scene(
                {"light": light, "age_seconds": 0, "fresh": True}
            )
            try:
                guidance = guidance_about_scene(fused_scene)
            except DeepSeekError:
                guidance = local_guidance(light)
                guidance["source"] = "local_fallback"
            record_scene(
                light=light,
                command=guidance["command"],
                message=guidance["message"],
                source=guidance["source"],
            )

            return JsonResponse(
                {
                    "light": light,
                    "command": guidance["command"],
                    "message": guidance["message"],
                    "source": guidance["source"],
                }
            )
        except Exception as e:
            logging.error(f"Identify endpoint failed: {e}")
            return JsonResponse({"error": str(e)}, status=500)


class EmergencyCall(APIView):
    permission_classes = [AllowAny]

    def get(self, request):
        global isCall
        isCall = 1

        def reset_is_call():
            global isCall
            isCall = 0

        reset_timer = Timer(300, reset_is_call)
        reset_timer.daemon = True
        reset_timer.start()

        return Response("ok")


def test_page(request):
    context = {"current_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
    return render(request, "test.html", context)


class ReceiveData(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        try:
            data_str = json.dumps(request.data, ensure_ascii=False)
            with open(log_file, "a", encoding="utf-8") as f:
                f.write(f"POST data: {data_str}\n")
            logging.info(f"ReceiveData wrote payload: {data_str}")
            return JsonResponse({"status": "success", "msg": "written"})
        except Exception as e:
            logging.error(f"ReceiveData failed: {e}")
            return JsonResponse({"status": "error", "msg": str(e)}, status=500)


class RegisterView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = UserRegisterSerializer(data=request.data)
        if serializer.is_valid():
            user = serializer.save()
            return Response(
                {"message": "register success", "user": UserSerializer(user).data},
                status=status.HTTP_201_CREATED,
            )
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


class LoginView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = UserLoginSerializer(data=request.data)
        if serializer.is_valid():
            user = authenticate(
                username=serializer.validated_data["username"],
                password=serializer.validated_data["password"],
            )
            if user:
                refresh = RefreshToken.for_user(user)
                return Response(
                    {
                        "message": "login success",
                        "token": str(refresh.access_token),
                        "user": UserSerializer(user).data,
                    }
                )
            return Response(
                {"message": "invalid username or password"},
                status=status.HTTP_401_UNAUTHORIZED,
            )
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


class IsCallView(APIView):
    permission_classes = [AllowAny]

    def get(self, request):
        global isCall
        should_call = isCall
        if should_call:
            isCall = 0
        return JsonResponse({"isCall": should_call})


class FindStickView(APIView):
    permission_classes = [AllowAny]

    def get(self, request):
        global isFindStick
        isFindStick = 1

        def reset_find_stick():
            global isFindStick
            isFindStick = 0

        reset_timer = Timer(30, reset_find_stick)
        reset_timer.daemon = True
        reset_timer.start()
        return Response(status=status.HTTP_200_OK)


class FindStickStatusView(APIView):
    permission_classes = [AllowAny]

    def get(self, request):
        global isFindStick
        record_heartbeat()
        if isFindStick:
            isFindStick = 0
            return Response(True)
        return Response(False)


class DeviceStatusView(APIView):
    permission_classes = [AllowAny]

    def get(self, request):
        return JsonResponse(get_device_status())
