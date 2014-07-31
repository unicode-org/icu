/*
 *******************************************************************************
 * Copyright (C) 1996-2014, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */

package com.ibm.icu.impl;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;

import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.VersionInfo;

public final class ICUBinary {
    /**
     * Reads the ICU .dat package file format.
     * Most methods do not modify the ByteBuffer in any way,
     * not even its position or other state.
     */
    private static final class DatPackageReader {
        /**
         * .dat package data format ID "CmnD".
         */
        private static final int DATA_FORMAT = 0x436d6e44;

        private static final class IsAcceptable implements Authenticate {
            // @Override when we switch to Java 6
            public boolean isDataVersionAcceptable(byte version[]) {
                return version[0] == 1;
            }
        }
        private static final IsAcceptable IS_ACCEPTABLE = new IsAcceptable();

        /**
         * Checks that the ByteBuffer contains a valid, usable ICU .dat package.
         * Moves the buffer position from 0 to after the data header.
         */
        private static boolean validate(ByteBuffer bytes) {
            try {
                readHeader(bytes, DATA_FORMAT, IS_ACCEPTABLE);
            } catch (IOException ignored) {
                return false;
            }
            int count = bytes.getInt(bytes.position());  // Do not move the position.
            if (count <= 0) {
                return false;
            }
            // For each item, there is one ToC entry (8 bytes) and a name string
            // and a data item of at least 16 bytes.
            // (We assume no data item duplicate elimination for now.)
            if (bytes.position() + 4 + count * (8 + 16) > bytes.capacity()) {
                return false;
            }
            if (!startsWithPackageName(bytes, getNameOffset(bytes, 0)) ||
                    !startsWithPackageName(bytes, getNameOffset(bytes, count - 1))) {
                return false;
            }
            return true;
        }

        private static boolean startsWithPackageName(ByteBuffer bytes, int start) {
            // Compare all but the trailing 'b' or 'l' which depends on the platform.
            int length = ICUData.PACKAGE_NAME.length() - 1;
            for (int i = 0; i < length; ++i) {
                if (bytes.get(start + i) != ICUData.PACKAGE_NAME.charAt(i)) {
                    return false;
                }
            }
            // Check for 'b' or 'l' followed by '/'.
            byte c = bytes.get(start + length++);
            if ((c != 'b' && c != 'l') || bytes.get(start + length) != '/') {
                return false;
            }
            return true;
        }

        private static ByteBuffer getData(ByteBuffer bytes, CharSequence key) {
            int base = bytes.position();
            int count = bytes.getInt(base);

            // Do a binary search for the key.
            int start = 0;
            int limit = count;
            while (start < limit) {
                int mid = (start + limit) >>> 1;
                int nameOffset = getNameOffset(bytes, mid);
                // Skip "icudt54b/".
                nameOffset += ICUData.PACKAGE_NAME.length() + 1;
                int result = compareKeys(key, bytes, nameOffset);
                if (result < 0) {
                    limit = mid;
                } else if (result > 0) {
                    start = mid + 1;
                } else {
                    // We found it!
                    ByteBuffer data = bytes.duplicate();
                    data.position(getDataOffset(bytes, mid));
                    data.limit(getDataOffset(bytes, mid + 1));
                    return ICUBinary.sliceWithOrder(data);
                }
            }
            return null;  // Not found or table is empty.
        }

        private static int getNameOffset(ByteBuffer bytes, int index) {
            int base = bytes.position();
            assert 0 <= index && index < bytes.getInt(base);  // count
            // The count integer (4 bytes)
            // is followed by count (nameOffset, dataOffset) integer pairs (8 bytes per pair).
            return base + bytes.getInt(base + 4 + index * 8);
        }

        private static int getDataOffset(ByteBuffer bytes, int index) {
            int base = bytes.position();
            int count = bytes.getInt(base);
            if (index == count) {
                // Return the limit of the last data item.
                return bytes.capacity();
            }
            assert 0 <= index && index < count;
            // The count integer (4 bytes)
            // is followed by count (nameOffset, dataOffset) integer pairs (8 bytes per pair).
            // The dataOffset follows the nameOffset (skip another 4 bytes).
            return base + bytes.getInt(base + 4 + 4 + index * 8);
        }
    }

