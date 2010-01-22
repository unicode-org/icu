/*
 **********************************************************************
 * Copyright (c) 2002-2009, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Alan Liu
 * Created: November 5 2002
 * Since: ICU 2.4
 **********************************************************************
 */

package com.ibm.icu.impl;

import java.io.*;
import java.util.MissingResourceException;

import com.ibm.icu.lang.*;

/**
 * Wrapper for the pnames.icu binary data file.  This data file is
 * imported from icu4c.  It contains property and property value
 * aliases from the UCD files PropertyAliases.txt and
 * PropertyValueAliases.txt.  The file is built by the icu4c tool
 * genpname.  It must be built on an ASCII big-endian platform to be
 * usable in icu4j.
 *
 * This class performs two functions.
 *
 * (1) It can import the flat binary data into a tree of usable
 * objects.
 *
 * (2) It provides an API to access the tree of objects.
 *
 * Needless to say, this class is tightly coupled to the binary format
 * of icu4c's pnames.icu file.
 *
 * Each time a UPropertyAliases is constructed, the pnames.icu file is
 * read, parsed, and a data tree assembled.  Clients should create one
 * singleton instance and cache it.
 *
 * @author Alan Liu
 * @since ICU 2.4
 */
public final class UPropertyAliases implements ICUBinary.Authenticate {

    //----------------------------------------------------------------
    // Runtime data.  This is an unflattened representation of the
    // data in pnames.icu.

    /**
     * Map from property enum value to nameGroupPool[] index
     */
    private NonContiguousEnumToShort enumToName;

    /**
     * Map from property alias to property enum value
     */
    private NameToEnum nameToEnum;

    /**
     * Map from property enum value to valueMapArray[] index
     */
    private NonContiguousEnumToShort enumToValue;

    /**
     * Each entry represents a binary or enumerated property
     */
    private ValueMap valueMapArray[];

    /**
     * Pool of concatenated integer runs.  Each run contains one
     * or more entries.  The last entry of the run is negative.
     * A zero entry indicates "n/a" in the Property*Aliases.txt.
     * Each entry is a stringPool[] index.
     */
    private short nameGroupPool[];

    /**
     * Pool of strings.
     */
    private String stringPool[];

    //----------------------------------------------------------------
    // Constants

    /**
     * Debug flag (not really constant)
     */
    private static boolean DEBUG = ICUDebug.enabled("pnames");

    /**
     * File format that this class understands.
     * See icu4c/src/common/propname.h.
     */
    private static final byte DATA_FORMAT_ID[] = {'p', 'n', 'a', 'm'};

    /**
     * File version that this class understands.
     * See icu4c/src/common/propname.h.
     */
    private static final byte DATA_FORMAT_VERSION = 1;

    /**
     * Name of the datafile
     */
    private static final String DATA_FILE_NAME = ICUResourceBundle.ICU_BUNDLE+"/pnames.icu";

    /**
     * Buffer size of datafile.  The whole file is < 16k.
     */
    private static final int DATA_BUFFER_SIZE = 8192;

    //----------------------------------------------------------------
    // Constructor

