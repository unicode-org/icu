/**
*******************************************************************************
* Copyright (C) 2006-2010, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
*******************************************************************************
*/ 
package com.ibm.icu.charset;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;

final class UConverterAlias {
    static final int UNNORMALIZED = 0;

    static final int STD_NORMALIZED = 1;

    static final int AMBIGUOUS_ALIAS_MAP_BIT = 0x8000;
    
    static final int CONTAINS_OPTION_BIT = 0x4000;

    static final int CONVERTER_INDEX_MASK = 0xFFF;

    static final int NUM_RESERVED_TAGS = 2;

    static final int NUM_HIDDEN_TAGS = 1;

    static int[] gConverterList = null;

    static int[] gTagList = null;

    static int[] gAliasList = null;

    static int[] gUntaggedConvArray = null;

    static int[] gTaggedAliasArray = null;

    static int[] gTaggedAliasLists = null;

    static int[] gOptionTable = null;

    static byte[] gStringTable = null;

    static byte[] gNormalizedStringTable = null;

    static final String GET_STRING(int idx) {
        return extractString(gStringTable, 2 * idx);
    }

    private static final String GET_NORMALIZED_STRING(int idx) {
        return extractString(gNormalizedStringTable, 2 * idx);
    }

    private static final String extractString(byte[] sArray, int sBegin) {
        char[] buf = new char[strlen(sArray, sBegin)];
        for (int i = 0; i < buf.length; i++) {
           buf[i] = (char)(sArray[sBegin + i] & 0xff);
        }
        return new String(buf);
    }

    private static final int strlen(byte[] sArray, int sBegin)
    {
        int i = sBegin;
        while(i < sArray.length && sArray[i++] != 0) {}
        return i - sBegin - 1;
    }

    /*private*/ static final int tocLengthIndex = 0;

    private static final int converterListIndex = 1;

    private static final int tagListIndex = 2;

    private static final int aliasListIndex = 3;

    private static final int untaggedConvArrayIndex = 4;

    private static final int taggedAliasArrayIndex = 5;

    private static final int taggedAliasListsIndex = 6;

    private static final int optionTableIndex = 7;

    private static final int stringTableIndex = 8;

    private static final int normalizedStringTableIndex = 9;

    private static final int minTocLength = 9; /*
                                                 * min. tocLength in the file,
                                                 * does not count the
                                                 * tocLengthIndex!
                                                 */

    private static final int offsetsCount = minTocLength + 1; /*
                                                                 * length of the
                                                                 * swapper's
                                                                 * temporary
                                                                 * offsets[]
                                                                 */

    static ByteBuffer gAliasData = null;

    private static final boolean isAlias(String alias) {
        if (alias == null) {
            throw new IllegalArgumentException("Alias param is null!");
        }
        return (alias.length() != 0);
    }

    private static final String CNVALIAS_DATA_FILE_NAME = ICUResourceBundle.ICU_BUNDLE + "/cnvalias.icu";

    /**
     * Default buffer size of datafile
     */
    private static final int CNVALIAS_DATA_BUFFER_SIZE = 25000;

