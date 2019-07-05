package com.ibm.icu.dev.tool.cldr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper class for converted ICU data which maps resource bundle paths to values.
 */
final class IcuData {
    private boolean hasFallback;
    private String sourceFile;
    private String name;
    private Map<String, List<String[]>> rbPathToValues;
    private String comment;
    private Map<String, String> enumMap;

    /**
     * IcuData constructor.
     *
     * @param sourceFile the source file of the IcuData object, displayed in  comments in the file.
     * @param name The name of the IcuData object, used as the name of the root node in the output file
     * @param hasFallback true if the output file has another ICU file as a fallback.
     */
    public IcuData(String sourceFile, String name, boolean hasFallback) {
        this(sourceFile, name, hasFallback, new HashMap<String, String>());
    }

    /**
     * IcuData constructor.
     *
     * @param sourceFile the source file of the IcuData object, displayed in  comments in the file.
     * @param name The name of the IcuData object, used as the name of the root node in the output file
     * @param hasFallback true if the output file has another ICU file as a fallback.
     * @param enumMap a mapping of CLDR string values to their integer values in ICU
     */
    // TODO: Deprecate and remove this method, since arbitrary enum conversion for any value is
    // potentially unsafe. Enum conversion should be handled by the transformation logic.
    public IcuData(String sourceFile, String name, boolean hasFallback, Map<String, String> enumMap) {
        this.hasFallback = hasFallback;
        this.sourceFile = sourceFile;
        this.name = name;
        rbPathToValues = new HashMap<String, List<String[]>>();
        this.enumMap = enumMap;
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

    /**
     * Sets an additional comment string (possibly containing newlines) for the top of the file.
     */
    public void setFileComment(String comment) {
        this.comment = comment;
    }

    public String getFileComment() {
        return comment;
    }

    /** Adds a series of values for the specified resource bundle path. */
    public void add(String path, String... values) {
        rbPathToValues
            .computeIfAbsent(path, p -> new ArrayList<>())
            .add(normalizeValues(path, values));
    }

    /** Replaces a series of values for the specified resource bundle path. */
    public void replace(String path, String... values) {
        List<String[]> list = new ArrayList<String[]>(1);
        rbPathToValues.put(path, list);
        list.add(normalizeValues(path, values));
    }

    private String[] normalizeValues(String rbPath, String[] values) {
        if (isIntRbPath(rbPath)) {
            List<String> normalizedValues = new ArrayList<String>();
            for (String curValue : values) {
                normalizedValues.add(enumMap.getOrDefault(curValue, curValue));
            }
            return normalizedValues.toArray(values);
        } else {
            return values;
        }
    }

    public Set<String> keySet() {
        return rbPathToValues.keySet();
    }

    public boolean containsKey(String key) {
        return rbPathToValues.containsKey(key);
    }

    public List<String[]> get(String path) {
        return rbPathToValues.get(path);
    }

    /** Returns whether a resource bundle path is specified as holding an integer sequence. */
    public static boolean isIntRbPath(String rbPath) {
        return rbPath.endsWith(":int") || rbPath.endsWith(":intvector");
    }
}
