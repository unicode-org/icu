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
        if (cp == '.') { // declarations or .match
            // No need to restore whitespace
            result = getComplexMessage();
        } else if (cp == '{') { // `{` or `{{`
            cp = input.readCodePoint();
            cp = input.peekChar();
            if (cp == '{') { // `{{`, complex body without declarations
                input.backup(1); // let complexBody deal with the wrapping {{ and }}
                // abnf: complex-message   = [s] *(declaration [s]) complex-body [s]
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

    // abnf: simple-message = [simple-start pattern]
    // abnf: simple-start = simple-start-char / text-escape / placeholder
    // abnf: pattern = *(text-char / text-escape / placeholder)
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

    private String getText() {
        StringBuilder result = new StringBuilder();
        while (true) {
            int cp = input.readCodePoint();
            switch (cp) {
                case EOF:
                    return result.toString();
                case '\\':
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
    // abnf:            / annotation-expression
    // abnf: literal-expression = "{" [s] literal [s annotation] *(s attribute) [s] "}"
    // abnf: variable-expression = "{" [s] variable [s annotation] *(s attribute) [s] "}"
    // abnf: annotation-expression = "{" [s] annotation *(s attribute) [s] "}"
    // abnf: markup = "{" [s] "#" identifier *(s option) *(s attribute) [s] ["/"] "}" ; open and standalone
    // abnf:        / "{" [s] "/" identifier *(s option) *(s attribute) [s] "}" ; close
    private MFDataModel.Expression getPlaceholder() throws MFParseException {
        int cp = input.peekChar();
        if (cp != '{') {
            return null;
        }
        input.readCodePoint(); // consume the '{'
        skipOptionalWhitespaces();
        cp = input.peekChar();

        MFDataModel.Expression result;
        if (cp == '#' || cp == '/') {
            result = getMarkup();
        } else if (cp == '$') {
            result = getVariableExpression();
        } else if (StringUtils.isFunctionSigil(cp)) {
            result = getAnnotationExpression();
        } else {
            result = getLiteralExpression();
        }

        skipOptionalWhitespaces();
        cp = input.readCodePoint(); // consume the '}'
        checkCondition(cp == '}', "Unclosed placeholder");

        return result;
    }

    private MFDataModel.Annotation getAnnotation(boolean whitespaceRequired) throws MFParseException {
        int position = input.getPosition();

        // Handle absent annotation first (before parsing mandatory whitespace)
        int cp = input.peekChar();
        if (cp == '}') {
            return null;
        }

        int whitespaceCount = 0;
        if (whitespaceRequired) {
            whitespaceCount = skipMandatoryWhitespaces();
        } else {
            whitespaceCount = skipOptionalWhitespaces();
        }

        cp = input.peekChar();
        switch (cp) {
            case '}': {
                // No annotation -- push the whitespace back,
                // in case it's the required whitespace before an attribute
                input.backup(whitespaceCount);
                return null;
            }
            case ':': // annotation, function
                // abnf: function = ":" identifier *(s option)
                input.readCodePoint(); // Consume the sigil
                String identifier = getIdentifier();
                checkCondition(identifier != null, "Annotation / function name missing");
                Map<String, MFDataModel.Option> options = getOptions();
                return new MFDataModel.FunctionAnnotation(identifier, options);
            default:
                // OK to continue and return null, it is an error.
        }
        input.gotoPosition(position);
        return null;
    }

    private MFDataModel.Annotation getMarkupAnnotation() throws MFParseException {
        skipOptionalWhitespaces();

        int cp = input.peekChar();
        switch (cp) {
            case '}':
                return null;
            case '#':
            case '/':
                // abnf: markup = "{" [s] "#" identifier *(s option) *(s attribute) [s] ["/"] "}"  ; open and standalone
                // abnf:        / "{" [s] "/" identifier *(s option) *(s attribute) [s] "}"  ; close
                input.readCodePoint(); // Consume the sigil
                String identifier = getIdentifier();
                checkCondition(identifier != null, "Annotation / function name missing");
                Map<String, MFDataModel.Option> options = getOptions();
                return new MFDataModel.FunctionAnnotation(identifier, options);
            default:
                // function or something else,
                return null;
        }
    }

    // abnf: literal-expression = "{" [s] literal [s annotation] *(s attribute) [s] "}"
    private MFDataModel.Expression getLiteralExpression() throws MFParseException {
        MFDataModel.Literal literal = getLiteral();
        checkCondition(literal != null, "Literal expression expected.");

        MFDataModel.Annotation annotation = null;
        boolean hasWhitespace = StringUtils.isWhitespace(input.peekChar());
        if (hasWhitespace) { // we might have an annotation
            annotation = getAnnotation(true);
            if (annotation == null) {
                // We had some spaces, but no annotation.
                // So we put (some) back for the possible attributes.
             //   input.backup(1);
            }
        }

        List<MFDataModel.Attribute> attributes = getAttributes();

        // Literal without a function, for example {|hello|} or {123}
        return new MFDataModel.LiteralExpression(literal, annotation, attributes);
    }

    // abnf: variable-expression = "{" [s] variable [s annotation] *(s attribute) [s] "}"
    private MFDataModel.VariableExpression getVariableExpression() throws MFParseException {
        MFDataModel.VariableRef variableRef = getVariableRef();
        MFDataModel.Annotation annotation = getAnnotation(true);
        List<MFDataModel.Attribute> attributes = getAttributes();
        // Variable without a function, for example {$foo}
        return new MFDataModel.VariableExpression(variableRef, annotation, attributes);
    }

    // abnf: annotation-expression = "{" [s] annotation *(s attribute) [s] "}"
    private MFDataModel.Expression getAnnotationExpression() throws MFParseException {
        MFDataModel.Annotation annotation = getAnnotation(false);
        List<MFDataModel.Attribute> attributes = getAttributes();

        if (annotation instanceof MFDataModel.FunctionAnnotation) {
            return new MFDataModel.FunctionExpression(
                    (MFDataModel.FunctionAnnotation) annotation, attributes);
        } else {
            error("Unexpected annotation : " + annotation);
        }
        return null;
    }

    // abnf: markup = "{" [s] "#" identifier *(s option) *(s attribute) [s] ["/"] "}" ; open and standalone
    // abnf:        / "{" [s] "/" identifier *(s option) *(s attribute) [s] "}" ; close
    private MFDataModel.Markup getMarkup() throws MFParseException {
        int cp = input.peekChar(); // consume the '{'
        checkCondition(cp == '#' || cp == '/', "Should not happen. Expecting a markup.");

        MFDataModel.Markup.Kind kind =
                cp == '/' ? MFDataModel.Markup.Kind.CLOSE : MFDataModel.Markup.Kind.OPEN;

        MFDataModel.Annotation annotation = getMarkupAnnotation();
        List<MFDataModel.Attribute> attributes = getAttributes();

        // Parse optional whitespace after attribute list
        skipOptionalWhitespaces();

        cp = input.peekChar();
        if (cp == '/') {
            kind = MFDataModel.Markup.Kind.STANDALONE;
            input.readCodePoint();
        }

        if (annotation instanceof MFDataModel.FunctionAnnotation) {
            MFDataModel.FunctionAnnotation fa = (MFDataModel.FunctionAnnotation) annotation;
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

    // abnf: attribute = "@" identifier [[s] "=" [s] (literal / variable)]
    private MFDataModel.Attribute getAttribute() throws MFParseException {
        int position = input.getPosition();
        if (skipWhitespaces() == 0) {
            input.gotoPosition(position);
            return null;
        }
        int cp = input.peekChar();
        if (cp == '@') {
            input.readCodePoint(); // consume the '@'
            String id = getIdentifier();
            int wsCount = skipWhitespaces();
            cp = input.peekChar();
            MFDataModel.LiteralOrVariableRef literalOrVariable = null;
            if (cp == '=') {
                input.readCodePoint();
                skipOptionalWhitespaces();
                literalOrVariable = getLiteralOrVariableRef();
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
    // abnf: name = name-start *name-char
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
            // function = ":" identifier *(s option)
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

    // abnf: option = identifier [s] "=" [s] (literal / variable)
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
        return getLiteral();
    }

    // abnf: literal = quoted / unquoted
    private MFDataModel.Literal getLiteral() throws MFParseException {
        int cp = input.readCodePoint();
        switch (cp) {
            case '|': // quoted
                // abnf: quoted = "|" *(quoted-char / quoted-escape) "|"
                input.backup(1);
                MFDataModel.Literal ql = getQuotedLiteral();
                return ql;
            default: // unquoted
                input.backup(1);
                MFDataModel.Literal unql = getUnQuotedLiteral();
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

    private MFDataModel.Literal getQuotedLiteral() throws MFParseException {
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
                cp = input.readCodePoint();
                boolean isValidEscape = cp == '|' || cp == '\\' || cp == '{' || cp == '}';
                checkCondition(isValidEscape, "Invalid escape sequence inside quoted literal");
                result.appendCodePoint(cp);
            } else {
                break;
            }
        }

        checkCondition(cp == '|', "expected ending '|'");

        return new MFDataModel.Literal(result.toString());
    }

    private MFDataModel.Literal getUnQuotedLiteral() throws MFParseException {
        String name = getName();
        if (name != null) {
            return new MFDataModel.Literal(name);
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

    private int skipMandatoryWhitespaces() throws MFParseException {
        int count = skipWhitespaces();
        checkCondition(count > 0, "Space expected");
        return count;
    }

    private int skipOptionalWhitespaces() {
        return skipWhitespaces();
    }

    private int skipWhitespaces() {
        int skipCount = 0;
        while (true) {
            int cp = input.readCodePoint();
            if (cp == EOF) {
                return skipCount;
            }
            if (!StringUtils.isWhitespace(cp)) {
                input.backup(1);
                return skipCount;
            }
            skipCount++;
        }
    }

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
        if (foundMatch) {
            return getMatch(declarations);
        } else { // Expect {{...}} or end of message
            skipOptionalWhitespaces();
            int cp = input.peekChar();
            // abnf: complex-message   = [s] *(declaration [s]) complex-body [s]
            checkCondition(cp != EOF, "Expected a quoted pattern or .match; got end-of-input");
            MFDataModel.Pattern pattern = getQuotedPattern();
            skipOptionalWhitespaces(); // Trailing whitespace is allowed
            checkCondition(input.atEnd(), "Content detected after the end of the message.");
            return new MFDataModel.PatternMessage(declarations, pattern);
        }
    }

    // abnf: matcher = match-statement s variant *([s] variant)
    // abnf: match-statement = match 1*(s selector)
    // abnf: selector = variable
    // abnf: variant = key *(s key) [s] quoted-pattern
    // abnf: key = literal / "*"
    // abnf: match = %s".match"
    private MFDataModel.SelectMessage getMatch(List<MFDataModel.Declaration> declarations)
            throws MFParseException {
        // ".match" was already consumed by the caller
        // Look for selectors
        List<MFDataModel.Expression> expressions = new ArrayList<>();
        while (true) {
            // Whitespace required between selectors but not required before first variant.
            skipMandatoryWhitespaces();
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

    // abnf: variant = key *(s key) [s] quoted-pattern
    // abnf: key = literal / "*"
    private MFDataModel.Variant getVariant() throws MFParseException {
        List<MFDataModel.LiteralOrCatchallKey> keys = new ArrayList<>();
        // abnf: variant = key *(s key) [s] quoted-pattern
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
            skipCount = skipMandatoryWhitespaces();
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
        return getLiteral();
    }

    private static class MatchDeclaration implements MFDataModel.Declaration {
        // Provides a common type that extends MFDataModel.Declaration but for match.
        // There is no such thing in the data model.
    }

    // abnf: input-declaration = input [s] variable-expression
    // abnf: local-declaration = local s variable [s] "=" [s] expression
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
                skipOptionalWhitespaces();
                expression = getPlaceholder();
                String inputVarName = null;
                checkCondition(expression instanceof MFDataModel.VariableExpression,
                               "Variable expression required in .input declaration");
                inputVarName = ((MFDataModel.VariableExpression) expression).arg.name;
                return new MFDataModel.InputDeclaration(inputVarName,
                                                        (MFDataModel.VariableExpression) expression);
            case "local":
                // abnf: local-declaration = local s variable [s] "=" [s] expression
                skipMandatoryWhitespaces();
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

    // quoted-pattern = "{{" pattern "}}"
    private MFDataModel.Pattern getQuotedPattern() throws MFParseException { // {{ ... }}
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

    private String getName() throws MFParseException {
        StringBuilder result = new StringBuilder();
        int cp = input.readCodePoint();
        checkCondition(cp != EOF, "Expected name or namespace.");
        if (!StringUtils.isNameStart(cp)) {
            input.backup(1);
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
        return result.toString();
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
        boolean found = m.find();
        if (found) {
            input.skip(m.group().length());
            return m.group();
        }
        return null;
    }
}