    private static final synchronized boolean haveAliasData() 
                                               throws IOException{
        boolean needInit;

        // agljport:todo umtx_lock(NULL);
        needInit = gAliasData == null;

        /* load converter alias data from file if necessary */
        if (needInit) {
            ByteBuffer data = null;
            int[] tableArray = null;
            int tableStart;
            //byte[] reservedBytes = null;

            InputStream i = ICUData.getRequiredStream(CNVALIAS_DATA_FILE_NAME);
            BufferedInputStream b = new BufferedInputStream(i, CNVALIAS_DATA_BUFFER_SIZE);
            UConverterAliasDataReader reader = new UConverterAliasDataReader(b);
            tableArray = reader.readToc(offsetsCount);

            tableStart = tableArray[0];
            if (tableStart < minTocLength) {
                throw new IOException("Invalid data format.");
            }
            gConverterList = new int[tableArray[converterListIndex]];
            gTagList= new int[tableArray[tagListIndex]];
            gAliasList = new int[tableArray[aliasListIndex]];
            gUntaggedConvArray = new int[tableArray[untaggedConvArrayIndex]];
            gTaggedAliasArray = new int[tableArray[taggedAliasArrayIndex]];
            gTaggedAliasLists = new int[tableArray[taggedAliasListsIndex]];
            gOptionTable = new int[tableArray[optionTableIndex]];
            gStringTable = new byte[tableArray[stringTableIndex]*2];
            gNormalizedStringTable = new byte[tableArray[normalizedStringTableIndex]*2];

            reader.read(gConverterList, gTagList,
                    gAliasList, gUntaggedConvArray,
                    gTaggedAliasArray, gTaggedAliasLists,
                    gOptionTable, gStringTable, gNormalizedStringTable);
            data =  ByteBuffer.allocate(0); // dummy UDataMemory object in absence
                                        // of memory mapping

            if (gOptionTable[0] != STD_NORMALIZED) {
                throw new IOException("Unsupported alias normalization");
            }
            
            // agljport:todo umtx_lock(NULL);
            if (gAliasData == null) {
                gAliasData = data;
                data = null;

                // agljport:fix ucln_common_registerCleanup(UCLN_COMMON_IO,
                // io_cleanup);
            }
            // agljport:todo umtx_unlock(NULL);

            /* if a different thread set it first, then close the extra data */
            if (data != null) {
                // agljport:fix udata_close(data); /* NULL if it was set
                // correctly */
            }
        }

        return true;
    }

    // U_CFUNC const char * io_getConverterName(const char *alias, UErrorCode
    // *pErrorCode)
//    public static final String io_getConverterName(String alias)
//                                    throws IOException{
//        if (haveAliasData() && isAlias(alias)) {
//            boolean[] isAmbigous = new boolean[1];
//            int convNum = findConverter(alias, isAmbigous);
//            if (convNum < gConverterList.length) {
//                return GET_STRING(gConverterList[(int) convNum]);
//            }
//            /* else converter not found */
//        }
//        return null;
//    }

    /*
     * search for an alias return the converter number index for gConverterList
     */
    // static U_INLINE uint32_t findConverter(const char *alias, UErrorCode
    // *pErrorCode)
    private static final int findConverter(String alias, boolean[] isAmbigous) {
        int mid, start, limit;
        int lastMid;
        int result;
        StringBuilder strippedName = new StringBuilder();
        String aliasToCompare;

        stripForCompare(strippedName, alias);
        alias = strippedName.toString();

        /* do a binary search for the alias */
        start = 0;
        limit = gUntaggedConvArray.length;
        mid = limit;
        lastMid = Integer.MAX_VALUE;

        for (;;) {
            mid = (start + limit) / 2;
            if (lastMid == mid) { /* Have we moved? */
                break; /* We haven't moved, and it wasn't found. */
            }
            lastMid = mid;
            aliasToCompare = GET_NORMALIZED_STRING(gAliasList[mid]);
            result = alias.compareTo(aliasToCompare);

            if (result < 0) {
                limit = mid;
            } else if (result > 0) {
                start = mid;
            } else {
                /*
                 * Since the gencnval tool folds duplicates into one entry, this
                 * alias in gAliasList is unique, but different standards may
                 * map an alias to different converters.
                 */
                if ((gUntaggedConvArray[mid] & AMBIGUOUS_ALIAS_MAP_BIT) != 0) {
                    isAmbigous[0]=true;
                }
                /* State whether the canonical converter name contains an option.
                This information is contained in this list in order to maintain backward & forward compatibility. */
                /*if (containsOption) {
                    UBool containsCnvOptionInfo = (UBool)gMainTable.optionTable->containsCnvOptionInfo;
                    *containsOption = (UBool)((containsCnvOptionInfo
                        && ((gMainTable.untaggedConvArray[mid] & UCNV_CONTAINS_OPTION_BIT) != 0))
                        || !containsCnvOptionInfo);
                }*/
                return gUntaggedConvArray[mid] & CONVERTER_INDEX_MASK;
            }
        }
        return Integer.MAX_VALUE;
    }

