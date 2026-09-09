// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.text;

import com.ibm.icu.impl.PatternProps;
import com.ibm.icu.impl.RuleCharacterIterator;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet.XSymbolTable;
import com.ibm.icu.util.VersionInfo;
import java.text.ParsePosition;
import java.util.Locale;

class UnicodeSetLexer {
    UnicodeSetLexer(
            final String pattern,
            final ParsePosition parsePosition,
            RuleCharacterIterator chars,
            int unicodeSetOptions,
            final SymbolTable symbols) {
        pattern_ = pattern;
        parsePosition_ = parsePosition;
        chars_ = chars;
        unicodeSetOptions_ = unicodeSetOptions;
        charsOptions_ =
                RuleCharacterIterator.PARSE_ESCAPES
                        | ((unicodeSetOptions & UnicodeSet.IGNORE_SPACE) != 0
                                ? RuleCharacterIterator.SKIP_WHITESPACE
                                : 0);
        symbols_ = symbols;
        if (symbols instanceof XSymbolTable) {
            xsymbols_ = (XSymbolTable) symbols;
        } else {
            xsymbols_ = null;
        }
    }

    IllegalArgumentException syntaxError(String expected, String actual) {
        return new IllegalArgumentException(
                "Expected " + expected + ", got " + actual + " " + getPositionForDebugging());
    }

    private IllegalArgumentException lexicalError(String message) {
        return new IllegalArgumentException(message + " " + getPositionForDebugging(BEHIND));
    }

    static class LexicalElement {
        boolean isSetOperator(char op) {
            return category_ == Category.SET_OPERATOR && string_.charAt(0) == op;
        }

        boolean isStringLiteral() {
            return category_ == Category.STRING_LITERAL;
        }

        boolean isNamedElement() {
            return category_ == Category.NAMED_ELEMENT;
        }

        boolean isBracketedElement() {
            return category_ == Category.BRACKETED_ELEMENT;
        }

        String element() {
            if (category_ == Category.LITERAL_ELEMENT
                    || category_ == Category.ESCAPED_ELEMENT
                    || category_ == Category.BRACKETED_ELEMENT
                    || category_ == Category.STRING_LITERAL) {
                return string_;
            }
            return null;
        }

        Integer codePoint() {
            if (category_ == Category.LITERAL_ELEMENT
                    || category_ == Category.ESCAPED_ELEMENT
                    || category_ == Category.BRACKETED_ELEMENT
                    || category_ == Category.NAMED_ELEMENT) {
                return string_.codePointAt(0);
            }
            return null;
        }

        // If `this` is a valid property-query or set-valued-variable, returns the set represented
        // by this lexical element.  Null otherwise.
        UnicodeSet set() {
            if (category_ == Category.PROPERTY_QUERY || category_ == Category.VARIABLE) {
                return set_;
            }
            return null;
        }

        String debugString() {
            return category_.toString() + " '" + sourceText_ + "'";
        }

        // See https://unicode.org/reports/tr61#Lexical-Elements.
        enum Category {
            SET_OPERATOR,
            LITERAL_ELEMENT,
            ESCAPED_ELEMENT,
            NAMED_ELEMENT,
            BRACKETED_ELEMENT,
            STRING_LITERAL,
            PROPERTY_QUERY,
            // Used for ill-formed variables and set-valued variables that are not directly a
            // property-query, e.g., $basicLatinLetters=[A-Za-z].  Variables that expand to a single
            // lexical element instead have the category of that lexical element, e.g., $Ll=\p{Ll}
            // has
            // the category PROPERTY_QUERY, $a=a has the category LITERAL_ELEMENT, and
            // $s={Zeichenkette}
            // has the category STRING_LITERAL.
            VARIABLE,
            END_OF_TEXT;

            @Override
            public String toString() {
                return this == END_OF_TEXT
                        ? "(end of text)"
                        : super.toString().toLowerCase(Locale.ROOT).replace('_', '-');
            }
        }

