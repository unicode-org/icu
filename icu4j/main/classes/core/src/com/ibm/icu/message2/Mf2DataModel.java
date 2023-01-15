// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;

/**
 * This maps closely to the official specification.
 * Since it is not final, we will not add javadoc everywhere.
 *
 * <p>See <a target="github" href="https://github.com/unicode-org/message-format-wg/blob/main/spec/syntax.md">the
 * description of the syntax with examples and use cases</a> and the corresponding
 * <a target="github" href="https://github.com/unicode-org/message-format-wg/blob/main/spec/message.ebnf">EBNF</a>.</p>
 *
 * @internal ICU 72 technology preview
 * @deprecated This API is for technology preview only.
 */
@Deprecated
@SuppressWarnings("javadoc")
public class Mf2DataModel {

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class SelectorKeys {
        private final List<String> keys;

        private SelectorKeys(Builder builder) {
            keys = new ArrayList<>();
            keys.addAll(builder.keys);
        }

        /**
         * Creates a builder.
         *
         * @return the Builder.
         *
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public static Builder builder() {
            return new Builder();
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public List<String> getKeys() {
            return Collections.unmodifiableList(keys);
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        @Override
        public String toString() {
            StringJoiner result = new StringJoiner(" ");
            for (String key : keys) {
                result.add(key);
            }
            return result.toString();
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public static class Builder {
            private final List<String> keys = new ArrayList<>();

            // Prevent direct creation
            private Builder() {
            }

            /**
             * @internal ICU 72 technology preview
             * @deprecated This API is for technology preview only.
             */
            @Deprecated
            public Builder add(String key) {
                keys.add(key);
                return this;
            }

            /**
             * @internal ICU 72 technology preview
             * @deprecated This API is for technology preview only.
             */
            @Deprecated
            public Builder addAll(Collection<String> otherKeys) {
                this.keys.addAll(otherKeys);
                return this;
            }

            /**
             * @internal ICU 72 technology preview
             * @deprecated This API is for technology preview only.
             */
            @Deprecated
            public SelectorKeys build() {
                return new SelectorKeys(this);
            }
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class Pattern {
        private final List<Part> parts;

        private Pattern(Builder builder) {
            parts = new ArrayList<>();
            parts.addAll(builder.parts);
        }

        /**
         * Creates a builder.
         *
         * @return the Builder.
         *
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public static Builder builder() {
            return new Builder();
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public List<Part> getParts() {
            return parts;
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("{");
            for (Part part : parts) {
                result.append(part);
            }
            result.append("}");
            return result.toString();
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public static class Builder {
            private final List<Part> parts = new ArrayList<>();

            // Prevent direct creation
            private Builder() {
            }

            /**
             * @internal ICU 72 technology preview
             * @deprecated This API is for technology preview only.
             */
            @Deprecated
            public Builder add(Part part) {
                parts.add(part);
                return this;
            }

            /**
             * @internal ICU 72 technology preview
             * @deprecated This API is for technology preview only.
             */
            @Deprecated
            public Builder addAll(Collection<Part> otherParts) {
                parts.addAll(otherParts);
                return this;
            }

            /**
             * @internal ICU 72 technology preview
             * @deprecated This API is for technology preview only.
             */
            @Deprecated
            public Pattern build() {
                return new Pattern(this);
            }

        }
    }

    /**
     * No functional role, this is only to be able to say that a message is a sequence of Part(s),
     * and that plain text {@link Text} and {@link Expression} are Part(s).
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public interface Part {
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class Text implements Part {
        private final String value;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        private Text(Builder builder) {
            this(builder.value);
        }

        /**
         * Creates a builder.
         *
         * @return the Builder.
         *
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public static Builder builder() {
            return new Builder();
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Text(String value) {
            this.value = value;
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public String getValue() {
            return value;
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        @Override
        public String toString() {
            return value;
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public static class Builder {
            private String value;

            // Prevent direct creation
            private Builder() {
            }

            /**
             * @internal ICU 72 technology preview
             * @deprecated This API is for technology preview only.
             */
            @Deprecated
            public Builder setValue(String value) {
                this.value = value;
                return this;
            }

