#
# Copyright 2024 DrWave
#
# WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
# countries, culminating in a global wave. The project aims to transcend physical and cultural
# boundaries, fostering unity, community, and shared human experience by leveraging real-time
# coordination and location-based services.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
import logging
import os
from moviepy import VideoFileClip, AudioFileClip, CompositeAudioClip, concatenate_videoclips
from moviepy.audio.fx import AudioFadeOut

from app.config import Config
from app.services.utils import u_num
from app.services.video_utils import display_static_page, render_progressive_text, generate_voice_for_text, \
    get_video_writer

# Paths
FONT_PATH = "app/fonts/montserrat.ttf"  # Update with your font path

def get_boot_frame(format):
    image_size = (format["IMAGE"]["WIDTH"], format["IMAGE"]["HEIGHT"])
    intro_video_path_output = os.path.join(Config.TEMPLATE_FOLDER, "output", f"boot-{format['FOLDER']}.mp4")

    if not os.path.exists(intro_video_path_output):
        try:
            logging.info(f"Generate boot video for format {format['FOLDER']}")

            video_writer, boot_video_path = get_video_writer(image_size)
            logo_path = os.path.join(Config.TEMPLATE_FOLDER, format["FOLDER"], "logo.jpg")
            display_static_page(video_writer, image_size, logo_path, 1)
            video_writer.release()
            boot_video = VideoFileClip(boot_video_path)
        except Exception as e:
            logging.error(f"Error generating boot video: {e}")
            raise
    else:
        boot_video = VideoFileClip(intro_video_path_output)

    return boot_video

def get_intro_video(format):
    intro_video_path_output = os.path.join(Config.TEMPLATE_FOLDER, "output", f"intro-{format['FOLDER']}.mp4")

    if not os.path.exists(intro_video_path_output):
        try:
            logging.info(f"Generate intro video for format {format['FOLDER']}")

            intro_glitch_audio_path = os.path.join(Config.TEMPLATE_FOLDER, "VIDEO", "intro-glitch.mp3")
            intro_www_audio_path = os.path.join(Config.TEMPLATE_FOLDER, "VIDEO", "intro-www.wav")
            intro_video_template_path = os.path.join(Config.TEMPLATE_FOLDER, format["FOLDER"], "intro.mp4")

            # Load intro video
            intro_video = VideoFileClip(intro_video_template_path)

            # Load and combine intro audio
            intro_glitch_audio = AudioFileClip(intro_glitch_audio_path).with_start(0.2)
            intro_www_audio = AudioFileClip(intro_www_audio_path).with_start(0.5)
            combined_intro_audio = CompositeAudioClip([intro_glitch_audio, intro_www_audio])

            intro_video = intro_video.with_audio(combined_intro_audio)
            intro_video.write_videofile(intro_video_path_output, codec="libx264", audio_codec="aac")

            intro_glitch_audio.close()
            intro_www_audio.close()

        except Exception as e:
            logging.error(f"Error generating intro video: {e}")
            raise
    else:
        intro_video = VideoFileClip(intro_video_path_output)

    return intro_video