        // Note that contrary to the C++, we do not have a `precomputedSet` (outliving this object)
        // separate from the `set` (owned by this `LexicalElement`), since we have garbage
        // collection.
        LexicalElement(
                Category category,
                String string,
                RuleCharacterIterator.Position after,
                UnicodeSet set,
                CharSequence sourceText,
                boolean separated,
                LexicalElement preceding,
                UnicodeSetLexer lexer) {
            category_ = category;
            string_ = string;
            after_ = after;
            set_ = set;
            sourceText_ = sourceText;
            if (separated || preceding == null) {
                return;
            }
            if (preceding.isSetOperator('[')
                    && category == Category.LITERAL_ELEMENT
                    && codePoint() == ':') {
                // Not in the C++ version since we do not have lexer error messages there:
                // set the position between the two unseparated lexical elements so the pointing
                // hand shows up where a space is needed.
                lexer.chars_.setPos(preceding.after_);
                throw lexer.lexicalError(
                        "white-space other than ignorable-format-control is required between"
                                + " set-operator [ and literal-element :");
            } else if (preceding.category_ == Category.ESCAPED_ELEMENT) {
                if (UCharacter.isHighSurrogate(preceding.codePoint())
                        && category_ == Category.ESCAPED_ELEMENT
                        && UCharacter.isLowSurrogate(codePoint())) {
                    lexer.chars_.setPos(preceding.after_);
                    throw lexer.lexicalError(
                            "white-space other than ignorable-format-control is required between"
                                    + " escaped-elements representing high and low surrogates");
                } else if (preceding.sourceText_.length() <= 3
                        && category == Category.LITERAL_ELEMENT) {
                    int cp = codePoint();
                    if (preceding.sourceText_.charAt(1) == 'x') {
                        if ((cp >= '0' && cp <= '9')
                                || (cp >= 'A' && cp <= 'F')
                                || (cp >= 'a' && cp <= 'f')) {
                            lexer.chars_.setPos(preceding.after_);
                            throw lexer.lexicalError(
                                    "white-space other than ignorable-format-control is required"
                                            + " between escaped-element "
                                            + preceding.sourceText_
                                            + " and literal-element "
                                            + sourceText_);
                        }
                    } else if (preceding.sourceText_.charAt(1) >= '0'
                            && preceding.sourceText_.charAt(1) <= '7') {
                        if (cp >= '0' && cp <= '7') {
                            lexer.chars_.setPos(preceding.after_);
                            throw lexer.lexicalError(
                                    "white-space other than ignorable-format-control is required"
                                            + " between escaped-element "
                                            + preceding.sourceText_
                                            + " and literal-element "
                                            + sourceText_);
                        }
                    }
                }
            }
        }

        Category category_;
        String string_;
        RuleCharacterIterator.Position after_;
        UnicodeSet set_;
        CharSequence sourceText_;
    }

    private String AHEAD = "☞";
    private String BEHIND = "☜";

    // Used by error messages produced by the parser; these are based on unexpected lookaheads, and
    // therefore always pertain to issues ahead.
    private String getPositionForDebugging() {
        return getPositionForDebugging(AHEAD);
    }

    // Also used in error messages produced by the lexer, which pertain to text behind: we scan and
    // then find out that the lexical element was ill-formed.
    private String getPositionForDebugging(String marker) {
        return pattern_.subSequence(0, parsePosition_.getIndex())
                + marker
                + pattern_.subSequence(
                        parsePosition_.getIndex(),
                        Math.min(pattern_.length(), parsePosition_.getIndex() + 60));
    }

    boolean acceptSetOperator(char op) {
        if (lookahead().isSetOperator(op)) {
            advance();
            return true;
        }
        return false;
    }

    LexicalElement lookahead() {
        if (ahead_ == null) {
            RuleCharacterIterator.Position before = getPos();
            ahead_ = nextToken(behind_);
            chars_.setPos(before);
        }
        return ahead_;
    }

    LexicalElement lookahead2() {
        if (ahead2_ == null) {
            // Note that if someone has called `getCharacterIterator` and played with the result,
            // `before` may not actually be before `ahead_`, but we do not actually depend on this
            // here, since we start from ahead_.after_.
            RuleCharacterIterator.Position before = getPos();
            chars_.setPos(lookahead().after_);
            ahead2_ = nextToken(ahead_);
            chars_.setPos(before);
        }
        return ahead2_;
    }

    // For use in older functions that take the `RuleCharacterIterator` directly.
    // Any advancement of the resulting `RuleCharacterIterator` has no effect on the result of
    // subsequentcalls to `lookahead`, `lookahead2`, `advance`, or `acceptSetOperator`.
    // Once `advance` or `acceptSetOperator` has been called, the result of a call to
    // `getCharacterIterator` preceding the call to `advance` or `acceptSetOperator` must no longer
    // be used.
    RuleCharacterIterator getCharacterIterator() {
        // Make sure we compute a correct `ahead_.after_` so we do not depend on the current value
        // of `getPos()` for lexing.
        lookahead();
        return chars_;
    }

