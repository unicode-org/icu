/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */


/*
 * @(#)RuleBasedBreakIterator.java    1.3 99/04/07
 *
 */

package com.ibm.icu.text;

import com.ibm.icu.util.CompactByteArray;
import com.ibm.icu.impl.Utility;
import java.util.Vector;
import java.util.Stack;
import java.util.Hashtable;
import java.util.Enumeration;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

import java.io.*;

/**
 * <p>A subclass of BreakIterator whose behavior is specified using a list of rules.</p>
 *
 * <p>There are two kinds of rules, which are separated by semicolons: <i>substitutions</i>
 * and <i>regular expressions.</i></p>
 *
 * <p>A substitution rule defines a name that can be used in place of an expression. It
 * consists of a name, an equals sign, and an expression. (There can be no whitespace on
 * either side of the equals sign.)  To keep its syntactic meaning intact, the expression
 * must be enclosed in parentheses or square brackets. A substitution is visible after its
 * definition, and is filled in using simple textual substitution (when a substitution is
 * used, its name is enclosed in curly braces.  The curly braces are optional in the
 * substition's definition). Substitution definitions can contain other substitutions, as
 * long as those substitutions have been defined first. Substitutions are generally used to
 * make the regular expressions (which can get quite complex) shorter and easier to read.
 * They typically define either character categories or commonly-used subexpressions.</p>
 *
 * <p>There is one special substitution.&nbsp; If the description defines a substitution
 * called &quot;_ignore_&quot;, the expression must be a [] expression, and the
 * expression defines a set of characters (the &quot;<em>ignore characters</em>&quot;) that
 * will be transparent to the BreakIterator.&nbsp; A sequence of characters will break the
 * same way it would if any ignore characters it contains are taken out.&nbsp; Break
 * positions never occur before ignore characters, except when the character before the
 * ignore characters is a line or paragraph terminator.</p>
 *
 * <p>A regular expression uses a syntax similar to the normal Unix regular-expression
 * syntax, and defines a sequence of characters to be kept together. With one significant
 * exception, the iterator uses a longest-possible-match algorithm when matching text to regular
 * expressions. The iterator also treats descriptions containing multiple regular expressions
 * as if they were ORed together (i.e., as if they were separated by |).</p>
 *
 * <p>The special characters recognized by the regular-expression parser are as follows:</p>
 *
 * <blockquote>
 *   <table border="1" width="100%">
 *     <tr>
 *       <td width="6%">*</td>
 *       <td width="94%">Specifies that the expression preceding the asterisk may occur any number
 *       of times (including not at all).</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">+</td>
 *       <td width="94%">Specifies that the expression preceding the asterisk may occur one or
 *       more times, but must occur at least once.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">?</td>
 *       <td width="94%">Specifies that the expression preceding the asterisk may occur once
 *       or not at all (i.e., it makes the preceding expression optional).</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">()</td>
 *       <td width="94%">Encloses a sequence of characters.  If followed by * or +, the
 *       sequence repeats.  If followed by ?, the sequence is optional.  Otherwise, the
 *       parentheses are just a grouping device and a way to delimit the ends of expressions
 *       containing |.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">|</td>
 *       <td width="94%">Separates two alternative sequences of characters.&nbsp; Either one
 *       sequence or the other, but not both, matches this expression.&nbsp; The | character can
 *       only occur inside ().</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">.</td>
 *       <td width="94%">Matches any character.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">*?</td>
 *       <td width="94%">Specifies a non-greedy asterisk.&nbsp; *? works the same way as *, except
 *       when there is overlap between the last group of characters in the expression preceding the
 *       * and the first group of characters following the *.&nbsp; When there is this kind of
 *       overlap, * will match the longest sequence of characters that match the expression before
 *       the *, and *? will match the shortest sequence of characters matching the expression
 *       before the *?.&nbsp; For example, if you have &quot;xxyxyyyxyxyxxyxyxyy&quot; in the text,
 *       &quot;x[xy]*x&quot; will match through to the last x (i.e., &quot;<strong>xxyxyyyxyxyxxyxyx</strong>yy&quot;,
 *       but &quot;x[xy]*?x&quot; will only match the first two xes (&quot;<strong>xx</strong>yxyyyxyxyxxyxyxyy&quot;).</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">[]</td>
 *       <td width="94%">Specifies a group of alternative characters.&nbsp; A [] expression will
 *       match any single character that is specified in the [] expression.&nbsp; For more on the
 *       syntax of [] expressions, see below.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">/</td>
 *       <td width="94%">Specifies where the break position should go if text matches this
 *       expression.&nbsp; (e.g., &quot;[a-z]&#42;/[:Zs:]*[1-0]&quot; will match if the iterator sees a run
 *       of letters, followed by a run of whitespace, followed by a digit, but the break position
 *       will actually go before the whitespace).&nbsp; Expressions that don't contain / put the
 *       break position at the end of the matching text.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">\</td>
 *       <td width="94%">Escape character.&nbsp; The \ itself is ignored, but causes the next
 *       character to be treated as literal character.&nbsp; This has no effect for many
 *       characters, but for the characters listed above, this deprives them of their special
 *       meaning.&nbsp; (There are no special escape sequences for Unicode characters, or tabs and
 *       newlines; these are all handled by a higher-level protocol.&nbsp; In a Java string,
 *       &quot;\n&quot; will be converted to a literal newline character by the time the
 *       regular-expression parser sees it.&nbsp; Of course, this means that \ sequences that are
 *       visible to the regexp parser must be written as \\ when inside a Java string.)&nbsp; All
 *       characters in the ASCII range except for letters, digits, and control characters are
 *       reserved characters to the parser and must be preceded by \ even if they currently don't
 *       mean anything.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">!</td>
 *       <td width="94%">If ! appears at the beginning of a regular expression, it tells the regexp
 *       parser that this expression specifies the backwards-iteration behavior of the iterator,
 *       and not its normal iteration behavior.&nbsp; This is generally only used in situations
 *       where the automatically-generated backwards-iteration behavior doesn't produce
 *       satisfactory results and must be supplemented with extra client-specified rules.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%"><em>(all others)</em></td>
 *       <td width="94%">All other characters are treated as literal characters, which must match
 *       the corresponding character(s) in the text exactly.</td>
 *     </tr>
 *   </table>
 * </blockquote>
 *
 * <p>Within a [] expression, a number of other special characters can be used to specify
 * groups of characters:</p>
 *
 * <blockquote>
 *   <table border="1" width="100%">
 *     <tr>
 *       <td width="6%">-</td>
 *       <td width="94%">Specifies a range of matching characters.&nbsp; For example
 *       &quot;[a-p]&quot; matches all lowercase Latin letters from a to p (inclusive).&nbsp; The -
 *       sign specifies ranges of continuous Unicode numeric values, not ranges of characters in a
 *       language's alphabetical order: &quot;[a-z]&quot; doesn't include capital letters, nor does
 *       it include accented letters such as a-umlaut.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">^</td>
 *       <td width="94%">Inverts the expression.  All characters the expression includes are
 *       excluded, and vice versa.  (i.e., it has the effect of saying "all Unicode characters
 *       except...")  This character only has its special meaning when it's the first character
 *       in the [] expression.  (Generally, you only see the ^ character inside a nested []
 *       expression used in conjunction with the syntax below.)</td>
 *     </tr>
 *     <tr>
 *       <td width="6%"><em>(all others)</em></td>
 *       <td width="94%">All other characters are treated as literal characters.&nbsp; (For
 *       example, &quot;[aeiou]&quot; specifies just the letters a, e, i, o, and u.)</td>
 *     </tr>
 *   </table>
 * </blockquote>
 *
 * <p>[] expressions can nest.  There are some other characters that have special meaning only
 * when used in conjunction with nester [] expressions:</p>
 *
 * <blockquote>
 *   <table border="1" width="100%">
 *     <tr>
 *       <td width="6%">::</td>
 *       <td width="94%">Within a nested [] expression, a pair of colons containing a one- or
 *       two-letter code matches all characters in the corresponding Unicode category.&nbsp;
 *       The :: expression has to be the only thing inside the [] expression. The two-letter codes
 *       are the same as the two-letter codes in the Unicode database (for example,
 *       &quot;[[:Sc:][:Sm:]]&quot; matches all currency symbols and all math symbols).&nbsp;
 *       Specifying a one-letter code is the same as specifying all two-letter codes that begin
 *       with that letter (for example, &quot;[[:L:]]&quot; matches all letters, and is equivalent
 *       to &quot;[[:Lu:][:Ll:][:Lo:][:Lm:][:Lt:]]&quot;).&nbsp; Anything other than a valid
 *       two-letter Unicode category code or a single letter that begins a valide Unicode category
 *       code is illegal within the colons.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">|</td>
 *       <td width="94%">Two nested [] expressions juxtaposed or separated only by a | character
 *       are merged together into a single [] expression matching all the characters in either
 *       of the original [] expressions.  (e.g., "[[ab][bc]]" is equivalent to "[abc]", and so
 *       is "[[ab]|[bc]]". <b>NOTE:</b>  "[ab][bc]" is NOT the same thing as "[[ab][bc]]".
 *       The first expression will match two characters: an a or b followed by either another
 *       b or a c.  The second expression will match a single character, which may be a, b, or c.
 *       The nesting is <em>required</em> for the expressions to merge together.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">&</td>
 *       <td width="94%">Two nested [] expressions with only & between them will match any
 *       character that appears in both nested [] expressions (this is a set intersection).
 *       (e.g., "[[ab]&[bc]]" will only match the letter b.)</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">-</td>
 *       <td width="94%">Two nested [] expressions with - between them will match any
 *       character that appears in the first nested [] expression <em>but not</em> the
 *       second one (this is an asymmetrical set difference).  (e.g., "[[:Sc:]-[$]]"
 *       matches any currency symbol except the dollar sign.  "[[ab]-[bc]] will match
 *       only the letter a.  This has exactly the same effect as "[[ab]&[^bc]]".)</td>
 *     </tr>
 *
 * <p>For a more complete explanation, see <a
 * href="http://oss.software.ibm.com/icu/docs/papers/text_boundary_analysis_in_java/index.html">http://oss.software.ibm.com/icu/docs/papers/text_boundary_analysis_in_java/index.html</a>.
 * &nbsp; For examples, see the resource data (which is annotated).</p>
 *
 * @author Richard Gillam
 * @internal ICU 2.0
 */
public class RuleBasedBreakIterator_Old extends RuleBasedBreakIterator {

    /**
     * A token used as a character-category value to identify ignore characters
     * @stable ICU 2.0
     */
    protected static final byte IGNORE = -1;

    /**
     * Special variable used to define ignore characters
     * @stable ICU 2.0
     */
    private static final String IGNORE_VAR = "_ignore_";

    /**
     * The state number of the starting state
     */
    private static final short START_STATE = 1;

    /**
     * The state-transition value indicating "stop"
     */
    private static final short STOP_STATE = 0;

    /**
     * The textual description this iterator was created from
     */
    private String description;

    /**
     * A table that indexes from character values to character category numbers
     */
    private CompactByteArray charCategoryTable = null;

    /**
     * The table of state transitions used for forward iteration
     */
    private short[] stateTable = null;

    /**
     * The table of state transitions used to sync up the iterator with the
     * text in backwards and random-access iteration
     */
    private short[] backwardsStateTable = null;

    /**
     * A list of flags indicating which states in the state table are accepting
     * ("end") states
     */
    private boolean[] endStates = null;

    /**
     * A list of flags indicating which states in the state table are
     * lookahead states (states which turn lookahead on and off)
     */
    private boolean[] lookaheadStates = null;

    /**
     * The number of character categories (and, thus, the number of columns in
     * the state tables)
     */
    private int numCategories;

    /**
     * The character iterator through which this BreakIterator accesses the text
     */
    private CharacterIterator text = null;

    //=======================================================================
    // constructors
    //=======================================================================

    /**
     * Constructs a RuleBasedBreakIterator_Old according to the description
     * provided.  If the description is malformed, throws an
     * IllegalArgumentException.  Normally, instead of constructing a
     * RuleBasedBreakIterator_Old directory, you'll use the factory methods
     * on BreakIterator to create one indirectly from a description
     * in the framework's resource files.  You'd use this when you want
     * special behavior not provided by the built-in iterators.
     * @stable ICU 2.0
     */
    public RuleBasedBreakIterator_Old(String description) {
//System.out.println(">>>RBBI constructor");
        this.description = description;

        // the actual work is done by the Builder class
        Builder builder = makeBuilder();
        builder.buildBreakIterator();
//System.out.println("<<<RBBI constructor");
    }

    /**
     * Creates a Builder.
     * @stable ICU 2.0
     */
    protected Builder makeBuilder() {
        return new Builder();
    }

    //=======================================================================
    // boilerplate
    //=======================================================================
    /**
     * Clones this iterator.
     * @return A newly-constructed RuleBasedBreakIterator_Old with the same
     * behavior as this one.
     * @stable ICU 2.0
     */
    public Object clone()
    {
        RuleBasedBreakIterator_Old result = (RuleBasedBreakIterator_Old) super.clone();
        if (text != null) {
            result.text = (CharacterIterator) text.clone();
        }
        return result;
    }

    /**
     * Returns true if both BreakIterators are of the same class, have the same
     * rules, and iterate over the same text.
     * @stable ICU 2.0
     */
    public boolean equals(Object that) {
        try {
            RuleBasedBreakIterator_Old other = (RuleBasedBreakIterator_Old) that;
            if (!description.equals(other.description)) {
                return false;
            }
            return getText().equals(other.getText());
        }
        catch(ClassCastException e) {
            return false;
        }
    }

    /**
     * Returns the description used to create this iterator
     * @stable ICU 2.0
     */
    public String toString() {
        return description;
    }

