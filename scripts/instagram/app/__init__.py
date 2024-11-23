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

import os
import logging
from flask import Flask
from app.config import Config

from app.routes.health import health
from app.routes.generate import generate
from app.routes.cover import cover
from app.routes.post import post
from app.routes.extract import extract
from app.routes.hashtags import hashtags
from app.routes.index import index
from app.routes.video import video

logging.basicConfig(level=logging.INFO)

template_dir = os.path.abspath('app/pages/')
print(template_dir)

def create_app():
    web = Flask(__name__, template_folder=template_dir)
    web.config.from_object(Config)

    # Register routes
    web.register_blueprint(index)
    web.register_blueprint(health)
    web.register_blueprint(generate)
    web.register_blueprint(cover)
    web.register_blueprint(post)
    web.register_blueprint(extract)
    web.register_blueprint(hashtags)
    web.register_blueprint(video)

    return web
