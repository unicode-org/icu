package com.ibm.text;

import com.ibm.text.resources.ResourceReader;
import com.ibm.util.Utility;
import java.util.Stack;
import java.util.Vector;
import java.text.ParsePosition;

class TransliteratorParser {

    //----------------------------------------------------------------------
    // Data members
    //----------------------------------------------------------------------

    /**
     * PUBLIC data member containing the parsed data object, or null if
     * there were no rules.
     */
    public RuleBasedTransliterator.Data data;

    /**
     * PUBLIC data member.
     * The block of ::IDs, both at the top and at the bottom.
     * Inserted into these may be additional rules at the
     * idSplitPoint.
     */
    public String idBlock;

    /**
     * PUBLIC data member.
     * In a compound RBT, the index at which the RBT rules are
     * inserted into the ID block.  Index 0 means before any IDs
     * in the block.  Index idBlock.length() means after all IDs
     * in the block.  Index is a string index.
     */
    public int idSplitPoint;

    /**
     * PUBLIC data member containing the parsed compound filter, if any.
     */
    public UnicodeSet compoundFilter;


    // The number of rules parsed.  This tells us if there were
    // any actual transliterator rules, or if there were just ::ID
    // block IDs.
    private int ruleCount;

    private int direction;

    /**
     * Temporary symbol table used during parsing.
     */
    private ParseData parseData;

    /**
     * Temporary vector of set variables.  When parsing is complete, this
     * is copied into the array data.variables.  As with data.variables,
     * element 0 corresponds to character data.variablesBase.
     */
    private Vector variablesVector;

    /**
     * The next available stand-in for variables.  This starts at some point in
     * the private use area (discovered dynamically) and increments up toward
     * <code>variableLimit</code>.  At any point during parsing, available
     * variables are <code>variableNext..variableLimit-1</code>.
     */
    private char variableNext;

    /**
     * The last available stand-in for variables.  This is discovered
     * dynamically.  At any point during parsing, available variables are
     * <code>variableNext..variableLimit-1</code>.  During variable definition
     * we use the special value variableLimit-1 as a placeholder.
     */
    private char variableLimit;

    /**
     * When we encounter an undefined variable, we do not immediately signal
     * an error, in case we are defining this variable, e.g., "$a = [a-z];".
     * Instead, we save the name of the undefined variable, and substitute
     * in the placeholder char variableLimit - 1, and decrement
     * variableLimit.
     */
    private String undefinedVariableName;

    //----------------------------------------------------------------------
    // Constants
    //----------------------------------------------------------------------

    // Indicator for ID blocks
    private static final String ID_TOKEN = "::";
    private static final int ID_TOKEN_LEN = 2;

    // Operators
    private static final char VARIABLE_DEF_OP   = '=';
    private static final char FORWARD_RULE_OP   = '>';
    private static final char REVERSE_RULE_OP   = '<';
    private static final char FWDREV_RULE_OP    = '~'; // internal rep of <> op

    private static final String OPERATORS = "=><";

    // Other special characters
    private static final char QUOTE               = '\'';
    private static final char ESCAPE              = '\\';
    private static final char END_OF_RULE         = ';';
    private static final char RULE_COMMENT_CHAR   = '#';

    private static final char CONTEXT_ANTE        = '{'; // ante{key
    private static final char CONTEXT_POST        = '}'; // key}post
    private static final char SET_OPEN            = '[';
    private static final char SET_CLOSE           = ']';
    private static final char CURSOR_POS          = '|';
    private static final char CURSOR_OFFSET       = '@';
    private static final char ANCHOR_START        = '^';

    private static final char KLEENE_STAR         = '*';
    private static final char ONE_OR_MORE         = '+';
    private static final char ZERO_OR_ONE         = '?';

    // By definition, the ANCHOR_END special character is a
    // trailing SymbolTable.SYMBOL_REF character.
    // private static final char ANCHOR_END       = '$';

    // Segments of the input string are delimited by "(" and ")".  In the
    // output string these segments are referenced as "$1" through "$9".
    private static final char SEGMENT_OPEN        = '(';
    private static final char SEGMENT_CLOSE       = ')';

    //----------------------------------------------------------------------
    // class ParseData
    //----------------------------------------------------------------------

    /**
     * This class implements the SymbolTable interface.  It is used
     * during parsing to give UnicodeSet access to variables that
     * have been defined so far.  Note that it uses variablesVector,
     * _not_ data.variables.
     */
    private class ParseData implements SymbolTable {

        /**
         * Implement SymbolTable API.
         */
        public char[] lookup(String name) {
            return (char[]) data.variableNames.get(name);
        }

        /**
         * Implement SymbolTable API.
         */
        public UnicodeSet lookupSet(int ch) {
            // Note that we cannot use data.lookupSet() because the
            // set array has not been constructed yet.
            int i = ch - data.variablesBase;
            if (i >= 0 && i < variablesVector.size()) {
                return (UnicodeSet) variablesVector.elementAt(i);
            }
            return null;
        }

        /**
         * Implement SymbolTable API.  Parse out a symbol reference
         * name.
         */
        public String parseReference(String text, ParsePosition pos, int limit) {
            int start = pos.getIndex();
            int i = start;
            while (i < limit) {
                char c = text.charAt(i);
                if ((i==start && !Character.isUnicodeIdentifierStart(c)) ||
                    !Character.isUnicodeIdentifierPart(c)) {
                    break;
                }
                ++i;
            }
            if (i == start) { // No valid name chars
                return null;
            }
            pos.setIndex(i);
            return text.substring(start, i);
        }
    }

    //----------------------------------------------------------------------
    // classes RuleBody, RuleArray, and RuleReader
    //----------------------------------------------------------------------