    boolean atEnd() {
        return chars_.atEnd();
    }

    void advance() {
        // If someone called `getCharacterIterator`, we are now changing the character iterator
        // under their feet; further, we may not have an `ahead_`, so if they keep playing with it
        // we would be working on incorrect values of `getPos`.  This is why the result of
        // `getCharacterIterator` must no longer be used.
        chars_.setPos(lookahead().after_);
        behind_ = ahead_;
        ahead_ = ahead2_;
        ahead2_ = null;
    }

    // A version of getPos that returns its position instead of taking it as at out parameter.
    private RuleCharacterIterator.Position getPos() {
        final var result = new RuleCharacterIterator.Position();
        chars_.getPos(result);
        return result;
    }

    private LexicalElement nextToken(LexicalElement preceding) {
        boolean separated = false;
        if ((charsOptions_ & RuleCharacterIterator.SKIP_WHITESPACE) != 0) {
            int s;
            RuleCharacterIterator.Position pos = new RuleCharacterIterator.Position();
            for (; ; ) {
                chars_.getPos(pos);
                s =
                        chars_.next(
                                charsOptions_
                                        & ~(RuleCharacterIterator.PARSE_ESCAPES
                                                | RuleCharacterIterator.SKIP_WHITESPACE));
                if (!PatternProps.isWhiteSpace(s)) {
                    chars_.setPos(pos);
                    break;
                }
                if (s != '\u200E' && s != '\u200F') {
                    separated = true;
                }
            }
        }
        chars_.skipIgnored(charsOptions_);
        if (chars_.atEnd()) {
            return new LexicalElement(
                    LexicalElement.Category.END_OF_TEXT,
                    null,
                    getPos(),
                    /* set= */ null,
                    "",
                    separated,
                    preceding,
                    this);
        }
        final int start = parsePosition_.getIndex();
        final RuleCharacterIterator.Position before = getPos();
        if (xsymbols_ != null) {
            // Whereas a SymbolTable only defines the lexing of variables past the $, an
            // XSymbolTable defines the complete lexing of variables, and may subtract from other
            // lexical elements in the process.
            // Note that we do not check that the XSymbolTable is not redefining operators; it is up
            // to the implementer of an XSymbolTable to ensure that their lexing results in a
            // conformant extension of UnicodeSet syntax.
            final var nameEnd = new ParsePosition(parsePosition_.getIndex());
            final var name = xsymbols_.scanVariable(pattern_, nameEnd, pattern_.length());
            if (name != null) {
                return lookupVariable(name, start, start, nameEnd);
            }
        }
        // First try to get the next character without parsing escapes.
        final int first = chars_.next(charsOptions_ & ~RuleCharacterIterator.PARSE_ESCAPES);
        if (first == '[' || first == '\\') {
            final RuleCharacterIterator.Position afterFirst = getPos();
            // This could be a property-query or named-element.
            final int second =
                    chars_.next(
                            charsOptions_
                                    & ~(RuleCharacterIterator.PARSE_ESCAPES
                                            | RuleCharacterIterator.SKIP_WHITESPACE));
            if ((first == '[' && second == ':')
                    || (first == '\\' && (second == 'p' || second == 'P' || second == 'N'))) {
                if (second == 'N') {
                    final int queryResult = scanNamedElementBrackets();
                    return new LexicalElement(
                            LexicalElement.Category.NAMED_ELEMENT,
                            Character.toString(queryResult),
                            getPos(),
                            /* set= */ null,
                            pattern_.subSequence(start, parsePosition_.getIndex()),
                            separated,
                            preceding,
                            this);
                } else {
                    UnicodeSet queryResult = scanPropertyQueryAfterStart(first, second, start);
                    return new LexicalElement(
                            LexicalElement.Category.PROPERTY_QUERY,
                            null,
                            getPos(),
                            /* set= */ queryResult,
                            pattern_.subSequence(start, parsePosition_.getIndex()),
                            separated,
                            preceding,
                            this);
                }
            }
            // Not a property-query.
            chars_.setPos(afterFirst);
        }
        if (first == '$' && symbols_ != null && xsymbols_ == null) {
            final var nameEnd = new ParsePosition(parsePosition_.getIndex());
            // The SymbolTable defines the lexing of variable names past the $.
            final String name = symbols_.parseReference(pattern_, nameEnd, pattern_.length());
            if (name != null) {
                return lookupVariable(name, start, start + 1, nameEnd);
            }
        }
        switch (first) {
            case '[':
                return new LexicalElement(
                        LexicalElement.Category.SET_OPERATOR,
                        "[",
                        getPos(),
                        /* set= */ null,
                        pattern_.subSequence(start, parsePosition_.getIndex()),
                        separated,
                        preceding,
                        this);
            case '\\':
                {
                    // Now try to parse the escape.
                    chars_.setPos(before);
                    int codePoint = chars_.next(charsOptions_);
                    return new LexicalElement(
                            LexicalElement.Category.ESCAPED_ELEMENT,
                            Character.toString(codePoint),
                            getPos(),
                            /* set= */ null,
                            pattern_.subSequence(start, parsePosition_.getIndex()),
                            separated,
                            preceding,
                            this);
                }
            case '&':
            case '-':
            case ']':
            case '^':
            case '$':
                // We make $ a set-operator to handle the ICU extensions involving $.
                return new LexicalElement(
                        LexicalElement.Category.SET_OPERATOR,
                        Character.toString(first),
                        getPos(),
                        /* set= */ null,
                        pattern_.subSequence(start, parsePosition_.getIndex()),
                        separated,
                        preceding,
                        this);
            case '{':
                {
                    final var string = new StringBuilder();
                    boolean escaped;
                    int next;
                    int codePointCount = 0;
                    while (!chars_.atEnd()) {
                        final RuleCharacterIterator.Position beforeNext = getPos();
                        next =
                                chars_.next(
                                        charsOptions_
                                                & ~(RuleCharacterIterator.PARSE_ESCAPES
                                                        | RuleCharacterIterator.SKIP_WHITESPACE));
                        if (next == '\\') {
                            final int afterBackslash =
                                    chars_.next(
                                            charsOptions_
                                                    & ~(RuleCharacterIterator.PARSE_ESCAPES
                                                            | RuleCharacterIterator
                                                                    .SKIP_WHITESPACE));
                            if (afterBackslash == 'N') {
                                next = scanNamedElementBrackets();
                                escaped = true;
                            } else if (afterBackslash == 'p' || afterBackslash == 'P') {
                                throw lexicalError(
                                        "Invalid escape sequence \\"
                                                + Character.toString(afterBackslash)
                                                + " in UnicodeSet string");
                            } else {
                                chars_.setPos(beforeNext);
                                // Parse the escape.
                                next = chars_.next(charsOptions_);
                                escaped = chars_.isEscaped();
                            }
                        } else {
                            if (VersionInfo.ICU_VERSION.getMajor() < 81) {
                                if (PatternProps.isWhiteSpace(next)) {
                                    // Transitional prohibition of unescaped spaces in string
                                    // literals (in ICU 78 and earlier, these were ignored; in
                                    // ICU 81 they will mean themselves).
                                    throw lexicalError(
                                            "Unescaped Pattern_White_Space in UnicodeSet string"
                                                    + " literals is prohibited until ICU 81. "
                                                    + " Escape U+"
                                                    + Utility.hex(next)
                                                    + ".");
                                }
                            } else {
                                throw new IllegalArgumentException(
                                        "Remove this transitional check, see ICU-23307"
                                                + " and ICU-TC minutes of 2026-01-16.");
                            }
                            escaped = false;
                        }
                        if (!escaped && next == '}') {
                            return new LexicalElement(
                                    codePointCount == 1
                                            ? LexicalElement.Category.BRACKETED_ELEMENT
                                            : LexicalElement.Category.STRING_LITERAL,
                                    string.toString(),
                                    getPos(),
                                    /* set= */ null,
                                    pattern_.subSequence(start, parsePosition_.getIndex()),
                                    separated,
                                    preceding,
                                    this);
                        }
                        string.append(Character.toString(next));
                        codePointCount += 1;
                    }
                    throw lexicalError(
                            "String literal was not terminated: "
                                    + pattern_.subSequence(start, parsePosition_.getIndex()));
                }
            default:
                return new LexicalElement(
                        LexicalElement.Category.LITERAL_ELEMENT,
                        Character.toString(first),
                        getPos(),
                        /* set= */ null,
                        pattern_.subSequence(start, parsePosition_.getIndex()),
                        separated,
                        preceding,
                        this);
        }
    }