    /**
     * stripForCompare Remove the underscores, dashes and spaces from
     * the name, and convert the name to lower case.
     * 
     * @param dst The destination buffer, which is <= the buffer of name.
     * @param name The alias to strip
     * @return the destination buffer.
     */
    public static final StringBuilder stripForCompare(StringBuilder dst, String name) {
        return io_stripASCIIForCompare(dst, name);
    }

    // enum {
    private static final byte IGNORE = 0;
    private static final byte ZERO = 1;
    private static final byte NONZERO = 2;
    static final byte MINLETTER = 3; /* any values from here on are lowercase letter mappings */
    // }
    
    /* character types for ASCII 00..7F */
    static final byte asciiTypes[] = new byte[] {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        ZERO, NONZERO, NONZERO, NONZERO, NONZERO, NONZERO, NONZERO, NONZERO, NONZERO, NONZERO, 0, 0, 0, 0, 0, 0,
        0, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6a, 0x6b, 0x6c, 0x6d, 0x6e, 0x6f,
        0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7a, 0, 0, 0, 0, 0,
        0, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6a, 0x6b, 0x6c, 0x6d, 0x6e, 0x6f,
        0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7a, 0, 0, 0, 0, 0
    };

    private static final char GET_CHAR_TYPE(char c) {
        return (char)((c < asciiTypes.length) ? asciiTypes[c] : (char)IGNORE);
    }
    
    /** @see UConverterAlias#compareNames */
    private static final StringBuilder io_stripASCIIForCompare(StringBuilder dst, String name) {
        int nameIndex = 0;
        char type, nextType;
        char c1;
        boolean afterDigit = false;

        while (nameIndex < name.length()) {
            c1 = name.charAt(nameIndex++);
            type = GET_CHAR_TYPE(c1);
            switch (type) {
            case IGNORE:
                afterDigit = false;
                continue; /* ignore all but letters and digits */
            case ZERO:
                if (!afterDigit && nameIndex < name.length()) {
                    nextType = GET_CHAR_TYPE(name.charAt(nameIndex));
                    if (nextType == ZERO || nextType == NONZERO) {
                        continue; /* ignore leading zero before another digit */
                    }
                }
                break;
            case NONZERO:
                afterDigit = true;
                break;
            default:
                c1 = type; /* lowercased letter */
                afterDigit = false;
                break;
            }
            dst.append(c1);
        }
        return dst;
    }

