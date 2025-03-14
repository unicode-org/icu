// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class parses a {@code MessageFormat 2} syntax into a data model {@link MFDataModel.Message}.
 *
 * <p>It is used by {@link MessageFormatter}, but it might be handy for various tools.</p>
 *
 * @internal ICU 75 technology preview
 * @deprecated This API is for technology preview only.
 */
@Deprecated
public class MFParser {
    private static final int EOF = -1;
    private final InputSource input;

    MFParser(String text) {
        this.input = new InputSource(text);
    }

    /**
     * Parses a {@code MessageFormat 2} syntax into a {@link MFDataModel.Message}.
     *
     * <p>It is used by {@link MessageFormatter}, but it might be handy for various tools.</p>
     * @param input the text to parse
     * @return the parsed {@code MFDataModel.Message}
     * @throws MFParseException if errors are detected
     *
     * @internal ICU 75 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static MFDataModel.Message parse(String input) throws MFParseException {
        return new MFParser(input).parseImpl();
    }

    // Parser proper
    private MFDataModel.Message parseImpl() throws MFParseException {
        MFDataModel.Message result;
        // Determine if message is simple or complex; this requires
        // looking through whitespace.
        int savedPosition = input.getPosition();
        skipOptionalWhitespaces();
        int cp = input.peekChar();
        // abnf: message = simple-message / complex-message
        if (cp == '.') { // declarations or .match
            // No need to restore whitespace
            result = getComplexMessage();
        } else if (cp == '{') { // `{` or `{{`
            cp = input.readCodePoint();
            cp = input.peekChar();
            if (cp == '{') { // `{{`, complex body without declarations
                input.backup(1); // let complexBody deal with the wrapping {{ and }}
                // abnf: complex-message   = o *(declaration o) complex-body o
                MFDataModel.Pattern pattern = getQuotedPattern();
                skipOptionalWhitespaces();
                result = new MFDataModel.PatternMessage(new ArrayList<>(), pattern);
            } else { // placeholder
                // Restore whitespace if applicable
                input.gotoPosition(savedPosition);
                MFDataModel.Pattern pattern = getPattern();
                result = new MFDataModel.PatternMessage(new ArrayList<>(), pattern);
            }
        } else {
            // Restore whitespace if applicable
            input.gotoPosition(savedPosition);
            MFDataModel.Pattern pattern = getPattern();
            result = new MFDataModel.PatternMessage(new ArrayList<>(), pattern);
        }
        checkCondition(input.atEnd(), "Content detected after the end of the message.");
        new MFDataModelValidator(result).validate();
        return result;
    }

    // abnf: simple-message = o [simple-start pattern]
    // abnf: simple-start = simple-start-char / escaped-char / placeholder
    private MFDataModel.Pattern getPattern() throws MFParseException {
        MFDataModel.Pattern pattern = new MFDataModel.Pattern();
        while (true) {
            MFDataModel.PatternPart part = getPatternPart();
            if (part == null) {
                break;
            }
            pattern.parts.add(part);
        }
        // checkCondition(!pattern.parts.isEmpty(), "Empty pattern");
        return pattern;
    }

    // abnf: pattern = *(text-char / escaped-char / placeholder)
    private MFDataModel.PatternPart getPatternPart() throws MFParseException {
        int cp = input.peekChar();
        switch (cp) {
            case EOF:
                return null;
            case '}': // This is the end, otherwise it would be escaped
                return null;
            case '{':
                MFDataModel.Expression ph = getPlaceholder();
                return ph;
            default:
                String plainText = getText();
                MFDataModel.StringPart sp = new MFDataModel.StringPart(plainText);
                return sp;
        }
    }

    // abnf: text-char = content-char / ws / "." / "@" / "|"
    private String getText() {
        StringBuilder result = new StringBuilder();
        while (true) {
            int cp = input.readCodePoint();
            switch (cp) {
                case EOF:
                    return result.toString();
                case '\\':
                    // abnf: escaped-char = backslash ( backslash / "{" / "|" / "}" )
                    cp = input.readCodePoint();
                    if (cp == '\\' || cp == '{' || cp == '|' | cp == '}') {
                        result.appendCodePoint(cp);
                    } else { // TODO: Error, treat invalid escape?
                        result.appendCodePoint('\\');
                        result.appendCodePoint(cp);
                    }
                    break;
                case '.':
                case '@':
                case '|':
                    result.appendCodePoint(cp);
                    break;
                default:
                    if (StringUtils.isContentChar(cp) || StringUtils.isWhitespace(cp)) {
                        result.appendCodePoint(cp);
                    } else {
                        input.backup(1);
                        return result.toString();
                    }
            }
        }
    }

    // abnf: placeholder = expression / markup
    // abnf: expression = literal-expression
    // abnf:            / variable-expression
    // abnf:            / function-expression
    // abnf: literal-expression = "{" o literal [s function] *(s attribute) o "}"
    // abnf: variable-expression = "{" o variable [s function] *(s attribute) o "}"
    // abnf: function-expression = "{" o function *(s attribute) o "}"
    // abnf: markup = "{" o "#" identifier *(s option) *(s attribute) o ["/"] "}"  ; open and standalone
    // abnf:        / "{" o "/" identifier *(s option) *(s attribute) o "}"  ; close
    private MFDataModel.Expression getPlaceholder() throws MFParseException {
        int cp = input.peekChar();
        if (cp != '{') {
            return null;
        }
        input.readCodePoint(); // consume the '{'
        skipOptionalWhitespaces(); // the o after '{'
        cp = input.peekChar();

        MFDataModel.Expression result;
        if (cp == '#' || cp == '/') {
            result = getMarkup();
        } else if (cp == '$') {
            result = getVariableExpression();
        } else if (StringUtils.isFunctionSigil(cp)) {
            result = getFunctionExpression();
        } else {
            result = getLiteralExpression();
        }

        skipOptionalWhitespaces();
        cp = input.readCodePoint(); // consume the '}'
        checkCondition(cp == '}', "Unclosed placeholder");

        return result;
    }

    private MFDataModel.Function getFunction(boolean whitespaceRequired) throws MFParseException {
        int position = input.getPosition();

        // Handle absent function first (before parsing mandatory whitespace)
        int cp = input.peekChar();
        if (cp == '}') {
            return null;
        }

        int whitespaceCount = 0;
        if (whitespaceRequired) {
            whitespaceCount = skipRequiredWhitespaces();
        } else {
            whitespaceCount = skipOptionalWhitespaces();
        }

        cp = input.peekChar();
        switch (cp) {
            case '}': {
                // No function -- push the whitespace back,
                // in case it's the required whitespace before an attribute
                input.backup(whitespaceCount);
                return null;
            }
            case ':': // function
                // abnf: function = ":" identifier *(s option)
                input.readCodePoint(); // Consume the sigil
                String identifier = getIdentifier();
                checkCondition(identifier != null, "Function name missing");
                Map<String, MFDataModel.Option> options = getOptions();
                return new MFDataModel.Function(identifier, options);
            default:
                // OK to continue and return null, it is an error.
        }
        input.gotoPosition(position);
        return null;
    }

    private MFDataModel.Function getMarkupFunction() throws MFParseException {
        skipOptionalWhitespaces();

        int cp = input.peekChar();
        switch (cp) {
            case '}':
                return null;
            case '#':
            case '/':
                // abnf: markup = "{" o "#" identifier *(s option) *(s attribute) o ["/"] "}"  ; open and standalone
                // abnf:        / "{" o "/" identifier *(s option) *(s attribute) o "}"  ; close
                input.readCodePoint(); // Consume the sigil
                String identifier = getIdentifier();
                checkCondition(identifier != null, "Function name missing");
                Map<String, MFDataModel.Option> options = getOptions();
                return new MFDataModel.Function(identifier, options);
            default:
                // function or something else,
                return null;
        }
    }

    // abnf: literal-expression = "{" o literal [s function] *(s attribute) o "}"
    private MFDataModel.Expression getLiteralExpression() throws MFParseException {
        MFDataModel.Literal literal = getLiteral(false);
        checkCondition(literal != null, "Literal expression expected.");

        MFDataModel.Function function = null;
        boolean hasWhitespace = StringUtils.isWhitespace(input.peekChar());
        if (hasWhitespace) { // we might have an function
            function = getFunction(true);
            if (function == null) {
                // We had some spaces, but no function.
                // So we put (some) back for the possible attributes.
             //   input.backup(1);
            }
        }

        hasWhitespace = StringUtils.isWhitespace(input.peekChar());
        List<MFDataModel.Attribute> attributes = getAttributes();
        if (!hasWhitespace && !attributes.isEmpty()) {
            error("syntax-error: missing space before attributes");
        }

        // Literal without a function, for example {|hello|} or {123}
        return new MFDataModel.LiteralExpression(literal, function, attributes);
    }

    // abnf: variable-expression = "{" o variable [s function] *(s attribute) o "}"
    private MFDataModel.VariableExpression getVariableExpression() throws MFParseException {
        MFDataModel.VariableRef variableRef = getVariableRef();
        MFDataModel.Function function = getFunction(true);
        List<MFDataModel.Attribute> attributes = getAttributes();
        // Variable without a function, for example {$foo}
        return new MFDataModel.VariableExpression(variableRef, function, attributes);
    }

    // abnf: function-expression = "{" o function *(s attribute) o "}"
    private MFDataModel.Expression getFunctionExpression() throws MFParseException {
        MFDataModel.Function function = getFunction(false);
        List<MFDataModel.Attribute> attributes = getAttributes();

        if (function instanceof MFDataModel.Function) {
            return new MFDataModel.FunctionExpression(
                    (MFDataModel.Function) function, attributes);
        } else {
            error("Unexpected function : " + function);
        }
        return null;
    }

    // abnf: markup = "{" o "#" identifier *(s option) *(s attribute) o ["/"] "}"  ; open and standalone
    // abnf:        / "{" o "/" identifier *(s option) *(s attribute) o "}"  ; close
    private MFDataModel.Markup getMarkup() throws MFParseException {
        int cp = input.peekChar(); // consume the '{'
        checkCondition(cp == '#' || cp == '/', "Should not happen. Expecting a markup.");

        MFDataModel.Markup.Kind kind =
                cp == '/' ? MFDataModel.Markup.Kind.CLOSE : MFDataModel.Markup.Kind.OPEN;

        MFDataModel.Function function = getMarkupFunction();
        List<MFDataModel.Attribute> attributes = getAttributes();

        // Parse optional whitespace after attribute list
        skipOptionalWhitespaces(); // the o before '/}' or '}'

        cp = input.peekChar();
        if (cp == '/') {
            kind = MFDataModel.Markup.Kind.STANDALONE;
            input.readCodePoint();
        }

        if (function instanceof MFDataModel.Function) {
            MFDataModel.Function fa = (MFDataModel.Function) function;
            return new MFDataModel.Markup(kind, fa.name, fa.options, attributes);
        }

        return null;
    }

    private List<MFDataModel.Attribute> getAttributes() throws MFParseException {
        List<MFDataModel.Attribute> result = new ArrayList<>();
        while (true) {
            MFDataModel.Attribute attribute = getAttribute();
            if (attribute == null) {
                break;
            }
            result.add(attribute);
        }
        return result;
    }

    // abnf: attribute = "@" identifier [o "=" o literal]
    private MFDataModel.Attribute getAttribute() throws MFParseException {
        int position = input.getPosition();
        skipOptionalWhitespaces();
        int cp = input.peekChar();
        if (cp == '@') {
            input.readCodePoint(); // consume the '@'
            String id = getIdentifier();
            int wsCount = skipOptionalWhitespaces();
            cp = input.peekChar();
            MFDataModel.LiteralOrVariableRef literalOrVariable = null;
            if (cp == '=') {
                input.readCodePoint();
                skipOptionalWhitespaces();
                literalOrVariable = getLiteral(false);
                checkCondition(literalOrVariable != null, "Attributes must have a value after `=`");
            } else {
                // was not equal, attribute without a value, put the "spaces" back.
                input.backup(wsCount);
            }
            return new MFDataModel.Attribute(id, literalOrVariable);
        } else {
            input.gotoPosition(position);
        }
        return null;
    }

    // abnf: identifier = [namespace ":"] name
    // abnf: namespace = name
    private String getIdentifier() throws MFParseException {
        String namespace = getName();
        if (namespace == null) {
            return null;
        }
        int cp = input.readCodePoint();
        if (cp == ':') { // the previous name was namespace
            String name = getName();
            checkCondition(name != null, "Expected name after namespace '" + namespace + "'");
            return namespace + ":" + name;
        } else {
            input.backup(1);
        }
        return namespace;
    }

    // abnf helper: *(s option)
    private Map<String, MFDataModel.Option> getOptions() throws MFParseException {
        Map<String, MFDataModel.Option> options = new LinkedHashMap<>();
        boolean first = true;
        int skipCount = 0;
        while (true) {
            MFDataModel.Option option = getOption();
            if (option == null) {
                break;
            }
            checkCondition(first || skipCount != 0,
                           "Expected whitespace before option " + option.name);
            first = false;
            if (options.containsKey(option.name)) {
                error("Duplicated option '" + option.name + "'");
            }
            options.put(option.name, option);
            // Can't just call skipMandatoryWhitespaces() here, because it
            // might be the last option. So check for whitespace when
            // parsing the next option instead.
            skipCount = skipOptionalWhitespaces();
        }
        // Restore the last chunk of whitespace in case there's an attribute following
        input.backup(skipCount);
        return options;
    }

    // abnf: option = identifier o "=" o (literal / variable)
    private MFDataModel.Option getOption() throws MFParseException {
        int position = input.getPosition();
        skipOptionalWhitespaces();
        String identifier = getIdentifier();
        if (identifier == null) {
            input.gotoPosition(position);
            return null;
        }
        skipOptionalWhitespaces();
        int cp = input.readCodePoint();
        checkCondition(cp == '=', "Expected '='");
        skipOptionalWhitespaces();
        MFDataModel.LiteralOrVariableRef litOrVar = getLiteralOrVariableRef();
        if (litOrVar == null) {
            error("Options must have a value. An empty string should be quoted.");
        }
        return new MFDataModel.Option(identifier, litOrVar);
    }

    private MFDataModel.LiteralOrVariableRef getLiteralOrVariableRef() throws MFParseException {
        int cp = input.peekChar();
        if (cp == '$') {
            return getVariableRef();
        }
        return getLiteral(false);
    }

    // abnf: literal = quoted-literal / unquoted-literal
    private MFDataModel.Literal getLiteral(boolean normalize) throws MFParseException {
        int cp = input.readCodePoint();
        switch (cp) {
            case '|': // quoted-literal
                // abnf: quoted-literal = "|" *(quoted-char / escaped-char) "|"
                input.backup(1);
                MFDataModel.Literal ql = getQuotedLiteral(normalize);
                return ql;
            default: // unquoted-literal
                // abnf: unquoted-literal = name / number-literal
                input.backup(1);
                MFDataModel.Literal unql = getUnQuotedLiteral(normalize);
                return unql;
        }
    }

    private MFDataModel.VariableRef getVariableRef() throws MFParseException {
        int cp = input.readCodePoint();
        if (cp != '$') {
            checkCondition(cp == '$', "We can't get here");
        }

        // abnf: variable = "$" name
        String name = getName();
        checkCondition(name != null, "Invalid variable reference following $");
        return new MFDataModel.VariableRef(name);
    }

    private MFDataModel.Literal getQuotedLiteral(boolean normalize) throws MFParseException {
        StringBuilder result = new StringBuilder();
        int cp = input.readCodePoint();
        checkCondition(cp == '|', "expected starting '|'");
        while (true) {
            cp = input.readCodePoint();
            if (cp == EOF) {
                break;
            } else if (StringUtils.isQuotedChar(cp)) {
                result.appendCodePoint(cp);
            } else if (cp == '\\') {
                // abnf: escaped-char = backslash ( backslash / "{" / "|" / "}" )
                cp = input.readCodePoint();
                boolean isValidEscape = cp == '|' || cp == '\\' || cp == '{' || cp == '}';
                checkCondition(isValidEscape, "Invalid escape sequence inside quoted literal");
                result.appendCodePoint(cp);
            } else {
                break;
            }
        }

        checkCondition(cp == '|', "expected ending '|'");

        return new MFDataModel.Literal(normalize ? StringUtils.toNfc(result) : result.toString());
    }

    private MFDataModel.Literal getUnQuotedLiteral(boolean normalize) throws MFParseException {
        String name = getName();
        if (name != null) {
            return new MFDataModel.Literal(normalize ? StringUtils.toNfc(name) : name);
        }
        return getNumberLiteral();
    }

    // abnf: ; number-literal matches JSON number (https://www.rfc-editor.org/rfc/rfc8259#section-6)
    // abnf: number-literal = ["-"] (%x30 / (%x31-39 *DIGIT)) ["." 1*DIGIT] [%i"e" ["-" / "+"] 1*DIGIT]
    private static final Pattern RE_NUMBER_LITERAL =
            Pattern.compile("^-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+\\-]?[0-9]+)?");

    private MFDataModel.Literal getNumberLiteral() {
        String numberString = peekWithRegExp(RE_NUMBER_LITERAL);
        if (numberString != null) {
            return new MFDataModel.Literal(numberString);
        }
        return null;
    }

    /*
     * ; Required whitespace
     * abnf: s = *bidi ws o
     */
    private int skipRequiredWhitespaces() throws MFParseException {
        int position = input.getPosition();
        skipOptionalBidi();
        int count = skipWhitespaces();
        checkCondition(count > 0, "Space expected");
        skipOptionalWhitespaces();
        return count;
    }

