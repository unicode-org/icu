// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;

/**
 * Mutable ICU data, represented as a mapping from resource bundle paths to a sequence of values.
 */
public final class IcuData {
    private static final RbPath RB_VERSION = RbPath.of("Version");
    private static final Pattern ARRAY_INDEX = Pattern.compile("(/[^\\[]++)(?:\\[(\\d++)])?$");

    private final String name;
    private final boolean hasFallback;
    private final NavigableSet<RbPath> paths = new TreeSet<>();
    private final ListMultimap<RbPath, RbValue> rbPathToValues = ArrayListMultimap.create();
    private ImmutableList<String> commentLines = ImmutableList.of();

    /**
     * IcuData constructor.
     *
     * @param name The name of the IcuData object, used as the name of the root node in the output file
     * @param hasFallback true if the output file has another ICU file as a fallback.
     */
    public IcuData(String name, boolean hasFallback) {
        this.hasFallback = hasFallback;
        this.name = name;
    }

    /** @return whether data should fallback on data in other ICU files. */
    public boolean hasFallback() {
        return hasFallback;
    }

    /**
     * @return the name of this ICU data instance. Used in the output filename, and in comments.
     */
    public String getName() {
        return name;
    }

    /** Sets additional comment lines for the top of the file. */
    public void setFileComment(String... commentLines) {
        setFileComment(Arrays.asList(commentLines));
    }

    public void setFileComment(Iterable<String> commentLines) {
        this.commentLines = ImmutableList.copyOf(commentLines);
    }

    public List<String> getFileComment() {
        return commentLines;
    }

    /** Adds a singleton resource bundle value for a given path. */
    public void add(RbPath rbPath, String element) {
        add(rbPath, RbValue.of(element));
    }

    /** Adds a single resource bundle value for a given path. */
    public void add(RbPath rbPath, RbValue rbValue) {
        rbPathToValues.put(rbPath, rbValue);
        paths.add(rbPath);
    }

    /** Adds a sequence of resource bundle values for a given path. */
    public void add(RbPath rbPath, Iterable<RbValue> rbValues) {
        rbValues.forEach(v -> rbPathToValues.put(rbPath, v));
        paths.add(rbPath);
    }

    /** Replaces all resource bundle values for a given path with the specified singleton value. */
    public void replace(RbPath rbPath, String element) {
        rbPathToValues.removeAll(rbPath);
        rbPathToValues.put(rbPath, RbValue.of(element));
        paths.add(rbPath);
    }

    /** Replaces all resource bundle values for a given path with the specified value. */
    public void replace(RbPath rbPath, RbValue rbValue) {
        rbPathToValues.removeAll(rbPath);
        add(rbPath, rbValue);
    }

    public void setVersion(String versionString) {
        add(RB_VERSION, versionString);
    }

    public void addResults(ListMultimap<RbPath, PathValueTransformer.Result> resultsByRbPath) {
        for (RbPath rbPath : resultsByRbPath.keySet()) {
            for (PathValueTransformer.Result r : resultsByRbPath.get(rbPath)) {
                if (r.isGrouped()) {
                    // Grouped results have all the values in a single value entry.
                    add(rbPath, RbValue.of(r.getValues()));
                } else {
                    if (rbPath.getSegment(rbPath.length() - 1).endsWith(":alias")) {
                        r.getValues().forEach(v -> add(rbPath, RbValue.of(v)));
                    } else {
                        // Ungrouped results are one value per entry, but might be expanded into
                        // grouped results if they are a path referencing a grouped entry.
                        r.getValues().forEach(v -> add(rbPath, replacePathValues(v)));
                    }
                }
            }
        }
    }

    /**
     * Replaces an ungrouped CLDR value for the form "/foo/bar" or "/foo/bar[N]" which is assumed
     * to be a reference to an existing value in a resource bundle. Note that the referenced bundle
     * might be grouped (i.e. an array with more than one element).
     */
    private RbValue replacePathValues(String value) {
        Matcher m = ARRAY_INDEX.matcher(value);
        if (!m.matches()) {
            return RbValue.of(value);
        }
        // The only constraint is that the "path" value starts with a leading '/', but parsing into
        // the RbPath ignores this. We must use "parse()" here, rather than RbPath.of(), since the
        // captured value contains '/' characters to represent path delimiters.
        RbPath replacePath = RbPath.parse(m.group(1));
        List<RbValue> replaceValues = get(replacePath);
        checkArgument(replaceValues != null, "Path %s is missing from IcuData", replacePath);
        // If no index is given (e.g. "/foo/bar") then treat it as index 0 (i.e. "/foo/bar[0]").
        int replaceIndex = m.groupCount() > 1 ? Integer.parseInt(m.group(2)) : 0;
        return replaceValues.get(replaceIndex);
    }

    /**
     * Returns the mutable list of values associated with the given path (or null if there are no
     * associated values).
     */
    public List<RbValue> get(RbPath rbPath) {
        return paths.contains(rbPath) ? rbPathToValues.get(rbPath) : null;
    }

    /** Returns an unmodifiable view of the set of paths in this instance. */
    public Set<RbPath> getPaths() {
        return Collections.unmodifiableSet(paths);
    }

    /** Returns whether the given path is present in this instance. */
    public boolean contains(RbPath rbPath) {
        return paths.contains(rbPath);
    }

    /** Returns whether there are any paths in this instance. */
    public boolean isEmpty() {
        return paths.isEmpty();
    }

    @Override public String toString() {
        StringWriter out = new StringWriter();
        PrintWriter w = new PrintWriter(out);
        w.format("IcuData{ name=%s, fallback=%s\n", name, hasFallback);
        commentLines.forEach(c -> w.format("  # %s\n", c));
        paths.forEach(p -> {
            w.format("  %s:\n", p);
            rbPathToValues.get(p).forEach(v -> w.format("    %s\n", v));
        });
        w.format("}\n");
        w.flush();
        return out.toString();
    }
}