    /**
     * Do a fuzzy compare of a two converter/alias names. The comparison is
     * case-insensitive. It also ignores the characters '-', '_', and ' ' (dash,
     * underscore, and space). Thus the strings "UTF-8", "utf_8", and "Utf 8"
     * are exactly equivalent.
     * 
     * This is a symmetrical (commutative) operation; order of arguments is
     * insignificant. This is an important property for sorting the list (when
     * the list is preprocessed into binary form) and for performing binary
     * searches on it at run time.
     * 
     * @param name1
     *            a converter name or alias, zero-terminated
     * @param name2
     *            a converter name or alias, zero-terminated
     * @return 0 if the names match, or a negative value if the name1 lexically
     *         precedes name2, or a positive value if the name1 lexically
     *         follows name2.
     * 
     * @see UConverterAlias#stripForCompare
     */
    static int compareNames(String name1, String name2){
        int rc, name1Index = 0, name2Index = 0;
        char type, nextType;
        char c1 = 0, c2 = 0;
        boolean afterDigit1 = false, afterDigit2 = false;

        for (;;) {
            while (name1Index < name1.length()) {
                c1 = name1.charAt(name1Index++);
                type = GET_CHAR_TYPE(c1);
                switch (type) {
                case IGNORE:
                    afterDigit1 = false;
                    continue; /* ignore all but letters and digits */
                case ZERO:
                    if (!afterDigit1 && name1Index < name1.length()) {
                        nextType = GET_CHAR_TYPE(name1.charAt(name1Index));
                        if (nextType == ZERO || nextType == NONZERO) {
                            continue; /* ignore leading zero before another digit */
                        }
                    }
                    break;
                case NONZERO:
                    afterDigit1 = true;
                    break;
                default:
                    c1 = type; /* lowercased letter */
                    afterDigit1 = false;
                    break;
                }
                break; /* deliver c1 */
            }
            while (name2Index < name2.length()) {
                c2 = name2.charAt(name2Index++);
                type = GET_CHAR_TYPE(c2);
                switch (type) {
                case IGNORE:
                    afterDigit2 = false;
                    continue; /* ignore all but letters and digits */
                case ZERO:
                    if (!afterDigit2 && name1Index < name1.length()) {
                        nextType = GET_CHAR_TYPE(name2.charAt(name2Index));
                        if (nextType == ZERO || nextType == NONZERO) {
                            continue; /* ignore leading zero before another digit */
                        }
                    }
                    break;
                case NONZERO:
                    afterDigit2 = true;
                    break;
                default:
                    c2 = type; /* lowercased letter */
                    afterDigit2 = false;
                    break;
                }
                break; /* deliver c2 */
            }

            /* If we reach the ends of both strings then they match */
            if (name1Index >= name1.length() && name2Index >= name2.length()) {
                return 0;
            }

            /* Case-insensitive comparison */
            rc = (int)c1 - (int)c2;
            if (rc != 0) {
                return rc;
            }
        }
    }

    static int io_countAliases(String alias) 
                        throws IOException{
        if (haveAliasData() && isAlias(alias)) {
            boolean[] isAmbigous = new boolean[1];
            int convNum = findConverter(alias, isAmbigous);
            if (convNum < gConverterList.length) {
                /* tagListNum - 1 is the ALL tag */
                int listOffset = gTaggedAliasArray[(gTagList.length - 1)
                        * gConverterList.length + convNum];

                if (listOffset != 0) {
                    return gTaggedAliasLists[listOffset];
                }
                /* else this shouldn't happen. internal program error */
            }
            /* else converter not found */
        }
        return 0;
    }

    /**
     * Return the number of all aliases (and converter names).
     * 
     * @return the number of all aliases
     */
    // U_CFUNC uint16_t io_countTotalAliases(UErrorCode *pErrorCode);
//    static int io_countTotalAliases() throws IOException{
//        if (haveAliasData()) {
//            return (int) gAliasList.length;
//        }
//        return 0;
//    }

    // U_CFUNC const char * io_getAlias(const char *alias, uint16_t n,
    // UErrorCode *pErrorCode)
    static String io_getAlias(String alias, int n) throws IOException{
        if (haveAliasData() && isAlias(alias)) {
            boolean[] isAmbigous = new boolean[1];
            int convNum = findConverter(alias,isAmbigous);
            if (convNum < gConverterList.length) {
                /* tagListNum - 1 is the ALL tag */
                int listOffset = gTaggedAliasArray[(gTagList.length - 1)
                        * gConverterList.length + convNum];

                if (listOffset != 0) {
                    //int listCount = gTaggedAliasListsArray[listOffset];
                    /* +1 to skip listCount */
                    int[] currListArray = gTaggedAliasLists;
                    int currListArrayIndex = listOffset + 1;

                    return GET_STRING(currListArray[currListArrayIndex + n]);
                    
                }
                /* else this shouldn't happen. internal program error */
            }
            /* else converter not found */
        }
        return null;
    }

