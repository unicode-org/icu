/*
 * (C) Copyright IBM Corp. 1998-2004.  All Rights Reserved.
 *
 * The program is provided "as is" without any warranty express or
 * implied, including the warranty of non-infringement and the implied
 * warranties of merchantibility and fitness for a particular purpose.
 * IBM will not be liable for any damages suffered by you as a result
 * of using the Program. In no event will IBM be liable for any
 * special, indirect or consequential damages or lost profits even if
 * IBM has been advised of the possibility of their occurrence. IBM
 * will not be liable for any third party claims against you.
 */
package com.ibm.richtext.test.unit;

import com.ibm.icu.dev.test.TestFmwk;

import com.ibm.richtext.textlayout.attributes.AttributeSet;
import com.ibm.richtext.textlayout.attributes.TextAttribute;
import com.ibm.richtext.textlayout.attributes.AttributeMap;
import java.util.Enumeration;

// Java2 imports
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;


public class TestAttributeMap extends TestFmwk  {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";


    // There are JDK 1.1 versions of AttributeMap and AttributeSet.
    // Some of the tests in this class require Java 2 API's.  I have
    // tried to isolate these tests by conditionalizing them on
    // this static variable.  If you are back-porting to 1.1, remove
    // the Java 2 tests ONLY.
    private static final boolean gJDK11 = false;

    public static void main(String[] args) throws Exception {

        new TestAttributeMap().run(args);
    }

    private AttributeSet maps;  // A Set of AttributeMaps
    private AttributeSet sets;  // A Set of Sets

    private static final class TestAttribute extends TextAttribute {

        TestAttribute(String name) {
            super(name);
        }
    }

    private static final TestAttribute[] attributes = {
        new TestAttribute("0"), new TestAttribute("1"), new TestAttribute("2")
    };

    private static final Object[] values = {
        "Hello world", new Float(-42), new Object(), new AttributeMap(new TestAttribute("3"), "HH")
    };

    /**
     * Returns lhs.equals(rhs) - but also checks for symmetry, and
     * consistency with hashCode().
     */
    private boolean equalMaps(AttributeMap lhs, Object rhs) {

        boolean equal = lhs.equals(rhs);
        if (equal != (rhs.equals(lhs))) {
            errln("AttributeMap.equals is not symetric");
        }
        if (equal) {
            if (lhs.hashCode() != rhs.hashCode()) {
                errln("AttributeMaps are equal but hashCodes differ");
            }
        }
        return equal;
    }

    public TestAttributeMap() {

        maps = AttributeSet.EMPTY_SET;
        maps = maps.addElement(AttributeMap.EMPTY_ATTRIBUTE_MAP);
        maps.addElement(new AttributeMap(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUB));
        maps.addElement(new AttributeMap(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUPER));

        for (int i=0; i < attributes.length; i++) {
            for (int j=0; j < values.length; j++) {
                maps = maps.addElement(new AttributeMap(attributes[i], values[j]));
            }
        }

        AttributeMap bigMap = new AttributeMap(new TestAttribute("4"), "value");
        for (int i=0; i < Math.min(attributes.length, values.length); i++) {
            bigMap = bigMap.addAttribute(attributes[i], values[values.length-i-1]);
        }
        maps = maps.addElement(bigMap);

        sets = AttributeSet.EMPTY_SET;

        sets = new AttributeSet(AttributeSet.EMPTY_SET);

        for (int i=0; i < attributes.length; i++) {
            AttributeSet newSet = new AttributeSet(attributes[i]);
            sets = sets.addElement(newSet);
        }

        AttributeSet allAttrs = AttributeSet.EMPTY_SET;
        for (int i=0; i < attributes.length; i++) {
            allAttrs = allAttrs.addElement(attributes[i]);
        }

        sets = sets.addElement(allAttrs);
    }

