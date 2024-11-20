import os
import json
import random
import re
import logging
import time
import yaml
from flask import Flask, render_template, request, jsonify
from PIL import Image, ImageDraw, ImageFont
import openai
import requests
from googleapiclient.discovery import build

logging.basicConfig(level=logging.INFO)

# Constants
FONT_NORMAL="montserrat.ttf"
FONT_BOLD="montserrat-bold.ttf"
TEXT_RECT_SIZE_W = 900
TEXT_RECT_SIZE_H = 800
MIN_FONT_SIZE = 18
MAX_FONT_SIZE = 58
IMAGE_SIZE = 1080

# Load configuration
def load_config():
    try:
        with open("config.yaml", "r") as f:
            return yaml.safe_load(f)
    except Exception as e:
        logging.error(f"Error loading config: {e}")
        return None

config = load_config()
if not config:
    raise RuntimeError("Configuration file could not be loaded. Exiting.")

# Configuration
OUTPUT_FOLDER = config["output_folder"]
TEMPLATE_FOLDER = config["template_folder"]
USED_TEXTS_FILE = "used_texts.json"
THEMES = config["themes"]
LANGUAGES = config["languages"]
INSTAGRAM = config["instagram"]
openai.api_key = config["openai_api_key"]

# Setup Flask
template_dir = os.path.abspath('.')
app = Flask(__name__, template_folder = template_dir)

# Ensure directories
os.makedirs(OUTPUT_FOLDER, exist_ok=True)

# -----------------------------------------------------------------------------

# Track used texts
used_texts = {}
if os.path.exists(USED_TEXTS_FILE):
    with open(USED_TEXTS_FILE, "r") as f:
        used_texts = json.load(f)

def save_used_texts():
    with open(USED_TEXTS_FILE, "w") as f:
        json.dump(used_texts, f)

def add_used_text(language, text):
    if language not in used_texts:
        used_texts[language] = []
    used_texts[language].append(text)

def get_used_texts(language):
    return used_texts.get(language, [])

def u_num():
    return ''.join(random.choices('0123456789', k=8))

# -----------------------------------------------------------------------------

def fetch_google_image(query):
    try:
        service = build("customsearch", "v1", developerKey=config["google_search_api_key"])
        response = service.cse().list(
            q=query,
            cx=config["google_cx"],
            searchType="image",
            imgSize="LARGE",
            num=10
        ).execute()

        items = response.get("items", [])
        if items:
            return random.choice(items)["link"]
    except Exception as e:
        logging.error(f"Error fetching Google image: {e}")
    return None

# -----------------------------------------------------------------------------