    private LexicalElement lookupVariable(
            String name, int lexicalElementStart, int nameStart, ParsePosition nameEnd) {
        // LexicalElements returned from this function and from evaluateVariable are always
        // constructed with separated=true, preceding=null: the separation constraints are lexical,
        // and variables are their own lexical elements; their values are not affected by lexical
        // constraints.
        chars_.jumpahead(nameEnd.getIndex() - nameStart);
        final var source = pattern_.subSequence(lexicalElementStart, parsePosition_.getIndex());
        UnicodeSet precomputedSet = symbols_.lookupSet(name);
        if (precomputedSet != null) {
            return new LexicalElement(
                    LexicalElement.Category.VARIABLE,
                    null,
                    getPos(),
                    precomputedSet,
                    source,
                    /* separated= */ true,
                    /* preceding= */ null,
                    this);
        }
        // The variable was not a precomputed set.  Use the old-fashioned `lookup`, which
        // should give us its source text; if that parses as a single set or element, use
        // it.  Note that variables are not allowed in that expansion.
        // Implementers of higher-level syntaxes that pre-parse UnicodeSet-valued variables
        // can use variables in their variable definitions, but those that simply use the
        // source text substitution API cannot.
        char[] expression = symbols_.lookup(name);
        if (expression == null) {
            throw lexicalError("Undefined variable " + name);
        }
        return evaluateVariable(new String(expression), source);
    }

