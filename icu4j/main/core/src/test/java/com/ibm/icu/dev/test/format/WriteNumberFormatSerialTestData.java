// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2001-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.test.format;

import com.ibm.icu.text.NumberFormat;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * @version 1.0
 * @author Ram Viswanadha
 */
public class WriteNumberFormatSerialTestData {
    static final String header =
            "/*\n"
                    + " *******************************************************************************\n"
                    + " * Copyright (C) 2001, International Business Machines Corporation and         *\n"
                    + " * others. All Rights Reserved.                                                *\n"
                    + " *******************************************************************************\n"
                    + " */\n\n"
                    + "package com.ibm.icu.dev.test.format;\n\n"
                    + "public class NumberFormatSerialTestData {\n"
                    + "    //get Content\n"
                    + "    public static byte[][] getContent() {\n"
                    + "            return content;\n"
                    + "    }\n";

    static final String footer =
            "\n    final static byte[][] content = {generalInstance, currencyInstance, percentInstance, scientificInstance};\n"
                    + "}\n";

    public static void main(String[] args) {
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        NumberFormat nfc = NumberFormat.getCurrencyInstance(Locale.US);
        NumberFormat nfp = NumberFormat.getPercentInstance(Locale.US);
        NumberFormat nfsp = NumberFormat.getScientificInstance(Locale.US);

        try (Writer writer =
                Files.newBufferedWriter(
                        Path.of("NumberFormatSerialTestData.java"), StandardCharsets.UTF_8)) {
            writer.write(header);
            write(writer, (Object) nf, "generalInstance", "//NumberFormat.getInstance(Locale.US)");
            write(
                    writer,
                    (Object) nfc,
                    "currencyInstance",
                    "//NumberFormat.getCurrencyInstance(Locale.US)");
            write(
                    writer,
                    (Object) nfp,
                    "percentInstance",
                    "//NumberFormat.getPercentInstance(Locale.US)");
            write(
                    writer,
                    (Object) nfsp,
                    "scientificInstance",
                    "//NumberFormat.getScientificInstance(Locale.US)");
            writer.write(footer);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private static void write(Writer writer, Object o, String name, String comment) {
        ByteArrayOutputStream bts = new ByteArrayOutputStream();
        try (ObjectOutputStream os = new ObjectOutputStream(bts)) {
            os.writeObject((Object) o);
            os.flush();
            byte[] myArr = bts.toByteArray();
            // String temp = new String(myArr);
            System.out.println("    " + comment + " :");
            /*System.out.println("minimumIntegerDigits : "  + (temp.indexOf("minimumIntegerDigits")+"minimumIntegerDigits".length()));
            System.out.println("maximumIntegerDigits : "  + (temp.indexOf("maximumIntegerDigits")+"maximumIntegerDigits".length()));
            System.out.println("minimumFractionDigits : " + (temp.indexOf("minimumFractionDigits")+"minimumFractionDigits".length()));
            System.out.println("maximumFractionDigits : " + (temp.indexOf("maximumFractionDigits")+"maximumFractionDigits".length()));
            */
            // file.write(myArr);
            writer.write("\n    " + comment);
            writer.write("\n    static byte[] " + name + " = new byte[]{ \n");
            writer.write("        ");
            for (int i = 0; i < myArr.length; i++) {
                writer.write(String.valueOf((int) myArr[i]));
                writer.write(", ");
                if ((i + 1) % 20 == 0) {
                    writer.write("\n");
                    writer.write("        ");
                }
            }
            writer.write("\n    };\n");
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
