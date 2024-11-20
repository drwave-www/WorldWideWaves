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

from flask import Blueprint, render_template
from app.config import Config

index = Blueprint("index", __name__)

def filter_sensitive_data(languages):
    filtered_languages = {}
    for lang, data in languages.items():
        filtered_languages[lang] = {
            "fixed_hashtags": data["fixed_hashtags"],
            "accounts": list(data["accounts"].keys())  # Only keep account names
        }
    return filtered_languages

@index.route('/', methods=['GET'])
def __index():
    return render_template("index.html", languages=filter_sensitive_data(Config.LANGUAGES))