/*
 *******************************************************************************
 * Copyright (C) 1996-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.rbbi;

import com.ibm.icu.util.CompactByteArray;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.impl.Utility;
import java.io.*;
import java.util.Vector;

public class BuildDictionaryFile {
    public static void main(String args[])
            throws FileNotFoundException, UnsupportedEncodingException, IOException {
        String filename = args[0];
        String encoding = "";
        String outputFile = "";
        String listingFile = "";

        if (args.length >= 2)
            encoding = args[1];

        if(args.length >= 3)
            outputFile = args[2];

        if (args.length >= 4)
            listingFile = args[3];

        BuildDictionaryFile dictionary = new BuildDictionaryFile();
        dictionary.build(filename, encoding);

        DataOutputStream out = null;
        if (outputFile.length() != 0) {
            out = new DataOutputStream(new FileOutputStream(outputFile));
            dictionary.writeDictionaryFile(out);
        }

        PrintWriter listing = null;
        if (listingFile.length() != 0) {
            listing = new PrintWriter(new OutputStreamWriter(new FileOutputStream(listingFile), "UnicodeLittle"));
            dictionary.printWordList("", 0, listing);
            listing.close();
        }
    }

    public BuildDictionaryFile() {
    }

    public void build(String filename, String encoding)
            throws FileNotFoundException, UnsupportedEncodingException, IOException {
        FileInputStream file = new FileInputStream(filename);
        InputStreamReader in;
        if (encoding.length() == 0)
            in = new InputStreamReader(file);
        else
            in = new InputStreamReader(file, encoding);

        buildColumnMap(in);

        file = new FileInputStream(filename);
        if (encoding.length() == 0)
            in = new InputStreamReader(file);
        else
            in = new InputStreamReader(file, encoding);

        buildStateTable(in);
//printTable();
    }

    public void buildColumnMap(InputStreamReader in) throws IOException {
System.out.println("Building column map...");
        UnicodeSet charsInFile = new UnicodeSet();
        int c = in.read();
int totalChars = 0;
        while (c >= 0) {
++totalChars; if (totalChars > 0 && totalChars % 5000 == 0) System.out.println("Read " + totalChars + " characters...");
            if (c > ' ')
                charsInFile.add((char)c);
            c = in.read();
        }
//        Test.debugPrintln(charsInFile.toString());

        StringBuffer tempReverseMap = new StringBuffer();
        tempReverseMap.append(' ');

        columnMap = new CompactByteArray();
        int n = charsInFile.getRangeCount();
        byte p = 1;
        for (int i=0; i<n; ++i) {
            char start = (char) charsInFile.getRangeStart(i);
            char end = (char) charsInFile.getRangeEnd(i);
            for (char ch = start; ch <= end; ch++) {
                if (columnMap.elementAt(Character.toLowerCase(ch)) == 0) {
                    columnMap.setElementAt(Character.toUpperCase(ch), Character.toUpperCase(ch),
                                        p);
                    columnMap.setElementAt(Character.toLowerCase(ch), Character.toLowerCase(ch),
                                        p);
                    ++p;
                    tempReverseMap.append(ch);
                }
            }
        }
//System.out.println("Compacting...");
        columnMap.compact();

//System.out.println(tempReverseMap.toString());
        reverseColumnMap = new char[p];
        if (0 != p) {
            tempReverseMap.getChars(0, p, reverseColumnMap, 0);
        }

        System.out.println("total columns = " + p);
        numCols = p;
        numColGroups = (numCols >> 5) + 1;

/*
short[] index = columnMap.getIndexArray();
System.out.println("Index:");
for (int i = 0; i < index.length; i++) {
if (i % 16 == 0) {
System.out.println();
System.out.print("    " + Integer.toHexString(i * 128) + ":");
}
System.out.print("\t" + Integer.toHexString(index[i]));
}
System.out.println();
byte[] data = columnMap.getStringArray();
System.out.print("Values:");
for (int i = 0; i < data.length; i++) {
if (i % 16 == 0) {
System.out.println();
System.out.print("    " + Integer.toHexString(i) + ":");
}
if (data[i] == 0)
System.out.print("\t.");
else
System.out.print("\t" + Integer.toString(data[i]));
}
System.out.println();
*/
    }

    public void buildStateTable(InputStreamReader in) throws IOException {
        Vector tempTable = new Vector();
        tempTable.addElement(new int[numCols + 1]);
        int state = 0;

        int c = in.read();
        int[] row = null;
        int charsInWord = 0;
        while (c >= 0) {
            charsInWord++;
            short column = columnMap.elementAt((char)c);

            row = (int[])(tempTable.elementAt(state));
            if (column != 0) {
                if (row[column] == 0) {
                    row[column] = tempTable.size();
                    ++row[numCols];
                    state = (tempTable.size());
                    tempTable.addElement(new int[numCols + 1]);
                }
                else
                    state = row[column];
            }
            else if (state != 0) {
                if (row[0] != -1) {
                    row[0] = -1;
                    ++row[numCols];
                    uniqueWords++;
                    totalUniqueWordChars += charsInWord;
                }
                totalWords++;
if (totalWords % 5000 == 0) System.out.println("Read " + totalWords + " words, " + tempTable.size() + " rows...");
                charsInWord = 0;
                state = 0;
            }
            c = in.read();
        }
        if (state != 0) {
            row = (int[])(tempTable.elementAt(state));
            if (row[0] != -1) {
                row[0] = -1;
                uniqueWords++;
                totalUniqueWordChars += charsInWord;
            }
            totalWords++;
        }

        compress(tempTable);

        table = new short[numCols * tempTable.size()];
        for (int i = 0; i < tempTable.size(); i++) {
            row = (int[])tempTable.elementAt(i);
            for (int j = 0; j < numCols; j++)
                table[i * numCols + j] = (short)row[j];
        }
    }

    private void compress(Vector tempTable) {
System.out.println("Before compression:");
System.out.println("  Number of rows = " + tempTable.size());
System.out.println("  Number of columns = " + numCols);
System.out.println("  Number of cells = " + tempTable.size() * numCols);
        deleteDuplicateRows(tempTable);
System.out.println("After removing duplicate rows:");
System.out.println("  Number of rows = " + tempTable.size());
System.out.println("  Number of columns = " + numCols);
System.out.println("  Number of cells = " + tempTable.size() * numCols);
        stackRows(tempTable);
if (tempTable.size() > 32767) throw new IllegalArgumentException("Too many rows in table!");
System.out.println("After doubling up on rows:");
System.out.println("  Number of rows = " + tempTable.size());
System.out.println("  Number of columns = " + numCols);
System.out.println("  Number of cells = " + tempTable.size() * numCols);
    }