    /**
     * Compute a hashcode for this BreakIterator
     * @return A hash code
     * @stable ICU 2.0
     */
    public int hashCode()
    {
        return description.hashCode();
    }

/**
 *   Dump out a more-or-less human readable form of the
 *   complete state table and character class definitions
 *   @internal
 */
    ///CLOVER:OFF
public void debugDumpTables() {
    System.out.println("Character Classes:");
    int currentCharClass = 257;
    int startCurrentRange = 0;
    int initialStringLength = 0;

    StringBuffer[] charClassRanges = new StringBuffer[numCategories];
    for (int i=0; i<numCategories; i++) {
        charClassRanges[i] = new StringBuffer();
    }

    for (int i = 0; i < 0xffff; i++) {
        if ((int)charCategoryTable.elementAt((char)i) != currentCharClass) {
            if (currentCharClass != 257) {
                // Complete the output of the previous range.
                if (i != startCurrentRange+1) {
                    charClassRanges[currentCharClass].append("-"+ Integer.toHexString(i-1));
                }
                if (charClassRanges[currentCharClass].length() % 72 < initialStringLength % 72) {
                    charClassRanges[currentCharClass].append("\n     ");
                }
            }

            // Output the start of the new range.
            currentCharClass = (int)charCategoryTable.elementAt((char)i);
            startCurrentRange = i;
            initialStringLength = charClassRanges[currentCharClass].length();
            if (charClassRanges[currentCharClass].length() > 0)
                charClassRanges[currentCharClass].append(", ");
            charClassRanges[currentCharClass].append(Integer.toHexString(i));
        }
    }

    for (int i=0; i<numCategories; i++) {
        System.out.println(i + ":     " + charClassRanges[i]);
    }


    System.out.println("\n\nState Table.   *: end state     %: look ahead state");
    System.out.print("C:\t");
    for (int i = 0; i < numCategories; i++)
        System.out.print(Integer.toString(i) + "\t");
    System.out.println(); System.out.print("=================================================");
    for (int i = 0; i < stateTable.length; i++) {
        if (i % numCategories == 0) {
            System.out.println();
            if (endStates[i / numCategories])
                System.out.print("*");
            else
                System.out.print(" ");
            if (lookaheadStates[i / numCategories]) {
                System.out.print("%");
            }
            else
                System.out.print(" ");
            System.out.print(Integer.toString(i / numCategories) + ":\t");
        }
        if (stateTable[i] == 0) {
            System.out.print(".\t");
        } else {
            System.out.print(Integer.toString(stateTable[i]) + "\t");
        }
    }
    System.out.println();
}
    ///CLOVER:ON

    ///CLOVER:OFF
// DELETE ME BEFORE RELEASE!!!
    /**
     * Write the RBBI runtime engine state transition tables to a file.
     *  Formerly used to export the tables to the C++ RBBI Implementation.
     *  Now obsolete, as C++ builds its own tables.
     * @internal
     */
public void writeTablesToFile(FileOutputStream file, boolean littleEndian) throws IOException {
    // NOTE: The format being written here is designed to be compatible with
    // the ICU udata interfaces and may not be useful for much else
    DataOutputStream out = new DataOutputStream(file);
    
//    --- write the file header ---
    byte[] comment = "Copyright (C) 1999, International Business Machines Corp. and others. All Rights Reserved.".getBytes("US-ASCII");
//    write the size of the header (rounded up to the next 16-byte boundary)
    short headerSize = (short)(comment.length + 1 // length of comment
            + 24); // size of static header data
    short realHeaderSize = (short)(headerSize + ((headerSize % 16 == 0) ? 0 : 16 - (headerSize % 16)));
    writeSwappedShort(realHeaderSize, out, littleEndian);
//    write magic byte values
    out.write(0xda);
    out.write(0x27);
//    write size of core header data
    writeSwappedShort((short)20, out, littleEndian);
//    write reserved bytes
    writeSwappedShort((short)0, out, littleEndian);
    
//    write flag indicating whether we're big-endian
    if (littleEndian) {
        out.write(0);
    } else {
        out.write(1);
    }
    
//    write character set family code (0 means ASCII)
    out.write(0);
//    write size of UChar in this file
    out.write(2);
//    write reserved byte
    out.write(0);
//    write data format identifier (this is an array of bytes in ICU, so the value is NOT swapped!)
    out.writeInt(0x42524b53);   // ("BRKS")
//    write file format version number (NOT swapped!)
    out.writeInt(0);
//    write data version number (NOT swapped!)
    out.writeInt(0);
//    write copyright notice
    out.write(comment);
    out.write(0);
//    fill in padding bytes
    while (headerSize < realHeaderSize) {
        out.write(0);
        ++headerSize;
    }
    
//    --- write index to the file ---
//    write the number of columns in the state table
    writeSwappedInt(numCategories, out, littleEndian);
    int fileEnd = 36;
//    write the location in the file of the BreakIterator description string
    writeSwappedInt(fileEnd, out, littleEndian);
    fileEnd += (description.length() + 1) * 2;
    fileEnd += (fileEnd % 4 == 0) ? 0 : 4 - (fileEnd % 4);
//    write the location of the character category table's index
    writeSwappedInt(fileEnd, out, littleEndian);
    fileEnd += charCategoryTable.getIndexArray().length * 2;
    fileEnd += (fileEnd % 4 == 0) ? 0 : 4 - (fileEnd % 4);
//    write the location of the character category table's values array
    writeSwappedInt(fileEnd, out, littleEndian);
    fileEnd += charCategoryTable.getValueArray().length;
    fileEnd += (fileEnd % 4 == 0) ? 0 : 4 - (fileEnd % 4);
//    write the location of the forward state table
    writeSwappedInt(fileEnd, out, littleEndian);
    fileEnd += stateTable.length * 2;
    fileEnd += (fileEnd % 4 == 0) ? 0 : 4 - (fileEnd % 4);
//    write the location of the backward state table
    writeSwappedInt(fileEnd, out, littleEndian);
    fileEnd += backwardsStateTable.length * 2;
    fileEnd += (fileEnd % 4 == 0) ? 0 : 4 - (fileEnd % 4);
//    write the location of the endStates flags
    writeSwappedInt(fileEnd, out, littleEndian);
    fileEnd += endStates.length;
    fileEnd += (fileEnd % 4 == 0) ? 0 : 4 - (fileEnd % 4);
//    write the location of the lookaheadStates flags
    writeSwappedInt(fileEnd, out, littleEndian);
    fileEnd += lookaheadStates.length;
    fileEnd += (fileEnd % 4 == 0) ? 0 : 4 - (fileEnd % 4);
//    write the length of the file
    writeSwappedInt(fileEnd, out, littleEndian);
    
//    --- write the actual data ---
//    write description string
    for (int i = 0; i < description.length(); i++)
        writeSwappedShort((short)description.charAt(i), out, littleEndian);
    out.writeShort(0);
    if ((description.length() + 1) % 2 == 1)
        out.writeShort(0);
//    write character category table
    char[] temp1 = charCategoryTable.getIndexArray();
    for (int i = 0; i < temp1.length; i++)
        writeSwappedShort((short)temp1[i], out, littleEndian);
    if (temp1.length % 2 == 1)
        out.writeShort(0);
    byte[] temp2 = charCategoryTable.getValueArray();
    out.write(temp2);
    switch (temp2.length % 4) {
        case 1: out.write(0);
        case 2: out.write(0);
        case 3: out.write(0);
        default: break;
    }
//    write the state transition tables
    for (int i = 0; i < stateTable.length; i++)
        writeSwappedShort(stateTable[i], out, littleEndian);
    if (stateTable.length % 2 == 1)
        out.writeShort(0);
    for (int i = 0; i < backwardsStateTable.length; i++)
        writeSwappedShort(backwardsStateTable[i], out, littleEndian);
    if (backwardsStateTable.length % 2 == 1)
        out.writeShort(0);
//    write the flag arrays
    for (int i = 0; i < endStates.length; i++)
        out.writeBoolean(endStates[i]);
    switch (endStates.length % 4) {
        case 1: out.write(0);
        case 2: out.write(0);
        case 3: out.write(0);
        default: break;
    }
    for (int i = 0; i < lookaheadStates.length; i++)
        out.writeBoolean(lookaheadStates[i]);
    switch (lookaheadStates.length % 4) {
        case 1: out.write(0);
        case 2: out.write(0);
        case 3: out.write(0);
        default: break;
    }
}

/**
 * @internal
 */
protected void writeSwappedShort(short x, DataOutputStream out, boolean littleEndian)
throws IOException{
    if (littleEndian) {
        out.write((byte)(x & 0xff));
        out.write((byte)((x >> 8) & 0xff));
    }
    else {
        out.write((byte)((x >> 8) & 0xff));
        out.write((byte)(x & 0xff));
    }
}

/**
 * @internal
 */
protected void writeSwappedInt(int x, DataOutputStream out, boolean littleEndian)
throws IOException {
    if (littleEndian) {
        out.write((byte)(x & 0xff));
        out.write((byte)((x >> 8) & 0xff));
        out.write((byte)((x >> 16) & 0xff));
        out.write((byte)((x >> 24) & 0xff));
    }
    else {
        out.write((byte)((x >> 24) & 0xff));
        out.write((byte)((x >> 16) & 0xff));
        out.write((byte)((x >> 8) & 0xff));
        out.write((byte)(x & 0xff));
    }
}
    ///CLOVER:ON

    //=======================================================================
    // BreakIterator overrides
    //=======================================================================

    /**
     * Sets the current iteration position to the beginning of the text.
     * (i.e., the CharacterIterator's starting offset).
     * @return The offset of the beginning of the text.
     * @stable ICU 2.0
     */
    public int first() {
        CharacterIterator t = getText();

        t.first();
        return t.getIndex();
    }

    /**
     * Sets the current iteration position to the end of the text.
     * (i.e., the CharacterIterator's ending offset).
     * @return The text's past-the-end offset.
     * @stable ICU 2.0
     */
    public int last() {
        CharacterIterator t = getText();

        // I'm not sure why, but t.last() returns the offset of the last character,
        // rather than the past-the-end offset
        t.setIndex(t.getEndIndex());
        return t.getIndex();
    }

    /**
     * Advances the iterator either forward or backward the specified number of steps.
     * Negative values move backward, and positive values move forward.  This is
     * equivalent to repeatedly calling next() or previous().
     * @param n The number of steps to move.  The sign indicates the direction
     * (negative is backwards, and positive is forwards).
     * @return The character offset of the boundary position n boundaries away from
     * the current one.
     * @stable ICU 2.0
     */
    public int next(int n) {
        int result = current();
        while (n > 0) {
            result = handleNext();
            --n;
        }
        while (n < 0) {
            result = previous();
            ++n;
        }
        return result;
    }

    /**
     * Advances the iterator to the next boundary position.
     * @return The position of the first boundary after this one.
     * @stable ICU 2.0
     */
    public int next() {
        return handleNext();
    }

    /**
     * Advances the iterator backwards, to the last boundary preceding this one.
     * @return The position of the last boundary position preceding this one.
     * @stable ICU 2.0
     */
    public int previous() {
        // if we're already sitting at the beginning of the text, return DONE
        CharacterIterator text = getText();
        if (current() == text.getBeginIndex()) {
            return BreakIterator.DONE;
        }

        // set things up.  handlePrevious() will back us up to some valid
        // break position before the current position (we back our internal
        // iterator up one step to prevent handlePrevious() from returning
        // the current position), but not necessarily the last one before
        // where we started
        int start = current();
        text.previous();
        int lastResult = handlePrevious();
        int result = lastResult;

        // iterate forward from the known break position until we pass our
        // starting point.  The last break position before the starting
        // point is our return value
        while (result != BreakIterator.DONE && result < start) {
            lastResult = result;
            result = handleNext();
        }

        // set the current iteration position to be the last break position
        // before where we started, and then return that value
        text.setIndex(lastResult);
        return lastResult;
    }

    /**
     * Throw IllegalArgumentException unless begin <= offset < end.
     * @stable ICU 2.0
     */
    protected static final void checkOffset(int offset, CharacterIterator text) {
        if (offset < text.getBeginIndex() || offset > text.getEndIndex()) {
            throw new IllegalArgumentException("offset out of bounds");
        }
    }

    /**
     * Sets the iterator to refer to the first boundary position following
     * the specified position.
     * @param offset The position from which to begin searching for a break position.
     * @return The position of the first break after the current position.
     * @stable ICU 2.0
     */
    public int following(int offset) {
        // if the offset passed in is already past the end of the text,
        // just return DONE
        CharacterIterator text = getText();
        if (offset == text.getEndIndex()) {
            return BreakIterator.DONE;
        }
        checkOffset(offset, text);

        // otherwise, set our internal iteration position (temporarily)
        // to the position passed in.  If this is the _beginning_ position,
        // then we can just use next() to get our return value
        text.setIndex(offset);
        if (offset == text.getBeginIndex()) {
            return handleNext();
        }

        // otherwise, we have to sync up first.  Use handlePrevious() to back
        // us up to a known break position before the specified position (if
        // we can determine that the specified position is a break position,
        // we don't back up at all).  This may or may not be the last break
        // position at or before our starting position.  Advance forward
        // from here until we've passed the starting position.  The position
        // we stop on will be the first break position after the specified one.
        int result = handlePrevious();
        while (result != BreakIterator.DONE && result <= offset) {
            result = handleNext();
        }
        return result;
    }

    /**
     * Sets the iterator to refer to the last boundary position before the
     * specified position.
     * @param offset The position to begin searching for a break from.
     * @return The position of the last boundary before the starting position.
     * @stable ICU 2.0
     */
    public int preceding(int offset) {
        // if we start by updating the current iteration position to the
        // position specified by the caller, we can just use previous()
        // to carry out this operation
        CharacterIterator text = getText();
        checkOffset(offset, text);
        text.setIndex(offset);
        return previous();
    }