    /**
     * A private abstract class representing the interface to rule
     * source code that is broken up into lines.  Handles the
     * folding of lines terminated by a backslash.  This folding
     * is limited; it does not account for comments, quotes, or
     * escapes, so its use to be limited.
     */
    private static abstract class RuleBody {

        /**
         * Retrieve the next line of the source, or return null if
         * none.  Folds lines terminated by a backslash into the
         * next line, without regard for comments, quotes, or
         * escapes.
         */
        String nextLine() {
            String s = handleNextLine();
            if (s != null &&
                s.length() > 0 &&
                s.charAt(s.length() - 1) == '\\') {

                StringBuffer b = new StringBuffer(s);
                do {
                    b.deleteCharAt(b.length()-1);
                    s = handleNextLine();
                    if (s == null) {
                        break;
                    }
                    b.append(s);
                } while (s.length() > 0 &&
                         s.charAt(s.length() - 1) == '\\');

                s = b.toString();
            }
            return s;
        }

        /**
         * Reset to the first line of the source.
         */
        abstract void reset();

        /**
         * Subclass method to return the next line of the source.
         */
        abstract String handleNextLine();
    };

    /**
     * RuleBody subclass for a String[] array.
     */
    private static class RuleArray extends RuleBody {
        String[] array;
        int i;
        public RuleArray(String[] array) { this.array = array; i = 0; }
        public String handleNextLine() {
            return (i < array.length) ? array[i++] : null;
        }
        public void reset() {
            i = 0;
        }
    };

    /**
     * RuleBody subclass for a ResourceReader.
     */
    private static class RuleReader extends RuleBody {
        ResourceReader reader;
        public RuleReader(ResourceReader reader) { this.reader = reader; }
        public String handleNextLine() {
            try {
                return reader.readLine();
            } catch (java.io.IOException e) {}
            return null;
        }
        public void reset() {
            reader.reset();
        }
    };

    //----------------------------------------------------------------------
    // class Segments
    //----------------------------------------------------------------------

    /**
     * Segments are parentheses-enclosed regions of the input string.
     * These are referenced in the output string using the notation $1,
     * $2, etc.  Numbering is in order of appearance of the left
     * parenthesis.  Number is one-based.  Segments are defined as start,
     * limit pairs.  Segments may nest.
     *
     * During parsing, segment data is encoded in an object of class
     * Segments.  At runtime, the same data is encoded in compact form as
     * an array of integers in a TransliterationRule.  The runtime encoding
     * must satisfy three goals:
     *
     * 1. Iterate over the offsets in a pattern, from left to right,
     *    and indicate all segment boundaries, in order.  This is done
     *    during matching.
     *
     * 2. Given a reference $n, produce the start and limit offsets
     *    for that segment.  This is done during replacement.
     *
     * 3. Similar to goal 1, but in addition, indicate whether each
     *    segment boundary is a start or a limit, in other words, whether
     *    each is an open paren or a close paren.  This is required by
     *    the toRule() method.
     *
     * Goal 1 must be satisfied at high speed since this is done during
     * matching.  Goal 2 is next most important.  Goal 3 is not performance
     * critical since it is only needed by toRule().
     *
     * The array of integers is actually two arrays concatenated.  The
     * first gives the index values of the open and close parentheses in
     * the order they appear.  The second maps segment numbers to the
     * indices of the first array.  The two arrays have the same length.
     * Iterating over the first array satisfies goal 1.  Indexing into the
     * second array satisfies goal 2.  Goal 3 is satisfied by iterating
     * over the second array and constructing the required data when
     * needed.  This is what toRule() does.
     *
     * Example:  (a b(c d)e f)
     *            0 1 2 3 4 5 6
     *
     * First array: Indices are 0, 2, 4, and 6.

     * Second array: $1 is at 0 and 6, and $2 is at 2 and 4, so the
     * second array is 0, 3, 1 2 -- these give the indices in the
     * first array at which $1:open, $1:close, $2:open, and $2:close
     * occur.
     *
     * The final array is: 2, 7, 0, 2, 4, 6, -1, 2, 5, 3, 4, -1
     *
     * Each subarray is terminated with a -1, and two leading entries
     * give the number of segments and the offset to the first entry
     * of the second array.  In addition, the second array value are
     * all offset by 2 so they index directly into the final array.
     * The total array size is 4*segments[0] + 4.  The second index is
     * 2*segments[0] + 3.
     *
     * In the output string, a segment reference is indicated by a
     * character in a special range, as defined by
     * RuleBasedTransliterator.Data.
     *
     * Most rules have no segments, in which case segments is null, and the
     * output string need not be checked for segment reference characters.
     *
     * See also rbt_rule.h/cpp.
     */
    private static class Segments {

        private Vector offsets; // holds Integer objects

        private Vector isOpenParen; // holds Boolean objects

        private int offset(int i) {
            return ((Integer) offsets.elementAt(i)).intValue();
        }

        private boolean isOpen(int i) {
            return ((Boolean) isOpenParen.elementAt(i)).booleanValue();
        }

        // size of the Vectors
        private int size() {
            // assert(offset.size() == isOpenParen.size());
            return offsets.size();
        }

        public Segments() {
            offsets = new Vector();
            isOpenParen = new Vector();
        }

        public void addParenthesisAt(int offset, boolean isOpen) {
            offsets.addElement(new Integer(offset));
            isOpenParen.addElement(new Boolean(isOpen));
        }

        public int getLastParenOffset(boolean[] isOpenParen) {
            if (size() == 0) {
                return -1;
            }
            isOpenParen[0] = isOpen(size()-1);
            return offset(size()-1);
        }