    /**
     * Constructs a UPropertyAliases object.  The binary file
     * DATA_FILE_NAME is read from the jar/classpath and unflattened
     * into member variables of this object.
     */
    private UPropertyAliases() throws IOException {

        // Open the .icu file from the jar/classpath
        InputStream is = ICUData.getRequiredStream(DATA_FILE_NAME);
        BufferedInputStream b = new BufferedInputStream(is, DATA_BUFFER_SIZE);
        // Read and discard Unicode version...
       /* byte unicodeVersion[] = */ICUBinary.readHeader(b, DATA_FORMAT_ID, this);
        DataInputStream d = new DataInputStream(b);

        // Record the origin position of the file.  Keep enough around
        // to seek back to the start of the header.
        d.mark(256);

        short enumToName_offset = d.readShort();
        short nameToEnum_offset = d.readShort();
        short enumToValue_offset = d.readShort();
        short total_size = d.readShort();
        short valueMap_offset = d.readShort();
        short valueMap_count = d.readShort();
        short nameGroupPool_offset = d.readShort();
        short nameGroupPool_count = d.readShort();
        short stringPool_offset = d.readShort();
        short stringPool_count = d.readShort();

        if (DEBUG) {
            System.out.println(
               "enumToName_offset=" + enumToName_offset + "\n" +
               "nameToEnum_offset=" + nameToEnum_offset + "\n" +
               "enumToValue_offset=" + enumToValue_offset + "\n" +
               "total_size=" + total_size + "\n" +
               "valueMap_offset=" + valueMap_offset + "\n" +
               "valueMap_count=" + valueMap_count + "\n" +
               "nameGroupPool_offset=" + nameGroupPool_offset + "\n" +
               "nameGroupPool_count=" + nameGroupPool_count + "\n" +
               "stringPool_offset=" + stringPool_offset + "\n" +
               "stringPool_count=" + stringPool_count);
        }

        // Read it all (less than 32k).  Seeking around (using
        // mark/reset/skipBytes) doesn't work directly on the file,
        // but it works fine if we read everything into a byte[] array
        // first.
        byte raw[] = new byte[total_size];
        d.reset();
        d.readFully(raw);
        d.close();

        Builder builder = new Builder(raw);

        stringPool = builder.readStringPool(stringPool_offset,
                                            stringPool_count);

        nameGroupPool = builder.readNameGroupPool(nameGroupPool_offset,
                                                  nameGroupPool_count);

        builder.setupValueMap_map(valueMap_offset, valueMap_count);

        // Some of the following data structures have to be set up
        // here, _not_ in Builder.  That's because they are instances
        // of non-static inner classes, and they contain implicit
        // references to this.

        builder.seek(enumToName_offset);
        enumToName = new NonContiguousEnumToShort(builder);
        builder.nameGroupOffsetToIndex(enumToName.offsetArray);

        builder.seek(nameToEnum_offset);
        nameToEnum = new NameToEnum(builder);

        builder.seek(enumToValue_offset);
        enumToValue = new NonContiguousEnumToShort(builder);
        builder.valueMapOffsetToIndex(enumToValue.offsetArray);

        valueMapArray = new ValueMap[valueMap_count];
        for (int i=0; i<valueMap_count; ++i) {
            // Must seek to the start of each entry.
            builder.seek(builder.valueMap_map[i]);
            valueMapArray[i] = new ValueMap(builder);
        }

        builder.close();
    }

    //----------------------------------------------------------------
    // Public API

    public static final UPropertyAliases INSTANCE;

    static {
        try {
            INSTANCE = new UPropertyAliases();
        } catch(IOException e) {
            ///CLOVER:OFF
            throw new MissingResourceException("Could not construct UPropertyAliases. Missing pnames.icu","","");
            ///CLOVER:ON
        }
    }

    /**
     * Return a property name given a property enum.  Multiple
     * names may be available for each property; the nameChoice
     * selects among them.
     */
    public String getPropertyName(int property,
                                  int nameChoice) {
        short nameGroupIndex = enumToName.getShort(property);
        return chooseNameInGroup(nameGroupIndex, nameChoice);
    }

    /**
     * Return a property enum given one of its property names.
     * If the property name is not known, this method returns
     * UProperty.UNDEFINED.
     */
    public int getPropertyEnum(String propertyAlias) {
        return nameToEnum.getEnum(propertyAlias);
    }

    /**
     * Return a value name given a property enum and a value enum.
     * Multiple names may be available for each value; the nameChoice
     * selects among them.
     */
    public String getPropertyValueName(int property,
                                       int value,
                                       int nameChoice) {
        ValueMap vm = getValueMap(property);
        short nameGroupIndex = vm.enumToName.getShort(value);
        return chooseNameInGroup(nameGroupIndex, nameChoice);
    }

    /**
     * Return a value enum given one of its value names and the
     * corresponding property alias.
     */
    public int getPropertyValueEnum(int property,
                                    String valueAlias) {
        ValueMap vm = getValueMap(property);
        return vm.nameToEnum.getEnum(valueAlias);
    }

