// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to register mappings between various function
 * names and the factories that can create those functions.
 *
 * <p>For example to add formatting for a {@code Person} object one would need to:</p>
 * <ul>
 *   <li>write a function (class, lambda, etc.) that does the formatting proper
 *     (implementing {@link Formatter})</li>
 *   <li>write a factory that creates such a function
 *     (implementing {@link FormatterFactory})</li>
 *  <li>add a mapping from the function name as used in the syntax
 *    (for example {@code "person"}) to the factory</li>
 *  <li>optionally add a mapping from the class to format ({@code ...Person.class}) to
 *     the formatter name ({@code "person"}), so that one can use a placeholder in the message
 *     without specifying a function (for example {@code "... {$me} ..."} instead of
 *     {@code "... {$me :person} ..."}, if the class of {@code $me} is an {@code instanceof Person}).
 *     </li>
 * </ul>
 *
 * @internal ICU 72 technology preview
 * @deprecated This API is for ICU internal use only.
 */
@Deprecated
public class Mf2FunctionRegistry {
    private final Map<String, FormatterFactory> formattersMap;
    private final Map<String, SelectorFactory> selectorsMap;
    private final Map<Class<?>, String> classToFormatter;

    private Mf2FunctionRegistry(Builder builder) {
        this.formattersMap = new HashMap<>(builder.formattersMap);
        this.selectorsMap = new HashMap<>(builder.selectorsMap);
        this.classToFormatter = new HashMap<>(builder.classToFormatter);
    }

    /**
     * Creates a builder.
     *
     * @return the Builder.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the formatter factory used to create the formatter for function
     * named {@code name}.
     *
     * <p>Note: function name here means the name used to refer to the function in the
     * MessageFormat 2 syntax, for example {@code "... {$exp :datetime} ..."}<br>
     * The function name here is {@code "datetime"}, and does not have to correspond to the
     * name of the methods / classes used to implement the functionality.</p>
     *
     * <p>For example one might write a {@code PersonFormatterFactory} returning a {@code PersonFormatter},
     * and map that to the MessageFormat function named {@code "person"}.<br>
     * The only name visible to the users of MessageFormat syntax will be {@code "person"}.</p>
     *
     * @param formatterName the function name.
     * @return the factory creating formatters for {@code name}. Returns {@code null} if none is registered.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    public FormatterFactory getFormatter(String formatterName) {
        return formattersMap.get(formatterName);
    }

    /**
     * Get all know names that have a mappings from name to {@link FormatterFactory}.
     *
     * @return a set of all the known formatter names.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    public Set<String> getFormatterNames() {
        return formattersMap.keySet();
    }

    /**
     * Returns the name of the formatter used to format an object of type {@code clazz}.
     *
     * @param clazz the class of the object to format.
     * @return the name of the formatter class, if registered. Returns {@code null} otherwise.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    public String getDefaultFormatterNameForType(Class<?> clazz) {
        // Search for the class "as is", to save time.
        // If we don't find it then we iterate the registered classes and check
        // if the class is an instanceof the ones registered.
        // For example a BuddhistCalendar when we only registered Calendar
        String result = classToFormatter.get(clazz);
        if (result != null) {
            return result;
        }
        // We didn't find the class registered explicitly "as is"
        for (Map.Entry<Class<?>, String> e : classToFormatter.entrySet()) {
            if (e.getKey().isAssignableFrom(clazz)) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * Get all know classes that have a mappings from class to function name.
     *
     * @return a set of all the known classes that have mapping to function names.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    public Set<Class<?>> getDefaultFormatterTypes() {
        return classToFormatter.keySet();
    }

    /**
     * Returns the selector factory used to create the selector for function
     * named {@code name}.
     *
     * <p>Note: the same comments about naming as the ones on {@code getFormatter} apply.</p>
     *
     * @param selectorName the selector name.
     * @return the factory creating selectors for {@code name}. Returns {@code null} if none is registered.
     * @see #getFormatter(String)
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    public SelectorFactory getSelector(String selectorName) {
        return selectorsMap.get(selectorName);
    }

    /**
     * Get all know names that have a mappings from name to {@link SelectorFactory}.
     *
     * @return a set of all the known selector names.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    public Set<String> getSelectorNames() {
        return selectorsMap.keySet();
    }

    /**
     * A {@code Builder} used to build instances of {@link Mf2FunctionRegistry}.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    public static class Builder {
        private final Map<String, FormatterFactory> formattersMap = new HashMap<>();
        private final Map<String, SelectorFactory> selectorsMap = new HashMap<>();
        private final Map<Class<?>, String> classToFormatter = new HashMap<>();

        // Prevent direct creation
        private Builder() {
        }

        /**
         * Adds all the mapping from another registry to this one.
         *
         * @param functionRegistry the registry to copy from.
         * @return the builder, for fluent use.
         *
         * @internal ICU 72 technology preview
         * @deprecated This API is for ICU internal use only.
         */
        @Deprecated
        public Builder addAll(Mf2FunctionRegistry functionRegistry) {
            formattersMap.putAll(functionRegistry.formattersMap);
            selectorsMap.putAll(functionRegistry.selectorsMap);
            classToFormatter.putAll(functionRegistry.classToFormatter);
            return this;
        }

