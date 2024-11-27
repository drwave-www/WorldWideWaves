from TTS.api import TTS

tts = TTS(model_name="tts_models/multilingual/multi-dataset/xtts_v2", progress_bar=False).to("cpu")
tts.tts_to_file("World, Wide, Waves.", speaker="Kumar Dahl", language="en", file_path="template/VIDEO/intro-www.wav")
