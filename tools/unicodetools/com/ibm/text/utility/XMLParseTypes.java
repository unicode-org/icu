package com.ibm.text.utility;

/** Interface of values for use with XMLParse.
 * Others classes can "implements" this also, to avoid typing XMLParseTypes.XXX
 */
public interface XMLParseTypes {

    /** Kind values, for XMLParse.getKind(), next()
     */
    public static final byte
        DONE = 0,
        ELEMENT_TAG = 1, ELEMENT_TAG_SLASH = 2, ELEMENT_TAG_COMMENT = 3, ELEMENT_TAG_QUESTION = 4,
        END_ELEMENT = 5, END_ELEMENT_SLASH = 6, END_ELEMENT_COMMENT = 7, END_ELEMENT_QUESTION = 8,
        ATTRIBUTE_TAG = 9, ATTRIBUTE_VALUE = 10,
        TEXT = 11;

    /** Flag masks for XMLParse.quote(x, flags). Use '|' to combine
     */
    public static final byte
        QUOTE_NON_ASCII = 1,
        QUOTE_ASCII = 2,
        QUOTE_IEBUG = 4,
        QUOTE_TABCRLF = 8,
        QUOTE_DECIMAL = 16;

    /** For Debugging
     */
    static final String[] kindNames = {
        "DONE",
        "ELEMENT_TAG", "ELEMENT_TAG_SLASH", "ELEMENT_TAG_COMMENT", "ELEMENT_TAG_QUESTION",
        "END_ELEMENT", "END_ELEMENT_SLASH", "END_ELEMENT_COMMENT", "END_ELEMENT_QUESTION",
        "ATTRIBUTE_TAG", "ATTRIBUTE_VALUE",
        "TEXT",
        };
}