def get_cover(author, title):
    image_path=os.path.join(TEMPLATE_FOLDER, "1.jpg")

    logging.info(f"Open template {image_path}")
    cover_template = Image.open(image_path)
    draw = ImageDraw.Draw(cover_template)

    logging.info(f"Create texts")
    font = ImageFont.truetype(FONT_BOLD, size=50)
    bbox = font.getbbox(f"{title}")
    draw.text(((IMAGE_SIZE - (bbox[2] - bbox[0])) // 2, 120), f"{title}", fill="white", font=font)
    
    bbox = font.getbbox(f"{author}")
    draw.text(((IMAGE_SIZE - (bbox[2] - bbox[0])) // 2, 900), f"{author}", fill="white", font=font)
 
    logging.info(f"Search for author image on Google")
    author_image_url = ""
    try:
        author_image_url = fetch_google_image(f"portrait of {author} -site:gettyimages.*")
    except Exception as e:
        logging.error(f"Exception while fetching author image: {e}")

    logging.info(f"Image found: {author_image_url}")
    if author_image_url:
        logging.info(f"Retrieve image raw")
        response = requests.get(author_image_url, stream=True, headers={"User-Agent": "Mozilla/5.0"})
        if response.headers["Content-Type"].startswith("image"):
            author_image = Image.open(response.raw)

            logging.info(f"Open and resize it")
            fixed_height = 560
            aspect_ratio = author_image.width / author_image.height
            new_width = int(fixed_height * aspect_ratio)
            author_image = author_image.resize((new_width, fixed_height))
            cover_template.paste(author_image, ((IMAGE_SIZE - new_width) // 2, (IMAGE_SIZE - fixed_height) // 2))
        else:
            logging.error("URL does not point to a valid image.")

    logging.info(f"Save cover image")
    cover_path = os.path.join(OUTPUT_FOLDER, f"{u_num()}_1.jpg")
    cover_template.save(cover_path)
    return cover_path
 
# -----------------------------------------------------------------------------

def draw_bounded_text(draw, idx, text, bold_parts):
    rect_x = (IMAGE_SIZE - TEXT_RECT_SIZE_W) // 2

    # Load the text and fonts
    font_size = MAX_FONT_SIZE
    font = ImageFont.truetype(FONT_NORMAL, size=font_size)

    # Split text into parts, tagging bold sections
    styled_parts = []
    remaining_text = text
    for bold_part in bold_parts:
        if bold_part in remaining_text:
            # Split around the bold part
            before, bold, remaining_text = remaining_text.partition(bold_part)
            if before.strip():
                styled_parts.append((before.strip(), FONT_NORMAL))
            if bold.strip():
                styled_parts.append((bold.strip(), FONT_BOLD))
    # Add any remaining text
    if remaining_text.strip():
        styled_parts.append((remaining_text.strip(), FONT_NORMAL))

    # Adjust font size to fit within the rectangle
    total_height = 0
    lines = []
    while font_size >= MIN_FONT_SIZE:
        # Simulate the layout with current font size
        lines, total_height = layout_text(font_size, styled_parts)

        # Check if the total height fits within the rectangle
        if total_height <= TEXT_RECT_SIZE_H:
            break

        # Reduce the font size
        logging.info(f"Reduce the font size from {font_size} as total height is {total_height} vs {TEXT_RECT_SIZE_H}")
        font_size -= 1

    # If text doesn't fit, raise an error
    if font_size < MIN_FONT_SIZE:
        raise ValueError("The text is too large to fit within the bounds.")

    # Calculate starting position to center the text within the rectangle
    y = ((IMAGE_SIZE - total_height) // 2) - font.getbbox("Ay")[3] // 2

    # Draw each line of justified text
    for i, line in enumerate(lines):
        x = rect_x
        justify_line(draw, line, x, y, is_last_line=(i == len(lines) - 1))
        y += font.getbbox("Ay")[3]  # Line height

    # Save the image
    page_path = os.path.join(OUTPUT_FOLDER, f"{u_num()}_{idx}.jpg")
    return page_path

def layout_text(font_size, styled_parts):
    lines = []
    current_line = []
    current_width = 0
    total_height = 0

    current_font = ImageFont.truetype(FONT_NORMAL, size=font_size)
    for part, current_font in styled_parts:
        current_font = ImageFont.truetype(current_font, size=font_size)
        space_width = current_font.getbbox(" ")[2]

        # Tokenize words and handle punctuation
        words = re.findall(r"[^\s,]+|[,.]", part)  # Split into words and punctuation
        for word in words:
            if word == ",":
                current_width +=  current_font.getbbox(",")[2]
                current_line[-1][0] += ","  # Append comma to the previous word
                continue
            elif word == ".":
                current_width +=  current_font.getbbox(".")[2]
                current_line[-1][0] += "."  # Append period to the previous word
                continue
            else:
                word_width = current_font.getbbox(word)[2]

            if current_width + word_width > TEXT_RECT_SIZE_W:
                # Finish the current line
                lines.append(current_line)
                total_height += current_font.getbbox("Ay")[3]
                current_line = []
                current_width = 0

            current_line.append([word, current_font])
            current_width += word_width + space_width

    if current_line:
        lines.append(current_line)
        total_height += current_font.getbbox("Ay")[3]

    return lines, total_height


def justify_line(draw, line, x, y, is_last_line=False):
    total_width = sum((font.getbbox(word)[2] for word, font in line))
    space_count = len(line) - 1

    if space_count > 0 and not is_last_line:
        space_width = (TEXT_RECT_SIZE_W - total_width) // space_count
    else:
        space_width = line[0][1].getbbox(" ")[2]

    for word, font in line:
        draw.text((x, y), word, font=font, fill="white")
        x += font.getbbox(word)[2] + space_width

def create_images(language, json_data):
    image_paths = []

    # 1. Page 1 and 2
    logging.info(f"Generate text pages")
    for idx, page_key in enumerate(["page1", "page2"], start=1):
        img_path = os.path.join(TEMPLATE_FOLDER, f"{idx}.jpg")
        
        logging.info(f"Open file {img_path}")
        text_template = Image.open(img_path)
        draw = ImageDraw.Draw(text_template)

        logging.info(f"Draw the text")
        page_path = draw_bounded_text(draw, idx, json_data[page_key], json_data["bold_parts"])

        logging.info(f"Save image")
        text_template.save(page_path)
        image_paths.append((idx, page_path))

    # 2. Cover Image
    cover_path = get_cover(json_data['author'], json_data['name'])
    image_paths.append((3, cover_path))

    # 3. 2026 Page
    year_path = os.path.join(TEMPLATE_FOLDER, "4.jpg")
    final_path = os.path.join(OUTPUT_FOLDER, f"{u_num()}_4.jpg")
    Image.open(year_path).save(final_path)
    image_paths.append((4, final_path))

    # 4. Logo Page
    logo_path = os.path.join(TEMPLATE_FOLDER, "5.jpg")
    final_path = os.path.join(OUTPUT_FOLDER, f"{u_num()}_5.jpg")
    Image.open(logo_path).save(final_path)
    image_paths.append((5, final_path))

    return image_paths

# -----------------------------------------------------------------------------

def get_openai_data(language):
    prompt = f"""
    Generate a structured JSON with information about a major historical, literary, or philosophical text in the {language} language (ISO 639 code), adhering to the following constraints:

    ### Context:
    WorldWideWaves is a global movement celebrating unity through synchronized human waves across cities and countries.
    This initiative inspires solidarity, community, and shared experiences through daily Instagram posts aligned with key themes.
    WorldWideWaves is an ambitious endeavor to harness the power of technology for a truly noble cause—connecting humanity in a celebration of unity and collective action.

    ### Key Priorities:
    1. Align with the following themes: {', '.join(THEMES)}.
    2. Ensure coherence and text continuity.
    3. Exclude texts already used: {', '.join(get_used_texts(language))}.
    4. Produce output in valid JSON format.

    ### Constraints:
    1. **Language**:
       - The text must be in {language}. Do not translate it; only use existing texts in this language.

    2. **Excerpt**:
       - Choose from major literary texts, essays, interviews, or speeches.
       - Total length: 150–280 words, divided into two consecutive, logical parts:
         - "page1": 50–140 words.
         - "page2": 50–140 words.
       - Content should be visually descriptive, thought-provoking, and aligned with the themes.
       - Avoid quotation marks in the text.

    3. **Output Format**:
       - Return only valid JSON with the following fields:
         - "name": Name of the text or work.
         - "author": Name of the author or creator.
         - "page1": The first excerpt (50–140 words).
         - "page2": The second excerpt (50–140 words).
         - "bold_parts": A list of 1–5 short phrases to emphasize (max 5 words each).
         - "hashtags": A list of relevant and engaging Instagram hashtags.

    4. **Hashtags**:
       - Include hashtags that maximize social engagement and reflect the text's themes and context.

    5. **Error Handling**:
       - If no suitable text is found, return: `{"error": "No suitable text found"}`.

    ### Output:
    Return only the structured JSON described above.
    """

    logging.debug(f"PROMPT USED: '{prompt}'")
    response = openai.ChatCompletion.create(
        model="gpt-4",
        messages=[
            {"role": "system", "content": "Tu es un assistant spécialisé en contenu inspirant pour des publications."},
            {"role": "user", "content": prompt}
        ],
        max_tokens=800,
        temperature=0.7,
    )
    content = response['choices'][0]['message']['content'].strip().replace("```json\n", "").replace("```", "")
    logging.info(content)
    json_data = json.loads(content)
    json_data["hashtags"] = config["languages"][language]["fixed_hashtags"] + json_data["hashtags"]
    return json_data

# -----------------------------------------------------------------------------

def is_token_valid(access_token):
    try:
        url = "https://graph.facebook.com/debug_token"
        params = {
            "input_token": access_token,
            "access_token": f"{INSTAGRAM['app_id']}|{INSTAGRAM['app_secret']}"  # App Access Token
        }
        response = requests.get(url, params=params)
        response_data = response.json()

        if "data" in response_data and response_data["data"].get("is_valid"):
            expires_at = response_data["data"].get("expires_at", None)
            return True, expires_at
        return False, None
    except Exception as e:
        logging.error(f"Error validating token: {e}")
        return False, None

def refresh_token(access_token, cache={}):
    try:
        # Check if token is valid and not expired
        is_valid, expires_at = is_token_valid(access_token)
        if is_valid:
            # If expiration time exists and is more than 24 hours away, skip refresh
            if expires_at and (expires_at - time.time()) > 24 * 60 * 60:
                logging.info("Token is still valid and does not need to be refreshed.")
                return access_token

        # Refresh the token
        logging.info("Refreshing the token...")
        url = "https://graph.instagram.com/refresh_access_token"
        params = {
            "grant_type": "ig_refresh_token",
            "access_token": access_token
        }
        response = requests.get(url, params=params)
        response_data = response.json()

        if "access_token" not in response_data:
            raise Exception(f"Failed to refresh token: {response_data}")

        # Cache the token and expiry time
        refreshed_token = response_data["access_token"]
        expires_in = response_data.get("expires_in", 5184000)  # Default to 60 days if not provided
        cache.update({"token": refreshed_token, "expires_in": time.time() + expires_in})

        logging.info("Token refreshed successfully.")
        return refreshed_token
    except Exception as e:
        logging.error(f"Error refreshing token: {e}")
        raise

# -----------------------------------------------------------------------------

def create_media_container(image_url, caption, is_carousel_item, ig_user_id, access_token):
    url = f"https://graph.instagram.com/{ig_user_id}/media"
    params = {
        "image_url": image_url,
        "caption": caption,
        "is_carousel_item": is_carousel_item,
        "access_token": access_token
    }
    response = requests.post(url, params=params)
    response_data = response.json()

    if "id" not in response_data:
        raise Exception(f"Failed to create media container: {response_data}")

    return response_data["id"]

def create_carousel_container(children_ids, caption, ig_user_id, access_token):
    url = f"https://graph.instagram.com/{ig_user_id}/media"
    params = {
        "media_type": "CAROUSEL",
        "children": ",".join(children_ids),
        "caption": caption,
        "access_token": access_token
    }
    response = requests.post(url, params=params)
    response_data = response.json()

    if "id" not in response_data:
        raise Exception(f"Failed to create carousel container: {response_data}")

    return response_data["id"]

def publish_carousel(container_id, ig_user_id, access_token):
    url = f"https://graph.instagram.com/{ig_user_id}/media_publish"
    params = {
        "creation_id": container_id,
        "access_token": access_token
    }
    response = requests.post(url, params=params)
    response_data = response.json()

    if "id" not in response_data:
        raise Exception(f"Failed to publish carousel: {response_data}")

    return response_data

def create_and_publish_carousel(image_urls, caption, ig_user_id, access_token):
    try:
        # Step 1: Create individual item containers
        container_ids = []
        for image_url in image_urls:
            container_id = create_media_container(
                image_url=image_url,
                caption="",
                is_carousel_item=True,
                ig_user_id=ig_user_id,
                access_token=access_token
            )
            container_ids.append(container_id)
            logging.info(f"Created container for image {image_url}: {container_id}")

        # Step 2: Create carousel container
        carousel_container_id = create_carousel_container(
            children_ids=container_ids,
            caption=caption,
            ig_user_id=ig_user_id,
            access_token=access_token
        )
        logging.info(f"Created carousel container: {carousel_container_id}")

        # Step 3: Publish the carousel
        publish_response = publish_carousel(
            container_id=carousel_container_id,
            ig_user_id=ig_user_id,
            access_token=access_token
        )
        logging.info(f"Published carousel successfully: {publish_response['id']}")
        return publish_response

    except Exception as e:
        logging.error(f"Error during carousel publishing workflow: {e}")
        raise

# -----------------------------------------------------------------------------

def filter_sensitive_data(languages):
    filtered_languages = {}
    for lang, data in languages.items():
        filtered_languages[lang] = {
            "fixed_hashtags": data["fixed_hashtags"],
            "accounts": list(data["accounts"].keys())  # Only keep account names
        }
    return filtered_languages

# -----------------------------------------------------------------------------
@app.route("/")
def index():
    return render_template("index.html", languages=filter_sensitive_data(LANGUAGES))

@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({"status": "healthy"}), 200

@app.route("/generate", methods=["POST"])
def generate_content():
    data = request.json
    language = data["language"]

    try:
        json_data = get_openai_data(language)
        images = create_images(language, json_data)
        return jsonify({"images": images, "json_data": json_data})
    except Exception as e:
        return jsonify({"openai error": str(e)}), 500

@app.route("/extract", methods=["POST"])
def generate_extract():
    data = request.json
    language = data["language"]

    try:
        json_data = get_openai_data(language)
        return jsonify(json_data)
    except Exception as e:
        return jsonify({"openai error": str(e)}), 500

@app.route("/cover", methods=["POST"])
def generate_cover():
    data = request.json
    title = data["title"]
    author = data["author"]

    try:
        json_data = get_cover(author, title)
        return jsonify(json_data)
    except Exception as e:
        return jsonify({"cover error": str(e)}), 500

@app.route("/post", methods=["POST"])
def post_to_instagram():
    data = request.json

    # Validate input data
    if not data or "language" not in data or "title" not in data or "images" not in data or "hashtags" not in data or "accounts" not in data:
        return jsonify({"error": "Missing required fields: language, title, images, hashtags, accounts"}), 400

    language = data["language"]
    title = data["title"]
    images = data["images"]
    hashtags = data["hashtags"]
    accounts = data["accounts"]

    try:
        # Construct the caption
        caption = " ".join(hashtags)

        # Iterate over each account
        results = {}
        for account in accounts:
            try:
                logging.info(f"Prepare to post on account {account}")
                account_id = LANGUAGES[language]["accounts"][account]["account_id"]
                access_token = LANGUAGES[language]["accounts"][account]["access_token"]

                # Refresh the token if needed
                access_token = refresh_token(access_token)
                LANGUAGES[language]["accounts"][account]["access_token"] = access_token

                # Publish the carousel for this account
                try:
                    response = create_and_publish_carousel(images, caption, account_id, access_token)
                    logging.info(f"Carousel Post ID: {response['id']}")
                except Exception as e:
                    logging.error(f"Failed to publish carousel: {e}")

            except Exception as account_error:
                logging.error(f"Error for account {account}: {account_error}")
                results[account] = {"error": str(account_error)}

        # Save used text
        add_used_text(language, title)
        save_used_texts()

        return jsonify({"status": "complete", "results": results})

    except Exception as e:
        logging.error(f"Critical error: {e}")
        return jsonify({"error": str(e)}), 500

# -----------------------------------------------------------------------------

if __name__ == "__main__":
    app.run(debug=True)