    //----------------------------------------------------------------
    // Data structures

    /**
     * A map for the legal values of a binary or enumerated properties.
     */
    private class ValueMap {

        /**
         * Maps value enum to index into the nameGroupPool[]
         */
        EnumToShort enumToName; // polymorphic

        /**
         * Maps value name to value enum.
         */
        NameToEnum nameToEnum;

        ValueMap(Builder b) throws IOException {
            short enumToName_offset = b.readShort();
            short ncEnumToName_offset = b.readShort();
            short nameToEnum_offset = b.readShort();
            if (enumToName_offset != 0) {
                b.seek(enumToName_offset);
                ContiguousEnumToShort x = new ContiguousEnumToShort(b);
                b.nameGroupOffsetToIndex(x.offsetArray);
                enumToName = x;
            } else {
                b.seek(ncEnumToName_offset);
                NonContiguousEnumToShort x = new NonContiguousEnumToShort(b);
                b.nameGroupOffsetToIndex(x.offsetArray);
                enumToName = x;
            }
            b.seek(nameToEnum_offset);
            nameToEnum = new NameToEnum(b);
        }
    }

    /**
     * Abstract map from enum values to integers.
     */
    private interface EnumToShort {
        short getShort(int enumProbe);
    }

    /**
     * Generic map from enum values to offsets.  Enum values are
     * contiguous.
     */
    private static class ContiguousEnumToShort implements EnumToShort {
        int enumStart;
        int enumLimit;
        short offsetArray[];

        public short getShort(int enumProbe) {
            if (enumProbe < enumStart || enumProbe >= enumLimit) {
                throw new IllegalIcuArgumentException("Invalid enum. enumStart = " +enumStart +
                                                   " enumLimit = " + enumLimit +
                                                   " enumProbe = " + enumProbe );
            }
            return offsetArray[enumProbe - enumStart];
        }

        ContiguousEnumToShort(ICUBinaryStream s) throws IOException  {
            enumStart = s.readInt();
            enumLimit = s.readInt();
            int count = enumLimit - enumStart;
            offsetArray = new short[count];
            for (int i=0; i<count; ++i) {
                offsetArray[i] = s.readShort();
            }
        }
    }

    /**
     * Generic map from enum values to offsets.  Enum values need not
     * be contiguous.
     */
    private static class NonContiguousEnumToShort implements EnumToShort {
        int enumArray[];
        short offsetArray[];

        public short getShort(int enumProbe) {
            for (int i=0; i<enumArray.length; ++i) {
                if (enumArray[i] < enumProbe) continue;
                if (enumArray[i] > enumProbe) break;
                return offsetArray[i];
            }
            throw new IllegalIcuArgumentException("Invalid enum");
        }

        NonContiguousEnumToShort(ICUBinaryStream s) throws IOException  {
            int i;
            int count = s.readInt();
            enumArray = new int[count];
            offsetArray = new short[count];
            for (i=0; i<count; ++i) {
                enumArray[i] = s.readInt();
            }
            for (i=0; i<count; ++i) {
                offsetArray[i] = s.readShort();
            }
        }
    }

    /**
     * Map from names to enum values.
     */
    private class NameToEnum {
        int enumArray[];
        short nameArray[];

        int getEnum(String nameProbe) {
            for (int i=0; i<nameArray.length; ++i) {
                int c = UPropertyAliases.compare(nameProbe,
                                                 stringPool[nameArray[i]]);
                if (c > 0) continue;
                if (c < 0) break;
                return enumArray[i];
            }
            return UProperty.UNDEFINED;
        }

        NameToEnum(Builder b) throws IOException {
            int i;
            int count = b.readInt();
            enumArray = new int[count];
            nameArray = new short[count];
            for (i=0; i<count; ++i) {
                enumArray[i] = b.readInt();
            }
            for (i=0; i<count; ++i) {
                nameArray[i] = b.stringOffsetToIndex(b.readShort());
            }
        }
    }

    //----------------------------------------------------------------
    // Runtime implementation

