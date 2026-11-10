#!/usr/bin/env python3
# © 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

"""Tests for generate_measure_units.py.

The tests run the tool on copies of the ICU files in this checkout, with unit
data that they write from the units in measunit.cpp.

  python3 tools/cldr/measure_units/test_generate_measure_units.py
"""

import os
import re
import shutil
import tempfile
import unittest

import generate_measure_units as gen

ICU_DIR = os.path.normpath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', '..')
)

INPUT_FILES = (
    gen.JAVA_CONSTANTS,
    gen.CPP_HEADER,
    gen.CPP_SOURCE,
    gen.JAVA_TEST,
    gen.CPP_TEST,
    gen.VERSION_HEADER,
    gen.CURRENCY_DATA,
)


def units_in_icu() -> list[tuple[str, str]]:
  """Returns the (type, subtype) pairs of the units in measunit.cpp."""
  text = gen.read_text(os.path.join(ICU_DIR, gen.CPP_SOURCE))

  def array(name):
    return re.search(name + r'\[\] = \{(.*?)\};', text, re.S)[1]

  types = re.findall(r'"(.*)"', array('gTypes'))
  subtypes = re.findall(r'"(.*)"', array('gSubTypes'))
  offsets = [int(n) for n in re.findall(r'\d+', array('gOffsets'))]
  return [
      (t, s)
      for i, t in enumerate(types)
      if t not in (gen.CURRENCY_TYPE, gen.NONE_TYPE)
      for s in subtypes[offsets[i] : offsets[i + 1]]
  ]


def write_file(icu_dir: str, path: str, text: str):
  full_path = os.path.join(icu_dir, path)
  os.makedirs(os.path.dirname(full_path), exist_ok=True)
  with open(full_path, 'w', encoding='utf-8') as f:
    f.write('\ufeff// © 2016 and later: Unicode, Inc. and others.\n' + text)


def write_unit_data(icu_dir: str, units: list[tuple[str, str]]):
  """Writes en.txt and root.txt unit data with these (type, subtype) pairs.

  Like ICU's real data, the "-person" units are only in root, as aliases.
  """
  en: dict[str, list[str]] = {}
  root: dict[str, list[str]] = {}
  for unit_type, subtype in units:
    data = root if subtype.endswith('-person') else en
    data.setdefault(unit_type, []).append(subtype)
  en_lines = ['en{', '    units{']
  for unit_type, subtypes in en.items():
    en_lines.append(f'        {unit_type}{{')
    for subtype in subtypes:
      en_lines += [
          f'            {subtype}{{',
          f'                dnam{{"{subtype}"}}',
          f'                other{{"{{0}} {subtype}"}}',
          '            }',
      ]
    en_lines.append('        }')
  en_lines += [
      '        compound{',
      '            per{"{0} per {1}"}',
      '        }',
      '        coordinate{',
      '            east{',
      '                other{"{0} east"}',
      '            }',
      '        }',
      '    }',
      '}',
  ]
  root_lines = [
      'root{',
      '    units:alias{"/LOCALE/unitsShort"}',
      '    unitsShort{',
  ]
  for unit_type, subtypes in root.items():
    root_lines.append(f'        {unit_type}{{')
    for subtype in subtypes:
      target = subtype.removesuffix('-person')
      root_lines.append(
          f'            {subtype}:alias{{"/LOCALE/unitsShort/{unit_type}/'
          f'{target}"}}'
      )
    root_lines.append('        }')
  root_lines += ['    }', '}']
  for locale, lines in (('en', en_lines), ('root', root_lines)):
    write_file(icu_dir, gen.UNIT_DATA.format(locale), '\n'.join(lines) + '\n')