    /**
     * Returns true if the specfied position is a boundary position.  As a side
     * effect, leaves the iterator pointing to the first boundary position at
     * or after "offset".
     * @param offset the offset to check.
     * @return True if "offset" is a boundary position.
     * @stable ICU 2.0
     */
    public boolean isBoundary(int offset) {
        CharacterIterator text = getText();
        checkOffset(offset, text);
        if (offset == text.getBeginIndex()) {
            return true;
        }

        // to check whether this is a boundary, we can use following() on the
        // position before the specified one and return true if the position we
        // get back is the one the user specified
        else {
            return following(offset - 1) == offset;
        }
    }

    /**
     * Returns the current iteration position.
     * @return The current iteration position.
     * @stable ICU 2.0
     */
    public int current() {
        return getText().getIndex();
    }

    
    /**
     * Return the status tag from the break rule that determined the most recently
     * returned break position.  The values appear in the rule source
     * within brackets, {123}, for example.  For rules that do not specify a
     * status, a default value of 0 is returned.  If more than one rule applies,
     * the numerically largest of the possible status values is returned.
     * <p>
     * Note that for old style break iterators (implemented by this class), no
     * status can be declared, and a status of zero is always assumed.
     * <p>
     *
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public int  getRuleStatus() {
        return 0;
    }



    /**
     * Get the status (tag) values from the break rule(s) that determined the most 
     * recently returned break position.  The values appear in the rule source
     * within brackets, {123}, for example.  The default status value for rules
     * that do not explicitly provide one is zero.
     * <p>
     * Note that for old style break iterators (implemented by this class), no
     * status can be declared, and a status of zero is always assumed.
     * <p>
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
     public int getRuleStatusVec(int[] fillInArray) {
         if (fillInArray != null && fillInArray.length >= 1) {
             fillInArray[0] = 0;
         }
         return 1;
     }



    /**
     * Return a CharacterIterator over the text being analyzed.  This version
     * of this method returns the actual CharacterIterator we're using internally.
     * Changing the state of this iterator can have undefined consequences.  If
     * you need to change it, clone it first.
     * @return An iterator over the text being analyzed.
     * @stable ICU 2.0
     */
    public CharacterIterator getText() {
        // The iterator is initialized pointing to no text at all, so if this
        // function is called while we're in that state, we have to fudge an
        // an iterator to return.
        if (text == null) {
            text = new StringCharacterIterator("");
        }
        return text;
    }

    /**
     * Set the iterator to analyze a new piece of text.  This function resets
     * the current iteration position to the beginning of the text.
     * @param newText An iterator over the text to analyze.
     * @stable ICU 2.0
     */
    public void setText(CharacterIterator newText) {
        // Test text to see if we need to wrap it in a SafeCharIterator:
        int end = newText.getEndIndex();
        newText.setIndex(end);
        if (newText.getIndex() != end) {
            // failed - wrap in correct implementation
            text = new SafeCharIterator(newText);
        }
        else {
            text = newText;
        }
        text.first();
    }


    //=======================================================================
    // implementation
    //=======================================================================

    /**
     * This method is the actual implementation of the next() method.  All iteration
     * vectors through here.  This method initializes the state machine to state 1
     * and advances through the text character by character until we reach the end
     * of the text or the state machine transitions to state 0.  We update our return
     * value every time the state machine passes through a possible end state.
     * @stable ICU 2.0
     */
    protected int handleNext() {
        // if we're already at the end of the text, return DONE.
        CharacterIterator text = getText();
        if (text.getIndex() == text.getEndIndex()) {
            return BreakIterator.DONE;
        }

        // no matter what, we always advance at least one character forward
        int result = text.getIndex() + 1;
        int lookaheadResult = 0;

        // begin in state 1
        int state = START_STATE;
        int category;
        char c = text.current();
        char lastC = c;
        int lastCPos = 0;

        // if the first character in this segment is an ignore character (which can happen
        // when it's either the first character in the file or follows a mandatory break
        // character), and the first non-ignore character isn't a glue character, always
        // put a break between the ignore characters and the rest of the text
        if (lookupCategory(c) == IGNORE) {
            while (lookupCategory(c) == IGNORE)
                c = text.next();

            if (Character.getType(c) == Character.NON_SPACING_MARK || Character.getType(c)
                    == Character.ENCLOSING_MARK) {
                return text.getIndex();
            }
        }

        // loop until we reach the end of the text or transition to state 0
        while (c != CharacterIterator.DONE && state != STOP_STATE) {

            // look up the current character's character category (which tells us
            // which column in the state table to look at)
            category = lookupCategory(c);

            // if the character isn't an ignore character, look up a state
            // transition in the state table
            if (category != IGNORE) {
                state = lookupState(state, category);
            }

            // if the state we've just transitioned to is a lookahead state,
            // (but not also an end state), save its position.  If it's
            // both a lookahead state and an end state, update the break position
            // to the last saved lookup-state position
            if (lookaheadStates[state]) {
                if (endStates[state]) {
                    if (lookaheadResult > 0) {
                        result = lookaheadResult;
                    }
                    else {
                        result = text.getIndex() + 1;
                    }
                }
                else {
                    lookaheadResult = text.getIndex() + 1;
                }
            }

            // otherwise, if the state we've just transitioned to is an accepting
            // state, update the break position to be the current iteration position
            else {
                if (endStates[state]) {
                    result = text.getIndex() + 1;
                }
            }

            // keep track of the last "real" character we saw.  If this character isn't an
            // ignore character, take note of it and its position in the text
            if (category != IGNORE && state != STOP_STATE) {
                lastC = c;
                lastCPos = text.getIndex();
            }
            c = text.next();
        }

        // if we've run off the end of the text, and the very last character took us into
        // a lookahead state, advance the break position to the lookahead position
        // (the theory here is that if there are no characters at all after the lookahead
        // position, that always matches the lookahead criteria)
        if (c == CharacterIterator.DONE && lookaheadResult == text.getEndIndex()) {
            result = lookaheadResult;
        }

        // if the last character we saw before the one that took us into the stop state
        // was a mandatory breaking character, then the break position goes right after it
        // (this is here so that breaks come before, rather than after, a string of
        // ignore characters when they follow a mandatory break character)
        else if ("\n\r\f\u2028\u2029".indexOf(lastC) != -1) {
            result = lastCPos + 1;
        }

        text.setIndex(result);
        return result;
    }

    /**
     * This method backs the iterator back up to a "safe position" in the text.
     * This is a position that we know, without any context, must be a break position.
     * The various calling methods then iterate forward from this safe position to
     * the appropriate position to return.  (For more information, see the description
     * of buildBackwardsStateTable() in RuleBasedBreakIterator_Old.Builder.)
     * @stable ICU 2.0
     */
    protected int handlePrevious() {
        CharacterIterator text = getText();
        int state = START_STATE;
        int category = 0;
        int lastCategory = 0;
        char c = text.current();

        // loop until we reach the beginning of the text or transition to state 0
        while (c != CharacterIterator.DONE && state != STOP_STATE) {
//System.out.print(" " + text.getIndex());

            // save the last character's category and look up the current
            // character's category
            lastCategory = category;
            category = lookupCategory(c);

            // if the current character isn't an ignore character, look up a
            // state transition in the backwards state table
            if (category != IGNORE) {
                state = lookupBackwardState(state, category);
            }

            // then advance one character backwards
            c = text.previous();
        }

        // if we didn't march off the beginning of the text, we're either one or two
        // positions away from the real break position.  (One because of the call to
        // previous() at the end of the loop above, and another because the character
        // that takes us into the stop state will always be the character BEFORE
        // the break position.)
        if (c != CharacterIterator.DONE) {
            if (lastCategory != IGNORE) {
                text.setIndex(text.getIndex() + 2);
            }
            else {
                text.next();
            }
        }
//System.out.print(",");
        return text.getIndex();
    }

//static int visitedChars = 0;
    /**
     * Looks up a character's category (i.e., its category for breaking purposes,
     * not its Unicode category)
     * @internal
     */
    protected int lookupCategory(char c) {
//++visitedChars;
        return charCategoryTable.elementAt(c);
    }

/*
static void printVisitedCharCount() {
System.out.println("Total number of characters visited = " + visitedChars);
visitedChars = 0;
}
*/

    /**
     * Given a current state and a character category, looks up the
     * next state to transition to in the state table.
     * @internal
     */
    protected int lookupState(int state, int category) {
        return stateTable[state * numCategories + category];
    }

    /**
     * Given a current state and a character category, looks up the
     * next state to transition to in the backwards state table.
     * @internal
     */
    protected int lookupBackwardState(int state, int category) {
        return backwardsStateTable[state * numCategories + category];
    }

    /**
     * This is a helper function for computing the intersection of
     * two <code>UnicodeSet</code> objects.
     * @param a, b the two <code>UnicodeSet</code>s to intersect
     * @return a new <code>UnicodeSet</code> which is the intersection of a and b
     */
    private static UnicodeSet intersection(UnicodeSet a, UnicodeSet b)
    {
        UnicodeSet result = new UnicodeSet(a);

        result.retainAll(b);

        return result;
    }

    //=======================================================================
    // RuleBasedBreakIterator.Builder
    //=======================================================================
    /**
     * The Builder class has the job of constructing a RuleBasedBreakIterator_Old from a
     * textual description.  A Builder is constructed by RuleBasedBreakIterator_Old's
     * constructor, which uses it to construct the iterator itself and then throws it
     * away.
     * <p>The construction logic is separated out into its own class for two primary
     * reasons:
     * <ul><li>The construction logic is quite sophisticated and large.  Separating it
     * out into its own class means the code must only be loaded into memory while a
     * RuleBasedBreakIterator_Old is being constructed, and can be purged after that.
     * <li>There is a fair amount of state that must be maintained throughout the
     * construction process that is not needed by the iterator after construction.
     * Separating this state out into another class prevents all of the functions that
     * construct the iterator from having to have really long parameter lists,
     * (hopefully) contributing to readability and maintainability.</ul>
     * <p>It'd be really nice if this could be an independent class rather than an
     * inner class, because that would shorten the source file considerably, but
     * making Builder an inner class of RuleBasedBreakIterator_Old allows it direct access
     * to RuleBasedBreakIterator_Old's private members, which saves us from having to
     * provide some kind of "back door" to the Builder class that could then also be
     * used by other classes.
     * @internal
     */
    protected class Builder {
        /**
         * A temporary holding place used for calculating the character categories.
         * This object contains UnicodeSet objects.
         * @internal
         */
        protected Vector categories = null;

        /**
         * A table used to map parts of regexp text to lists of character categories,
         * rather than having to figure them out from scratch each time
         * @internal
         */
        protected Hashtable expressions = null;

        /**
         * A temporary holding place for the list of ignore characters
         * @internal
         */
        protected UnicodeSet ignoreChars = null;

        /**
         * A temporary holding place where the forward state table is built
         * @internal
         */
        protected Vector tempStateTable = null;

        /**
         * A list of all the states that have to be filled in with transitions to the
         * next state that is created.  Used when building the state table from the
         * regular expressions.
         * @internal
         */
        protected Vector decisionPointList = null;

        /**
         * A stack for holding decision point lists.  This is used to handle nested
         * parentheses and braces in regexps.
         * @internal
         */
        protected Stack decisionPointStack = null;

        /**
         * A list of states that loop back on themselves.  Used to handle .*?
         * @internal
         */
        protected Vector loopingStates = null;

        /**
         * Looping states actually have to be backfilled later in the process
         * than everything else.  This is where a the list of states to backfill
         * is accumulated.  This is also used to handle .*?
         * @internal
         */
        protected Vector statesToBackfill = null;

        /**
         * A list mapping pairs of state numbers for states that are to be combined
         * to the state number of the state representing their combination.  Used
         * in the process of making the state table deterministic to prevent
         * infinite recursion.
         * @internal
         */
        protected Vector mergeList = null;

        /**
         * A flag that is used to indicate when the list of looping states can
         * be reset.
         * @internal
         */
        protected boolean clearLoopingStates = false;

        /**
         * A bit mask used to indicate a bit in the table's flags column that marks a
         * state as an accepting state.
         * @internal
         */
        protected static final int END_STATE_FLAG = 0x8000;

        /**
         * A bit mask used to indicate a bit in the table's flags column that marks a
         * state as one the builder shouldn't loop to any looping states
         * @internal
         */
        protected static final int DONT_LOOP_FLAG = 0x4000;

        /**
         * A bit mask used to indicate a bit in the table's flags column that marks a
         * state as a lookahead state.
         * @internal
         */
        protected static final int LOOKAHEAD_STATE_FLAG = 0x2000;

        /**
         * A bit mask representing the union of the mask values listed above.
         * Used for clearing or masking off the flag bits.
         * @internal
         */
        protected static final int ALL_FLAGS = END_STATE_FLAG | LOOKAHEAD_STATE_FLAG
                | DONT_LOOP_FLAG;

        /**
         * No special construction is required for the Builder.
         * @internal
         */
        public Builder() {
        }

        /**
         * This is the main function for setting up the BreakIterator's tables.  It
         * just vectors different parts of the job off to other functions.
         * @internal
         */
        public void buildBreakIterator() {
            Vector tempRuleList = buildRuleList(description);
            buildCharCategories(tempRuleList);
            buildStateTable(tempRuleList);
            buildBackwardsStateTable(tempRuleList);
        }

