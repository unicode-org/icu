#!/usr/bin/env python3
# © 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

"""Tests for icu_units.py.

  python3 tools/cldr/measure_units/test_icu_units.py
"""

import os
import re
import shutil
import tempfile
import unittest

import icu_units

ICU_DIR = os.path.normpath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', '..')
)
CPP_SOURCE = 'icu4c/source/i18n/measunit.cpp'


def arrays_in_measunit_cpp() -> tuple[list[tuple[str, str]], list[str]]:
  """Returns the units and the currencies in the arrays of measunit.cpp."""
  with open(os.path.join(ICU_DIR, CPP_SOURCE), encoding='utf-8') as f:
    text = f.read()

  def array(name):
    return re.search(name + r'\[\] = \{(.*?)\};', text, re.S)[1]

  types = re.findall(r'"(.*)"', array('gTypes'))
  subtypes = re.findall(r'"(.*)"', array('gSubTypes'))
  offsets = [int(n) for n in re.findall(r'\d+', array('gOffsets'))]
  units, currencies = [], []
  for i, unit_type in enumerate(types):
    type_subtypes = subtypes[offsets[i] : offsets[i + 1]]
    if unit_type == 'currency':
      currencies = type_subtypes
    elif unit_type != 'none':
      units += [(unit_type, subtype) for subtype in type_subtypes]
  return units, currencies


class FallbackTest(unittest.TestCase):
  """Tests read_units() with small unit data files."""

  def setUp(self):
    super().setUp()
    self.icu_dir = tempfile.mkdtemp()
    self.addCleanup(shutil.rmtree, self.icu_dir)

  def write_unit_data(self, en: str, root: str):
    for locale, text in (('en', en), ('root', root)):
      path = os.path.join(self.icu_dir, icu_units.UNIT_DATA.format(locale))
      os.makedirs(os.path.dirname(path), exist_ok=True)
      with open(path, 'w', encoding='utf-8-sig') as f:
        f.write(text)

  def test_fallback_through_root_alias(self):
    self.write_unit_data(
        'en{\n'
        '    units{\n'
        '        length{ meter{ dnam{"meter"} } }\n'
        '        compound{ per{"{0} per {1}"} }\n'
        '    }\n'
        '    unitsShort{\n'
        '        length{ foot{ dnam{"ft"} } }\n'
        '        coordinate{ east{ dnam{"E"} } }\n'
        '    }\n'
        '}\n',
        'root{\n'
        '    units:alias{"/LOCALE/unitsShort"}\n'
        '    unitsShort{\n'
        '        duration{\n'
        '            day-person:alias{"/LOCALE/unitsShort/duration/day"}\n'
        '        }\n'
        '    }\n'
        '}\n',
    )
    self.assertEqual(
        icu_units.read_units(self.icu_dir),
        [('duration', 'day-person'), ('length', 'foot'), ('length', 'meter')],
    )

  def test_alias_loop(self):
    self.write_unit_data(
        'en{ units:alias{"/LOCALE/unitsShort"} }',
        'root{ unitsShort:alias{"/LOCALE/units"} }',
    )
    with self.assertRaisesRegex(icu_units.Error, 'alias loop'):
      icu_units.read_units(self.icu_dir)

  def test_unexpected_alias(self):
    self.write_unit_data(
        'en{ units{ length{ meter{"m"} } } }',
        'root{ units:alias{"/ICUDATA/unitsShort"} }',
    )
    with self.assertRaisesRegex(icu_units.Error, 'unexpected alias'):
      icu_units.read_units(self.icu_dir)

  def test_type_that_is_not_a_table(self):
    self.write_unit_data('en{ units{ length{"m"} } }', 'root{ Version{"49"} }')
    with self.assertRaisesRegex(icu_units.Error, 'length is not a table'):
      icu_units.read_units(self.icu_dir)


class IcuDataTest(unittest.TestCase):
  """Tests with the ICU data in this checkout.

  The arrays in measunit.cpp were generated from ICU4J's
  MeasureUnit.getAvailable(), so they show what ICU4J returns. These tests
  fail when ICU's data has changed but the MeasureUnit code was not
  generated again.
  """

  def test_units_match_measunit_cpp(self):
    units, _ = arrays_in_measunit_cpp()
    self.assertEqual(icu_units.read_units(ICU_DIR), sorted(units))

  def test_currencies_match_measunit_cpp(self):
    _, currencies = arrays_in_measunit_cpp()
    self.assertEqual(icu_units.read_currencies(ICU_DIR), currencies)


if __name__ == '__main__':
  unittest.main()
