package com.ibm.text.UCD;
import com.ibm.text.utility.*;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;


public final class Default implements UCD_Types {
    
    private static String ucdVersion = UCD.latestVersion;
    public static UCD ucd;
    public static Normalizer nfc;
    public static Normalizer nfd;
    public static Normalizer nfkc;
    public static Normalizer nfkd;
    public static Normalizer[] nf = new Normalizer[4];
    
    public static void ensureUCD() {
    	if (ucd == null) setUCD();
    }
    
    public static void setUCD(String version) {
    	setUcdVersion(version);
    	setUCD();
    }
    
    public static void setUCD() {
        ucd = UCD.make(getUcdVersion());
        nfd = nf[NFD] = new Normalizer(Normalizer.NFD, getUcdVersion());
        nfc = nf[NFC] = new Normalizer(Normalizer.NFC, getUcdVersion());
        nfkd = nf[NFKD] = new Normalizer(Normalizer.NFKD, getUcdVersion());
        nfkc = nf[NFKC] = new Normalizer(Normalizer.NFKC, getUcdVersion());
        System.out.println("Loaded UCD" + ucd.getVersion() + " " + (new Date(ucd.getDate())));
    }
    
    static DateFormat myDateFormat = new SimpleDateFormat("yyyy-MM-dd', 'HH:mm:ss' GMT'");
    static {
        myDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    
    public static String getDate() {
        return myDateFormat.format(new Date());
    }

    public static void setUcdVersion(String ucdVersion) {
        Default.ucdVersion = ucdVersion;
    }

    public static String getUcdVersion() {
        return ucdVersion;
    }

}