        /**
         * Thus function has three main purposes:
         * <ul><li>Perform general syntax checking on the description, so the rest of the
         * build code can assume that it's parsing a legal description.
         * <li>Split the description into separate rules
         * <li>Perform variable-name substitutions (so that no one else sees variable names)
         * </ul>
         */
        private Vector buildRuleList(String description) {
            // invariants:
            // - parentheses must be balanced: ()[]{}<>
            // - nothing can be nested inside {}
            // - nothing can be nested inside [] except more []s
            // - pairs of ()[]{}<> must not be empty
            // - ; can only occur at the outer level
            // - | can only appear inside ()
            // - only one = or / can occur in a single rule
            // - = and / cannot both occur in the same rule
            // - the right-hand side of a = expression must be enclosed in [] or ()
            // - * may not occur at the beginning of a rule, nor may it follow
            //   =, /, (, (, |, }, ;, or *
            // - ? may only follow *
            // - the rule list must contain at least one / rule
            // - no rule may be empty
            // - all printing characters in the ASCII range except letters and digits
            //   are reserved and must be preceded by \
            // - ! may only occur at the beginning of a rule

            // set up a vector to contain the broken-up description (each entry in the
            // vector is a separate rule) and a stack for keeping track of opening
            // punctuation
            Vector tempRuleList = new Vector();
            Stack parenStack = new Stack();

            int p = 0;
            int ruleStart = 0;
            char c = '\u0000';
            char lastC = '\u0000';
            char lastOpen = '\u0000';
            boolean haveEquals = false;
            boolean havePipe = false;
            boolean sawVarName = false;
            boolean sawIllegalChar = false;
            int illegalCharPos = 0;
            final String charsThatCantPrecedeAsterisk = "=/<(|>*+?;\u0000";

            // if the description doesn't end with a semicolon, tack a semicolon onto the end
            if (description.length() != 0 && description.charAt(description.length() - 1) != ';') {
                description = description + ";";
            }

            // for each character, do...
            while (p < description.length()) {
                c = description.charAt(p);
                switch (c) {
                    // if the character is opening punctuation, verify that no nesting
                    // rules are broken, and push the character onto the stack
                    case '{':
                    case '[':
                    case '(':
                        if (lastOpen == '{') {
                            error("Can't nest brackets inside {}", p, description);
                        }
                        if (lastOpen == '[' && c != '[') {
                            error("Can't nest anything in [] but []", p, description);
                        }

                        // if we see { anywhere except on the left-hand side of =,
                        // we must be seeing a variable name that was never defined
                        if (c == '{' && (haveEquals || havePipe)) {
                            error("Unknown variable name", p, description);
                        }

                        lastOpen = c;
                        parenStack.push(new Character(c));
                        if (c == '{') {
                            sawVarName = true;
                        }
                        break;

                    // if the character is closing punctuation, verify that it matches the
                    // last opening punctuation we saw, and that the brackets contain
                    // something, then pop the stack
                    case '}':
                    case ']':
                    case ')':
                        char expectedClose = '\u0000';
                        switch (lastOpen) {
                            case '{':
                                expectedClose = '}';
                                break;
                            case '[':
                                expectedClose = ']';
                                break;
                            case '(':
                                expectedClose = ')';
                                break;
                        }
                        if (c != expectedClose) {
                            error("Unbalanced parentheses", p, description);
                        }
                        if (lastC == lastOpen) {
                            error("Parens don't contain anything", p, description);
                        }
                        parenStack.pop();
                        if (!parenStack.empty()) {
                            lastOpen = ((Character)(parenStack.peek())).charValue();
                        }
                        else {
                            lastOpen = '\u0000';
                        }

                        break;

                    // if the character is an asterisk, make sure it occurs in a place
                    // where an asterisk can legally go
                    case '*': case '+': case '?':
                        if (charsThatCantPrecedeAsterisk.indexOf(lastC) != -1
                                && (c != '?' || lastC != '*')) {
                            error("Misplaced *, +, or ?", p, description);
                        }
                        break;

                    // if the character is an equals sign, make sure we haven't seen another
                    // equals sign or a slash yet
                    case '=':
                        if (haveEquals || havePipe) {
                            error("More than one = or / in rule", p, description);
                        }
                        haveEquals = true;
                        sawIllegalChar = false;
                        break;

                    // if the character is a slash, make sure we haven't seen another slash
                    // or an equals sign yet
                    case '/':
                        if (haveEquals || havePipe) {
                            error("More than one = or / in rule", p, description);
                        }
                        if (sawVarName) {
                            error("Unknown variable name", p, description);
                        }
                        havePipe = true;
                        break;

                    // if the character is an exclamation point, make sure it occurs only
                    // at the beginning of a rule
                    case '!':
                        if (lastC != ';' && lastC != '\u0000') {
                            error("! can only occur at the beginning of a rule", p, description);
                        }
                        break;

                    // if the character is a backslash, skip the character that follows it
                    // (it'll get treated as a literal character)
                    case '\\':
                        ++p;
                        break;

                    // we don't have to do anything special on a period
                    case '.':
                        break;

                    // if the character is a syntax character that can only occur
                    // inside [], make sure that it does in fact only occur inside []
                    // (or in a variable name)
                    case '^':
                    case '-':
                    case ':':
                    case '&':
                        if (lastOpen != '[' && lastOpen != '{' && !sawIllegalChar) {
                            sawIllegalChar = true;
                            illegalCharPos = p;
                        }
                        break;

                    // if the character is a semicolon, do the following...
                    case ';':
                        // if we saw any illegal characters along the way, throw
                        // an error
                        if (sawIllegalChar) {
                            error("Illegal character", illegalCharPos, description);
                        }

                        // make sure the rule contains something and that there are no
                        // unbalanced parentheses or brackets
                        if (lastC == ';' || lastC == '\u0000') {
                            error("Empty rule", p, description);
                        }
                        if (!parenStack.empty()) {
                            error("Unbalanced parenheses", p, description);
                        }

                        if (parenStack.empty()) {
                            // if the rule contained an = sign, call processSubstitution()
                            // to replace the substitution name with the substitution text
                            // wherever it appears in the description
                            if (haveEquals) {
                                description = processSubstitution(description.substring(ruleStart,
                                                p), description, p + 1);
                            }
                            else {
                                // otherwise, check to make sure the rule doesn't reference
                                // any undefined substitutions
                                if (sawVarName) {
                                    error("Unknown variable name", p, description);
                                }

                                // then add it to tempRuleList
                                tempRuleList.addElement(description.substring(ruleStart, p));
                            }

                            // and reset everything to process the next rule
                            ruleStart = p + 1;
                            haveEquals = havePipe = sawVarName = sawIllegalChar = false;
                        }
                        break;

                    // if the character is a vertical bar, check to make sure that it
                    // occurs inside a () expression and that the character that precedes
                    // it isn't also a vertical bar
                    case '|':
                        if (lastC == '|') {
                            error("Empty alternative", p, description);
                        }
                        if (parenStack.empty() || lastOpen != '(') {
                            error("Misplaced |", p, description);
                        }
                        break;

                    // if the character is anything else (escaped characters are
                    // skipped and don't make it here), it's an error
                    default:
                        if (c >= ' ' && c < '\u007f' && !Character.isLetter(c)
                                && !Character.isDigit(c) && !sawIllegalChar) {
                            sawIllegalChar = true;
                            illegalCharPos = p;
                        }
                        break;
                }
                lastC = c;
                ++p;
            }
            if (tempRuleList.size() == 0) {
                error("No valid rules in description", p, description);
            }
            return tempRuleList;
        }

        /**
         * This function performs variable-name substitutions.  First it does syntax
         * checking on the variable-name definition.  If it's syntactically valid, it
         * then goes through the remainder of the description and does a simple
         * find-and-replace of the variable name with its text.  (The variable text
         * must be enclosed in either [] or () for this to work.)
         * @internal
         */
        protected String processSubstitution(String substitutionRule, String description,
                        int startPos) {
            // isolate out the text on either side of the equals sign
            String replace;
            String replaceWith;
            int equalPos = substitutionRule.indexOf('=');
            if (substitutionRule.charAt(0) != '$') {
                error("Missing '$' on left-hand side of =", startPos, description);
            }
            replace = substitutionRule.substring(1, equalPos);
            replaceWith = substitutionRule.substring(equalPos + 1);

            // check to see whether the substitution name is something we've declared
            // to be "special".  For RuleBasedBreakIterator itself, this is IGNORE_VAR.
            // This function takes care of any extra processing that has to be done
            // with "special" substitution names.
            handleSpecialSubstitution(replace, replaceWith, startPos, description);

            // perform various other syntax checks on the rule
            if (replaceWith.length() == 0) {
                error("Nothing on right-hand side of =", startPos, description);
            }
            if (replace.length() == 0) {
                error("Nothing on left-hand side of =", startPos, description);
            }
            if (!(replaceWith.charAt(0) == '[' && replaceWith.charAt(replaceWith.length() - 1)
                    == ']') && !(replaceWith.charAt(0) == '(' && replaceWith.charAt(
                    replaceWith.length() - 1) == ')')) {
                error("Illegal right-hand side for =", startPos, description);
            }

            // now go through the rest of the description (which hasn't been broken up
            // into separate rules yet) and replace every occurrence of the
            // substitution name with the substitution body
            replace = "$" + replace;
            StringBuffer result = new StringBuffer();
            result.append(description.substring(0, startPos));
            int lastPos = startPos;
            int pos = description.indexOf(replace, startPos);
            while (pos != -1) {
                // [liu] Check that the string we've found isn't a redefinition
                // of the variable.
                if (description.charAt(pos-1) == ';' &&
                    description.charAt(pos + replace.length()) == '=') {
                    error("Attempt to redefine " + replace, pos, description);
                }
                result.append(description.substring(lastPos, pos));
                result.append(replaceWith);
                lastPos = pos + replace.length();
                pos = description.indexOf(replace, lastPos);
            }
            result.append(description.substring(lastPos));
            return result.toString();
        }

        /**
         * This function defines a protocol for handling substitution names that
         * are "special," i.e., that have some property beyond just being
         * substitutions.  At the RuleBasedBreakIterator_Old level, we have one
         * special substitution name, IGNORE_VAR.  Subclasses can override this
         * function to add more.  Any special processing that has to go on beyond
         * that which is done by the normal substitution-processing code is done
         * here.
         * @internal
         */
        protected void handleSpecialSubstitution(String replace, String replaceWith,
                    int startPos, String description) {
            // if we get a definition for a substitution called IGNORE_VAR, it defines
            // the ignore characters for the iterator.  Check to make sure the expression
            // is a [] expression, and if it is, parse it and store the characters off
            // to the side.
            if (replace.equals(IGNORE_VAR)) {
                if (replaceWith.charAt(0) == '(') {
                    error("Ignore group can't be enclosed in (", startPos, description);
                }
                ignoreChars = new UnicodeSet(replaceWith, false);
            }
        }

        /**
         * This function builds the character category table.  On entry,
         * tempRuleList is a vector of break rules that has had variable names substituted.
         * On exit, the charCategoryTable data member has been initialized to hold the
         * character category table, and tempRuleList's rules have been munged to contain
         * character category numbers everywhere a literal character or a [] expression
         * originally occurred.
         * @internal
         */
        protected void buildCharCategories(Vector tempRuleList) {
            int bracketLevel = 0;
            int p = 0;
            int lineNum = 0;

            // build hash table of every literal character or [] expression in the rule list
            // and derive a UnicodeSet object representing the characters each refers to
            expressions = new Hashtable();
            while (lineNum < tempRuleList.size()) {
                String line = (String)(tempRuleList.elementAt(lineNum));
                p = 0;
                while (p < line.length()) {
                    char c = line.charAt(p);
                    switch (c) {
                        // skip over all syntax characters except [
                        case '(': case ')': case '*': case '.': case '/':
                        case '|': case ';': case '?': case '!': case '+':
                            break;

                        // for [, find the matching ] (taking nested [] pairs into account)
                        // and add the whole expression to the expression list
                        case '[':
                            int q = p + 1;
                            ++bracketLevel;
                            while (q < line.length() && bracketLevel != 0) {
                                c = line.charAt(q);
                                if (c == '[') {
                                    ++bracketLevel;
                                }
                                else if (c == ']') {
                                    --bracketLevel;
                                }
                                ++q;
                            }
                            if (expressions.get(line.substring(p, q)) == null) {
                                expressions.put(line.substring(p, q), new UnicodeSet(line.
                                                substring(p, q), false));
//Test.debugPrintln("1. Adding expression: " + line.substring(p, q));
                            }
                            p = q - 1;
                            break;

                        // for \ sequences, just move to the next character and treat
                        // it as a single character
                        case '\\':
                            ++p;
                            c = line.charAt(p);
                            // DON'T break; fall through into "default" clause

                        // for an isolated single character, add it to the expression list
                        default:
                            UnicodeSet s = new UnicodeSet();
                            s.add(line.charAt(p));
                            expressions.put(line.substring(p, p + 1), s);
//Test.debugPrintln("2. Adding expression: " + line.substring(p, p + 1));
                            break;
                    }
                    ++p;
                }
                ++lineNum;
            }

            // create the temporary category table (which is a vector of UnicodeSet objects)
            categories = new Vector();
            if (ignoreChars != null) {
                categories.addElement(ignoreChars);
            }
            else {
                categories.addElement(new UnicodeSet());
            }
            ignoreChars = null;

            // this is a hook to allow subclasses to add categories on their own
            mungeExpressionList(expressions);

            // Derive the character categories.  Go through the existing character categories
            // looking for overlap.  Any time there's overlap, we create a new character
            // category for the characters that overlapped and remove them from their original
            // category.  At the end, any characters that are left in the expression haven't
            // been mentioned in any category, so another new category is created for them.
            // For example, if the first expression is [abc], then a, b, and c will be placed
            // into a single character category.  If the next expression is [bcd], we will first
            // remove b and c from their existing category (leaving a behind), create a new
            // category for b and c, and then create another new category for d (which hadn't
            // been mentioned in the previous expression).
            // At no time should a character ever occur in more than one character category.

            // for each expression in the expressions list, do...
            Enumeration iter = expressions.elements();
            while (iter.hasMoreElements()) {
                // initialize the working char set to the chars in the current expression
                UnicodeSet work = new UnicodeSet((UnicodeSet)iter.nextElement());

                // for each category in the category list, do...
                for (int j = categories.size() - 1; !work.isEmpty() && j > 0; j--) {

                    // if there's overlap between the current working set of chars
                    // and the current category...
                    UnicodeSet cat = (UnicodeSet)(categories.elementAt(j));
                    UnicodeSet overlap = intersection(work, cat);

                    if (!overlap.isEmpty()) {
                        // if the current category is not a subset of the current
                        // working set of characters, then remove the overlapping
                        // characters from the current category and create a new
                        // category for them
                        if (!overlap.equals(cat)) {
                            cat.removeAll(overlap);
                            categories.addElement(overlap);
                        }

                        // and always remove the overlapping characters from the current
                        // working set of characters
                        work.removeAll(overlap);
                    }
                }

                // if there are still characters left in the working char set,
                // add a new category containing them
                if (!work.isEmpty()) {
                    categories.addElement(work);
                }
            }

            // we have the ignore characters stored in position 0.  Make an extra pass through
            // the character category list and remove anything from the ignore list that shows
            // up in some other category
            UnicodeSet allChars = new UnicodeSet();
            for (int i = 1; i < categories.size(); i++)
                allChars.addAll((UnicodeSet)(categories.elementAt(i)));
            UnicodeSet ignoreChars = (UnicodeSet)(categories.elementAt(0));
            ignoreChars.removeAll(allChars);

            // now that we've derived the character categories, go back through the expression
            // list and replace each UnicodeSet object with a String that represents the
            // character categories that expression refers to.  The String is encoded: each
            // character is a character category number (plus 0x100 to avoid confusing them
            // with syntax characters in the rule grammar)
            iter = expressions.keys();
            while (iter.hasMoreElements()) {
                String key = (String)iter.nextElement();
                UnicodeSet cs = (UnicodeSet)expressions.get(key);
                StringBuffer cats = new StringBuffer();

                // for each category...
                for (int j = 1; j < categories.size(); j++) {
                    UnicodeSet cat = new UnicodeSet((UnicodeSet) categories.elementAt(j));

                    // if the current expression contains characters in that category...
                    if (cs.containsAll(cat)) {

                        // then add the encoded category number to the String for this
                        // expression
                        cats.append((char)(0x100 + j));
                        if (cs.equals(cat)) {
                            break;
                        }
                    }
                }

                // once we've finished building the encoded String for this expression,
                // replace the UnicodeSet object with it
                expressions.put(key, cats.toString());
            }

            // and finally, we turn the temporary category table into a permanent category
            // table, which is a CompactByteArray. (we skip category 0, which by definition
            // refers to all characters not mentioned specifically in the rules)
            charCategoryTable = new CompactByteArray((byte)0);

            // for each category...
            for (int i = 0; i < categories.size(); i++) {
                UnicodeSet chars = (UnicodeSet)(categories.elementAt(i));
                int n = chars.getRangeCount();

                // go through the character ranges in the category one by one...
                for (int j = 0; j < n; ++j) {
                    int rangeStart = chars.getRangeStart(j);

                    // (ignore anything above the BMP for now...)
                    if (rangeStart >= 0x10000) {
                        break;
                    }

                    // and set the corresponding elements in the CompactArray accordingly
                    if (i != 0) {
                        charCategoryTable.setElementAt((char)rangeStart,
                            (char)chars.getRangeEnd(j), (byte)i);
                    }

                    // (category 0 is special-- it's the hiding place for the ignore
                    // characters, whose real category number in the CompactArray is
                    // -1 [this is because category 0 contains all characters not
                    // specifically mentioned anywhere in the rules] )
                    else {
                        charCategoryTable.setElementAt((char)rangeStart,
                            (char)chars.getRangeEnd(j), IGNORE);
                    }
                }
            }

            // once we've populated the CompactArray, compact it
            charCategoryTable.compact();

            // initialize numCategories
            numCategories = categories.size();
        }

