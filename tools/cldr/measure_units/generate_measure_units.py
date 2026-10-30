#!/usr/bin/env python3
# © 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

"""Updates the generated MeasureUnit code of ICU4C and ICU4J from CLDR data.

Reads the unit lists that CLDR generates in common/properties/
(units_info.tsv, unit_categories.tsv, and unit_quantities.tsv) and rewrites
the generated code in place:

* MeasureUnit.java, measunit.h, measunit.cpp: everything between the
  "Start generated" and "End generated" comments.
* MeasureUnitCompatibilityTest.java, measfmttest.cpp: a TestCompatible<N>()
  test for ICU major version N, when the list of constants changed.

Usage:
  python3 tools/cldr/measure_units/generate_measure_units.py --cldr-dir=<cldr>

See README.md next to this file, and
docs/processes/release/tasks/updating-measure-unit.md.
"""

from __future__ import annotations

import argparse
import dataclasses
import difflib
import os
import re
import sys
import textwrap

# The generated files, relative to the ICU root.
JAVA_CONSTANTS = (
    'icu4j/main/core/src/main/java/com/ibm/icu/util/MeasureUnit.java'
)
CPP_HEADER = 'icu4c/source/i18n/unicode/measunit.h'
CPP_SOURCE = 'icu4c/source/i18n/measunit.cpp'
JAVA_TEST = (
    'icu4j/main/common_tests/src/test/java/com/ibm/icu/dev/test/format/'
    'MeasureUnitCompatibilityTest.java'
)
CPP_TEST = 'icu4c/source/test/intltest/measfmttest.cpp'

# Other ICU files that the tool reads.
VERSION_HEADER = 'icu4c/source/common/unicode/uvernum.h'
CURRENCY_DATA = 'icu4c/source/data/misc/currencyNumericCodes.txt'

# The CLDR files that the tool reads, relative to the CLDR root.
CLDR_UNITS = 'common/properties/units_info.tsv'
CLDR_CATEGORIES = 'common/properties/unit_categories.tsv'
CLDR_QUANTITIES = 'common/properties/unit_quantities.tsv'

# The lines around the generated code.
JAVA_CONSTANTS_REGION = (
    '    // Start generated MeasureUnit constants',
    '    // End generated MeasureUnit constants',
)
CPP_HEADER_REGION = (
    '// Start generated createXXX methods',
    '// End generated createXXX methods',
)
CPP_SOURCE_REGION = (
    '// Start generated code for measunit.cpp',
    '// End generated code for measunit.cpp',
)

# The types that don't come from CLDR's unit categories. Currencies are in
# the C++ arrays so that currency units are fast to create, but they have no
# constants. "none" (C++ only) has one subtype, "", for the dimensionless base
# unit that the default constructor returns.
CURRENCY_TYPE = 'currency'
NONE_TYPE = 'none'

# Constant and function names are API, so they must not change. These CLDR
# units (long IDs: category-unit) get their names from another ID.
NAME_OVERRIDES = {
    # These CLDR IDs are too generic to be good names.
    'angle-revolution': 'revolution-angle',
    'temperature-generic': 'generic-temperature',
    # CLDR 37 renamed these units. ICU kept the old names.
    'acceleration-meter-per-square-second': 'meter-per-second-squared',
    'consumption-liter-per-100-kilometer': 'liter-per-100kilometers',
    'pressure-inch-ofhg': 'inch-hg',
    'pressure-millimeter-ofhg': 'millimeter-of-mercury',
    'pressure-pound-force-per-square-inch': 'pound-per-square-inch',
    'torque-pound-force-foot': 'pound-foot',
}

# ICU4J had TimeUnit constants before it had MeasureUnit constants, so the
# constants for these units are TimeUnit objects.
TIME_UNITS = frozenset((
    'duration-year',
    'duration-month',
    'duration-week',
    'duration-day',
    'duration-hour',
    'duration-minute',
    'duration-second',
))


@dataclasses.dataclass(frozen=True)
class Alias:
  """An old constant that ICU keeps after CLDR renamed its unit.

  It returns the renamed unit, and it comes right after the constant for the
  renamed unit.
  """

  unit: str  # The CLDR long ID of the renamed unit, e.g. "mass-tonne".
  old_unit: str  # The old CLDR unit ID. The old names come from it.
  renamed: str  # When CLDR renamed the unit, e.g. "CLDR 42 / ICU 72".
  # The metric-ton C++ comments have an older layout: no period after the
  # unit ID, and one "Also see" line instead of two.
  short_cpp_comment: bool = False


