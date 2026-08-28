import json
import os
import pathlib
import platform
from io import BytesIO

import cv2 as cv
import numpy as np
import requests
import torch
from PIL import Image

if platform.system() != "Windows":
    # Compatibility for models serialized on Windows and loaded on Linux.
    pathlib.WindowsPath = pathlib.PosixPath

CURRENT_DIR = pathlib.Path(__file__).resolve().parent
MODEL_PATH = CURRENT_DIR / "runs" / "train" / "exp" / "weights" / "best.pt"
_MODEL = None


def load_model():
    global _MODEL
    if _MODEL is not None:
        return _MODEL

    if not MODEL_PATH.exists():
        raise FileNotFoundError(f"Model file not found: {MODEL_PATH}")

    original_cwd = os.getcwd()
    try:
        # torch.hub.load('.', ...) expects cwd at the YOLOv5 project root.
        os.chdir(str(CURRENT_DIR))
        _MODEL = torch.hub.load(
            ".",
            "custom",
            path=str(MODEL_PATH),
            source="local",
        )
        return _MODEL
    finally:
        os.chdir(original_cwd)


def predict(image_input):
    """
    Returns JSON string like:
    {
        "Red Light": 1,
        "Green Light": 2
    }
    """
    try:
        model = load_model()
        print("Model class names:", model.names)

        if isinstance(image_input, str):
            if image_input.startswith(("http://", "https://")):
                response = requests.get(image_input)
                img = Image.open(BytesIO(response.content))
            else:
                img = Image.open(image_input)
        elif isinstance(image_input, Image.Image):
            img = image_input
        elif isinstance(image_input, np.ndarray):
            img = Image.fromarray(cv.cvtColor(image_input, cv.COLOR_BGR2RGB))
        else:
            raise ValueError("Unsupported input type")

        results = model(img)

        detection_counts = {
            "Red Light": 0,
            "Green Light": 0,
        }

        for det in results.xyxy[0]:
            cls = int(det[5])
            if cls == 2:
                detection_counts["Red Light"] += 1
            elif cls == 0:
                detection_counts["Green Light"] += 1

        return json.dumps(detection_counts, indent=2)
    except Exception as e:
        error = {
            "error": str(e),
            "type": type(e).__name__,
        }
        return json.dumps(error, indent=2)


if __name__ == "__main__":
    print(predict("1"))