            /**
             * @internal ICU 72 technology preview
             * @deprecated This API is for technology preview only.
             */
            @Deprecated
            public Text build() {
                return new Text(this);
            }
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class Expression implements Part {
        private final Value operand; // Literal | Variable
        private final String functionName;
        private final Map<String, Value> options;
        Formatter formatter = null;

        private Expression(Builder builder) {
            this.operand = builder.operand;
            this.functionName = builder.functionName;
            this.options = builder.options;
        }

        /**
         * Creates a builder.
         *
         * @return the Builder.
         *
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public static Builder builder() {
            return new Builder();
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Value getOperand() {
            return operand;
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public String getFunctionName() {
            return functionName;
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Map<String, Value> getOptions() {
            return options;
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("{");
            if (operand != null) {
                result.append(operand);
            }
            if (functionName != null) {
                result.append(" :").append(functionName);
            }
            for (Entry<String, Value> option : options.entrySet()) {
                result.append(" ").append(option.getKey()).append("=").append(option.getValue());
            }
            result.append("}");
            return result.toString();
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public static class Builder {
            private Value operand = null;
            private String functionName = null;
            private final OrderedMap<String, Value> options = new OrderedMap<>();

            // Prevent direct creation
            private Builder() {
            }

            /**
             * @internal ICU 72 technology preview
             * @deprecated This API is for technology preview only.
             */
            @Deprecated
            public Builder setOperand(Value operand) {
                this.operand = operand;
                return this;
            }

            /**
             * @internal ICU 72 technology preview
             * @deprecated This API is for technology preview only.
             */
            @Deprecated
            public Builder setFunctionName(String functionName) {
                this.functionName = functionName;
                return this;
            }

            /**
             * @internal ICU 72 technology preview
             * @deprecated This API is for technology preview only.
             */
            @Deprecated
            public Builder addOption(String key, Value value) {
                options.put(key, value);
                return this;
            }

            /**
             * @internal ICU 72 technology preview
             * @deprecated This API is for technology preview only.
             */
            @Deprecated
            public Builder addOptions(Map<String, Value> otherOptions) {
                options.putAll(otherOptions);
                return this;
            }

            /**
             * @internal ICU 72 technology preview
             * @deprecated This API is for technology preview only.
             */
            @Deprecated
            public Expression build() {
                return new Expression(this);
            }
        }
    }

