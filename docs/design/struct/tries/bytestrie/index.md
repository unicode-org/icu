---
layout: default
title: BytesTrie
parent: Data Structures
grand_parent: Design Docs
---

<!--
© 2016 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# BytesTrie

This is an idea for a trie that is intended to be fairly simple but also fairly
efficient and versatile. It maps from arbitrary byte sequences to 32-bit
integers. (Small non-negative integers are stored more efficiently. Negative
integers are the least efficient.)

Input strings would be mapped to byte sequences. Invariant-character strings
could be used directly, if the trie was built for the appropriate charset
family, or we could map EBCDIC input to ASCII (while lowercasing for
case-insensitive matching).

For Thai DBBI, each of U+0E00..U+0EFF could be mapped to its low byte.

For CJK DBBI, we could use UTF-16BE or a slight variant of it. For general
Unicode strings (e.g., time zone names), we could devise a simple encoding that
maps printable ASCII to single bytes, Unihan & Hangul and some other ranges to
two bytes per character, and the rest to three bytes per character. (We could
also use this for CJK DBBI, to reduce the number of such "converters".) Or, we
use a [UCharsTrie](../ucharstrie.md) for those.

Sample code is linked below.

See the [UCharsTrie](../ucharstrie.md) sibling page for some more details. The
BytesTrie and UCharsTrie structures are nearly the same, except that the
UCharsTrie uses fewer, larger units.

See also the [diagram of a BytesTrie for a sample set of string-to-value
mappings](https://docs.google.com/drawings/edit?id=1-doZNpcByYItcDAcvKmIpwJMWFgXpYCm43GnUrbat3g).

## Design points

*   The BytesTrie and UCharsTrie are designed to be
    byte-serialized/UChar-serialized, for trivial platform swapping.
*   Compact: Small values and jump deltas should be encoded in few bytes. This
    requires variable-length encodings.
*   The length of each value/delta is encoded either in a preceding node or in
    its own lead unit. This makes skipping values efficient, and fewer units
    need to be range-checked while reading variable-length values.
*   Nodes with small values are encoded in single units.
*   Linear-match nodes match a sequence of units without choice/selection.
*   Branches
    *   Branches store relative deltas to "jump" to following nodes. Small
        deltas are encoded in single units; encoding deltas is much more
        efficient than encoding absolute offsets.
    *   Variable-width values make binary search on branch nodes infeasible.
        Therefore, branches with lists of (key, value) pairs are limited to
        short list lengths for linear search.
    *   For large branches, branch nodes contain one unit, for branching to the
        left (less-than) or to the right (greater-or-equal). This encodes a
        binary search into the data structure.
        *   Initially, I had an equals edge in split-branch sub-nodes as well,
            but that slowed down matching significantly (9% in one case) without
            noticeably helping with the serialized size (0.2% in that case).
    *   At the end of each node (except for a final-value node), matching
        continues with the next node, rather than using another jump to a
        different location.
    *   Each branch head node encodes the length of the branch (the number of
        units to select from). The split-branch and list-branch sub-nodes do not
        have node heads. Instead, the code tracks the remaining length of the
        branch, halving it for each split-branch edge and counting down in a
        list-branch sub-node.
    *   The maximum length of a list-branch sub-node is fixed, that is, part of
        the serialized data format and cannot be changed compatibly. This
        constant is used in the branching code to decide whether to split
        less-than/greater-or-equal vs. walk a list of key-value pairs.
    *   This constant must be at least 3 so that split-branch sub-nodes have a
        length of at least 4 so that the following list-branch nodes have a
        length of at least 2 and can use a do-while loop rather than a while
        loop. (Saving one length check.)
    *   I explored an alternative, with only split-branch nodes down to length 1
        and then a final match unit with continuing matching after that. It was
        fast but also significantly larger. A branch like this is about twice
        the size of a key-value pair list. If the average list-branch length is
        n, a branch has (length/n)-1 split-branch sub-nodes. This experiment
        corresponds to n=1.
*   API
    *   The API is simple and low-level. At the core, next(unit) "turns the
        crank" and returns basically a 2-bit result that encodes matches() (this
        unit continues a matching sequence), hasNext() (another unit can
        continue a matching sequence) and hasValue() (the units so far are a
        matching string).
    *   Higher-level functions that handle different input (e.g., normalize
        units on the fly) and provide variations of functionality (e.g., longest
        match, startsWith, find all matches from some point in text, ...) can be
        built on top of the low-level functions without cluttering the API or
        pulling in further dependencies.
    *   The next(unit) function stops on a value node rather than decoding the
        value, saving time until the value is requested (via getValue()). The
        following next(unit2) call will then skip over the value node.
    *   There is enough API to serve a variety of uses, including
        matching/mapping whole strings, finding out if a prefix belongs only to
        strings with the same value, getting all units that can continue from
        some point, and getting all (string, value) pairs. This should be able
        to support lookups, parsing with abbreviations, word segmentation, etc.
*   The "fast" builder code is simple. The builder builds, it need not use a
    trie structure until writing the serialized form, and it need not provide
    any of the trie runtime API.
*   There is builder code that makes a "small" trie, attempting to avoid writing
    duplicate nodes. This is possible when whole trees of nodes are the same and
    at least one is reached via a "jump" delta which can "jump" to the
    previously written serialization of such a tree.

## Sample Code

The following demo code was last updaed Nov. 2010:

* [`bytetrie.h`](./bytetrie.h)
* [`bytetriebuilder.h`](./bytetriebuilder.h)
* [`bytetriedemo.cpp`](./bytetriedemo.cpp)
* [`bytetrieiterator.h`](./bytetrieiterator.h)
* [`denseranges.h`](./denseranges.h)
* [`genpname.cpp`](./genpname.cpp)

### Latest versions of source code

The latest versions of the above sample code (except for `bytetriedemo.cpp`) exist in the ICU repository, sometimes under slightly different names and reorganized:

* [icu4c/source/common/unicode/**bytestrie.h**](https://github.com/unicode-org/icu/blob/main/icu4c/source/common/unicode/bytestrie.h)
* [icu4c/source/common/unicode/**bytestriebuilder.h**](https://github.com/unicode-org/icu/blob/main/icu4c/source/common/unicode/bytestriebuilder.h)
* [icu4c/source/tools/toolutil/**denseranges.h**](https://github.com/unicode-org/icu/blob/main/icu4c/source/tools/toolutil/denseranges.h)
* [tools/unicode/c/genprops/**pnamesbuilder.cpp**](https://github.com/unicode-org/icu/blob/main/tools/unicode/c/genprops/pnamesbuilder.cpp)

