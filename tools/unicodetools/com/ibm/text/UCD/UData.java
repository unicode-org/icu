package com.ibm.text.UCD;

import java.io.*;
import com.ibm.text.utility.*;

class UData implements UCD_Types {
    String name;
    String decompositionMapping;
    String simpleUppercase;
    String simpleLowercase;
    String simpleTitlecase;
    String simpleCaseFolding;
    String fullUppercase;
    String fullLowercase;
    String fullTitlecase;
    String fullCaseFolding;
    String specialCasing = "";
    String bidiMirror;
    
    int codePoint = -1;
    float numericValue = Float.NaN;
    int binaryProperties; // bidiMirroring, compositionExclusions, PropList
    
    byte generalCategory = Cn;
    byte combiningClass = 0;
    byte bidiClass = BIDI_ON;
    byte decompositionType = NONE;
    byte numericType = NUMERIC_NONE;
    
    byte eastAsianWidth = EAN;
    byte lineBreak = LBXX;
    byte joiningType = JT_U;
    byte joiningGroup = NO_SHAPING;
    byte script = COMMON_SCRIPT;
    byte age = 0;
    
    static final UData UNASSIGNED = new UData();
    //static final UData NONCHARACTER = new UData();
    static {
        UNASSIGNED.name = "<unassigned>";
        UNASSIGNED.decompositionMapping = UNASSIGNED.bidiMirror
        = UNASSIGNED.simpleUppercase
        = UNASSIGNED.simpleLowercase
        = UNASSIGNED.simpleTitlecase = "";
        UNASSIGNED.fleshOut();
        
        /*NONCHARACTER.name = "<noncharacter>";
        NONCHARACTER.decompositionMapping = NONCHARACTER.bidiMirror
        = NONCHARACTER.simpleUppercase
        = NONCHARACTER.simpleLowercase
        = NONCHARACTER.simpleTitlecase = "";

        NONCHARACTER.binaryProperties = Noncharacter_Code_PointMask;
        NONCHARACTER.fleshOut();
        */
    }
    
    public UData (int codePoint) {
        this.codePoint = codePoint;
    }
    
    public UData () {
    }
    
    public boolean equals(Object that) {
        UData other = (UData) that;
        if (!name.equals(other.name)) return false;
        if (!decompositionMapping.equals(other.decompositionMapping)) return false;
        if (!simpleUppercase.equals(other.simpleUppercase)) return false;
        if (!simpleLowercase.equals(other.simpleLowercase)) return false;
        if (!simpleTitlecase.equals(other.simpleTitlecase)) return false;
        if (!simpleCaseFolding.equals(other.simpleCaseFolding)) return false;
        if (!fullUppercase.equals(other.fullUppercase)) return false;
        if (!fullLowercase.equals(other.fullLowercase)) return false;
        if (!fullTitlecase.equals(other.fullTitlecase)) return false;
        if (!fullCaseFolding.equals(other.fullCaseFolding)) return false;
        if (!specialCasing.equals(other.specialCasing)) return false;
        if (!bidiMirror.equals(other.bidiMirror)) return false;
        if (codePoint != other.codePoint) return false;
        if (numericValue != other.numericValue) return false;
        if (binaryProperties != other.binaryProperties) return false;
        if (generalCategory != other.generalCategory) return false;
        if (combiningClass != other.combiningClass) return false;
        if (bidiClass != other.bidiClass) return false;
        if (decompositionType != other.decompositionType) return false;
        if (numericType != other.numericType) return false;
        if (eastAsianWidth != other.eastAsianWidth) return false;
        if (lineBreak != other.lineBreak) return false;
        if (joiningType != other.joiningType) return false;
        if (joiningGroup != other.joiningGroup) return false;
        if (script != other.script) return false;
        if (age != other.age) return false;
        return true;
    }
    
    public void fleshOut() {
        String codeValue = UTF32.valueOf32(codePoint);
        
        if (decompositionMapping == null) decompositionMapping = codeValue;
        if (bidiMirror == null) bidiMirror = codeValue;
        
        if (simpleLowercase == null) simpleLowercase = codeValue;
        if (simpleCaseFolding == null) simpleCaseFolding = simpleLowercase;
        if (fullLowercase == null) fullLowercase = simpleLowercase;
        if (fullCaseFolding == null) fullCaseFolding = fullLowercase;
        
        if (simpleUppercase == null) simpleUppercase = codeValue;
        if (simpleTitlecase == null) simpleTitlecase = codeValue;
        if (fullUppercase == null) fullUppercase = simpleUppercase;
        
        if (fullTitlecase == null) fullTitlecase = simpleTitlecase;
    }
    