        /**   @internal */
        protected void mungeExpressionList(Hashtable expressions) {
            // empty in the parent class.  This function provides a hook for subclasses
            // to mess with the character category table.
        }

        /**
         * This is the function that builds the forward state table.  Most of the real
         * work is done in parseRule(), which is called once for each rule in the
         * description.
         */
        private void buildStateTable(Vector tempRuleList) {
            // initialize our temporary state table, and fill it with two states:
            // state 0 is a dummy state that allows state 1 to be the starting state
            // and 0 to represent "stop".  State 1 is added here to seed things
            // before we start parsing
            tempStateTable = new Vector();
            tempStateTable.addElement(new short[numCategories + 1]);
            tempStateTable.addElement(new short[numCategories + 1]);

            // call parseRule() for every rule in the rule list (except those which
            // start with !, which are actually backwards-iteration rules)
            // variable not used int n = tempRuleList.size();
            for (int i = 0; i < tempRuleList.size(); i++) {
                String rule = (String)tempRuleList.elementAt(i);
                if (rule.charAt(0) != '!') {
                    parseRule(rule, true);
                }
            }

            // finally, use finishBuildingStateTable() to minimize the number of
            // states in the table and perform some other cleanup work
            finishBuildingStateTable(true);
/*
System.out.print("C:\t");
for (int i = 0; i < numCategories; i++)
System.out.print(Integer.toString(i) + "\t");
System.out.println(); System.out.print("=================================================");
for (int i = 0; i < stateTable.length; i++) {
if (i % numCategories == 0) {
System.out.println();
if (endStates[i / numCategories])
System.out.print("*");
else
System.out.print(" ");
if (lookaheadStates[i / numCategories]) {
System.out.print("%");
}
else
System.out.print(" ");
System.out.print(Integer.toString(i / numCategories) + ":\t");
}
if (stateTable[i] == 0) System.out.print(".\t"); else System.out.print(Integer.toString(stateTable[i]) + "\t");
}
System.out.println();
*/
    }