ALIASES = (
    Alias(
        'concentr-milligram-ofglucose-per-deciliter',
        'milligram-per-deciliter',
        'CLDR 39 / ICU 69',
    ),
    Alias('concentr-part-per-1e6', 'part-per-million', 'CLDR 48 / ICU 78'),
    Alias(
        'mass-tonne', 'metric-ton', 'CLDR 42 / ICU 72', short_cpp_comment=True
    ),
)

# ICU4J uses google-java-format (through Spotless), which wraps lines at 100
# columns. The tool writes Java code the way google-java-format would.
JAVA_LINE_LIMIT = 100

BACKWARD_COMPATIBILITY = ' // backward compatibility API'


class Error(Exception):
  """A problem in the input files that a person needs to look at."""


@dataclasses.dataclass(frozen=True)
class Constant:
  """A Java MeasureUnit constant, and the C++ createXXX() and getXXX()."""

  type: str  # The ICU type, which is the CLDR category, e.g. "length".
  subtype: str  # The ICU subtype: the CLDR unit ID without the category.
  type_index: int  # The index in gTypes.
  subtype_index: int  # The index within the type's part of gSubTypes.
  java_name: str  # e.g. "METER"
  cpp_name: str  # e.g. "Meter"
  alias: Alias | None = None
  target: Constant | None = None  # For an alias: the renamed unit's constant.

  @property
  def long_id(self) -> str:
    return f'{self.type}-{self.subtype}'


# Reading the input files.


def read_text(path: str) -> str:
  with open(path, encoding='utf-8', newline='') as f:
    return f.read()


def read_tsv(path: str, columns: tuple[str, ...]) -> list[dict[str, str]]:
  """Returns the rows of a CLDR TSV file as dicts keyed by column name.

  The first line that starts with "#" names the columns. Other lines that
  start with "#" are comments.
  """
  header = None
  rows = []
  for line in read_text(path).splitlines():
    if not line:
      continue
    if line.startswith('#'):
      if header is None:
        header = line[1:].strip().split('\t')
        missing = [c for c in columns if c not in header]
        if missing:
          raise Error(f'{path}: missing columns {missing}')
      continue
    cells = line.split('\t')
    if header is None or len(cells) != len(header):
      raise Error(f'{path}: unexpected line: {line!r}')
    rows.append(dict(zip(header, cells)))
  return rows


def is_current(row: dict[str, str], path: str) -> bool:
  """Whether a CLDR row is not deprecated. "new" means new in this release."""
  status = row['status']
  if status not in ('new', 'regular', 'deprecated'):
    raise Error(f'{path}: unknown status {status!r} in {row}')
  return status != 'deprecated'


def read_cldr_units(cldr_dir: str) -> list[tuple[str, str]]:
  """Returns the (category, unit) pairs of the current CLDR units.

  Also checks that units_info.tsv agrees with unit_categories.tsv and
  unit_quantities.tsv.
  """
  units_path = os.path.join(cldr_dir, CLDR_UNITS)
  categories_path = os.path.join(cldr_dir, CLDR_CATEGORIES)
  quantities_path = os.path.join(cldr_dir, CLDR_QUANTITIES)
  categories = {
      row['category_id']: row
      for row in read_tsv(
          categories_path, ('category_id', 'category', 'status')
      )
  }
  quantities = {
      row['quantity_id']: row['quantity']
      for row in read_tsv(quantities_path, ('quantity_id', 'quantity'))
  }
  units = []
  for row in read_tsv(
      units_path,
      ('category_id', 'category', 'unit', 'quantity_id', 'quantity', 'status'),
  ):
    category = categories.get(row['category_id'])
    if category is None or category['category'] != row['category']:
      raise Error(f'{units_path}: category not in {categories_path}: {row}')
    quantity_id = row['quantity_id']
    if quantity_id and quantities.get(quantity_id) != row['quantity']:
      raise Error(f'{units_path}: quantity not in {quantities_path}: {row}')
    if not is_current(row, units_path):
      continue
    if not is_current(category, categories_path):
      raise Error(f'{units_path}: current unit in a deprecated category: {row}')
    units.append((row['category'], row['unit']))
  if len(set(units)) != len(units):
    raise Error(f'{units_path}: duplicate units')
  return units