    // U_CFUNC uint16_t io_countStandards(UErrorCode *pErrorCode) {
//    static int io_countStandards() throws IOException{
//        if (haveAliasData()) {
//            return (int) (gTagList.length - NUM_HIDDEN_TAGS);
//        }
//        return 0;
//    }

    // U_CAPI const char * U_EXPORT2getStandard(uint16_t n, UErrorCode
    // *pErrorCode)
//    static String getStandard(int n) throws IOException{
//        if (haveAliasData()) {
//            return GET_STRING(gTagList[n]);
//        }
//        return null;
//    }

    // U_CAPI const char * U_EXPORT2 getStandardName(const char *alias, const
    // char *standard, UErrorCode *pErrorCode)
    static final String getStandardName(String alias, String standard)throws IOException {
        if (haveAliasData() && isAlias(alias)) {
            int listOffset = findTaggedAliasListsOffset(alias, standard);

            if (0 < listOffset && listOffset < gTaggedAliasLists.length) {
                int[] currListArray = gTaggedAliasLists;
                int currListArrayIndex = listOffset + 1;
                if (currListArray[0] != 0) {
                    return GET_STRING(currListArray[currListArrayIndex]);
                }
            }
        }
        return null;
    }

    // U_CAPI uint16_t U_EXPORT2 countAliases(const char *alias, UErrorCode
    // *pErrorCode)
    static int countAliases(String alias) throws IOException{
        return io_countAliases(alias);
    }

    // U_CAPI const char* U_EXPORT2 getAlias(const char *alias, uint16_t n,
    // UErrorCode *pErrorCode)
    static String getAlias(String alias, int n) throws IOException{
        return io_getAlias(alias, n);
    }

    // U_CFUNC uint16_t countStandards(void)
//    static int countStandards()throws IOException{
//        return io_countStandards();
//    }
    
    /*returns a single Name from the list, will return NULL if out of bounds
     */
    static String getAvailableName (int n){
        try{
          if (0 <= n && n <= 0xffff) {
            String name = bld_getAvailableConverter(n);
            return name;
          }
        }catch(IOException ex){
            //throw away exception
        }
        return null;
    }
    // U_CAPI const char * U_EXPORT2 getCanonicalName(const char *alias, const
    // char *standard, UErrorCode *pErrorCode) {
    static String getCanonicalName(String alias, String standard) throws IOException{
        if (haveAliasData() && isAlias(alias)) {
            int convNum = findTaggedConverterNum(alias, standard);

            if (convNum < gConverterList.length) {
                return GET_STRING(gConverterList[convNum]);
            }
        }

        return null;
    }
    static int countAvailable (){
        try{
            return bld_countAvailableConverters();
        }catch(IOException ex){
            //throw away exception
        }
        return -1;
    }
        
    // U_CAPI UEnumeration * U_EXPORT2 openStandardNames(const char *convName,
    // const char *standard, UErrorCode *pErrorCode)
/*    static final UConverterAliasesEnumeration openStandardNames(String convName, String standard)throws IOException {
        UConverterAliasesEnumeration aliasEnum = null;
        if (haveAliasData() && isAlias(convName)) {
            int listOffset = findTaggedAliasListsOffset(convName, standard);

            
             * When listOffset == 0, we want to acknowledge that the converter
             * name and standard are okay, but there is nothing to enumerate.
             
            if (listOffset < gTaggedAliasLists.length) {

                UConverterAliasesEnumeration.UAliasContext context = new UConverterAliasesEnumeration.UAliasContext(listOffset, 0);
                aliasEnum = new UConverterAliasesEnumeration();
                aliasEnum.setContext(context);
            }
             else converter or tag not found 
        }
        return aliasEnum;
    }*/

    // static uint32_t getTagNumber(const char *tagname)
    private static int getTagNumber(String tagName) {
        if (gTagList != null) {
            int tagNum;
            for (tagNum = 0; tagNum < gTagList.length; tagNum++) {
                if (tagName.equals(GET_STRING(gTagList[tagNum]))) {
                    return tagNum;
                }
            }
        }

        return Integer.MAX_VALUE;
    }

