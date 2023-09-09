#!/usr/bin/python3 -B
# -*- coding: utf-8 -*-
# © 2016 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
# Copyright (c) 2009-2016 International Business Machines
# Corporation and others. All Rights Reserved.
#
#   file name:  preparseucd.py
#   encoding:   US-ASCII
#   tab size:   8 (not used)
#   indentation:4
#
#   created on: 2011nov03 (forked from ucdcopy.py)
#   created by: Markus W. Scherer
#
# Copies Unicode Character Database (UCD) files from a tree
# of files downloaded from (for example) ftp://www.unicode.org/Public/6.1.0/
# to ICU's source/data/unidata/ and source/test/testdata/
# and modifies some of the files to make them more compact.
# Parses them and writes unidata/ppucd.txt (PreParsed UCD) with simple syntax.
#
# Invoke with two command-line parameters:
# 1. source folder with UCD & idna files
# 2. ICU source root folder (ICU 59+ combined trunk with icu4c, icu4j, tools)
#
# Sample invocation:
#   ~/svn.icu/tools/trunk/src/unicode$ py/preparseucd.py ~/uni61/20120118 ~/svn.icu/trunk/src

import array
import bisect
import codecs
import os
import os.path
import re
import shutil
import sys

# Unicode version ---------------------------------------------------------- ***

_ucd_version = "?"

# ISO 15924 script codes --------------------------------------------------- ***

# Script codes from ISO 15924 http://www.unicode.org/iso15924/codechanges.html
# that are not yet in the UCD.
_scripts_only_in_iso15924 = (
    "Afak", "Blis", "Cirt", "Cyrs",
    "Egyd", "Egyh", "Geok",
    "Hanb", "Hans", "Hant",
    "Inds", "Jamo", "Jpan", "Jurc", "Kore", "Kpel", "Latf", "Latg", "Loma",
    "Maya", "Moon", "Nkgb", "Phlv", "Roro",
    "Sara", "Syre", "Syrj", "Syrn",
    "Teng", "Visp", "Wole", "Zmth", "Zsye", "Zsym", "Zxxx"
)

# Properties --------------------------------------------------------------- ***

# Properties that we do not want to store in ppucd.txt.
# Not a frozenset so that we can add aliases for simpler subsequent testing.
_ignored_properties = set((
  # Other_Xyz only contribute to Xyz, store only the latter.
  "OAlpha",
  "ODI",
  "OGr_Ext",
  "OIDC",
  "OIDS",
  "OLower",
  "OMath",
  "OUpper",
  # Further properties that just contribute to others.
  "CE",  # Composition_Exclusion just contributes to Full_Composition_Exclusion.
  "JSN",
  # These properties just don't seem useful.
  # They are deprecated since Unicode 6.0.
  "XO_NFC",
  "XO_NFD",
  "XO_NFKC",
  "XO_NFKD",
  # ICU does not use Unihan properties.
  "cjkAccountingNumeric",
  "cjkOtherNumeric",
  "cjkPrimaryNumeric",
  "cjkCompatibilityVariant",
  "cjkIICore",
  "cjkIRG_GSource",
  "cjkIRG_HSource",
  "cjkIRG_JSource",
  "cjkIRG_KPSource",
  "cjkIRG_KSource",
  "cjkIRG_MSource",
  "cjkIRG_SSource",
  "cjkIRG_TSource",
  "cjkIRG_UKSource",
  "cjkIRG_USource",
  "cjkIRG_VSource",
  "cjkRSUnicode"
))

# These properties (short names) map code points to
# strings or other unusual values (property types String or Miscellaneous)
# that cannot be block-compressed (or would be confusing).
_uncompressible_props = frozenset((
  "bmg", "bpb", "cf", "Conditional_Case_Mappings", "dm", "FC_NFKC",
  "isc", "lc", "na", "na1", "Name_Alias", "NFKC_CF",
  # scx is block-compressible.
  "scf", "slc", "stc", "suc", "tc", "Turkic_Case_Folding", "uc"
))

# Dictionary of properties.
# Keyed by normalized property names and aliases.
# Each value is a tuple with
# 0: Type of property (binary, enum, ...)
# 1: List of aliases; short & long name followed by other aliases.
#    The short name is "" if it is listed as "n/a" in PropertyValueAliases.txt.
# 2: Set of short property value names.
# 3: Dictionary of property values.
#    For Catalog & Enumerated properties,
#    maps each value name to a list of aliases.
#    Empty for other types of properties.
_properties = {}

# Dictionary of binary-property values which we store as False/True.
# Same as the values dictionary of one of the binary properties.
_binary_values = {}

# Dictionary of null values.
# Keyed by short property names.
# These are type-specific values for properties that occur in the data.
# They are overridden by _defaults, block and code point properties.
_null_values = {}

# Property value names for null values.
# We do not store these in _defaults.
_null_names = frozenset(("<none>", "NaN"))

# Dictionary of explicit default property values.
# Keyed by short property names.
_defaults = {"gc": "Cn"}

# _null_values overridden by explicit _defaults.
# Initialized after parsing is done.
_null_or_defaults = {}

# List of properties with an ICU UProperty enum.
# Each item is an (enum, pname, values) tuple.
# - enum: the ICU enum UProperty constant string
# - pname: the UCD short property name
# - values: list of (enum, vname) pairs per property value
#   - enum: the ICU property value's enum constant string
#   - vname: the UCD short property value name
_icu_properties = []

# Dictionary of short property names mapped to _icu_properties items.
_pname_to_icu_prop = {}

_non_alnum_re = re.compile("[^a-zA-Z0-9]")

def NormPropName(pname):
  """Returns a normalized form of pname.
  Removes non-ASCII-alphanumeric characters and lowercases letters."""
  return _non_alnum_re.sub("", pname).lower()


def GetProperty(pname):
  """Returns the _properties value for the pname.
  Returns null if the property is ignored.
  Caches alternate spellings of the property name."""
  # Try the input name.
  prop = _properties.get(pname)
  if prop != None: return prop
  if pname in _ignored_properties: return None
  # Try the normalized input name.
  norm_name = NormPropName(pname)
  prop = _properties.get(norm_name)
  if prop != None:
    _properties[pname] = prop  # Cache prop under this new name spelling.
    return prop
  elif pname in _ignored_properties:
    _ignored_properties.add(pname)  # Remember to ignore this new name spelling.
    return None
  else:
    raise NameError("unknown property %s\n" % pname)


def GetShortPropertyName(pname):
  if pname in _null_values: return pname  # pname is already the short name.
  prop = GetProperty(pname)
  if not prop: return ""  # For ignored properties.
  return prop[1][0] or prop[1][1]  # Long name if no short name.


def GetShortPropertyValueName(prop, vname):
  if vname in prop[2]: return vname
  values = prop[3]
  aliases = values.get(vname)
  if aliases == None:
    norm_name = NormPropName(vname)
    aliases = values.get(norm_name)
    if aliases == None:
      raise NameError("unknown value name %s for property %s\n" %
                      (vname, prop[1][0]))
    values[vname] = aliases
  return aliases[0] or aliases[1]  # Long name if no short name.


def NormalizePropertyValue(prop, vname):
  if prop[2]:  # Binary/Catalog/Enumerated property.
    value = GetShortPropertyValueName(prop, vname)
    if prop[0] == "Binary":
      value = value == "Y"
    if prop[1][0].endswith("ccc"):
      value = int(value)
  else:
    value = vname
  return value

# Character data ----------------------------------------------------------- ***

# Lists of NamesList h1 and h2 headings.
# Each h1 value is a (start, end, comment) tuple.
# Each h2 value is a (cp, comment) tuple.
_h1 = []
_h2 = []

# List of Unicode blocks.
# Each item is a tuple of start & end code point integers
# and a dictionary of default property values.
_blocks = []

# List of ranges with algorithmic names.
# Each value is a list of [start, end, type, prefix]
# where prefix is optional.
_alg_names_ranges = []

# List of Unicode character ranges and their properties,
# stored as an inversion map with range_start & props dictionary.
# Starts with one range for all of Unicode without any properties.
# Setting values subdivides ranges.
_starts = array.array('l', [0, 0x110000])  # array of int32_t
_props = [{}, {}]  # props for 0 and 110000

def FindRange(x):
  """ Binary search for x in the inversion map.
  Returns the smallest i where x < _starts[i]"""
  return bisect.bisect(_starts, x) - 1


def GetProps(c):
  i = FindRange(c)
  return _props[i]


def UpdateProps(start, end, update):
  assert 0 <= start <= end <= 0x10ffff
  (need_to_update, do_update, u) = (update[0], update[1], update[2])
  # Find the index i of the range in _starts that contains start.
  i = FindRange(start)
  limit = end + 1
  # Intersect [start, limit[ with ranges in _starts.
  c_start = _starts[i]
  c_limit = _starts[i + 1]
  c_props = _props[i]
  # c_start <= start < c_limit
  if c_start < start:
    update_limit = c_limit if c_limit <= limit else limit
    if need_to_update(u, start, update_limit - 1, c_props):
      # Split off [c_start, start[ with a copy of c_props.
      i += 1
      c_props = c_props.copy()
      _starts.insert(i, start)
      _props.insert(i, c_props)
      c_start = start
  # Modify all ranges that are fully inside [start, limit[.
  while c_limit <= limit:
    # start <= c_start < c_limit <= limit
    if need_to_update(u, c_start, c_limit - 1, c_props):
      do_update(u, c_start, c_limit - 1, c_props)
    if c_limit == 0x110000: return
    i += 1
    c_start = c_limit
    c_limit = _starts[i + 1]
    c_props = _props[i]
  if c_start < limit and need_to_update(u, c_start, limit - 1, c_props):
    # Split off [limit, c_limit[ with a copy of c_props.
    _starts.insert(i + 1, limit)
    _props.insert(i + 1, c_props.copy())
    # Modify [c_start, limit[ c_props.
    do_update(u, c_start, limit - 1, c_props)


def NeedToSetProps(props, start, end, c_props):
  """Returns True if props is not a sub-dict of c_props."""
  for (pname, value) in props.items():
    if pname not in c_props or value != c_props[pname]: return True
  return False


def DoSetProps(props, start, end, c_props):
  c_props.update(props)


def SetProps(start, end, props):
  UpdateProps(start, end, (NeedToSetProps, DoSetProps, props))


def NeedToSetAlways(nv, start, end, c_props):
  return True


# For restoring boundaries after merging adjacent same-props ranges.
def AddBoundary(x):
  """Ensure that there is a range start/limit at x."""
  assert 0 <= x <= 0x10ffff
  i = FindRange(x)
  if _starts[i] == x: return
  # Split the range at x.
  c_start = _starts[i]
  c_limit = _starts[i + 1]
  c_props = _props[i]
  # c_start < x < c_limit
  i += 1
  _starts.insert(i, x)
  _props.insert(i, c_props.copy())