    private int scanNamedElementBrackets() {
        int open =
                chars_.next(
                        charsOptions_
                                & ~(RuleCharacterIterator.PARSE_ESCAPES
                                        | RuleCharacterIterator.SKIP_WHITESPACE));
        if (open == '{') {
            int start = parsePosition_.getIndex();
            Integer hex = null;
            Integer literal = null;
            while (!chars_.atEnd()) {
                int last =
                        chars_.next(
                                charsOptions_
                                        & ~(RuleCharacterIterator.PARSE_ESCAPES
                                                | RuleCharacterIterator.SKIP_WHITESPACE));
                if (last == ':') {
                    if (hex == null) {
                        hex = 0;
                        if (start == parsePosition_.getIndex() - 1) {
                            throw lexicalError("Empty hexadecimal-digits in named-element");
                        }
                        for (int digit :
                                (Iterable<Integer>)
                                        pattern_.subSequence(start, parsePosition_.getIndex() - 1)
                                                        .chars()
                                                ::iterator) {
                            int nibble;
                            if (digit >= '0' && digit <= '9') {
                                nibble = digit - '0';
                            } else {
                                digit = digit & ~0x20;
                                if (digit >= 'A' && digit <= 'F') {
                                    nibble = digit - 'A' + 0xA;
                                } else {
                                    throw lexicalError(
                                            "Ill-formed hexadecimal-digits: "
                                                    + pattern_.subSequence(
                                                            start, parsePosition_.getIndex() - 1));
                                }
                            }
                            hex = (hex << 4) + nibble;
                            if (hex > 0x10FFFF) {
                                throw lexicalError(
                                        "hexadecimal-digits out of range: "
                                                + pattern_.subSequence(
                                                        start, parsePosition_.getIndex() - 1));
                            }
                        }
                    } else if (literal == null) {
                        final var literalCodePoints =
                                pattern_.subSequence(start, parsePosition_.getIndex() - 1)
                                        .codePoints()
                                        .toArray();
                        if (literalCodePoints.length != 1) {
                            throw lexicalError(
                                    "Ill-formed named-literal-element '"
                                            + pattern_.subSequence(
                                                    start, parsePosition_.getIndex() - 1)
                                            + "' ("
                                            + literalCodePoints.length
                                            + " code points)");
                        }
                        literal = literalCodePoints[0];
                    } else {
                        throw lexicalError("Too many colons in named-element");
                    }
                    start = parsePosition_.getIndex();
                } else if (last == '}') {
                    final var name = pattern_.substring(start, parsePosition_.getIndex() - 1);
                    // We should use the "Name" property of the XSymbolTable if present, so
                    // in that case we cannot directly call UCharacter.getCharFromExtendedName
                    // and we need to construct a set.
                    int result;
                    if (symbols_ != null && (symbols_ instanceof XSymbolTable)
                            || UnicodeSet.getDefaultXSymbolTable() != null) {
                        final var resultSet = new UnicodeSet();
                        resultSet.applyPropertyAlias("Name", name, symbols_);
                        result = resultSet.charAt(0);
                    } else {
                        result = UCharacter.getCharFromExtendedName(name);
                    }
                    if (result < 0
                            || (hex != null && result != hex)
                            || (literal != null && result != literal)) {
                        throw lexicalError(
                                "Ill-formed named-element: "
                                        + (result < 0
                                                ? name + " not found"
                                                : "inconsistent with "
                                                        + (hex != null
                                                                ? Utility.hex(hex) + ":"
                                                                : "")
                                                        + (literal != null
                                                                ? (Character.toString(literal)
                                                                        + ":")
                                                                : "")));
                    }
                    return result;
                }
            }
        }
        throw lexicalError("Ill-formed named-element");
    }