/*
experimental...
    private void deleteDuplicateRows(Vector tempTable) {
        int[] rowNumMap = new int[tempTable.size()];
        for (int i = 0; i < rowNumMap.length; i++)
            rowNumMap[i] = i;

        int nextClass = numCols;
        int currentClass;
        int lastClass;
        boolean split;
        int[] row1, row2, tempRow;
        int tempCat;

        do {
System.out.println("Making a pass (" + nextClass + " classes)...");
            currentClass = 0;
            lastClass = nextClass;
            while (currentClass < nextClass) {
System.out.println("   currentClass = " + currentClass);
                split = false;
                row1 = row2 = null;
                for (int i = 0; i < tempTable.size(); i++) {
                    tempRow = (int[])tempTable.elementAt(i);
                    if (tempRow[numCols] == currentClass) {
                        if (row1 == null) {
                            row1 = (int[])tempTable.elementAt(i);
                        }
                        else {
                            row2 = (int[])tempTable.elementAt(i);
                            for (int j = 0; j < numCols; j++) {
                                if ((row1[j] == 0) != (row2[j] == 0) ||
                                            (row1[j] == -1) != (row2[j] == -1)) {
                                    row2[numCols] = nextClass;
                                    split = true;
                                    break;
                                }
                                else if (row1[j] != 0 && row2[j] != 0 && row1[j] != -1
                                                && row2[j] != -1) {
                                    tempRow = (int[])tempTable.elementAt(row1[j]);
                                    tempCat = tempRow[numCols];
                                    tempRow = (int[])tempTable.elementAt(row2[j]);
                                    if (tempCat != tempRow[numCols]) {
                                        row2[numCols] = nextClass;
                                        split = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                if (split)
                    ++nextClass;
                ++currentClass;
//System.out.println();
            }
        } while (lastClass != nextClass);

        int[] representatives = new int[nextClass];
        for (int i = 1; i < tempTable.size(); i++) {
            tempRow = (int[])tempTable.elementAt(i);
            if (representatives[tempRow[numCols]] == 0)
                representatives[tempRow[numCols]] = i;
            else
                rowNumMap[i] = representatives[tempRow[numCols]];
        }
System.out.println("Renumbering...");

        // renumber all remaining rows
        for (int i = 0; i < rowNumMap.length; i++)
            if (rowNumMap[i] != i)
                tempTable.setElementAt(null, i);
        int newRowNum = 0;
        for (int i = 0; i < rowNumMap.length; i++)
            if (tempTable.elementAt(i) != null)
                rowNumMap[i] = newRowNum++;
        for (int i = 0; i < rowNumMap.length; i++)
            if (tempTable.elementAt(i) == null)
                rowNumMap[i] = rowNumMap[rowNumMap[i]];

        for (int i = tempTable.size() - 1; i >= 0; i--) {
            tempRow = (int[])tempTable.elementAt(i);
            if (tempRow == null)
                tempTable.removeElementAt(i);
            else {
                for (int j = 0; j < numCols; j++)
                    if (tempRow[j] != -1)
                        tempRow[j] = rowNumMap[j];
            }
        }
//for (int i = 1; i < rowNumMap.length; i++) rowNumMap[i] = i; int newRowNum = rowNumMap.length;
    }
*/

    private void deleteDuplicateRows(Vector tempTable) {
        Vector work = (Vector)(tempTable.clone());
        boolean didDeleteRow = true;

        Vector tempMapping = new Vector(work.size());
        int[] mapping = new int[work.size()];
        for (int i = 0; i < mapping.length; i++) {
            mapping[i] = i;
            tempMapping.addElement(new Integer(i));
        }
        boolean[] tbd = new boolean[work.size()];

        while (didDeleteRow) {
System.out.println(" " + work.size() + " rows...");
int deletedRows = 0;
            didDeleteRow = false;

            sortTable(work, tempMapping, mapping, 1, work.size());
            for (int i = 0; i < work.size() - 1; ) {
System.out.print("Deleting, inspecting row " + i + ", deleted " + deletedRows + " rows...\r");
                int rowToDelete = ((Integer)(tempMapping.elementAt(i + 1))).intValue();
                int rowToMapTo = ((Integer)(tempMapping.elementAt(i))).intValue();
                if (compareRows((int[])work.elementAt(i), (int[])work.elementAt(i + 1),
                                mapping) == 0) {
                    tbd[rowToDelete] = true;
                    tempTable.setElementAt(null, rowToDelete);
                    while (tbd[mapping[rowToMapTo]])
                        mapping[rowToMapTo] = mapping[mapping[rowToMapTo]];
                    mapping[rowToDelete] = mapping[rowToMapTo];
                    didDeleteRow = true;
deletedRows++;
                    work.removeElementAt(i + 1);
                    tempMapping.removeElementAt(i + 1);
                }
                else
                    i++;
            }
            for (int i = 0; i < mapping.length; i++) {
                if (tbd[i] && tbd[mapping[i]])
                    mapping[i] = mapping[mapping[i]];
            }
        }

        int decrementBy = 0;
        for (int i = 0; i < mapping.length; i++) {
            if (tbd[i])
                decrementBy++;
            else
                mapping[i] -= decrementBy;
        }
        for (int i = 0; i < mapping.length; i++) {
            if (tbd[i])
                mapping[i] = mapping[mapping[i]];
        }
        for (int i = tempTable.size() - 1; i >= 0; i--) {
            if (tbd[i])
                tempTable.removeElementAt(i);
            else {
                int[] row = (int[])tempTable.elementAt(i);
                for (int j = 0; j < numCols; j++)
                    row[j] = (row[j] == -1) ? -1 : mapping[row[j]];
            }
        }
    }

    private void sortTable(Vector tbl, Vector tempMapping, int[] mapping, int start, int end) {
System.out.print("Sorting (" + start + ", " + end + ")...\r");
        if (start + 1 >= end)
            return;
        else if (start + 10 >= end) {
            for (int i = start + 1; i < end; i++) {
                int[] row = (int[])tbl.elementAt(i);
                Integer tempMap = (Integer)tempMapping.elementAt(i);
                int j;
                for (j = i - 1; j >= start; j--) {
                    if (compareRows((int[])tbl.elementAt(j), row, mapping) > 0) {
                        tbl.setElementAt((int[])tbl.elementAt(j), j + 1);
                        tempMapping.setElementAt((Integer)tempMapping.elementAt(j), j + 1);
                    }
                    else {
                        tbl.setElementAt(row, j + 1);
                        tempMapping.setElementAt(tempMap, j + 1);
                        break;
                    }
                }
                if (j < start) {
                    tbl.setElementAt(row, start);
                    tempMapping.setElementAt(tempMap, start);
                }
            }
        }
        else {
            int boundaryPos = (start + end) / 2;
            int i;
            boolean allTheSame = true;
            int firstDifferent = 0;

            do {
                int[] boundary = (int[])tbl.elementAt(boundaryPos);
                i = start;
                int j = end - 1;
                int[] row = null;
                byte compResult;
                while (i < j) {
                    row = (int[])tbl.elementAt(i);
                    while (i <= j && compareRows(row, boundary, mapping) < 0) {
                        i++;
                        row = (int[])tbl.elementAt(i);
                    }

                    row = (int[])tbl.elementAt(j);
                    compResult = compareRows(row, boundary, mapping);
                    while (i <= j && (compResult >= 0)) {
                        if (compResult != 0) {
                            allTheSame = false;
                            firstDifferent = j;
                        }
                        j--;
                        row = (int[])tbl.elementAt(j);
                        compResult = compareRows(row, boundary, mapping);
                    }
                    if (i <= j) {
                        row = (int[])tbl.elementAt(j);
                        tbl.setElementAt(tbl.elementAt(i), j);
                        tbl.setElementAt(row, i);
                        Object temp = tempMapping.elementAt(j);
                        tempMapping.setElementAt(tempMapping.elementAt(i), j);
                        tempMapping.setElementAt(temp, i);
                    }
                }
                if (i <= start) {
                    if (allTheSame)
                        return;
                    else
                        boundaryPos = firstDifferent;
                }
            } while (i <= start);
            sortTable(tbl, tempMapping, mapping, start, i);
            sortTable(tbl, tempMapping, mapping, i, end);
        }
    }

    private byte compareRows(int[] row1, int[] row2, int[] mapping) {
        for (int i = 0; i < numCols; i++) {
            int c1 = (row1[i] == -1) ? -1 : mapping[row1[i]];
            int c2 = (row2[i] == -1) ? -1 : mapping[row2[i]];
            if (c1 < c2)
                return -1;
            else if (c1 > c2)
                return 1;
        }
        return 0;
    }

    private int[] buildRowIndex(Vector tempTable) {
        int[] tempRowIndex = new int[tempTable.size()];
        rowIndexFlagsIndex = new short[tempTable.size()];
        Vector tempRowIndexFlags = new Vector();
        rowIndexShifts = new byte[tempTable.size()];

        // build the row index.  Each entry in the row index starts out referring
        // to the original row (so it doesn't actually do any mapping), and we set
        // up the index flags to show which cells in the row are populated
        for (int i = 0; i < tempTable.size(); i++) {
            tempRowIndex[i] = i;

            int[] row = (int[])tempTable.elementAt(i);
            if (row[numCols] == 1 && row[0] == 0) {
                int j = 0;
                while (row[j] == 0)
                    ++j;
                rowIndexFlagsIndex[i] = (short)(-j);
            }
            else {
                int[] flags = new int[numColGroups];
                int nextFlag = 1;
                int colGroup = 0;
                for (int j = 0; j < numCols; j++) {
                    if (row[j] != 0)
                        flags[colGroup] |= nextFlag;
                    nextFlag <<= 1;
                    if (nextFlag == 0) {
                        ++colGroup;
                        nextFlag = 1;
                    }
                }
                colGroup = 0;
                int j = 0;
                while (j < tempRowIndexFlags.size()) {
                    if (((Integer)tempRowIndexFlags.elementAt(j)).intValue() ==
                                    flags[colGroup]) {
                        ++colGroup;
                        ++j;
                        if (colGroup >= numColGroups)
                            break;
                    }
                    else if (colGroup != 0)
                        colGroup = 0;
                    else
                        ++j;
                }
                rowIndexFlagsIndex[i] = (short)(j - colGroup);
                while (colGroup < numColGroups) {
                    tempRowIndexFlags.addElement(new Integer(flags[colGroup]));
                    ++colGroup;
                }
            }
        }
        rowIndexFlags = new int[tempRowIndexFlags.size()];
        for (int i = 0; i < rowIndexFlags.length; i++)
            rowIndexFlags[i] = ((Integer)tempRowIndexFlags.elementAt(i)).intValue();
System.out.println("Number of column groups = " + numColGroups);
System.out.println("Size of rowIndexFlags = " + rowIndexFlags.length);

        return tempRowIndex;
    }

    private void stackRows(Vector tempTable) {
/*
System.out.print("Row:\t");
for (int i = 0; i < numCols; i++)
System.out.print(reverseColumnMap[i] + "\t");
System.out.println();
for (int i = 0; i < tempTable.size(); i++) {
System.out.print(Integer.toString(i) + ":\t");
int[] row = (int[])tempTable.elementAt(i);
for (int j = 0; j < row.length; j++)
if (row[j] != 0) System.out.print(Integer.toString(row[j]) + "\t");
else System.out.print(".\t");
System.out.println();
}
*/

        int[] tempRowIndex = buildRowIndex(tempTable);
        boolean[] tbd = new boolean[tempTable.size()];

        // now we actually go through and stack rows together
        for (int i = 0; i < tempTable.size(); i++) {
            if (tbd[i])
                continue;
System.out.print("Stacking, inspecting row " + i + "...\r");
//System.out.println("Stacking, inspecting row " + i + "...");

            int[] destRow = (int[])tempTable.elementAt(i);

            boolean[] tempFlags = new boolean[numCols];
            boolean[] filledCells = new boolean[numCols];
            for (int j = 0; j < numCols; j++)
                filledCells[j] = destRow[j] != 0;

            for (int j = i + 1; destRow[numCols] < numCols && j < tempTable.size(); j++) {
                if (tbd[j])
                    continue;

                int[] srcRow = (int[])tempTable.elementAt(j);
                if (srcRow[numCols] + destRow[numCols] > numCols)
                    continue;

                int maxLeftShift = -999;
                int maxRightShift = 0;
                for (int k = 0; k < numCols; k++) {
                    tempFlags[k] = srcRow[k] != 0;
                    if (tempFlags[k]) {
                        if (maxLeftShift == -999)
                            maxLeftShift = -k;
                        maxRightShift = (numCols - 1) - k;
                    }
                }

                int shift;
                for (shift = maxLeftShift; shift <= maxRightShift; shift++) {
                    int k;
                    for (k = 0; k < numCols; k++) {
                        if (tempFlags[k] && filledCells[k + shift])
                            break;
                    }
                    if (k >= numCols)
                        break;
                }
                if (shift <= maxRightShift) {
//System.out.println("Packing row " + j + " into row " + i + " with shift = " + shift);
                    for (int k = 0; k < numCols; k++) {
                        if (tempFlags[k]) {
                            filledCells[k + shift] = true;
                            destRow[k + shift] = srcRow[k];
                            ++destRow[numCols];
                        }
                    }
                    tbd[j] = true;
                    tempRowIndex[j] = i;
                    rowIndexShifts[j] = (byte)shift;
                }
            }
        }

        // finally, we squeeze out all the deleted rows
        int decrementBy = 0;
        for (int i = 0; i < tempRowIndex.length; i++) {
            if (!tbd[i])
                tempRowIndex[i] -= decrementBy;
            else
                ++decrementBy;
        }
        rowIndex = new short[tempRowIndex.length];
        for (int i = tempRowIndex.length - 1; i >= 0; i--) {
            if (tbd[i]) {
                rowIndex[i] = (short)(tempRowIndex[tempRowIndex[i]]);
                tempTable.removeElementAt(i);
            }
            else
                rowIndex[i] = (short)tempRowIndex[i];
        }
    }