def SetDefaultValue(pname, value):
  """Sets the property's default value. Ignores null values."""
  prop = GetProperty(pname)
  if prop and value not in _null_names:
    value = NormalizePropertyValue(prop, value)
    if value != _null_values[prop[1][0]]:
      _defaults[prop[1][0]] = value
      SetProps(0, 0x10ffff, {prop[1][0]: value})


def SetBinaryPropertyToTrue(pname, start, end):
  prop = GetProperty(pname)
  if prop:
    assert prop[0] == "Binary"
    SetProps(start, end, {prop[1][0]: True})


def SetPropValue(prop, vname, start, end):
  value = NormalizePropertyValue(prop, vname)
  SetProps(start, end, {prop[1][0]: value})


def SetPropertyValue(pname, vname, start, end):
  prop = GetProperty(pname)
  if prop: SetPropValue(prop, vname, start, end)

# Parsing ------------------------------------------------------------------ ***

_stripped_cp_re = re.compile("([0-9a-fA-F]+)$")
_stripped_range_re = re.compile("([0-9a-fA-F]+)\.\.([0-9a-fA-F]+)$")
# Default value for all of Unicode.
_missing_re = re.compile("# *@missing: *0000\.\.10FFFF *; *(.+)$")
# Default value for some range.
_missing2_re = re.compile("# *@missing: *(.+)$")

def ReadUCDLines(in_file, want_ranges=True, want_other=False,
                 want_comments=False, want_missing=False):
  """Parses lines from a semicolon-delimited UCD text file.
  Strips comments, ignores empty and all-comment lines.
  Returns a tuple (type, line, ...).
  """
  for line in in_file:
    line = line.strip()
    if not line: continue
    if line.startswith("#"):  # whole-line comment
      parse_data = False
      if want_missing:
        match = _missing_re.match(line)
        if match:
          fields = match.group(1).split(";")
          for i in range(len(fields)): fields[i] = fields[i].strip()
          yield ("missing", line, fields)
          continue
        match = _missing2_re.match(line)
        if match:
          # Strip the "missing" comment prefix and fall through to
          # parse the remainder of the line like regular data.
          parse_data = True
          line = match.group(1)
      if not parse_data:
        if want_comments: yield ("comment", line)
        continue
    comment_start = line.find("#")  # inline comment
    if comment_start >= 0:
      line = line[:comment_start].rstrip()
      if not line: continue
    fields = line.split(";")
    for i in range(len(fields)): fields[i] = fields[i].strip()
    if want_ranges:
      first = fields[0]
      match = _stripped_range_re.match(first)
      if match:
        start = int(match.group(1), 16)
        end = int(match.group(2), 16)
        yield ("range", line, start, end, fields)
        continue
      match = _stripped_cp_re.match(first)
      if match:
        c = int(match.group(1), 16)
        yield ("range", line, c, c, fields)
        continue
    if want_other:
      yield ("other", line, fields)
    else:
      raise SyntaxError("unable to parse line\n  %s\n" % line)


def AddBinaryProperty(short_name, long_name):
  _null_values[short_name] = False
  bin_prop = _properties["Math"]
  prop = ("Binary", [short_name, long_name], bin_prop[2], bin_prop[3])
  _properties[short_name] = prop
  _properties[long_name] = prop
  _properties[NormPropName(short_name)] = prop
  _properties[NormPropName(long_name)] = prop


def AddSingleNameBinaryProperty(name):
  # For some properties, the short name is the same as the long name.
  _null_values[name] = False
  bin_prop = _properties["Math"]
  prop = ("Binary", [name, name], bin_prop[2], bin_prop[3])
  _properties[name] = prop
  _properties[NormPropName(name)] = prop


def AddPOSIXBinaryProperty(name):
  # We only define a long name for ICU-specific (non-UCD) POSIX properties.
  _null_values[name] = False
  bin_prop = _properties["Math"]
  prop = ("Binary", ["", name], bin_prop[2], bin_prop[3])
  _properties[name] = prop
  _properties[NormPropName(name)] = prop
  # This is to match UProperty UCHAR_POSIX_ALNUM etc.
  _properties["posix" + NormPropName(name)] = prop


# Match a comment line like
# PropertyAliases-6.1.0.txt
# and extract the Unicode version.
_ucd_version_re = re.compile("# *PropertyAliases" +
                             "-([0-9]+(?:\\.[0-9]+)*)(?:d[0-9]+)?" +
                             "\\.txt")

def ParsePropertyAliases(in_file):
  global _ucd_version
  prop_type_nulls = {
    "Binary": False,
    "Catalog": "??",  # Must be specified, e.g., in @missing line.
    "Enumerated": "??",  # Must be specified.
    "Numeric": "NaN",
    "String": "",
    "Miscellaneous": ""
  }
  for data in ReadUCDLines(in_file, want_ranges=False,
                           want_other=True, want_comments=True):
    if data[0] == "comment":
      line = data[1]
      match = _ucd_version_re.match(line)
      if match:
        _ucd_version = match.group(1)
      else:
        words = line[1:].lstrip().split()
        if len(words) == 2 and words[1] == "Properties":
          prop_type = words[0]
          null_value = prop_type_nulls[prop_type]
    else:
      # type == "other"
      aliases = data[2]
      name = aliases[0]
      if name in _ignored_properties:
        for alias in aliases:
          _ignored_properties.add(alias)
          _ignored_properties.add(NormPropName(alias))
      else:
        if name.endswith("ccc"):
          _null_values[name] = 0
        else:
          _null_values[name] = null_value
        prop = (prop_type, aliases, set(), {})
        for alias in aliases:
          _properties[alias] = prop
          _properties[NormPropName(alias)] = prop
  # Add provisional and ICU-specific properties we need.
  # We add some in support of runtime API, even if we do not write
  # data for them to ppucd.txt (e.g., lccc & tccc).
  # We add others just to represent UCD data that contributes to
  # some functionality, although Unicode has not "blessed" them
  # as separate properties (e.g., Turkic_Case_Folding).

  # Turkic_Case_Folding: The 'T' mappings in CaseFolding.txt.
  name = "Turkic_Case_Folding"
  _null_values[name] = ""
  prop = ("String", [name, name], set(), {})
  _properties[name] = prop
  _properties[NormPropName(name)] = prop
  # Conditional_Case_Mappings: SpecialCasing.txt lines with conditions.
  name = "Conditional_Case_Mappings"
  _null_values[name] = ""
  prop = ("Miscellaneous", [name, name], set(), {})
  _properties[name] = prop
  _properties[NormPropName(name)] = prop
  # lccc = ccc of first cp in canonical decomposition.
  _null_values["lccc"] = 0
  ccc_prop = list(_properties["ccc"])
  ccc_prop[1] = ["lccc", "Lead_Canonical_Combining_Class"]
  prop = tuple(ccc_prop)
  _properties["lccc"] = prop
  _properties["Lead_Canonical_Combining_Class"] = prop
  _properties["leadcanonicalcombiningclass"] = prop
  # tccc = ccc of last cp in canonical decomposition.
  _null_values["tccc"] = 0
  ccc_prop[1] = ["tccc", "Trail_Canonical_Combining_Class"]
  prop = tuple(ccc_prop)
  _properties["tccc"] = prop
  _properties["Trail_Canonical_Combining_Class"] = prop
  _properties["trailcanonicalcombiningclass"] = prop
  # Script_Extensions
  if "scx" not in _properties:
    _null_values["scx"] = ""
    prop = ("Miscellaneous", ["scx", "Script_Extensions"], set(), {})
    _properties["scx"] = prop
    _properties["Script_Extensions"] = prop
    _properties["scriptextensions"] = prop
  # General Category as a bit mask.
  _null_values["gcm"] = "??"
  gc_prop = _properties["gc"]
  prop = ("Bitmask", ["gcm", "General_Category_Mask"], gc_prop[2], gc_prop[3])
  _properties["gcm"] = prop
  _properties["General_Category_Mask"] = prop
  _properties["generalcategorymask"] = prop
  # Various binary properties.
  AddBinaryProperty("Sensitive", "Case_Sensitive")
  AddBinaryProperty("nfdinert", "NFD_Inert")
  AddBinaryProperty("nfkdinert", "NFKD_Inert")
  AddBinaryProperty("nfcinert", "NFC_Inert")
  AddBinaryProperty("nfkcinert", "NFKC_Inert")
  AddBinaryProperty("segstart", "Segment_Starter")
  # https://www.unicode.org/reports/tr51/#Emoji_Properties
  AddBinaryProperty("Emoji", "Emoji")
  AddBinaryProperty("EPres", "Emoji_Presentation")
  AddBinaryProperty("EMod", "Emoji_Modifier")
  AddBinaryProperty("EBase", "Emoji_Modifier_Base")
  AddBinaryProperty("EComp", "Emoji_Component")
  AddBinaryProperty("ExtPict", "Extended_Pictographic")
  # https://www.unicode.org/reports/tr51/#Emoji_Sets
  AddSingleNameBinaryProperty("Basic_Emoji")
  AddSingleNameBinaryProperty("Emoji_Keycap_Sequence")
  AddSingleNameBinaryProperty("RGI_Emoji_Modifier_Sequence")
  AddSingleNameBinaryProperty("RGI_Emoji_Flag_Sequence")
  AddSingleNameBinaryProperty("RGI_Emoji_Tag_Sequence")
  AddSingleNameBinaryProperty("RGI_Emoji_ZWJ_Sequence")
  AddSingleNameBinaryProperty("RGI_Emoji")
  # C/POSIX character classes that do not have Unicode property [value] aliases.
  # See uchar.h.
  AddPOSIXBinaryProperty("alnum")
  AddPOSIXBinaryProperty("blank")
  AddPOSIXBinaryProperty("graph")
  AddPOSIXBinaryProperty("print")
  AddPOSIXBinaryProperty("xdigit")


