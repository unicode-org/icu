---
layout: default
title: UCharsTrie
parent: Data Structures
grand_parent: Design Docs
---

<!--
© 2016 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# UCharsTrie

Same design as a [BytesTrie](bytestrie/index.md), but mapping any UnicodeString
(any sequence of 16-bit units) to 32-bit integer values. This can use somewhat
simpler code because there are more bits to work with in each unit, and it is
probably more appropriate and faster than a BytesTrie for collation
contractions/prefixes, CJK dictionaries, and maybe for use with Unicode strings
in general when it is not known that we work with a small script or mostly with
ASCII.

The code and data structure are quite similar to the BytesTrie. In general,
larger units are used to store larger values and deltas in single units than
possible in a BytesTrie, and fewer variable-length units are needed in all
cases.

In addition, some of the bits of match-nodes (linear-match and branch nodes) are
used for intermediate values (small values or most significant bits), rather
than separate intermediate-value nodes in a BytesTrie. Larger intermediate
values have one or two units following the match node head, then followed by the
match node's contents.