        // Remove the last (rightmost) segment.  Store its offsets in start
        // and limit, and then convert all offsets at or after start to be
        // equal to start.  Upon failure, return FALSE.  Assume that the
        // caller has already called getLastParenOffset() and validated that
        // there is at least one parenthesis and that the last one is a close
        // paren.
        public boolean extractLastParenSubstring(int[] start, int[] limit) {
            // assert(offsets.size() > 0);
            // assert(isOpenParen.elementAt(isOpenParen.size()-1) == 0);
            int i = size() - 1;
            int n = 1; // count of close parens we need to match
            // Record position of the last close paren
            limit[0] = offset(i);
            --i; // back up to the one before the last one
            while (i >= 0 && n != 0) {
                n += isOpen(i) ? -1 : 1;
            }
            if (n != 0) {
                return false;
            }
            // assert(i>=0);
            start[0] = offset(i);
            // Reset all segment pairs from i to size() - 1 to [start, start+1).
            while (i<size()) {
                int o = isOpen(i) ? start[0] : (start[0]+1);
                offsets.setElementAt(new Integer(o), i);
                ++i;
            }
            return true;
        }

        // Assume caller has already gotten a TRUE validate().
        public int[] createArray() {
            int c = count(); // number of segments
            int arrayLen = 4*c + 4;
            int[] array = new int[arrayLen];
            int a2offset = 2*c + 3; // offset to array 2

            array[0] = c;
            array[1] = a2offset;
            int i;
            for (i=0; i<2*c; ++i) {
                array[2+i] = offset(i);
            }
            array[a2offset-1] = -1;
            array[arrayLen-1] = -1;
            // Now walk through and match up segment numbers with parentheses.
            // Number segments from 0.  We're going to offset all entries by 2
            // to skip the first two elements, array[0] and array[1].
            Stack stack = new Stack();
            int nextOpen = 0; // seg # of next open, 0-based
            for (i=0; i<2*c; ++i) {
                boolean open = isOpen(i);
                // Let seg be the zero-based segment number.
                // Open parens are at 2*seg in array 2.
                // Close parens are at 2*seg+1 in array 2.
                if (open) {
                    array[a2offset + 2*nextOpen] = 2+i;
                    stack.push(new Integer(nextOpen));
                    ++nextOpen;
                } else {
                    int nextClose = ((Integer) stack.pop()).intValue();
                    array[a2offset + 2*nextClose+1] = 2+i;
                }
            }
            // assert(stack.empty());

            return array;
        }

        public boolean validate() {
            // want number of parens >= 2
            // want number of parens to be even
            // want first paren '('
            // want parens to match up in the end
            if ((size() < 2) || (size() % 2 != 0) || !isOpen(0)) {
                return false;
            }
            int n = 0;
            for (int i=0; i<size(); ++i) {
                n += isOpen(i) ? 1 : -1;
                if (n < 0) {
                    return false;
                }
            }
            return n == 0;
        }

        // Number of segments
        // Assume caller has already gotten a TRUE validate().
        public int count() {
            // assert(validate());
            return size() / 2;
        }
    }

    //----------------------------------------------------------------------
    // class RuleHalf
    //----------------------------------------------------------------------

    /**
     * A class representing one side of a rule.  This class knows how to
     * parse half of a rule.  It is tightly coupled to the method
     * TransliteratorParser.parseRule().
     */
    private static class RuleHalf {

        public String text;

        public int cursor = -1; // position of cursor in text
        public int ante = -1;   // position of ante context marker '{' in text
        public int post = -1;   // position of post context marker '}' in text

        // Record the position of the segment substrings and references.  A
        // given side should have segments or segment references, but not
        // both.
        public Segments segments = null;
        public int maxRef = -1; // index of largest ref (1..9)

        // Record the offset to the cursor either to the left or to the
        // right of the key.  This is indicated by characters on the output
        // side that allow the cursor to be positioned arbitrarily within
        // the matching text.  For example, abc{def} > | @@@ xyz; changes
        // def to xyz and moves the cursor to before abc.  Offset characters
        // must be at the start or end, and they cannot move the cursor past
        // the ante- or postcontext text.  Placeholders are only valid in
        // output text.
        public int cursorOffset = 0; // only nonzero on output side

        public boolean anchorStart = false;
        public boolean anchorEnd   = false;