def read_icu_major_version(icu_dir: str) -> int:
  text = read_text(os.path.join(icu_dir, VERSION_HEADER))
  match = re.search(r'^#define U_ICU_VERSION_MAJOR_NUM (\d+)$', text, re.M)
  if not match:
    raise Error(f'{VERSION_HEADER}: no U_ICU_VERSION_MAJOR_NUM')
  return int(match[1])


def read_currencies(icu_dir: str) -> list[str]:
  """Returns the currency codes in ICU's currency numeric code data.

  This data comes from ISO 4217, not from CLDR. ICU4J's
  MeasureUnit.getAvailable("currency") returns the same codes.
  """
  text = read_text(os.path.join(icu_dir, CURRENCY_DATA))
  codes = re.findall(r'^\s+([A-Z]{3}):int\{\d+\}$', text, re.M)
  if not codes or len(set(codes)) != len(codes):
    raise Error(f'{CURRENCY_DATA}: no currency codes, or duplicate codes')
  return sorted(codes)


# Names.


def java_name(name_id: str) -> str:
  """Returns the Java constant name, e.g. "part-per-1e6" -> "PART_PER_1E6"."""
  return name_id.upper().replace('-', '_')


def cpp_name(name_id: str) -> str:
  """Returns the C++ function name suffix, e.g. "part-per-1e6" -> "PartPer1E6".

  A letter is upper case at the start, after "-", and after a digit.
  """
  result = []
  upper = True
  for c in name_id:
    if c == '-':
      upper = True
    elif c.isdigit():
      upper = True
      result.append(c)
    elif upper:
      result.append(c.upper())
      upper = False
    else:
      result.append(c)
  return ''.join(result)


# The data model.


def sorted_types(
    units: list[tuple[str, str]], currencies: list[str]
) -> list[tuple[str, list[str]]]:
  """Returns the types and their subtypes, sorted as in the C++ arrays."""
  types: dict[str, list[str]] = {}
  for category, unit in units:
    types.setdefault(category, []).append(unit)
  for extra in (CURRENCY_TYPE, NONE_TYPE):
    if extra in types:
      raise Error(f'CLDR has a unit category named {extra!r}')
  types[CURRENCY_TYPE] = currencies
  types[NONE_TYPE] = ['']
  return sorted((t, sorted(subtypes)) for t, subtypes in types.items())


def make_constants(types: list[tuple[str, list[str]]]) -> list[Constant]:
  """Returns the constants in the order of the generated code."""
  aliases = {alias.unit: alias for alias in ALIASES}
  constants = []
  for type_index, (unit_type, subtypes) in enumerate(types):
    if unit_type in (CURRENCY_TYPE, NONE_TYPE):
      continue
    for subtype_index, subtype in enumerate(subtypes):
      long_id = f'{unit_type}-{subtype}'
      name_id = NAME_OVERRIDES.get(long_id, subtype)
      constant = Constant(
          unit_type,
          subtype,
          type_index,
          subtype_index,
          java_name(name_id),
          cpp_name(name_id),
      )
      constants.append(constant)
      alias = aliases.get(long_id)
      if alias:
        constants.append(
            dataclasses.replace(
                constant,
                java_name=java_name(alias.old_unit),
                cpp_name=cpp_name(alias.old_unit),
                alias=alias,
                target=constant,
            )
        )
  long_ids = {c.long_id for c in constants}
  for long_id in list(NAME_OVERRIDES) + list(aliases):
    if long_id not in long_ids:
      raise Error(
          f'CLDR no longer has the unit {long_id}. Its ICU constants are API:'
          ' update NAME_OVERRIDES or ALIASES so that they stay.'
      )
  for kind in ('java_name', 'cpp_name'):
    names = [getattr(c, kind) for c in constants]
    duplicates = sorted({n for n in names if names.count(n) > 1})
    if duplicates:
      raise Error(f'Two units have the same name: {duplicates}')
  return constants


# Reading the API tags that are in the files now.

