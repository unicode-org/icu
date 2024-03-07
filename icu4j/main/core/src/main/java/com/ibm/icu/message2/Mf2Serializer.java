// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.util.ArrayList;
import java.util.List;

import com.ibm.icu.message2.Mf2DataModel.Expression;
import com.ibm.icu.message2.Mf2DataModel.Pattern;
import com.ibm.icu.message2.Mf2DataModel.SelectorKeys;
import com.ibm.icu.message2.Mf2DataModel.Text;
import com.ibm.icu.message2.Mf2DataModel.Value;
import com.ibm.icu.message2.Mf2Parser.EventHandler;
import com.ibm.icu.message2.Mf2Serializer.Token.Type;

// TODO: find a better name for this class
class Mf2Serializer implements EventHandler {
    private String input;
    private final List<Token> tokens = new ArrayList<>();

    static class Token {
        final String name;
        final int begin;
        final int end;
        final Kind kind;
        private final Type type;
        private final String input;

        enum Kind {
            TERMINAL,
            NONTERMINAL_START,
            NONTERMINAL_END
        }

        enum Type {
            MESSAGE,
            PATTERN,
            TEXT,
            PLACEHOLDER,
            EXPRESSION,
            OPERAND,
            VARIABLE,
            IGNORE,
            FUNCTION,
            OPTION,
            NAME,
            NMTOKEN,
            LITERAL,
            SELECTOR,
            VARIANT,
            DECLARATION, VARIANTKEY, DEFAULT,
        }

        Token(Kind kind, String name, int begin, int end, String input) {
            this.kind = kind;
            this.name = name;
            this.begin = begin;
            this.end = end;
            this.input = input;
            switch (name) {
                case "Message": type = Type.MESSAGE; break;
                case "Pattern": type = Type.PATTERN; break;
                case "Text": type = Type.TEXT; break;
                case "Placeholder": type = Type.PLACEHOLDER; break;
                case "Expression": type = Type.EXPRESSION; break;
                case "Operand": type = Type.OPERAND; break;
                case "Variable": type = Type.VARIABLE; break;
                case "Function": type = Type.FUNCTION; break;
                case "Option": type = Type.OPTION; break;
                case "Annotation": type = Type.IGNORE; break;
                case "Name": type = Type.NAME; break;
                case "Nmtoken": type = Type.NMTOKEN; break;
                case "Literal": type = Type.LITERAL; break;
                case "Selector": type = Type.SELECTOR; break;
                case "Variant": type = Type.VARIANT; break;
                case "VariantKey": type = Type.VARIANTKEY; break;
                case "Declaration": type = Type.DECLARATION; break;

                case "Markup": type = Type.IGNORE; break;
                case "MarkupStart": type = Type.IGNORE; break;
                case "MarkupEnd": type = Type.IGNORE; break;

                case "'['": type = Type.IGNORE; break;
                case "']'": type = Type.IGNORE; break;
                case "'{'": type = Type.IGNORE; break;
                case "'}'": type = Type.IGNORE; break;
                case "'='": type = Type.IGNORE; break;
                case "'match'": type = Type.IGNORE; break;
                case "'when'": type = Type.IGNORE; break;
                case "'let'": type = Type.IGNORE; break;
                case "'*'": type = Type.DEFAULT; break;
                default:
                    throw new IllegalArgumentException("Parse error: Unknown token \"" + name + "\"");
            }
        }

        boolean isStart() {
            return Kind.NONTERMINAL_START.equals(kind);
        }

        boolean isEnd() {
            return Kind.NONTERMINAL_END.equals(kind);
        }

        boolean isTerminal() {
            return Kind.TERMINAL.equals(kind);
        }

        @Override
        public String toString() {
            int from = begin == -1 ? 0 : begin;
            String strval = end == -1 ? input.substring(from) : input.substring(from, end);
            return String.format("Token(\"%s\", [%d, %d], %s) // \"%s\"", name, begin, end, kind, strval);
        }
    }

    Mf2Serializer() {}

    @Override
    public void reset(CharSequence input) {
        this.input = input.toString();
        tokens.clear();
    }

    @Override
    public void startNonterminal(String name, int begin) {
        tokens.add(new Token(Token.Kind.NONTERMINAL_START, name, begin, -1, input));
    }

    @Override
    public void endNonterminal(String name, int end) {
        tokens.add(new Token(Token.Kind.NONTERMINAL_END, name, -1, end, input));
    }

    @Override
    public void terminal(String name, int begin, int end) {
        tokens.add(new Token(Token.Kind.TERMINAL, name, begin, end, input));
    }

    @Override
    public void whitespace(int begin, int end) {
    }

    Mf2DataModel build() {
        if (!tokens.isEmpty()) {
            Token firstToken = tokens.get(0);
            if (Type.MESSAGE.equals(firstToken.type) && firstToken.isStart()) {
                return parseMessage();
            }
        }
        return null;
    }

