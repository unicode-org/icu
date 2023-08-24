// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2006-2015, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */

package com.ibm.icu.charset;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.ibm.icu.impl.ICUBinary;


/* Format of cnvalias.icu -----------------------------------------------------
 *
 * cnvalias.icu is a binary, memory-mappable form of convrtrs.txt.
 * This binary form contains several tables. All indexes are to uint16_t
 * units, and not to the bytes (uint8_t units). Addressing everything on
 * 16-bit boundaries allows us to store more information with small index
 * numbers, which are also 16-bit in size. The majority of the table (except
 * the string table) are 16-bit numbers.
 *
 * First there is the size of the Table of Contents (TOC). The TOC
 * entries contain the size of each section. In order to find the offset
 * you just need to sum up the previous offsets.
 * The TOC length and entries are an array of uint32_t values.
 * The first section after the TOC starts immediately after the TOC.
 *
 * 1) This section contains a list of converters. This list contains indexes
 * into the string table for the converter name. The index of this list is
 * also used by other sections, which are mentioned later on.
 * This list is not sorted.
 *
 * 2) This section contains a list of tags. This list contains indexes
 * into the string table for the tag name. The index of this list is
 * also used by other sections, which are mentioned later on.
 * This list is in priority order of standards.
 *
 * 3) This section contains a list of sorted unique aliases. This
 * list contains indexes into the string table for the alias name. The
 * index of this list is also used by other sections, like the 4th section.
 * The index for the 3rd and 4th section is used to get the
 * alias -> converter name mapping. Section 3 and 4 form a two column table.
 *
 * 4) This section contains a list of mapped converter names. Consider this
 * as a table that maps the 3rd section to the 1st section. This list contains
 * indexes into the 1st section. The index of this list is the same index in
 * the 3rd section. There is also some extra information in the high bits of
 * each converter index in this table. Currently it's only used to say that
 * an alias mapped to this converter is ambiguous. See UCNV_CONVERTER_INDEX_MASK
 * and UCNV_AMBIGUOUS_ALIAS_MAP_BIT for more information. This section is
 * the predigested form of the 5th section so that an alias lookup can be fast.
 *
 * 5) This section contains a 2D array with indexes to the 6th section. This
 * section is the full form of all alias mappings. The column index is the
 * index into the converter list (column header). The row index is the index
 * to tag list (row header). This 2D array is the top part a 3D array. The
 * third dimension is in the 6th section.
 *
 * 6) This is blob of variable length arrays. Each array starts with a size,
 * and is followed by indexes to alias names in the string table. This is
 * the third dimension to the section 5. No other section should be referencing
 * this section.
 *
 * 7) Reserved at this time (There is no information). This _usually_ has a
 * size of 0. Future versions may add more information here.
 *
 * 8) This is the string table. All strings are indexed on an even address.
 * There are two reasons for this. First many chip architectures locate strings
 * faster on even address boundaries. Second, since all indexes are 16-bit
 * numbers, this string table can be 128KB in size instead of 64KB when we
 * only have strings starting on an even address.
 *
 *
 * Here is the concept of section 5 and 6. It's a 3D cube. Each tag
 * has a unique alias among all converters. That same alias can
 * be mentioned in other standards on different converters,
 * but only one alias per tag can be unique.
 *
 *
 *              Converter Names (Usually in TR22 form)
 *           -------------------------------------------.
 *     T    /                                          /|
 *     a   /                                          / |
 *     g  /                                          /  |
 *     s /                                          /   |
 *      /                                          /    |
 *      ------------------------------------------/     |
 *    A |                                         |     |
 *    l |                                         |     |
 *    i |                                         |    /
 *    a |                                         |   /
 *    s |                                         |  /
 *    e |                                         | /
 *    s |                                         |/
 *      -------------------------------------------
 *
 *
 *
 * Here is what it really looks like. It's like swiss cheese.
 * There are holes. Some converters aren't recognized by
 * a standard, or they are really old converters that the
 * standard doesn't recognize anymore.
 *
 *              Converter Names (Usually in TR22 form)
 *           -------------------------------------------.
 *     T    /##########################################/|
 *     a   /     #            #                       /#
 *     g  /  #      ##     ##     ### # ### ### ### #/
 *     s / #             #####  ####        ##  ## #/#
 *      / ### # # ##  #  #   #          ### # #   #/##
 *      ------------------------------------------/# #
 *    A |### # # ##  #  #   #          ### # #   #|# #
 *    l |# # #    #     #               ## #     #|# #
 *    i |# # #    #     #                #       #|#
 *    a |#                                       #|#
 *    s |                                        #|#
 *    e
 *    s
 *
 */

final class UConverterAliasDataReader implements ICUBinary.Authenticate {
//    private final static boolean debug = ICUDebug.enabled("UConverterAliasDataReader");

   /**
    * <p>Protected constructor.</p>
    * @param bytes ICU uprop.dat file buffer
    * @exception IOException throw if data file fails authentication
    */
    protected UConverterAliasDataReader(ByteBuffer bytes)
                                        throws IOException{
        //if(debug) System.out.println("Bytes in buffer " + bytes.remaining());

        byteBuffer = bytes;
        /*unicodeVersion = */ICUBinary.readHeader(byteBuffer, DATA_FORMAT_ID, this);

        //if(debug) System.out.println("Bytes left in byteBuffer " + byteBuffer.remaining());
    }

    // protected methods -------------------------------------------------

    protected int[] readToc(int n)throws IOException
    {
        //Read the toc
        return ICUBinary.getInts(byteBuffer, n, 0);
    }

    @Override
    public boolean isDataVersionAcceptable(byte version[])
    {
        return version.length >= DATA_FORMAT_VERSION.length
            && version[0] == DATA_FORMAT_VERSION[0]
            && version[1] == DATA_FORMAT_VERSION[1]
            && version[2] == DATA_FORMAT_VERSION[2];
    }

    /*byte[] getUnicodeVersion(){
        return ICUBinary.getVersionByteArrayFromCompactInt(unicodeVersion);
    }*/
    // private data members -------------------------------------------------


    /**
    * ICU data file buffer
    */
    private ByteBuffer byteBuffer;

//    private int unicodeVersion;

    /**
    * File format version that this class understands.
    * No guarantees are made if a older version is used
    * see store.c of gennorm for more information and values
    */
        // DATA_FORMAT_ID_ values taken from icu4c isAcceptable (ucnv_io.c)
    private static final int DATA_FORMAT_ID = 0x4376416c; // dataFormat="CvAl"
    private static final byte DATA_FORMAT_VERSION[] = {3, 0, 1};
}