# A doc comment followed by a Java constant or a C++ function declaration.
JAVA_DECLARATION = re.compile(
    r'^    /\*\*\n((?:     \*.*\n)*?)     \*/\n'
    r'(?:    @Deprecated\n)?'
    r'    public static final (?:MeasureUnit|TimeUnit) (\w+) =',
    re.M,
)
CPP_DECLARATION = re.compile(
    r'^    /\*\*\n((?:     \*.*\n)*?)     \*/\n'
    r'    static MeasureUnit '
    r'(?:\*(create\w+)\(UErrorCode &status\)|(get\w+)\(\));$',
    re.M,
)


def api_tags(comment: str) -> list[str]:
  """Returns the lines of a doc comment from its first API tag on.

  The API tags are @stable, @draft, @deprecated, and so on. @param is not one.
  The lines lose their "     * " prefix but keep any further indentation.
  """
  lines = [line[7:] for line in comment.splitlines()]
  for i, line in enumerate(lines):
    if line.startswith('@') and not line.startswith('@param'):
      return lines[i:]
  raise Error(f'No API tag in this doc comment:\n{comment}')


def read_tags(
    region: str, pattern: re.Pattern[str], declaration: str, path: str
) -> dict[str, list[str]]:
  """Maps the names declared in a generated region to their API tags."""
  tags = {
      m[2] or m[3]: api_tags(m[1]) for m in pattern.finditer(region)
  }
  if len(tags) != region.count(declaration):
    raise Error(f'{path}: some declarations have no doc comment like usual')
  return tags


def tags_or_draft(
    tags: dict[str, list[str]], name: str, version: int
) -> list[str]:
  """Returns the API tags of a name, or draft tags for a new name."""
  return tags.get(name, [f'@draft ICU {version}'])


def hiding_macro(tags: list[str]) -> str | None:
  """Returns the C++ macro that hides an API with these tags, if any."""
  status = tags[0].split()[0]
  macros = {
      '@stable': None,
      '@draft': 'U_HIDE_DRAFT_API',
      '@deprecated': 'U_HIDE_DEPRECATED_API',
  }
  if status not in macros:
    raise Error(f'Unexpected API tag: {tags[0]}')
  return macros[status]


def check_no_removals(old_names: set[str], new_names: set[str], path: str):
  removed = sorted(old_names - new_names)
  if removed:
    raise Error(
        f'{path}: these constants would be removed because CLDR no longer has'
        f' their units: {removed}. They are API: keep them with ALIASES or'
        ' NAME_OVERRIDES, or remove them by hand if they are draft API.'
    )


# Writing the generated code.


def doc_comment(lines: list[str]) -> list[str]:
  """Returns a doc comment indented by 4 spaces."""
  result = ['    /**']
  result += [f'     * {line}' if line else '     *' for line in lines]
  result.append('     */')
  return result


def java_doc_comment(text: str, tags: list[str]) -> list[str]:
  """Returns a Javadoc comment as google-java-format writes it.

  The text is wrapped to the line limit, and an empty line separates it from
  the API tags.
  """
  width = JAVA_LINE_LIMIT - len('     * ')
  lines = textwrap.wrap(
      text, width, break_long_words=False, break_on_hyphens=False
  )
  return doc_comment(lines + [''] + tags)


def java_field(
    java_type: str, name: str, call: str, args: list[str]
) -> list[str]:
  """Returns a constant declaration as google-java-format writes it.

  The value is a method call: call(args[0], args[1], ...).
  """
  head = f'    public static final {java_type} {name} ='
  value = f'{call}({", ".join(args)});'
  indent = ' ' * 12
  for lines in (
      [f'{head} {value}'],
      [head, f'{indent}{value}'],
      [head, f'{indent}{call}(', f'{indent}        {", ".join(args)});'],
  ):
    if all(len(line) <= JAVA_LINE_LIMIT for line in lines):
      return lines
  return (
      [head, f'{indent}{call}(']
      + [f'{indent}        {arg},' for arg in args[:-1]]
      + [f'{indent}        {args[-1]});']
  )