    private LexicalElement evaluateVariable(String expression, CharSequence source) {
        ParsePosition expressionPosition = new ParsePosition(0);
        final var expressionIterator =
                new RuleCharacterIterator(expression, symbols_, expressionPosition);
        // Do not pass the symbols: we do not support recursive expansion of variables.
        final var expressionLexer =
                new UnicodeSetLexer(
                        expression,
                        expressionPosition,
                        expressionIterator,
                        unicodeSetOptions_,
                        /* symbols= */ null);
        final var variableToken = expressionLexer.lookahead();
        if (variableToken.isSetOperator('[')) {
            final var rebuiltPattern = new StringBuilder();
            final var expressionValue = new UnicodeSet();
            try {
                expressionValue.parseUnicodeSet(
                        expressionLexer, rebuiltPattern, unicodeSetOptions_, /* depth= */ 0);
            } catch (IllegalArgumentException e) {
                throw lexicalError(
                        "The value of variable "
                                + source
                                + " failed to parse as a UnicodeSet ("
                                + e.getMessage()
                                + "). See usage of "
                                + source
                                + ":" /*lexcalError will show the context here.*/);
            }
            expressionValue.setPattern(rebuiltPattern.toString());
            if (!expressionLexer.atEnd()) {
                throw lexicalError(
                        "Variable "
                                + source
                                + " expands to '"
                                + expression
                                + "' which contains additional text beyond a UnicodeSet expression: "
                                + expression);
            }
            return new LexicalElement(
                    LexicalElement.Category.VARIABLE,
                    null,
                    getPos(),
                    /* set= */ expressionValue,
                    source,
                    /* separated= */ true,
                    /* preceding= */ null,
                    this);
        } else {
            expressionLexer.advance();
            if (!expressionLexer.atEnd()) {
                throw lexicalError(
                        "Variable "
                                + source
                                + " expands to '"
                                + expression
                                + "' which consists of several lexical elements that do not form a UnicodeSet expression");
            }
            switch (variableToken.category_) {
                case LITERAL_ELEMENT:
                case ESCAPED_ELEMENT:
                case NAMED_ELEMENT:
                case BRACKETED_ELEMENT:
                case STRING_LITERAL:
                case PROPERTY_QUERY:
                    // Return the same lexical element that we found while parsing the variable
                    // contents, except the source position corresponds to the position of the
                    // variable rather than 0 in its expansion, and the source is the name of the
                    // variable rather than its expansion.
                    return new LexicalElement(
                            variableToken.category_,
                            variableToken.string_,
                            getPos(),
                            variableToken.set_,
                            source,
                            /* separated= */ true,
                            /* preceding= */ null,
                            this);
                default:
                    throw lexicalError(
                            "Value of variable "
                                    + source
                                    + " expands to invalid lexical element of type "
                                    + variableToken.category_
                                    + " :"
                                    + expression);
            }
        }
    }

