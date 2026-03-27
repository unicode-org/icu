# Copyright (C) 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

import os

from . import iculog

HOME_DIR: str = os.path.expanduser('~')
ICU_DIR: str = os.getenv('ICU_ROOT', '.')

# Initialize logging
iculog.init_logging()
