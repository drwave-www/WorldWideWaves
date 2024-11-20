import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont

# Configuration
IMAGE_SIZE = (1024, 1024)
FPS = 30
TEXT_SPEED = 5  # Number of characters to add per frame
DISPLAY_TIME = 3  # Seconds to display static pages

# Paths
FONT_PATH = "app/fonts/montserrat.ttf"  # Update with your font path
OUTPUT_VIDEO = "output_video.mp4"

def generate_video(page1, page2, static_pages):
    # Initialize video writer
    fourcc = cv2.VideoWriter_fourcc(*"mp4v")
    video_writer = cv2.VideoWriter(OUTPUT_VIDEO, fourcc, FPS, IMAGE_SIZE)

    # Create pages with progressive text rendering
    render_progressive_text(video_writer, page1, "template/1.jpg", y_start=200)
    render_progressive_text(video_writer, page2, "template/1.jpg", y_start=200)

    # Display static pages
    for static_page in static_pages:
        display_static_page(video_writer, static_page)

    video_writer.release()
    print(f"Video saved to {OUTPUT_VIDEO}")

def render_progressive_text(video_writer, text, background_path, y_start=200):
    """Render text progressively onto frames."""
    bg_image = cv2.imread(background_path)
    bg_image = cv2.resize(bg_image, IMAGE_SIZE)
    pil_image = Image.fromarray(cv2.cvtColor(bg_image, cv2.COLOR_BGR2RGB))  # Convert to PIL format
    draw = ImageDraw.Draw(pil_image)

    font = ImageFont.truetype(FONT_PATH, size=42)

    current_text = ""
    for i in range(len(text) + 1):
        current_text = text[:i]
        frame.fill(0)  # Clear the frame
        draw_text(draw, current_text, font, y_start)

        # Convert PIL image back to OpenCV format
        cv_frame = cv2.cvtColor(np.array(pil_image), cv2.COLOR_RGB2BGR)
        video_writer.write(cv_frame)

def draw_text(draw, text, font, y_start):
    """Draw text on the image."""
    lines = text.split("\n")
    line_height = font.getbbox("Ay")[3] + 10
    y = y_start

    for line in lines:
        bbox = font.getbbox(line)
        x = (IMAGE_SIZE[0] - bbox[2]) // 2
        draw.text((x, y), line, font=font, fill="white")
        y += line_height

def display_static_page(video_writer, page_path):
    """Display a static page for a fixed duration."""
    static_frame = cv2.imread(page_path)
    static_frame = cv2.resize(static_frame, IMAGE_SIZE)

    for _ in range(FPS * DISPLAY_TIME):
        video_writer.write(static_frame)

# Example usage
page1_text = "This is the text for page 1.\nIt will be written progressively."
page2_text = "This is the text for page 2.\nIt also will be written progressively."
static_page_paths = ["template/3.jpg", "template/4.jpg", "template/5.jpg"]

generate_video(page1_text, page2_text, static_page_paths)