    private UnicodeSet scanPropertyQueryAfterStart(int first, int second, int queryStart) {
        Integer queryOperatorPosition = null;
        int queryExpressionStart = parsePosition_.getIndex();
        boolean exteriorlyNegated = false;
        boolean interiorlyNegated = false;
        // Do not skip whitespace so we can recognize unspaced :].  Lex escapes and
        // named-element: while ICU does not support string-valued properties and thus has no
        // use for escapes, we still want to lex through escapes to allow downstream
        // implementations (mostly unicodetools) to implement string-valued properties.
        int third =
                chars_.next(
                        charsOptions_
                                & ~(RuleCharacterIterator.PARSE_ESCAPES
                                        | RuleCharacterIterator.SKIP_WHITESPACE));
        if (first == '\\') {
            if (third != '{') {
                throw lexicalError("Missing { in property-query");
            }
            exteriorlyNegated = second == 'P';
            queryExpressionStart = parsePosition_.getIndex();
        } else {
            if (third == '^') {
                exteriorlyNegated = true;
                queryExpressionStart = parsePosition_.getIndex();
            }
        }
        RuleCharacterIterator.Position beforePenultimate = getPos();
        int penultimateUnescaped =
                chars_.next(
                        charsOptions_
                                & ~(RuleCharacterIterator.PARSE_ESCAPES
                                        | RuleCharacterIterator.SKIP_WHITESPACE));

        while (!chars_.atEnd()) {
            RuleCharacterIterator.Position beforeLast = getPos();
            int lastUnescaped =
                    chars_.next(
                            charsOptions_
                                    & ~(RuleCharacterIterator.PARSE_ESCAPES
                                            | RuleCharacterIterator.SKIP_WHITESPACE));
            if (penultimateUnescaped == '\\') {
                if (lastUnescaped == 'N') {
                    scanNamedElementBrackets();
                } else {
                    // There must be an escaped-element starting at beforePenultimate.  Go
                    // back there and advance through it.
                    chars_.setPos(beforePenultimate);
                    chars_.next(charsOptions_ & ~RuleCharacterIterator.SKIP_WHITESPACE);
                }
                // Neither a named-element nor an escaped-element can be part of a closing :].
                lastUnescaped = -1;
            } else if (queryOperatorPosition == null && lastUnescaped == '=') {
                queryOperatorPosition = parsePosition_.getIndex() - 1;
            } else if (queryOperatorPosition == null && lastUnescaped == '≠') {
                if (exteriorlyNegated) {
                    // Reject doubly negated property queries.
                    throw lexicalError("Found doubly negated property-query");
                }
                interiorlyNegated = true;
                queryOperatorPosition = parsePosition_.getIndex() - 1;
            } else if ((first == '[' && penultimateUnescaped == ':' && lastUnescaped == ']')
                    || (first == '\\' && lastUnescaped == '}')) {
                // Note that no unescaping is performed here, as ICU does not support string-valued
                // or miscellaneous properties.
                // The unicodetools use this implementation and support such properties; they should
                // do their own unescaping.
                int queryExpressionLimit =
                        first == '['
                                ? parsePosition_.getIndex() - 2
                                : parsePosition_.getIndex() - 1;
                // Note that in C++, we pass an empty, rather than null, propertyPredicate for
                // \p{X}.  In Java we distinguish those since ICU4J underlies the unicodetools,
                // which support properties for which \p{X=} is meaningful, e.g., \p{nfckcf=}:
                // https://util.unicode.org/UnicodeJsps/list-unicodeset.jsp?a=%5Cp%7BNFKC_Casefold%3D%7D
                // See also https://github.com/unicode-org/icu/pull/3456.
                String propertyPredicate = null;
                if (queryOperatorPosition != null) {
                    propertyPredicate =
                            pattern_.substring(queryOperatorPosition + 1, queryExpressionLimit);
                }
                final var result = new UnicodeSet();
                result.applyPropertyAlias(
                        pattern_.substring(
                                queryExpressionStart,
                                queryOperatorPosition != null
                                        ? queryOperatorPosition
                                        : queryExpressionLimit),
                        propertyPredicate,
                        symbols_);
                if (exteriorlyNegated != interiorlyNegated) {
                    result.complement().removeAllStrings();
                }
                result.setPattern(pattern_.substring(queryStart, parsePosition_.getIndex()));
                return result;
            }
            beforePenultimate = beforeLast;
            penultimateUnescaped = lastUnescaped;
        }
        throw lexicalError("property-query was not terminated");
    }

    final String pattern_;
    final ParsePosition parsePosition_;
    RuleCharacterIterator chars_;
    final int unicodeSetOptions_;
    final int charsOptions_;
    final SymbolTable symbols_;
    final XSymbolTable xsymbols_;
    LexicalElement behind_;
    LexicalElement ahead_;
    LexicalElement ahead2_;
}
