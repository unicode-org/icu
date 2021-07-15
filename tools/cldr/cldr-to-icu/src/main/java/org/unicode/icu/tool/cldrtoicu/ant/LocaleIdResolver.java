// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.ant;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.unicode.icu.tool.cldrtoicu.SupplementalData;

import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

/** Helper class to reslove ID configuration. */
final class LocaleIdResolver {
    /** Returns the expanded set of target locale IDs based on the given ID specifications. */
    public static ImmutableSet<String> expandTargetIds(
        Set<String> idSpecs, SupplementalData supplementalData) {
        return new LocaleIdResolver(supplementalData).resolve(idSpecs);
    }

    private final SupplementalData supplementalData;

    private LocaleIdResolver(SupplementalData supplementalData) {
        this.supplementalData = checkNotNull(supplementalData);
    }

    // ---- Code below here is to expand the incoming set of locale IDs ----

    private static final Pattern WILDCARD_LOCALE = Pattern.compile("[a-z]{2,3}(?:_[A-Z][a-z]{3})?");

    private ImmutableSet<String> resolve(Set<String> idSpecs) {
        ImmutableSet<String> allAvailableIds = supplementalData.getAvailableLocaleIds();
        // Get the minimized wildcard set, converting things like "en_Latn" --> "en".
        ImmutableSet<String> wildcardIds = idSpecs.stream()
            .filter(supplementalData.getAvailableLocaleIds()::contains)
            .filter(id -> WILDCARD_LOCALE.matcher(id).matches())
            .map(this::removeDefaultScript)
            .collect(toImmutableSet());

        // Get the set of IDs which are implied by the wildcard IDs.
        Set<String> targetIds = new TreeSet<>();
        allAvailableIds.forEach(id -> addWildcardMatches(id, wildcardIds::contains, targetIds));

        // Get the IDs which don't need to be in the config (because they are implied).
        Set<String> redundant = Sets.intersection(idSpecs, targetIds);
        if (!redundant.isEmpty()) {
            System.err.println("Configuration lists redundant locale IDs");
            System.err.println("The following IDs should be removed from the configuration:");
            Iterables.partition(redundant, 16)
                .forEach(ids -> System.err.println(String.join(", ", ids)));

            // Note that the minimal configuration includes aliases.
            Set<String> minimalConfigIds = new TreeSet<>(Sets.difference(idSpecs, targetIds));
            minimalConfigIds.remove("root");
            ImmutableListMultimap<Character, String> idsByFirstChar =
                Multimaps.index(minimalConfigIds, s -> s.charAt(0));

            System.err.println("Canonical ID list is:");
            for (char c: idsByFirstChar.keySet()) {
                System.err.println("    // " + Ascii.toUpperCase(c));
                Iterables.partition(idsByFirstChar.get(c), 16)
                    .forEach(ids -> System.err.println("    " + String.join(", ", ids)));
                System.err.println();
            }
            System.err.flush();
            throw new IllegalStateException("Non-canonical configuration");
        }

        // We return the set of IDs made up of:
        // 1: The original IDs specified by the configuration (and any parent IDs).
        // 2: IDs expanded from wildcard IDs (e.g. "en_Latn_GB" & "en_Latn" from "en").
        //    (this is what's already in targetIds).
        // 3: The "root" ID.
        idSpecs.forEach(id -> addRecursively(id, targetIds));
        return ImmutableSet.<String>builder().add("root").addAll(targetIds).build();
    }

    // E.g. "xx_Fooo" --> "xx" --> "xx_Baar_YY" ==> "xx_Fooo"
    // E.g. "xx_Fooo" --> "xx" --> "xx_Fooo_YY" ==> "xx"
    private String removeDefaultScript(String id) {
        if (id.contains("_")) {
            String lang = id.substring(0, id.indexOf("_"));
            String maxId = supplementalData.maximize(lang)
                .orElseThrow(
                    () -> new IllegalStateException("cannot maximize language subtag: " + lang));
            if (maxId.startsWith(id)) {
                return lang;
            }
        }
        return id;
    }

    private void addRecursively(String id, Set<String> dst) {
        while (!id.equals("root") && dst.add(id)) {
            id = supplementalData.getParent(id);
        }
    }

    private boolean addWildcardMatches(
        String id, Predicate<String> isWildcard, Set<String> dst) {
        if (id.equals("root")) {
            return false;
        }
        String parentId = supplementalData.getParent(id);
        if (isWildcard.test(parentId) || addWildcardMatches(parentId, isWildcard, dst)) {
            dst.add(id);
            return true;
        }
        return false;
    }
}