    public void compact() {
        fleshOut();
        String codeValue = UTF32.valueOf32(codePoint);
        
        if (fullTitlecase.equals(simpleTitlecase)) fullTitlecase = null;
        
        if (fullUppercase.equals(simpleUppercase)) fullUppercase = null;
        if (simpleTitlecase.equals(codeValue)) simpleTitlecase = null;
        if (simpleUppercase.equals(codeValue)) simpleUppercase = null;
        
        if (fullCaseFolding.equals(fullLowercase)) fullCaseFolding = null;
        if (fullLowercase.equals(simpleLowercase)) fullLowercase = null;
        if (simpleCaseFolding.equals(simpleLowercase)) simpleCaseFolding = null;
        if (simpleLowercase.equals(codeValue)) simpleLowercase = null;
        
        if (decompositionMapping.equals(codeValue)) decompositionMapping = null;
        if (bidiMirror.equals(codeValue)) bidiMirror = null;
    }
    
    public void setBinaryProperties(int binaryProperties) {
        this.binaryProperties = binaryProperties;
    }
    
    public boolean isLetter() {
        return ((1<<generalCategory) & UCD_Types.LETTER_MASK) != 0;
    }
    
    public static void writeString(DataOutputStream os, String s) throws IOException {
        if (s == null) {
            os.writeByte(0);            
        } else {
            os.writeByte(1);
            os.writeUTF(s);
        }
    }
    
    static final byte[] byteBuffer = new byte[256];
    
    public static String readString(DataInputStream is) throws IOException {
        int type = is.readUnsignedByte();
        if (type == 0) return null;
        return is.readUTF();
    }
    
    static final byte ABBREVIATED = 0, FULL = 1;
    
    public String toString() {
        return toString(FULL);
    }
    
    public String toString(byte style) {
        boolean full = style == FULL;
        StringBuffer result = new StringBuffer();
        String s = UTF32.valueOf32(codePoint);
        
        result.append("<e c='").append(Utility.quoteXML(codePoint)).append('\'');
        result.append(" hx='").append(Utility.hex(codePoint)).append('\'');
        if (full || script != COMMON_SCRIPT) result.append(" sn='").append(UCD_Names.SCRIPT[script]).append('\'');
        result.append(" n='").append(Utility.quoteXML(name)).append("'\r\n");
        
        int lastPos = result.length();
        
        if (full || generalCategory != Lo) result.append(" gc='").append(UCD_Names.GC[generalCategory]).append('\'');
        if (full || combiningClass != 0) result.append(" cc='").append(combiningClass & 0xFF).append('\'');
        if (full || decompositionType != NONE) result.append(" dt='").append(UCD_Names.DT[decompositionType]).append('\'');
        if (full || !s.equals(decompositionMapping)) result.append(" dm='").append(Utility.quoteXML(decompositionMapping)).append('\'');

        if (full || numericType != NUMERIC_NONE) result.append(" nt='").append(UCD_Names.NT[numericType]).append('\'');
        if (full || !Double.isNaN(numericValue)) result.append(" nv='").append(numericValue).append('\'');
        
        if (full || eastAsianWidth != EAN) result.append(" ea='").append(UCD_Names.EA[eastAsianWidth]).append('\'');
        if (full || lineBreak != LBAL) result.append(" lb='").append(UCD_Names.LB[lineBreak]).append('\'');
        if (full || joiningType != JT_U) result.append(" jt='").append(UCD_Names.JOINING_TYPE[joiningType]).append('\'');
        if (full || joiningGroup != NO_SHAPING) result.append(" jg='").append(UCD_Names.JOINING_GROUP[joiningGroup]).append('\'');
        if (full || age != 0) result.append(" ag='").append(UCD_Names.AGE[age]).append('\'');
        
        if (full || bidiClass != BIDI_L) result.append(" bc='").append(UCD_Names.BC[bidiClass]).append('\'');
        if (full || !bidiMirror.equals(s)) result.append(" bmg='").append(Utility.quoteXML(bidiMirror)).append('\'');
        
        if (lastPos != result.length()) {
            result.append("\r\n");
            lastPos = result.length();
        }
        
        //String bp = "";
        int bprops = binaryProperties;
        for (int i = 0; i < LIMIT_BINARY_PROPERTIES; ++i) {
            if ((bprops & (1<<i)) != 0) result.append(UCD_Names.BP[i]).append("='T' ");
        }
        if (lastPos != result.length()) {
            result.append("\r\n");
            lastPos = result.length();
        }
        
        if (full || !fullLowercase.equals(s)) result.append(" lc='").append(Utility.quoteXML(fullLowercase)).append('\'');
        if (full || !fullUppercase.equals(simpleUppercase)) result.append(" uc='").append(Utility.quoteXML(fullUppercase)).append('\'');
        if (full || !fullTitlecase.equals(fullUppercase)) result.append(" tc='").append(Utility.quoteXML(fullTitlecase)).append('\'');
        if (full || !fullCaseFolding.equals(fullLowercase)) result.append(" cf='").append(Utility.quoteXML(fullCaseFolding)).append('\'');
        
        if (full || !simpleLowercase.equals(simpleLowercase)) result.append(" slc='").append(Utility.quoteXML(simpleLowercase)).append('\'');
        if (full || !simpleUppercase.equals(simpleUppercase)) result.append(" suc='").append(Utility.quoteXML(simpleUppercase)).append('\'');
        if (full || !simpleTitlecase.equals(simpleUppercase)) result.append(" stc='").append(Utility.quoteXML(simpleTitlecase)).append('\'');
        if (full || !simpleCaseFolding.equals(simpleLowercase)) result.append(" sfc='").append(Utility.quoteXML(simpleCaseFolding)).append('\'');
        
        if (full || !specialCasing.equals("")) result.append(" fsc='").append(Utility.quoteXML(specialCasing)).append('\'');
        result.append("/>");
        return result.toString();
    }
    
