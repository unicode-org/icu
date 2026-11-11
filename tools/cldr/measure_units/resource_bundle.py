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

The grammar, as far as this module reads it:

  resource = key "{" value "}"
  key      = name | name ":" type | quoted-string
  value    = string+      (when the type is "alias")
           | resource*    (a table, when the value starts with key "{")
           | anything     (other values, which are skipped)
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


class _Parser:
  """Reads the tokens of one file, with one method for each grammar rule."""

  def __init__(self, tokens: list[str], path: str):
    self.tokens = tokens
    self.pos = 0
    self.path = path

  def error(self, message: str) -> Error:
    return Error(f'{self.path}: {message}')

  def peek(self, offset: int = 0) -> str:
    """Returns a token at or after the current one, without consuming it."""
    if self.pos + offset >= len(self.tokens):
      raise self.error('unexpected end of the text')
    return self.tokens[self.pos + offset]

  def take(self) -> str:
    """Consumes the current token and returns it."""
    token = self.peek()
    self.pos += 1
    return token

  def expect(self, expected: str):
    token = self.take()
    if token != expected:
      raise self.error(f'expected {expected!r}, found {token!r}')

  def at_key(self) -> bool:
    """Whether the next tokens are a key and its "{"."""
    return self.peek() not in ('{', '}', ',') and self.peek(1) == '{'

  def resource(self) -> tuple[str, object]:
    """resource = key "{" value "}" """
    if not self.at_key():
      raise self.error(f'expected a key and "{{", found {self.peek()!r}')
    name, kind = self.key()
    self.expect('{')
    if kind == 'alias':
      value = Alias(self.strings())
    elif self.at_key():
      value = self.table()
    else:
      self.skip_value()
      value = None
    self.expect('}')
    return name, value

  def key(self) -> tuple[str, str]:
    """Returns the name and the type of a key, like ("units", "alias")."""
    token = self.take()
    if token.startswith('"'):
      return token[1:-1], ''
    name, _, kind = token.partition(':')
    return name, kind

  def strings(self) -> str:
    """Reads one or more adjacent strings and joins them, like genrb does."""
    parts = []
    while self.peek().startswith('"'):
      parts.append(self.take()[1:-1])
    if not parts:
      raise self.error(f'expected a string, found {self.peek()!r}')
    return ''.join(parts)

  def table(self) -> dict[str, object]:
    """Reads the resources of a table, up to its closing brace."""
    table = {}
    while self.peek() != '}':
      name, value = self.resource()
      table[name] = value
    return table

  def skip_value(self):
    """Skips any other value, up to its closing brace."""
    depth = 0
    while depth or self.peek() != '}':
      token = self.take()
      if token == '{':
        depth += 1
      elif token == '}':
        depth -= 1


def parse(text: str, path: str = '<text>') -> dict[str, object]:
  """Parses resource bundle text into {name: value} for its top table.

  path is used only in error messages.
  """
  parser = _Parser(_tokens(text, path), path)
  name, value = parser.resource()
  if parser.pos != len(parser.tokens):
    raise parser.error(f'unexpected text after the table {name}')
  return {name: value}


def read(path: str) -> dict[str, object]:
  """Reads and parses a resource bundle .txt file (UTF-8, maybe with a BOM)."""
  with open(path, encoding='utf-8-sig') as f:
    return parse(f.read(), path)
