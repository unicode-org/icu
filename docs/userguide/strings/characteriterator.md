---
layout: default
title: CharacterIterator
nav_order: 3
parent: Chars and Strings
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# CharacterIterator Class

## Overview

CharacterIterator is the abstract base class that defines a protocol for
accessing characters in a text-storage object. This class has methods for
iterating forward and backward over Unicode characters to return either the
individual Unicode characters or their corresponding index values.

Using CharacterIterator ICU iterates over text that is independent of its
storage method. The text can be stored locally or remotely in a string, file,
database, or other method. The CharacterIterator methods make the text appear as
if it is local.

The CharacterIterator keeps track of its current position and index in the text
and can do the following

1.  Move forward or backward one Unicode character at a time

2.  Jump to a new location using absolute or relative positioning

3.  Move to the beginning or end of its range

4.  Return a character or the index to a character

The information can be restricted to a sub-range of characters, can contain a
large block of text that can be iterated as a whole, or can be broken into
smaller blocks for the purpose of iteration.

> :point_right: **Note**: *CharacterIterator is different from
[Normalizer](../transforms/normalization/index) in that CharacterIterator
walks through the Unicode characters without interpretation.*

Prior to ICU release 1.6, the CharacterIterator class allowed access to a single
UChar at a time and did not support variable-width encoding. Single UChar
support makes it difficult when supplementary support is expected in UTF16
encodings. Beginning with ICU release 1.6, the CharacterIterator class now
efficiently supports UTF-16 encodings and provides new APIs for UTF32 return
values. The API names for the UTF16 and UTF32 encodings differ because the UTF32
APIs include "32" within their naming structure. For example,
CharacterIterator::current() returns the code unit and Character::current32()
returns a code point.

## Base class inherited by CharacterIterator

The class,
[ForwardCharacterIterator,](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/classForwardCharacterIterator.html)
is a superclass of the CharacterIterator class. This superclass provides methods
for forward iteration only for both UTF16 and UTF32 access, and is and based on
a efficient forward iteration mechanism. In some situations, where you need to
iterate over text that does not allow random-access, the
ForwardCharacterIterator superclass is the most efficient method. For example,
iterate a UChar string using a character converter with the [ucnv_getNextUChar()
function.](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/ucnv_8h.html)

## Subclasses of CharacterIterator provided by ICU

ICU provides the following concrete subclasses of the CharacterIteratorclass:

1.  [UCharCharacterIterator](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/classUCharCharacterIterator.html)
    subclass iterates over a `UChar[]` array.

2.  [StringCharacterIterator](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/classStringCharacterIterator.html)
    subclass extends from `UCharCharacterIterator` and iterates over the contents
    of a `UnicodeString`.

## Usage

To use the methods specified in CharacterIterator class, do one of the
following:

1.  Make a subclass that inherits from the CharacterIterator class

2.  Use the StringCharacterIterator subclass

3.  Use the UCharCharacterIterator subclass

CharacterIterator objects keep track of its current position within the text
that is iterated over. The CharacterIterator class uses an object similar to a
cursor that gets initialized to the beginning of the text and advances according
to the operations that are used on the object. The current index can move
between two positions (a start and a limit) that are set with the text. The
limit position is one character greater than the position of the last UChar
character that is used.

### Forward iteration

For efficiency, ICU can iterate over text using post-increment semantics or
Forward Iteration. Forward Iteration is an access method that reads a character
from the current index position and moves the index forward. It leaves the index
behind the character it read and returns the character read. ICU can use
nextPostInc() or next32PostInc() calls with hasNext() to perform Forward
Iteration. These calls are the only character access methods provided by the
ForwardCharacterIterator. An iteration loop can be started with the
setToStart(), firstPostInc() or first32PostInc()calls . (The setToStart() call
is implied after instantiating the iterator or setting the text.)

The less efficient forward iteration mechanism that is available for
compatibility with Java™ provides pre-increment semantics. With these methods,
the current character is skipped, and then the following character is read and
returned. This is a less efficient method for a variable-width encoding because
the width of each character is determined twice; once to read it and once to
skip it the next time ICU calls the method. The methods used for Forward
Iteration are the next() or next32() calls. An iteration loop must start with
first() or first32() calls to get the first character.

### Backward iteration

Backward Iteration has pre-decrement semantics, which are the exact opposite of
the post-increment Forward Iteration. The current index reads the character that
precedes the index, the character is returned, and the index is left at the
beginning of this character. The methods used for Backward Iteration are the
previous() or previous32() calls with the hasPrevious() call . An iteration loop
can be started with setToEnd(), last(), or last32() calls.

### Direct index manipulation

The index can be set and moved directly without iteration to start iterating at
an arbitrary position, skip some characters, or reset the index to an earlier
position. It is possible to set the index to one after the last text code unit
for backward iteration.

The setIndex() and setIndex32() calls set the index to a new position and return
the character at that new position. The setIndex32() call ensures that the new
position is at the beginning of the character (on its first code unit). Since
the character at the new position is returned, these functions can be used for
both pre-increment and post-increment iteration semantics.
Similarly, the current() and current32() calls return the character at the
current index without modifying the index. The current32() call retrieves the
complete character whether the index is on the first code unit or not.

The index and the iteration boundaries can be retrieved using separate
functions. The following syntax is used by ICU: startIndex() <= getIndex() <=
endIndex().

Without accessing the text, the setToStart() and setToEnd() calls set the index
to the start or to the end of the text. Therefore, these calls are efficient in
starting a forward (post-increment) or backward iteration.

The most general functions for manipulating the index position are the move()
and move32() calls. These calls allow you to move the index forward or backward
relative to its current position, start the index, or move to the end of the
index. The move() and move32() calls do not access the text and are best used
for skipping part of it. The move32() call skips complete code points like
next32PostInc() call and other UChar32-access methods.

### Access to the iteration text

The CharacterIterator class provides the following access methods for the entire
text under iteration:

1.  getText() sets a UnicodeString with the text

2.  getLength() returns just the length of the text.

This text (and the length) may include more than the actual iteration area
because the start and end indexes may not be the start and end of the entire
text. The text and the iteration range are set in the implementing subclasses.

## Additional Sample Code

C/C++: See
[icu4c/source/samples/citer/](https://github.com/unicode-org/icu/blob/master/icu4c/source/samples/citer/)
in the ICU source distribution for code samples.
