/*
 *******************************************************************************
 * Copyright (C) 1998-2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Dec 3, 2003
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

import java.util.*;

import com.ibm.icu.impl.Utility;

public class ClassTable implements LookupSubtable
{
    static class ClassEntry
    {
        private int glyphID;
        private int classID;
        
        public ClassEntry(int glyphID, int classID)
        {
            this.glyphID = glyphID;
            this.classID = classID;
        }
        
        public int getGlyphID()
        {
            return glyphID;
        }
        
        public int getClassID()
        {
            return classID;
        }
        
        public int compareTo(ClassEntry that)
        {
            return this.glyphID - that.glyphID;
        }
        
        //
        // Straight insertion sort from Knuth vol. III, pg. 81
        //
        public static void sort(ClassEntry[] table, Vector unsorted)
        {
            for (int e = 0; e < table.length; e += 1) {
                int i;
                ClassEntry v = (ClassEntry) unsorted.elementAt(e);

                for (i = e - 1; i >= 0; i -= 1) {
                    if (v.compareTo(table[i]) >= 0) {
                      break;
                    }

                    table[i + 1] = table[i];
                }

                table[i + 1] = v;
            }
        }
        
        public static int search(ClassEntry[] table, int glyphID)
        {
            int log2 = Utility.highBit(table.length);
            int power = 1 << log2;
            int extra = table.length - power;
            int probe = power;
            int index = 0;

            if (table[extra].glyphID <= glyphID) {
              index = extra;
            }

            while (probe > (1 << 0)) {
                probe >>= 1;

                if (table[index + probe].glyphID <= glyphID) {
                    index += probe;
                }
            }

            if (table[index].glyphID == glyphID) {
                return index;
            }

            return -1;
        }
    }
    
    static class ClassRangeRecord
    {
        private int startGlyphID;
        private int endGlyphID;
        private int classID;
        
        public ClassRangeRecord(int startGlyphID, int endGlyphID, int classID)
        {
            this.startGlyphID = startGlyphID;
            this.endGlyphID = endGlyphID;
            this.classID = classID;
        }
        
        public void write(OpenTypeTableWriter writer)
        {
            System.out.print(Utility.hex(startGlyphID, 6));
            System.out.print(" - ");
            System.out.print(Utility.hex(endGlyphID, 6));
            System.out.print(": ");
            System.out.println(classID);
            
            writer.writeData(startGlyphID);
            writer.writeData(endGlyphID);
            writer.writeData(classID);
        }
    }
    
    private Vector classMap;
    private ClassEntry[] classTable;
    private int snapshotSize;
    
    public ClassTable()
    {
        this.classMap = new Vector();
        this.classTable = null;
        this.snapshotSize = -1;
        
    }
    
    public void addMapping(int charID, int classID)
    {
        ClassEntry entry = new ClassEntry(charID, classID);
        
        classMap.addElement(entry);
    }
    
    public void addMapping(int startCharID, int endCharID, int classID)
    {
        for (int charID = startCharID; charID <= endCharID; charID += 1) {
            addMapping(charID, classID);
        }
    }
    
    public int getGlyphClassID(int glyphID)
    {
        int index = ClassEntry.search(classTable, glyphID);
        
        if (index >= 0) {
            return classTable[index].getClassID();
        }
        
        return 0;
    }
    
    public void snapshot()
    {
        if (snapshotSize != classMap.size()) {
            snapshotSize = classMap.size();
            classTable = new ClassEntry[snapshotSize];

            ClassEntry.sort(classTable, classMap);
        }
    }
    
    public void writeClassTable(OpenTypeTableWriter writer)
    {
        snapshot();
        
        Vector classRanges = new Vector();
        int startIndex = 0;
        
        while (startIndex < classTable.length) {
            int startID = classTable[startIndex].getGlyphID();
            int classID = classTable[startIndex].getClassID();
            int nextID = startID;
            int endID = startID;
            int endIndex;
            
            for (endIndex = startIndex; endIndex < classTable.length; endIndex += 1) {
                if (classTable[endIndex].getGlyphID() != nextID ||
                    classTable[endIndex].getClassID() != classID) {
                    break;
                }
                
                endID = nextID;
                nextID += 1;
            }
            
            if (classID != 0) {
                ClassRangeRecord range = new ClassRangeRecord(startID, endID, classID);
                
                classRanges.addElement(range);
            }
            
            startIndex = endIndex;
        }
        
        writer.writeData(2);                    // table format = 2 (class ranges)
        writer.writeData(classRanges.size());   // class range count
        
        for (int i = 0; i < classRanges.size(); i += 1) {
            ClassRangeRecord range = (ClassRangeRecord) classRanges.elementAt(i);
            
            range.write(writer);
        }
    }

    public void writeLookupSubtable(OpenTypeTableWriter writer)
    {
        int singleSubstitutionsBase = writer.getOutputIndex();
        int coverageTableIndex;
        
        snapshot();
        
        writer.writeData(2); // format 2: Specified output glyph indices
        coverageTableIndex = writer.getOutputIndex();
        writer.writeData(0); // offset to coverage table (fixed later)
        writer.writeData(classTable.length); // number of glyphIDs in substitution array
        
        for (int i = 0; i < classTable.length; i += 1) {
            writer.writeData(classTable[i].getClassID());
        }
        
        writer.fixOffset(coverageTableIndex, singleSubstitutionsBase);
        writer.writeData(1);
        writer.writeData(classTable.length);
        
        for (int i = 0; i < classTable.length; i += 1) {
            writer.writeData(classTable[i].getGlyphID());
        }
    }
}
    
    