class NamesTest(unittest.TestCase):

  def test_java_name(self):
    self.assertEqual(gen.java_name('g-force'), 'G_FORCE')
    self.assertEqual(gen.java_name('part-per-1e6'), 'PART_PER_1E6')

  def test_cpp_name(self):
    self.assertEqual(gen.cpp_name('g-force'), 'GForce')
    self.assertEqual(gen.cpp_name('part-per-1e6'), 'PartPer1E6')
    self.assertEqual(
        gen.cpp_name('liter-per-100kilometers'), 'LiterPer100Kilometers'
    )


class JavaFormatTest(unittest.TestCase):
  """The expected lines are what google-java-format 1.32.0 (AOSP) writes."""

  def field(self, name, unit_type, subtype):
    return gen.java_field(
        'MeasureUnit',
        name,
        'MeasureUnit.internalGetInstance',
        [f'"{unit_type}"', f'"{subtype}"'],
    )

  def test_field_on_one_line(self):
    self.assertEqual(
        self.field('DEGREE', 'angle', 'degree'),
        [
            '    public static final MeasureUnit DEGREE ='
            ' MeasureUnit.internalGetInstance("angle", "degree");'
        ],
    )

  def test_field_wrapped_after_equals(self):
    self.assertEqual(
        self.field('G_FORCE', 'acceleration', 'g-force'),
        [
            '    public static final MeasureUnit G_FORCE =',
            '            MeasureUnit.internalGetInstance("acceleration",'
            ' "g-force");',
        ],
    )

  def test_field_wrapped_before_arguments(self):
    self.assertEqual(
        self.field('A' * 38, 'acceleration', 'a' * 39),
        [
            f'    public static final MeasureUnit {"A" * 38} =',
            '            MeasureUnit.internalGetInstance(',
            f'                    "acceleration", "{"a" * 39}");',
        ],
    )

  def test_field_with_one_argument_per_line(self):
    self.assertEqual(
        self.field('A' * 38, 'acceleration', 'a' * 68),
        [
            f'    public static final MeasureUnit {"A" * 38} =',
            '            MeasureUnit.internalGetInstance(',
            '                    "acceleration",',
            f'                    "{"a" * 68}");',
        ],
    )

  def test_doc_comment(self):
    self.assertEqual(
        gen.java_doc_comment(
            'Constant for unit of concentr: part-per-million (renamed to'
            ' part-per-1e6 in CLDR 48 / ICU 78).',
            ['@stable ICU 57'],
        ),
        [
            '    /**',
            '     * Constant for unit of concentr: part-per-million (renamed to'
            ' part-per-1e6 in CLDR 48 / ICU',
            '     * 78).',
            '     *',
            '     * @stable ICU 57',
            '     */',
        ],
    )


class UnitDataTest(unittest.TestCase):

  def test_parse_resource_bundle(self):
    text = (
        '// A comment with { and "\n'
        'en{\n'
        '    /* Another comment */\n'
        '    Version{"49"}\n'
        '    units{\n'
        '        length{\n'
        '            meter{\n'
        '                dnam{"m{eter}"}\n'
        '                one{"{0} \\"meter\\""}\n'
        '                other{"{0} "\n'
        '                      "meters"}\n'
        '            }\n'
        '            yard:alias{"/LOCALE/units/length/foot"}\n'
        '        }\n'
        '        count:int{5}\n'
        '        list{"a", "b"}\n'
        '    }\n'
        '}\n'
    )
    self.assertEqual(
        gen.parse_resource_bundle(text, 'en.txt'),
        {
            'en': {
                'Version': None,
                'units': {
                    'length': {
                        'meter': {'dnam': None, 'one': None, 'other': None},
                        'yard': gen.RbAlias('/LOCALE/units/length/foot'),
                    },
                    'count': None,
                    'list': None,
                },
            }
        },
    )

  def test_parse_errors(self):
    for text in ('en{', 'en{ units{ length{ } }', 'en{}}', '"en"{}'):
      with self.subTest(text=text):
        with self.assertRaises(gen.Error):
          gen.parse_resource_bundle(text, 'en.txt')

  def test_fallback_through_root_alias(self):
    """en's units, then en's unitsShort, then root's unitsShort."""
    icu_dir = tempfile.mkdtemp()
    self.addCleanup(shutil.rmtree, icu_dir)
    write_file(
        icu_dir,
        gen.UNIT_DATA.format('en'),
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
    )
    write_file(
        icu_dir,
        gen.UNIT_DATA.format('root'),
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
        gen.read_units(icu_dir),
        [('duration', 'day-person'), ('length', 'foot'), ('length', 'meter')],
    )

  def test_icu_data(self):
    units = gen.read_units(ICU_DIR)
    self.assertIn(('length', 'meter'), units)
    self.assertIn(('duration', 'day-person'), units)
    self.assertFalse({t for t, _ in units} & gen.NOT_UNIT_TYPES)