def ParsePropertyValueAliases(in_file):
  global _binary_values
  for data in ReadUCDLines(in_file, want_ranges=False,
                           want_other=True, want_missing=True):
    if data[0] == "missing":
      SetDefaultValue(data[2][0], data[2][1])
    else:
      # type == "other"
      fields = data[2]
      pname = fields[0]
      prop = GetProperty(pname)
      if prop:
        del fields[0]  # Only the list of aliases remains.
        short_name = fields[0]
        if short_name == "n/a":  # no short name
          fields[0] = ""
          short_name = fields[1]
        prop[2].add(short_name)
        values = prop[3]
        for alias in fields:
          if alias:
            values[alias] = fields
            values[NormPropName(alias)] = fields
        if prop[0] == "Binary" and not _binary_values:
          _binary_values = values
  # Some of the @missing lines with non-null default property values
  # are in files that we do not parse;
  # either because the data for that property is easily
  # (i.e., the @missing line would be the only reason to parse such a file)
  # or because we compute the property at runtime,
  # such as the Hangul_Syllable_Type.
  if "dt" not in _defaults:  # DerivedDecompositionType.txt
    _defaults["dt"] = "None"
  if "nt" not in _defaults:  # DerivedNumericType.txt
    _defaults["nt"] = "None"
  if "hst" not in _defaults:  # HangulSyllableType.txt
    _defaults["hst"] = "NA"
  if "gc" not in _defaults:  # No @missing line in any .txt file?
    _defaults["gc"] = "Cn"
  # Copy the gc default value to gcm.
  _defaults["gcm"] = _defaults["gc"]
  # Add ISO 15924-only script codes.
  # Only for the ICU script code API, not necessary for parsing the UCD.
  script_prop = _properties["sc"]
  short_script_names = script_prop[2]  # set
  script_values = script_prop[3]  # dict
  remove_scripts = []
  for script in _scripts_only_in_iso15924:
    if script in short_script_names:
      remove_scripts.append(script)
    else:
      short_script_names.add(script)
      # Do not invent a Unicode long script name before the UCD adds the script.
      script_list = [script, script]  # [short, long]
      script_values[script] = script_list
      # Probably not necessary because
      # we will not parse these scripts from the UCD:
      script_values[NormPropName(script)] = script_list
  if remove_scripts:
    raise ValueError(
        "remove %s from _scripts_only_in_iso15924" % remove_scripts)


def ParseBlocks(in_file):
  for data in ReadUCDLines(in_file, want_missing=True):
    if data[0] == "missing":
      SetDefaultValue("blk", data[2][0])
    else:
      # type == "range"
      (start, end, name) = (data[2], data[3], data[4][1])
      _blocks.append((start, end, {"blk": name}))
      SetPropertyValue("blk", name, start, end)
  _blocks.sort()
  # Check for overlapping blocks.
  prev_end = -1
  for b in _blocks:
    start = b[0]
    end = b[1]
    if prev_end >= start:
      raise ValueError(
          "block %04lX..%04lX %s overlaps with another " +
          "ending at %04lX\n  %s\n" %
          (start, end, b[2]["blk"], prev_end))
    prev_end = end


def ParseUnicodeData(in_file):
  dt_prop = GetProperty("dt")
  range_first_line = ""
  range_first = -1
  for data in ReadUCDLines(in_file, want_missing=True):
    # type == "range"
    (line, c, end, fields) = (data[1], data[2], data[3], data[4])
    assert c == end
    name = fields[1]
    if name.startswith("<"):
      if name.endswith(", First>"):
        if range_first >= 0:
          raise SyntaxError(
              "error: unterminated range started at\n  %s\n" %
              range_first_line)
        range_first = c
        range_first_line = line
        continue
      elif name.endswith(", Last>"):
        if range_first < 0:
          raise SyntaxError(
              "error: range end without start at\n  %s\n" %
              line)
        elif range_first > c:
          raise SyntaxError(
              "error: range start/end out of order at\n  %s\n  %s\n" %
              (range_first_line, line))
        first_name = range_first_line.split(";")[1][1:-8]
        name = name[1:-7]
        if first_name != name:
          raise SyntaxError(
              "error: range start/end name mismatch at\n  %s\n  %s\n" %
              (range_first_line, line))
        end = c
        c = range_first
        range_first = -1
        # Remember algorithmic name ranges.
        if "Ideograph" in name:
          prefix = "CJK UNIFIED IDEOGRAPH-"
          if c == 0x17000 or c == 0x18D00: prefix = "TANGUT IDEOGRAPH-"
          _alg_names_ranges.append([c, end, "han", prefix])
        elif name == "Hangul Syllable":
          _alg_names_ranges.append([c, end, "hangul"])
        name = ""
      else:
        # Ignore non-names like <control>.
        name = ""
    props = {}
    if name: props["na"] = name
    props["gc"] = fields[2]
    ccc = int(fields[3])
    if ccc: props["ccc"] = ccc
    props["bc"] = fields[4]
    # Decomposition type & mapping.
    dm = fields[5]
    if dm:
      if dm.startswith("<"):
        dt_limit = dm.index(">")
        dt = NormalizePropertyValue(dt_prop, dm[1:dt_limit])
        dm = dm[dt_limit + 1:].lstrip()
      else:
        dt = "Can"
      props["dt"] = dt
      props["dm"] = dm
    # Numeric type & value.
    decimal = fields[6]
    digit = fields[7]
    nv = fields[8]
    if (decimal and decimal != nv) or (digit and digit != nv):
      raise SyntaxError("error: numeric values differ at\n  %s\n" % line)
    if nv:
      # Map improper fractions to proper ones.
      # U+109F7 MEROITIC CURSIVE FRACTION TWO TWELFTHS
      # .. U+109FF MEROITIC CURSIVE FRACTION TEN TWELFTHS
      if nv == "2/12":
        nv = "1/6"
      elif nv == "3/12":
        nv = "1/4"
      elif nv == "4/12":
        nv = "1/3"
      elif nv == "6/12":
        nv = "1/2"
      elif nv == "8/12":
        nv = "2/3"
      elif nv == "9/12":
        nv = "3/4"
      elif nv == "10/12":
        nv = "5/6"
      props["nv"] = nv
      props["nt"] = "De" if decimal else "Di" if digit else "Nu"
    if fields[9] == "Y": props["Bidi_M"] = True
    # ICU 49 and above does not support Unicode_1_Name any more.
    # See ticket #9013.
    # na1 = fields[10]
    # if na1: props["na1"] = na1
    # ISO_Comment is deprecated and has no values.
    # isc = fields[11]
    # if isc: props["isc"] = isc
    # Simple case mappings.
    suc = fields[12]
    slc = fields[13]
    stc = fields[14]
    if suc: props["suc"] = suc
    if slc: props["slc"] = slc
    if stc: props["stc"] = stc
    SetProps(c, end, props)
  if range_first >= 0:
    raise SyntaxError(
        "error: unterminated range started at\n  %s\n" %
        range_first_line)
  # Hangul syllables have canonical decompositions which are not listed in UnicodeData.txt.
  SetPropertyValue("dt", "Can", 0xac00, 0xd7a3)
  _alg_names_ranges.sort()


_names_h1_re = re.compile("@@\t([0-9a-fA-F]+)\t(.+?)\t([0-9a-fA-F]+)$")
_names_h2_re = re.compile("@\t\t(.+)")
_names_char_re = re.compile("([0-9a-fA-F]+)\t.+")

def ParseNamesList(in_file):
  pending_h2 = ""
  for line in in_file:
    line = line.strip()
    if not line: continue
    match = _names_h1_re.match(line)
    if match:
      pending_h2 = ""  # Drop a pending h2 when we get to an h1.
      start = int(match.group(1), 16)
      end = int(match.group(3), 16)
      comment = match.group(2).replace(u"\xa0", " ")
      _h1.append((start, end, comment))
      continue
    match = _names_h2_re.match(line)
    if match:
      pending_h2 = match.group(1).replace(u"\xa0", " ")
      continue
    if pending_h2:
      match = _names_char_re.match(line)
      if match:
        c = int(match.group(1), 16)
        _h2.append((c, pending_h2))
        pending_h2 = ""
  _h1.sort()
  _h2.sort()


def ParseNamedProperties(in_file):
  """Parses a .txt file where the first column is a code point range
  and the second column is a property name.
  Sets binary properties to True,
  and other properties to the values in the third column."""
  for data in ReadUCDLines(in_file, want_missing=True):
    if data[0] == "missing":
      SetDefaultValue(data[2][0], data[2][1])
    else:
      # type == "range"
      if len(data[4]) == 2:
        SetBinaryPropertyToTrue(data[4][1], data[2], data[3])
      else:
        SetPropertyValue(data[4][1], data[4][2], data[2], data[3])


def ParseOneProperty(in_file, pname):
  """Parses a .txt file where the first column is a code point range
  and the second column is the value of a known property."""
  prop = GetProperty(pname)
  for data in ReadUCDLines(in_file, want_missing=True):
    if data[0] == "missing":
      SetDefaultValue(pname, data[2][0])
    else:
      # type == "range"
      SetPropValue(prop, data[4][1], data[2], data[3])


def ParseBidiMirroring(in_file): ParseOneProperty(in_file, "bmg")
def ParseDerivedAge(in_file): ParseOneProperty(in_file, "age")
def ParseDerivedBidiClass(in_file): ParseOneProperty(in_file, "bc")
def ParseDerivedJoiningGroup(in_file): ParseOneProperty(in_file, "jg")
def ParseDerivedJoiningType(in_file): ParseOneProperty(in_file, "jt")
def ParseEastAsianWidth(in_file): ParseOneProperty(in_file, "ea")
def ParseGraphemeBreakProperty(in_file): ParseOneProperty(in_file, "GCB")
def ParseIndicPositionalCategory(in_file): ParseOneProperty(in_file, "InPC")
def ParseIndicSyllabicCategory(in_file): ParseOneProperty(in_file, "InSC")
def ParseLineBreak(in_file): ParseOneProperty(in_file, "lb")
def ParseScripts(in_file): ParseOneProperty(in_file, "sc")
def ParseScriptExtensions(in_file): ParseOneProperty(in_file, "scx")
def ParseSentenceBreak(in_file): ParseOneProperty(in_file, "SB")
def ParseVerticalOrientation(in_file): ParseOneProperty(in_file, "vo")
def ParseWordBreak(in_file): ParseOneProperty(in_file, "WB")


def DoSetNameAlias(alias, start, end, c_props):
  if "Name_Alias" in c_props:
    c_props["Name_Alias"] += ',' + alias
  else:
    c_props["Name_Alias"] = alias


def ParseNameAliases(in_file):
  """Parses Name_Alias from NameAliases.txt.
  A character can have multiple aliases.

  In Unicode 6.0, there are two columns,
  with a name correction in the second column.

  In Unicode 6.1, there are three columns.
  The second contains an alias, the third its type.
  The documented types are:
    correction, control, alternate, figment, abbreviation

  This function does not sort the types, assuming they appear in this order."""
  for data in ReadUCDLines(in_file):
    start = data[2]
    end = data[3]
    if start != end:
      raise ValueError("NameAliases.txt has an alias for a range %04lX..%04lX" %
                       (start, end))
    fields = data[4]
    if len(fields) == 2:
      alias = "correction=" + fields[1]
    else:
      alias = fields[2] + '=' + fields[1]
    update = (NeedToSetAlways, DoSetNameAlias, alias)
    UpdateProps(start, end, update)


def NeedToSetNumericValue(nv, start, end, c_props):
  c_nv = c_props.get("nv")
  if c_nv == None:
    # DerivedNumericValues.txt adds a Numeric_Value.
    assert "nt" not in c_props
    return True
  if nv != c_nv:
    raise ValueError(("UnicodeData.txt has nv=%s for %04lX..%04lX " +
                     "but DerivedNumericValues.txt has nv=%s") %
                     (c_nv, start, end, nv))
  return False


