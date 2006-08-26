/**
*******************************************************************************
* Copyright (C) 2006, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
*******************************************************************************
*/ 
package com.ibm.icu.impl;

import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.ibm.icu.charset.CharsetICU;

public final class UConverterAlias {
    /** The largest value a 32 bit unsigned integer can hold @draft ICU 3.6 */
    public static final long UINT32_MAX = 4294967295L;

    public static final int AMBIGUOUS_ALIAS_MAP_BIT = 0x8000;

    public static final int CONVERTER_INDEX_MASK = 0xFFF;

    public static final int NUM_RESERVED_TAGS = 2;

    public static final int NUM_HIDDEN_TAGS = 1;

    static int[] gConverterListArray = null;

    static int gConverterListArrayIndex;

    static int[] gTagListArray = null;

    static int gTagListArrayIndex;

    static int[] gAliasListArray = null;

    static int gAliasListArrayIndex;

    static int[] gUntaggedConvArrayArray = null;

    static int gUntaggedConvArrayArrayIndex;

    static int[] gTaggedAliasArrayArray = null;

    static int gTaggedAliasArrayArrayIndex;

    static int[] gTaggedAliasListsArray = null;

    static int gTaggedAliasListsArrayIndex;

    static byte[] gStringTableArray = null;

    static int gStringTableArrayIndex;

    static long gConverterListSize;

    static long gTagListSize;

    static long gAliasListSize;

    static long gUntaggedConvArraySize;

    static long gTaggedAliasArraySize;

    static long gTaggedAliasListsSize;

    static long gStringTableSize;

    static final String GET_STRING(int idx) {
        return new String(gStringTableArray, 2 * idx, (int) strlen(gStringTableArray, 2 * idx));
    }

    public static final int strlen(byte[] sArray, int sBegin)
    {
        int i = sBegin;
        while(i < sArray.length && sArray[i++] != 0) {}
        return i - sBegin - 1;
    }

    public static final int tocLengthIndex = 0;

    public static final int converterListIndex = 1;

    public static final int tagListIndex = 2;

    public static final int aliasListIndex = 3;

    public static final int untaggedConvArrayIndex = 4;

    public static final int taggedAliasArrayIndex = 5;

    public static final int taggedAliasListsIndex = 6;

    public static final int reservedIndex1 = 7;

    public static final int stringTableIndex = 8;

    public static final int minTocLength = 8; /*
                                                 * min. tocLength in the file,
                                                 * does not count the
                                                 * tocLengthIndex!
                                                 */

    public static final int offsetsCount = minTocLength + 1; /*
                                                                 * length of the
                                                                 * swapper's
                                                                 * temporary
                                                                 * offsets[]
                                                                 */

    static ByteBuffer gAliasData = null;