    private Mf2DataModel parseMessage() {
        Mf2DataModel.Builder result = Mf2DataModel.builder();

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            switch (token.type) {
                case MESSAGE:
                    if (token.isStart() && i == 0) {
                        // all good
                    } else if (token.isEnd() && i == tokens.size() - 1) {
                        // We check if this last token is at the end of the input
                        if (token.end != input.length()) {
                            String leftover = input.substring(token.end)
                                    .replace("\n", "")
                                    .replace("\r", "")
                                    .replace(" ", "")
                                    .replace("\t", "")
                                    ;
                            if (!leftover.isEmpty()) {
                                throw new IllegalArgumentException("Parse error: Content detected after the end of the message: '"
                                        + input.substring(token.end) + "'");
                            }
                        }
                        return result.build();
                    } else {
                        // End of message, we ignore the rest
                        throw new IllegalArgumentException("Parse error: Extra tokens at the end of the message");
                    }
                    break;
                case PATTERN:
                    ParseResult<Pattern> patternResult = parsePattern(i);
                    i = patternResult.skipLen;
                    result.setPattern(patternResult.resultValue);
                    break;
                case DECLARATION:
                    Declaration declaration = new Declaration();
                    i = parseDeclaration(i, declaration);
                    result.addLocalVariable(declaration.variableName, declaration.expr);
                    break;
                case SELECTOR:
                    ParseResult<List<Expression>> selectorResult = parseSelector(i);
                    result.addSelectors(selectorResult.resultValue);
                    i = selectorResult.skipLen;
                    break;
                case VARIANT:
                    ParseResult<Variant> variantResult = parseVariant(i);
                    i = variantResult.skipLen;
                    Variant variant = variantResult.resultValue;
                    result.addVariant(variant.getSelectorKeys(), variant.getPattern());
                    break;
                case IGNORE:
                    break;
                default:
                    throw new IllegalArgumentException("Parse error: parseMessage UNEXPECTED TOKEN: '" + token + "'");
            }
        }
        throw new IllegalArgumentException("Parse error: Error parsing MessageFormatter");
    }

    private ParseResult<Variant> parseVariant(int startToken) {
        Variant.Builder result = Variant.builder();

        for (int i = startToken; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            switch (token.type) {
                case VARIANT:
                    if (token.isStart()) { // all good
                    } else if (token.isEnd()) {
                        return new ParseResult<>(i, result.build());
                    }
                    break;
                case LITERAL:
                    result.addSelectorKey(input.substring(token.begin + 1, token.end - 1));
                    break;
                case NMTOKEN:
                    result.addSelectorKey(input.substring(token.begin, token.end));
                    break;
                case DEFAULT:
                    result.addSelectorKey("*");
                    break;
                case PATTERN:
                    ParseResult<Pattern> patternResult = parsePattern(i);
                    i = patternResult.skipLen;
                    result.setPattern(patternResult.resultValue);
                    break;
                case VARIANTKEY:
//                    variant.variantKey = new VariantKey(input.substring(token.begin, token.end));
                    break;
                case IGNORE:
                    break;
                default:
                    throw new IllegalArgumentException("Parse error: parseVariant UNEXPECTED TOKEN: '" + token + "'");
            }
        }
        throw new IllegalArgumentException("Parse error: Error parsing Variant");
    }

    private ParseResult<List<Expression>> parseSelector(int startToken) {
        List<Expression> result = new ArrayList<>();

        for (int i = startToken; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            switch (token.type) {
                case SELECTOR:
                    if (token.isStart()) { // all good, do nothing
                    } else if (token.isEnd()) {
                        return new ParseResult<>(i, result);
                    }
                    break;
                case EXPRESSION:
                    ParseResult<Expression> exprResult = parseExpression(i);
                    i = exprResult.skipLen;
                    result.add(exprResult.resultValue);
                    break;
                case IGNORE:
                    break;
                default:
                    throw new IllegalArgumentException("Parse error: parseSelector UNEXPECTED TOKEN: '" + token + "'");
            }
        }
        throw new IllegalArgumentException("Parse error: Error parsing selectors");
    }

    private int parseDeclaration(int startToken, Declaration declaration) {
        for (int i = startToken; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            switch (token.type) {
                case DECLARATION:
                    if (token.isStart()) { // all good
                    } else if (token.isEnd()) {
                        return i;
                    }
                    break;
                case VARIABLE:
                    declaration.variableName = input.substring(token.begin + 1, token.end);
                    break;
                case EXPRESSION:
                    ParseResult<Expression> exprResult = parseExpression(i);
                    i = exprResult.skipLen;
                    declaration.expr = exprResult.resultValue;
                    break;
                case IGNORE:
                    break;
                default:
                    throw new IllegalArgumentException("Parse error: parseDeclaration UNEXPECTED TOKEN: '" + token + "'");
            }
        }
        throw new IllegalArgumentException("Parse error: Error parsing Declaration");
    }

    private ParseResult<Pattern> parsePattern(int startToken) {
        Pattern.Builder result = Pattern.builder();

        for (int i = startToken; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            switch (token.type) {
                case TEXT:
                    Text text = new Text(input.substring(token.begin, token.end));
                    result.add(text);
                    break;
                case PLACEHOLDER:
                    break;
                case EXPRESSION:
                    ParseResult<Expression> exprResult = parseExpression(i);
                    i = exprResult.skipLen;
                    result.add(exprResult.resultValue);
                    break;
                case VARIABLE:
                case IGNORE:
                    break;
                case PATTERN:
                    if (token.isStart() && i == startToken) { // all good, do nothing
                    } else if (token.isEnd()) {
                        return new ParseResult<>(i, result.build());
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Parse error: parsePattern UNEXPECTED TOKEN: '" + token + "'");
            }
        }
        throw new IllegalArgumentException("Parse error: Error parsing Pattern");
    }

    static class Option {
        String name;
        Value value;
    }

    static class Declaration {
        String variableName;
        Expression expr;
    }

    static class Variant {
        private final SelectorKeys selectorKeys;
        private final Pattern pattern;

        private Variant(Builder builder) {
            this.selectorKeys = builder.selectorKeys.build();
            this.pattern = builder.pattern;
        }

        /**
         * Creates a builder.
         *
         * @return the Builder.
         */
        public static Builder builder() {
            return new Builder();
        }

        public SelectorKeys getSelectorKeys() {
            return selectorKeys;
        }

        public Pattern getPattern() {
            return pattern;
        }

        public static class Builder {
            private final SelectorKeys.Builder selectorKeys = SelectorKeys.builder();
            private Pattern pattern = Pattern.builder().build();

            // Prevent direct creation
            private Builder() {
            }

            public Builder setSelectorKeys(SelectorKeys selectorKeys) {
                this.selectorKeys.addAll(selectorKeys.getKeys());
                return this;
            }

            public Builder addSelectorKey(String selectorKey) {
                this.selectorKeys.add(selectorKey);
                return this;
            }

            public Builder setPattern(Pattern pattern) {
                this.pattern = pattern;
                return this;
            }

            public Variant build() {
                return new Variant(this);
            }
        }
    }

    static class ParseResult<T> {
        final int skipLen;
        final T resultValue;

        public ParseResult(int skipLen, T resultValue) {
            this.skipLen = skipLen;
            this.resultValue = resultValue;
        }
    }

    private ParseResult<Expression> parseExpression(int startToken) {
        Expression.Builder result = Expression.builder();

        for (int i = startToken; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            switch (token.type) {
                case EXPRESSION: // intentional fall-through
                case PLACEHOLDER:
                    if (token.isStart() && i == startToken) {
                        // all good
                    } else if (token.isEnd()) {
                        return new ParseResult<>(i, result.build());
                    }
                    break;
                case FUNCTION:
                    result.setFunctionName(input.substring(token.begin + 1, token.end));
                    break;
                case LITERAL:
                    result.setOperand(Value.builder()
                            .setLiteral(input.substring(token.begin + 1, token.end - 1))
                            .build());
                    break;
                case VARIABLE:
                    result.setOperand(Value.builder()
                            .setVariableName(input.substring(token.begin + 1, token.end))
                            .build());
                    break;
                case OPTION:
                    Option option = new Option();
                    i = parseOptions(i, option);
                    result.addOption(option.name, option.value);
                    break;
                case OPERAND:
                    break;
                case IGNORE:
                    break;
                default:
                    throw new IllegalArgumentException("Parse error: parseExpression UNEXPECTED TOKEN: '" + token + "'");
            }
        }
        throw new IllegalArgumentException("Parse error: Error parsing Expression");
    }

    private int parseOptions(int startToken, Option option) {
        for (int i = startToken; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            switch (token.type) {
                case OPTION:
                    if (token.isStart() && i == startToken) {
                        // all good
                    } else if (token.isEnd()) {
                        return i;
                    }
                    break;
                case NAME:
                    option.name = input.substring(token.begin, token.end);
                    break;
                case LITERAL:
                    option.value = Value.builder()
                            .setLiteral(input.substring(token.begin + 1, token.end - 1))
                            .build();
                    break;
                case NMTOKEN:
                    option.value = Value.builder()
                            .setLiteral(input.substring(token.begin, token.end))
                            .build();
                    break;
                case VARIABLE:
                    option.value = Value.builder()
                            .setVariableName(input.substring(token.begin + 1, token.end))
                            .build();
                    break;
                case IGNORE:
                    break;
                default:
                    throw new IllegalArgumentException("Parse error: parseOptions UNEXPECTED TOKEN: '" + token + "'");
            }
        }
        throw new IllegalArgumentException("Parse error: Error parsing Option");
    }

    static String dataModelToString(Mf2DataModel dataModel) {
        return dataModel.toString();
    }
}