        /**
         * Adds a mapping from a formatter name to a {@link FormatterFactory}
         *
         * @param formatterName the function name (as used in the MessageFormat 2 syntax).
         * @param formatterFactory the factory that handles the name.
         * @return the builder, for fluent use.
         *
         * @internal ICU 72 technology preview
         * @deprecated This API is for ICU internal use only.
         */
        @Deprecated
        public Builder setFormatter(String formatterName, FormatterFactory formatterFactory) {
            formattersMap.put(formatterName, formatterFactory);
            return this;
        }

        /**
         * Remove the formatter associated with the name.
         *
         * @param formatterName the name of the formatter to remove.
         * @return the builder, for fluent use.
         *
         * @internal ICU 72 technology preview
         * @deprecated This API is for ICU internal use only.
         */
        @Deprecated
        public Builder removeFormatter(String formatterName) {
            formattersMap.remove(formatterName);
            return this;
        }

        /**
         * Remove all the formatter mappings.
         *
         * @return the builder, for fluent use.
         *
         * @internal ICU 72 technology preview
         * @deprecated This API is for ICU internal use only.
         */
        @Deprecated
        public Builder clearFormatters() {
            formattersMap.clear();
            return this;
        }

        /**
         * Adds a mapping from a type to format to a {@link FormatterFactory} formatter name.
         *
         * @param clazz the class of the type to format.
         * @param formatterName the formatter name (as used in the MessageFormat 2 syntax).
         * @return the builder, for fluent use.
         *
         * @internal ICU 72 technology preview
         * @deprecated This API is for ICU internal use only.
         */
        @Deprecated
        public Builder setDefaultFormatterNameForType(Class<?> clazz, String formatterName) {
            classToFormatter.put(clazz, formatterName);
            return this;
        }

        /**
         * Remove the function name associated with the class.
         *
         * @param clazz the class to remove the mapping for.
         * @return the builder, for fluent use.
         *
         * @internal ICU 72 technology preview
         * @deprecated This API is for ICU internal use only.
         */
        @Deprecated
        public Builder removeDefaultFormatterNameForType(Class<?> clazz) {
            classToFormatter.remove(clazz);
            return this;
        }

        /**
         * Remove all the class to formatter-names mappings.
         *
         * @return the builder, for fluent use.
         *
         * @internal ICU 72 technology preview
         * @deprecated This API is for ICU internal use only.
         */
        @Deprecated
        public Builder clearDefaultFormatterNames() {
            classToFormatter.clear();
            return this;
        }

        /**
         * Adds a mapping from a selector name to a {@link SelectorFactory}
         *
         * @param selectorName the function name (as used in the MessageFormat 2 syntax).
         * @param selectorFactory the factory that handles the name.
         * @return the builder, for fluent use.
         *
         * @internal ICU 72 technology preview
         * @deprecated This API is for ICU internal use only.
         */
        @Deprecated
        public Builder setSelector(String selectorName, SelectorFactory selectorFactory) {
            selectorsMap.put(selectorName, selectorFactory);
            return this;
        }

        /**
         * Remove the selector associated with the name.
         *
         * @param selectorName the name of the selector to remove.
         * @return the builder, for fluent use.
         *
         * @internal ICU 72 technology preview
         * @deprecated This API is for ICU internal use only.
         */
        @Deprecated
        public Builder removeSelector(String selectorName) {
            selectorsMap.remove(selectorName);
            return this;
        }

        /**
         * Remove all the selector mappings.
         *
         * @return the builder, for fluent use.
         *
         * @internal ICU 72 technology preview
         * @deprecated This API is for ICU internal use only.
         */
        @Deprecated
        public Builder clearSelectors() {
            selectorsMap.clear();
            return this;
        }

        /**
         * Builds an instance of {@link Mf2FunctionRegistry}.
         *
         * @return the function registry created.
         *
         * @internal ICU 72 technology preview
         * @deprecated This API is for ICU internal use only.
         */
        @Deprecated
        public Mf2FunctionRegistry build() {
            return new Mf2FunctionRegistry(this);
        }
    }
}