    private int skipOptionalBidi() {
        int skipCount = 0;
        while (true) {
            int cp = input.peekChar();
            if (StringUtils.isBidi(cp)) {
                skipCount++;
                input.readCodePoint();
            } else {
                return skipCount;
            }
        }
    }

    /*
     * ; Optional whitespace
     * abnf: o = *(ws / bidi)
     */
    private int skipOptionalWhitespaces() {
        int skipCount = 0;
        while (true) {
            int cp = input.peekChar();
            if (StringUtils.isWhitespace(cp) || StringUtils.isBidi(cp)) {
                input.readCodePoint();
                skipCount++;
            } else {
                return skipCount;
            }
        }
    }

    // abnf: ws = SP / HTAB / CR / LF / %x3000
    private int skipWhitespaces() {
        int skipCount = 0;
        while (true) {
            int cp = input.peekChar();
            if (StringUtils.isWhitespace(cp)) {
                skipCount++;
                input.readCodePoint();
            } else {
                return skipCount;
            }
        }
    }

    private int skipOneOptionalBidi() {
        int c = input.peekChar();
        if (StringUtils.isBidi(c)) {
            // Consume it
            input.readCodePoint();
            return 1;
        }
        return 0;
    }

    // abnf: complex-message = o *(declaration o) complex-body o
    private MFDataModel.Message getComplexMessage() throws MFParseException {
        List<MFDataModel.Declaration> declarations = new ArrayList<>();
        boolean foundMatch = false;
        while (true) {
            MFDataModel.Declaration declaration = getDeclaration();
            if (declaration == null) {
                break;
            }
            if (declaration instanceof MatchDeclaration) {
                foundMatch = true;
                break;
            }
            declarations.add(declaration);
        }
        // abnf: complex-body = quoted-pattern / matcher
        if (foundMatch) {
            return getMatch(declarations);
        } else { // Expect {{...}} or end of message
            // abnf: complex-message   = o *(declaration o) complex-body o
            skipOptionalWhitespaces();
            int cp = input.peekChar();
            checkCondition(cp != EOF, "Expected a quoted pattern or .match; got end-of-input");
            MFDataModel.Pattern pattern = getQuotedPattern();
            skipOptionalWhitespaces(); // Trailing whitespace is allowed
            checkCondition(input.atEnd(), "Content detected after the end of the message.");
            return new MFDataModel.PatternMessage(declarations, pattern);
        }
    }