        /**
         * This is where most of the work really happens.  This routine parses a single
         * rule in the rule description, adding and modifying states in the state
         * table according to the new expression.  The state table is kept deterministic
         * throughout the whole operation, although some ugly postprocessing is needed
         * to handle the *? token.
         */
        private void parseRule(String rule, boolean forward) {
            // algorithm notes:
            //   - The basic idea here is to read successive character-category groups
            //   from the input string.  For each group, you create a state and point
            //   the appropriate entries in the previous state to it.  This produces a
            //   straight line from the start state to the end state.  The ?, +, *, and (|)
            //   idioms produce branches in this straight line.  These branches (states
            //   that can transition to more than one other state) are called "decision
            //   points."  A list of decision points is kept.  This contains a list of
            //   all states that can transition to the next state to be created.  For a
            //   straight line progression, the only thing in the decision-point list is
            //   the current state.  But if there's a branch, the decision-point list
            //   will contain all of the beginning points of the branch when the next
            //   state to be created represents the end point of the branch.  A stack is
            //   used to save decision point lists in the presence of nested parentheses
            //   and the like.  For example, when a { is encountered, the current decision
            //   point list is saved on the stack and restored when the corresponding }
            //   is encountered.  This way, after the } is read, the decision point list
            //   will contain both the state right before the } _and_ the state before
            //   the whole {} expression.  Both of these states can transition to the next
            //   state after the {} expression.
            //   - one complication arises when we have to stamp a transition value into
            //   an array cell that already contains one.  The updateStateTable() and
            //   mergeStates() functions handle this case.  Their basic approach is to
            //   create a new state that combines the two states that conflict and point
            //   at it when necessary.  This happens recursively, so if the merged states
            //   also conflict, they're resolved in the same way, and so on.  There are
            //   a number of tests aimed at preventing infinite recursion.
            //   - another complication arises with repeating characters.  It's somewhat
            //   ambiguous whether the user wants a greedy or non-greedy match in these cases.
            //   (e.g., whether "[a-z]*abc" means the SHORTEST sequence of letters ending in
            //   "abc" or the LONGEST sequence of letters ending in "abc".  We've adopted
            //   the *? to mean "shortest" and * by itself to mean "longest".  (You get the
            //   same result with both if there's no overlap between the repeating character
            //   group and the group immediately following it.)  Handling the *? token is
            //   rather complicated and involves keeping track of whether a state needs to
            //   be merged (as described above) or merely overwritten when you update one of
            //   its cells, and copying the contents of a state that loops with a *? token
            //   into some of the states that follow it after the rest of the table-building
            //   process is complete ("backfilling").
            // implementation notes:
            //   - This function assumes syntax checking has been performed on the input string
            //   prior to its being passed in here.  It assumes that parentheses are
            //   balanced, all literal characters are enclosed in [] and turned into category
            //   numbers, that there are no illegal characters or character sequences, and so
            //   on.  Violation of these invariants will lead to undefined behavior.
            //   - It'd probably be better to use linked lists rather than Vector and Stack
            //   to maintain the decision point list and stack.  I went for simplicity in
            //   this initial implementation.  If performance is critical enough, we can go
            //   back and fix this later.
            //   -There are a number of important limitations on the *? token.  It does not work
            //   right when followed by a repeating character sequence (e.g., ".*?(abc)*")
            //   (although it does work right when followed by a single repeating character).
            //   It will not always work right when nested in parentheses or braces (although
            //   sometimes it will).  It also will not work right if the group of repeating
            //   characters and the group of characters that follows overlap partially
            //   (e.g., "[a-g]*?[e-j]").  None of these capabilites was deemed necessary for
            //   describing breaking rules we know about, so we left them out for
            //   expeditiousness.
            //   - Rules such as "[a-z]*?abc;" will be treated the same as "[a-z]*?aa*bc;"--
            //   that is, if the string ends in "aaaabc", the break will go before the first
            //   "a" rather than the last one.  Both of these are limitations in the design
            //   of RuleBasedBreakIterator and not limitations of the rule parser.

            int p = 0;
            int currentState = 1;   // don't use state number 0; 0 means "stop"
            int lastState = currentState;
            String pendingChars = "";

            decisionPointStack = new Stack();
            decisionPointList = new Vector();
            loopingStates = new Vector();
            statesToBackfill = new Vector();

            short[] state;
            boolean sawEarlyBreak = false;

            // if we're adding rules to the backward state table, mark the initial state
            // as a looping state
            if (!forward) {
                loopingStates.addElement(new Integer(1));
            }

            // put the current state on the decision point list before we start
            decisionPointList.addElement(new Integer(currentState)); // we want currentState to
                                                                     // be 1 here...
            currentState = tempStateTable.size() - 1;   // but after that, we want it to be
                                                        // 1 less than the state number of the next state
            while (p < rule.length()) {
                char c = rule.charAt(p);
                clearLoopingStates = false;

                // this section handles literal characters, escaped characters (which are
                // effectively literal characters too), the . token, and [] expressions
                if (c == '['
                    || c == '\\'
                    || Character.isLetter(c)
                    || Character.isDigit(c)
                    || c < ' '
                    || c == '.'
                    || c >= '\u007f') {

                    // if we're not on a period, isolate the expression and look up
                    // the corresponding category list
                    if (c != '.') {
                        int q = p;

                        // if we're on a backslash, the expression is the character
                        // after the backslash
                        if (c == '\\') {
                            q = p + 2;
                            ++p;
                        }

                        // if we're on an opening bracket, scan to the closing bracket
                        // to isolate the expression
                        else if (c == '[') {
                            int bracketLevel = 1;
                            while (bracketLevel > 0) {
                                ++q;
                                c = rule.charAt(q);
                                if (c == '[') {
                                    ++bracketLevel;
                                }
                                else if (c == ']') {
                                    --bracketLevel;
                                }
                                else if (c == '\\') {
                                    ++q;
                                }
                            }
                            ++q;
                        }

                        // otherwise, the expression is just the character itself
                        else {
                            q = p + 1;
                        }

                        // look up the category list for the expression and store it
                        // in pendingChars
                        pendingChars = (String)expressions.get(rule.substring(p, q));

                        // advance the current position past the expression
                        p = q - 1;
                    }

                    // if the character we're on is a period, we end up down here
                    else {
                        int rowNum = ((Integer)decisionPointList.lastElement()).intValue();
                        state = (short[])tempStateTable.elementAt(rowNum);

                        // if the period is followed by an asterisk, then just set the current
                        // state to loop back on itself
                        if (p + 1 < rule.length() && rule.charAt(p + 1) == '*' && state[0] != 0) {
                            decisionPointList.addElement(new Integer(state[0]));
                            pendingChars = "";
                            ++p;
                            if (p + 1 < rule.length() && rule.charAt(p + 1) == '?') {
//System.out.println("Saw *?");
                                setLoopingStates(decisionPointList, decisionPointList);
                                ++p;
                            }
//System.out.println("Saw .*");
                        }

                        // otherwise, fabricate a category list ("pendingChars") with
                        // every category in it
                        else {
                            StringBuffer temp = new StringBuffer();
                            for (int i = 0; i < numCategories; i++)
                                temp.append((char)(i + 0x100));
                            pendingChars = temp.toString();
                        }
                    }

                    // we'll end up in here for all expressions except for .*, which is
                    // special-cased above
                    if (pendingChars.length() != 0) {

                        // if the expression is followed by an asterisk or a question mark,
                        //  then push a copy of the current decision point list onto the stack
                        if (p + 1 < rule.length() && (
                            rule.charAt(p + 1) == '*' ||
                            rule.charAt(p + 1) == '?'
                        )) {
                            decisionPointStack.push(decisionPointList.clone());
                        }

                        // create a new state, add it to the list of states to backfill
                        // if we have looping states to worry about, set its "don't make
                        // me an accepting state" flag if we've seen a slash, and add
                        // it to the end of the state table
                        int newState = tempStateTable.size();
                        if (loopingStates.size() != 0) {
                            statesToBackfill.addElement(new Integer(newState));
                        }
                        state = new short[numCategories + 1];
                        if (sawEarlyBreak) {
                            state[numCategories] = DONT_LOOP_FLAG;
                        }
                        tempStateTable.addElement(state);

                        // update everybody in the decision point list to point to
                        // the new state (this also performs all the reconciliation
                        // needed to make the table deterministic), then clear the
                        // decision point list
                        updateStateTable(decisionPointList, pendingChars, (short)newState);
                        decisionPointList.removeAllElements();

                        // add all states created since the last literal character we've
                        // seen to the decision point list
                        lastState = currentState;
                        do {
                            ++currentState;
                            decisionPointList.addElement(new Integer(currentState));
                        } while (currentState + 1 < tempStateTable.size());
                    }
                }

                // a * or a + denotes a repeating character or group, and a ? denotes an
                // optional character group. (*, + and ? after () are handled separately below.)
                if (c == '+' || c == '*' || c == '?') {
                    // when there's a * or a +, update the current state to loop back on itself
                    // on the character categories that caused us to enter this state
                    if (c == '*' || c == '+') {
                        // Note: we process one state at a time because updateStateTable
                        // may add new states, and we want to process them as well.
                        for (int i = lastState + 1; i < tempStateTable.size(); i++) {
                            Vector temp = new Vector();
                            temp.addElement(new Integer(i));
                            updateStateTable(temp, pendingChars, (short)(lastState + 1));
                        }

                        // If we just added any new states, add them to the decison point list
                        // Note: it might be a good idea to avoid adding new states to the
                        // decision point list in more than one place...
                        while (currentState + 1 < tempStateTable.size()) {
                            decisionPointList.addElement(new Integer(++currentState));
                        }
                    }

                    // for * and ? pop the top element off the decision point stack and merge
                    // it with the current decision point list (this causes the divergent
                    // paths through the state table to come together again on the next
                    // new state)
                    if (c == '*' || c == '?') {
                        Vector temp = (Vector)decisionPointStack.pop();
                        for (int i = 0; i < decisionPointList.size(); i++)
                            temp.addElement(decisionPointList.elementAt(i));
                        decisionPointList = temp;

                        // a ? after a * modifies the behavior of * in cases where there is overlap
                        // between the set of characters that repeat and the characters which follow.
                        // Without the ?, all states following the repeating state, up to a state which
                        // is reached by a character that doesn't overlap, will loop back into the
                        // repeating state.  With the ?, the mark states following the *? DON'T loop
                        // back into the repeating state.  Thus, "[a-z]*xyz" will match the longest
                        // sequence of letters that ends in "xyz," while "[a-z]*? will match the
                        // _shortest_ sequence of letters that ends in "xyz".
                        // We use extra bookkeeping to achieve this effect, since everything else works
                        // according to the "longest possible match" principle.  The basic principle
                        // is that transitions out of a looping state are written in over the looping
                        // value instead of being reconciled, and that we copy the contents of the
                        // looping state into empty cells of all non-terminal states that follow the
                        // looping state.
//System.out.println("c = " + c + ", p = " + p + ", rule.length() = " + rule.length());
                        if (c == '*' && p + 1 < rule.length() && rule.charAt(p + 1) == '?') {
//System.out.println("Saw *?");
                            setLoopingStates(decisionPointList, decisionPointList);
                            ++p;
                        }
                    }
                }

                // a ( marks the beginning of a sequence of characters.  Parentheses can either
                // contain several alternative character sequences (i.e., "(ab|cd|ef)"), or
                // they can contain a sequence of characters that can repeat (i.e., "(abc)*").  Thus,
                // A () group can have multiple entry and exit points.  To keep track of this,
                // we reserve TWO spots on the decision-point stack.  The top of the stack is
                // the list of exit points, which becomes the current decision point list when
                // the ) is reached.  The next entry down is the decision point list at the
                // beginning of the (), which becomes the current decision point list at every
                // entry point.
                // In addition to keeping track of the exit points and the active decision
                // points before the ( (i.e., the places from which the () can be entered),
                // we need to keep track of the entry points in case the expression loops
                // (i.e., is followed by *).  We do that by creating a dummy state in the
                // state table and adding it to the decision point list (BEFORE it's duplicated
                // on the stack).  Nobody points to this state, so it'll get optimized out
                // at the end.  It exists only to hold the entry points in case the ()
                // expression loops.
                if (c == '(') {

                    // add a new state to the state table to hold the entry points into
                    // the () expression
                    tempStateTable.addElement(new short[numCategories + 1]);

                    // we have to adjust lastState and currentState to account for the
                    // new dummy state
                    lastState = currentState;
                    ++currentState;

                    // add the current state to the decision point list (add it at the
                    // BEGINNING so we can find it later)
                    decisionPointList.insertElementAt(new Integer(currentState), 0);

                    // finally, push a copy of the current decision point list onto the
                    // stack (this keeps track of the active decision point list before
                    // the () expression), followed by an empty decision point list
                    // (this will hold the exit points)
                    decisionPointStack.push(decisionPointList.clone());
                    decisionPointStack.push(new Vector());
                }

                // a | separates alternative character sequences in a () expression.  When
                // a | is encountered, we add the current decision point list to the exit-point
                // list, and restore the decision point list to its state prior to the (.
                if (c == '|') {

                    // pick out the top two decision point lists on the stack
                    Vector oneDown = (Vector)decisionPointStack.pop();
                    Vector twoDown = (Vector)decisionPointStack.peek();
                    decisionPointStack.push(oneDown);

                    // append the current decision point list to the list below it
                    // on the stack (the list of exit points), and restore the
                    // current decision point list to its state before the () expression
                    for (int i = 0; i < decisionPointList.size(); i++)
                        oneDown.addElement(decisionPointList.elementAt(i));
                    decisionPointList = (Vector)twoDown.clone();
                }

                // a ) marks the end of a sequence of characters.  We do one of two things
                // depending on whether the sequence repeats (i.e., whether the ) is followed
                // by *):  If the sequence doesn't repeat, then the exit-point list is merged
                // with the current decision point list and the decision point list from before
                // the () is thrown away.  If the sequence does repeat, then we fish out the
                // state we were in before the ( and copy all of its forward transitions
                // (i.e., every transition added by the () expression) into every state in the
                // exit-point list and the current decision point list.  The current decision
                // point list is then merged with both the exit-point list AND the saved version
                // of the decision point list from before the ().  Then we throw out the *.
                if (c == ')') {

                    // pull the exit point list off the stack, merge it with the current
                    // decision point list, and make the merged version the current
                    // decision point list
                    Vector exitPoints = (Vector)decisionPointStack.pop();
                    for (int i = 0; i < decisionPointList.size(); i++)
                        exitPoints.addElement(decisionPointList.elementAt(i));
                    decisionPointList = exitPoints;

                    // if the ) isn't followed by a *, + or ?, then all we have to do is throw
                    // away the other list on the decision point stack, and we're done
                    if (p + 1 >= rule.length() || (
                            rule.charAt(p + 1) != '*' &&
                            rule.charAt(p + 1) != '+' &&
                            rule.charAt(p + 1) != '?')
                    ) {
                        decisionPointStack.pop();
                    }

                    // but if the sequence is conditional or it repeats,
                    // we have a lot more work to do...
                    else {

                        // now exitPoints and decisionPointList have to point to equivalent
                        // vectors, but not the SAME vector
                        exitPoints = (Vector)decisionPointList.clone();

                        // pop the original decision point list off the stack
                        Vector temp = (Vector)decisionPointStack.pop();

                        // we squirreled away the row number of our entry point list
                        // at the beginning of the original decision point list.  Fish
                        // that state number out and retrieve the entry point list
                        int tempStateNum = ((Integer)temp.firstElement()).intValue();
                        short[] tempState = (short[])tempStateTable.elementAt(tempStateNum);

                        // merge the original decision point list with the current
                        // decision point list
                        if (rule.charAt(p + 1) == '?' || rule.charAt(p + 1) == '*') {
                            for (int i = 0; i < decisionPointList.size(); i++)
                                temp.addElement(decisionPointList.elementAt(i));
                            decisionPointList = temp;
                        }

                        // finally, for * and + copy every forward reference from the entry point
                        // list into every state in the new decision point list
                        if (rule.charAt(p + 1) == '+' || rule.charAt(p + 1) == '*') {
                            for (int i = 0; i < tempState.length; i++) {
                                if (tempState[i] > tempStateNum) {
                                    updateStateTable(exitPoints,
                                                     new Character((char)(i + 0x100)).toString(),
                                                     tempState[i]);
                                }
                            }
                        }

                        // update lastState and currentState, and throw away the *, +, or ?
                        lastState = currentState;
                        currentState = tempStateTable.size() - 1;
                        ++p;
                    }
                }

                // a / marks the position where the break is to go if the character sequence
                // matches this rule.  We update the flag word of every state on the decision
                // point list to mark them as ending states, and take note of the fact that
                // we've seen the slash
                if (c == '/') {
                    sawEarlyBreak = true;
                    for (int i = 0; i < decisionPointList.size(); i++) {
                        state = (short[])tempStateTable.elementAt(((Integer)decisionPointList.
                                        elementAt(i)).intValue());
                        state[numCategories] |= LOOKAHEAD_STATE_FLAG;
                    }
                }

                // if we get here without executing any of the above clauses, we have a
                // syntax error.  However, for now we just ignore the offending character
                // and move on
/*
debugPrintln("====Parsed \"" + rule.substring(0, p + 1) + "\"...");
System.out.println("      currentState = " + currentState);
debugPrintVectorOfVectors("      decisionPointStack:", "        ", decisionPointStack);
debugPrintVector("        ", decisionPointList);
debugPrintVector("      loopingStates = ", loopingStates);
debugPrintVector("      statesToBackfill = ", statesToBackfill);
System.out.println("      sawEarlyBreak = " + sawEarlyBreak);
debugPrintTempStateTable();
*/

                // clearLoopingStates is a signal back from updateStateTable() that we've
                // transitioned to a state that won't loop back to the current looping
                // state.  (In other words, we've gotten to a point where we can no longer
                // go back into a *? we saw earlier.)  Clear out the list of looping states
                // and backfill any states that need to be backfilled.
                if (clearLoopingStates) {
                    setLoopingStates(null, decisionPointList);
                }

                // advance to the next character, now that we've processed the current
                // character
                ++p;
            }

            // this takes care of backfilling any states that still need to be backfilled
            setLoopingStates(null, decisionPointList);

            // when we reach the end of the string, we do a postprocessing step to mark the
            // end states.  The decision point list contains every state that can transition
            // to the end state-- that is, every state that is the last state in a sequence
            // that matches the rule.  All of these states are considered "mark states"
            // or "accepting states"-- that is, states that cause the position returned from
            // next() to be updated.  A mark state represents a possible break position.
            // This allows us to look ahead and remember how far the rule matched
            // before following the new branch (see next() for more information).
            // The temporary state table has an extra "flag column" at the end where this
            // information is stored.  We mark the end states by setting a flag in their
            // flag column.
            // Now if we saw the / in the rule, then everything after it is lookahead
            // material and the break really goes where the slash is.  In this case,
            // we mark these states as BOTH accepting states and lookahead states.  This
            // signals that these states cause the break position to be updated to the
            // position of the slash rather than the current break position.
            for (int i = 0; i < decisionPointList.size(); i++) {
                int rowNum = ((Integer)decisionPointList.elementAt(i)).intValue();
                state = (short[])tempStateTable.elementAt(rowNum);
                state[numCategories] |= END_STATE_FLAG;
                if (sawEarlyBreak) {
                    state[numCategories] |= LOOKAHEAD_STATE_FLAG;
                }
            }
/*
debugPrintln("====Parsed \"" + rule + ";");
System.out.println();
System.out.println("      currentState = " + currentState);
debugPrintVectorOfVectors("      decisionPointStack:", "        ", decisionPointStack);
debugPrintVector("        ", decisionPointList);
debugPrintVector("      loopingStates = ", loopingStates);
debugPrintVector("      statesToBackfill = ", statesToBackfill);
System.out.println("      sawEarlyBreak = " + sawEarlyBreak);
debugPrintTempStateTable();
*/
        }


        /**
         * Update entries in the state table, and merge states when necessary to keep
         * the table deterministic.
         * @param rows The list of rows that need updating (the decision point list)
         * @param pendingChars A character category list, encoded in a String.  This is the
         * list of the columns that need updating.
         * @param newValue Update the cells specfied above to contain this value
         */
        private void updateStateTable(Vector rows,
                                      String pendingChars,
                                      short newValue) {
            // create a dummy state that has the specified row number (newValue) in
            // the cells that need to be updated (those specified by pendingChars)
            // and 0 in the other cells
            short[] newValues = new short[numCategories + 1];
            for (int i = 0; i < pendingChars.length(); i++)
                newValues[(int)(pendingChars.charAt(i)) - 0x100] = newValue;

            // go through the list of rows to update, and update them by calling
            // mergeStates() to merge them the the dummy state we created
            for (int i = 0; i < rows.size(); i++) {
                mergeStates(((Integer)rows.elementAt(i)).intValue(), newValues, rows);
            }
        }

