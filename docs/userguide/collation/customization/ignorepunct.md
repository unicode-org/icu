---
layout: default
title: Ignore Punctuation Options
nav_order: 8
parent: Collation
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# “Ignore Punctuation” Options
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

By default, spaces and punctuation characters add primary (base character)
differences. Such characters sort less-than digits and letters. For example, the
default collation yields “De Anza” < “de-luge” < “deanza”.

UCA/CLDR/ICU provide several options for “ignore punctuation” collation
settings, also known as Variable Weighting or Alternate Handling. These options
change the sorting behavior of “variable” characters algorithmically. “Variable”
characters are those with low (but non-zero) primary weights up to a threshold,
the “variable top”. By default, CLDR and ICU treat spaces and punctuation as
variable. (This can be changed via API.) The DUCET also includes most symbols.

## Non-Ignorable

The default behavior in CLDR & ICU, shown above, is to not ignore punctuation
(alternate=non-ignorable) but to map variable characters to their normal primary
collation elements.

All of the following options cause variable characters to be ignored on levels
1..3. Only when strings compare equal up to the tertiary level may variable
characters make a difference, depending on the options.

See also

*   [UCA: Variable
    Weighting](http://www.unicode.org/reports/tr10/#Variable_Weighting)
*   [LDML: Setting
    Options](https://htmlpreview.github.io/?https://github.com/unicode-org/cldr/blob/master/docs/ldml/tr35-collation.html#Setting_Options)

Here is an overview of the sorting results with these options.

Non-ignorable | Blanked      | Shifted | Shift-Trimmed | Variable-After
------------- | ------------ | ------- | ------------- | --------------
delug         | delug        | delug   | delug         | delug
de-luge       | de-luge      | de-luge | *deluge*      | *deluge*
delu-ge       | delu-ge (*)  | delu-ge | de-luge       | deluge-
*deluge*      | *deluge* (*) | *deluge* | delu-ge     | delu-ge
Deluge        | deluge- (*)  | deluge-  | deluge-     | de-luge
deluge-       | Deluge       | Deluge   | Deluge      | Deluge

Items with (*) compare equal to the preceding ones, and their relative order
is arbitrary. These only occur in the Blanked column. This table shows the
results of a stable sort algorithm with the non-ignorable column as input.

## Blanked

The simplest option is to “ignore punctuation” completely, as if all variable
characters (and following combining marks) had been removed from the input
strings before comparing them.

For example: “De Anza” = “De-Anza” = “DeAnza”.

In ICU, this option is selected with alternate=shifted and
strength=primary|secondary|tertiary. (ICU does not support Blanked combined with
strength=identical.)

The implementation “blanks” out all weights of the variable characters’
collation elements.

*With all of the following options, variable characters are ignored on levels
1..3 but add distinctions on level 4 (quaternary level).*

## Shifted

Among strings that compare tertiary-equal, that is, they contain the same
letters, accents and casing:

*   Sorts all variable characters less-than (before) regular characters.
*   Appending a variable character makes a string sort *greater-than* the string
    without it.
*   *Inserting* a variable character makes a string sort *less-than* the string
    without it.
*   Inserting a variable character *earlier* in a string makes it sort
    *less-than* inserting the variable character *later* in the string.

The result is similar to [Merging Sort
Keys](http://www.unicode.org/reports/tr10/#Merging_Sort_Keys) (with shorter
prefixes sorting less-than longer ones), like in last-name+first-name sorting,
except only among tertiary-equal strings.

For example: “de-luge” < “delu-ge” < “deluge” < “deluge-”.

In ICU, this option is selected with alternate=shifted and
strength=quaternary|identical.

The implementation “shifts” the primary weight p of the collation element \[p,
s, t, q\] of each variable characters down three levels: \[0, 0, 0, p\]. Regular
characters with primary collation elements get a high quaternary weight, higher
than that of any variable character.

Note that this behavior is different from collation on secondary and tertiary
level, because normal collation elements get low secondary & tertiary weights
but high quaternary weights. Adding an accent difference anywhere makes a string
sort greater-than the string without it, and adding an accent difference earlier
makes it sort greater-than adding it later. For example, “deanza” < “deanzä” <
“deänza” < “dëanza”. (Compare the ‘ä’/‘ë’ positions here with the ‘-’ positions
above.)

## Shift-Trimmed

*Note: This method is not currently implemented in ICU.*

Among strings that compare tertiary-equal:

*   Sorts variable characters sometimes less-than, sometimes greater-than
    regular characters.
*   Inserting a variable character anywhere makes a string sort *greater-than*
    the string without it. (The string without variable characters gets an empty
    quaternary level.)
*   Inserting a variable character *earlier* in a string makes it sort
    *less-than* inserting the variable character *later* in the string.

For example: “deluge” < “de-luge” < “delu-ge” < “deluge-”.

The Shift-Trimmed method works like Shifted, except that *trailing*
high-quaternary weights (from regular characters) are removed (trimmed).
Compared with Shifted, the Shift-Trimmed method sorts strings without variable
characters before ones with variable characters added, rather than producing the
equivalent of [Merging Sort
Keys](http://www.unicode.org/reports/tr10/#Merging_Sort_Keys).

Shift-Trimmed is more complicated to implement than all of the other options:
When comparing strings, a lookahead (or equivalent) is needed to determine
whether a non-variable character gets a zero quaternary weight (if no variables
follow) or a high quaternary weight (if at least one variable follows). When
building sort keys, trailing high/common quaternary weights are trimmed (backed
out) at the end of the quaternary level.

## Variable-After

*Note: This method is not currently implemented in ICU.*

Among strings that compare tertiary-equal:

*   Sorts all variable characters greater-than (after) regular characters.
*   Inserting a variable character anywhere makes a string sort *greater-than*
    the string without it. (Like Shift-Trimmed.)
*   Inserting a variable character *earlier* in a string makes it sort
    *greater-than* inserting the variable character *later* in the string. (Like
    accent differences.)

For example: “deluge” < “deluge-” < “delu-ge” < “de-luge”.

The implementation “shifts” the primary weight p of the collation element \[p,
s, t, q\] of each variable characters down three levels: \[0, 0, 0, p\]. Regular
characters with primary collation elements get a *low* quaternary weight,
*lower* than that of any variable character. This is consistent with collation
on secondary and tertiary levels but unlike [Merging Sort
Keys](http://www.unicode.org/reports/tr10/#Merging_Sort_Keys).

This method extends the [UCA well-formedness condition
2](http://www.unicode.org/reports/tr10/#WF2) to apply to quaternary weights.
(UCA versions before UCA 6.2 did not limit WF2 to secondary & tertiary weights,
which meant that several of the Variable Weighting options technically created
ill-formed quaternary weights.)