//    private void printTable() {
//        short cell;
//        int populatedCells = 0;
///*
//        System.out.println("Conceptual table:");
//        System.out.println(" Row:   a   b   c   d   e   f   g   h   i   j   k   l   m   n"
//                + "   o   p   q   r   s   t   u   v   w   x   y   z   '   #");
//
//        boolean[] rowPrintFlags = new boolean[rowIndex.length];
//        printConceptualTable("", 0, rowPrintFlags);
//*/
//
//        System.out.println();
//        System.out.println("Conceptual table:");
//        System.out.print(" Row:");
//        for (int i = 0; i < reverseColumnMap.length; i++) {
//                System.out.print("   " + reverseColumnMap[i]);
//        }
//        for (int i = 0; i < rowIndex.length; i++) {
//            System.out.println();
//            printNumber(i, 4);
//            System.out.print(":");
//            for (int j = 0; j < numCols; j++)
//                printNumber(at(i, j), 4);
//        }
//        System.out.println('\n');
//
//        System.out.println();
//        System.out.println("Internally stored table:");
//        System.out.print(" Row:");
//        for (int i = 0; i < reverseColumnMap.length; i++) {
//                System.out.print("   " + reverseColumnMap[i]);
//        }
//        for (int i = 0; i < table.length; i++) {
//            if (i % numCols == 0) {
//                System.out.println();
//                printNumber(i / numCols, 4);
//                System.out.print(":");
//            }
//            cell = table[i];
//            if (cell != 0)
//                populatedCells++;
//            printNumber(cell, 4);
//        }
//        System.out.println('\n');
//
//System.out.println("Row index:");
//for (int i = 0; i < rowIndex.length; i++) {
//    System.out.print("   " + i + " -> " + rowIndex[i]);
//    if (rowIndexFlagsIndex[i] < 0)
//        System.out.print(", flags = " + Integer.toBinaryString((1 << (-rowIndexFlagsIndex[i]))) + " (" + rowIndexFlagsIndex[i]);
//    else
//        System.out.print(", flags = " + Integer.toBinaryString(rowIndexFlags[rowIndexFlagsIndex[i]]) + " (" + rowIndexFlagsIndex[i]);
//    System.out.println("), shift = " + rowIndexShifts[i]);
//}
///*
//        int theoreticalMinRows = populatedCells / numCols;
//        if (populatedCells % numCols != 0)
//            theoreticalMinRows++;
//        int oneCellRows = 0;
//        for (int i = 0; i < rowIndexFlags.length; i++) {
//            double temp = Math.log(rowIndexFlags[i]) / Math.log(2);
//            if (temp == (int)temp)
//                oneCellRows++;
//        }
//
//        System.out.println('\n');
//        System.out.println("Total words in input = " + totalWords);
//        System.out.println("Total unique words = " + uniqueWords + ", comprising " +
//                        totalUniqueWordChars + " characters\n");
//        System.out.println("Number of populated cells = " + populatedCells);
//        System.out.println("Total number of cells = " + (table.length));
//        System.out.println("Residency = " + ((float)populatedCells / table.length * 100) + '%');
//        System.out.println("Ratio of populated cells to unique-word characters = " +
//                        ((float)populatedCells / totalUniqueWordChars * 100) + '%');
//        System.out.println("Ratio of total cells to unique-word characters = " +
//                        ((float)table.length / totalUniqueWordChars * 100) + '%');
//        System.out.println("Number of rows = " + (table.length / numCols));
//        System.out.println("Theoretical minimum number of rows = " + theoreticalMinRows);
//        System.out.println("Ratio of number of rows to theoretical minimum = " +
//                        ((float)(table.length / numCols) / theoreticalMinRows * 100) + '%');
//        System.out.println("Number of conceptual rows = " + rowIndex.length);
//        System.out.println("Conceptual rows with only one populated cell = " + oneCellRows);
//        System.out.println("Ratio of one-cell rows to total conceptual rows = " + (((float)oneCellRows)
//                        / rowIndex.length * 100) + '%');
//        System.out.println("Average number of populated cells in multi-cell rows = " +
//                        ((float)(populatedCells - oneCellRows) / (rowIndex.length - oneCellRows)));
//
//        int storageUsed = table.length * 2 + rowIndex.length * 2
//                        + rowIndexFlags.length * 4 + rowIndexShifts.length;
//        System.out.println("Total number of bytes in table (including indexes) = " +
//                        storageUsed);
//        System.out.println("Bytes of overhead per unique-word character = " + ((double)(storageUsed
//                        - (totalUniqueWordChars * 2)) / totalUniqueWordChars));
//*/
//    }

