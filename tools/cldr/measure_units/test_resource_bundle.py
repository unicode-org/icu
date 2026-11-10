#!/usr/bin/env python3
# © 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

"""Tests for resource_bundle.py.

  python3 tools/cldr/measure_units/test_resource_bundle.py
"""

import os
import tempfile
import unittest

import resource_bundle as rb

UNIT_DATA = os.path.normpath(
    os.path.join(
        os.path.dirname(os.path.abspath(__file__)),
        '..', '..', '..', 'icu4c', 'source', 'data', 'unit',
    )
)


class ParseTest(unittest.TestCase):

  def test_tables_aliases_and_other_values(self):
    text = (
        '// A comment with { and "\n'
        'en{\n'
        '    /* Another comment */\n'
        '    Version{"49"}\n'
        '    units:table(nofallback){\n'
        '        length{\n'
        '            meter{\n'
        '                dnam{"m{eter}"}\n'
        '                one{"{0} \\"meter\\""}\n'
        '                other{\n'
        '                    "{0} "\n'
        '                    "meters"\n'
        '                }\n'
        '            }\n'
        '            yard:alias{"/LOCALE/units/length/foot"}\n'
        '        }\n'
        '        count:int{5}\n'
        '        list{"a", "b"}\n'
        '        "quoted key"{"x"}\n'
        '    }\n'
        '}\n'
    )
    self.assertEqual(
        rb.parse(text),
        {
            'en': {
                'Version': None,
                'units': {
                    'length': {
                        'meter': {'dnam': None, 'one': None, 'other': None},
                        'yard': rb.Alias('/LOCALE/units/length/foot'),
                    },
                    'count': None,
                    'list': None,
                    'quoted key': None,
                },
            }
        },
    )

  def test_keys_keep_their_order(self):
    self.assertEqual(list(rb.parse('t{ b{"1"} a{"2"} }')['t']), ['b', 'a'])

  def test_alias_split_into_strings(self):
    self.assertEqual(
        rb.parse('t{ a:alias{\n"/LOCALE/x/abbreviate"\n"d"\n} }'),
        {'t': {'a': rb.Alias('/LOCALE/x/abbreviated')}},
    )

  def test_errors(self):
    for text in (
        'en{',
        'en{ units{ length{ } }',
        'en{}}',
        '{}',
        'en{ a:alias{x} }',
        'en{ "abc }',
    ):
      with self.subTest(text=text):
        with self.assertRaises(rb.Error):
          rb.parse(text)


class ReadTest(unittest.TestCase):

  def test_byte_order_mark(self):
    with tempfile.TemporaryDirectory() as tmp:
      path = os.path.join(tmp, 'root.txt')
      with open(path, 'w', encoding='utf-8-sig') as f:
        f.write(
            '// © 2016 and later: Unicode, Inc. and others.\n'
            'root{\n'
            '    units:alias{"/LOCALE/unitsShort"}\n'
            '}\n'
        )
      self.assertEqual(
          rb.read(path), {'root': {'units': rb.Alias('/LOCALE/unitsShort')}}
      )

  def test_icu_unit_data(self):
    en = rb.read(os.path.join(UNIT_DATA, 'en.txt'))['en']
    root = rb.read(os.path.join(UNIT_DATA, 'root.txt'))['root']
    self.assertIn('meter', en['units']['length'])
    self.assertIn('meter', en['unitsShort']['length'])
    self.assertEqual(root['units'], rb.Alias('/LOCALE/unitsShort'))
    self.assertIsInstance(root['unitsShort'], dict)


if __name__ == '__main__':
  unittest.main()
