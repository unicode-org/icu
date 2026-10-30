#!/usr/bin/env python3
# © 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

"""Tests for generate_measure_units.py.

The tests run the tool on copies of the ICU files in this checkout. They
don't need CLDR: they make the CLDR TSV files from the units in measunit.cpp.

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


def write_cldr_files(cldr_dir: str, units: list[tuple[str, str]]):
  """Writes CLDR TSV files with these (category, unit) pairs."""
  categories = sorted({category for category, _ in units})
  properties = os.path.join(cldr_dir, 'common', 'properties')
  os.makedirs(properties)
  with open(os.path.join(properties, 'unit_categories.tsv'), 'w') as f:
    f.write('# category_id\tcategory\tstatus\n')
    for i, category in enumerate(categories):
      f.write(f'{i}\t{category}\tregular\n')
  with open(os.path.join(properties, 'unit_quantities.tsv'), 'w') as f:
    f.write('# quantity_id\tquantity\n')
  with open(os.path.join(properties, 'units_info.tsv'), 'w') as f:
    f.write('# category_id\tcategory\tunit\tquantity_id\tquantity\tstatus\n')
    for category, unit in units:
      category_id = categories.index(category)
      f.write(f'{category_id}\t{category}\t{unit}\t\t\tregular\n')


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
    """Runs the tool with these CLDR units. Returns the new file contents."""
    cldr_dir = tempfile.mkdtemp(dir=self.tmp)
    write_cldr_files(cldr_dir, units)
    files = gen.update_files(self.icu_dir, cldr_dir)
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