    public void writeBytes(DataOutputStream os) throws IOException {
        compact();
        os.writeInt(codePoint);
        
        writeString(os, name);
        writeString(os, decompositionMapping);
        writeString(os, simpleUppercase);
        writeString(os, simpleLowercase);
        writeString(os, simpleTitlecase);
        writeString(os, simpleCaseFolding);
        writeString(os, fullUppercase);
        writeString(os, fullLowercase);
        writeString(os, fullTitlecase);
        writeString(os, fullCaseFolding);
        writeString(os, specialCasing);
        writeString(os, bidiMirror);
        
        os.writeFloat(numericValue);
        os.writeInt(binaryProperties);
        
        os.writeByte(generalCategory);
        os.writeByte(combiningClass);
        os.writeByte(bidiClass);
        os.writeByte(decompositionType);
        os.writeByte(numericType);
        os.writeByte(eastAsianWidth);
        os.writeByte(lineBreak);
        os.writeByte(joiningType);
        os.writeByte(joiningGroup);
        os.writeByte(script);
        os.writeByte(age);
    }
    
    public void readBytes(DataInputStream is) throws IOException {
        codePoint = is.readInt();
        
        name = readString(is);
        decompositionMapping = readString(is);
        simpleUppercase = readString(is);
        simpleLowercase = readString(is);
        simpleTitlecase = readString(is);
        simpleCaseFolding = readString(is);
        fullUppercase = readString(is);
        fullLowercase = readString(is);
        fullTitlecase = readString(is);
        fullCaseFolding = readString(is);
        specialCasing = readString(is);
        bidiMirror = readString(is);
        
        numericValue = is.readFloat();
        binaryProperties = is.readInt();
        
        generalCategory = is.readByte();
        combiningClass = is.readByte();
        bidiClass = is.readByte();
        decompositionType = is.readByte();
        numericType = is.readByte();
        eastAsianWidth = is.readByte();
        lineBreak = is.readByte();
        joiningType = is.readByte();
        joiningGroup = is.readByte();
        script = is.readByte();
        age = is.readByte();
        fleshOut();
        
        // HACK
        /*
        int bp = binaryProperties;
        bp &= ~(1 << CaseFoldTurkishI); // clear bit
        if (codePoint == 'i' || codePoint == 'I') {
            bp |= (1 << CaseFoldTurkishI);
        }
        if (bp != binaryProperties) {
            if (!HACK) {
                System.out.println("\tHACK Resetting CaseFoldTurkishI on U+" + Utility.hex(codePoint) + " " + name + " and others...");
                HACK = true;
            }
            binaryProperties = bp;
        }
        */
        
        /*
        if (generalCategory == Sm) {
            if ((binaryProperties & Math_PropertyMask) != 0) {
                if (!HACK) {
                    System.out.println("Stripping " + Utility.hex(codePoint) + " " + name + " and others...");
                    HACK = true;
                }
                binaryProperties &= ~Math_PropertyMask;
            }
        }
        */
    }
    static boolean HACK = false;
}