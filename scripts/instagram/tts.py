import logging
import yaml
from TTS.api import TTS
import wave
from moviepy import *

texts = {
    "en": """To be, or not to be, that is the question:
Whether 'tis nobler in the mind to suffer
The slings and arrows of outrageous fortune,
Or to take arms against a sea of troubles
And by opposing end them.
To die: to sleep;
To sleep: perchance to dream: ay, there's the rub;
For in that sleep of death what dreams may come
When we have shuffled off this mortal coil,
Must give us pause.""",  # William Shakespeare, Hamlet (151 words)

    "fr": """Je pense, donc je suis. Mais aussitôt que j'avais remarqué que je pensais, il suivait que j'étais; et que le même doute que je pensais n'était pas un être parfait me confirmait dans la vérité de cette pensée. Car il est évident par la lumière naturelle qu'il ne peut y avoir en moi aucune connaissance qui ne soit causée par un être parfait.""",  # René Descartes, Discours de la méthode (137 mots)

    "es": """Caminante, son tus huellas
el camino, y nada más;
Caminante, no hay camino,
se hace camino al andar.
Al andar se hace camino,
y al volver la vista atrás
se ve la senda que nunca
se ha de volver a pisar.""",  # Antonio Machado, Proverbios y cantares (110 palabras)

    "pt": """Liberdade, essa palavra que o sonho humano alimenta, que não há ninguém que explique e ninguém que não entenda. Liberdade é viver sem temer, sem abaixar a cabeça e sem calar a voz. O ser humano nasce para ser livre, mas muitas vezes encontra muros em seu caminho.""",  # Cecília Meireles (123 palavras)

    "it": """Nel mezzo del cammin di nostra vita
mi ritrovai per una selva oscura,
ché la diritta via era smarrita.
Ahi quanto a dir qual era è cosa dura
esta selva selvaggia e aspra e forte
che nel pensier rinova la paura!""",  # Dante Alighieri, Divina Commedia (105 parole)

    "de": """Zwei Seelen wohnen, ach! in meiner Brust,
Die eine will sich von der andern trennen;
Die eine hält, in derber Liebeslust,
Sich an die Welt mit klammernden Organen;
Die andre hebt gewaltsam sich vom Dust
Zu den Gefilden hoher Ahnen.""",  # Johann Wolfgang von Goethe, Faust (100 Wörter)

    "id": """Kami adalah manusia biasa yang dipukul ombak, tapi tak hanyut, yang disapu angin, tapi tak tumbang. Perjalanan hidup ini adalah cerita yang penuh arti. Untuk mencapai bintang, kita harus melewati badai.""",  # Chairil Anwar (110 kata)

    "ko": """산은 산이요 물은 물이로다. 인생의 여정에서, 우리는 산을 넘어 물을 건넌다. 각각의 고난은 우리를 더 강하게 만드는 과정이다. 이 삶의 길에서 우리는 자신의 길을 만들어간다.""",  # Zen philosophy (125 characters)

    "tr": """Ne varlığa sevinirim,
Ne yokluğa yerinirim.
Aşkın ile avunurum,
Bana seni gerek seni.
Sevgiyi arayan insan, sonunda aşkın sonsuzluğunu keşfeder.
Bu, hayatta anlam bulmanın yoludur.""",  # Yunus Emre (125 kelime)

    "sw": """Heri wenye njaa na kiu ya haki, maana hao watashibishwa. Lakini kumbuka, safari ya haki si rahisi. Ni safari ya maamuzi magumu na msimamo thabiti.""",  # Swahili Bible + wisdom (105 characters)

    "ja": """止まない雨はない。人生の旅の中で、雨は時折降るが、いつか必ず止む。そのとき、私たちは新しい光を見つける。""",  # Japanese proverb (110 characters)

    "zh": """千里之行，始于足下。人生中，每一步都是新的挑战和机会。只要坚持努力，就能到达目标。""",  # Laozi, Dao De Jing (110 characters)

    "pa": """ਮਨ ਜੀਤੇ ਜਗ ਜੀਤ। ਜੀਵਨ ਵਿੱਚ ਸਫਲਤਾ ਲਈ ਸਬਰ ਅਤੇ ਹੌਸਲੇ ਦੀ ਲੋੜ ਹੁੰਦੀ ਹੈ। ਹਰ ਮੁਸ਼ਕਲ ਸਾਨੂੰ ਨਵਾਂ ਦਰਸਾ ਦਿੰਦੀ ਹੈ।""",  # Guru Granth Sahib + wisdom (100 characters)

    "hi": """श्री गुरु चरन सरोज रज, निज मनु मुकुरु सुधारि।
बरनऊं रघुबर बिमल जसु, जो दायकु फल चारि॥
बुद्धिहीन तनु जानिके, सुमिरौं पवन-कुमार।
बल बुधि विद्या देहु मोहिं, हरहु कलेस बिकार॥
""",

    "ar": """اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَنْ ذَا الَّذِي يَشْفَعُ عِنْدَهُ إِلَّا بِإِذْنِهِ ۚ"""
}