def DoSetNumericValue(nv, start, end, c_props):
  c_props.update({"nt": "Nu", "nv": nv})


def ParseDerivedNumericValues(in_file):
  """Parses DerivedNumericValues.txt.
  For most characters, the numeric type & value were parsed previously
  from UnicodeData.txt but that does not show the values for Han characters.
  Here we check that values match those from UnicodeData.txt
  and add new ones."""
  # Ignore the @missing line which has an incorrect number of fields,
  # and the "NaN" in the wrong field (at least in Unicode 5.1..6.1).
  # Also, "NaN" is just the Numeric null value anyway.
  for data in ReadUCDLines(in_file):
    # Conditional update to the numeric value in the 4th field.
    update = (NeedToSetNumericValue, DoSetNumericValue, data[4][3])
    UpdateProps(data[2], data[3], update)


def ParseCaseFolding(in_file):
  for data in ReadUCDLines(in_file, want_missing=True):
    if data[0] == "missing":
      assert data[2][0] == "C"  # common to scf & cf
      SetDefaultValue("scf", data[2][1])
      SetDefaultValue("cf", data[2][1])
    else:
      # type == "range"
      start = data[2]
      end = data[3]
      status = data[4][1]
      mapping = data[4][2]
      assert status in "CSFT"
      if status == "C":
        SetProps(start, end, {"scf": mapping, "cf": mapping})
      elif status == "S":
        SetPropertyValue("scf", mapping, start, end)
      elif status == "F":
        SetPropertyValue("cf", mapping, start, end)
      else:  # status == "T"
        SetPropertyValue("Turkic_Case_Folding", mapping, start, end)


def DoSetConditionalCaseMappings(ccm, start, end, c_props):
  if "Conditional_Case_Mappings" in c_props:
    c_props["Conditional_Case_Mappings"] += ',' + ccm
  else:
    c_props["Conditional_Case_Mappings"] = ccm


def ParseSpecialCasing(in_file):
  for data in ReadUCDLines(in_file, want_missing=True):
    if data[0] == "missing":
      SetDefaultValue("lc", data[2][0])
      SetDefaultValue("tc", data[2][1])
      SetDefaultValue("uc", data[2][2])
    else:
      # type == "range"
      start = data[2]
      end = data[3]
      fields = data[4]
      if len(fields) < 5 or not fields[4]:
        # Unconditional mappings.
        SetProps(start, end, {"lc": fields[1], "tc": fields[2], "uc": fields[3]})
      else:
        # Conditional_Case_Mappings
        ccm = (fields[4] + ":lc=" + fields[1] +
               "&tc=" + fields[2] + "&uc=" + fields[3])
        update = (NeedToSetAlways, DoSetConditionalCaseMappings, ccm)
        UpdateProps(start, end, update)


def ParseBidiBrackets(in_file):
  for data in ReadUCDLines(in_file, want_missing=True):
    if data[0] == "missing":
      SetDefaultValue("bpt", data[2][1])
    else:
      # type == "range"
      start = data[2]
      end = data[3]
      assert start == end
      mapping = data[4][1]
      bracket_type = data[4][2]
      SetProps(start, end, {"bpb": mapping, "bpt": bracket_type})

# Postprocessing ----------------------------------------------------------- ***

def PrintedSize(pname, value):
  if isinstance(value, bool):
    if value:
      return len(pname) + 1  # ";pname"
    else:
      return len(pname) + 2  # ";-pname"
  else:
    return len(pname) + len(str(value)) + 2  # ";pname=value"


def CompactBlock(b, i):
  assert b[0] == _starts[i]
  b_props = b[2]  # Normally just blk from Blocks.txt.
  # b_props["blk"] has not been canonicalized yet.
  b_props["blk"] = _props[i]["blk"]
  orig_i = i
  # Count the number of occurrences of each property's value in this block.
  # To minimize the output, count the number of assigned ranges,
  # not the number of code points.
  num_ranges = 0
  prop_counters = {}
  if "gc" in b_props:
    b_is_unassigned = b_props["gc"] == "Cn"  # Unreachable with normal data.
  else:
    b_is_unassigned = _defaults["gc"] == "Cn"  # This is expected to be true.
  while True:
    start = _starts[i]
    if start > b[1]: break
    props = _props[i]
    if "gc" in props:
      is_unassigned = props["gc"] == "Cn"
    else:
      is_unassigned = b_is_unassigned
    if is_unassigned:
      # Compact an unassigned range inside the block and
      # mark it to be written with "unassigned".
      # It falls back to default properties, not block properties,
      # except for the blk=Block property.
      assert props["blk"] == b_props["blk"]
      del props["blk"]
      for pname in list(props.keys()):  # .keys() is a copy so we can del props[pname].
        if props[pname] == _null_or_defaults[pname]: del props[pname]
      # What remains are unusual default values for unassigned code points.
      # For example, bc=R or lb=ID.
      # See http://www.unicode.org/reports/tr44/#Default_Values_Table
      props["unassigned"] = True
    else:
      for (pname, value) in props.items():
        if pname in prop_counters:
          counter = prop_counters[pname]
        else:
          counter = {_null_or_defaults[pname]: num_ranges}
          prop_counters[pname] = counter
        if value in counter:
          counter[value] += 1
        else:
          counter[value] = 1
      # Also count default values for properties that do not occur in a range.
      for pname in prop_counters:
        if pname not in props:
          counter = prop_counters[pname]
          value = _null_or_defaults[pname]
          counter[value] += 1
      num_ranges += 1
      # Invariant: For each counter, the sum of counts must equal num_ranges.
    i += 1
  # For each property that occurs within this block,
  # set the value that reduces the file size the most as a block property value.
  # This is usually the most common value.
  for (pname, counter) in prop_counters.items():
    default_value = _null_or_defaults[pname]
    default_size = PrintedSize(pname, default_value) * counter[default_value]
    max_value = None
    max_count = 0
    max_savings = 0
    for (value, count) in counter.items():
      if value != default_value and count > 1:
        # Does the file get smaller by setting the block default?
        # We save writing the block value as often as it occurs,
        # minus once for writing it for the block,
        # minus writing the default value instead.
        savings = PrintedSize(pname, value) * (count - 1) - default_size
        # For two values with the same savings, pick the one that compares lower,
        # to make this deterministic (avoid flip-flopping).
        if (savings > max_savings or
            (savings > 0 and savings == max_savings and value < max_value)):
          max_value = value
          max_count = count
          max_savings = savings
    # Do not compress uncompressible properties,
    # with an exception for many empty-string values in a block
    # (NFKC_CF='' for tags and variation selectors).
    if (max_savings > 0 and
        ((pname not in _uncompressible_props) or
          (max_value == '' and max_count >= 12))):
      b_props[pname] = max_value
  # For each range and property, remove the default+block value
  # but set the default value if that property was not set
  # (i.e., it used to inherit the default value).
  b_defaults = _null_or_defaults.copy()
  b_defaults.update(b_props)
  i = orig_i
  while True:
    start = _starts[i]
    if start > b[1]: break
    props = _props[i]
    if "unassigned" not in props:
      # Compact an assigned range inside the block.
      for pname in prop_counters:
        if pname in props:
          if props[pname] == b_defaults[pname]: del props[pname]
        elif pname in b_props:
          # b_props only has non-default values.
          # Set the default value if it used to be inherited.
          props[pname] = _null_or_defaults[pname]
      # If there is only one assigned range, then move all of its properties
      # to the block.
      if num_ranges == 1:
        b_props.update(props)
        props.clear()
    i += 1
  # Return the _starts index of the first range after this block.
  return i


def CompactNonBlock(limit, i):
  """Remove default property values from between-block ranges."""
  default_is_unassigned = _defaults["gc"] == "Cn"  # This is expected to be true.
  while True:
    start = _starts[i]
    if start >= limit: break
    props = _props[i]
    if "gc" in props:
      is_unassigned = props["gc"] == "Cn"
    else:
      is_unassigned = default_is_unassigned
    for pname in list(props.keys()):  # .keys() is a copy so we can del props[pname].
      if props[pname] == _null_or_defaults[pname]: del props[pname]
    assert "blk" not in props
    # If there are no props left, then nothing will be printed.
    # Otherwise, add "unassigned" for more obvious output.
    if props and is_unassigned:
      props["unassigned"] = True
    i += 1
  # Return the _starts index of the first range after this block.
  return i


def CompactBlocks():
  """Optimizes block properties.
  Sets properties on blocks to the most commonly used values,
  and removes default+block values from code point properties."""
  # Ensure that there is a boundary in _starts for each block
  # so that the simple mixing method below works.
  for b in _blocks:
    AddBoundary(b[0])
    limit = b[1] + 1
    if limit <= 0x10ffff: AddBoundary(limit)
  # Walk through ranges and blocks together.
  i = 0
  for b in _blocks:
    b_start = b[0]
    if _starts[i] < b_start:
      i = CompactNonBlock(b_start, i)
    i = CompactBlock(b, i)
  CompactNonBlock(0x110000, i)

# Output ------------------------------------------------------------------- ***

def AppendRange(fields, start, end):
  if start == end:
    fields.append("%04lX" % start)
  else:
    fields.append("%04lX..%04lX" % (start, end))


def AppendProps(fields, props):
  # Sort property names (props keys) by their normalized forms
  # and output properties in that order.
  for pname in sorted(props, key=NormPropName):
    value = props[pname]
    if isinstance(value, bool):
      if not value: pname = "-" + pname
      fields.append(pname)
    else:
      fields.append("%s=%s" % (pname, value))


def WriteFieldsRangeProps(fields, start, end, props, out_file):
  AppendRange(fields, start, end)
  AppendProps(fields, props)
  out_file.write(";".join(fields))
  out_file.write("\n")


def EscapeNonASCII(s):
  i = 0
  while i < len(s):
    c = ord(s[i])
    if c <= 0x7f:
      i = i + 1
    else:
      if c <= 0xffff:
        esc = u"\\u%04X" % c
      else:
        esc = u"\\U%08X" % c
      s = s[:i] + esc + s[i+1:]
      i = i + len(esc)
  return s