    /**
     * Run tests on AttributeMap.  If a test fails an exception will propogate out
     * of this method.
     */
    public void test() {

        easyTests();

        Enumeration mapIter = maps.elements();
        while (mapIter.hasMoreElements()) {

            AttributeMap testMap = (AttributeMap) mapIter.nextElement();

            _testModifiers(testMap);
            _testViews(testMap);

            Enumeration unionIter = maps.elements();
            while (unionIter.hasMoreElements()) {
                _testUnionWith(testMap, (AttributeMap) unionIter.nextElement());
            }

            Enumeration setIter = sets.elements();
            while (setIter.hasMoreElements()) {
                AttributeSet testSet = (AttributeSet) setIter.nextElement();
                _testIntersectWith(testMap, testSet);
                _testRemoveAttributes(testMap, testSet);
            }
        }
    }

    /**
     * Invoke modifiers on map.  All should throw
     * UnsupportedOperationException, and leave map unmodified.
     */
    void _testModifiers(AttributeMap map) {

        if (gJDK11) {
            return;
        }
        
        AttributeMap originalMap = new AttributeMap(map);

        try {
            map.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
            errln("Put should throw UnsupportedOperationException.");
        }
        catch(UnsupportedOperationException e) {
            System.out.print("");
        }

        try {
            Object key = TextAttribute.WEIGHT;
            Iterator iter = map.keySet().iterator();
            if (iter.hasNext()) {
                key = iter.next();
            }
            map.remove(key);
            errln("Set should throw UnsupportedOperationException.");
        }
        catch(UnsupportedOperationException e) {
            System.out.print("");
        }

        try {
            map.putAll(map);
            errln("putAll should throw UnsupportedOperationException.");
        }
        catch(UnsupportedOperationException e) {
            System.out.print("");
        }

        try {
            map.clear();
            errln("clear should throw UnsupportedOperationException.");
        }
        catch(UnsupportedOperationException e) {
            System.out.print("");
        }

        if (!originalMap.equals(map)) {
            errln("Modifiers changed map.");
        }
    }

    /**
     * Ensure that map.addAttributes(addMap) is equivalent to calling
     * map.add on all of addMap's entries.
     */
    void _testUnionWith(AttributeMap map, AttributeMap addMap) {

        AttributeMap lhs = map.addAttributes(addMap);

        AttributeMap rhs = map;

        Enumeration iter = addMap.getKeySet().elements();
        while (iter.hasMoreElements()) {
            Object attr = iter.nextElement();
            Object value = addMap.get(attr);
            rhs = rhs.addAttribute(attr, value);
        }

        if (!equalMaps(lhs, rhs)) {
            errln("Maps are not equal.");
        }
    }

    /**
     * Ensure that map.removeAttributes(remove) is equivalent to calling
     * map.removeAttribute on remove's elements.
     */
    void _testRemoveAttributes(AttributeMap map, AttributeSet remove) {

        AttributeMap lhs = map.removeAttributes(remove);

        AttributeMap rhs = map;

        Enumeration iter = remove.elements();
        while (iter.hasMoreElements()) {
            Object attr = iter.nextElement();
            rhs = rhs.removeAttribute(attr);
        }

        if (!equalMaps(lhs, rhs)) {
            errln("Maps are not equal.");
        }
    }

    /**
     * Ensure that map.intersectWith(intersect) is equivalent to
     * map.removeAttributes(map.keySet() - intersect);
     */
    void _testIntersectWith(AttributeMap map, AttributeSet intersect) {

        AttributeMap lhs = map.intersectWith(intersect);

        AttributeSet keySet = map.getKeySet();
        AttributeSet removeSet = keySet.subtract(intersect);
        AttributeMap rhs = map.removeAttributes(removeSet);

        if (!equalMaps(lhs, rhs)) {
            map.intersectWith(intersect);
            logln("intersect: " + intersect);
            logln("keySet: " + keySet);
            logln("removeSet: " + removeSet);
            logln("map: " + map);
            logln("lhs: " + lhs);
            logln("rhs: " + rhs);
            errln("Maps are not equal.");
        }
    }

