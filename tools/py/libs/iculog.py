# Copyright (C) 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

"""Color logger to console, and also log to file."""

import logging
import pathlib
import sys


class ColorLogFormatter(logging.Formatter):
  """Logging with colors."""

  # https://en.wikipedia.org/wiki/ANSI_escape_code#Select_Graphic_Rendition_parameters
  _COLOR_CODES = {
      logging.CRITICAL: '\x1b[1;95m',  # bold ; bright magenta
      logging.ERROR: '\x1b[91m',  # bright red
      logging.WARNING: '\x1b[93m',  # bright yellow
      logging.INFO: '\x1b[32m',  # green
      logging.DEBUG: '\x1b[90m',  # dark gray
  }

  _RESET_CODE = '\x1b[m'

  def __init__(self, *args, **kwargs):  # type: ignore
    super().__init__(*args, **kwargs)  # type: ignore

  def format(self, record: logging.LogRecord, *args, **kwargs):  # type: ignore
    record.color_on = self._COLOR_CODES[record.levelno]
    record.color_off = self._RESET_CODE
    return super().format(record, *args, **kwargs)


def init_logging(
    logfile_name: pathlib.Path | None = None,
    console_level: str = 'info',
    color: bool = True
):
  """Configure logging."""
  logger = logging.getLogger()
  logger.setLevel(logging.DEBUG)
  logger.handlers.clear()

  # Console handler
  console_handler = logging.StreamHandler(sys.stdout)
  console_level = console_level or 'info'
  console_handler.setLevel(console_level.upper())
  global _head_attr, _head_reset
  if color:
    console_formatter: logging.Formatter = ColorLogFormatter(
        fmt='%(color_on)s[%(levelname)s]%(color_off)s %(message)s'
    )
    _head_attr = '\x1b[93;44m'  # fg:yellow, bg:blue
    _head_reset = '\x1b[m'
  else:
    console_formatter = logging.Formatter(fmt='[%(levelname)s] %(message)s')
    _head_attr = ''
    _head_reset = ''
  console_handler.setFormatter(console_formatter)
  logger.addHandler(console_handler)

  if logfile_name:
    # Log file handler
    logfile_handler = logging.FileHandler(logfile_name, encoding='utf-8')
    logfile_handler.setLevel(logging.DEBUG)
    logfile_formatter = logging.Formatter(
        '%(asctime)s [%(levelname)s] %(message)s'
    )
    logfile_handler.setFormatter(logfile_formatter)
    logger.addHandler(logfile_handler)


def failure(msg: str, exit_code: int = 1) -> None:
  """Log a message with severity `CRITICAL` and exits with `exit_code`."""
  logging.critical(msg)
  exit(exit_code)


def error(msg: str) -> None:
  """Log a message with severity `ERROR`."""
  logging.error(msg)


def warning(msg: str) -> None:
  """Log a message with severity `WARNING`."""
  logging.warning(msg)


def info(msg: str) -> None:
  """Log a message with severity `INFO`."""
  logging.info(msg)


def debug(msg: str) -> None:
  """Log a message with severity `DEBUG`."""
  logging.debug(msg)

# The width and color attributes of a title / subtitle
# For example:
# *----------------------------------*
# | Step 1: this is a title          |
# *----------------------------------*
# and
# -------[ This is a subtitle ]-------
_WIDTH: int = 70
_head_attr = '\x1b[93;44m'  # fg:yellow, bg:blue
_head_reset = '\x1b[m'


def title(text: str) -> None:
  """Log a message with severity `DEBUG`."""
  width = max(len(text) + 2, _WIDTH)
  logging.info('%s┌%s┐%s', _head_attr, ''.center(width, '─'), _head_reset)
  logging.info('%s│%s│%s', _head_attr, text.center(width, ' '), _head_reset)
  logging.info('%s└%s┘%s', _head_attr, ''.center(width, '─'), _head_reset)


def subtitle(text: str, pre: str = '[', post: str = ']') -> None:
  """Log a message with severity `DEBUG`."""
  text = pre + ' ' + text + ' ' + post
  logging.info('%s%s%s', _head_attr, text.center(_WIDTH + 2, '─'), _head_reset)


def separator():
  logging.info('%s%s%s', _head_attr, ''.center(_WIDTH + 2, '─'), _head_reset)