        /**
         * Parse one side of a rule, stopping at either the limit,
         * the END_OF_RULE character, or an operator.  Return
         * the pos of the terminating character (or limit).
         */
        public int parse(String rule, int pos, int limit,
                         TransliteratorParser parser) {
            int start = pos;
            StringBuffer buf = new StringBuffer();
            ParsePosition pp = null;
            int cursorOffsetPos = 0; // Position of first CURSOR_OFFSET on _right_
            boolean done = false;
            int quoteStart = -1; // Most recent 'single quoted string'
            int quoteLimit = -1;
            int varStart = -1; // Most recent $variableReference
            int varLimit = -1;
            int[] iref = new int[1];

        main:
            while (pos < limit && !done) {
                char c = rule.charAt(pos++);
                if (Character.isWhitespace(c)) {
                    // Ignore whitespace.  Note that this is not Unicode
                    // spaces, but Java spaces -- a subset, representing
                    // whitespace likely to be seen in code.
                    continue;
                }
                if (OPERATORS.indexOf(c) >= 0) {
                    --pos; // Backup to point to operator
                    break main;
                }
                if (anchorEnd) {
                    // Text after a presumed end anchor is a syntax err
                    syntaxError("Malformed variable reference", rule, start);
                }
                // Handle escapes
                if (c == ESCAPE) {
                    if (pos == limit) {
                        syntaxError("Trailing backslash", rule, start);
                    }
                    iref[0] = pos;
                    int escaped = Utility.unescapeAt(rule, iref);
                    pos = iref[0];
                    if (escaped == -1) {
                        syntaxError("Malformed escape", rule, start);
                    }
                    UTF16.append(buf, escaped);
                    continue;
                }
                // Handle quoted matter
                if (c == QUOTE) {
                    int iq = rule.indexOf(QUOTE, pos);
                    if (iq == pos) {
                        buf.append(c); // Parse [''] outside quotes as [']
                        ++pos;
                    } else {
                        /* This loop picks up a segment of quoted text of the
                         * form 'aaaa' each time through.  If this segment
                         * hasn't really ended ('aaaa''bbbb') then it keeps
                         * looping, each time adding on a new segment.  When it
                         * reaches the final quote it breaks.
                         */
                        quoteStart = buf.length();
                        for (;;) {
                            if (iq < 0) {
                                syntaxError("Unterminated quote", rule, start);
                            }
                            buf.append(rule.substring(pos, iq));
                            pos = iq+1;
                            if (pos < limit && rule.charAt(pos) == QUOTE) {
                            // Parse [''] inside quotes as [']
                                iq = rule.indexOf(QUOTE, pos+1);
                            // Continue looping
                            } else {
                                break;
                            }
                        }
                        quoteLimit = buf.length();
                    }
                    continue;
                }
                switch (c) {
                case ANCHOR_START:
                    if (buf.length() == 0 && !anchorStart) {
                        anchorStart = true;
                    } else {
                        syntaxError("Misplaced anchor start",
                                    rule, start);
                    }
                    break;
                case SEGMENT_OPEN:
                case SEGMENT_CLOSE:
                    // Handle segment definitions "(" and ")"
                    // Parse "(", ")"
                    if (segments == null) {
                        segments = new Segments();
                    }
                    segments.addParenthesisAt(buf.length(), c == SEGMENT_OPEN);
                    break;
                case END_OF_RULE:
                    --pos; // Backup to point to END_OF_RULE
                    break main;
                case SymbolTable.SYMBOL_REF:
                    // Handle variable references and segment references "$1" .. "$9"
                    {
                        // A variable reference must be followed immediately
                        // by a Unicode identifier start and zero or more
                        // Unicode identifier part characters, or by a digit
                        // 1..9 if it is a segment reference.
                        if (pos == limit) {
                            // A variable ref character at the end acts as
                            // an anchor to the context limit, as in perl.
                            anchorEnd = true;
                            break;
                        }
                        // Parse "$1" "$2" .. "$9" .. (no upper limit)
                        c = rule.charAt(pos);
                        int r = Character.digit(c, 10);
                        if (r >= 1 && r <= 9) {
                            ++pos;
                            while (pos < limit) {
                                c = rule.charAt(pos);
                                int d = Character.digit(c, 10);
                                if (d < 0) {
                                    break;
                                }
                                if (r > 214748364 ||
                                    (r == 214748364 && d > 7)) {
                                    syntaxError("Undefined segment reference",
                                                rule, start);
                                }
                                r = 10*r + d;
                            }
                            if (r > maxRef) {
                                maxRef = r;
                            }
                            buf.append(parser.getSegmentStandin(r));
                        } else {
                            if (pp == null) { // Lazy create
                                pp = new ParsePosition(0);
                            }
                            pp.setIndex(pos);
                            String name = parser.parseData.
                                            parseReference(rule, pp, limit);
                            if (name == null) {
                                // This means the '$' was not followed by a
                                // valid name.  Try to interpret it as an
                                // end anchor then.  If this also doesn't work
                                // (if we see a following character) then signal
                                // an error.
                                anchorEnd = true;
                                break;
                            }
                            pos = pp.getIndex();
                            // If this is a variable definition statement,
                            // then the LHS variable will be undefined.  In
                            // that case appendVariableDef() will append the
                            // special placeholder char variableLimit-1.
                            varStart = buf.length();
                            parser.appendVariableDef(name, buf);
                            varLimit = buf.length();
                        }
                    }
                    break;
                case CONTEXT_ANTE:
                    if (ante >= 0) {
                        syntaxError("Multiple ante contexts", rule, start);
                    }
                    ante = buf.length();
                    break;
                case CONTEXT_POST:
                    if (post >= 0) {
                        syntaxError("Multiple post contexts", rule, start);
                    }
                    post = buf.length();
                    break;
                case SET_OPEN:
                    if (pp == null) {
                        pp = new ParsePosition(0);
                    }
                    pp.setIndex(pos-1); // Backup to opening '['
                    buf.append(parser.parseSet(rule, pp));
                    pos = pp.getIndex();
                    break;
                case CURSOR_POS:
                    if (cursor >= 0) {
                        syntaxError("Multiple cursors", rule, start);
                    }
                    cursor = buf.length();
                    break;
                case CURSOR_OFFSET:
                    if (cursorOffset < 0) {
                        if (buf.length() > 0) {
                            syntaxError("Misplaced " + c, rule, start);
                        }
                        --cursorOffset;
                    } else if (cursorOffset > 0) {
                        if (buf.length() != cursorOffsetPos || cursor >= 0) {
                            syntaxError("Misplaced " + c, rule, start);
                        }
                        ++cursorOffset;
                    } else {
                        if (cursor == 0 && buf.length() == 0) {
                            cursorOffset = -1;
                        } else if (cursor < 0) {
                            cursorOffsetPos = buf.length();
                            cursorOffset = 1;
                        } else {
                            syntaxError("Misplaced " + c, rule, start);
                        }
                    }
                    break;
                case KLEENE_STAR:
                case ONE_OR_MORE:
                case ZERO_OR_ONE:
                    // Quantifiers.  We handle single characters, quoted strings,
                    // variable references, and segments.
                    //  a+      matches  aaa
                    //  'foo'+  matches  foofoofoo
                    //  $v+     matches  xyxyxy if $v == xy
                    //  (seg)+  matches  segsegseg
                    {
                        int qstart, qlimit;
                        boolean[] isOpenParen = new boolean[1];
                        boolean isSegment = false;
                        if (segments != null &&
                            segments.getLastParenOffset(isOpenParen) == buf.length()) {
                            // The */+ immediately follows a segment
                            if (isOpenParen[0]) {
                                syntaxError("Misplaced quantifier", rule, start);
                            }
                            int[] startparam = new int[1];
                            int[] limitparam = new int[1];
                            if (!segments.extractLastParenSubstring(startparam, limitparam)) {
                                syntaxError("Mismatched segment delimiters", rule, start);
                            }
                            qstart = startparam[0];
                            qlimit = limitparam[0];
                            isSegment = true;
                        } else {
                            // The */+ follows an isolated character or quote
                            // or variable reference
                            if (buf.length() == quoteLimit) {
                                // The */+ follows a 'quoted string'
                                qstart = quoteStart;
                                qlimit = quoteLimit;
                            } else if (buf.length() == varLimit) {
                                // The */+ follows a $variableReference
                                qstart = varStart;
                                qlimit = varLimit;
                            } else {
                                // The */+ follows a single character
                                qstart = buf.length() - 1;
                                qlimit = qstart + 1;
                            }
                        }
                        UnicodeMatcher m =
                            new StringMatcher(buf.toString(), qstart, qlimit,
                                              isSegment, parser.data);
                        int min = 0;
                        int max = Quantifier.MAX;
                        switch (c) {
                        case ONE_OR_MORE:
                            min = 1;
                            break;
                        case ZERO_OR_ONE:
                            min = 0;
                            max = 1;
                            break;
                            // case KLEENE_STAR:
                            //    do nothing -- min, max already set
                        }
                        m = new Quantifier(m, min, max);
                        buf.setLength(qstart);
                        buf.append(parser.generateStandInFor(m));
                    }
                    break;
                // case SET_CLOSE:
                default:
                    // Disallow unquoted characters other than [0-9A-Za-z]
                    // in the printable ASCII range.  These characters are
                    // reserved for possible future use.
                    if (c >= 0x0021 && c <= 0x007E &&
                        !((c >= '0' && c <= '9') ||
                          (c >= 'A' && c <= 'Z') ||
                          (c >= 'a' && c <= 'z'))) {
                        syntaxError("Unquoted " + c, rule, start);
                    }
                    buf.append(c);
                    break;
                }
            }

            if (cursorOffset > 0 && cursor != cursorOffsetPos) {
                syntaxError("Misplaced " + CURSOR_POS, rule, start);
            }
            text = buf.toString();
            return pos;
        }