def WritePreparsedUCD(out_file):
  out_file.write("""# Preparsed UCD generated by ICU preparseucd.py
# Copyright (C) 1991 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
""");
  out_file.write("ucd;%s\n\n" % _ucd_version)
  # Sort property names (props keys) by their normalized forms
  # and output properties in that order.
  pnames = sorted(_null_values, key=NormPropName)
  for pname in pnames:
    prop = _properties[pname]
    out_file.write(";".join(["property", prop[0]] + prop[1]))
    out_file.write("\n")
  out_file.write("\n")
  out_file.write(";".join(["binary"] + _binary_values["N"]))
  out_file.write("\n")
  out_file.write(";".join(["binary"] + _binary_values["Y"]))
  out_file.write("\n")
  for pname in pnames:
    prop = _properties[pname]
    short_names = prop[2]
    if short_names and prop[0] != "Binary":
      for name in sorted(short_names):
        out_file.write(";".join(["value", prop[1][0]] + prop[3][name]))
        out_file.write("\n")
  out_file.write("\n")
  # Ensure that there is a boundary in _starts for each
  # range of data we mix into the output,
  # so that the simple mixing method below works.
  for b in _blocks: AddBoundary(b[0])
  for r in _alg_names_ranges: AddBoundary(r[0])
  for h in _h1: AddBoundary(h[0])
  for h in _h2: AddBoundary(h[0])
  # Write the preparsed data. ppucd.txt = preparsed UCD
  # Syntax: http://site.icu-project.org/design/props/ppucd
  WriteFieldsRangeProps(["defaults"], 0, 0x10ffff, _defaults, out_file)
  i_blocks = 0
  i_alg = 0
  i_h1 = 0
  i_h2 = 0
  b_end = -1
  for i in range(len(_starts) - 1):
    start = _starts[i]
    end = _starts[i + 1] - 1
    # Block with default properties.
    if i_blocks < len(_blocks) and start == _blocks[i_blocks][0]:
      b = _blocks[i_blocks]
      b_end = b[1]
      WriteFieldsRangeProps(["\nblock"], b[0], b_end, b[2], out_file)
      i_blocks += 1
    # NamesList h1 heading (for [most of] a block).
    if i_h1 < len(_h1) and start == _h1[i_h1][0]:
      h = _h1[i_h1]
      out_file.write("# %04lX..%04lX %s\n" % (h[0], h[1], EscapeNonASCII(h[2])))
      i_h1 += 1
    # Algorithmic-names range.
    if i_alg < len(_alg_names_ranges) and start == _alg_names_ranges[i_alg][0]:
      r = _alg_names_ranges[i_alg]
      fields = ["algnamesrange"]
      AppendRange(fields, r[0], r[1])
      fields.extend(r[2:])
      out_file.write(";".join(fields))
      out_file.write("\n")
      i_alg += 1
    # NamesList h2 heading.
    if i_h2 < len(_h2) and start == _h2[i_h2][0]:
      out_file.write("# %s\n" % EscapeNonASCII(_h2[i_h2][1]))
      i_h2 += 1
    # Code point/range data.
    props = _props[i]
    # Omit ranges with only default+block properties.
    if props:
      if start > b_end and b_end >= 0:
        # First range with values after the last block.
        # Separate it visually from the block lines.
        out_file.write("\n# No block\n")
        b_end = -1
      if "unassigned" in props:
        # Do not output "unassigned" as a property.
        del props["unassigned"]
        line_type = "unassigned"
      else:
        line_type = "cp"
      WriteFieldsRangeProps([line_type], start, end, props, out_file)

# Write Normalizer2 input files -------------------------------------------- ***
# Ported from gennorm/store.c.

def WriteAllCC(out_file):
  out_file.write("# Canonical_Combining_Class (ccc) values\n");
  prev_start = 0
  prev_cc = 0
  for i in range(len(_starts)):
    start = _starts[i]
    props = _props[i]
    cc = props.get("ccc")
    if not cc: cc = 0
    if prev_cc != cc:
      if prev_cc != 0:
        last_code_point = start - 1
        if prev_start == last_code_point:
          out_file.write("%04X:%d\n" % (last_code_point, prev_cc))
        else:
          out_file.write("%04X..%04X:%d\n" %
                         (prev_start, last_code_point, prev_cc))
      prev_start = start
      prev_cc = cc


def HasMapping(c):
  props = GetProps(c)
  dt = props.get("dt")
  return dt and dt != "None"


def HasOneWayMapping(c):
  while True:
    props = GetProps(c)
    dt = props.get("dt")
    if not dt or dt == "None":
      return False  # no mapping
    elif dt == "Can":
      # The canonical decomposition is a one-way mapping if
      # - it does not map to exactly two code points
      # - c has ccc!=0
      # - c has the Composition_Exclusion property
      # - its starter has a one-way mapping (loop for this)
      # - its non-starter decomposes
      nfd = props["dm"].split()
      if (len(nfd) != 2 or
          props.get("ccc") or
          props.get("Comp_Ex") or
          HasMapping(int(nfd[1], 16))):
        return True
      c = int(nfd[0], 16)  # continue
    else:
      # c has a compatibility mapping.
      return True


_data_file_copyright = """# Copyright (C) 2016 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
# Copyright (C) 1999-2016, International Business Machines
# Corporation and others.  All Rights Reserved.
#
"""

def WriteNorm2NFCTextFile(path):
  global _data_file_copyright
  with open(os.path.join(path, "nfc.txt"), "w") as out_file:
    out_file.write(
        _data_file_copyright + """# file name: nfc.txt
#
# machine-generated by ICU preparseucd.py
#
# Complete data for Unicode NFC normalization.

* Unicode """ + _ucd_version + """

""")
    WriteAllCC(out_file)
    out_file.write("\n# Canonical decomposition mappings\n")
    for i in range(len(_starts) - 1):
      start = _starts[i]
      end = _starts[i + 1] - 1
      props = _props[i]
      dm = props.get("dm")
      if dm and dm[0] != '<' and props["dt"] == "Can":
        assert start == end
        # The Comp_Ex=Full_Composition_Exclusion property tells us
        # whether the canonical decomposition round-trips.
        separator = '>' if props.get("Comp_Ex") else '='
        out_file.write("%04X%s%s\n" % (start, separator, dm))


def WriteNorm2NFKCTextFile(path):
  global _data_file_copyright
  with open(os.path.join(path, "nfkc.txt"), "w") as out_file:
    out_file.write(
        _data_file_copyright + """# file name: nfkc.txt
#
# machine-generated by ICU preparseucd.py
#
# Data for Unicode NFKC normalization.
# This file contains only compatibility decomposition mappings,
# plus those canonical decompositions that change from NFC round-trip mappings
# to NFKC one-way mappings.
# Use this file as the second gennorm2 input file after nfc.txt.

* Unicode """ + _ucd_version + """

""")
    for i in range(len(_starts) - 1):
      start = _starts[i]
      end = _starts[i + 1] - 1
      props = _props[i]
      dm = props.get("dm")
      if dm and dm[0] != '<':
        assert start == end
        if props["dt"] != "Can":
          # Compatibility decomposition.
          out_file.write("%04X>%s\n" % (start, dm))
        elif not props.get("Comp_Ex") and HasOneWayMapping(start):
          # NFC round-trip mapping turns into NFKC one-way mapping.
          out_file.write("%04X>%s  # NFC round-trip, NFKC one-way\n" %
                         (start, dm))


def WriteNorm2NFKC_CFTextFile(path, filename, prop_name):
  global _data_file_copyright
  with open(os.path.join(path, filename), "w") as out_file:
    out_file.write(
        _data_file_copyright + """# file name: %s
#
# machine-generated by ICU preparseucd.py
#
# This file contains the Unicode %s mappings,
# extracted from the UCD file DerivedNormalizationProps.txt,
# and reformatted into syntax for the gennorm2 Normalizer2 data generator tool.
# Use this file as the third gennorm2 input file after nfc.txt and nfkc.txt.

""" %
        (filename, prop_name))
    out_file.write("* Unicode " + _ucd_version + "\n\n")
    if prop_name == "NFKC_SCF":
      # Hack: Override the NFC round-trip mapping for U+0130 and
      # 36 Greek small letters that decompose to xxxx 0345. See PAG issue #182.
      out_file.write("""# Each of these maps to itself.
0130-
1F80..1F87-
1F90..1F97-
1FA0..1FA7-
1FB2..1FB4-
1FB7-
1FC2..1FC4-
1FC7-
1FF2..1FF4-
1FF7-

""")
    prev_start = 0
    prev_end = 0
    prev_nfkc_cf = None
    for i in range(len(_starts) - 1):
      start = _starts[i]
      end = _starts[i + 1] - 1
      props = _props[i]
      nfkc_cf = props.get(prop_name)
      # Merge with the previous range if possible,
      # or remember this range for merging.
      if nfkc_cf == prev_nfkc_cf and (prev_end + 1) == start:
        prev_end = end
      else:
        if prev_nfkc_cf != None and (not prev_nfkc_cf or prev_nfkc_cf[0] != '<'):
          if prev_start == prev_end:
            out_file.write("%04X>%s\n" % (prev_start, prev_nfkc_cf))
          else:
            out_file.write("%04X..%04X>%s\n" %
                           (prev_start, prev_end, prev_nfkc_cf))
        prev_start = start
        prev_end = end
        prev_nfkc_cf = nfkc_cf


def WriteNorm2(path):
  WriteNorm2NFCTextFile(path)
  WriteNorm2NFKCTextFile(path)
  WriteNorm2NFKC_CFTextFile(path, "nfkc_cf.txt", "NFKC_CF")
  WriteNorm2NFKC_CFTextFile(path, "nfkc_scf.txt", "NFKC_SCF")

# UTS #46 Normalizer2 input file ------------------------------------------- ***

_idna_replacements = [
  # Several versions of avoiding circular FFFD>FFFD mappings,
  # depending on the version of the input file.
  (re.compile(r"FFFD          ; disallowed"), "# FFFD (avoid circular mapping)"),
  (re.compile(r"\.\.FFFD"), "..FFFC"),
  (re.compile(r"(FFF[^E])\.\.FFFF"), "\1..FFFC"),
  # Since we switch between checking and not checking for STD3 character
  # restrictions at runtime, checking the non-LDH ASCII characters in code,
  # we treat these values here like their regular siblings.
  (re.compile(r"^([^;]+)  ; disallowed_STD3_valid"), r"# \1disallowed_STD3_valid"),
  (re.compile(r"; disallowed_STD3_mapped +; "), ">"),
  # For UTS #46, we do not care about "not valid in IDNA2008".
  (re.compile(r"; *; NV8 +"), ""),
  # ICU 63+ normalization no longer allows mappings for surrogate code points,
  # and the UTS #46 code handles them instead.
  (re.compile(r"^D800..DFFF    ; disallowed"), r"# D800..DFFF disallowed in code"),
  # Normal transformations.
  (re.compile(r"; disallowed"), ">FFFD"),
  (re.compile(r"; ignored"), ">"),
  (re.compile(r"^([^;]+)  ; valid"), r"# \1valid"),
  (re.compile(r"; mapped +; "), ">"),
  (re.compile(r"^([^;]+)  ; deviation +; "), r"# \1deviation >")
]

