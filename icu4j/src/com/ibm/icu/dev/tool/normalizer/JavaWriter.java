package com.ibm.tools.normalizer;

//import com.ibm.text.*;
import com.ibm.util.Utility;
import com.ibm.util.CompactCharArray;
import com.ibm.util.CompactByteArray;
import java.io.*;

/**
 * JavaWriter knows how to write data structures out to a Java source file
 */
class JavaWriter extends SourceWriter {
    PrintStream out;

    public JavaWriter(String name) throws FileNotFoundException {
        // Find the class name
        int cIndex = name.lastIndexOf('/');
        String cName = (cIndex >= 0) ? name.substring(cIndex+1) : name;

        out = new PrintStream(new FileOutputStream(name + ".java"));

        out.println(kHeader);
        out.println("class " + cName + " {");
    }

    public void close() {
        out.println("}");
        out.close();
        out = null;
    }

    public void write(String name, short value) {
        out.println("    static final short " + name + " = " + value + ";");
    }

    public void write(String name, int value) {
        out.println("    static final int " + name + " = " + value + ";");
    }

    public void writeHex(String name, char value) {
        out.println("    static final char " + name + " = 0x" + Integer.toString((int)value,16) + ";");
    }

    public void writeHex(String name, int value) {
        out.println("    static final int " + name + " = 0x" + Integer.toString(value,16) + ";");
    }

    public void write(String name, CompactCharArray array) {
        array.compact(false);
        out.println("");
        out.println("    static final CompactCharArray " + name + " = new CompactCharArray(");
        out.println(Utility.formatForSource(Utility.arrayToRLEString(array.getIndexArray())));
        out.println("        ," );
        out.println(Utility.formatForSource(Utility.arrayToRLEString(array.getValueArray())));
        out.println("        );" );
    }

    public void write(String name, CompactByteArray array) {
        array.compact(false);
        out.println("");
        out.println("    static final CompactByteArray " + name + " = new CompactByteArray(");
        out.println(Utility.formatForSource(Utility.arrayToRLEString(array.getIndexArray())));
        out.println("        ," );
        out.println(Utility.formatForSource(Utility.arrayToRLEString(array.getValueArray())));
        out.println("        );" );
    }

    public void write(String name, StringBuffer str) {
        out.println("");
        out.println("    static final String " + name + " = ");
        out.println(Utility.formatForSource(str.toString()));
        out.println("    ;");
    }

    public void write(String name, char[] array) {
        out.println("");
        out.println("    static final char[] " + name + " = Utility.RLEStringToCharArray(");
        out.println(Utility.formatForSource(Utility.arrayToRLEString(array)));
        out.println("    );");
    }

    public void write(String name, int[] array) {
        out.println("");
        out.println("    static final int[] " + name + " = Utility.RLEStringToIntArray(");
        out.println(Utility.formatForSource(Utility.arrayToRLEString(array)));
        out.println("    );");
    }

    static final String kHeader =
         "/*\n"
        +" * (C) Copyright IBM Corp. 1997-1998 - All Rights Reserved\n"
        +" *\n"
        +" * The program is provided 'as is' without any warranty express or\n"
        +" * implied, including the warranty of non-infringement and the implied\n"
        +" * warranties of merchantibility and fitness for a particular purpose.\n"
        +" * IBM will not be liable for any damages suffered by you as a result\n"
        +" * of using the Program. In no event will IBM be liable for any\n"
        +" * special, indirect or consequential damages or lost profits even if\n"
        +" * IBM has been advised of the possibility of their occurrence. IBM\n"
        +" * will not be liable for any third party claims against you.\n"
        +" */\n"
        +"\n"
        +"package com.ibm.text;\n"
        + "// This class is MACHINE GENERATED.  Run NormalizerBuilder to regenerate.\n"
        +"\n";
}