class GenerateTest(unittest.TestCase):

  def setUp(self):
    super().setUp()
    self.tmp = tempfile.mkdtemp()
    self.addCleanup(shutil.rmtree, self.tmp)
    self.icu_dir = os.path.join(self.tmp, 'icu')
    for path in INPUT_FILES:
      copy = os.path.join(self.icu_dir, path)
      os.makedirs(os.path.dirname(copy), exist_ok=True)
      shutil.copy(os.path.join(ICU_DIR, path), copy)
    self.units = units_in_icu()
    self.version = gen.read_icu_major_version(ICU_DIR)

  def generate(self, units):
    """Runs the tool with these units. Returns the new file contents."""
    write_unit_data(self.icu_dir, units)
    files = gen.update_files(self.icu_dir)
    for path, (_, new) in files.items():
      with open(
          os.path.join(self.icu_dir, path), 'w', encoding='utf-8', newline=''
      ) as f:
        f.write(new)
    return files

  def assert_unchanged(self, files):
    for path, (old, new) in files.items():
      self.assertTrue(old == new, f'{path} changed')

  def test_same_units(self):
    self.assert_unchanged(self.generate(self.units))

  def test_new_unit(self):
    files = self.generate(self.units + [('length', 'smoot')])
    self.assertIn(
        '     * Constant for unit of length: smoot\n'
        '     *\n'
        f'     * @draft ICU {self.version}\n'
        '     */\n'
        '    public static final MeasureUnit SMOOT ='
        ' MeasureUnit.internalGetInstance("length", "smoot");\n',
        files[gen.JAVA_CONSTANTS][1],
    )
    self.assertIn(
        '#ifndef U_HIDE_DRAFT_API\n'
        '    /**\n'
        '     * Returns by pointer, unit of length: smoot.\n',
        files[gen.CPP_HEADER][1],
    )
    self.assertIn(
        'MeasureUnit MeasureUnit::getSmoot() {\n', files[gen.CPP_SOURCE][1]
    )
    java_test = files[gen.JAVA_TEST][1]
    self.assertEqual(java_test.count(f' TestCompatible{self.version}()'), 1)
    self.assertIn('            MeasureUnit.SMOOT,\n', java_test)
    cpp_test = files[gen.CPP_TEST][1]
    self.assertEqual(cpp_test.count(f'TestCompatible{self.version}'), 3)
    self.assertIn('MeasureUnit::createSmoot(status)', cpp_test)
    # The second run keeps everything, including the new test.
    self.assert_unchanged(self.generate(self.units + [('length', 'smoot')]))

  def test_removed_unit(self):
    units = [unit for unit in self.units if unit != ('length', 'furlong')]
    with self.assertRaisesRegex(gen.Error, 'FURLONG'):
      self.generate(units)

  def test_removed_alias_target(self):
    units = [unit for unit in self.units if unit != ('mass', 'tonne')]
    with self.assertRaisesRegex(gen.Error, 'mass-tonne'):
      self.generate(units)


if __name__ == '__main__':
  unittest.main()