def IdnaToUTS46TextFile(s, t):
  """Turn Unicode IdnaMappingTable.txt into ICU gennorm2 source file format."""
  # Different input/output file names.
  dest_path = os.path.dirname(t)
  t = os.path.join(dest_path, "uts46.txt")
  # TODO: With Python 2.7+, combine the two with statements into one.
  with open(s, "r") as in_file:
    with open(t, "w") as out_file:
      out_file.write("# Original file:\n")
      for line in in_file:
        orig_line = line
        if line.startswith("# For documentation"):
          out_file.write(line)
          out_file.write(r"""
# ================================================
# This file has been reformatted into syntax for the
# gennorm2 Normalizer2 data generator tool.
#
# "valid", "disallowed_STD3_valid" and "deviation" lines are commented out.
# "mapped" and "disallowed_STD3_mapped" are changed to use the ">" mapping syntax.
# "disallowed" lines map to U+FFFD.
# "ignored" lines map to an empty string.
#
# Characters disallowed under STD3 rules are treated as valid or mapped;
# they are handled in code.
# Deviation characters are also handled in code.
#
# Use this file as the second gennorm2 input file after nfc.txt.
# ================================================
""")
          continue
        if line[0] in "#\r\n":
          out_file.write(line)
          continue
        for rep in _idna_replacements: line = rep[0].sub(rep[1], line)
        # Align inline comments at column 40.
        comment_pos = line.find("#", 1)
        if comment_pos < 40:
          line = (line[:comment_pos] + ((40 - comment_pos) * ' ') +
                  line[comment_pos:])
        elif comment_pos > 40:
          space_pos = comment_pos
          while space_pos > 0 and line[space_pos - 1] == ' ':
            space_pos = space_pos - 1
          if space_pos < 40:
            # Fewer than 40 characters before the comment:
            # Align comments at column 40.
            line = line[:40] + line[comment_pos:]
          else:
            # 40 or more characters before the comment:
            # Keep one space between contents and comment.
            line = line[:space_pos] + " " + line[comment_pos:]
        # Write the modified line.
        out_file.write(line)
        if "..FFFF" in orig_line and "..FFFC" in line:
          out_file.write("FFFE..FFFF    >FFFD\n");
  return t

# Preprocessing ------------------------------------------------------------ ***

_strip_re = re.compile("([0-9a-fA-F]+.+?) *#.*")
_code_point_re = re.compile("\s*([0-9a-fA-F]+)\s*;")

def CopyAndStripWithOptionalMerge(s, t, do_merge):
  # TODO: We do not seem to need the do_merge argument and logic any more.
  with open(s, "r") as in_file, open(t, "w") as out_file:
    first = -1  # First code point with first_data.
    last = -1  # Last code point with first_data.
    first_data = ""  # Common data for code points [first..last].
    for line in in_file:
      match = _strip_re.match(line)
      if match:
        line = match.group(1)
      else:
        line = line.rstrip()
      if do_merge:
        match = _code_point_re.match(line)
        if match:
          c = int(match.group(1), 16)
          data = line[match.end() - 1:]
        else:
          c = -1
          data = ""
        if last >= 0 and (c != (last + 1) or data != first_data):
          # output the current range
          if first == last:
            out_file.write("%04X%s\n" % (first, first_data))
          else:
            out_file.write("%04X..%04X%s\n" % (first, last, first_data))
          first = -1
          last = -1
          first_data = ""
        if c < 0:
          # no data on this line, output as is
          out_file.write(line)
          out_file.write("\n")
        else:
          # data on this line, store for possible range compaction
          if last < 0:
            # set as the first line in a possible range
            first = c
            last = c
            first_data = data
          else:
            # must be c == (last + 1) and data == first_data
            # because of previous conditions
            # continue with the current range
            last = c
      else:
        # Only strip, don't merge: just output the stripped line.
        out_file.write(line)
        out_file.write("\n")
    if do_merge and last >= 0:
      # output the last range in the file
      if first == last:
        out_file.write("%04X%s\n" % (first, first_data))
      else:
        out_file.write("%04X..%04X%s\n" % (first, last, first_data))
      first = -1
      last = -1
      first_data = ""
    out_file.flush()
  return t


def CopyAndStrip(s, t):
  """Copies a file and removes comments behind data lines but not in others."""
  return CopyAndStripWithOptionalMerge(s, t, False)


def CopyAndStripAndMerge(s, t):
  """Copies and strips a file and merges lines.

  Copies a file, removes comments, and
  merges lines with adjacent code point ranges and identical per-code point
  data lines into one line with range syntax.
  """
  return CopyAndStripWithOptionalMerge(s, t, True)


def CopyOnly(s, t):
  shutil.copy(s, t)
  return t


def DontCopy(s, t):
  return s


# Each _files value is a
# (preprocessor, dest_folder, parser, order) tuple
# where all fields except the preprocessor are optional.
# After the initial preprocessing (copy/strip/merge),
# if a parser is specified, then a tuple is added to _files_to_parse
# at index "order" (default order 9).
# An explicit order number is set only for files that must be parsed
# before others.
_files = {
  "BidiBrackets.txt": (DontCopy, ParseBidiBrackets),
  "BidiMirroring.txt": (DontCopy, ParseBidiMirroring),
  "BidiTest.txt": (CopyOnly, "testdata"),
  "Blocks.txt": (DontCopy, ParseBlocks),
  "CaseFolding.txt": (CopyOnly, ParseCaseFolding),
  "DerivedAge.txt": (DontCopy, ParseDerivedAge),
  "DerivedBidiClass.txt": (DontCopy, ParseDerivedBidiClass),
  "DerivedCoreProperties.txt": (CopyAndStrip, ParseNamedProperties),
  "DerivedJoiningGroup.txt": (DontCopy, ParseDerivedJoiningGroup),
  "DerivedJoiningType.txt": (DontCopy, ParseDerivedJoiningType),
  "DerivedNormalizationProps.txt": (CopyAndStrip, ParseNamedProperties),
  "DerivedNumericValues.txt": (DontCopy, ParseDerivedNumericValues),
  "EastAsianWidth.txt": (DontCopy, ParseEastAsianWidth),
  "emoji-data.txt": (DontCopy, ParseNamedProperties),
  "emoji-sequences.txt": (CopyOnly,),
  "emoji-zwj-sequences.txt": (CopyOnly,),
  "GraphemeBreakProperty.txt": (DontCopy, ParseGraphemeBreakProperty),
  "GraphemeBreakTest-cldr.txt": (CopyOnly, "testdata"),
  "IdnaTestV2.txt": (CopyOnly, "testdata"),
  "IndicPositionalCategory.txt": (DontCopy, ParseIndicPositionalCategory),
  "IndicSyllabicCategory.txt": (DontCopy, ParseIndicSyllabicCategory),
  "LineBreak.txt": (DontCopy, ParseLineBreak),
  "LineBreakTest.txt": (CopyOnly, "testdata"),
  "NameAliases.txt": (DontCopy, ParseNameAliases),
  "NamesList.txt": (DontCopy, ParseNamesList),
  "NormalizationCorrections.txt": (CopyOnly,),  # Only used in gensprep.
  "NormalizationTest.txt": (CopyAndStrip,),
  "PropertyAliases.txt": (DontCopy, ParsePropertyAliases, 0),
  "PropertyValueAliases.txt": (DontCopy, ParsePropertyValueAliases, 1),
  "PropList.txt": (DontCopy, ParseNamedProperties),
  "SentenceBreakProperty.txt": (DontCopy, ParseSentenceBreak),
  "SentenceBreakTest.txt": (CopyOnly, "testdata"),
  "Scripts.txt": (DontCopy, ParseScripts),
  "ScriptExtensions.txt": (DontCopy, ParseScriptExtensions),
  "SpecialCasing.txt": (CopyOnly, ParseSpecialCasing),
  "UnicodeData.txt": (CopyOnly, ParseUnicodeData, 2),
  "VerticalOrientation.txt": (DontCopy, ParseVerticalOrientation),
  "WordBreakProperty.txt": (DontCopy, ParseWordBreak),
  "WordBreakTest.txt": (CopyOnly, "testdata"),
  # From www.unicode.org/Public/idna/<version>/
  "IdnaMappingTable.txt": (IdnaToUTS46TextFile, "norm2")
}

# List of lists of files to be parsed in order.
# Inner lists contain (basename, path, parser) tuples.
_files_to_parse = [[], [], [], [], [], [], [], [], [], []]

# Get the standard basename from a versioned filename.
# For example, match "UnicodeData-6.1.0d8.txt"
# so we can turn it into "UnicodeData.txt".
_file_version_re = re.compile("([a-zA-Z0-9_-]+)" +
                              "-[0-9]+(?:\\.[0-9]+)*(?:d[0-9]+)?" +
                              "(\\.[a-z]+)$")

def PreprocessFiles(source_files, icu4c_src_root):
  unidata_path = os.path.join(icu4c_src_root, "source", "data", "unidata")
  norm2_path = os.path.join(unidata_path, "norm2")
  testdata_path = os.path.join(icu4c_src_root, "source", "test", "testdata")
  folder_to_path = {
    "unidata": unidata_path,
    "norm2": norm2_path,
    "testdata": testdata_path
  }
  files_processed = set()
  for source_file in source_files:
    (folder, basename) = os.path.split(source_file)
    match = _file_version_re.match(basename)
    if match:
      new_basename = match.group(1) + match.group(2)
      if new_basename != basename:
        print("Removing version suffix from " + source_file)
        # ... so that we can easily compare UCD files.
        new_source_file = os.path.join(folder, new_basename)
        shutil.move(source_file, new_source_file)
        basename = new_basename
        source_file = new_source_file
    if basename in _files:
      print("Preprocessing %s" % basename)
      if basename in files_processed:
        raise Exception("duplicate file basename %s!" % basename)
      files_processed.add(basename)
      value = _files[basename]
      preprocessor = value[0]
      if len(value) >= 2 and isinstance(value[1], (str)):
        # The value was [preprocessor, dest_folder, ...], leave [...].
        dest_folder = value[1]
        value = value[2:]
      else:
        # The value was [preprocessor, ...], leave [...].
        dest_folder = "unidata"
        value = value[1:]
      dest_path = folder_to_path[dest_folder]
      if not os.path.exists(dest_path): os.makedirs(dest_path)
      dest_basename = basename
      # Source GraphemeBreakTest-cldr.txt --> destination GraphemeBreakTest.txt.
      if basename.endswith("-cldr.txt"):
        dest_basename = basename[:-9] + basename[-4:]
      dest_file = os.path.join(dest_path, dest_basename)
      parse_file = preprocessor(source_file, dest_file)
      if value:
        order = 9 if len(value) < 2 else value[1]
        _files_to_parse[order].append((basename, parse_file, value[0]))

# Character names ---------------------------------------------------------- ***

# TODO: Turn this script into a module that
# a) gives access to the parsed data
# b) has a PreparseUCD(ucd_root, icu4c_src_root) function
# c) has a ParsePreparsedUCD(filename) function
# d) has a WritePreparsedUCD(filename) function
# and then use it from a new script for names.
# Some more API:
# - generator GetRangesAndProps() -> (start, end, props)*