//    private void printConceptualTable(String initialString, int state, boolean[] flags) {
//        if (initialString.length() == 0)
//            System.out.println("root:");
//        else
//            System.out.println(initialString + ':');
//
//        if (!flags[state]) {
//            flags[state] = true;
//            printNumber(state, 4);
//            System.out.print(":");
//            for (int i = 0; i < numCols; i++)
//                printNumber(at(state, i), 4);
//            System.out.println();
//        }
//
//        int nextState;
//        for (int i = 0; i < numCols; i++) {
//            nextState = at(state, i);
//            if (nextState > 0 && !flags[nextState]) {
//                printNumber(nextState, 4);
//                System.out.print(":");
//                for (int j = 0; j < numCols; j++)
//                    printNumber(at(nextState, j), 4);
//                System.out.println();
//            }
//        }
//        for (int i = 0; i < numCols; i++) {
//            nextState = at(state, i);
//            if (nextState > 0 && !flags[nextState]) {
//                char nextChar;
//                if (nextState == 27)
//                    nextChar = ' ';
//                else if (nextState == 26)
//                    nextChar = '\'';
//                else
//                    nextChar = (char)(i + 'a');
//                flags[nextState] = true;
//                printConceptualTable(initialString + nextChar, nextState, flags);
//            }
//        }
//    }

    private void printWordList(String partialWord, int state, PrintWriter out)
            throws IOException {
        if (state == -1) {
            System.out.println(partialWord);
            if (out != null)
                out.println(partialWord);
        }
        else {
            for (int i = 0; i < numCols; i++) {
                if (at(state, i) != 0)
                    printWordList(partialWord + reverseColumnMap[i], at(state, i), out);
            }
        }
    }

    private void writeDictionaryFile(DataOutputStream out) throws IOException {
        out.writeInt(0);    // version number

        char[] columnMapIndexes = columnMap.getIndexArray();
        out.writeInt(columnMapIndexes.length);
        for (int i = 0; i < columnMapIndexes.length; i++)
            out.writeShort((short)columnMapIndexes[i]);
        byte[] columnMapValues = columnMap.getValueArray();
        out.writeInt(columnMapValues.length);
        for (int i = 0; i < columnMapValues.length; i++)
            out.writeByte((byte)columnMapValues[i]);

        out.writeInt(numCols);
        out.writeInt(numColGroups);

        out.writeInt(rowIndex.length);
        for (int i = 0; i < rowIndex.length; i++)
            out.writeShort(rowIndex[i]);

        out.writeInt(rowIndexFlagsIndex.length);
        for (int i = 0; i < rowIndexFlagsIndex.length; i++)
            out.writeShort(rowIndexFlagsIndex[i]);
        out.writeInt(rowIndexFlags.length);
        for (int i = 0; i < rowIndexFlags.length; i++)
            out.writeInt(rowIndexFlags[i]);

        out.writeInt(rowIndexShifts.length);
        for (int i = 0; i < rowIndexShifts.length; i++)
            out.writeByte(rowIndexShifts[i]);

        out.writeInt(table.length);
        for (int i = 0; i < table.length; i++)
            out.writeShort(table[i]);

        out.close();
    }