        /**
         * Remove context.
         */
        void removeContext() {
            text = text.substring(ante < 0 ? 0 : ante,
                                  post < 0 ? text.length() : post);
            ante = post = -1;
            anchorStart = anchorEnd = false;
        }

        /**
         * Create and return an int[] array of segments.
         */
        int[] createSegments() {
            return (segments == null) ? null : segments.createArray();
        }
    }

    //----------------------------------------------------------------------
    // class Range
    //----------------------------------------------------------------------

    /**
     * A range of Unicode characters.  Support the operations of testing for
     * inclusion (does this range contain this character?) and splitting.
     * Splitting involves breaking a range into two smaller ranges around a
     * character inside the original range.  The split character is not included
     * in either range.  If the split character is at either extreme end of the
     * range, one of the split products is an empty range.
     *
     * This class is used internally to determine the largest available private
     * use character range for variable stand-ins.
     */
    private static class Range implements Cloneable {
        char start;
        int length;

        Range(char start, int length) {
            this.start = start;
            this.length = length;
        }

        public Object clone() {
            return new Range(start, length);
        }

        boolean contains(char c) {
            return c >= start && (c - start) < length;
        }

        /**
         * Assume that contains(c) is true.  Split this range into two new
         * ranges around the character c.  Make this range one of the new ranges
         * (modify it in place) and return the other new range.  The character
         * itself is not included in either range.  If the split results in an
         * empty range (that is, if c == start or c == start + length - 1) then
         * return null.
         */
        Range split(char c) {
            if (c == start) {
                ++start;
                --length;
                return null;
            } else if (c - start == length - 1) {
                --length;
                return null;
            } else {
                ++c;
                Range r = new Range(c, start + length - c);
                length = --c - start;
                return r;
            }
        }

        /**
         * Finds the largest unused subrange by the given string.  A
         * subrange is unused by a string if the string contains no
         * characters in that range.  If the given string contains no
         * characters in this range, then this range itself is
         * returned.
         */
        Range largestUnusedSubrange(RuleBody strings) {
            Vector v = new Vector(1);
            v.addElement(clone());

            strings.reset();
            for (;;) {
                String str = strings.nextLine();
                if (str == null) {
                    break;
                }
                int n = str.length();
                for (int i=0; i<n; ++i) {
                    char c = str.charAt(i);
                    if (contains(c)) {
                        for (int j=0; j<v.size(); ++j) {
                            Range r = (Range) v.elementAt(j);
                            if (r.contains(c)) {
                                r = r.split(c);
                                if (r != null) {
                                    v.addElement(r);
                                }
                                break;
                            }
                        }
                    }
                }
            }

            Range bestRange = null;
            for (int j=0; j<v.size(); ++j) {
                Range r = (Range) v.elementAt(j);
                if (bestRange == null || r.length > bestRange.length) {
                    bestRange = r;
                }
            }

            return bestRange;
        }
    }

    //----------------------------------------------------------------------
    // PUBLIC methods
    //----------------------------------------------------------------------

    /**
     * Constructor.
     */
    public TransliteratorParser() {
    }

    /**
     * Parse a set of rules.  After the parse completes, examine the public
     * data members for results.
     */
    public void parse(String rules, int direction) {
        parseRules(new RuleArray(new String[] { rules }), direction);
    }
   