    private static final boolean isAlias(String alias) {
        if (alias == null) {
            throw new IllegalArgumentException("Alias param is null!");
        } else if (alias.length() == 0) {
            return false;
        } else {
            return true;
        }
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
            long[] tableArray = null;
            long tableStart;
            long reservedSize1;
            byte[] reservedBytes = null;

            // agljport:fix data = udata_openChoice(NULL, DATA_TYPE, DATA_NAME,
            // isAcceptable, NULL, pErrorCode);
            // data = udata_openChoice(null, DATA_TYPE, DATA_NAME, 0,
            // isAcceptable, null, pErrorCode);
            InputStream i = ICUData.getRequiredStream(CNVALIAS_DATA_FILE_NAME);
            BufferedInputStream b = new BufferedInputStream(i, CNVALIAS_DATA_BUFFER_SIZE);
            UConverterAliasDataReader reader = new UConverterAliasDataReader(b);
            tableArray = reader.readToc(offsetsCount);

            tableStart = tableArray[0];
            if (tableStart < minTocLength) {
                throw new IOException("Invalid data format.");
            }
            gConverterListSize = tableArray[1];
            gTagListSize = tableArray[2];
            gAliasListSize = tableArray[3];
            gUntaggedConvArraySize = tableArray[4];
            gTaggedAliasArraySize = tableArray[5];
            gTaggedAliasListsSize = tableArray[6];
            reservedSize1 = tableArray[7] * 2;
            gStringTableSize = tableArray[8] * 2;

            gConverterListArray = new int[(int) gConverterListSize];
            gTagListArray = new int[(int) gTagListSize];
            gAliasListArray = new int[(int) gAliasListSize];
            gUntaggedConvArrayArray = new int[(int) gUntaggedConvArraySize];
            gTaggedAliasArrayArray = new int[(int) gTaggedAliasArraySize];
            gTaggedAliasListsArray = new int[(int) gTaggedAliasListsSize];
            reservedBytes = new byte[(int) reservedSize1];
            gStringTableArray = new byte[(int) gStringTableSize];

            reader.read(gConverterListArray, gTagListArray,
                    gAliasListArray, gUntaggedConvArrayArray,
                    gTaggedAliasArrayArray, gTaggedAliasListsArray,
                    reservedBytes, gStringTableArray);
            data =  ByteBuffer.allocate(0); // dummy UDataMemory object in absence
                                        // of memory mapping

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
    public static final String io_getConverterName(String alias)
                                    throws IOException{
        if (haveAliasData() && isAlias(alias)) {
            boolean[] isAmbigous = new boolean[1];
            long convNum = findConverter(alias, isAmbigous);
            if (convNum < gConverterListSize) {
                return GET_STRING(gConverterListArray[(int) convNum]);
            }
            /* else converter not found */
        }
        return null;
    }

    /*
     * search for an alias return the converter number index for gConverterList
     */
    // static U_INLINE uint32_t findConverter(const char *alias, UErrorCode
    // *pErrorCode)
    private static final long findConverter(String alias, boolean[] isAmbigous) {
        long mid, start, limit;
        long lastMid;
        long result;

        /* do a binary search for the alias */
        start = 0;
        limit = gUntaggedConvArraySize;
        mid = limit;
        lastMid = UINT32_MAX;

        for (;;) {
            mid = (start + limit) / 2;
            if (lastMid == mid) { /* Have we moved? */
                break; /* We haven't moved, and it wasn't found. */
            }
            lastMid = mid;
            result = compareNames(alias, GET_STRING(gAliasListArray[(int) mid]));

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
                if ((gUntaggedConvArrayArray[(int) mid] & AMBIGUOUS_ALIAS_MAP_BIT) != 0) {
                    isAmbigous[0]=true;
                }
                return gUntaggedConvArrayArray[(int) mid] & CONVERTER_INDEX_MASK;
            }
        }
//  public static final long UINT32_MAX = 4294967295L;
        return Long.MAX_VALUE;
    }

    /**
     * \var io_stripForCompare Remove the underscores, dashes and spaces from
     * the name, and convert the name to lower case.
     * 
     * @param dst
     *            The destination buffer, which is <= the buffer of name.
     * @param dst
     *            The destination buffer, which is <= the buffer of name.
     * @return the destination buffer.
     */
    public static final StringBuffer io_stripForCompare(StringBuffer dst, String name) {
        return io_stripASCIIForCompare(dst, name);
    }

