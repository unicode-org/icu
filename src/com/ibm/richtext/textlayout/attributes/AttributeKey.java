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
package com.ibm.richtext.textlayout.attributes;

import java.io.Serializable;

/**
 * This class provides a cannonical mapping between fields in TextAttribute
 * and instances of itself.  It is used by AttributeMap to serialize
 * and deserialize TextAttribute to preserve uniqueness of TextAttribute
 * instances (ie so that TextAttribute instances remain singletons),
 * and to provide compatability between 1.1 and 1.2 versions of
 * TextAttribute.
 * <p>
 * Example use - instead of doing this:
 * <blockquote><pre>
 *     out.writeObject(anAttribute);
 * </pre></blockquote>
 * do this:
 * <blockquote><pre>
 *     out.writeObject(AttributeKey.mapAttributeToKey(anAttribute));
 * </pre></blockquote>
 * Similarly, instead of this:
 * <blockquote><pre>
 *     anAttribute = in.readObject();
 * </pre></blockquote>
 * do this:
 * <blockquote><pre>
 *     anAttribute = AttributeKey.mapKeyToAttribute(in.readObject());
 * </pre></blockquote>
 * <p>
 * If anAttribute is not a known TextAttribute, then <code>mapAttributeToKey</code>
 * will just return its argument.  Similarly, <code>mapKeyToAttribute</code> will
 * return its argument if the argument is not a known AttributeKey.
 */

/*public*/ final class AttributeKey implements Serializable {

/*
    In this implementation, two parallel Vectors are
    maintained.  TextAttribute(i) maps to AttributeKey(i).
    For compatability with existing data, this mapping must
    be maintained in the future!  So, when new attributes
    are added, add them to the end of the list.
*/
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private static final long serialVersionUID = 3772371253277107294L;

    private static Object[] fgTextAttributes;
    private static Object[] fgAttributeKeys;

    static {
        fgTextAttributes = new Object[] {
            TextAttribute.FONT,
            TextAttribute.FAMILY,
            TextAttribute.WEIGHT,
            TextAttribute.POSTURE,
            TextAttribute.SIZE,
            TextAttribute.SUPERSCRIPT,
            TextAttribute.FOREGROUND,
            TextAttribute.BACKGROUND,
            TextAttribute.UNDERLINE,
            TextAttribute.STRIKETHROUGH,
            TextAttribute.CHAR_REPLACEMENT,
            TextAttribute.EXTRA_LINE_SPACING,
            TextAttribute.FIRST_LINE_INDENT,
            TextAttribute.MIN_LINE_SPACING,
            TextAttribute.LINE_FLUSH,
            TextAttribute.LEADING_MARGIN,
            TextAttribute.TRAILING_MARGIN,
            TextAttribute.TAB_RULER,
            TextAttribute.RUN_DIRECTION,
            TextAttribute.BIDI_EMBEDDING,
            TextAttribute.JUSTIFICATION,
        };

        final int attrCount = fgTextAttributes.length;
        fgAttributeKeys = new Object[attrCount];

        for (int i=0; i < attrCount; i += 1) {
            fgAttributeKeys[i] = new AttributeKey(i);
        }
    }

    /**
     * Return the TextAttribute corresponding to the given key.
     * If key is an instance of AttributeKey it will be mapped to
     * a TextAttribute.  Otherwise, the key is returned.
     * @param key the key to map to a TextAttribute field
     * @return the TextAttribute for <code>key</code> if <code>key</code>
     *    is an AttributeKey; otherwise <code>key</code> is returned
     */
    /*public*/ static Object mapKeyToAttribute(Object key) {

        try {
            AttributeKey aKey = (AttributeKey) key;
            if (aKey.fId < fgTextAttributes.length) {
                return fgTextAttributes[aKey.fId];
            }
            else {
                return key;
            }
        }
        catch(ClassCastException e) {
            return key;
        }
    }

    /**
     * If attribute is a known TextAttribute, return an AttributeKey
     * for it.  Otherwise the object is returned.
     * @param attribute the attribute to map to an AttributeKey
     * @return an AttributeKey for <code>attribute</code>
     *     if <code>attribute</code> is a known attribute; otherwise
     *     <code>attribute</code> is returned
     */
    /*public*/ static Object mapAttributeToKey(Object attribute) {

        final int attrCount = fgTextAttributes.length;
        
        for (int index = 0; index < attrCount; index += 1) {
            if (fgTextAttributes[index].equals(attribute)) {
                return fgAttributeKeys[index];
            }
        }
        
        return attribute;
    }


    private int fId;

    private AttributeKey(int id) {

        fId = id;
    }

    public boolean equals(Object rhs) {

        try {
            return ((AttributeKey)rhs).fId == fId;
        }
        catch(ClassCastException e) {
            return false;
        }
    }

    public int hashCode() {

        return fId;
    }
}