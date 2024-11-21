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

import json
import random
import os
from app.config import Config

def u_num():
    return ''.join(random.choices('0123456789', k=8))

# -----------------------------------------------------------------------------

used_texts = {}
def open_used_texts():
    global used_texts
    if os.path.exists(Config.USED_TEXTS_FILE):
        with open(Config.USED_TEXTS_FILE, "r") as f:
            used_texts = json.load(f)
open_used_texts()

def save_used_texts():
    global used_texts
    with open(Config.USED_TEXTS_FILE, "w") as f:
        json.dump(used_texts, f)

def add_used_text(language, text):
    global used_texts
    open_used_texts()
    if language not in used_texts:
        used_texts[language] = []
    used_texts[language].append(text)
    save_used_texts()

def get_used_texts(language):
    global used_texts
    open_used_texts()
    return used_texts.get(language, [])