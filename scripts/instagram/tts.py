from TTS.api import TTS


 

# Init TTS with the target model name
tts = TTS(model_name="tts_models/fr/mai/tacotron2-DDC", progress_bar=False).to("cpu")

# Run TTS
tts.tts_to_file(text="Dans ce beau château, Candide, élevé dans un esprit de parfaite harmonie, apprenait que tout était pour le mieux dans le meilleur des mondes possibles. Toutefois, un jour, il fut banni de ce paradis terrestre, initiant un voyage semé de péripéties. Au fur et à mesure de ses aventures, Candide découvrait les affres de l'oppression et de la guerre, se confrontant à la dure réalité de la condition humaine. En traversant les frontières invisibles qui séparent les hommes, Candide comprit le pouvoir du collectif pour changer le monde. Chaque expérience vécue était une étape vers la compréhension, illuminée par l'espoir et la croyance dans une humanité transcendant les barrières et les diktats. Il pressentait que l'avenir pourrait être meilleur si chacun agissait avec sagesse et bienveillance.", file_path="output1.wav")

# Init TTS with the target model name
tts = TTS(model_name="tts_models/fr/css10/vits", progress_bar=False).to("cpu")

# Run TTS
tts.tts_to_file(text="Dans ce beau château, Candide, élevé dans un esprit de parfaite harmonie, apprenait que tout était pour le mieux dans le meilleur des mondes possibles. Toutefois, un jour, il fut banni de ce paradis terrestre, initiant un voyage semé de péripéties. Au fur et à mesure de ses aventures, Candide découvrait les affres de l'oppression et de la guerre, se confrontant à la dure réalité de la condition humaine. En traversant les frontières invisibles qui séparent les hommes, Candide comprit le pouvoir du collectif pour changer le monde. Chaque expérience vécue était une étape vers la compréhension, illuminée par l'espoir et la croyance dans une humanité transcendant les barrières et les diktats. Il pressentait que l'avenir pourrait être meilleur si chacun agissait avec sagesse et bienveillance.", file_path="output2.wav")




# Example voice cloning with YourTTS in English, French and Portuguese
#tts = TTS(model_name="tts_models/multilingual/multi-dataset/your_tts", progress_bar=False).to("cpu")
#tts.tts_to_file("This is voice cloning.", speaker_wav="my/cloning/audio.wav", language="en", file_path="output2.wav")
#tts.tts_to_file("C'est le clonage de la voix.", speaker_wav="my/cloning/audio.wav", language="fr-fr", file_path="output3.wav")
#tts.tts_to_file("Isso é clonagem de voz.", speaker_wav="my/cloning/audio.wav", language="pt-br", file_path="output4.wav")