    // abnf: matcher = match-statement s variant *(o variant)
    // abnf: match-statement = match 1*(s selector)
    // abnf: selector = variable
    // abnf: variant = key *(s key) o quoted-pattern
    // abnf: key = literal / "*"
    // abnf: match = %s".match"
    private MFDataModel.SelectMessage getMatch(List<MFDataModel.Declaration> declarations)
            throws MFParseException {
        // ".match" was already consumed by the caller
        // Look for selectors
        List<MFDataModel.Expression> expressions = new ArrayList<>();
        while (true) {
            // Whitespace required between selectors but not required before first variant.
            skipRequiredWhitespaces();
            int cp = input.peekChar();
            if (cp != '$') {
                break;
            }
            MFDataModel.VariableRef variableRef = getVariableRef();
            if (variableRef == null) {
                break;
            }
            MFDataModel.Expression expression =
                    new MFDataModel.VariableExpression(variableRef, null, new ArrayList<>());
            expressions.add(expression);
        }

        checkCondition(!expressions.isEmpty(), "There should be at least one selector expression.");

        // At this point we need to look for variants, which are key - value
        List<MFDataModel.Variant> variants = new ArrayList<>();
        while (true) {
            MFDataModel.Variant variant = getVariant();
            if (variant == null) {
                break;
            }
            variants.add(variant);
        }
        checkCondition(input.atEnd(), "Content detected after the end of the message.");
        return new MFDataModel.SelectMessage(declarations, expressions, variants);
    }

