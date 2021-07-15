// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.joining;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Stores any explicit locale relationships for a single directory (e.g. "lang" or "coll").
 * This class just reflects a concise version of the "%%Parent and %%ALIAS" paths set in files and
 * allows them to be written to the dependency graph files in each ICU data directory.
 */
final class DependencyGraph {
    private final String cldrVersion;
    private final Map<String, String> parentMap = new TreeMap<>();
    private final Map<String, String> aliasMap = new TreeMap<>();

    public DependencyGraph(String cldrVersion) {
        this.cldrVersion = checkNotNull(cldrVersion);
    }

    void addParent(String localeId, String parentId) {
        // Aliases take priority (since they can be forced and will replace empty files). Note
        // however that this only happens in a tiny number of places due to the somewhat "hacky"
        // forced aliases, and in future it's perfectly possibly that there would never be an
        // overlap, and this code could just prohibit overlap between alias and parent mappings.
        if (!aliasMap.containsKey(localeId)) {
            parentMap.put(localeId, parentId);
        }
    }

    void addAlias(String sourceId, String targetId) {
        parentMap.remove(sourceId);
        aliasMap.put(sourceId, targetId);
    }

    /**
     * Outputs a JSON dictionary containing the parent and alias mappings to the given writer. The
     * output contains non-JSON line comments and is of the form:
     * <pre>{@code
     * // <copyright message>
     * {
     *     "cldrVersion": "<version>"
     *     "aliases": {
     *         "<source>": "<target>"
     *         ...
     *     }
     *     "parents": {
     *         "<id>": "<parent>"
     *         ...
     *     }
     * }
     * }</pre>
     * where all values (other than the version) are locale IDs.
     *
     * <p>Anything reading the produced files must strip the line comments prior to processing the
     * JSON data. Line comments only appear as a contiguous block in the header, so comment
     * processing can stop at the first non-comment line (i.e. the first bare '{').
     */
    void writeJsonTo(PrintWriter out, List<String> fileHeader) {
        fileHeader.forEach(s -> out.println("// " + s));
        out.println();
        out.format("{\n    \"cldrVersion\": \"%s\"", cldrVersion);
        writeMap(out, "aliases", aliasMap);
        writeMap(out, "parents", parentMap);
        out.append("\n}\n");
        out.close();
    }

    private static void writeMap(PrintWriter out, String name, Map<String, String> map) {
        if (!map.isEmpty()) {
            out.append(
                map.entrySet().stream()
                    .map(e -> String.format("\n        \"%s\": \"%s\"", e.getKey(), e.getValue()))
                    .collect(joining(",", ",\n    \"" + name + "\": {", "\n    }")));
        }
    }
}