def java_constants(
    constants: list[Constant], tags: dict[str, list[str]], version: int
) -> list[str]:
  """Returns the lines between the markers in MeasureUnit.java."""
  lines = ['']
  for c in constants:
    c_tags = tags_or_draft(tags, c.java_name, version)
    if c.alias:
      text = (
          f'Constant for unit of {c.type}: {c.alias.old_unit}'
          f' (renamed to {c.subtype} in {c.alias.renamed}).'
      )
    else:
      text = f'Constant for unit of {c.type}: {c.subtype}'
    lines += java_doc_comment(text, c_tags)
    if any(tag.startswith('@deprecated') for tag in c_tags):
      lines.append('    @Deprecated')
    call = 'MeasureUnit.internalGetInstance'
    args = [f'"{c.type}"', f'"{c.subtype}"']
    if c.long_id in TIME_UNITS and not c.alias:
      lines += java_field('TimeUnit', c.java_name, f'(TimeUnit) {call}', args)
    else:
      lines += java_field('MeasureUnit', c.java_name, call, args)
    lines.append('')
  return lines


def cpp_comments(c: Constant) -> tuple[list[str], list[str]]:
  """Returns the doc comment text of createXXX() and getXXX(), without tags."""
  name = c.cpp_name
  if not c.alias:
    unit = f'unit of {c.type}: {c.subtype}.'
    return (
        [
            f'Returns by pointer, {unit}',
            'Caller owns returned value and must free it.',
            f'Also see {{@link #get{name}()}}.',
            '@param status ICU error code.',
        ],
        [
            f'Returns by value, {unit}',
            f'Also see {{@link #create{name}()}}.',
        ],
    )
  target = c.target.cpp_name
  renamed = f'(renamed to {c.subtype} in {c.alias.renamed}).'
  unit = f'unit of {c.type}: {c.alias.old_unit}'
  if c.alias.short_cpp_comment:
    create_see = [
        f'Also see {{@link #get{name}()}} and {{@link #create{target}()}}.'
    ]
    get_see = [
        f'Also see {{@link #create{name}()}} and {{@link #get{target}()}}.'
    ]
  else:
    unit += '.'
    create_see = [
        f'Also see {{@link #create{target}()}}.',
        f'Also see {{@link #get{name}()}}.',
    ]
    get_see = [
        f'Also see {{@link #get{target}()}}.',
        f'Also see {{@link #create{name}()}}.',
    ]
  return (
      [
          f'Returns by pointer, {unit}',
          renamed,
          'Caller owns returned value and must free it.',
      ]
      + create_see
      + ['@param status ICU error code.'],
      [f'Returns by value, {unit}', renamed] + get_see,
  )


def cpp_header(
    constants: list[Constant], tags: dict[str, list[str]], version: int
) -> list[str]:
  """Returns the lines between the markers in measunit.h."""
  lines = ['']
  for c in constants:
    create = f'create{c.cpp_name}'
    get = f'get{c.cpp_name}'
    create_tags = tags_or_draft(tags, create, version)
    get_tags = tags_or_draft(tags, get, version)
    macro = hiding_macro(create_tags)
    if hiding_macro(get_tags) != macro:
      raise Error(f'{create}() and {get}() have different API tags')
    create_text, get_text = cpp_comments(c)
    if macro:
      lines.append(f'#ifndef {macro}')
    lines += doc_comment(create_text + create_tags)
    lines.append(f'    static MeasureUnit *{create}(UErrorCode &status);')
    lines.append('')
    lines += doc_comment(get_text + get_tags)
    lines.append(f'    static MeasureUnit {get}();')
    if macro == 'U_HIDE_DEPRECATED_API':
      lines.append(f'#endif  /* {macro} */')
    elif macro:
      lines.append(f'#endif /* {macro} */')
    lines.append('')
  return lines


def cpp_source(
    types: list[tuple[str, list[str]]], constants: list[Constant]
) -> list[str]:
  """Returns the lines between the markers in measunit.cpp."""
  type_names = [t for t, _ in types]
  subtypes = [s for _, subs in types for s in subs]
  lines = [
      '',
      '// Maps from Type ID to offset in gSubTypes.',
      'static const int32_t gOffsets[] = {',
  ]
  offset = 0
  for _, subs in types:
    lines.append(f'    {offset},')
    offset += len(subs)
  lines += [
      f'    {offset}',
      '};',
      '',
      'static const int32_t kCurrencyOffset = '
      f'{type_names.index(CURRENCY_TYPE)};',
      '',
      '// Must be sorted alphabetically.',
      'static const char * const gTypes[] = {',
      ',\n'.join(f'    "{t}"' for t in type_names),
      '};',
      '',
      '// Must be grouped by type and sorted alphabetically within each type.',
      'static const char * const gSubTypes[] = {',
      ',\n'.join(f'    "{s}"' for s in subtypes),
      '};',
      '',
      '// Shortcuts to the base unit in order to make the default constructor'
      ' fast',
      f'static const int32_t kBaseTypeIdx = {type_names.index(NONE_TYPE)};',
      'static const int32_t kBaseSubTypeIdx = 0;',
      '',
  ]
  for c in constants:
    indices = f'{c.type_index}, {c.subtype_index}'
    lines += [
        f'MeasureUnit *MeasureUnit::create{c.cpp_name}(UErrorCode &status) {{',
        f'    return MeasureUnit::create({indices}, status);',
        '}',
        '',
        f'MeasureUnit MeasureUnit::get{c.cpp_name}() {{',
        f'    return MeasureUnit({indices});',
        '}',
        '',
    ]
  return lines


