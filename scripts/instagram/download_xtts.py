import os
from TTS.api import TTS

os.environ["COQUI_TOS_AGREED"] = "1"
tts = TTS(model_name="tts_models/multilingual/multi-dataset/xtts_v2", progress_bar=False).to("cpu")