    /**
     * Ensure that:
     *    map, map.keySet(), and map.entrySet() are the same size;
     *    map.containsKey() is true for every key in keySet();
     *    map.containsValue() is true for every value in values;
     *    every entry key is in keySet, every entry value is in map.values();
     *    map.get() is consistent with entry's key, value;
     *    sum of hashcodes of entries equals map.hashCode().
     */
    void _testViews(AttributeMap map) {

        AttributeSet keySet = map.getKeySet();

        Enumeration keyIter = keySet.elements();
        while (keyIter.hasMoreElements()) {
            if (!map.containsKey(keyIter.nextElement())) {
                errln("keySet contains key not in map");
            }
        }

        if (gJDK11) {
            return;
        }
        
        Collection values = map.values();
        Set entrySet = map.entrySet();

        if (keySet.size() != map.size() || entrySet.size() != map.size()) {
            errln("Set sizes are inconsistent with map size.");
        }

        int hashCode = 0;

        Iterator valueIter = values.iterator();
        while (valueIter.hasNext()) {
            if (!map.containsValue(valueIter.next())) {
                errln("value set contains value not in map");
            }
        }

        Iterator entryIter = entrySet.iterator();
        while (entryIter.hasNext()) {

            Entry entry = (Entry) entryIter.next();

            Object key = entry.getKey();
            if (!keySet.contains(key)) {
                errln("Entry key is not in key set.");
            }

            Object value = map.get(entry.getKey());
            if (!values.contains(value)) {
                errln("Entry value is not in value set.");
            }

            if (map.get(key) != value) {
                errln("map.get did not return entry value.");
            }

            hashCode += entry.hashCode();
        }

        if (hashCode != map.hashCode()) {
            errln("map hashcode is not sum of entry hashcodes.");
        }
    }

    /**
     * Look for correct behavior in obvious cases.
     */
    void easyTests() {

        AttributeMap map = new AttributeMap();
        if (!map.equals(AttributeMap.EMPTY_ATTRIBUTE_MAP)) {
            errln("Default-constructed map is not equal to empty map.");
        }

        map = map.addAttribute(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
        Object otherMap = new AttributeMap(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
        if (!map.equals(otherMap)) {
            errln("Maps are inconsistent after map.add");
        }

        otherMap = map.addAttributes(map);
        if (!equalMaps(map,otherMap)) {
            errln("Maps are inconsistent after addAttributes");
        }

        map = map.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);

        if (map.size() != 2) {
            errln("Map size is wrong.  map="+map);
        }

        if (equalMaps(map,otherMap)) {
            errln("Maps should not be equal");
        }

        Object posture = new Float(0);
        map = map.addAttribute(TextAttribute.POSTURE, posture);

        if (map.size() != 2) {
            errln("Map size is wrong");
        }

        if (!map.get(TextAttribute.POSTURE).equals(posture)) {
            errln("Map element is wrong");
        }

        map = map.removeAttribute(TextAttribute.UNDERLINE);

        if (map.size() != 1) {
            errln("Map size is wrong");
        }

        if (map.get(TextAttribute.UNDERLINE) != null) {
            errln("Map should not have element");
        }

        // map has POSTURE_REGULAR.  If we addAttributes a map with
        // POSTURE_ITALIC the new map should have POSTURE_ITALIC

        map = map.addAttributes(new AttributeMap(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE));
        if (map.get(TextAttribute.POSTURE) != TextAttribute.POSTURE_OBLIQUE) {
            errln("Map element is wrong");
        }

        _testModifiers(map);
        _testViews(map);

        Enumeration mapIter = maps.elements();
        while (mapIter.hasMoreElements()) {
            AttributeMap testMap = (AttributeMap) mapIter.nextElement();
            Object newValue = new Object();
            AttributeMap newMap = testMap.addAttribute(attributes[0], newValue);
            if (newMap.get(attributes[0]) != newValue) {
                errln("Did not get expected value back.  map=" + map);
            }
        }
    }
}