    /* @see compareNames */
    private static final StringBuffer io_stripASCIIForCompare(StringBuffer dst, String name) {
        name = name.concat("\000");
        int nameIndex = 0;
        char c1 = name.charAt(0);
        int dstItr = 0;

        while (c1 != 0) {
            /* Ignore delimiters '-', '_', and ' ' */
            while ((c1 = name.charAt(nameIndex)) == 0x2d || c1 == 0x5f
                    || c1 == 0x20) {
                ++nameIndex;
            }

            /* lowercase for case-insensitive comparison */
            dst.append(Character.toLowerCase(c1));
            ++dstItr;
            ++nameIndex;
        }
        if (dst.length() > 0)
            dst.deleteCharAt(dst.length() - 1);
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
     * @see io_stripForCompare
     */
    public static int compareNames(String name1, String name2){
        int result = 0;
        int i1 = 0;
        int i2 = 0;
        while (true) {
            char ch1 = 0;
            char ch2 = 0;
            // Ignore delimiters '-', '_', and ASCII White_Space
            if (i1 < name1.length()) {
                ch1 = name1.charAt(i1 ++);
            }
            while (ch1 == '-' || ch1 == '_' || ch1 == ' ' ) {
                if (i1 < name1.length()) {
                    ch1 = name1.charAt(i1 ++);
                }
                else {
                    ch1 = 0;
                }
            }
            if (i2 < name2.length()) {
                ch2 = name2.charAt(i2 ++);
            }
            while (ch2 == '-' || ch2 == '_' || ch2 == ' ' ) {
                if (i2 < name2.length()) {
                    ch2 = name2.charAt(i2 ++);
                }
                else {
                    ch2 = 0;
                }
            }

            // If we reach the ends of both strings then they match
            if (ch1 == 0 && ch2 == 0) {
                return 0;
            }

            // Case-insensitive comparison
            if (ch1 != ch2) {
                result = Character.toLowerCase(ch1)- Character.toLowerCase(ch2);
                if (result != 0) {
                    return result;
                }
            }
        }
    }

    public static int io_countAliases(String alias) 
                        throws IOException{
        if (haveAliasData() && isAlias(alias)) {
            boolean[] isAmbigous = new boolean[1];
            long convNum = findConverter(alias, isAmbigous);
            if (convNum < gConverterListSize) {
                /* tagListNum - 1 is the ALL tag */
                int listOffset = gTaggedAliasArrayArray[(int) ((gTagListSize - 1)
                        * gConverterListSize + convNum)];

                if (listOffset != 0) {
                    return gTaggedAliasListsArray[listOffset];
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
     * @param pErrorCode
     *            The error code
     * @return the number of all aliases
     */
    // U_CFUNC uint16_t io_countTotalAliases(UErrorCode *pErrorCode);
    public static int io_countTotalAliases() throws IOException{
        if (haveAliasData()) {
            return (int) gAliasListSize;
        }
        return 0;
    }

    // U_CFUNC const char * io_getAlias(const char *alias, uint16_t n,
    // UErrorCode *pErrorCode)
    public static String io_getAlias(String alias, int n) throws IOException{
        if (haveAliasData() && isAlias(alias)) {
            boolean[] isAmbigous = new boolean[1];
            long convNum = findConverter(alias,isAmbigous);
            if (convNum < gConverterListSize) {
                /* tagListNum - 1 is the ALL tag */
                int listOffset = gTaggedAliasArrayArray[(int) ((gTagListSize - 1)
                        * gConverterListSize + convNum)];

                if (listOffset != 0) {
                    //long listCount = gTaggedAliasListsArray[listOffset];
                    /* +1 to skip listCount */
                    int[] currListArray = gTaggedAliasListsArray;
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
    public static int io_countStandards() throws IOException{
        if (haveAliasData()) {
            return (int) (gTagListSize - NUM_HIDDEN_TAGS);
        }
        return 0;
    }

    // U_CAPI const char * U_EXPORT2getStandard(uint16_t n, UErrorCode
    // *pErrorCode)
    public static String getStandard(int n) throws IOException{
        if (haveAliasData()) {
            return GET_STRING(gTagListArray[n]);
        }
        return null;
    }

    // U_CAPI const char * U_EXPORT2 getStandardName(const char *alias, const
    // char *standard, UErrorCode *pErrorCode)
    public static final String getStandardName(String alias, String standard)throws IOException {
        if (haveAliasData() && isAlias(alias)) {
            long listOffset = findTaggedAliasListsOffset(alias, standard);

            if (0 < listOffset && listOffset < gTaggedAliasListsSize) {
                int[] currListArray = gTaggedAliasListsArray;
                long currListArrayIndex = listOffset + 1;
                if (currListArray[0] != 0) {
                    return GET_STRING(currListArray[(int) currListArrayIndex]);
                }
            }
        }
        return null;
    }

    // U_CAPI uint16_t U_EXPORT2 countAliases(const char *alias, UErrorCode
    // *pErrorCode)
    public static int countAliases(String alias) throws IOException{
        return io_countAliases(alias);
    }

    // U_CAPI const char* U_EXPORT2 getAlias(const char *alias, uint16_t n,
    // UErrorCode *pErrorCode)
    public static String getAlias(String alias, int n) throws IOException{
        return io_getAlias(alias, n);
    }

    // U_CFUNC uint16_t countStandards(void)
    public static int countStandards()throws IOException{
        return io_countStandards();
    }
    
    /*returns a single Name from the list, will return NULL if out of bounds
     */
    public static String getAvailableName (int n){
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
    public static String getCanonicalName(String alias, String standard) throws IOException{
        if (haveAliasData() && isAlias(alias)) {
            long convNum = findTaggedConverterNum(alias, standard);

            if (convNum < gConverterListSize) {
                return GET_STRING(gConverterListArray[(int) convNum]);
            }
        }

        return null;
    }
    public static int countAvailable (){
        try{
            return bld_countAvailableConverters();
        }catch(IOException ex){
            //throw away exception
        }
        return -1;
    }
        
    // U_CAPI UEnumeration * U_EXPORT2 openStandardNames(const char *convName,
    // const char *standard, UErrorCode *pErrorCode)
    public static final UConverterAliasesEnumeration openStandardNames(String convName, String standard)throws IOException {
        UConverterAliasesEnumeration aliasEnum = null;
        if (haveAliasData() && isAlias(convName)) {
            long listOffset = findTaggedAliasListsOffset(convName, standard);

            /*
             * When listOffset == 0, we want to acknowledge that the converter
             * name and standard are okay, but there is nothing to enumerate.
             */
            if (listOffset < gTaggedAliasListsSize) {

                UConverterAliasesEnumeration.UAliasContext context = new UConverterAliasesEnumeration.UAliasContext(listOffset, 0);
                aliasEnum = new UConverterAliasesEnumeration();
                aliasEnum.setContext(context);
            }
            /* else converter or tag not found */
        }
        return aliasEnum;
    }

    // static uint32_t getTagNumber(const char *tagname)
    private static long getTagNumber(String tagName) {
        if (gTagListArray != null) {
            long tagNum;
            for (tagNum = 0; tagNum < gTagListSize; tagNum++) {
                if (tagName.equals(GET_STRING(gTagListArray[(int) tagNum]))) {
                    return tagNum;
                }
            }
        }

        return UINT32_MAX;
    }

    // static uint32_t findTaggedAliasListsOffset(const char *alias, const char
    // *standard, UErrorCode *pErrorCode)
    private static long findTaggedAliasListsOffset(String alias, String standard) {
        long idx;
        long listOffset;
        long convNum;
        long tagNum = getTagNumber(standard);
        boolean[] isAmbigous = new boolean[1];
        /* Make a quick guess. Hopefully they used a TR22 canonical alias. */
        convNum = findConverter(alias, isAmbigous);

        if (tagNum < (gTagListSize - NUM_HIDDEN_TAGS)
                && convNum < gConverterListSize) {
            listOffset = gTaggedAliasArrayArray[(int) (tagNum
                    * gConverterListSize + convNum)];
            if (listOffset != 0
                    && gTaggedAliasListsArray[(int) listOffset + 1] != 0) {
                return listOffset;
            }
            if (isAmbigous[0]==true) {
                /*
                 * Uh Oh! They used an ambiguous alias. We have to search the
                 * whole swiss cheese starting at the highest standard affinity.
                 * This may take a while.
                 */

                for (idx = 0; idx < gTaggedAliasArraySize; idx++) {
                    listOffset = gTaggedAliasArrayArray[(int) idx];
                    if (listOffset != 0 && isAliasInList(alias, listOffset)) {
                        long currTagNum = idx / gConverterListSize;
                        long currConvNum = (idx - currTagNum
                                * gConverterListSize);
                        long tempListOffset = gTaggedAliasArrayArray[(int) (tagNum
                                * gConverterListSize + currConvNum)];
                        if (tempListOffset != 0
                                && gTaggedAliasListsArray[(int) tempListOffset + 1] != 0) {
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

        return UINT32_MAX;
    }

    /* Return the canonical name */
    // static uint32_t findTaggedConverterNum(const char *alias, const char
    // *standard, UErrorCode *pErrorCode)
    private static long findTaggedConverterNum(String alias, String standard) {
        long idx;
        long listOffset;
        long convNum;
        long tagNum = getTagNumber(standard);
        boolean[] isAmbigous = new boolean[1];
        
        /* Make a quick guess. Hopefully they used a TR22 canonical alias. */
        convNum = findConverter(alias, isAmbigous);        

        if (tagNum < (gTagListSize - NUM_HIDDEN_TAGS)
                && convNum < gConverterListSize) {
            listOffset = gTaggedAliasArrayArray[(int) (tagNum
                    * gConverterListSize + convNum)];
            if (listOffset != 0 && isAliasInList(alias, listOffset)) {
                return convNum;
            }
            if (isAmbigous[0] == true) {
                /*
                 * Uh Oh! They used an ambiguous alias. We have to search one
                 * slice of the swiss cheese. We search only in the requested
                 * tag, not the whole thing. This may take a while.
                 */
                long convStart = (tagNum) * gConverterListSize;
                long convLimit = (tagNum + 1) * gConverterListSize;
                for (idx = convStart; idx < convLimit; idx++) {
                    listOffset = gTaggedAliasArrayArray[(int) idx];
                    if (listOffset != 0 && isAliasInList(alias, listOffset)) {
                        return idx - convStart;
                    }
                }
                /* The standard doesn't know about the alias */
            }
            /* else no canonical name */
        }
        /* else converter or tag not found */

        return UINT32_MAX;
    }

    // static U_INLINE UBool isAliasInList(const char *alias, uint32_t
    // listOffset)
    private static boolean isAliasInList(String alias, long listOffset) {
        if (listOffset != 0) {
            long currAlias;
            long listCount = gTaggedAliasListsArray[(int) listOffset];
            /* +1 to skip listCount */
            int[] currList = gTaggedAliasListsArray;
            long currListArrayIndex = listOffset + 1;
            for (currAlias = 0; currAlias < listCount; currAlias++) {
                if (currList[(int) (currAlias + currListArrayIndex)] != 0
                        && compareNames(
                                alias,
                                GET_STRING(currList[(int) (currAlias + currListArrayIndex)])) == 0) {
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
            localConverterList = new String[(int) gConverterListSize];

            localConverterCount = 0;

            for (idx = 0; idx < gConverterListSize; idx++) {
                converterName = GET_STRING(gConverterListArray[idx]);
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
    public static int bld_countAvailableConverters() throws IOException{
        if (haveAvailableConverterList()) {
            return gAvailableConverterCount;
        }
        return 0;
    }

    // U_CFUNC const char * bld_getAvailableConverter(uint16_t n, UErrorCode
    // *pErrorCode)
    public static String bld_getAvailableConverter(int n) throws IOException{
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
    public static final synchronized String getDefaultName() {
        /* local variable to be thread-safe */
        String name;

        //agljport:todo umtx_lock(null);
        name = gDefaultConverterName;
        //agljport:todo umtx_unlock(null);

        if (name == null) {
            //UConverter cnv = null;
            long length = 0;

            name = CharsetICU.getDefaultCharsetName();

            /* if the name is there, test it out and get the canonical name with options */
            if (name != null) {
               // cnv = UConverter.open(name); 
               // name = cnv.getName(cnv);
                // TODO: fix me
            }

            if (name == null || name.length() == 0 ||/* cnv == null ||*/
                     length >= gDefaultConverterNameBuffer.length) {
                /* Panic time, let's use a fallback. */
                name = new String("US-ASCII");
            }

            //length=(int32_t)(strlen(name));

            /* Copy the name before we close the converter. */
            name = gDefaultConverterName;
        }

        return name;
    }

    //end bld.c
}