def java_compatibility_test(
    constants: list[Constant], version: int
) -> list[str]:
  lines = [
      '    @Test',
      f'    public void TestCompatible{version}() {{',
      '        MeasureUnit[] units = {',
  ]
  for c in constants:
    comment = BACKWARD_COMPATIBILITY if c.alias else ''
    lines.append(f'            MeasureUnit.{c.java_name},{comment}')
  lines += [
      '        };',
      f'        assertEquals("", {len(constants)}, units.length);',
      '    }',
  ]
  return lines


def cpp_compatibility_test(
    constants: list[Constant], version: int
) -> list[str]:
  lines = [
      f'void MeasureFormatTest::TestCompatible{version}() {{',
      '    UErrorCode status = U_ZERO_ERROR;',
      '    LocalPointer<MeasureUnit> measureUnit;',
      '    MeasureUnit measureUnitValue;',
  ]
  for c in constants:
    comment = BACKWARD_COMPATIBILITY if c.alias else ''
    create = f'MeasureUnit::create{c.cpp_name}(status)'
    lines += [
        f'    measureUnit.adoptInstead({create});{comment}',
        f'    measureUnitValue = MeasureUnit::get{c.cpp_name}();{comment}',
    ]
  lines += ['    assertSuccess("", status);', '}']
  return lines


# Updating the files.


def joined(lines: list[str]) -> str:
  return ''.join(line + '\n' for line in lines)


def find_region(
    text: str, markers: tuple[str, str], path: str
) -> tuple[int, int]:
  """Returns where the text between the marker lines starts and ends."""
  positions = []
  for marker in markers:
    found = [
        m.start() for m in re.finditer(f'^{re.escape(marker)}$', text, re.M)
    ]
    if len(found) != 1:
      raise Error(f'{path}: expected one line {marker!r}, found {len(found)}')
    positions.append(found[0])
  start = positions[0] + len(markers[0]) + 1
  if start > positions[1]:
    raise Error(f'{path}: the markers are out of order')
  return start, positions[1]


def update_compatibility_test(
    text: str,
    path: str,
    test_pattern: str,
    name_pattern: str,
    new_test: list[str],
    names: list[str],
    version: int,
    extra_lines: tuple[str, ...] = (),
) -> str:
  """Adds or updates the TestCompatible<version>() test.

  Nothing changes when the newest test already uses the same constants.
  Otherwise the new test replaces the newest test if that one is for this
  version too, or else comes after it. extra_lines are formats of lines (like
  a C++ declaration) that each test has once; the new test's line goes after
  the newest test's line.
  """
  tests = list(re.finditer(test_pattern, text, re.M | re.S))
  if not tests:
    raise Error(f'{path}: no TestCompatible tests')
  newest = max(tests, key=lambda m: int(m[1]))
  newest_version = int(newest[1])
  if newest_version > version:
    raise Error(f'{path}: has TestCompatible{newest_version} for a newer ICU')
  if re.findall(name_pattern, newest[0]) == names:
    return text
  start, end = newest.span()
  if newest_version == version:
    return text[:start] + joined(new_test) + text[end:]
  text = text[:end] + '\n' + joined(new_test) + text[end:]
  for line in extra_lines:
    old_line = line.format(newest_version) + '\n'
    if text.count(old_line) != 1:
      raise Error(f'{path}: expected one line {old_line!r}')
    text = text.replace(old_line, old_line + line.format(version) + '\n')
  return text


