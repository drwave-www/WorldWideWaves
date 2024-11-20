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

import openai
import logging
import json
from app.config import Config
from app.services.utils import get_used_texts

openai.api_key = Config.OPENAI_API_KEY

def get_openai_data(language):
    prompt = f"""
    Generate a structured JSON with information about a major historical, literary, or philosophical text in the {language} language (ISO 639 code), adhering to the following constraints:

    ### Context:
    WorldWideWaves is a global movement celebrating unity through synchronized human waves across cities and countries.
    This initiative inspires solidarity, community, and shared experiences through daily Instagram posts aligned with key themes.
    WorldWideWaves is an ambitious endeavor to harness the power of technology for a truly noble cause—connecting humanity in a celebration of unity and collective action.

    ### Key Priorities:
    1. Align with the following themes: {', '.join(Config.THEMES)}.
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
       - If no suitable text is found, return: `{{"error": "No suitable text found"}}`.

    ### Output:
    Return only the structured JSON described above.
    """
    logging.info(f"PROMPT USED: '{prompt}'")
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
    json_data["hashtags"] = Config.LANGUAGES[language]["fixed_hashtags"] + json_data["hashtags"]
    return json_data