        /**
         * The real work of making the state table deterministic happens here.  This function
         * merges a state in the state table (specified by rowNum) with a state that is
         * passed in (newValues).  The basic process is to copy the nonzero cells in newStates
         * into the state in the state table (we'll call that oldValues).  If there's a
         * collision (i.e., if the same cell has a nonzero value in both states, and it's
         * not the SAME value), then we have to reconcile the collision.  We do this by
         * creating a new state, adding it to the end of the state table, and using this
         * function recursively to merge the original two states into a single, combined
         * state.  This process may happen recursively (i.e., each successive level may
         * involve collisions).  To prevent infinite recursion, we keep a log of merge
         * operations.  Any time we're merging two states we've merged before, we can just
         * supply the row number for the result of that merge operation rather than creating
         * a new state just like it.
         * @param rowNum The row number in the state table of the state to be updated
         * @param newValues The state to merge it with.
         * @param rowsBeingUpdated A copy of the list of rows passed to updateStateTable()
         * (itself a copy of the decision point list from parseRule()).  Newly-created
         * states get added to the decision point list if their "parents" were on it.
         */
        private void mergeStates(int rowNum,
                                 short[] newValues,
                                 Vector rowsBeingUpdated) {
            short[] oldValues = (short[])(tempStateTable.elementAt(rowNum));
/*
System.out.print("***Merging " + rowNum + ":");
for (int i = 0; i < oldValues.length; i++) System.out.print("\t" + oldValues[i]);
System.out.println();
System.out.print("    with   \t");
for (int i = 0; i < newValues.length; i++) System.out.print("\t" + newValues[i]);
System.out.println();
*/

            boolean isLoopingState = loopingStates.contains(new Integer(rowNum));

            // for each of the cells in the rows we're reconciling, do...
            for (int i = 0; i < oldValues.length; i++) {

                // if they contain the same value, we don't have to do anything
                if (oldValues[i] == newValues[i]) {
                    continue;
                }

                // if oldValues is a looping state and the state the current cell points to
                // is too, then we can just stomp over the current value of that cell (and
                // set the clear-looping-states flag if necessary)
                else if (isLoopingState && loopingStates.contains(new Integer(oldValues[i]))) {
                    if (newValues[i] != 0) {
                        if (oldValues[i] == 0) {
                            clearLoopingStates = true;
                        }
                        oldValues[i] = newValues[i];
                    }
                }

                // if the current cell in oldValues is 0, copy in the corresponding value
                // from newValues
                else if (oldValues[i] == 0) {
                    oldValues[i] = newValues[i];
                }

                // the last column of each row is the flag column.  Take care to merge the
                // flag words correctly
                else if (i == numCategories) {
                    oldValues[i] = (short)((newValues[i] & ALL_FLAGS) | oldValues[i]);
                }

                // if both newValues and oldValues have a nonzero value in the current
                // cell, and it isn't the same value both places...
                else if (oldValues[i] != 0 && newValues[i] != 0) {

                    // look up this pair of cell values in the merge list.  If it's
                    // found, update the cell in oldValues to point to the merged state
                    int combinedRowNum = searchMergeList(oldValues[i], newValues[i]);
                    if (combinedRowNum != 0) {
                        oldValues[i] = (short)combinedRowNum;
                    }

                    // otherwise, we have to reconcile them...
                    else {
                        // copy our row numbers into variables to make things easier
                        int oldRowNum = oldValues[i];
                        int newRowNum = newValues[i];
                        combinedRowNum = tempStateTable.size();

                        // add this pair of row numbers to the merge list (create it first
                        // if we haven't created the merge list yet)
                        if (mergeList == null) {
                            mergeList = new Vector();
                        }
                        mergeList.addElement(new int[] { oldRowNum, newRowNum, combinedRowNum });

//System.out.println("***At " + rowNum + ", merging " + oldRowNum + " and " + newRowNum + " into " + combinedRowNum);

                        // create a new row to represent the merged state, and copy the
                        // contents of oldRow into it, then add it to the end of the
                        // state table and update the original row (oldValues) to point
                        // to the new, merged, state
                        short[] newRow = new short[numCategories + 1];
                        short[] oldRow = (short[])(tempStateTable.elementAt(oldRowNum));
                        System.arraycopy(oldRow, 0, newRow, 0, numCategories + 1);
                        tempStateTable.addElement(newRow);
                        oldValues[i] = (short)combinedRowNum;


//System.out.println("lastOldRowNum = " + lastOldRowNum);
//System.out.println("lastCombinedRowNum = " + lastCombinedRowNum);
//System.out.println("decisionPointList.contains(lastOldRowNum) = " + decisionPointList.contains(new Integer(lastOldRowNum)));
//System.out.println("decisionPointList.contains(lastCombinedRowNum) = " + decisionPointList.contains(new Integer(lastCombinedRowNum)));

                        // if the decision point list contains either of the parent rows,
                        // update it to include the new row as well
                        if ((decisionPointList.contains(new Integer(oldRowNum))
                                || decisionPointList.contains(new Integer(newRowNum)))
                            && !decisionPointList.contains(new Integer(combinedRowNum))
                        ) {
                            decisionPointList.addElement(new Integer(combinedRowNum));
                        }

                        // do the same thing with the list of rows being updated
                        if ((rowsBeingUpdated.contains(new Integer(oldRowNum))
                                || rowsBeingUpdated.contains(new Integer(newRowNum)))
                            && !rowsBeingUpdated.contains(new Integer(combinedRowNum))
                        ) {
                            decisionPointList.addElement(new Integer(combinedRowNum));
                        }
                        // now (groan) do the same thing for all the entries on the
                        // decision point stack
                        for (int k = 0; k < decisionPointStack.size(); k++) {
                            Vector dpl = (Vector)decisionPointStack.elementAt(k);
                            if ((dpl.contains(new Integer(oldRowNum))
                                    || dpl.contains(new Integer(newRowNum)))
                                && !dpl.contains(new Integer(combinedRowNum))
                            ) {
                                dpl.addElement(new Integer(combinedRowNum));
                            }
                        }

                        // FINALLY (puff puff puff), call mergeStates() recursively to copy
                        // the row referred to by newValues into the new row and resolve any
                        // conflicts that come up at that level
                        mergeStates(combinedRowNum, (short[])(tempStateTable.elementAt(
                                        newValues[i])), rowsBeingUpdated);
                    }
                }
            }
            return;
        }

        /**
         * The merge list is a list of pairs of rows that have been merged somewhere in
         * the process of building this state table, along with the row number of the
         * row containing the merged state.  This function looks up a pair of row numbers
         * and returns the row number of the row they combine into.  (It returns 0 if
         * this pair of rows isn't in the merge list.)
         */
        private int searchMergeList(int a, int b) {
            // if there is no merge list, there obviously isn't anything in it
            if (mergeList == null) {
                return 0;
            }

            // otherwise, for each element in the merge list...
            else {
                int[] entry;
                for (int i = 0; i < mergeList.size(); i++) {
                    entry = (int[])(mergeList.elementAt(i));

                    // we have a hit if the two row numbers match the two row numbers
                    // in the beginning of the entry (the two that combine), in either
                    // order
                    if ((entry[0] == a && entry[1] == b) || (entry[0] == b && entry[1] == a)) {
                        return entry[2];
                    }

                    // we also have a hit if one of the two row numbers matches the marged
                    // row number and the other one matches one of the original row numbers
                    if ((entry[2] == a && (entry[0] == b || entry[1] == b))) {
                        return entry[2];
                    }
                    if ((entry[2] == b && (entry[0] == a || entry[1] == a))) {
                        return entry[2];
                    }
                }
                return 0;
            }
        }

        /**
         * This function is used to update the list of current loooping states (i.e.,
         * states that are controlled by a *? construct).  It backfills values from
         * the looping states into unpopulated cells of the states that are currently
         * marked for backfilling, and then updates the list of looping states to be
         * the new list
         * @param newLoopingStates The list of new looping states
         * @param endStates The list of states to treat as end states (states that
         * can exit the loop).
         */
        private void setLoopingStates(Vector newLoopingStates, Vector endStates) {

            // if the current list of looping states isn't empty, we have to backfill
            // values from the looping states into the states that are waiting to be
            // backfilled
            if (!loopingStates.isEmpty()) {
                int loopingState = ((Integer)loopingStates.lastElement()).intValue();
                int rowNum;

                // don't backfill into an end state OR any state reachable from an end state
                // (since the search for reachable states is recursive, it's split out into
                // a separate function, eliminateBackfillStates(), below)
                for (int i = 0; i < endStates.size(); i++) {
                    eliminateBackfillStates(((Integer)endStates.elementAt(i)).intValue());
                }

                // we DON'T actually backfill the states that need to be backfilled here.
                // Instead, we MARK them for backfilling.  The reason for this is that if
                // there are multiple rules in the state-table description, the looping
                // states may have some of their values changed by a succeeding rule, and
                // this wouldn't be reflected in the backfilled states.  We mark a state
                // for backfilling by putting the row number of the state to copy from
                // into the flag cell at the end of the row
                for (int i = 0; i < statesToBackfill.size(); i++) {
                    rowNum = ((Integer)statesToBackfill.elementAt(i)).intValue();
                    short[] state = (short[])tempStateTable.elementAt(rowNum);
                    state[numCategories] =
                        (short)((state[numCategories] & ALL_FLAGS) | loopingState);
                }
                statesToBackfill.removeAllElements();
                loopingStates.removeAllElements();
            }

            if (newLoopingStates != null) {
                loopingStates = (Vector)newLoopingStates.clone();
            }
        }

        /**
         * This removes "ending states" and states reachable from them from the
         * list of states to backfill.
         * @param The row number of the state to remove from the backfill list
         */
        private void eliminateBackfillStates(int baseState) {

            // don't do anything unless this state is actually in the backfill list...
            if (statesToBackfill.contains(new Integer(baseState))) {

                // if it is, take it out
                statesToBackfill.removeElement(new Integer(baseState));

                // then go through and recursively call this function for every
                // state that the base state points to
                short[] state = (short[])tempStateTable.elementAt(baseState);
                for (int i = 0; i < numCategories; i++) {
                    if (state[i] != 0) {
                        eliminateBackfillStates(state[i]);
                    }
                }
            }
        }

        /**
         * This function completes the backfilling process by actually doing the
         * backfilling on the states that are marked for it
         */
        private void backfillLoopingStates() {
            short[] state;
            short[] loopingState = null;
            int loopingStateRowNum = 0;
            int fromState;

            // for each state in the state table...
            for (int i = 0; i < tempStateTable.size(); i++) {
                state = (short[])tempStateTable.elementAt(i);

                // check the state's flag word to see if it's marked for backfilling
                // (it's marked for backfilling if any bits other than the two high-order
                // bits are set-- if they are, then the flag word, minus the two high bits,
                // is the row number to copy from)
                fromState = state[numCategories] & ~ALL_FLAGS;
                if (fromState > 0) {

                    // load up the state to copy from (if we haven't already)
                    if (fromState != loopingStateRowNum) {
                        loopingStateRowNum = fromState;
                        loopingState = (short[])tempStateTable.elementAt(loopingStateRowNum);
                    }

                    // clear out the backfill part of the flag word
                    state[numCategories] &= ALL_FLAGS;

                    // then fill all zero cells in the current state with values
                    // from the corresponding cells of the fromState
                    for (int j = 0; j < state.length; j++) {
                        if (state[j] == 0) {
                            state[j] = loopingState[j];
                        }
                        else if (state[j] == DONT_LOOP_FLAG) {
                            state[j] = 0;
                        }
                    }
                }
            }
        }

        /**
         * This function completes the state-table-building process by doing several
         * postprocessing steps and copying everything into its final resting place
         * in the iterator itself
         * @param forward True if we're working on the forward state table
         */
        private void finishBuildingStateTable(boolean forward) {
//debugPrintTempStateTable();
            // start by backfilling the looping states
            backfillLoopingStates();
//debugPrintTempStateTable();

            int[] rowNumMap = new int[tempStateTable.size()];
            Stack rowsToFollow = new Stack();
            rowsToFollow.push(new Integer(1));
            rowNumMap[1] = 1;

            // determine which states are no longer reachable from the start state
            // (the reachable states will have their row numbers in the row number
            // map, and the nonreachable states will have zero in the row number map)
            while (rowsToFollow.size() != 0) {
                int rowNum = ((Integer)rowsToFollow.pop()).intValue();
                short[] row = (short[])(tempStateTable.elementAt(rowNum));

                for (int i = 0; i < numCategories; i++) {
                    if (row[i] != 0) {
                        if (rowNumMap[row[i]] == 0) {
                            rowNumMap[row[i]] = row[i];
                            rowsToFollow.push(new Integer(row[i]));
                        }
                    }
                }
            }
/*
System.out.println("The following rows are not reachable:");
for (int i = 1; i < rowNumMap.length; i++)
if (rowNumMap[i] == 0) System.out.print("\t" + i);
System.out.println();
*/

            // variable not used boolean madeChange;
            int newRowNum;

            // algorithm for minimizing the number of states in the table adapted from
            // Aho & Ullman, "Principles of Compiler Design"
            // The basic idea here is to organize the states into classes.  When we're done,
            // all states in the same class can be considered identical and all but one eliminated.

            // initially assign states to classes based on the number of populated cells they
            // contain (the class number is the number of populated cells)
            int[] stateClasses = new int[tempStateTable.size()];
            int nextClass = numCategories + 1;
            short[] state1, state2;
            for (int i = 1; i < stateClasses.length; i++) {
                if (rowNumMap[i] == 0) {
                    continue;
                }
                state1 = (short[])tempStateTable.elementAt(i);
                for (int j = 0; j < numCategories; j++) {
                    if (state1[j] != 0) {
                        ++stateClasses[i];
                    }
                }
                if (stateClasses[i] == 0) {
                    stateClasses[i] = nextClass;
                }
            }
            ++nextClass;

            // then, for each class, elect the first member of that class as that class's
            // "representative".  For each member of the class, compare it to the "representative."
            // If there's a column position where the state being tested transitions to a
            // state in a DIFFERENT class from the class where the "representative" transitions,
            // then move the state into a new class.  Repeat this process until no new classes
            // are created.
            int currentClass;
            int lastClass;
            boolean split;

            do {
//System.out.println("Making a pass...");
                currentClass = 1;
                lastClass = nextClass;
                while (currentClass < nextClass) {
//System.out.print("States in class #" + currentClass +":");
                    split = false;
                    state1 = state2 = null;
                    for (int i = 0; i < stateClasses.length; i++) {
                        if (stateClasses[i] == currentClass) {
//System.out.print("\t" + i);
                            if (state1 == null) {
                                state1 = (short[])tempStateTable.elementAt(i);
                            }
                            else {
                                state2 = (short[])tempStateTable.elementAt(i);
                                for (int j = 0; j < state2.length; j++)
                                    if ((j == numCategories && state1[j] != state2[j] && forward)
                                            || (j != numCategories && stateClasses[state1[j]]
                                            != stateClasses[state2[j]])) {
                                        stateClasses[i] = nextClass;
                                        split = true;
                                        break;
                                    }
                            }
                        }
                    }
                    if (split) {
                        ++nextClass;
                    }
                    ++currentClass;
//System.out.println();
                }
            } while (lastClass != nextClass);

            // at this point, all of the states in a class except the first one (the
            //"representative") can be eliminated, so update the row-number map accordingly
            int[] representatives = new int[nextClass];
            for (int i = 1; i < stateClasses.length; i++)
                if (representatives[stateClasses[i]] == 0) {
                    representatives[stateClasses[i]] = i;
                }
                else {
                    rowNumMap[i] = representatives[stateClasses[i]];
                }
//System.out.println("Renumbering...");

            // renumber all remaining rows...
            // first drop all that are either unreferenced or not a class representative
            for (int i = 1; i < rowNumMap.length; i++) {
                if (rowNumMap[i] != i) {
                    tempStateTable.setElementAt(null, i);
                }
            }

            // then calculate everybody's new row number and update the row
            // number map appropriately (the first pass updates the row numbers
            // of all the class representatives [the rows we're keeping], and the
            // second pass updates the cross references for all the rows that
            // are being deleted)
            newRowNum = 1;
            for (int i = 1; i < rowNumMap.length; i++) {
                if (tempStateTable.elementAt(i) != null) {
                    rowNumMap[i] = newRowNum++;
                }
            }
            for (int i = 1; i < rowNumMap.length; i++) {
                if (tempStateTable.elementAt(i) == null) {
                    rowNumMap[i] = rowNumMap[rowNumMap[i]];
                }
            }
//for (int i = 1; i < rowNumMap.length; i++) rowNumMap[i] = i; int newRowNum = rowNumMap.length;

            // allocate the permanent state table, and copy the remaining rows into it
            // (adjusting all the cell values, of course)

            // this section does that for the forward state table
            if (forward) {
                endStates = new boolean[newRowNum];
                lookaheadStates = new boolean[newRowNum];
                stateTable = new short[newRowNum * numCategories];
                int p = 0;
                int p2 = 0;
                for (int i = 0; i < tempStateTable.size(); i++) {
                    short[] row = (short[])(tempStateTable.elementAt(i));
                    if (row == null) {
                        continue;
                    }
                    for (int j = 0; j < numCategories; j++) {
                        stateTable[p] = (short)(rowNumMap[row[j]]);
                        ++p;
                    }
                    endStates[p2] = ((row[numCategories] & END_STATE_FLAG) != 0);
                    lookaheadStates[p2] = ((row[numCategories] & LOOKAHEAD_STATE_FLAG) != 0);
                    ++p2;
                }
            }

            // and this section does it for the backward state table
            else {
                backwardsStateTable = new short[newRowNum * numCategories];
                int p = 0;
                for (int i = 0; i < tempStateTable.size(); i++) {
                    short[] row = (short[])(tempStateTable.elementAt(i));
                    if (row == null) {
                        continue;
                    }
                    for (int j = 0; j < numCategories; j++) {
                        backwardsStateTable[p] = (short)(rowNumMap[row[j]]);
                        ++p;
                    }
                }
            }
        }