    /**
     * Parse a set of rules.  After the parse completes, examine the public
     * data members for results.
     */
    public void parse(ResourceReader rules, int direction) {
        parseRules(new RuleReader(rules), direction);
    }

    //----------------------------------------------------------------------
    // PRIVATE methods
    //----------------------------------------------------------------------

    /**
     * Parse an array of zero or more rules.  The strings in the array are
     * treated as if they were concatenated together, with rule terminators
     * inserted between array elements if not present already.
     *
     * Any previous rules are discarded.  Typically this method is called exactly
     * once, during construction.
     *
     * The member this.data will be set to null if there are no rules.
     *
     * @exception IllegalArgumentException if there is a syntax error in the
     * rules
     */
    void parseRules(RuleBody ruleArray, int dir) {
        data = new RuleBasedTransliterator.Data();
        direction = dir;
        ruleCount = 0;
        compoundFilter = null;

        determineVariableRange(ruleArray);
        variablesVector = new Vector();
        parseData = new ParseData();

        StringBuffer errors = null;
        int errorCount = 0;

        ruleArray.reset();

        StringBuffer idBlockResult = new StringBuffer();
        idSplitPoint = -1;
        // The mode marks whether we are in the header ::id block, the
        // rule block, or the footer ::id block.
        // mode == 0: start: rule->1, ::id->0
        // mode == 1: in rules: rule->1, ::id->2
        // mode == 2: in footer rule block: rule->ERROR, ::id->2
        int mode = 0;

        // The compound filter offset is an index into idBlockResult.
        // If it is 0, then the compound filter occurred at the start,
        // and it is the offset to the _start_ of the compound filter
        // pattern.  Otherwise it is the offset to the _limit_ of the
        // compound filter pattern within idBlockResult.
        this.compoundFilter = null;
        int compoundFilterOffset = -1;

    main:
        for (;;) {
            String rule = ruleArray.nextLine();
            if (rule == null) {
                break;
            }
            int pos = 0;
            int limit = rule.length();
            while (pos < limit) {
                char c = rule.charAt(pos++);
                if (Character.isWhitespace(c)) {
                    // Ignore leading whitespace.  Note that this is not
                    // Unicode spaces, but Java spaces -- a subset,
                    // representing whitespace likely to be seen in code.
                    continue;
                }
                // Skip lines starting with the comment character
                if (c == RULE_COMMENT_CHAR) {
                    pos = rule.indexOf("\n", pos) + 1;
                    if (pos == 0) {
                        break; // No "\n" found; rest of rule is a commnet
                    }
                    continue; // Either fall out or restart with next line
                }
                // Often a rule file contains multiple errors.  It's
                // convenient to the rule author if these are all reported
                // at once.  We keep parsing rules even after a failure, up
                // to a specified limit, and report all errors at once.
                try {
                    // We've found the start of a rule or ID.  c is its first
                    // character, and pos points past c.
                    --pos;
                    // Look for an ID token.  Must have at least ID_TOKEN_LEN + 1
                    // chars left.
                    if ((pos + ID_TOKEN_LEN + 1) <= limit &&
                        rule.regionMatches(pos, ID_TOKEN, 0, ID_TOKEN_LEN)) {
                        pos += ID_TOKEN_LEN;
                        c = rule.charAt(pos);
                        while (UCharacter.isWhitespace(c) && pos < limit) {
                            ++pos;
                            c = rule.charAt(pos);
                        }
                        int[] p = new int[] { pos };
                        boolean[] sawDelim = new boolean[1];
                        StringBuffer regenID = new StringBuffer();
                        UnicodeSet[] cpdFilter = new UnicodeSet[1];
                        Transliterator.parseID(rule, regenID, p, sawDelim, cpdFilter, direction, false);
                        if (p[0] == pos || !sawDelim[0]) {
                            // Invalid ::id
                            int i1 = pos + 2;
                            while (i1 < rule.length() && rule.charAt(i1) != ';') {
                                ++i1;
                            }
                            throw new IllegalArgumentException("Invalid ::ID " +
                                                               rule.substring(pos, i1));
                        } else {
                            if (mode == 1) {
                                mode = 2;
                                idSplitPoint = idBlockResult.length();
                            }
                            if (cpdFilter[0] != null) {
                                if (compoundFilter != null) {
                                    // Multiple compound filters
                                    throw new IllegalArgumentException("Multiple compound filters");
                                }
                                compoundFilter = cpdFilter[0];
                                if (idBlockResult.length() == 0) {
                                    compoundFilterOffset = 0;
                                }
                            }
                            String str = rule.substring(pos, p[0]);
                            idBlockResult.append(str);
                            if (!sawDelim[0]) {
                                idBlockResult.append(';');
                            }
                            if (cpdFilter[0] != null && compoundFilterOffset < 0) {
                                compoundFilterOffset = idBlockResult.length();
                            }
                            pos = p[0];
                        }
                    } else {
                        // Parse a rule
                        pos = parseRule(rule, pos, limit);
                        ++ruleCount;
                        if (mode == 2) {
                            // ::id in illegal position (because a rule
                            // occurred after the ::id footer block)
                            throw new IllegalArgumentException("::ID in illegal position");
                        }
                        mode = 1;
                    }
                } catch (IllegalArgumentException e) {
                    if (errorCount == 30) {
                        errors.append("\nMore than 30 errors; further messages squelched");
                        break main;
                    }
                    if (errors == null) {
                        errors = new StringBuffer(e.getMessage());
                    } else {
                        errors.append("\n" + e.getMessage());
                    }
                    ++errorCount;
                    pos = ruleEnd(rule, pos, limit) + 1; // +1 advances past ';'
                }
            }
        }

        idBlock = idBlockResult.toString();

        // Convert the set vector to an array
        data.variables = new UnicodeMatcher[variablesVector.size()];
        variablesVector.copyInto(data.variables);
        variablesVector = null;

        // Do more syntax checking and index the rules
        try {
            if (compoundFilter != null) {
                if ((direction == Transliterator.FORWARD &&
                     compoundFilterOffset != 0) ||
                    (direction == Transliterator.REVERSE &&
                     compoundFilterOffset != idBlock.length())) {
                    throw new IllegalArgumentException("Compound filters misplaced");
                }
            }

            data.ruleSet.freeze();

            if (idSplitPoint < 0) {
                idSplitPoint = idBlock.length();
            }

            if (ruleCount == 0) {
                data = null;
            }
        } catch (IllegalArgumentException e) {
            if (errors == null) {
                errors = new StringBuffer(e.getMessage());
            } else {
                errors.append("\n").append(e.getMessage());
            }
        }

        if (errors != null) {
            throw new IllegalArgumentException(errors.toString());
        }
    }