    private static final class DataFile {
        public final String itemPath;
        /**
         * null if a .dat package.
         */
        public final File path;
        /**
         * .dat package bytes, or null if not a .dat package.
         * position() is after the header.
         * Do not modify the position or other state, for thread safety.
         */
        public final ByteBuffer pkgBytes;

        public DataFile(String item, File path) {
            itemPath = item;
            this.path = path;
            pkgBytes = null;
        }
        public DataFile(String item, ByteBuffer bytes) {
            itemPath = item;
            path = null;
            pkgBytes = bytes;
        }
        public String toString() {
            return path.toString();
        }
    }
    private static final List<DataFile> icuDataFiles = new ArrayList<DataFile>();

    static {
        // Normally com.ibm.icu.impl.ICUBinary.dataPath.
        String dataPath = ICUConfig.get(ICUBinary.class.getName() + ".dataPath");
        if (dataPath != null) {
            addDataFilesFromPath(dataPath, icuDataFiles);
        }
    }

    private static void addDataFilesFromPath(String dataPath, List<DataFile> files) {
        // Split the path and find files in each location.
        // This splitting code avoids the regex pattern compilation in String.split()
        // and its array allocation.
        // (There is no simple by-character split()
        // and the StringTokenizer "is discouraged in new code".)
        int pathStart = 0;
        while (pathStart < dataPath.length()) {
            int sepIndex = dataPath.indexOf(File.pathSeparatorChar, pathStart);
            int pathLimit;
            if (sepIndex >= 0) {
                pathLimit = sepIndex;
            } else {
                pathLimit = dataPath.length();
            }
            String path = dataPath.substring(pathStart, pathLimit).trim();
            if (path.endsWith(File.separator)) {
                path = path.substring(0, path.length() - 1);
            }
            if (path.length() != 0) {
                addDataFilesFromFolder(new File(path), new StringBuilder(), icuDataFiles);
            }
            if (sepIndex < 0) {
                break;
            }
            pathStart = sepIndex + 1;
        }
    }

    private static void addDataFilesFromFolder(File folder, StringBuilder itemPath,
            List<DataFile> dataFiles) {
        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        int folderPathLength = itemPath.length();
        if (folderPathLength > 0) {
            // The item path must use the ICU file separator character,
            // not the platform-dependent File.separatorChar,
            // so that the enumerated item paths match the paths requested by ICU code.
            itemPath.append('/');
            ++folderPathLength;
        }
        for (File file : files) {
            String fileName = file.getName();
            if (fileName.endsWith(".txt")) {
                continue;
            }
            itemPath.append(fileName);
            if (file.isDirectory()) {
                // TODO: Within a folder, put all single files before all .dat packages?
                addDataFilesFromFolder(file, itemPath, dataFiles);
            } else if (fileName.endsWith(".dat")) {
                ByteBuffer pkgBytes = mapFile(file);
                if (pkgBytes != null && DatPackageReader.validate(pkgBytes)) {
                    dataFiles.add(new DataFile(itemPath.toString(), pkgBytes));
                }
            } else {
                dataFiles.add(new DataFile(itemPath.toString(), file));
            }
            itemPath.setLength(folderPathLength);
        }
    }

    /**
     * Compares the length-specified input key with the
     * NUL-terminated table key. (ASCII)
     */
    static int compareKeys(CharSequence key, ByteBuffer bytes, int offset) {
        for (int i = 0;; ++i, ++offset) {
            int c2 = bytes.get(offset);
            if (c2 == 0) {
                if (i == key.length()) {
                    return 0;
                } else {
                    return 1;  // key > table key because key is longer.
                }
            } else if (i == key.length()) {
                return -1;  // key < table key because key is shorter.
            }
            int diff = (int)key.charAt(i) - c2;
            if (diff != 0) {
                return diff;
            }
        }
    }