//    private void printNumber(int x, int width) {
//        String s = String.valueOf(x);
//        if (width > s.length())
//            System.out.print(spaces.substring(0, width - s.length()));
//        if (x != 0)
//            System.out.print(s);
//        else
//            System.out.print('.');
//    }

    public final short at(int row, char ch) {
        int col = columnMap.elementAt(ch);
        return at(row, col);
    }

    public final short at(int row, int col) {
        if (cellIsPopulated(row, col))
            return internalAt(rowIndex[row], col + rowIndexShifts[row]);
        else
            return 0;
    }

    private final boolean cellIsPopulated(int row, int col) {
        if (rowIndexFlagsIndex[row] < 0)
            return col == -rowIndexFlagsIndex[row];
        else {
            int flags = rowIndexFlags[rowIndexFlagsIndex[row] + (col >> 5)];
            return (flags & (1 << (col & 0x1f))) != 0;
        }
    }

    private final short internalAt(int row, int col) {
        return table[row * numCols + col];
    }

    private CompactByteArray columnMap = null;
    private char[] reverseColumnMap = null;
    private int numCols;
    private int numColGroups;
    private short[] table = null;
    private short[] rowIndex = null;
    private int[] rowIndexFlags = null;
    private short[] rowIndexFlagsIndex = null;
    private byte[] rowIndexShifts = null;

    private int totalWords = 0;
    private int uniqueWords = 0;
    private int totalUniqueWordChars = 0;

    //private static final String spaces = "      ";
}