def generate_video(format, language, page1, page2, bold_parts, cover_link):
    format = Config.FORMATS[format]
    image_size = (format["IMAGE"]["WIDTH"], format["IMAGE"]["HEIGHT"])
    output_video_path = os.path.join(Config.OUTPUT_FOLDER, f"{u_num()}_video.mp4")

    text_video_path = None
    audio_page1, audio_page2, text_video = None, None, None
    BOOT_VIDEO, INTRO_VIDEO, combined_text_audio = None, None, None

    template_tictac_path = os.path.join(Config.TEMPLATE_FOLDER, "VIDEO", "tictac.wav")
    template_2026_path = os.path.join(Config.TEMPLATE_FOLDER, format["FOLDER"], "2026.jpg")
    template_logo_path = os.path.join(Config.TEMPLATE_FOLDER, format["FOLDER"], "logo.jpg")

    try:
        ## AUDIO: READ TEXT
        logging.info(f"Generate voices for text")
        audio_page1_path, t_audio_page1 = generate_voice_for_text(language, page1)
        audio_page2_path, t_audio_page2 = generate_voice_for_text(language, page2)

        ## VIDEO: READ TEXT
        logging.info(f"Render text in frames")
        video_writer, text_video_path = get_video_writer(image_size)

        t_audio_page1_for_text = Config.RATE_TEXT_ADVANCE * t_audio_page1
        t_audio_page2_for_text = Config.RATE_TEXT_ADVANCE * t_audio_page2

        t_video_end_time_page1 = (t_audio_page1 - t_audio_page1_for_text) + Config.VIDEO_TEXT_END_TIME # + Config.VIDEO_START_READ_AFTER
        t_video_end_time_page2 = (t_audio_page2 - t_audio_page2_for_text) # + Config.VIDEO_TEXT_END_TIME + Config.VIDEO_START_READ_AFTER

        render_progressive_text(format, video_writer, image_size, t_audio_page1_for_text, t_video_end_time_page1, language, page1, bold_parts)
        render_progressive_text(format, video_writer, image_size, t_audio_page2_for_text, t_video_end_time_page2, language, page2, bold_parts)

        display_static_page(video_writer, image_size, "app/" + cover_link, Config.STATIC_PAGE_TIME * Config.VIDEO_FPS)
        display_static_page(video_writer, image_size, template_2026_path, Config.STATIC_PAGE_TIME * Config.VIDEO_FPS)
        display_static_page(video_writer, image_size, template_logo_path, Config.STATIC_PAGE_TIME * Config.VIDEO_FPS)

        video_writer.release()

        t_audio_start_page1 = Config.VIDEO_START_READ_AFTER
        t_audio_start_page2 = t_audio_page1 + Config.VIDEO_TEXT_END_TIME + Config.VIDEO_START_READ_AFTER * 2
        t_audio_start_tictac = t_audio_start_page2 + t_audio_page2 + Config.VIDEO_TEXT_END_TIME + Config.STATIC_PAGE_TIME

        logging.info(f"Page1: Video duration {t_audio_page1_for_text + t_video_end_time_page1}, Audio duration: {t_audio_page1} startsec: {t_audio_start_page1}")
        logging.info(f"Page2: Video duration {t_audio_page2_for_text + t_video_end_time_page2}, Audio duration: {t_audio_page2} startsec: {t_audio_start_page2}")

        # Load and combine audio
        logging.info(f"Combine audio")
        audio_page1 = AudioFileClip(audio_page1_path).with_start(t_audio_start_page1)
        audio_page2 = AudioFileClip(audio_page2_path).with_start(t_audio_start_page2)
        audio_tictac = AudioFileClip(template_tictac_path).with_start(t_audio_start_tictac)
        combined_text_audio = CompositeAudioClip([audio_page1, audio_page2, audio_tictac])

        text_video = VideoFileClip(text_video_path)
        TEXT_VIDEO = text_video.with_audio(combined_text_audio)

        # Concatenate and write final video
        logging.info(f"Concatenate and write video")
        BOOT_VIDEO = get_boot_frame(format)
        INTRO_VIDEO = get_intro_video(format)

        output_video = concatenate_videoclips([BOOT_VIDEO, INTRO_VIDEO, TEXT_VIDEO])

        # Add background music
        audio_background_path = os.path.join(Config.TEMPLATE_FOLDER, "VIDEO", "background.mp3")
        background_music = AudioFileClip(audio_background_path).with_volume_scaled(0.05)
        background_music = background_music.with_start(BOOT_VIDEO.duration + INTRO_VIDEO.duration)
        background_music = background_music.with_duration(combined_text_audio.duration - Config.STATIC_PAGE_TIME * 2)
        background_music = background_music.with_effects([AudioFadeOut(Config.STATIC_PAGE_TIME / 2)])
        final_audio = CompositeAudioClip([output_video.audio, background_music])
        output_video = output_video.with_audio(final_audio)

        output_video.write_videofile(output_video_path, codec="libx264", audio_codec="aac")

    except Exception as e:
        logging.error(f"Error generating video: {e}")
        raise

    finally:
        # Close all resources
        if audio_page1:
            audio_page1.close()
        if audio_page2:
            audio_page2.close()
        if text_video:
            text_video.close()
        if combined_text_audio:
            combined_text_audio.close()
        if BOOT_VIDEO:
            BOOT_VIDEO.close()
        if INTRO_VIDEO:
            INTRO_VIDEO.close()

        if os.path.exists(text_video_path):
            os.remove(text_video_path)

    logging.info(f"Video successfully saved to {output_video_path}")
    return output_video_path.replace("app/", "", 1)