def IncCounter(counters, key, inc=1):
  if key in counters:
    counters[key] += inc
  else:
    counters[key] = inc


endings = (
  # List PHASE- before LETTER for BAMUM LETTER PHASE-xyz.
  "PHASE-",
  "LETTER ", "LIGATURE ", "CHARACTER ", "SYLLABLE ",
  "CHOSEONG ", "JUNGSEONG ", "JONGSEONG ",
  "SYLLABICS ", "IDEOGRAPH ", "IDEOGRAPH-", "IDEOGRAM ", "MONOGRAM ",
  "ACROPHONIC ", "HIEROGLYPH ",
  "DIGIT ", "NUMBER ", "NUMERAL ", "FRACTION ",
  "PUNCTUATION ", "SIGN ", "SYMBOL ",
  "TILE ", "CARD ", "FACE ",
  "ACCENT ", "POINT ",
  # List SIGN before VOWEL to catch "vowel sign".
  "VOWEL ", "TONE ", "RADICAL ",
  # For names of math symbols,
  # e.g., MATHEMATICAL BOLD ITALIC CAPITAL A
  "SCRIPT ", "FRAKTUR ", "MONOSPACE ",
  "ITALIC ", "BOLD ", "DOUBLE-STRUCK ", "SANS-SERIF ",
  "INITIAL ", "TAILED ", "STRETCHED ", "LOOPED ",
  # BRAILLE PATTERN DOTS-xyz
  "DOTS-",
  "SELECTOR ", "SELECTOR-"
)

def SplitName(name, tokens):
  start = 0
  for e in endings:
    i = name.find(e)
    if i >= 0:
      start = i + len(e)
      token = name[:start]
      IncCounter(tokens, token)
      break
  for i in range(start, len(name)):
    c = name[i]
    if c == ' ' or c == '-':
      token = name[start:i + 1]
      IncCounter(tokens, token)
      start = i + 1
  IncCounter(tokens, name[start:])


def PrintNameStats():
  # TODO: This name analysis code is out of date.
  # It needs to consider the multi-type Name_Alias values.
  name_pnames = ("na", "na1", "Name_Alias")
  counts = {}
  for pname in name_pnames:
    counts[pname] = 0
  total_lengths = counts.copy()
  max_length = 0
  max_per_cp = 0
  name_chars = set()
  num_digits = 0
  token_counters = {}
  char_counters = {}
  for i in range(len(_starts) - 1):
    start = _starts[i]
    # end = _starts[i + 1] - 1
    props = _props[i]
    per_cp = 0
    for pname in name_pnames:
      if pname in props:
        counts[pname] += 1
        name = props[pname]
        total_lengths[pname] += len(name)
        name_chars |= set(name)
        if len(name) > max_length: max_length = len(name)
        per_cp += len(name) + 1
        if per_cp > max_per_cp: max_per_cp = per_cp
        tokens = SplitName(name, token_counters)
        for c in name:
          if c in "0123456789": num_digits += 1
          IncCounter(char_counters, c)
  print
  for pname in name_pnames:
    print("'%s' character names: %d / %d bytes" %
          (pname, counts[pname], total_lengths[pname]))
  print("%d total bytes in character names" % sum(total_lengths.itervalues()))
  print("%d name-characters: %s" %
        (len(name_chars), "".join(sorted(name_chars))))
  print("%d digits 0-9" % num_digits)
  count_chars = [(count, c) for (c, count) in char_counters.items()]
  count_chars.sort(reverse=True)
  for cc in count_chars:
    print("name-chars: %6d * '%s'" % cc)
  print("max. name length: %d" % max_length)
  print("max. length of all (names+NUL) per cp: %d" % max_per_cp)

  token_lengths = sum([len(t) + 1 for t in token_counters])
  print("%d total tokens, %d bytes with NUL" %
        (len(token_counters), token_lengths))

  counts_tokens = []
  for (token, count) in token_counters.items():
    # If we encode a token with a 1-byte code, then we save len(t)-1 bytes each time
    # but have to store the token string itself with a length or terminator byte,
    # plus a 2-byte entry in an token index table.
    savings = count * (len(token) - 1) - (len(token) + 1 + 2)
    if savings > 0:
      counts_tokens.append((savings, count, token))
  counts_tokens.sort(reverse=True)
  print("%d tokens might save space with 1-byte codes" % len(counts_tokens))

  # Codes=bytes, 40 byte values for name_chars.
  # That leaves 216 units for 1-byte tokens or lead bytes of 2-byte tokens.
  # Make each 2-byte token the token string index itself, rather than
  # and index into a string index table.
  # More lead bytes but also more savings.
  num_units = 256
  max_lead = (token_lengths + 255) / 256
  max_token_units = num_units - len(name_chars)
  results = []
  for num_lead in range(min(max_lead, max_token_units) + 1):
    max1 = max_token_units - num_lead
    ct = counts_tokens[:max1]
    tokens1 = set([t for (s, c, t) in ct])
    for (token, count) in token_counters.items():
      if token in tokens1: continue
      # If we encode a token with a 2-byte code, then we save len(t)-2 bytes each time
      # but have to store the token string itself with a length or terminator byte.
      savings = count * (len(token) - 2) - (len(token) + 1)
      if savings > 0:
        ct.append((savings, count, token))
    ct.sort(reverse=True)
    # A 2-byte-code-token index cannot be limit_t_lengths or higher.
    limit_t_lengths = num_lead * 256
    token2_index = 0
    for i in range(max1, len(ct)):
      if token2_index >= limit_t_lengths:
        del ct[i:]
        break
      token2_index += len(ct[i][2]) + 1
    cumul_savings = sum([s for (s, c, t) in ct])
    # print ("%2d 1-byte codes: %4d tokens might save %6d bytes" %
    #        (max1, len(ct), cumul_savings))
    results.append((cumul_savings, max1, ct))
  best = max(results)  # (cumul_savings, max1, ct)

  max1 = best[1]
  print("maximum savings: %d bytes with %d 1-byte codes & %d lead bytes" %
         (best[0], max1, max_token_units - max1))
  counts_tokens = best[2]
  cumul_savings = 0
  for i in range(len(counts_tokens)):
    n = 1 if i < max1 else 2
    i1 = i + 1
    t = counts_tokens[i]
    cumul_savings += t[0]
    if i1 <= 250 or (i1 % 100) == 0 or i1 == len(counts_tokens):
      print(("%04d. cumul. %6d bytes save %6d bytes from " +
              "%5d * %d-byte token for %2d='%s'") %
          (i1, cumul_savings, t[0], t[1], n, len(t[2]), t[2]))

# ICU API ------------------------------------------------------------------ ***

# Sample line to match:
#    UCHAR_UNIFIED_IDEOGRAPH=29,
_uchar_re = re.compile(
    " *(UCHAR_[0-9A-Z_]+) *= *(?:[0-9]+|0x[0-9a-fA-F]+),")

# Sample line to match:
#    /** Zs @stable ICU 2.0 */
_gc_comment_re = re.compile(" */\*\* *([A-Z][a-z]) ")

# Sample line to match:
#    U_SPACE_SEPARATOR         = 12,
_gc_re = re.compile(" *(U_[A-Z_]+) *= *[0-9]+,")

# Sample line to match:
#    /** L @stable ICU 2.0 */
_bc_comment_re = re.compile(" */\*\* *([A-Z]{1,3}) ")

# Sample line to match:
#    U_LEFT_TO_RIGHT               = 0,
_bc_re = re.compile(" *(U_[A-Z_]+) *= *[0-9]+,")

# Sample line to match:
#    UBLOCK_CYRILLIC =9,
_ublock_re = re.compile(" *(UBLOCK_[0-9A-Z_]+) *= *[0-9]+,")

# Sample line to match:
#    U_EA_AMBIGUOUS,
_prop_and_value_re = re.compile(
    " *(U_(BPT|DT|EA|GCB|HST|INPC|INSC|LB|JG|JT|NT|SB|VO|WB)_([0-9A-Z_]+))")

# Sample line to match if it has matched _prop_and_value_re
# (we want to exclude aliases):
#    U_JG_HAMZA_ON_HEH_GOAL=U_JG_TEH_MARBUTA_GOAL,
_prop_and_alias_re = re.compile(" *U_[0-9A-Z_]+ *= *U")

def ParseUCharHeader(icu4c_src_root):
  uchar_path = os.path.join(icu4c_src_root, "source",
                            "common", "unicode", "uchar.h")
  with open(uchar_path, "r") as uchar_file:
    mode = ""  # Mode string (=pname) during context-sensitive parsing.
    comment_value = ""  # Property value from a comment preceding an enum.
    # Note: The enum UProperty is first in uchar.h, before the enums for values.
    for line in uchar_file:
      # Parse some enums via context-sensitive "modes".
      # Necessary because the enum constant names do not contain
      # enough information.
      if "enum UCharCategory" in line:
        mode = "gc"
        comment_value = ""
        continue
      if mode == "gc":
        if line.startswith("}"):
          mode = ""
          continue
        match = _gc_comment_re.match(line)
        if match:
          comment_value = match.group(1)
          continue
        match = _gc_re.match(line)
        if match and comment_value:
          gc_enum = match.group(1)
          prop = _properties["gc"]
          vname = GetShortPropertyValueName(prop, comment_value)
          icu_values = _pname_to_icu_prop["gc"][2]
          icu_values.append((gc_enum, vname))
        comment_value = ""
        continue
      if "enum UCharDirection {" in line:
        mode = "bc"
        comment_value = ""
        continue
      if mode == "bc":
        if line.startswith("}"):
          mode = ""
          continue
        match = _bc_comment_re.match(line)
        if match:
          comment_value = match.group(1)
          continue
        match = _bc_re.match(line)
        if match and comment_value:
          bc_enum = match.group(1)
          prop = _properties["bc"]
          vname = GetShortPropertyValueName(prop, comment_value)
          icu_values = _pname_to_icu_prop["bc"][2]
          icu_values.append((bc_enum, vname))
        comment_value = ""
        continue
      # No mode, parse enum constants whose names contain
      # enough information to parse without requiring context.
      match = _uchar_re.match(line)
      if match:
        prop_enum = match.group(1)
        if prop_enum.endswith("_LIMIT"):
          # Ignore "UCHAR_BINARY_LIMIT=57," etc.
          continue
        pname = GetShortPropertyName(prop_enum[6:])
        icu_prop = (prop_enum, pname, [])
        _icu_properties.append(icu_prop)
        _pname_to_icu_prop[pname] = icu_prop
        continue
      match = _ublock_re.match(line)
      if match:
        prop_enum = match.group(1)
        if prop_enum == "UBLOCK_COUNT":
          continue
        prop = _properties["blk"]
        vname = GetShortPropertyValueName(prop, prop_enum[7:])
        icu_values = _pname_to_icu_prop["blk"][2]
        icu_values.append((prop_enum, vname))
        continue
      match = _prop_and_value_re.match(line)
      if match:
        (prop_enum, vname) = match.group(1, 3)
        if vname == "COUNT" or _prop_and_alias_re.match(line):
          continue
        pname = GetShortPropertyName(match.group(2))
        prop = _properties[pname]
        vname = GetShortPropertyValueName(prop, vname)
        icu_values = _pname_to_icu_prop[pname][2]
        icu_values.append((prop_enum, vname))
  # ccc, lccc, tccc use their numeric values as "enum" values.
  # In the UCD data, these numeric values are the first value names,
  # followed by the short & long value names.
  # List the ccc values in numeric order.
  prop = _properties["ccc"]
  icu_values = _pname_to_icu_prop["ccc"][2]
  for ccc in sorted([int(name) for name in prop[2]]):
    icu_values.append((ccc, str(ccc)))
  _pname_to_icu_prop["lccc"][2].extend(icu_values)  # Copy ccc -> lccc.
  _pname_to_icu_prop["tccc"][2].extend(icu_values)  # Copy ccc -> tccc.

  # No need to parse predictable General_Category_Mask enum constants.
  # Just define them in ASCII order.
  prop = _properties["gcm"]
  icu_values = _pname_to_icu_prop["gcm"][2]
  for vname in sorted(prop[2]):
    icu_values.append(("U_GC_" + vname.upper() + "_MASK", vname))
  # Hardcode known values for the normalization quick check properties,
  # see unorm2.h for the UNormalizationCheckResult enum.
  icu_values = _pname_to_icu_prop["NFC_QC"][2]
  icu_values.append(("UNORM_NO", "N"))
  icu_values.append(("UNORM_YES", "Y"))
  icu_values.append(("UNORM_MAYBE", "M"))
  _pname_to_icu_prop["NFKC_QC"][2].extend(icu_values)  # Copy NFC -> NFKC.
  # No "maybe" values for NF[K]D.
  icu_values = _pname_to_icu_prop["NFD_QC"][2]
  icu_values.append(("UNORM_NO", "N"))
  icu_values.append(("UNORM_YES", "Y"))
  _pname_to_icu_prop["NFKD_QC"][2].extend(icu_values)  # Copy NFD -> NFKD.