    /**
     * MAIN PARSER.  Parse the next rule in the given rule string, starting
     * at pos.  Return the index after the last character parsed.  Do not
     * parse characters at or after limit.
     *
     * Important:  The character at pos must be a non-whitespace character
     * that is not the comment character.
     *
     * This method handles quoting, escaping, and whitespace removal.  It
     * parses the end-of-rule character.  It recognizes context and cursor
     * indicators.  Once it does a lexical breakdown of the rule at pos, it
     * creates a rule object and adds it to our rule list.
     *
     * This method is tightly coupled to the inner class RuleHalf.
     */
    private int parseRule(String rule, int pos, int limit) {
        // Locate the left side, operator, and right side
        int start = pos;
        char operator = 0;

        RuleHalf left  = new RuleHalf();
        RuleHalf right = new RuleHalf();

        undefinedVariableName = null;
        pos = left.parse(rule, pos, limit, this);

        if (pos == limit ||
            OPERATORS.indexOf(operator = rule.charAt(pos++)) < 0) {
            syntaxError("No operator", rule, start);
        }

        // Found an operator char.  Check for forward-reverse operator.
        if (operator == REVERSE_RULE_OP &&
            (pos < limit && rule.charAt(pos) == FORWARD_RULE_OP)) {
            ++pos;
            operator = FWDREV_RULE_OP;
        }

        pos = right.parse(rule, pos, limit, this);

        if (pos < limit) {
            if (rule.charAt(pos) == END_OF_RULE) {
                ++pos;
            } else {
                // RuleHalf parser must have terminated at an operator
                syntaxError("Unquoted operator", rule, start);
            }
        }

        if (operator == VARIABLE_DEF_OP) {
            // LHS is the name.  RHS is a single character, either a literal
            // or a set (already parsed).  If RHS is longer than one
            // character, it is either a multi-character string, or multiple
            // sets, or a mixture of chars and sets -- syntax error.

            // We expect to see a single undefined variable (the one being
            // defined).
            if (undefinedVariableName == null) {
                syntaxError("Missing '$' or duplicate definition", rule, start);
            }
            if (left.text.length() != 1 || left.text.charAt(0) != variableLimit) {
                syntaxError("Malformed LHS", rule, start);
            }
            if (left.anchorStart || left.anchorEnd ||
                right.anchorStart || right.anchorEnd) {
                syntaxError("Malformed variable def", rule, start);
            }
            // We allow anything on the right, including an empty string.
            int n = right.text.length();
            char[] value = new char[n];
            right.text.getChars(0, n, value, 0);
            data.variableNames.put(undefinedVariableName, value);

            ++variableLimit;
            return pos;
        }

        // If this is not a variable definition rule, we shouldn't have
        // any undefined variable names.
        if (undefinedVariableName != null) {
            syntaxError("Undefined variable $" + undefinedVariableName,
                        rule, start);
        }

        // If the direction we want doesn't match the rule
        // direction, do nothing.
        if (operator != FWDREV_RULE_OP &&
            ((direction == Transliterator.FORWARD) != (operator == FORWARD_RULE_OP))) {
            return pos;
        }

        // Transform the rule into a forward rule by swapping the
        // sides if necessary.
        if (direction == Transliterator.REVERSE) {
            RuleHalf temp = left;
            left = right;
            right = temp;
        }

        // Remove non-applicable elements in forward-reverse
        // rules.  Bidirectional rules ignore elements that do not
        // apply.
        if (operator == FWDREV_RULE_OP) {
            right.removeContext();
            right.segments = null;
            left.cursor = left.maxRef = -1;
            left.cursorOffset = 0;
        }

        // Normalize context
        if (left.ante < 0) {
            left.ante = 0;
        }
        if (left.post < 0) {
            left.post = left.text.length();
        }

        // Context is only allowed on the input side.  Cursors are only
        // allowed on the output side.  Segment delimiters can only appear
        // on the left, and references on the right.  Cursor offset
        // cannot appear without an explicit cursor.  Cursor offset
        // cannot place the cursor outside the limits of the context.
        // Anchors are only allowed on the input side.
        if (right.ante >= 0 || right.post >= 0 || left.cursor >= 0 ||
            right.segments != null || left.maxRef >= 0 ||
            (right.cursorOffset != 0 && right.cursor < 0) ||
            // - The following two checks were used to ensure that the
            // - the cursor offset stayed within the ante- or postcontext.
            // - However, with the addition of quantifiers, we have to
            // - allow arbitrary cursor offsets and do runtime checking.
            //(right.cursorOffset > (left.text.length() - left.post)) ||
            //(-right.cursorOffset > left.ante) ||
            right.anchorStart || right.anchorEnd) {
            syntaxError("Malformed rule", rule, start);
        }

        // Check integrity of segments and segment references.  Each
        // segment's start must have a corresponding limit, and the
        // references must not refer to segments that do not exist.
        if (left.segments != null) {
            if (!left.segments.validate()) {
                syntaxError("Missing segment close", rule, start);
            }
            int n = left.segments.count();
            if (right.maxRef > n) {
                syntaxError("Undefined segment reference", rule, start);
            }
        }

        data.ruleSet.addRule(new TransliterationRule(
                                     left.text, left.ante, left.post,
                                     right.text, right.cursor, right.cursorOffset,
                                     left.createSegments(),
                                     left.anchorStart, left.anchorEnd,
                                     data));

        return pos;
    }