    /**
     * Compare two property names, returning <0, 0, or >0.  The
     * comparison is that described as "loose" matching in the
     * Property*Aliases.txt files.
     */
    public static int compare(String stra, String strb) {
        // Note: This implementation is a literal copy of
        // uprv_comparePropertyNames.  It can probably be improved.
        int istra=0, istrb=0, rc;
        int cstra=0, cstrb=0;
        for (;;) {
            /* Ignore delimiters '-', '_', and ASCII White_Space */
            while (istra<stra.length()) {
                cstra = stra.charAt(istra);
                switch (cstra) {
                case '-':  case '_':  case ' ':  case '\t':
                case '\n': case 0xb/*\v*/: case '\f': case '\r':
                    ++istra;
                    continue;
                }
                break;
            }

            while (istrb<strb.length()) {
                cstrb = strb.charAt(istrb);
                switch (cstrb) {
                case '-':  case '_':  case ' ':  case '\t':
                case '\n': case 0xb/*\v*/: case '\f': case '\r':
                    ++istrb;
                    continue;
                }
                break;
            }

            /* If we reach the ends of both strings then they match */
            boolean endstra = istra==stra.length();
            boolean endstrb = istrb==strb.length();
            if (endstra) {
                if (endstrb) return 0;
                cstra = 0;
            } else if (endstrb) {
                cstrb = 0;
            }

            rc = UCharacter.toLowerCase(cstra) - UCharacter.toLowerCase(cstrb);
            if (rc != 0) {
                return rc;
            }

            ++istra;
            ++istrb;
        }
    }

    /**
     * Given an index to a run within the nameGroupPool[], and a
     * nameChoice (0,1,...), select the nameChoice-th entry of the run.
     */
    private String chooseNameInGroup(short nameGroupIndex, int nameChoice) {
        if (nameChoice < 0) {
            throw new IllegalIcuArgumentException("Invalid name choice");
        }
        while (nameChoice-- > 0) {
            if (nameGroupPool[nameGroupIndex++] < 0) {
                throw new IllegalIcuArgumentException("Invalid name choice");
            }
        }
        short a = nameGroupPool[nameGroupIndex];
        return stringPool[(a < 0) ? -a : a];
    }

    /**
     * Return the valueMap[] entry for a given property.
     */
    private ValueMap getValueMap(int property) {
        int valueMapIndex = enumToValue.getShort(property);
        return valueMapArray[valueMapIndex];
    }

    //----------------------------------------------------------------
    // ICUBinary API

    /**
     * Return true if the given data version can be used.
     */
    public boolean isDataVersionAcceptable(byte version[])   {
        return version[0] == DATA_FORMAT_VERSION;
    }

    //----------------------------------------------------------------
    // Builder

    /**
     * A specialized ICUBinaryStream that can map between offsets and
     * index values into various arrays (stringPool, nameGroupPool,
     * and valueMap).  It also knows how to read various structures.
     */
    static class Builder extends ICUBinaryStream {

        // map[i] = offset of object i.  We need maps for all of our
        // arrays.  The arrays are indexed by offset in the raw binary
        // file; we need to translate that to index.

        private short stringPool_map[];

        private short valueMap_map[];

        private short nameGroup_map[];

        public Builder(byte raw[]) {
            super(raw);
        }

        /**
         * The valueMap_map[] must be setup in advance.  This method
         * does that.
         */
        public void setupValueMap_map(short offset, short count) {
            valueMap_map = new short[count];
            for (int i=0; i<count; ++i) {
                // Start of each entry.  Each entry is 6 bytes long.
                valueMap_map[i] = (short) (offset + i * 6);
            }
        }