        /**
         * This function builds the backward state table from the forward state
         * table and any additional rules (identified by the ! on the front)
         * supplied in the description
         */
        private void buildBackwardsStateTable(Vector tempRuleList) {

            // create the temporary state table and seed it with two rows (row 0
            // isn't used for anything, and we have to create row 1 (the initial
            // state) before we can do anything else
            tempStateTable = new Vector();
            tempStateTable.addElement(new short[numCategories + 1]);
            tempStateTable.addElement(new short[numCategories + 1]);

            // although the backwards state table is built automatically from the forward
            // state table, there are some situations (the default sentence-break rules,
            // for example) where this doesn't yield enough stop states, causing a dramatic
            // drop in performance.  To help with these cases, the user may supply
            // supplemental rules that are added to the backward state table.  These have
            // the same syntax as the normal break rules, but begin with '!' to distinguish
            // them from normal break rules
            for (int i = 0; i < tempRuleList.size(); i++) {
                String rule = (String)tempRuleList.elementAt(i);
                if (rule.charAt(0) == '!') {
                    parseRule(rule.substring(1), false);
                }
            }
            backfillLoopingStates();

            // Backwards iteration is qualitatively different from forwards iteration.
            // This is because backwards iteration has to be made to operate from no context
            // at all-- the user should be able to ask BreakIterator for the break position
            // immediately on either side of some arbitrary offset in the text.  The
            // forward iteration table doesn't let us do that-- it assumes complete
            // information on the context, which means starting from the beginning of the
            // document.
            // The way we do backward and random-access iteration is to back up from the
            // current (or user-specified) position until we see something we're sure is
            // a break position (it may not be the last break position immediately
            // preceding our starting point, however).  Then we roll forward from there to
            // locate the actual break position we're after.
            // This means that the backwards state table doesn't have to identify every
            // break position, allowing the building algorithm to be much simpler.  Here,
            // we use a "pairs" approach, scanning the forward-iteration state table for
            // pairs of character categories we ALWAYS break between, and building a state
            // table from that information.  No context is required-- all this state table
            // looks at is a pair of adjacent characters.

            // It's possible that the user has supplied supplementary rules (see above).
            // This has to be done first to keep parseRule() and friends from becoming
            // EVEN MORE complicated.  The automatically-generated states are appended
            // onto the end of the state table, and then the two sets of rules are
            // stitched together at the end.  Take note of the row number of the
            // first row of the auromatically-generated part.
            int backTableOffset = tempStateTable.size();
            if (backTableOffset > 2) {
                ++backTableOffset;
            }

            // the automatically-generated part of the table models a two-dimensional
            // array where the two dimensions represent the two characters we're currently
            // looking at.  To model this as a state table, we actually need one additional
            // row to represent the initial state.  It gets populated with the row numbers
            // of the other rows (in order).
            for (int i = 0; i < numCategories + 1; i++)
                tempStateTable.addElement(new short[numCategories + 1]);

            short[] state = (short[])tempStateTable.elementAt(backTableOffset - 1);
            for (int i = 0; i < numCategories; i++)
                state[i] = (short)(i + backTableOffset);

            // scavenge the forward state table for pairs of character categories
            // that always have a break between them.  The algorithm is as follows:
            // Look down each column in the state table.  For each nonzero cell in
            // that column, look up the row it points to.  For each nonzero cell in
            // that row, populate a cell in the backwards state table: the row number
            // of that cell is the number of the column we were scanning (plus the
            // offset that locates this sub-table), and the column number of that cell
            // is the column number of the nonzero cell we just found.  This cell is
            // populated with its own column number (adjusted according to the actual
            // location of the sub-table).  This process will produce a state table
            // whose behavior is the same as looking up successive pairs of characters
            // in an array of Booleans to determine whether there is a break.
            int numRows = stateTable.length / numCategories;
            for (int column = 0; column < numCategories; column++) {
                for (int row = 0; row < numRows; row++) {
                    int nextRow = lookupState(row, column);
                    if (nextRow != 0) {
                        for (int nextColumn = 0; nextColumn < numCategories; nextColumn++) {
                            int cellValue = lookupState(nextRow, nextColumn);
                            if (cellValue != 0) {
                                state = (short[])tempStateTable.elementAt(nextColumn +
                                                backTableOffset);
                                state[column] = (short)(column + backTableOffset);
                            }
                        }
                    }
                }
            }

//debugPrintTempStateTable();
            // if the user specified some backward-iteration rules with the ! token,
            // we have to merge the resulting state table with the auto-generated one
            // above.  First copy the populated cells from row 1 over the populated
            // cells in the auto-generated table.  Then copy values from row 1 of the
            // auto-generated table into all of the the unpopulated cells of the
            // rule-based table.
            if (backTableOffset > 1) {

                // for every row in the auto-generated sub-table, if a cell is
                // populated that is also populated in row 1 of the rule-based
                // sub-table, copy the value from row 1 over the value in the
                // auto-generated sub-table
                state = (short[])tempStateTable.elementAt(1);
                for (int i = backTableOffset - 1; i < tempStateTable.size(); i++) {
                    short[] state2 = (short[])tempStateTable.elementAt(i);
                    for (int j = 0; j < numCategories; j++) {
                        if (state[j] != 0 && state2[j] != 0) {
                            state2[j] = state[j];
                        }
                    }
                }

                // now, for every row in the rule-based sub-table that is not
                // an end state, fill in all unpopulated cells with the values
                // of the corresponding cells in the first row of the auto-
                // generated sub-table.
                state = (short[])tempStateTable.elementAt(backTableOffset - 1);
                for (int i = 1; i < backTableOffset - 1; i++) {
                    short[] state2 = (short[])tempStateTable.elementAt(i);
                    if ((state2[numCategories] & END_STATE_FLAG) == 0) {
                        for (int j = 0; j < numCategories; j++) {
                            if (state2[j] == 0) {
                                state2[j] = state[j];
                            }
                        }
                    }
                }
            }

//debugPrintTempStateTable();

            // finally, clean everything up and copy it into the actual BreakIterator
            // by calling finishBuildingStateTable()
            finishBuildingStateTable(false);
/*
System.out.print("C:\t");
for (int i = 0; i < numCategories; i++)
System.out.print(Integer.toString(i) + "\t");
System.out.println(); System.out.print("=================================================");
for (int i = 0; i < backwardsStateTable.length; i++) {
if (i % numCategories == 0) {
System.out.println();
System.out.print(Integer.toString(i / numCategories) + ":\t");
}
if (backwardsStateTable[i] == 0) System.out.print(".\t"); else System.out.print(Integer.toString(backwardsStateTable[i]) + "\t");
}
System.out.println();
*/
        }

        /**
         * Throws an IllegalArgumentException representing a syntax error in the rule
         * description.  The exception's message contains some debugging information.
         * @param message A message describing the problem
         * @param position The position in the description where the problem was
         * discovered
         * @param context The string containing the error
         * @internal
         */
        protected void error(String message, int position, String context) {
            throw new IllegalArgumentException("Parse error: " + message + "\n" +
                    Utility.escape(context.substring(0, position)) + "\n\n" +
                    Utility.escape(context.substring(position)));
        }

    ///CLOVER:OFF
        /**
         * @internal
         */
        protected void debugPrintVector(String label, Vector v) {
            System.out.print(label);
            for (int i = 0; i < v.size(); i++)
                System.out.print(v.elementAt(i).toString() + "\t");
            System.out.println();
        }

        /**
         * @internal
         */
        protected void debugPrintVectorOfVectors(String label1, String label2, Vector v) {
            System.out.println(label1);
            for (int i = 0; i < v.size(); i++)
                debugPrintVector(label2, (Vector)v.elementAt(i));
        }

        /**
         * @internal
         */
        protected void debugPrintTempStateTable() {
            System.out.println("      tempStateTable:");
            System.out.print("        C:\t");
            for (int i = 0; i <= numCategories; i++)
                System.out.print(Integer.toString(i) + "\t");
            System.out.println();
            for (int i = 1; i < tempStateTable.size(); i++) {
                short[] row = (short[])(tempStateTable.elementAt(i));
                System.out.print("        " + i + ":\t");
                for (int j = 0; j < row.length; j++) {
                    if (row[j] == 0) {
                        System.out.print(".\t");
                    }
                    else {
                        System.out.print(Integer.toString(row[j]) + "\t");
                    }
                }
                System.out.println();
            }
        }

    }
    ///CLOVER:ON

    /*
     * This class exists to work around a bug in HotJava's implementation
     * of CharacterIterator, which incorrectly handles setIndex(endIndex).
     * This iterator relies only on base.setIndex(n) where n is less than
     * endIndex.
     *
     * One caveat:  if the base iterator's begin and end indices change
     * the change will not be reflected by this wrapper.  Does that matter?
     */
    ///CLOVER:OFF
    // Only used for HotJava, so clover won't encounter it
    private static final class SafeCharIterator implements CharacterIterator,
                                                           Cloneable {

        private CharacterIterator base;
        private int rangeStart;
        private int rangeLimit;
        private int currentIndex;

        SafeCharIterator(CharacterIterator base) {
            this.base = base;
            this.rangeStart = base.getBeginIndex();
            this.rangeLimit = base.getEndIndex();
            this.currentIndex = base.getIndex();
        }

        public char first() {
            return setIndex(rangeStart);
        }

        public char last() {
            return setIndex(rangeLimit - 1);
        }

        public char current() {
            if (currentIndex < rangeStart || currentIndex >= rangeLimit) {
                return DONE;
            }
            else {
                return base.setIndex(currentIndex);
            }
        }

        public char next() {

            currentIndex++;
            if (currentIndex >= rangeLimit) {
                currentIndex = rangeLimit;
                return DONE;
            }
            else {
                return base.setIndex(currentIndex);
            }
        }

        public char previous() {

            currentIndex--;
            if (currentIndex < rangeStart) {
                currentIndex = rangeStart;
                return DONE;
            }
            else {
                return base.setIndex(currentIndex);
            }
        }

        public char setIndex(int i) {

            if (i < rangeStart || i > rangeLimit) {
                throw new IllegalArgumentException("Invalid position");
            }
            currentIndex = i;
            return current();
        }

        public int getBeginIndex() {
            return rangeStart;
        }

        public int getEndIndex() {
            return rangeLimit;
        }

        public int getIndex() {
            return currentIndex;
        }

        public Object clone() {

            SafeCharIterator copy = null;
            try {
                copy = (SafeCharIterator) super.clone();
            }
            catch(CloneNotSupportedException e) {
                throw new Error("Clone not supported: " + e);
            }

            CharacterIterator copyOfBase = (CharacterIterator) base.clone();
            copy.base = copyOfBase;
            return copy;
        }
    }
    ///CLOVER:ON

    ///CLOVER:OFF
   /**
     * @internal
     */
    public static void debugPrintln(String s) {
        final String zeros = "0000";
        String temp;
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= ' ' && c < '\u007f') {
                out.append(c);
            }
            else {
                out.append("\\u");
                temp = Integer.toHexString((int)c);
                out.append(zeros.substring(0, 4 - temp.length()));
                out.append(temp);
            }
        }
        System.out.println(out);
    }
    ///CLOVER:ON
}