    // abnf: variant = key *(s key) o quoted-pattern
    // abnf: key = literal / "*"
    private MFDataModel.Variant getVariant() throws MFParseException {
        List<MFDataModel.LiteralOrCatchallKey> keys = new ArrayList<>();
        while (true) {
            // Space is required between keys
            MFDataModel.LiteralOrCatchallKey key = getKey(!keys.isEmpty());
            if (key == null) {
                break;
            }
            keys.add(key);
        }
        // Trailing whitespace is allowed after the message
        skipOptionalWhitespaces();
        if (input.atEnd()) {
            checkCondition(
                    keys.isEmpty(), "After selector keys it is mandatory to have a pattern.");
            return null;
        }
        MFDataModel.Pattern pattern = getQuotedPattern();
        return new MFDataModel.Variant(keys, pattern);
    }

    private MFDataModel.LiteralOrCatchallKey getKey(boolean requireSpaces) throws MFParseException {
        int cp = input.peekChar();
        // Whitespace not required between last key and pattern:
        // variant = key *(s key) [s] quoted-pattern
        if (cp == '{') {
            return null;
        }
        int skipCount = 0;
        if (requireSpaces) {
            skipCount = skipRequiredWhitespaces();
        } else {
            skipCount = skipOptionalWhitespaces();
        }
        cp = input.peekChar();
        if (cp == '*') {
            input.readCodePoint(); // consume the '*'
            return new MFDataModel.CatchallKey();
        }
        if (cp == EOF) {
            // Restore whitespace, in order to detect the error case of whitespace at the end of a message
            input.backup(skipCount);
            return null;
        }
        return getLiteral(true);
    }

