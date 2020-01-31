// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;

/**
 * Mutable ICU data, represented as a mapping from resource bundle paths to a sequence of values.
 */
public final class IcuData {
    private static final RbPath RB_VERSION = RbPath.of("Version");

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

    /**
     * Sets the value of the "/Version" path to be the given string, replacing any previous value.
     */
    public void setVersion(String versionString) {
        replace(RB_VERSION, versionString);
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
