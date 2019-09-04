// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.regex;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;
import org.unicode.icu.tool.cldrtoicu.PathValueTransformer.DynamicVars;
import org.unicode.icu.tool.cldrtoicu.PathValueTransformer.Result;
import org.unicode.icu.tool.cldrtoicu.RbPath;

import com.google.common.collect.ImmutableList;

/*
 * Each rule corresponds to a single target xpath specification in the configuration file
 * (lines starting //) but may have more than one result specification. For example:
 *
 * //supplementalData/languageData/language[@type="(%W)"][@scripts="(%W)"][@territories="(%W)"]
 *      ; /languageData/$1/primary/scripts ; values=$2
 *      ; /languageData/$1/primary/territories; values=$3
 *
 * is represented by a single rule with two result specifications.
 */
abstract class Rule {
    /** Returns a rule for which all '%X' arguments have been resolved (almost all cases). */
    static Rule staticRule(
        CldrDataType dtdType,
        String prefix,
        Iterable<ResultSpec> specs,
        String pathRegex,
        String xpathSpec,
        int lineNumber) {

        return new StaticRule(dtdType, prefix, specs, pathRegex, xpathSpec, lineNumber);
    }

    /** Returns a rule for which some '%X' arguments are unresolved until matching occurs. */
    static Rule dynamicRule(
        CldrDataType dtdType,
        String pathRegex,
        Iterable<ResultSpec> specs,
        VarString varString,
        Function<Character, CldrPath> varFn,
        String xpathSpec,
        int lineNumber) {

        return new DynamicRule(dtdType, pathRegex, specs, varString, varFn, xpathSpec, lineNumber);
    }

    // Type of CLDR path which can match this rule.
    private final CldrDataType dtdType;
    // The first path element below the root, used to do fast rejection of non-matching paths
    // and to "bucket" rules by their prefix to speed up matching.
    private final String pathPrefix;
    // One or more result specifications to be processed for matching CLDR paths/values.
    private final ImmutableList<ResultSpec> resultSpecs;
    // Debug information only to help determine unused rules.
    private final String xpathSpec;
    private final int lineNumber;

    private Rule(
        CldrDataType dtdType,
        String pathPrefix,
        Iterable<ResultSpec> resultSpecs,
        String xpathSpec,
        int lineNumber) {

        this.dtdType = checkNotNull(dtdType);
        this.pathPrefix = checkNotNull(pathPrefix);
        this.resultSpecs = ImmutableList.copyOf(resultSpecs);
        this.xpathSpec = checkNotNull(xpathSpec);
        this.lineNumber = lineNumber;
    }

    /** Returns the CLDR DTD type of the path that the rule can match. */
    final CldrDataType getDataType() {
        return dtdType;
    }

    /** Returns the name of the first path element below the path root. */
    final String getPathPrefix() {
        return pathPrefix;
    }

    /** Returns the regular expression against which CLDR path strings are matched. */
    abstract Pattern getPathPattern(DynamicVars varLookupFn);

    /**
     * Attempts to match the incoming xpath and (if successful) use captured arguments to
     * generate one result for each result specification.
     */
    final ImmutableList<Result> transform(CldrValue v, String fullXPath, DynamicVars varFn) {
        Matcher m = getPathPattern(varFn).matcher(fullXPath);
        return m.matches()
            ? resultSpecs.stream()
                .flatMap(r -> r.transform(v, m, varFn))
                .collect(toImmutableList())
            : ImmutableList.of();
    }

    /**
     * Returns any fallback functions defined in results specifications. These are used to
     * determine the set of possible fallback values for a given resource bundle path.
     */
    final Stream<BiFunction<RbPath, DynamicVars, Optional<Result>>> getFallbackFunctions() {
        return resultSpecs.stream()
            .map(ResultSpec::getFallbackFunction)
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

    // Debugging only
    final String getXpathSpec() {
        return xpathSpec;
    }

    // Debugging only
    final int getLineNumber() {
        return lineNumber;
    }

    private static final class StaticRule extends Rule {
        // The processed xpath specification yielding an xpath matching regular expression. This is
        // only suitable for matching incoming xpaths and cannot be processed in any other way.
        private final Pattern xpathPattern;

        StaticRule(
            CldrDataType dtdType,
            String prefix,
            Iterable<ResultSpec> specs,
            String pathRegex,
            String xpathSpec,
            int lineNumber) {

            super(dtdType, prefix, specs, xpathSpec, lineNumber);
            this.xpathPattern = Pattern.compile(pathRegex);
        }

        @Override
        Pattern getPathPattern(DynamicVars varLookupFn) {
            return xpathPattern;
        }
    }

    private static final class DynamicRule extends Rule {
        // The processed xpath specification yielding an xpath matching regular expression. This is
        // only suitable for matching incoming xpaths and cannot be processed in any other way.
        private final VarString varString;
        private final Function<Character, CldrPath> dynamicVarFn;

        DynamicRule(
            CldrDataType dtdType,
            String prefix,
            Iterable<ResultSpec> specs,
            VarString varString,
            Function<Character, CldrPath> varFn,
            String xpathSpec,
            int lineNumber) {

            super(dtdType, prefix, specs, xpathSpec, lineNumber);
            this.varString = checkNotNull(varString);
            this.dynamicVarFn = checkNotNull(varFn);
        }

        @Override Pattern getPathPattern(DynamicVars varLookupFn) {
            String pathRegex = varString.apply(dynamicVarFn.andThen(varLookupFn)).get();
            return Pattern.compile(pathRegex);
        }
    }
}
