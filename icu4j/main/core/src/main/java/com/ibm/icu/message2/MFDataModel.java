// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This maps closely to the official specification.
 * Since it is not final, we will not add javadoc everywhere.
 *
 * <p>See <a target="github" href="https://github.com/unicode-org/message-format-wg/blob/main/spec/data-model/README.md">the
 * latest description</a>.</p>
 *
 * @internal ICU 72 technology preview
 * @deprecated This API is for technology preview only.
 */
@Deprecated
@SuppressWarnings("javadoc")
public class MFDataModel {

    private MFDataModel() {
        // Prevent instantiation
    }

    // Messages

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public interface Message {
        // Provides a common type for PatternMessage and SelectMessage.
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class PatternMessage implements Message {
        public final List<Declaration> declarations;
        public final Pattern pattern;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public PatternMessage(List<Declaration> declarations, Pattern pattern) {
            this.declarations = declarations;
            this.pattern = pattern;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class SelectMessage implements Message {
        public final List<Declaration> declarations;
        public final List<Expression> selectors;
        public final List<Variant> variants;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public SelectMessage(
                List<Declaration> declarations,
                List<Expression> selectors,
                List<Variant> variants) {
            this.declarations = declarations;
            this.selectors = selectors;
            this.variants = variants;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public interface Declaration {
        // Provides a common type for InputDeclaration, LocalDeclaration, and UnsupportedStatement.
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class InputDeclaration implements Declaration {
        public final String name;
        public final VariableExpression value;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public InputDeclaration(String name, VariableExpression value) {
            this.name = name;
            this.value = value;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class LocalDeclaration implements Declaration {
        public final String name;
        public final Expression value;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public LocalDeclaration(String name, Expression value) {
            this.name = name;
            this.value = value;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class UnsupportedStatement implements Declaration {
        public final String keyword;
        public final String body;
        public final List<Expression> expressions;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public UnsupportedStatement(String keyword, String body, List<Expression> expressions) {
            this.keyword = keyword;
            this.body = body;
            this.expressions = expressions;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public interface LiteralOrCatchallKey {
        // Provides a common type for the selection keys: Variant, Literal, or CatchallKey.
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class Variant implements LiteralOrCatchallKey {
        public final List<LiteralOrCatchallKey> keys;
        public final Pattern value;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Variant(List<LiteralOrCatchallKey> keys, Pattern value) {
            this.keys = keys;
            this.value = value;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class CatchallKey implements LiteralOrCatchallKey {
        // String value; // Always '*' in MF2
    }

    // Patterns

    // type Pattern = Array<string | Expression | Markup>;
    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class Pattern {
        public final List<PatternPart> parts;

        Pattern() {
            this.parts = new ArrayList<>();
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public interface PatternPart {
        // Provides a common type for StringPart and Expression.
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class StringPart implements PatternPart {
        public final String value;

        StringPart(String value) {
            this.value = value;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public interface Expression extends PatternPart {
        // Provides a common type for all kind of expressions:
        // LiteralExpression, VariableExpression, FunctionExpression, UnsupportedExpression, Markup
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class LiteralExpression implements Expression {
        public final Literal arg;
        public final Annotation annotation;
        public final List<Attribute> attributes;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public LiteralExpression(Literal arg, Annotation annotation, List<Attribute> attributes) {
            this.arg = arg;
            this.annotation = annotation;
            this.attributes = attributes;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class VariableExpression implements Expression {
        public final VariableRef arg;
        public final Annotation annotation;
        public final List<Attribute> attributes;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public VariableExpression(
                VariableRef arg, Annotation annotation, List<Attribute> attributes) {
            this.arg = arg;
            this.annotation = annotation;
            this.attributes = attributes;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public interface Annotation {
        // Provides a common type for FunctionAnnotation, UnsupportedAnnotation
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class FunctionExpression implements Expression {
        public final FunctionAnnotation annotation;
        public final List<Attribute> attributes;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public FunctionExpression(FunctionAnnotation annotation, List<Attribute> attributes) {
            this.annotation = annotation;
            this.attributes = attributes;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class UnsupportedExpression implements Expression {
        public final UnsupportedAnnotation annotation;
        public final List<Attribute> attributes;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public UnsupportedExpression(UnsupportedAnnotation annotation, List<Attribute> attributes) {
            this.annotation = annotation;
            this.attributes = attributes;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class Attribute {
        public final String name;
        public final LiteralOrVariableRef value;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Attribute(String name, LiteralOrVariableRef value) {
            this.name = name;
            this.value = value;
        }
    }

    // Expressions

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public interface LiteralOrVariableRef {
        // Provides a common type for Literal and VariableRef,
        // to represent things like `foo` / `|foo|` / `1234` (literals)
        // and `$foo` (VariableRef), as argument for placeholders or value in options.
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class Literal implements LiteralOrVariableRef, LiteralOrCatchallKey {
        public final String value;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Literal(String value) {
            this.value = value;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class VariableRef implements LiteralOrVariableRef {
        public final String name;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public VariableRef(String name) {
            this.name = name;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class FunctionAnnotation implements Annotation {
        public final String name;
        public final Map<String, Option> options;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public FunctionAnnotation(String name, Map<String, Option> options) {
            this.name = name;
            this.options = options;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class Option {
        public final String name;
        public final LiteralOrVariableRef value;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Option(String name, LiteralOrVariableRef value) {
            this.name = name;
            this.value = value;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class UnsupportedAnnotation implements Annotation {
        public final String source;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public UnsupportedAnnotation(String source) {
            this.source = source;
        }
    }

    // Markup

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class Markup implements Expression {
        enum Kind {
            OPEN,
            CLOSE,
            STANDALONE
        }

        public final Kind kind;
        public final String name;
        public final Map<String, Option> options;
        public final List<Attribute> attributes;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Markup(
                Kind kind, String name, Map<String, Option> options, List<Attribute> attributes) {
            this.kind = kind;
            this.name = name;
            this.options = options;
            this.attributes = attributes;
        }
    }
}
