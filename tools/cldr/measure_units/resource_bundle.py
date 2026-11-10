# © 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

"""Reads the structure of ICU resource bundle text.

This is the format of ICU's .txt data files, for example
icu4c/source/data/unit/en.txt. The MeasureUnit tool in this directory needs
only the keys of tables and the targets of aliases, so that is all this
module returns:

* A table becomes a dict, in file order.
* An alias, like units:alias{"/LOCALE/unitsShort"}, becomes an Alias.
* Other values (strings, integers, arrays, binary data) become None.

Keys lose their type, like ":alias", ":int", or ":table", and quoted keys lose
their quotes.
"""

from __future__ import annotations

import dataclasses
import re

# The tokens: quoted strings, braces, commas, and bare words such as keys.
# Whitespace and comments are matched too, and then skipped.
_TOKEN = re.compile(
    r'\s+|//[^\n]*|/\*.*?\*/|"(?:[^"\\]|\\.)*"|[{},]|[^\s{},"]+', re.S
)


class Error(Exception):
  """Resource bundle text that this module can't read."""


@dataclasses.dataclass(frozen=True)
class Alias:
  """An alias resource, e.g. units:alias{"/LOCALE/unitsShort"}."""

  path: str


def _tokens(text: str, path: str) -> list[str]:
  tokens = []
  pos = 0
  while pos < len(text):
    match = _TOKEN.match(text, pos)
    if not match:
      raise Error(f'{path}: unexpected text: {text[pos:pos + 20]!r}')
    token = match[0]
    if not (token[0].isspace() or token.startswith(('//', '/*'))):
      tokens.append(token)
    pos = match.end()
  return tokens


def parse(text: str, path: str = '<text>') -> dict[str, object]:
  """Parses resource bundle text into {name: value} for its top table.

  path is used only in error messages.
  """
  tokens = _tokens(text, path)
  pos = 0

  def peek(offset: int) -> str:
    if pos + offset >= len(tokens):
      raise Error(f'{path}: unexpected end of the text')
    return tokens[pos + offset]

  def next_token() -> str:
    nonlocal pos
    token = peek(0)
    pos += 1
    return token

  def is_key(token: str) -> bool:
    """Whether a token can be a key: a bare word or a quoted string."""
    return token not in ('{', '}', ',')

  def resource() -> tuple[str, object]:
    """Parses a key and its value, from the key to the closing brace."""
    key = next_token()
    if not is_key(key) or next_token() != '{':
      raise Error(f'{path}: expected a key and "{{", found {key!r}')
    if key.startswith('"'):
      name, kind = key[1:-1], ''
    else:
      name, _, kind = key.partition(':')
    if kind == 'alias':
      # The path can be split into adjacent strings, like any string.
      parts = []
      while peek(0).startswith('"'):
        parts.append(next_token()[1:-1])
      if not parts or next_token() != '}':
        raise Error(f'{path}: unexpected value of the alias {key}')
      return name, Alias(''.join(parts))
    if is_key(peek(0)) and peek(1) == '{':
      table = {}
      while peek(0) != '}':
        item_name, value = resource()
        table[item_name] = value
      next_token()
      return name, table
    # A string, an integer, an array, or binary data: skip it.
    depth = 1
    while depth:
      token = next_token()
      if token == '{':
        depth += 1
      elif token == '}':
        depth -= 1
    return name, None

  name, value = resource()
  if pos != len(tokens):
    raise Error(f'{path}: unexpected text after the table {name}')
  return {name: value}


def read(path: str) -> dict[str, object]:
  """Reads and parses a resource bundle .txt file (UTF-8, maybe with a BOM)."""
  with open(path, encoding='utf-8-sig') as f:
    return parse(f.read(), path)