    // public inner interface ------------------------------------------------

    /**
     * Special interface for data authentication
     */
    public static interface Authenticate
    {
        /**
         * Method used in ICUBinary.readHeader() to provide data format
         * authentication. 
         * @param version version of the current data
         * @return true if dataformat is an acceptable version, false otherwise
         */
        public boolean isDataVersionAcceptable(byte version[]);
    }
    
    // public methods --------------------------------------------------------

    /**
     * Loads an ICU binary data file and returns it as a ByteBuffer.
     * The buffer contents is normally read-only, but its position etc. can be modified.
     *
     * @param itemPath Relative ICU data item path, for example "root.res" or "coll/ucadata.icu".
     * @return The data as a read-only ByteBuffer,
     *         or null if the resource could not be found.
     */
    public static ByteBuffer getData(String itemPath) {
        return getData(null, null, itemPath, false);
    }

    /**
     * Loads an ICU binary data file and returns it as a ByteBuffer.
     * The buffer contents is normally read-only, but its position etc. can be modified.
     *
     * @param loader Used for loader.getResourceAsStream() unless the data is found elsewhere.
     * @param resourceName Resource name for use with the loader.
     * @param itemPath Relative ICU data item path, for example "root.res" or "coll/ucadata.icu".
     * @return The data as a read-only ByteBuffer,
     *         or null if the resource could not be found.
     */
    public static ByteBuffer getData(ClassLoader loader, String resourceName, String itemPath) {
        return getData(loader, resourceName, itemPath, false);
    }

    /**
     * Loads an ICU binary data file and returns it as a ByteBuffer.
     * The buffer contents is normally read-only, but its position etc. can be modified.
     *
     * @param itemPath Relative ICU data item path, for example "root.res" or "coll/ucadata.icu".
     * @return The data as a read-only ByteBuffer.
     * @throws MissingResourceException if required==true and the resource could not be found
     */
    public static ByteBuffer getRequiredData(String itemPath) {
        return getData(null, null, itemPath, true);
    }

    /**
     * Loads an ICU binary data file and returns it as a ByteBuffer.
     * The buffer contents is normally read-only, but its position etc. can be modified.
     *
     * @param loader Used for loader.getResourceAsStream() unless the data is found elsewhere.
     * @param resourceName Resource name for use with the loader.
     * @param itemPath Relative ICU data item path, for example "root.res" or "coll/ucadata.icu".
     * @return The data as a read-only ByteBuffer.
     * @throws MissingResourceException if required==true and the resource could not be found
     */
//    public static ByteBuffer getRequiredData(ClassLoader loader, String resourceName,
//            String itemPath) {
//        return getData(loader, resourceName, itemPath, true);
//    }

