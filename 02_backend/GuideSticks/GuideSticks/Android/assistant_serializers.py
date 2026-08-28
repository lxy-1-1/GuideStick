from rest_framework import serializers


class AssistantChatRequestSerializer(serializers.Serializer):
    question = serializers.CharField(max_length=200, trim_whitespace=True)
    history = serializers.ListField(required=False, default=list)

    def validate_history(self, value):
        cleaned = []
        for item in value:
            if not isinstance(item, dict):
                continue
            role = item.get("role")
            content = item.get("content")
            if role not in {"user", "assistant"} or not isinstance(content, str):
                continue
            content = content.strip()[:500]
            if content:
                cleaned.append({"role": role, "content": content})
        return cleaned[-6:]


class SensorContextSerializer(serializers.Serializer):
    latitude = serializers.FloatField(min_value=-90, max_value=90)
    longitude = serializers.FloatField(min_value=-180, max_value=180)
    accuracy = serializers.FloatField(min_value=0, max_value=10000)
    address = serializers.CharField(
        required=False,
        allow_blank=True,
        default="",
        max_length=300,
        trim_whitespace=True,
    )
    captured_at_ms = serializers.IntegerField(min_value=0)
