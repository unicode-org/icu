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
        int cp = input.peekChar();
        if (cp == '.') { // declarations or .match
            result = getComplexMessage();
        } else if (cp == '{') { // `{` or `{{`
            cp = input.readCodePoint();
            cp = input.peekChar();
            if (cp == '{') { // `{{`, complex body without declarations
                input.backup(1); // let complexBody deal with the wrapping {{ and }}
                MFDataModel.Pattern pattern = getQuotedPattern();
                result = new MFDataModel.PatternMessage(new ArrayList<>(), pattern);
            } else { // placeholder
                input.backup(1); // We want the '{' present, to detect the part as placeholder.
                MFDataModel.Pattern pattern = getPattern();
                result = new MFDataModel.PatternMessage(new ArrayList<>(), pattern);
            }
        } else {
            MFDataModel.Pattern pattern = getPattern();
            result = new MFDataModel.PatternMessage(new ArrayList<>(), pattern);
        }
        skipOptionalWhitespaces();
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
    // abnf: / variable-expression
    // abnf: / annotation-expression
    // abnf: literal-expression = "{" [s] literal [s annotation] *(s attribute) [s] "}"
    // abnf: variable-expression = "{" [s] variable [s annotation] *(s attribute) [s] "}"
    // abnf: annotation-expression = "{" [s] annotation *(s attribute) [s] "}"
    // abnf: markup = "{" [s] "#" identifier *(s option) *(s attribute) [s] ["/"] "}" ; open and standalone
    // abnf: / "{" [s] "/" identifier *(s option) *(s attribute) [s] "}" ; close
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
        } else if (StringUtils.isFunctionSigil(cp)
                || StringUtils.isPrivateAnnotationSigil(cp)
                || StringUtils.isReservedAnnotationSigil(cp)) {
            result = getAnnotationExpression();
        } else {
            result = getLiteralExpression();
        }

        skipOptionalWhitespaces();
        cp = input.readCodePoint(); // consume the '}'
        checkCondition(cp == '}', "Unclosed placeholder");

        return result;
    }

    private MFDataModel.Annotation getAnnotation() throws MFParseException {
        int position = input.getPosition();
        skipOptionalWhitespaces();

        int cp = input.peekChar();
        switch (cp) {
            case '}':
                return null;
            case ':': // annotation, function
                // abnf: function = ":" identifier *(s option)
                input.readCodePoint(); // Consume the sigil
                String identifier = getIdentifier();
                checkCondition(identifier != null, "Annotation / function name missing");
                Map<String, MFDataModel.Option> options = getOptions();
                return new MFDataModel.FunctionAnnotation(identifier, options);
            default: // reserved && private
                if (StringUtils.isReservedAnnotationSigil(cp)
                        || StringUtils.isPrivateAnnotationSigil(cp)) {
                    cp = input.readCodePoint();
                    // The sigil is part of the body.
                    // Safe to cast to char, the code point is in BMP
                    identifier = (char) cp + getIdentifier();
                    String body = getReservedBody();
                    return new MFDataModel.UnsupportedAnnotation(identifier + body);
                }
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
                // reserved, private, function, something else,
                return null;
        }
    }

    // abnf: literal-expression = "{" [s] literal [s annotation] *(s attribute) [s] "}"
    private MFDataModel.Expression getLiteralExpression() throws MFParseException {
        MFDataModel.Literal literal = getLiteral();
        checkCondition(literal != null, "Literal expression expected.");

        MFDataModel.Annotation annotation = null;
        int wsCount = skipWhitespaces();
        if (wsCount > 0) { // we might have an annotation
            annotation = getAnnotation();
            if (annotation == null) {
                // We had some spaces, but no annotation.
                // So we put (some) back for the possible attributes.
                input.backup(1);
            }
        }

        List<MFDataModel.Attribute> attributes = getAttributes();

        // Literal without a function, for example {|hello|} or {123}
        return new MFDataModel.LiteralExpression(literal, annotation, attributes);
    }

    // abnf: variable-expression = "{" [s] variable [s annotation] *(s attribute) [s] "}"
    private MFDataModel.VariableExpression getVariableExpression() throws MFParseException {
        MFDataModel.VariableRef variableRef = getVariableRef();
        MFDataModel.Annotation annotation = getAnnotation();
        List<MFDataModel.Attribute> attributes = getAttributes();
        // Variable without a function, for example {$foo}
        return new MFDataModel.VariableExpression(variableRef, annotation, attributes);
    }

    // abnf: annotation-expression = "{" [s] annotation *(s attribute) [s] "}"
    private MFDataModel.Expression getAnnotationExpression() throws MFParseException {
        MFDataModel.Annotation annotation = getAnnotation();
        List<MFDataModel.Attribute> attributes = getAttributes();

        if (annotation instanceof MFDataModel.FunctionAnnotation) {
            return new MFDataModel.FunctionExpression(
                    (MFDataModel.FunctionAnnotation) annotation, attributes);
        } else if (annotation instanceof MFDataModel.UnsupportedAnnotation) {
            return new MFDataModel.UnsupportedExpression(
                    (MFDataModel.UnsupportedAnnotation) annotation, attributes);
        } else {
            error("Unexpected annotation : " + annotation);
        }
        return null;
    }

    // abnf: markup = "{" [s] "#" identifier *(s option) *(s attribute) [s] ["/"] "}" ; open and standalone
    // abnf: / "{" [s] "/" identifier *(s option) *(s attribute) [s] "}" ; close
    private MFDataModel.Markup getMarkup() throws MFParseException {
        int cp = input.peekChar(); // consume the '{'
        checkCondition(cp == '#' || cp == '/', "Should not happen. Expecting a markup.");

        MFDataModel.Markup.Kind kind =
                cp == '/' ? MFDataModel.Markup.Kind.CLOSE : MFDataModel.Markup.Kind.OPEN;

        MFDataModel.Annotation annotation = getMarkupAnnotation();
        List<MFDataModel.Attribute> attributes = getAttributes();

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

    // abnf: reserved-body = *([s] 1*(reserved-char / reserved-escape / quoted))
    // abnf: reserved-escape = backslash ( backslash / "{" / "|" / "}" )
    private String getReservedBody() throws MFParseException {
        int spaceCount = skipWhitespaces();
        StringBuilder result = new StringBuilder();
        while (true) {
            int cp = input.readCodePoint();
            if (StringUtils.isReservedChar(cp)) {
                result.appendCodePoint(cp);
            } else if (cp == '\\') {
                cp = input.readCodePoint();
                checkCondition(
                        cp == '{' || cp == '|' || cp == '}',
                        "Invalid escape sequence. Only \\{, \\| and \\} are valid here.");
                result.append(cp);
            } else if (cp == '|') {
                input.backup(1);
                MFDataModel.Literal quoted = getQuotedLiteral();
                result.append(quoted.value);
            } else if (cp == EOF) {
                return result.toString();
            } else {
                if (result.length() == 0) {
                    input.backup(spaceCount + 1);
                    return "";
                } else {
                    input.backup(1);
                    return result.toString();
                }
            }
        }
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
        while (true) {
            MFDataModel.Option option = getOption();
            if (option == null) {
                break;
            }
            if (options.containsKey(option.name)) {
                error("Duplicated option '" + option.name + "'");
            }
            options.put(option.name, option);
        }
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
        // skipOptionalWhitespaces();
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
                checkCondition(cp == '|', "Invalid escape sequence, only \"\\|\" is valid here");
                result.appendCodePoint('|');
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

    private void skipMandatoryWhitespaces() throws MFParseException {
        int count = skipWhitespaces();
        checkCondition(count > 0, "Space expected");
    }

    private void skipOptionalWhitespaces() {
        skipWhitespaces();
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
            if (cp == EOF) {
                // Only declarations, no pattern
                return new MFDataModel.PatternMessage(declarations, null);
            } else {
                MFDataModel.Pattern pattern = getQuotedPattern();
                return new MFDataModel.PatternMessage(declarations, pattern);
            }
        }
    }

    // abnf: matcher = match-statement 1*([s] variant)
    // abnf: match-statement = match 1*([s] selector)
    // abnf: selector = expression
    // abnf: variant = key *(s key) [s] quoted-pattern
    // abnf: key = literal / "*"
    // abnf: match = %s".match"
    private MFDataModel.SelectMessage getMatch(List<MFDataModel.Declaration> declarations)
            throws MFParseException {
        // ".match" was already consumed by the caller
        // Look for selectors
        List<MFDataModel.Expression> expressions = new ArrayList<>();
        while (true) {
            skipMandatoryWhitespaces();
            MFDataModel.Expression expression = getPlaceholder();
            if (expression == null) {
                break;
            }
            checkCondition(
                    !(expression instanceof MFDataModel.Markup), "Cannot do selection on markup");
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
        return new MFDataModel.SelectMessage(declarations, expressions, variants);
    }

    // abnf: variant = key *(s key) [s] quoted-pattern
    // abnf: key = literal / "*"
    private MFDataModel.Variant getVariant() throws MFParseException {
        List<MFDataModel.LiteralOrCatchallKey> keys = new ArrayList<>();
        // abnf variant = key *(s key) [s] quoted-pattern
        while (true) {
            // Space is required between keys
            MFDataModel.LiteralOrCatchallKey key = getKey(!keys.isEmpty());
            if (key == null) {
                break;
            }
            keys.add(key);
        }
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
        if (requireSpaces) {
            skipMandatoryWhitespaces();
        } else {
            skipOptionalWhitespaces();
        }
        int cp = input.peekChar();
        if (cp == '*') {
            input.readCodePoint(); // consume the '*'
            return new MFDataModel.CatchallKey();
        }
        if (cp == EOF) {
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
    // abnf: reserved-statement = reserved-keyword [s reserved-body] 1*([s] expression)
    // abnf: reserved-keyword = "." name
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
                skipMandatoryWhitespaces();
                expression = getPlaceholder();
                String inputVarName = null;
                if (expression instanceof MFDataModel.VariableExpression) {
                    inputVarName = ((MFDataModel.VariableExpression) expression).arg.name;
                }
                if (expression instanceof MFDataModel.VariableExpression) {
                    return new MFDataModel.InputDeclaration(
                            inputVarName, (MFDataModel.VariableExpression) expression);
                }
                break;
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
            default: // abnf: reserved-statement = reserved-keyword [s reserved-body] 1*([s] expression)
                skipOptionalWhitespaces();
                String body = getReservedBody();
                List<MFDataModel.Expression> expressions = new ArrayList<>();
                while (true) {
                    skipOptionalWhitespaces();
                    expression = getPlaceholder();
                    // This also covers != null
                    if (expression instanceof MFDataModel.VariableExpression) {
                        expressions.add(expression);
                    } else {
                        break;
                    }
                }
                return new MFDataModel.UnsupportedStatement(declName, body, expressions);
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