    private static class MatchDeclaration implements MFDataModel.Declaration {
        // Provides a common type that extends MFDataModel.Declaration but for match.
        // There is no such thing in the data model.
    }

    // abnf: declaration = input-declaration / local-declaration
    // abnf: input-declaration = input o variable-expression
    // abnf: local-declaration = local s variable o "=" o expression
    private MFDataModel.Declaration getDeclaration() throws MFParseException {
        int position = input.getPosition();
        skipOptionalWhitespaces();
        int cp = input.readCodePoint();
        if (cp != '.') {
            input.gotoPosition(position);
            return null;
        }
        String declName = getName();
        checkCondition(declName != null, "Expected a declaration after the '.'");

        MFDataModel.Expression expression;
        switch (declName) {
            case "input":
                // abnf: input = %s".input"
                skipOptionalWhitespaces();
                expression = getPlaceholder();
                String inputVarName = null;
                checkCondition(expression instanceof MFDataModel.VariableExpression,
                               "Variable expression required in .input declaration");
                inputVarName = ((MFDataModel.VariableExpression) expression).arg.name;
                return new MFDataModel.InputDeclaration(inputVarName,
                                                        (MFDataModel.VariableExpression) expression);
            case "local":
                // abnf: local = %s".local"
                // abnf: local-declaration = local s variable o "=" o expression
                skipRequiredWhitespaces();
                MFDataModel.LiteralOrVariableRef varName = getVariableRef();
                skipOptionalWhitespaces();
                cp = input.readCodePoint();
                checkCondition(cp == '=', declName);
                skipOptionalWhitespaces();
                expression = getPlaceholder();
                if (varName instanceof MFDataModel.VariableRef) {
                    return new MFDataModel.LocalDeclaration(
                            ((MFDataModel.VariableRef) varName).name, expression);
                }
                break;
            case "match":
                return new MatchDeclaration();
            default:
               // OK to continue and return null, it is an error.
        }
        return null;
    }

