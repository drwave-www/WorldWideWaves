#!/usr/bin/python3

from ruamel.yaml import YAML
from pathlib import Path

path = Path('eventmod .yaml')
yaml = YAML(typ='safe')
data = yaml.load(path)