    // static uint32_t findTaggedAliasListsOffset(const char *alias, const char
    // *standard, UErrorCode *pErrorCode)
    private static int findTaggedAliasListsOffset(String alias, String standard) {
        int idx;
        int listOffset;
        int convNum;
        int tagNum = getTagNumber(standard);
        boolean[] isAmbigous = new boolean[1];
        /* Make a quick guess. Hopefully they used a TR22 canonical alias. */
        convNum = findConverter(alias, isAmbigous);

        if (tagNum < (gTagList.length - NUM_HIDDEN_TAGS)
                && convNum < gConverterList.length) {
            listOffset = gTaggedAliasArray[tagNum
                    * gConverterList.length + convNum];
            if (listOffset != 0
                    && gTaggedAliasLists[listOffset + 1] != 0) {
                return listOffset;
            }
            if (isAmbigous[0]==true) {
                /*
                 * Uh Oh! They used an ambiguous alias. We have to search the
                 * whole swiss cheese starting at the highest standard affinity.
                 * This may take a while.
                 */

                for (idx = 0; idx < gTaggedAliasArray.length; idx++) {
                    listOffset = gTaggedAliasArray[idx];
                    if (listOffset != 0 && isAliasInList(alias, listOffset)) {
                        int currTagNum = idx / gConverterList.length;
                        int currConvNum = (idx - currTagNum
                                * gConverterList.length);
                        int tempListOffset = gTaggedAliasArray[tagNum
                                * gConverterList.length + currConvNum];
                        if (tempListOffset != 0
                                && gTaggedAliasLists[tempListOffset + 1] != 0) {
                            return tempListOffset;
                        }
                        /*
                         * else keep on looking We could speed this up by
                         * starting on the next row because an alias is unique
                         * per row, right now. This would change if alias
                         * versioning appears.
                         */
                    }
                }
                /* The standard doesn't know about the alias */
            }
            /* else no default name */
            return 0;
        }
        /* else converter or tag not found */

        return Integer.MAX_VALUE;
    }

    /* Return the canonical name */
    // static uint32_t findTaggedConverterNum(const char *alias, const char
    // *standard, UErrorCode *pErrorCode)
    private static int findTaggedConverterNum(String alias, String standard) {
        int idx;
        int listOffset;
        int convNum;
        int tagNum = getTagNumber(standard);
        boolean[] isAmbigous = new boolean[1];
        
        /* Make a quick guess. Hopefully they used a TR22 canonical alias. */
        convNum = findConverter(alias, isAmbigous);        

        if (tagNum < (gTagList.length - NUM_HIDDEN_TAGS)
                && convNum < gConverterList.length) {
            listOffset = gTaggedAliasArray[tagNum
                    * gConverterList.length + convNum];
            if (listOffset != 0 && isAliasInList(alias, listOffset)) {
                return convNum;
            }
            if (isAmbigous[0] == true) {
                /*
                 * Uh Oh! They used an ambiguous alias. We have to search one
                 * slice of the swiss cheese. We search only in the requested
                 * tag, not the whole thing. This may take a while.
                 */
                int convStart = (tagNum) * gConverterList.length;
                int convLimit = (tagNum + 1) * gConverterList.length;
                for (idx = convStart; idx < convLimit; idx++) {
                    listOffset = gTaggedAliasArray[idx];
                    if (listOffset != 0 && isAliasInList(alias, listOffset)) {
                        return idx - convStart;
                    }
                }
                /* The standard doesn't know about the alias */
            }
            /* else no canonical name */
        }
        /* else converter or tag not found */

        return Integer.MAX_VALUE;
    }