# Sample line to match:
#    USCRIPT_LOMA   = 139,/* Loma */
_uscript_re = re.compile(
    " *(USCRIPT_[A-Z_]+) *= *[0-9]+ *, */\* *([A-Z][a-z]{3}) *\*/")

def ParseUScriptHeader(icu4c_src_root):
  uscript_path = os.path.join(icu4c_src_root, "source",
                              "common", "unicode", "uscript.h")
  icu_values = _pname_to_icu_prop["sc"][2]
  with open(uscript_path, "r") as uscript_file:
    for line in uscript_file:
      match = _uscript_re.match(line)
      if match:
        (script_enum, script_code) = match.group(1, 2)
        icu_values.append((script_enum, script_code))


def CheckPNamesData():
  """Checks that every ICU property has a full set of value enum constants,
  and that the _icu_properties value names map back to the UCD."""
  missing_enums = []
  for (p_enum, pname, values) in _icu_properties:
    prop = _properties[pname]
    vnames = set(prop[2])  # Modifiable copy of the set of short value names.
    for (v_enum, vname) in values:
      if vname not in vnames:
        raise ValueError("%s = %s (uchar.h %s) not in the UCD\n" %
                         (pname, vname, v_enum))
      vnames.remove(vname)
    # Exceptions to the all-values check:
    # - ICU does not have specific enum values for binary No/Yes.
    # - ICU represents Age values via UVersionInfo rather than enum constants.
    # - gc: ICU enum UCharCategory only has the single-category values.
    #       (ICU's gcm property has all of the UCD gc property values.)
    if vnames and not (prop[0] == "Binary" or pname in ("age", "gc")):
      missing_enums.append((pname, vnames))
  if missing_enums:
    raise ValueError(
        "missing uchar.h enum constants for some property values: %s" %
        missing_enums)


def WritePNamesDataHeader(out_path):
  with open(out_path, "w") as out_file:
    out_file.write("""// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
 * Copyright (C) 2002-2016, International Business Machines Corporation and
 * others. All Rights Reserved.
 *
 * machine-generated by: icu/tools/unicode/py/preparseucd.py
 */

""")

    # Note: The uchar.h & uscript.h parsers store the ICU Unicode properties
    # and values in the order of their definition,
    # and this function writes them in that order.
    # Since the ICU API constants are stable and new values are only
    # appended at the end
    # (new properties are added at the end of each binary/enum/... range),
    # the output is stable as well.
    # When a property or value constant is renamed,
    # it only changes the name itself in the output;
    # it does not move in the output since there is no sorting.
    # This minimizes diffs and assists with reviewing and evaluating updates.

    version = _ucd_version.split('.')
    while len(version) < 4: version.append("0")
    out_file.write("#define UNICODE_VERSION { %s }\n\n" % ", ".join(version))

    # Count the maximum number of aliases for any property or value.
    # We write the final value at the end.
    max_aliases = max(len(_binary_values["N"]), len(_binary_values["Y"]))

    # Write an array of "binprop" Value object initializers
    # with the value aliases shared among all binary properties.
    out_file.write("static const Value VALUES_binprop[2] = {\n")
    out_file.write('    Value(0, "%s"),\n' % " ".join(_binary_values["N"]))
    out_file.write('    Value(1, "%s"),\n' % " ".join(_binary_values["Y"]))
    out_file.write("};\n\n")

    # For each property with named values, write an array of
    # Value object initializers with the value enum and the aliases.
    for (p_enum, pname, values) in _icu_properties:
      prop = _properties[pname]
      aliases = prop[1]
      if len(aliases) > max_aliases: max_aliases = len(aliases)
      if not values: continue
      out_file.write("static const Value VALUES_%s[%d] = {\n" %
                     (pname, len(values)))
      for (v_enum, vname) in values:
        aliases = _properties[pname][3][vname]
        # ccc, lccc, tccc: Omit the numeric strings from the aliases.
        # (See the comment about ccc in the PropertyValueAliases.txt header.)
        if pname.endswith("ccc"): aliases = aliases[1:]
        if len(aliases) > max_aliases: max_aliases = len(aliases)
        cast = "(int32_t)" if pname == "gcm" else ""
        out_file.write('    Value(%s%s, "%s"),\n' %
                       (cast, v_enum, " ".join(aliases)))
      out_file.write("};\n\n")

    # For each property, write a Property object initializer
    # with the property enum, its aliases, and a reference to its values.
    out_file.write("static const Property PROPERTIES[%d] = {\n" %
                   len(_icu_properties))
    for (enum, pname, values) in _icu_properties:
      prop = _properties[pname]
      aliases = " ".join(prop[1])
      if prop[0] == "Binary":
        out_file.write('    Property(%s, "%s"),\n' % (enum, aliases))
      elif values:  # Property with named values.
        out_file.write('    Property(%s, "%s", VALUES_%s, %d),\n' %
                       (enum, aliases, pname, len(values)))
      else:
        out_file.write('    Property(%s, "%s"),\n' % (enum, aliases))
    out_file.write("};\n\n")

    out_file.write("const int32_t MAX_ALIASES = %d;\n" % max_aliases)

# main() ------------------------------------------------------------------- ***

def main():
  global _null_or_defaults
  only_ppucd = False
  if len(sys.argv) == 3:
    (ucd_root, icu_src_root) = sys.argv[1:3]
    ppucd_path = None
  elif len(sys.argv) == 4 and sys.argv[2] == "--only_ppucd":
    # For debugging:
    # preparseucd.py  path/to/UCD/root  --only_ppucd  path/to/ppucd/outputfile
    ucd_root = sys.argv[1]
    ppucd_path = sys.argv[3]
    only_ppucd = True
    icu_src_root = "/tmp/ppucd"
  else:
    print("Usage: %s  path/to/UCD/root  path/to/ICU/src/root" % sys.argv[0])
    return
  icu4c_src_root = os.path.join(icu_src_root, "icu4c")
  icu_tools_root = os.path.join(icu_src_root, "tools")
  source_files = []
  for root, dirs, files in os.walk(ucd_root):
    for file in files:
      source_files.append(os.path.join(root, file))
  PreprocessFiles(source_files, icu4c_src_root)
  # Parse the processed files in a particular order.
  for files in _files_to_parse:
    for (basename, path, parser) in files:
      print("Parsing %s" % basename)
      value = _files[basename]
      # Unicode data files are in UTF-8.
      charset = "UTF-8"
      if basename == "NamesList.txt":
        # The NamesList used to be in Latin-1 before Unicode 6.2.
        numeric_ucd_version = [int(field) for field in _ucd_version.split('.')]
        if numeric_ucd_version < [6, 2]: charset = "ISO-8859-1"
      in_file = codecs.open(path, "r", charset)
      with in_file:
        parser(in_file)
  _null_or_defaults = _null_values.copy()
  _null_or_defaults.update(_defaults)
  # Every Catalog and Enumerated property must have a default value,
  # from a @missing line. "nv" = "null value".
  pnv = [pname for (pname, nv) in _null_or_defaults.items() if nv == "??"]
  if pnv:
    raise Exception("no default values (@missing lines) for " +
                    "some Catalog or Enumerated properties: %s " % pnv)
  unidata_path = os.path.join(icu4c_src_root, "source", "data", "unidata")
  if not only_ppucd:
    # Write Normalizer2 input text files.
    # Do this before compacting the data so that we need not handle fallbacks.
    norm2_path = os.path.join(unidata_path, "norm2")
    if not os.path.exists(norm2_path): os.makedirs(norm2_path)
    WriteNorm2(norm2_path)
  # Optimize block vs. cp properties.
  CompactBlocks()
  # Write the ppucd.txt output file.
  # Use US-ASCII so that ICU tests can parse it in the platform charset,
  # which may be EBCDIC.
  # Fix up non-ASCII data (NamesList.txt headings) to fit.
  if not ppucd_path:
    ppucd_path = os.path.join(unidata_path, "ppucd.txt")
  with codecs.open(ppucd_path, "w", "US-ASCII") as out_file:
    WritePreparsedUCD(out_file)
    out_file.flush()

  # TODO: PrintNameStats()

  if only_ppucd: return

  # ICU data for property & value names API
  ParseUCharHeader(icu4c_src_root)
  ParseUScriptHeader(icu4c_src_root)
  CheckPNamesData()
  genprops_path = os.path.join(icu_tools_root, "unicode", "c", "genprops")
  if not os.path.exists(genprops_path): os.makedirs(genprops_path)
  out_path = os.path.join(genprops_path, "pnames_data.h")
  WritePNamesDataHeader(out_path)


if __name__ == "__main__":
  main()