supported=['en', 'es', 'fr', 'de', 'it', 'pt', 'pl', 'tr', 'ru', 'nl', 'cs', 'ar', 'zh-cn', 'hu', 'ko', 'ja', 'hi']

speakers=['Claribel Dervla', 'Daisy Studious', 'Gracie Wise', 'Tammie Ema', 'Alison Dietlinde', 'Ana Florence', 'Annmarie Nele', 'Asya Anara', 'Brenda Stern', 'Gitta Nikolina', 'Henriette Usha', 'Sofia Hellen', 'Tammy Grit', 'Tanja Adelina', 'Vjollca Johnnie', 'Andrew Chipper', 'Badr Odhiambo', 'Dionisio Schuyler', 'Royston Min', 'Viktor Eka', 'Abrahan Mack', 'Adde Michal', 'Baldur Sanjin', 'Craig Gutsy', 'Damien Black', 'Gilberto Mathias', 'Ilkin Urbano', 'Kazuhiko Atallah', 'Ludvig Milivoj', 'Suad Qasim', 'Torcull Diarmuid', 'Viktor Menelaos', 'Zacharie Aimilios', 'Nova Hogarth', 'Maja Ruoho', 'Uta Obando', 'Lidiya Szekeres', 'Chandra MacFarland', 'Szofi Granger', 'Camilla Holmström', 'Lilya Stainthorpe', 'Zofija Kendrick', 'Narelle Moon', 'Barbora MacLean', 'Alexandra Hisakawa', 'Alma María', 'Rosemary Okafor', 'Ige Behringer', 'Filip Traverse', 'Damjan Chapman', 'Wulf Carlevaro', 'Aaron Dreschner', 'Kumar Dahl', 'Eugenio Mataracı', 'Ferran Simen', 'Xavier Hayasaka', 'Luis Moray', 'Marcos Rudaski']

# Function to calculate the length of a .wav file
def get_audio_length(audio_file_path):
    with wave.open(audio_file_path, 'rb') as wav_file:
        # Calculate the length in seconds
        frames = wav_file.getnframes()
        rate = wav_file.getframerate()
        duration = frames / float(rate)
    return duration

def load_tts_config():
    try:
        with open("tts.yaml", "r") as f:
            return yaml.safe_load(f)
    except Exception as e:
        logging.error(f"Error loading config: {e}")
        return None

VOICES = load_tts_config()

tts = TTS(model_name="tts_models/multilingual/multi-dataset/xtts_v2", progress_bar=False).to("cpu")
for language, text in texts.items():
    print(f"Generate voice for language {language}")
    text = text.replace(".", ";") # Workaround to not pronounce the dot
    if language not in VOICES["languages"]:
        print(f"Language '{language}' not supported.")
        continue
    engine = VOICES["languages"][language]["engine"]
    speaker = VOICES["languages"][language].get("default-voice", "default")
    output_file = f"/tmp/{engine}-{speaker}-{language}.wav"
    if engine == "xtts":
        print(f"Generate for speaker {speaker} in language {language}")
        if len(text) >= VOICES["languages"][language]["char-limit"]:
            logging.info("Text will not be split")
            split_sentences = True
        else:
            logging.info("Text will be split")
            split_sentences = False
        code = VOICES["languages"][language].get("code", language)
        tts.tts_to_file(text, split_sentences=split_sentences, speaker=speaker, language=code, file_path=output_file)

        audio_length = get_audio_length(output_file)
        print(f"Audio length: {audio_length:.2f} seconds")
    else:
        print(f"Skip language {language} NOT SUPPORTED")

    # Paths
    intro_video_path = "template/SQUARE/intromp4"
    intro_glitch_audio_path = "template/VIDEO/intro-glitch.mp3"
    intro_www_audio_path = "template/VIDEO/intro-www.wav"
    main_video_path = "tts_test_video.mp4"
    main_audio_path = output_file  # Assuming this is defined elsewhere
    outro_video_path = "template/outro-video.mp4"
    output_path = "app/static/output/output.mp4"

    try:
        # Load intro video
        intro_video = VideoFileClip(intro_video_path)

        # Load and combine intro audio
        intro_glitch_audio = AudioFileClip(intro_glitch_audio_path).with_start(0.2)
        intro_www_audio = AudioFileClip(intro_www_audio_path).with_start(0.5)
        combined_intro_audio = CompositeAudioClip([intro_glitch_audio, intro_www_audio])
        intro_video = intro_video.with_audio(combined_intro_audio)

        # Load main video and audio
        main_video = VideoFileClip(main_video_path)
        main_audio = AudioFileClip(main_audio_path)
        main_video = main_video.with_audio(main_audio)

        # Load outro video
        #outro_video = VideoFileClip(outro_video_path)

        # Concatenate all video clips
        final_video = concatenate_videoclips([intro_video, main_video]) #, outro_video])

        # Write the final output
        final_video.write_videofile(output_path, codec="libx264", audio_codec="aac")

        intro_glitch_audio.close()
        intro_www_audio.close()
        main_audio.close()

        print(f"Video successfully saved to {output_path}")

    except Exception as e:
        print(f"An error occurred: {e}")

    break # test


