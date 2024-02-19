---
layout: default
title: ICU String Tries
parent: Data Structures
grand_parent: Design Docs
---

<!--
© 2016 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# ICU String Tries

We have several implementations of string tries, mapping strings to boolean or
integer values: Currently for time zone name parsing and DBBI. Other areas might
also benefit from tries: Property names, character names, UnicodeSetStringSpan,
.dat package file TOC.

We should have a small number of common map-from-string trie implementations;
fairly compact, fairly efficient, easily serializable, and well-tested.

See the subpages for ideas.

For a UnicodeSetStringSpan, we would want to find each next match starting from
some point in the text, rather than passing each unit of text and finding out if
the units so far match.

Note: In terms of whole-string-lookup performance, the fastest data structure is
a hash map. Where whole-string-lookup is the only relevant operation, we could
consider implementing an easily serialized hash map.

See also [ICU Code Point Tries](../utrie.md).

Implementations:

* [BytesTrie](./bytestrie/)
* [UCharsTrie](./ucharstrie)