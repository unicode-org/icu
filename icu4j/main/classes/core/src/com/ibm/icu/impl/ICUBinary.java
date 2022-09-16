// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2015, International Business Machines Corporation and
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
import java.util.Arrays;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Set;

import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.VersionInfo;

public final class ICUBinary {

    // private methods / classes for reading individual files ------------------

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
            @Override
            public boolean isDataVersionAcceptable(byte version[]) {
                return version[0] == 1;
            }
        }
        private static final IsAcceptable IS_ACCEPTABLE = new IsAcceptable();

        /**
         * Checks that the ByteBuffer contains a valid, usable ICU .dat package.
         * Moves the buffer position from 0 to after the data header.
         */
        static boolean validate(ByteBuffer bytes) {
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

        static ByteBuffer getData(ByteBuffer bytes, CharSequence key) {
            int index = binarySearch(bytes, key);
            if (index >= 0) {
                ByteBuffer data = bytes.duplicate();
                data.position(getDataOffset(bytes, index));
                data.limit(getDataOffset(bytes, index + 1));
                return ICUBinary.sliceWithOrder(data);
            } else {
                return null;
            }
        }

        static void addBaseNamesInFolder(ByteBuffer bytes, String folder, String suffix, Set<String> names) {
            // Find the first data item name that starts with the folder name.
            int index = binarySearch(bytes, folder);
            if (index < 0) {
                index = ~index;  // Normal: Otherwise the folder itself is the name of a data item.
            }

            int base = bytes.position();
            int count = bytes.getInt(base);
            StringBuilder sb = new StringBuilder();
            while (index < count && addBaseName(bytes, index, folder, suffix, sb, names)) {
                ++index;
            }
        }

        /**
         * @param bytes resource file bytes
         * @param key the name of the resource file
         * @return the index of a resource file by its name in the .dat file
         * table (ordering) of all contained resource files.
         */
        private static int binarySearch(ByteBuffer bytes, CharSequence key) {
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
                    return mid;
                }
            }
            return ~start;  // Not found or table is empty.
        }

        /**
         * @param bytes
         * @param index among the name data, index number for a resource within the ordering of all
         * contained resources
         * @return the offset for the name data for a contained resource file,
         * given its index number in the ordering of all contained resources.
         */
        private static int getNameOffset(ByteBuffer bytes, int index) {
            int base = bytes.position();
            assert 0 <= index && index < bytes.getInt(base);  // count
            // The count integer (4 bytes)
            // is followed by count (nameOffset, dataOffset) integer pairs (8 bytes per pair).
            return base + bytes.getInt(base + 4 + index * 8);
        }

        /**
         * @param bytes
         * @param index among the contents data, index number for a resource within the ordering of all
         * contained resources
         * @return the offset for the contents data for a contained resource file,
         * given its index number in the ordering of all contained resources.
         */
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

        /**
         * Add the name of the contained resource file to `names` given its index
         * number in the ordering of all contained resources, only if it is a
         * top-level file.
         * Also, return true if at least all files & folders' names are well-formed
         * (ex: have a '/' after the package name base).
         */
        static boolean addBaseName(ByteBuffer bytes, int index,
                String folder, String suffix, StringBuilder sb, Set<String> names) {
            int offset = getNameOffset(bytes, index);
            // Skip "icudt54b/".
            offset += ICUData.PACKAGE_NAME.length() + 1;
            if (folder.length() != 0) {
                // Test name.startsWith(folder + '/').
                for (int i = 0; i < folder.length(); ++i, ++offset) {
                    if (bytes.get(offset) != folder.charAt(i)) {
                        return false;
                    }
                }
                if (bytes.get(offset++) != '/') {
                    return false;
                }
            }
            // Collect the NUL-terminated name and test for a subfolder, then test for the suffix.
            sb.setLength(0);
            byte b;
            while ((b = bytes.get(offset++)) != 0) {
                char c = (char) b;
                if (c == '/') {
                    return true;  // Skip subfolder contents.
                }
                sb.append(c);
            }
            int nameLimit = sb.length() - suffix.length();
            if (sb.lastIndexOf(suffix, nameLimit) >= 0) {
                names.add(sb.substring(0, nameLimit));
            }
            return true;
        }
    }

    /**
     * Interface for ICU data files, whether a single .res file or a .dat
     * file that contains .res files.
     */
    private static abstract class DataFile {
        protected final String itemPath;

        DataFile(String item) {
            itemPath = item;
        }
        @Override
        public String toString() {
            return itemPath;
        }

        abstract ByteBuffer getData(String requestedPath);

        /**
         * @param folder The relative ICU data folder, like "" or "coll".
         * @param suffix Usually ".res".
         * @param names File base names relative to the folder are added without the suffix,
         *        for example "de_CH".
         */
        abstract void addBaseNamesInFolder(String folder, String suffix, Set<String> names);
    }

    /** .res file */
    private static final class SingleDataFile extends DataFile {
        private final File path;

        SingleDataFile(String item, File path) {
            super(item);
            this.path = path;
        }
        @Override
        public String toString() {
            return path.toString();
        }

        @Override
        ByteBuffer getData(String requestedPath) {
            if (requestedPath.equals(itemPath)) {
                return mapFile(path);
            } else {
                return null;
            }
        }

        @Override
        void addBaseNamesInFolder(String folder, String suffix, Set<String> names) {
            if (itemPath.length() > folder.length() + suffix.length() &&
                    itemPath.startsWith(folder) &&
                    itemPath.endsWith(suffix) &&
                    itemPath.charAt(folder.length()) == '/' &&
                    itemPath.indexOf('/', folder.length() + 1) < 0) {
                names.add(itemPath.substring(folder.length() + 1,
                        itemPath.length() - suffix.length()));
            }
        }
    }

    /** .dat file */
    private static final class PackageDataFile extends DataFile {
        /**
         * .dat package bytes, or null if not a .dat package.
         * position() is after the header.
         * Do not modify the position or other state, for thread safety.
         */
        private final ByteBuffer pkgBytes;

        PackageDataFile(String item, ByteBuffer bytes) {
            super(item);
            pkgBytes = bytes;
        }

        @Override
        ByteBuffer getData(String requestedPath) {
            return DatPackageReader.getData(pkgBytes, requestedPath);
        }

        @Override
        void addBaseNamesInFolder(String folder, String suffix, Set<String> names) {
            DatPackageReader.addBaseNamesInFolder(pkgBytes, folder, suffix, names);
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
            int diff = key.charAt(i) - c2;
            if (diff != 0) {
                return diff;
            }
        }
    }

    static int compareKeys(CharSequence key, byte[] bytes, int offset) {
        for (int i = 0;; ++i, ++offset) {
            int c2 = bytes[offset];
            if (c2 == 0) {
                if (i == key.length()) {
                    return 0;
                } else {
                    return 1;  // key > table key because key is longer.
                }
            } else if (i == key.length()) {
                return -1;  // key < table key because key is shorter.
            }
            int diff = key.charAt(i) - c2;
            if (diff != 0) {
                return diff;
            }
        }
    }

    // private classes for impls of file loading -------------------------------

    /**
     * Represents implementations of the {@code DataFiles} interface.
     *
     * {@code NORMAL} - locale lookups fallback to locales based on truncating
     * the locale ID string from right to left based on the last underscore. The
     * presence or absence of user-provided data files does not affect locale
     * fallback (parent) relationships among files.
     * See
     *
     * {@code OVERRIDE} - locale lookups are similar to {@code NORMAL}, but the
     * values in user-provided override data files for a locale supersede any
     * other values of built-in ICU data or {@code NORMAL} data files.
     */
    public enum DataFilesCategory { NORMAL, OVERRIDE };

    /**
     * An interface that represents the user-provided data files to look up
     * resource data before looking at built-in ICU data.
     */
    private static abstract class DataFiles {

        // A list storing all ICU data files specified by the user
        // that they want to be included in the data file loading lookup path.
        // The results are ultimately included in the data file lookups that occur
        // in the public getData and getRequiredData methods.
        protected final List<DataFile> dataFiles = new ArrayList<>();

        public DataFiles() {
            initializeDataFiles();
        }

        /**
         * Initialize the set of files to be stored in the list from {@code getDataFiles}.
         */
        abstract void initializeDataFiles();

        /**
         * Indicate whether the field {@code dataFiles} has a size > 0.
         */
        public boolean hasFiles() {
            return !dataFiles.isEmpty();
        };

        /**
         * Reset the collection of {@code DataFile}s with the files contained in the directories
         * in {@code dataPath}.  Use the empty string to clear the collection.
         *
         * This is analogous to ICU4C's u_setDataDirectory.
         *
         * @param dataPath New path of filesystem location(s) containing user-provided files to be
         * loaded.
         */
        public void setDataPath(String dataPath) {
            dataFiles.clear();
            addDataFilesFromPath(dataPath);
        }

        /**
         * This is public only for the concrete implementations that themselves are private.
         * @param dataPath base filepath in which new files should be detected and added
         *      to the current data files list.
         */
        public void addDataFilesFromPath(String dataPath) {
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
                    addDataFilesFromFolder(new File(path), new StringBuilder(), dataFiles);
                }
                if (sepIndex < 0) {
                    break;
                }
                pathStart = sepIndex + 1;
            }
        }

        private void addDataFilesFromFolder(File folder, StringBuilder itemPath,
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
                        dataFiles.add(new PackageDataFile(itemPath.toString(), pkgBytes));
                    }
                } else {
                    dataFiles.add(new SingleDataFile(itemPath.toString(), file));
                }
                itemPath.setLength(folderPathLength);
            }
        }

        /**
         * Loads an ICU binary data file and returns it as a ByteBuffer.
         * The buffer contents is normally read-only, but its position etc. can be modified.
         *
         * Note: method is public in order to support legacy private helper methods in
         * the superclass that support the legacy APIs which assume the non-override/overlay
         * style of loading resources.
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
        public ByteBuffer getData(ClassLoader loader, String resourceName,
                String itemPath, boolean required) {
            ByteBuffer bytes = this.getDataFromFile(itemPath);
            if (bytes != null) {
                return bytes;
            }
            if (loader == null) {
                loader = ClassLoaderUtil.getClassLoader(ICUData.class);
            }
            if (resourceName == null) {
                resourceName = ICUData.ICU_BASE_NAME + '/' + itemPath;
            }
            ByteBuffer buffer = null;
            try {
                @SuppressWarnings("resource")  // Closed by getByteBufferFromInputStreamAndCloseStream().
                InputStream is = ICUData.getStream(loader, resourceName, required);
                if (is == null) {
                    return null;
                }
                buffer = getByteBufferFromInputStreamAndCloseStream(is);
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
            return buffer;
        }

        private ByteBuffer getDataFromFile(String itemPath) {
            for (DataFile dataFile : dataFiles) {
                ByteBuffer data = dataFile.getData(itemPath);
                if (data != null) {
                    return data;
                }
            }
            return null;
        }

        /**
         * Note: method is public in order to support legacy private helper methods in
         * the superclass that support the legacy APIs which assume the non-override/overlay
         * style of loading resources.
         *
         * @param folder The relative ICU data folder, like "" or "coll".
         * @param suffix Usually ".res".
         * @param names File base names relative to the folder are added without the suffix,
         *        for example "de_CH".
         */
        public void addBaseNamesInFileFolder(String folder, String suffix, Set<String> names) {
            for (DataFile dataFile : dataFiles) {
                dataFile.addBaseNamesInFolder(folder, suffix, names);
            }
        }
    }

    static final class NormalDataFiles extends DataFiles {
        // Environment variable name for the path to ICU Data Overlay files.
        // Note: this matches the environment variable of the same name in ICU4C
        // that allows users to override data. (Whereas ICU_DATA can be ignored
        // when ICU_NO_USER_DATA_OVERRIDE is defined in ICU4C.)
        private static final String DATA_FILES_ENV_VAR = "ICU_DATA";

        /* (non-Javadoc)
         * @see com.ibm.icu.impl.ICUBinary.DataFiles#initializeDataFiles()
         */
        @Override
        void initializeDataFiles() {
            String dataPath = System.getenv(DATA_FILES_ENV_VAR);
            if (dataPath != null) {
                addDataFilesFromPath(dataPath);
            }

            // Normally com.ibm.icu.impl.ICUBinary.dataPath.
            dataPath = ICUConfig.get(ICUBinary.class.getName() + ".dataPath");
            if (dataPath != null) {
                addDataFilesFromPath(dataPath);
            }

        }
    }

    // VisibleForTesting
    public static final class OverrideDataFiles extends DataFiles {
        private static final String OVERRIDE_DATA_FILES_ENV_VAR = "ICU_OVERRIDE_DATA";

        @Override
        void initializeDataFiles() {
            String dataPath = System.getenv(OVERRIDE_DATA_FILES_ENV_VAR);

            if (dataPath != null) {
                addDataFilesFromPath(dataPath);
            }
        }

    }

    public static final NormalDataFiles NORMAL_DATA_FILES = new NormalDataFiles();

    public static final OverrideDataFiles OVERRIDE_DATA_FILES = new OverrideDataFiles();

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

    // public methods (existing API for NORMAL_DATA_FILES) ---------------------

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

        return getData(loader, resourceName, itemPath, required, DataFilesCategory.NORMAL);
    }

    /**
     * Loads an ICU binary data file and returns it as a ByteBuffer.
     * The buffer contents is normally read-only, but its position etc. can be modified.
     *
     * @param loader Used for loader.getResourceAsStream() unless the data is found elsewhere.
     * @param resourceName Resource name for use with the loader.
     * @param itemPath Relative ICU data item path, for example "root.res" or "coll/ucadata.icu".
     * @param required If the resource cannot be found,
     *        this method returns null (!required) or throws an exception (required).
     * @param dataFilesCategory Whether the data files parent chain is loaded as NORMAL, OVERRIDE, etc.
     * @return The data as a read-only ByteBuffer,
     *         or null if required==false and the resource could not be found.
     * @throws MissingResourceException if required==true and the resource could not be found
     */
    public static ByteBuffer getData(ClassLoader loader, String resourceName,
            String itemPath, boolean required, DataFilesCategory dataFilesCategory) {

        switch(dataFilesCategory) {
        case OVERRIDE:
            return OVERRIDE_DATA_FILES.getData(loader, resourceName, itemPath, required);
        case NORMAL:
        default:
            return NORMAL_DATA_FILES.getData(loader, resourceName, itemPath, required);
        }

    }

    @SuppressWarnings("resource")  // Closing a file closes its channel.
    private static ByteBuffer mapFile(File path) {
        FileInputStream file;
        try {
            file = new FileInputStream(path);
            FileChannel channel = file.getChannel();
            ByteBuffer bytes = null;
            try {
                bytes = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            } finally {
                file.close();
            }
            return bytes;
        } catch (FileNotFoundException ignored) {
            System.err.println(ignored);
        } catch (IOException ignored) {
            System.err.println(ignored);
        }
        return null;
    }

    /**
     * @param folder The relative ICU data folder, like "" or "coll".
     * @param suffix Usually ".res".
     * @param names File base names relative to the folder are added without the suffix,
     *        for example "de_CH".
     */
    public static void addBaseNamesInFileFolder(String folder, String suffix, Set<String> names) {
        NORMAL_DATA_FILES.addBaseNamesInFileFolder(folder, suffix, names);
    }

    // static utility functions-------------------------------------------------

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
        assert bytes != null && bytes.position() == 0;
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
                (bytes.get(20) << 24) |
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

    public static byte[] getBytes(ByteBuffer bytes, int length, int additionalSkipLength) {
        byte[] dest = new byte[length];
        bytes.get(dest);
        if (additionalSkipLength > 0) {
            skipBytes(bytes, additionalSkipLength);
        }
        return dest;
    }

    public static String getString(ByteBuffer bytes, int length, int additionalSkipLength) {
        CharSequence cs = bytes.asCharBuffer();
        String s = cs.subSequence(0, length).toString();
        skipBytes(bytes, length * 2 + additionalSkipLength);
        return s;
    }

    public static char[] getChars(ByteBuffer bytes, int length, int additionalSkipLength) {
        char[] dest = new char[length];
        bytes.asCharBuffer().get(dest);
        skipBytes(bytes, length * 2 + additionalSkipLength);
        return dest;
    }

    public static short[] getShorts(ByteBuffer bytes, int length, int additionalSkipLength) {
        short[] dest = new short[length];
        bytes.asShortBuffer().get(dest);
        skipBytes(bytes, length * 2 + additionalSkipLength);
        return dest;
    }

    public static int[] getInts(ByteBuffer bytes, int length, int additionalSkipLength) {
        int[] dest = new int[length];
        bytes.asIntBuffer().get(dest);
        skipBytes(bytes, length * 4 + additionalSkipLength);
        return dest;
    }

    public static long[] getLongs(ByteBuffer bytes, int length, int additionalSkipLength) {
        long[] dest = new long[length];
        bytes.asLongBuffer().get(dest);
        skipBytes(bytes, length * 8 + additionalSkipLength);
        return dest;
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
    public static ByteBuffer getByteBufferFromInputStreamAndCloseStream(InputStream is) throws IOException {
        try {
            // is.available() may return 0, or 1, or the total number of bytes in the stream,
            // or some other number.
            // Do not try to use is.available() == 0 to find the end of the stream!
            byte[] bytes;
            int avail = is.available();
            if (avail > 32) {
                // There are more bytes available than just the ICU data header length.
                // With luck, it is the total number of bytes.
                bytes = new byte[avail];
            } else {
                bytes = new byte[128];  // empty .res files are even smaller
            }
            // Call is.read(...) until one returns a negative value.
            int length = 0;
            for(;;) {
                if (length < bytes.length) {
                    int numRead = is.read(bytes, length, bytes.length - length);
                    if (numRead < 0) {
                        break;  // end of stream
                    }
                    length += numRead;
                } else {
                    // See if we are at the end of the stream before we grow the array.
                    int nextByte = is.read();
                    if (nextByte < 0) {
                        break;
                    }
                    int capacity = 2 * bytes.length;
                    if (capacity < 128) {
                        capacity = 128;
                    } else if (capacity < 0x4000) {
                        capacity *= 2;  // Grow faster until we reach 16kB.
                    }
                    bytes = Arrays.copyOf(bytes, capacity);
                    bytes[length++] = (byte) nextByte;
                }
            }
            return ByteBuffer.wrap(bytes, 0, length);
        } finally {
            is.close();
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