    /**
     * Throw an exception indicating a syntax error.  Search the rule string
     * for the probable end of the rule.  Of course, if the error is that
     * the end of rule marker is missing, then the rule end will not be found.
     * In any case the rule start will be correctly reported.
     * @param msg error description
     * @param rule pattern string
     * @param start position of first character of current rule
     */
    static final void syntaxError(String msg, String rule, int start) {
        int end = ruleEnd(rule, start, rule.length());
        throw new IllegalArgumentException(msg + " in \"" +
                                           Utility.escape(rule.substring(start, end)) + '"');
    }

    static final int ruleEnd(String rule, int start, int limit) {
        int end = quotedIndexOf(rule, start, limit, ";");
        if (end < 0) {
            end = limit;
        }
        return end;
    }

    /**
     * Parse a UnicodeSet out, store it, and return the stand-in character
     * used to represent it.
     */
    private final char parseSet(String rule, ParsePosition pos) {
        UnicodeSet set = new UnicodeSet(rule, pos, parseData);
        if (variableNext >= variableLimit) {
            throw new RuntimeException("Private use variables exhausted");
        }
        set.compact();
        return generateStandInFor(set);
    }

    /**
     * Generate and return a stand-in for a new UnicodeMatcher.  Store
     * the matcher.
     */
    char generateStandInFor(UnicodeMatcher matcher) {
        // assert(matcher != null);
        if (variableNext >= variableLimit) {
            throw new RuntimeException("Private use variables exhausted");
        }
        variablesVector.addElement(matcher);
        return variableNext++;
    }

    /**
     * Append the value of the given variable name to the given
     * StringBuffer.
     * @exception IllegalArgumentException if the name is unknown.
     */
    private void appendVariableDef(String name, StringBuffer buf) {
        char[] ch = (char[]) data.variableNames.get(name);
        if (ch == null) {
            // We allow one undefined variable so that variable definition
            // statements work.  For the first undefined variable we return
            // the special placeholder variableLimit-1, and save the variable
            // name.
            if (undefinedVariableName == null) {
                undefinedVariableName = name;
                if (variableNext >= variableLimit) {
                    throw new RuntimeException("Private use variables exhausted");
                }
                buf.append((char) --variableLimit);
            } else {
                throw new IllegalArgumentException("Undefined variable $"
                                                   + name);
            }
        } else {
            buf.append(ch);
        }
    }

    char getSegmentStandin(int r) {
        // assert(r>=1);
        if (r > data.segmentCount) {
            data.segmentCount = r;
            variableLimit = (char) (data.segmentBase - r + 1);
            if (variableNext >= variableLimit) {
                throw new IllegalArgumentException("Too many variables / segments");
            }
        }
        return data.getSegmentStandin(r);
    }

    /**
     * Determines what part of the private use region of Unicode we can use for
     * variable stand-ins.  The correct way to do this is as follows: Parse each
     * rule, and for forward and reverse rules, take the FROM expression, and
     * make a hash of all characters used.  The TO expression should be ignored.
     * When done, everything not in the hash is available for use.  In practice,
     * this method may employ some other algorithm for improved speed.
     */
    private final void determineVariableRange(RuleBody ruleArray) {
        // As an initial implementation, we just run through all the
        // characters, ignoring any quoting.  This works since the quote
        // mechanisms are outside the private use area.

        Range r = new Range('\uE000', 0x1900); // Private use area
        r = r.largestUnusedSubrange(ruleArray);

        if (r == null) {
            throw new RuntimeException(
                "No private use characters available for variables");
        }

        // Segment references work down; variables work up.  We don't
        // know how many of each we will need.
        data.segmentBase = (char) (r.start + r.length - 1);
        data.segmentCount = 0;
        data.variablesBase = variableNext = (char) r.start;
        variableLimit = (char) (r.start + r.length);

        if (variableNext >= variableLimit) {
            throw new RuntimeException(
                    "Too few private use characters available for variables");
        }
    }

    /**
     * Returns the index of the first character in a set, ignoring quoted text.
     * For example, in the string "abc'hide'h", the 'h' in "hide" will not be
     * found by a search for "h".  Unlike String.indexOf(), this method searches
     * not for a single character, but for any character of the string
     * <code>setOfChars</code>.
     * @param text text to be searched
     * @param start the beginning index, inclusive; <code>0 <= start
     * <= limit</code>.
     * @param limit the ending index, exclusive; <code>start <= limit
     * <= text.length()</code>.
     * @param setOfChars string with one or more distinct characters
     * @return Offset of the first character in <code>setOfChars</code>
     * found, or -1 if not found.
     * @see #indexOf
     */
    private static int quotedIndexOf(String text, int start, int limit,
                                     String setOfChars) {
        for (int i=start; i<limit; ++i) {
            char c = text.charAt(i);
            if (c == ESCAPE) {
                ++i;
            } else if (c == QUOTE) {
                while (++i < limit
                       && text.charAt(i) != QUOTE) {}
            } else if (setOfChars.indexOf(c) >= 0) {
                return i;
            }
        }
        return -1;
    }
}

//eof