    /**
     * Loads an ICU binary data file and returns it as a ByteBuffer.
     * The buffer contents is normally read-only, but its position etc. can be modified.
     *
     * @param loader Used for loader.getResourceAsStream() unless the data is found elsewhere.
     * @param resourceName Resource name for use with the loader.
     * @param itemPath Relative ICU data item path, for example "root.res" or "coll/ucadata.icu".
     * @param required If the resource cannot be found,
     *        this method returns null (!required) or throws an exception (required).
     * @return The data as a read-only ByteBuffer,
     *         or null if required==false and the resource could not be found.
     * @throws MissingResourceException if required==true and the resource could not be found
     */
    private static ByteBuffer getData(ClassLoader loader, String resourceName,
            String itemPath, boolean required) {
        ByteBuffer bytes = getDataFromFile(itemPath);
        if (bytes != null) {
            return bytes;
        }
        if (loader == null) {
            loader = ICUData.class.getClassLoader();
        }
        if (resourceName == null) {
            resourceName = ICUData.ICU_BASE_NAME + '/' + itemPath;
        }
        InputStream is = ICUData.getStream(loader, resourceName, required);
        if (is == null) {
            return null;
        }
        try {
            return getByteBufferFromInputStream(is);
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    private static ByteBuffer getDataFromFile(String itemPath) {
        for (DataFile dataFile : icuDataFiles) {
            if (dataFile.pkgBytes != null) {
                ByteBuffer data = DatPackageReader.getData(dataFile.pkgBytes, itemPath);
                if (data != null) {
                    return data;
                }
            } else if (itemPath.equals(dataFile.itemPath)) {
                return mapFile(dataFile.path);
            }
        }
        return null;
    }

    private static ByteBuffer mapFile(File path) {
        FileInputStream file;
        try {
            file = new FileInputStream(path);
            FileChannel channel = file.getChannel();
            ByteBuffer bytes = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            // Close the file and its channel; this seems to keep the ByteBuffer valid.
            // If not, then we will need to return the pair of (file, bytes).
            file.close();
            return bytes;
        } catch(FileNotFoundException ignored) {
            System.err.println(ignored);
        } catch (IOException ignored) {
            System.err.println(ignored);
        }
        return null;
    }

    /**
     * Same as readHeader(), but returns a VersionInfo rather than a compact int.
     */
    public static VersionInfo readHeaderAndDataVersion(ByteBuffer bytes,
                                                             int dataFormat,
                                                             Authenticate authenticate)
                                                                throws IOException {
        return getVersionInfoFromCompactInt(readHeader(bytes, dataFormat, authenticate));
    }

    /**
     * Reads an ICU data header, checks the data format, and returns the data version.
     *
     * <p>Assumes that the ByteBuffer position is 0 on input.
     * The buffer byte order is set according to the data.
     * The buffer position is advanced past the header (including UDataInfo and comment).
     *
     * <p>See C++ ucmndata.h and unicode/udata.h.
     *
     * @return dataVersion
     * @throws IOException if this is not a valid ICU data item of the expected dataFormat
     */
    public static int readHeader(ByteBuffer bytes, int dataFormat, Authenticate authenticate)
            throws IOException {
        assert bytes.position() == 0;
        byte magic1 = bytes.get(2);
        byte magic2 = bytes.get(3);
        if (magic1 != MAGIC1 || magic2 != MAGIC2) {
            throw new IOException(MAGIC_NUMBER_AUTHENTICATION_FAILED_);
        }

        byte isBigEndian = bytes.get(8);
        byte charsetFamily = bytes.get(9);
        byte sizeofUChar = bytes.get(10);
        if (isBigEndian < 0 || 1 < isBigEndian ||
                charsetFamily != CHAR_SET_ || sizeofUChar != CHAR_SIZE_) {
            throw new IOException(HEADER_AUTHENTICATION_FAILED_);
        }
        bytes.order(isBigEndian != 0 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        int headerSize = bytes.getChar(0);
        int sizeofUDataInfo = bytes.getChar(4);
        if (sizeofUDataInfo < 20 || headerSize < (sizeofUDataInfo + 4)) {
            throw new IOException("Internal Error: Header size error");
        }
        // TODO: Change Authenticate to take int major, int minor, int milli, int micro
        // to avoid array allocation.
        byte[] formatVersion = new byte[] {
            bytes.get(16), bytes.get(17), bytes.get(18), bytes.get(19)
        };
        if (bytes.get(12) != (byte)(dataFormat >> 24) ||
                bytes.get(13) != (byte)(dataFormat >> 16) ||
                bytes.get(14) != (byte)(dataFormat >> 8) ||
                bytes.get(15) != (byte)dataFormat ||
                (authenticate != null && !authenticate.isDataVersionAcceptable(formatVersion))) {
            throw new IOException(HEADER_AUTHENTICATION_FAILED_ +
                    String.format("; data format %02x%02x%02x%02x, format version %d.%d.%d.%d",
                            bytes.get(12), bytes.get(13), bytes.get(14), bytes.get(15),
                            formatVersion[0] & 0xff, formatVersion[1] & 0xff,
                            formatVersion[2] & 0xff, formatVersion[3] & 0xff));
        }

        bytes.position(headerSize);
        return  // dataVersion
                ((int)bytes.get(20) << 24) |
                ((bytes.get(21) & 0xff) << 16) |
                ((bytes.get(22) & 0xff) << 8) |
                (bytes.get(23) & 0xff);
    }

    /**
     * Writes an ICU data header.
     * Does not write a copyright string.
     *
     * @return The length of the header (number of bytes written).
     * @throws IOException from the DataOutputStream
     */
    public static int writeHeader(int dataFormat, int formatVersion, int dataVersion,
            DataOutputStream dos) throws IOException {
        // ucmndata.h MappedData
        dos.writeChar(32);  // headerSize
        dos.writeByte(MAGIC1);
        dos.writeByte(MAGIC2);
        // unicode/udata.h UDataInfo
        dos.writeChar(20);  // sizeof(UDataInfo)
        dos.writeChar(0);  // reservedWord
        dos.writeByte(1);  // isBigEndian
        dos.writeByte(CHAR_SET_);  // charsetFamily
        dos.writeByte(CHAR_SIZE_);  // sizeofUChar
        dos.writeByte(0);  // reservedByte
        dos.writeInt(dataFormat);
        dos.writeInt(formatVersion);
        dos.writeInt(dataVersion);
        // 8 bytes padding for 32 bytes headerSize (multiple of 16).
        dos.writeLong(0);
        assert dos.size() == 32;
        return 32;
    }

    public static void skipBytes(ByteBuffer bytes, int skipLength) {
        if (skipLength > 0) {
            bytes.position(bytes.position() + skipLength);
        }
    }

    /**
     * Same as ByteBuffer.slice() plus preserving the byte order.
     */
    public static ByteBuffer sliceWithOrder(ByteBuffer bytes) {
        ByteBuffer b = bytes.slice();
        return b.order(bytes.order());
    }

    /**
     * Reads the entire contents from the stream into a byte array
     * and wraps it into a ByteBuffer. Closes the InputStream at the end.
     */
    public static ByteBuffer getByteBufferFromInputStream(InputStream is) throws IOException {
        try {
            int avail = is.available();
            byte[] bytes = new byte[avail];
            readFully(is, bytes, 0, avail);
            while((avail = is.available()) != 0) {
                // TODO Java 6 replace new byte[] and arraycopy(): byte[] newBytes = Arrays.copyOf(bytes, bytes.length + avail);
                byte[] newBytes = new byte[bytes.length + avail];
                System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
                readFully(is, newBytes, bytes.length, avail);
                bytes = newBytes;
            }
            return ByteBuffer.wrap(bytes);
        } finally {
            is.close();
        }
    }

    private static void readFully(InputStream is, byte[] bytes, int offset, int avail)
            throws IOException {
        while (avail > 0) {
            int numRead = is.read(bytes, offset, avail);
            assert numRead > 0;
            offset += numRead;
            avail -= numRead;
        }
    }

    /**
     * Returns a VersionInfo for the bytes in the compact version integer.
     */
    public static VersionInfo getVersionInfoFromCompactInt(int version) {
        return VersionInfo.getInstance(
                version >>> 24, (version >> 16) & 0xff, (version >> 8) & 0xff, version & 0xff);
    }

    /**
     * Returns an array of the bytes in the compact version integer.
     */
    public static byte[] getVersionByteArrayFromCompactInt(int version) {
        return new byte[] {
                (byte)(version >> 24),
                (byte)(version >> 16),
                (byte)(version >> 8),
                (byte)(version)
        };
    }

    // private variables -------------------------------------------------
  
    /**
    * Magic numbers to authenticate the data file
    */
    private static final byte MAGIC1 = (byte)0xda;
    private static final byte MAGIC2 = (byte)0x27;
      
    /**
    * File format authentication values
    */
    private static final byte CHAR_SET_ = 0;
    private static final byte CHAR_SIZE_ = 2;
                                                    
    /**
    * Error messages
    */
    private static final String MAGIC_NUMBER_AUTHENTICATION_FAILED_ = 
                       "ICU data file error: Not an ICU data file";
    private static final String HEADER_AUTHENTICATION_FAILED_ =
        "ICU data file error: Header authentication failed, please check if you have a valid ICU data file";
}