    // static U_INLINE UBool isAliasInList(const char *alias, uint32_t
    // listOffset)
    private static boolean isAliasInList(String alias, int listOffset) {
        if (listOffset != 0) {
            int currAlias;
            int listCount = gTaggedAliasLists[listOffset];
            /* +1 to skip listCount */
            int[] currList = gTaggedAliasLists;
            int currListArrayIndex = listOffset + 1;
            for (currAlias = 0; currAlias < listCount; currAlias++) {
                if (currList[currAlias + currListArrayIndex] != 0
                        && compareNames(
                                alias,
                                GET_STRING(currList[currAlias + currListArrayIndex])) == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    // begin bld.c
    static String[] gAvailableConverters = null;

    static int gAvailableConverterCount = 0;

    static byte[] gDefaultConverterNameBuffer; // [MAX_CONVERTER_NAME_LENGTH +
                                                // 1]; /* +1 for NULL */

    static String gDefaultConverterName = null;

    // static UBool haveAvailableConverterList(UErrorCode *pErrorCode)
    static boolean haveAvailableConverterList() throws IOException{
        if (gAvailableConverters == null) {
            int idx;
            int localConverterCount;
            String converterName;
            String[] localConverterList;

            if (!haveAliasData()) {
                return false;
            }

            /* We can't have more than "*converterTable" converters to open */
            localConverterList = new String[gConverterList.length];

            localConverterCount = 0;

            for (idx = 0; idx < gConverterList.length; idx++) {
                converterName = GET_STRING(gConverterList[idx]);
                //UConverter cnv = UConverter.open(converterName);
                //TODO: Fix me
                localConverterList[localConverterCount++] = converterName;
                
            }

            // agljport:todo umtx_lock(NULL);
            if (gAvailableConverters == null) {
                gAvailableConverters = localConverterList;
                gAvailableConverterCount = localConverterCount;
                /* haveData should have already registered the cleanup function */
            } else {
                // agljport:todo free((char **)localConverterList);
            }
            // agljport:todo umtx_unlock(NULL);
        }
        return true;
    }

    // U_CFUNC uint16_t bld_countAvailableConverters(UErrorCode *pErrorCode)
    static int bld_countAvailableConverters() throws IOException{
        if (haveAvailableConverterList()) {
            return gAvailableConverterCount;
        }
        return 0;
    }

    // U_CFUNC const char * bld_getAvailableConverter(uint16_t n, UErrorCode
    // *pErrorCode)
    static String bld_getAvailableConverter(int n) throws IOException{
        if (haveAvailableConverterList()) {
            if (n < gAvailableConverterCount) {
                return gAvailableConverters[n];
            }
        }
        return null;
    }

    /* default converter name --------------------------------------------------- */

    /*
     * In order to be really thread-safe, the get function would have to take
     * a buffer parameter and copy the current string inside a mutex block.
     * This implementation only tries to be really thread-safe while
     * setting the name.
     * It assumes that setting a pointer is atomic.
     */

    // U_CFUNC const char * getDefaultName()
//    static final synchronized String getDefaultName() {
//        /* local variable to be thread-safe */
//        String name;
//
//        //agljport:todo umtx_lock(null);
//        name = gDefaultConverterName;
//        //agljport:todo umtx_unlock(null);
//
//        if (name == null) {
//            //UConverter cnv = null;
//            int length = 0;
//
//            name = CharsetICU.getDefaultCharsetName();
//
//            /* if the name is there, test it out and get the canonical name with options */
//            if (name != null) {
//               // cnv = UConverter.open(name); 
//               // name = cnv.getName(cnv);
//                // TODO: fix me
//            }
//
//            if (name == null || name.length() == 0 ||/* cnv == null ||*/
//                     length >= gDefaultConverterNameBuffer.length) {
//                /* Panic time, let's use a fallback. */
//                name = new String("US-ASCII");
//            }
//
//            //length=(int32_t)(strlen(name));
//
//            /* Copy the name before we close the converter. */
//            name = gDefaultConverterName;
//        }
//
//        return name;
//    }

    //end bld.c
}