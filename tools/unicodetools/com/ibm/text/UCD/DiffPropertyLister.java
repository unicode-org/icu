package com.ibm.text.UCD;
import java.io.*;

class DiffPropertyLister extends PropertyLister {
    private UCD oldUCD;
        
    public DiffPropertyLister(String oldUCDName, String newUCDName, PrintStream output) {
        this.output = output;
        this.ucdData = UCD.make(newUCDName);
        if (oldUCDName != null) this.oldUCD = UCD.make(oldUCDName);
    }
    
    public byte status (int cp) {
        return INCLUDE;
    }
        
    public String propertyName(int cp) {
        return ucdData.getVersion();
    }
    
    /*
    public String optionalName(int cp) {
        if ((propMask & 0xFF00) == DECOMPOSITION_TYPE) {
            return Utility.hex(ucdData.getDecompositionMapping(cp));
        } else {
            return "";
        }
    }
    */
        

    public byte status(int lastCp, int cp) {
        /*if (cp == 0xFFFF) {
            System.out.println("# " + Utility.hex(cp));
        }
        */
        return ucdData.isAllocated(cp) && (oldUCD == null || !oldUCD.isAllocated(cp)) ? INCLUDE : EXCLUDE;
    }
    
    public int print() {
        String status;
        if (oldUCD != null) {
            status = "# Differences between " + ucdData.getVersion() + " and " + oldUCD.getVersion();
        } else {
            status = "# Allocated as of " + ucdData.getVersion();
        }
        output.println();
        output.println();
        output.println(status);
        output.println();
        System.out.println(status);
        int count = super.print();
        output.println();
        if (oldUCD != null) {
            output.println("# Total " + count + " new code points allocated in " + ucdData.getVersion());
        } else {
            output.println("# Total " + count + " code points allocated in " + ucdData.getVersion());
        }
        
        output.println();
        return count;
    }
        
}
    