def update_files(icu_dir: str, cldr_dir: str) -> dict[str, tuple[str, str]]:
  """Returns the old and the new text of each generated file."""
  version = read_icu_major_version(icu_dir)
  types = sorted_types(read_cldr_units(cldr_dir), read_currencies(icu_dir))
  constants = make_constants(types)
  java_names = [c.java_name for c in constants]
  cpp_names = [c.cpp_name for c in constants]

  def java_constants_file(text):
    start, end = find_region(text, JAVA_CONSTANTS_REGION, JAVA_CONSTANTS)
    tags = read_tags(
        text[start:end],
        JAVA_DECLARATION,
        'public static final ',
        JAVA_CONSTANTS,
    )
    check_no_removals(set(tags), set(java_names), JAVA_CONSTANTS)
    lines = java_constants(constants, tags, version)
    return text[:start] + joined(lines) + text[end:]

  def cpp_header_file(text):
    start, end = find_region(text, CPP_HEADER_REGION, CPP_HEADER)
    tags = read_tags(
        text[start:end], CPP_DECLARATION, 'static MeasureUnit ', CPP_HEADER
    )
    old_names = {name[6:] for name in tags if name.startswith('create')}
    check_no_removals(old_names, set(cpp_names), CPP_HEADER)
    lines = cpp_header(constants, tags, version)
    return text[:start] + joined(lines) + text[end:]

  def cpp_source_file(text):
    start, end = find_region(text, CPP_SOURCE_REGION, CPP_SOURCE)
    return text[:start] + joined(cpp_source(types, constants)) + text[end:]

  def java_test_file(text):
    return update_compatibility_test(
        text,
        JAVA_TEST,
        r'^    @Test\n    public void TestCompatible(\d+)\(\) \{.*?^    \}\n',
        r'MeasureUnit\.(\w+),',
        java_compatibility_test(constants, version),
        java_names,
        version,
    )

  def cpp_test_file(text):
    return update_compatibility_test(
        text,
        CPP_TEST,
        r'^void MeasureFormatTest::TestCompatible(\d+)\(\) \{.*?^\}\n',
        r'MeasureUnit::create(\w+)\(status\)',
        cpp_compatibility_test(constants, version),
        cpp_names,
        version,
        (
            '    void TestCompatible{}();',
            '    TESTCASE_AUTO(TestCompatible{});',
        ),
    )

  files = {}
  for path, update in (
      (JAVA_CONSTANTS, java_constants_file),
      (CPP_HEADER, cpp_header_file),
      (CPP_SOURCE, cpp_source_file),
      (JAVA_TEST, java_test_file),
      (CPP_TEST, cpp_test_file),
  ):
    old = read_text(os.path.join(icu_dir, path))
    files[path] = (old, update(old))
  return files


def main(argv: list[str] | None = None) -> int:
  tool_dir = os.path.dirname(os.path.abspath(__file__))
  parser = argparse.ArgumentParser(
      description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
  )
  parser.add_argument(
      '--cldr-dir',
      default=os.environ.get('CLDR_DIR'),
      help='the CLDR root directory (default: $CLDR_DIR)',
  )
  parser.add_argument(
      '--icu-dir',
      default=os.path.normpath(os.path.join(tool_dir, '..', '..', '..')),
      help='the ICU root directory (default: the one that has this tool)',
  )
  parser.add_argument(
      '--check',
      action='store_true',
      help="don't write; print a diff and exit with 1 if a file would change",
  )
  args = parser.parse_args(argv)
  if not args.cldr_dir:
    parser.error('set --cldr-dir or CLDR_DIR')

  try:
    files = update_files(args.icu_dir, args.cldr_dir)
  except (Error, OSError) as e:
    print(f'error: {e}', file=sys.stderr)
    return 2

  changed = False
  for path, (old, new) in files.items():
    if old == new:
      print(f'Unchanged: {path}')
      continue
    changed = True
    if args.check:
      print(f'Would change: {path}')
      sys.stdout.writelines(
          difflib.unified_diff(
              old.splitlines(keepends=True),
              new.splitlines(keepends=True),
              f'a/{path}',
              f'b/{path}',
          )
      )
    else:
      full_path = os.path.join(args.icu_dir, path)
      with open(full_path, 'w', encoding='utf-8', newline='') as f:
        f.write(new)
      print(f'Updated: {path}')
  return 1 if args.check and changed else 0


if __name__ == '__main__':
  sys.exit(main())
