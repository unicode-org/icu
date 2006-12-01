package com.ibm.text.UCD;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.text.utility.Pair;
import com.ibm.text.utility.Utility;

public class ChineseFrequency {
    static final String DICT_DIR = "C:\\DATA\\dict\\";
    static NumberFormat percent = new DecimalFormat("0.000000%");
    static NumberFormat percent3 = new DecimalFormat("000.000000%");
    static NumberFormat number = new DecimalFormat("#,##0");
    
    static class InverseCompareTo implements Comparator {
        public int compare(Object o1, Object o2) {
             return -((Comparable)o1).compareTo(o2);
        }        
    }
    
    public static void test() throws IOException{
        Set freq_char = new TreeSet(new InverseCompareTo());
        BufferedReader br = BagFormatter.openUTF8Reader(DICT_DIR, "kHYPLCDPF.txt");
        double grandTotal = 0.0;
        while (true) {
            String line = br.readLine();
            if (line == null) break;
            String[] pieces = Utility.split(line,'\t');
            int cp = Integer.parseInt(pieces[0],16);
            String[] says = Utility.split(pieces[1],',');
            long total = 0;
            for (int i = 0; i < says.length; ++i) {
                int start = says[i].indexOf('(');
                int end = says[i].indexOf(')');
                long count = Long.parseLong(says[i].substring(start+1, end));
                total += count;
            }
            grandTotal += total;
            freq_char.add(new Pair(new Long(total), new Integer(cp)));
        }
        br.close();
        PrintWriter pw = BagFormatter.openUTF8Writer(DICT_DIR,"kHYPLCDPF_frequency.txt");
        pw.write("\uFEFF");
        pw.println("No.\tPercentage\tAccummulated\tHex\tChar");

        Iterator it = freq_char.iterator();
        int counter = 0;
        double cummulative = 0;
        double cummulativePercentage = 0;
        while (it.hasNext()) {
            Pair item = (Pair)it.next();
            Long total = (Long) item.first;
            Integer cp = (Integer) item.second;
            double current = total.longValue();
            cummulative += current;
            double percentage = current / grandTotal;
            cummulativePercentage += percentage;
            pw.println(
                ++counter
            //+ "\t" + number.format(current)
            //+ "\t" + number.format(cummulative)
            + "\t" + percent.format(percentage)
            + "\t" + percent3.format(cummulativePercentage)
                + "\t" + Integer.toHexString(cp.intValue()).toUpperCase()
                + "\t" + UTF16.valueOf(cp.intValue()));
        }
        //pw.println("Grand total: " + (long)grandTotal);
        pw.close();
    }
}