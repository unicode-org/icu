// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to register mappings between various function names and the factories that can
 * create those functions.
 *
 * <p>For example to add formatting for a {@code Person} object one would need to:
 *
 * <ul>
 *   <li>write a function (class, lambda, etc.) that does the formatting proper (implementing {@link
 *       Function})
 *   <li>write a factory that creates such a function (implementing {@link FunctionFactory})
 *   <li>add a mapping from the function name as used in the syntax (for example {@code "person"})
 *       to the factory
 *   <li>optionally add a mapping from the class to format ({@code ...Person.class}) to the function
 *       name ({@code "person"}), so that one can use a placeholder in the message without
 *       specifying a function (for example {@code "... {$me} ..."} instead of {@code "... {$me
 *       :person} ..."}, if the class of {@code $me} is an {@code instanceof Person}).
 * </ul>
 *
 * <p><b>NOTE:</b> all function names are normalized to NFC.
 *
 * @internal ICU 72 technology preview
 * @deprecated This API is for technology preview only.
 */
@Deprecated
public class MFFunctionRegistry {
    private final Map<String, FunctionFactory> functionMap;
    private final Map<Class<?>, String> classToFunction;

    private MFFunctionRegistry(Builder builder) {
        this.functionMap = new HashMap<>(builder.functionMap);
        this.classToFunction = new HashMap<>(builder.classToFunction);
    }

    /**
     * Creates a builder.
     *
     * @return the Builder.
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the function factory used to create the function named {@code name}.
     *
     * <p>Note: function name here means the name used to refer to the function in the MessageFormat
     * 2 syntax, for example {@code "... {$exp :datetime} ..."}<br>
     * The function name here is {@code "datetime"}, and does not have to correspond to the name of
     * the methods / classes used to implement the functionality.
     *
     * <p>For example one might write a {@code PersonFunctionFactory} returning a {@code
     * PersonFunction}, and map that to the MessageFormat function named {@code "person"}.<br>
     * The only name visible to the users of MessageFormat syntax will be {@code "person"}.
     *
     * @param functionName the function name.
     * @return the factory creating function for {@code name}. Returns {@code null} if none is
     *     registered.
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public FunctionFactory getFunction(String functionName) {
        return functionMap.get(StringUtils.toNfc(functionName));
    }

    /**
     * Get all know names that have a mappings from name to {@link FunctionFactory}.
     *
     * @return a set of all the known function names.
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public Set<String> getFunctionNames() {
        return functionMap.keySet();
    }

    /**
     * Returns the name of the function used to format an object of type {@code clazz}.
     *
     * @param clazz the class of the object to format.
     * @return the name of the function class, if registered. Returns {@code null} otherwise.
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public String getDefaultFunctionNameForType(Class<?> clazz) {
        // Search for the class "as is", to save time.
        // If we don't find it then we iterate the registered classes and check
        // if the class is an instanceof the ones registered.
        // For example a BuddhistCalendar when we only registered Calendar
        String result = classToFunction.get(clazz);
        if (result != null) {
            return result;
        }
        // We didn't find the class registered explicitly "as is"
        for (Map.Entry<Class<?>, String> e : classToFunction.entrySet()) {
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
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public Set<Class<?>> getDefaultFunctionTypes() {
        return classToFunction.keySet();
    }

    /**
     * A {@code Builder} used to build instances of {@link MFFunctionRegistry}.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class Builder {
        private final Map<String, FunctionFactory> functionMap = new HashMap<>();
        private final Map<Class<?>, String> classToFunction = new HashMap<>();

        // Prevent direct creation
        private Builder() {}

        /**
         * Adds all the mapping from another registry to this one.
         *
         * @param functionRegistry the registry to copy from.
         * @return the builder, for fluent use.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Builder addAll(MFFunctionRegistry functionRegistry) {
            functionMap.putAll(functionRegistry.functionMap);
            classToFunction.putAll(functionRegistry.classToFunction);
            return this;
        }

        /**
         * Adds a mapping from a function name to a {@link FunctionFactory}.
         *
         * @param functionName the function name (as used in the MessageFormat 2 syntax).
         * @param functionFactory the factory that handles the name.
         * @return the builder, for fluent use.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Builder setFunction(String functionName, FunctionFactory functionFactory) {
            functionMap.put(StringUtils.toNfc(functionName), functionFactory);
            return this;
        }

        /**
         * Remove the function associated with the name.
         *
         * @param functionName the name of the function to remove.
         * @return the builder, for fluent use.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Builder removeFunction(String functionName) {
            functionMap.remove(StringUtils.toNfc(functionName));
            return this;
        }

        /**
         * Remove all the function mappings.
         *
         * @return the builder, for fluent use.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Builder clearFunctions() {
            functionMap.clear();
            return this;
        }

        /**
         * Adds a mapping from a type to format to a {@link FunctionFactory} function name.
         *
         * @param clazz the class of the type to format.
         * @param functionName the unction name (as used in the MessageFormat 2 syntax).
         * @return the builder, for fluent use.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Builder setDefaultFunctionNameForType(Class<?> clazz, String functionName) {
            classToFunction.put(clazz, StringUtils.toNfc(functionName));
            return this;
        }

        /**
         * Remove the function name associated with the class.
         *
         * @param clazz the class to remove the mapping for.
         * @return the builder, for fluent use.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Builder removeDefaultFunctionNameForType(Class<?> clazz) {
            classToFunction.remove(clazz);
            return this;
        }

        /**
         * Remove all the class to function-names mappings.
         *
         * @return the builder, for fluent use.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Builder clearDefaultFunctionNames() {
            classToFunction.clear();
            return this;
        }

        /**
         * Builds an instance of {@link MFFunctionRegistry}.
         *
         * @return the function registry created.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public MFFunctionRegistry build() {
            return new MFFunctionRegistry(this);
        }
    }
}
