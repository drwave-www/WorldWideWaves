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

from app.config import Config
from app.services.utils import u_num
from app.services.video_utils import display_static_page, render_progressive_text, generate_voice_for_text, \
    get_video_writer

# Paths
FONT_PATH = "app/fonts/montserrat.ttf"  # Update with your font path

def generate_video(format, language, page1, page2, bold_parts, cover_link):
    format = Config.FORMATS[format]
    image_size = (format["IMAGE"]["WIDTH"], format["IMAGE"]["HEIGHT"])

    # Static files
    output_video = os.path.join(Config.OUTPUT_FOLDER, f"{u_num()}_video.mp4")
    intro_video_path = os.path.join(Config.TEMPLATE_FOLDER, format["FOLDER"], "intro.mp4")
    intro_glitch_audio_path = os.path.join(Config.TEMPLATE_FOLDER, "VIDEO", "intro-glitch.mp3")
    intro_www_audio_path = os.path.join(Config.TEMPLATE_FOLDER, "VIDEO", "intro-www.wav")

    # Generate voices for page1 and page2
    try:
        logging.info(f"Generate voices for text")
        audio_page1_path, t_audio_page1 = generate_voice_for_text(language, page1)
        audio_page2_path, t_audio_page2 = generate_voice_for_text(language, page2)
    except Exception as e:
        logging.error(f"Error generating voices for text: {e}")
        raise

    ## INTRO ##########################
    try: # FIXME: could be cached
        logging.info(f"Generate intro video")

        # Load intro video
        intro_video = VideoFileClip(intro_video_path)

        # Load and combine intro audio
        intro_glitch_audio = AudioFileClip(intro_glitch_audio_path).with_start(0.2)
        intro_www_audio = AudioFileClip(intro_www_audio_path).with_start(0.5)
        combined_intro_audio = CompositeAudioClip([intro_glitch_audio, intro_www_audio])
        INTRO_VIDEO = intro_video.with_audio(combined_intro_audio)
    except Exception as e:
        logging.error(f"Error loading intro video: {e}")
        raise

    ## READ TEXT ######################

    # Render text on specified time
    try:
        logging.info(f"Render text in frames")
        video_writer, text_video_path = get_video_writer(image_size)

        render_progressive_text(format, video_writer, image_size, t_audio_page1 + Config.VIDEO_START_READ_AFTER, language, page1, bold_parts)
        render_progressive_text(format, video_writer, image_size, t_audio_page2 + Config.VIDEO_START_READ_AFTER, language, page2, bold_parts)
        display_static_page(video_writer, image_size, "app/" + cover_link, 3 * Config.VIDEO_FPS)

        video_writer.release()
        text_video = VideoFileClip(text_video_path)
    except Exception as e:
        logging.error(f"Error rendering text: {e}")
        raise

    # Load and combine audio
    try:
        logging.info(f"Combine audio")

        audio_page1 = AudioFileClip(audio_page1_path).with_start(Config.VIDEO_START_READ_AFTER)
        audio_page2 = AudioFileClip(audio_page2_path).with_start(t_audio_page1 + Config.VIDEO_TEXT_END_TIME + Config.VIDEO_START_READ_AFTER * 2)
        combined_text_audio = CompositeAudioClip([audio_page1, audio_page2])
        TEXT_VIDEO = text_video.with_audio(combined_text_audio)
    except Exception as e:
        logging.error(f"Error loading and combining audio: {e}")
        raise

    # TODO : add outro video
    #logging.info(f"Display static pages")
    #for static_page in [ "2026.jpg", "logo.jpg" ]:
    #    static_page = os.path.join(Config.TEMPLATE_FOLDER, format["FOLDER"], static_page)
    #    display_static_page(video_writer, image_size, static_page, 3)

    try:
        logging.info(f"Concatenate and write video")

        # Add a single frame with the logo # FIXME can be cached
        logging.info(f"Render text in frames")
        video_writer, boot_video_path = get_video_writer(image_size)
        logo_path = os.path.join(Config.TEMPLATE_FOLDER, format["FOLDER"], "logo.jpg")
        display_static_page(video_writer, image_size, logo_path, 1)
        video_writer.release()
        BOOT_VIDEO = VideoFileClip(boot_video_path)

        # Concatenate all video clips
        final_video = concatenate_videoclips([BOOT_VIDEO, INTRO_VIDEO, TEXT_VIDEO])  # , OUTRO_VIDEO])

        # Write the final output
        final_video.write_videofile(output_video, codec="libx264", audio_codec="aac")

        intro_glitch_audio.close()
        intro_www_audio.close()
        audio_page1.close()
        audio_page2.close()
    except Exception as e:
        logging.error(f"Error concatenating and writing video: {e}")
        raise

    print(f"Video successfully saved to {output_video}")
    return output_video.replace("app/", "", 1)