        /**
         * Read stringPool[].  Build up translation table from offsets
         * to string indices (stringPool_map[]).
         */
        public String[] readStringPool(short offset, short count)
            throws IOException {
            seek(offset);
            // Allocate one more stringPool entry than needed.  Use this
            // to store a "no string" entry in the pool, at index 0.  This
            // maps to offset 0, so let stringPool_map[0] = 0.
            String stringPool[] = new String[count + 1];
            stringPool_map = new short[count + 1];
            short pos = offset;
            StringBuffer buf = new StringBuffer();
            stringPool_map[0] = 0;
            for (int i=1; i<=count; ++i) {
                buf.setLength(0);
                for (;;) {
                    // This works because the name is invariant-ASCII
                    char c = (char) readUnsignedByte();
                    if (c == 0) break;
                    buf.append(c);
                }
                stringPool_map[i] = pos;
                stringPool[i] = buf.toString();
                pos += stringPool[i].length() + 1;
            }
            if (DEBUG) {
                System.out.println("read stringPool x " + count +
                                   ": " + stringPool[1] + ", " +
                                   stringPool[2] + ", " +
                                   stringPool[3] + ",...");
            }
            return stringPool;
        }

        /**
         * Read the nameGroupPool[], and build up the offset->index
         * map (nameGroupPool_map[]).
         */
        public short[] readNameGroupPool(short offset, short count)
            throws IOException {
            // Read nameGroupPool[].  This contains offsets from start of
            // header.  We translate these into indices into stringPool[]
            // on the fly.  The offset 0, which indicates "no entry", we
            // translate into index 0, which contains a null String
            // pointer.
            seek(offset);
            short pos = offset;
            short nameGroupPool[] = new short[count];
            nameGroup_map = new short[count];
            for (int i=0; i<count; ++i) {
                nameGroup_map[i] = pos;
                nameGroupPool[i] = stringOffsetToIndex(readShort());
                pos += 2;
            }
            if (DEBUG) {
                System.out.println("read nameGroupPool x " + count +
                                   ": " + nameGroupPool[0] + ", " +
                                   nameGroupPool[1] + ", " +
                                   nameGroupPool[2] + ",...");
            }
            return nameGroupPool;
        }

        /**
         * Convert an offset into the string pool into a stringPool[]
         * index.
         */
        private short stringOffsetToIndex(short offset) {
            int probe = offset;
            if (probe < 0) probe = -probe;
            for (int i=0; i<stringPool_map.length; ++i) {
                if (stringPool_map[i] == probe) {
                    return (short) ((offset < 0) ? -i : i);
                }
            }
            throw new IllegalStateException("Can't map string pool offset " + 
                                            offset + " to index");
        }

        /**
         * Convert an array of offsets into the string pool into an
         * array of stringPool[] indices.  MODIFIES THE ARRAY IN
         * PLACE.
         */
/*        private void stringOffsetToIndex(short array[]) {
            for (int i=0; i<array.length; ++i) {
                array[i] = stringOffsetToIndex(array[i]);
            }
        }*/

        /**
         * Convert an offset into the value map into a valueMap[]
         * index.
         */
        private short valueMapOffsetToIndex(short offset) {
            for (short i=0; i<valueMap_map.length; ++i) {
                if (valueMap_map[i] == offset) {
                    return i;
                }
            }
            throw new IllegalStateException("Can't map value map offset " + 
                                            offset + " to index");
        }

        /**
         * Convert an array of offsets into the value map array into
         * an array of valueMap[] indices.  MODIFIES THE ARRAY IN
         * PLACE.
         */
        private void valueMapOffsetToIndex(short array[]) {
            for (int i=0; i<array.length; ++i) {
                array[i] = valueMapOffsetToIndex(array[i]);
            }
        }

        /**
         * Convert an offset into the name group pool into a
         * nameGroupPool[] index.
         */
        private short nameGroupOffsetToIndex(short offset) {
            for (short i=0; i<nameGroup_map.length; ++i) {
                if (nameGroup_map[i] == offset) {
                    return i;
                }
            }
            throw new RuntimeException("Can't map name group offset " + offset +
                                       " to index");
        }

        /**
         * Convert an array of offsets into the name group pool into an
         * array of nameGroupPool[] indices.  MODIFIES THE ARRAY IN
         * PLACE.
         */
        private void nameGroupOffsetToIndex(short array[]) {
            for (int i=0; i<array.length; ++i) {
                array[i] = nameGroupOffsetToIndex(array[i]);
            }
        }
    }
}