    // abnf: quoted-pattern = "{{" pattern "}}"
    private MFDataModel.Pattern getQuotedPattern() throws MFParseException {
        // abnf: quoted-pattern = "{{" pattern "}}"
        int cp = input.readCodePoint();
        checkCondition(cp == '{', "Expected { for a complex body");
        cp = input.readCodePoint();
        checkCondition(cp == '{', "Expected second { for a complex body");
        MFDataModel.Pattern pattern = getPattern();
        cp = input.readCodePoint();
        checkCondition(cp == '}', "Expected } to end a complex body");
        cp = input.readCodePoint();
        checkCondition(cp == '}', "Expected second } to end a complex body");
        return pattern;
    }

    // abnf: name = [bidi] name-start *name-char [bidi]
    private String getName() throws MFParseException {
        int savedPosition = input.getPosition();
        StringBuilder result = new StringBuilder();
        skipOneOptionalBidi();
        int cp = input.readCodePoint();
        checkCondition(cp != EOF, "Expected name or namespace.");
        if (!StringUtils.isNameStart(cp)) {
            input.gotoPosition(savedPosition);
            return null;
        }
        result.appendCodePoint(cp);
        while (true) {
            cp = input.readCodePoint();
            if (StringUtils.isNameChar(cp)) {
                result.appendCodePoint(cp);
            } else if (cp == EOF) {
                break;
            } else {
                input.backup(1);
                break;
            }
        }
        skipOneOptionalBidi();
        return StringUtils.toNfc(result.toString());
    }

    private void checkCondition(boolean condition, String message) throws MFParseException {
        if (!condition) {
            error(message);
        }
    }

    private void error(String message) throws MFParseException {
        StringBuilder finalMsg = new StringBuilder();
        if (input == null) {
            finalMsg.append("Parse error: ");
            finalMsg.append(message);
        } else {
            int position = input.getPosition();
            finalMsg.append("Parse error [" + input.getPosition() + "]: ");
            finalMsg.append(message);
            finalMsg.append("\n");
            if (position != EOF) {
                finalMsg.append(input.buffer.substring(0, position));
                finalMsg.append("^^^");
                finalMsg.append(input.buffer.substring(position));
            } else {
                finalMsg.append(input.buffer);
                finalMsg.append("^^^");
            }
        }
        throw new MFParseException(finalMsg.toString(), input.getPosition());
    }

    private String peekWithRegExp(Pattern pattern) {
        StringView sv = new StringView(input.buffer, input.getPosition());
        Matcher m = pattern.matcher(sv);
        if (m.find()) {
            input.skip(m.group().length());
            return m.group();
        }
        return null;
    }
}