//    public static class Placeholder extends Expression implements Part {
//        public Placeholder(Builder builder) {
//            super(builder);
//        }
//    }

    /**
     * A Value can be either a Literal, or a Variable, but not both.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class Value {
        private final String literal;
        private final String variableName;

        private Value(Builder builder) {
            this.literal = builder.literal;
            this.variableName = builder.variableName;
//            this(builder.literal, builder.variableName);
        }

        /**
         * Creates a builder.
         *
         * @return the Builder.
         *
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public static Builder builder() {
            return new Builder();
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public String getLiteral() {
            return literal;
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public String getVariableName() {
            return variableName;
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public boolean isLiteral() {
            return literal != null;
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public boolean isVariable() {
            return variableName != null;
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        @Override
        public String toString() {
            return isLiteral() ? "(" + literal + ")" : "$" + variableName;
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public static class Builder {
            private String literal;
            private String variableName;

            // Prevent direct creation
            private Builder() {
            }

            /**
             * @internal ICU 72 technology preview
             * @deprecated This API is for technology preview only.
             */
            @Deprecated
            public Builder setLiteral(String literal) {
                this.literal = literal;
                this.variableName = null;
                return this;
            }

            /**
             * @internal ICU 72 technology preview
             * @deprecated This API is for technology preview only.
             */
            @Deprecated
            public Builder setVariableName(String variableName) {
                this.variableName = variableName;
                this.literal = null;
                return this;
            }

            /**
             * @internal ICU 72 technology preview
             * @deprecated This API is for technology preview only.
             */
            @Deprecated
            public Value build() {
                return new Value(this);
            }
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class Variable {
        private final String name;

        private Variable(Builder builder) {
            this.name = builder.name;
        }

        /**
         * Creates a builder.
         *
         * @return the Builder.
         *
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public static Builder builder() {
            return new Builder();
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public String getName() {
            return name;
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public static class Builder {
            private String name;

            // Prevent direct creation
            private Builder() {
            }

            /**
             * @internal ICU 72 technology preview
             * @deprecated This API is for technology preview only.
             */
            @Deprecated
            public Builder setName(String name) {
                this.name = name;
                return this;
            }

            /**
             * @internal ICU 72 technology preview
             * @deprecated This API is for technology preview only.
             */
            @Deprecated
            public Variable build() {
                return new Variable(this);
            }
        }
    }

    /**
     * This is only to not force LinkedHashMap on the public API.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class OrderedMap<K, V> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = -7049361727790825496L;

        /**
         * {@inheritDoc}
         *
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public OrderedMap() {
            super();
        }
    }

    private final OrderedMap<String, Expression> localVariables;
    private final List<Expression> selectors;
    private final OrderedMap<SelectorKeys, Pattern> variants;
    private final Pattern pattern;

    private Mf2DataModel(Builder builder) {
        this.localVariables = builder.localVariables;
        this.selectors = builder.selectors;
        this.variants = builder.variants;
        this.pattern = builder.pattern;
    }

    /**
     * Creates a builder.
     *
     * @return the Builder.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public OrderedMap<String, Expression> getLocalVariables() {
        return localVariables;
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public List<Expression>  getSelectors() {
        return selectors;
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public OrderedMap<SelectorKeys, Pattern>  getVariants() {
        return variants;
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public Pattern getPattern() {
        return pattern;
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (Entry<String, Expression> lv : localVariables.entrySet()) {
            result.append("let $").append(lv.getKey());
            result.append(" = ");
            result.append(lv.getValue());
            result.append("\n");
        }
        if (!selectors.isEmpty()) {
            result.append("match");
            for (Expression e : this.selectors) {
                result.append(" ").append(e);
            }
            result.append("\n");
            for (Entry<SelectorKeys, Pattern> variant : variants.entrySet()) {
                result.append("  when ").append(variant.getKey());
                result.append(" ");
                result.append(variant.getValue());
                result.append("\n");
            }
        } else {
            result.append(pattern);
        }
        return result.toString();
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class Builder {
        private final OrderedMap<String, Expression> localVariables = new OrderedMap<>(); // declaration*
        private final List<Expression> selectors = new ArrayList<>();
        private final OrderedMap<SelectorKeys, Pattern> variants = new OrderedMap<>();
        private Pattern pattern = Pattern.builder().build();

        // Prevent direct creation
        private Builder() {
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Builder addLocalVariable(String variableName, Expression expression) {
            this.localVariables.put(variableName, expression);
            return this;
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Builder addLocalVariables(OrderedMap<String, Expression> otherLocalVariables) {
            this.localVariables.putAll(otherLocalVariables);
            return this;
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Builder addSelector(Expression otherSelector) {
            this.selectors.add(otherSelector);
            return this;
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Builder addSelectors(List<Expression> otherSelectors) {
            this.selectors.addAll(otherSelectors);
            return this;
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Builder addVariant(SelectorKeys keys, Pattern newPattern) {
            this.variants.put(keys, newPattern);
            return this;
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Builder addVariants(OrderedMap<SelectorKeys, Pattern> otherVariants) {
            this.variants.putAll(otherVariants);
            return this;
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Builder setPattern(Pattern pattern) {
            this.pattern = pattern;
            return this;
        }

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Mf2DataModel build() {
            return new Mf2DataModel(this);
        }
    }
}
