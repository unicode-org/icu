package com.ibm.text.UCD;
import com.ibm.text.utility.*;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;


public final class Default implements UCD_Types {
    
    public static String ucdVersion = UCD.latestVersion;
    public static UCD ucd;
    public static Normalizer nfc;
    public static Normalizer nfd;
    public static Normalizer nfkc;
    public static Normalizer nfkd;
    public static Normalizer[] nf = new Normalizer[4];
    
    public static void setUCD(String version) {
    	ucdVersion = version;
    	setUCD();
    }
    
    public static void setUCD() {
        ucd = UCD.make(ucdVersion);
        nfd = nf[NFD] = new Normalizer(Normalizer.NFD, ucdVersion);
        nfc = nf[NFC] = new Normalizer(Normalizer.NFC, ucdVersion);
        nfkd = nf[NFKD] = new Normalizer(Normalizer.NFKD, ucdVersion);
        nfkc = nf[NFKC] = new Normalizer(Normalizer.NFKC, ucdVersion);
        System.out.println("Loaded UCD" + ucd.getVersion() + " " + (new Date(ucd.getDate())));
    }
    
    static DateFormat myDateFormat = new SimpleDateFormat("yyyy-MM-dd','HH:mm:ss' GMT'");
    static {
        myDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    
    public static String getDate() {
        return myDateFormat.format(new Date());
    }

}