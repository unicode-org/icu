# © 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

"""Reads the list of built-in units from ICU's data files.

The MeasureUnit tool in this directory generates code for these units. They
are the units that ICU4J's MeasureUnit.getAvailable() returns:

* The units in the English unit data (icu4c/source/data/unit/en.txt), with
  fallback to root.txt. See read_units().
* The currencies in icu4c/source/data/misc/currencyNumericCodes.txt. See
  read_currencies().
"""

from __future__ import annotations

import os
import re

import resource_bundle

UNIT_DATA = 'icu4c/source/data/unit/{}.txt'
CURRENCY_DATA = 'icu4c/source/data/misc/currencyNumericCodes.txt'

# ICU4J reads the units from the English unit data, because English has all
# of them, and en falls back to root.
UNIT_LOCALES = ('en', 'root')
UNIT_TABLE = 'units'

# Tables in the unit data that are not unit types. ICU4J skips them too.
NOT_UNIT_TYPES = frozenset(('compound', 'coordinate'))


class Error(Exception):
  """ICU data that doesn't look the way this module expects."""


def read_units(icu_dir: str) -> list[tuple[str, str]]:
  """Returns the (type, subtype) pairs of the units in ICU's unit data, sorted.

  They are the units that ICU4J's MeasureUnit.getAvailable() returns, other
  than currencies. ICU4J reads the "units" table of the English unit data
  with resource fallback, so this reads these tables, in this order:

  1. en's "units".
  2. root's "units", which is an alias to "/LOCALE/unitsShort". ICU4J follows
     the alias in en, so the fallback goes on with:
  3. en's "unitsShort", and then
  4. root's "unitsShort". Some units, like duration-day-person, are only here.
  """
  bundles = {
      locale: _read_unit_bundle(icu_dir, locale) for locale in UNIT_LOCALES
  }
  units = sorted(_units_with_fallback(bundles, UNIT_TABLE, set()))
  if not units:
    raise Error(f'{UNIT_DATA.format(UNIT_LOCALES[0])}: no units')
  return units


def read_currencies(icu_dir: str) -> list[str]:
  """Returns the currency codes in ICU's currency numeric code data, sorted.

  This data comes from ISO 4217, not from CLDR. ICU4J's
  MeasureUnit.getAvailable("currency") returns the same codes: the keys of
  the "codeMap" table.
  """
  path = os.path.join(icu_dir, CURRENCY_DATA)
  top = resource_bundle.read(path).get('currencyNumericCodes')
  code_map = top.get('codeMap') if isinstance(top, dict) else None
  if not isinstance(code_map, dict) or not code_map:
    raise Error(f'{path}: no currencyNumericCodes/codeMap table')
  bad = [code for code in code_map if not re.fullmatch('[A-Z]{3}', code)]
  if bad:
    raise Error(f'{path}: unexpected currency codes {bad}')
  return sorted(code_map)


def _read_unit_bundle(icu_dir: str, locale: str) -> dict[str, object]:
  path = os.path.join(icu_dir, UNIT_DATA.format(locale))
  bundle = resource_bundle.read(path).get(locale)
  if not isinstance(bundle, dict):
    raise Error(f'{path}: expected a table named {locale}')
  return bundle


def _units_with_fallback(
    bundles: dict[str, dict[str, object]], table_name: str, seen: set[str]
) -> set[tuple[str, str]]:
  """Returns the units in a table of en and its fallback locales.

  Like ICU4J, this follows a "/LOCALE/..." alias in en, and the fallback then
  goes on from the alias target.
  """
  if table_name in seen:
    raise Error(f'The unit data has an alias loop at {table_name}')
  seen.add(table_name)
  units = set()
  for locale in UNIT_LOCALES:
    table = bundles[locale].get(table_name)
    if table is None:
      continue
    where = f'{UNIT_DATA.format(locale)}: {table_name}'
    if isinstance(table, resource_bundle.Alias):
      match = re.fullmatch(r'/LOCALE/(\w+)', table.path)
      if not match:
        raise Error(f'{where} is an unexpected alias: {table.path}')
      # The fallback starts over at the alias target, so this table isn't
      # read in the remaining locales.
      return units | _units_with_fallback(bundles, match[1], seen)
    if not isinstance(table, dict):
      raise Error(f'{where} is not a table')
    units |= _units_in_table(table, where)
  return units


def _units_in_table(
    table: dict[str, object], where: str
) -> set[tuple[str, str]]:
  """Returns the (type, subtype) pairs in a table like en's "units"."""
  units = set()
  for unit_type, subtypes in table.items():
    if unit_type in NOT_UNIT_TYPES:
      continue
    if not isinstance(subtypes, dict):
      raise Error(f'{where}/{unit_type} is not a table')
    units.update((unit_type, subtype) for subtype in subtypes)
  return units
