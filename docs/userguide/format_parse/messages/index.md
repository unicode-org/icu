---
layout: default
title: Formatting Messages
nav_order: 3
parent: Formatting
has_children: true
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Formatting Messages
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

Messages are user-visible strings, often with variable elements like names,
numbers and dates. Message strings are typically translated into the different
languages of a UI, and translators move around the variable elements according
to the grammar of the target language.

For this to work in many languages, a message has to be written and translated
as a single unit, typically a string with placeholder syntax for the variable
elements. If the user-visible string were concatenated directly from fragments
and formatted elements, then translators would not be able to rearrange the
pieces, and they would have a hard time translating each of the string
fragments.

## `MessageFormat`

The ICU **`MessageFormat`** class uses message `"pattern"` strings with
variable-element placeholders (called "arguments" in the API docs) enclosed in
{curly braces}. The argument syntax can include formatting details, otherwise a
default format is used. For details about the pattern syntax and the formatting
behavior see the `MessageFormat` API docs
([Java](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/MessageFormat.html),
[C++](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/classMessageFormat.html#_details),
[C](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/umsg_8h.html#_details)).

### Complex Argument Types

Certain types of arguments select among several choices which are nested
`MessageFormat` pattern strings. Keeping these choices together in one message
pattern string facilitates translation in context, by one single translator.
(Commercial translation systems often distribute different messages to different
translators.)

*   Use a `"plural"` argument to select sub-messages based on a numeric value,
    together with the plural rules for the specified language.
*   Use a `"select"` argument to select sub-messages via a fixed set of keywords.
*   Use of the old `"choice"` argument type is discouraged. It cannot handle
    plural rules for many languages, and is clumsy for simple selection.

It is tempting to cover only a minimal part of a message string with a complex
argument (e.g., plural). However, this is difficult for translators for two
reasons: 1. They might have trouble understanding how the sentence fragments in
the argument sub-messages interact with the rest of the sentence, and 2. They
will not know whether and how they can shrink or grow the extent of the part of
the sentence that is inside the argument to make the whole message work for
their language.

**Recommendation:** If possible, use complex arguments as the outermost
structure of a message, and write **full sentences** in their sub-messages. If
you have nested select and plural arguments, place the **select** arguments
(with their fixed sets of choices) on the **outside** and nest the plural
arguments (hopefully at most one) inside.

For example:

{% raw  %}

```text
"{gender_of_host, select, "
  "female {"
    "{num_guests, plural, offset:1 "
      "=0 {{host} does not give a party.}"
      "=1 {{host} invites {guest} to her party.}"
      "=2 {{host} invites {guest} and one other person to her party.}"
      "other {{host} invites {guest} and # other people to her party.}}}"
  "male {"
    "{num_guests, plural, offset:1 "
      "=0 {{host} does not give a party.}"
      "=1 {{host} invites {guest} to his party.}"
      "=2 {{host} invites {guest} and one other person to his party.}"
      "other {{host} invites {guest} and # other people to his party.}}}"
  "other {"
    "{num_guests, plural, offset:1 "
      "=0 {{host} does not give a party.}"
      "=1 {{host} invites {guest} to their party.}"
      "=2 {{host} invites {guest} and one other person to their party.}"
      "other {{host} invites {guest} and # other people to their party.}}}}"
```

{% endraw %}

**Note:** In a plural argument like in the example above, if the English message
has both `=0` and `=1` (up to `=offset`+1) then it does not need a "`one`"
variant because that would never be selected. It does always need an "`other`"
variant.

**Note:** *The translation system and the translator together need to add
["`one`", "`few`" etc. if and as necessary per target
language](http://cldr.unicode.org/index/cldr-spec/plural-rules).*

### Quoting/Escaping

If syntax characters occur in the text portions, then they need to be quoted by
enclosing the syntax in pairs of ASCII apostrophes. A pair of ASCII apostrophes
always represents one ASCII apostrophe, similar to `%%` in `printf` representing one `%`,
although this rule still applies inside quoted text. ("`This '{isn''t}' obvious`" → "`This {isn't} obvious`")

*   Before ICU 4.8, ASCII apostrophes always started quoted text and had
    inconsistent behavior in nested sub-messages, which was a source of problems
    with authoring and translating message pattern strings.
*   Starting with ICU 4.8, an ASCII apostrophe only starts quoted text if it
    immediately precedes a character that requires quoting (that is, "only where
    needed"), and works the same in nested messages as on the top level of the
    pattern. The new behavior is otherwise compatible; for details see the
    MessageFormat and MessagePattern (new in ICU 4.8) API docs.
*   Recommendation: Use the real apostrophe (single quote) character `’` (U+2019)
    for human-readable text, and use the ASCII apostrophe `'` (U+0027) only in
    program syntax, like quoting in MessageFormat. See the annotations for
    U+0027 Apostrophe in The Unicode Standard.

### Argument formatting

Arguments are formatted according to their type, using the default ICU
formatters for those types, unless otherwise specified. For unknown types the
Java `MessageFormat` will call `toString()`.

There are also several ways to control the formatting.

#### Predefined styles (recommended)

You can specify the `argStyle` to be one of the predefined values `short`, `medium`,
`long`, `full` (to get one of the standard forms for dates / times) and `integer`,
`currency`, `percent` (for number formatting).

#### Skeletons (recommended)

Numbers, dates, and times can use a skeleton in `argStyle`, prefixed with `::` to
distinguish them from patterns. These are locale-independent ways to specify the
format, and this is the recommended mechanism if the predefined styles are not
appropriate.

##### Date skeletons:

- **ICU4J:**
<https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/SimpleDateFormat.html>

- **ICU4C:** <https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/classSimpleDateFormat.html>

##### Number formatter skeletons:

- **ICU4J:**
<https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/number/NumberFormatter.html>

- **ICU4C:** <https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/classicu_1_1NumberFormat.html>

#### Format the parameters separately (recommended)

You can format the parameter as you need **before** calling `MessageFormat`, and
then passing the resulting string as a parameter to `MessageFormat`.

This offers maximum control, and is preferred to using custom format objects
(see below).

#### String patterns (discouraged)

These can be used for numbers, dates, and times, but they are locale-sensitive,
and they therefore would need to be localized by your translators, which adds
complexity to the localization, and placeholder details are often not accessible
by translators. If such a pattern is not localized, then users see confusing
formatting. Consider using skeletons instead of patterns in your message
strings.

Allowing translators to localize date patterns is error-prone, as translators
might make mistakes (resulting in invalid ICU date formatter syntax). Also, CLDR
provides curated patterns for many locales, and using your own pattern means
that you don't benefit from that CLDR data and the results will likely be
inconsistent with the rest of the patterns that ICU uses.

It is also a bad internationalization practice, because most companies only
translate into "generic" versions of the languages (French, or Spanish, or
Arabic). So the translated patterns get used in tens of countries. On the other
hand, skeletons are localized according to the MessageFormat locale, which
should include regional variants (e.g., “fr-CA”).

#### Custom Format Objects (discouraged)

The `MessageFormat` class allows setting custom Format objects to format
arguments, overriding the arguments' pattern specification. This is discouraged:
For custom formatting of some values it should normally suffice to format them
externally and to provide the formatted strings to the `MessageFormat.format()`
methods.

Only the top-level arguments are accessible and settable via `setFormat()`,
`getFormat()` etc. Arguments inside nested sub-messages, inside
choice/plural/select arguments, are "invisible" via these API methods.

Some of these methods (the ones corresponding to the original JDK `MessageFormat`
API) address the top-level arguments in their order of appearance in the pattern
string, which is usually not useful because it varies with translations. Newer
methods address arguments by argument number ("index") or name.

### Examples

The following code fragment created this output: "At 4:34 PM on March 23, there
was a disturbance in the Force on planet 7."

```cpp
    UErrorCode err = U_ZERO_ERROR;
    Formattable arguments[] = {
       (int32_t)7,
       Formattable(Calendar.getNow(), Formattable::kIsDate),
       "a disturbance in the Force"
    };

    UnicodeString result;
    result = MessageFormat::format(
       "At {1,time,::jmm} on {1,date,::dMMMM}, there was {2} on planet{0,number,integer}.",
       arguments,
       3,
       result,
       err);
```

There are several more usage examples for the `MessageFormat` and `ChoiceFormat`
classes in [C , C++ and Java](examples.md).
