/**
 *******************************************************************************
 * Copyright (C) 2001-2002, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/util/ICUServiceTest.java,v $
 * $Date: 2002/09/14 21:36:30 $
 * $Revision: 1.5 $
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.ICUNotifier;
import com.ibm.icu.impl.ICURWLock;
import com.ibm.icu.impl.ICUService;
import com.ibm.icu.impl.ICUService.Factory;
import com.ibm.icu.impl.ICUService.Key;
import com.ibm.icu.impl.ICUService.ServiceListener;
import com.ibm.icu.impl.ICUService.SimpleFactory;
import com.ibm.icu.impl.LocaleUtility;
import com.ibm.icu.impl.ICULocaleData;
import com.ibm.icu.impl.ICULocaleService;
import com.ibm.icu.impl.ICULocaleService.LocaleKey;
import com.ibm.icu.impl.ICULocaleService.MultipleKeyFactory;
import com.ibm.icu.impl.ICULocaleService.ICUResourceBundleFactory;

import java.util.Arrays;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

public class ICUServiceTest extends TestFmwk
{    
    public static void main(String[] args) throws Exception {
	ICUServiceTest test = new ICUServiceTest();
	test.run(args);
    }

    private String lrmsg(String message, Object lhs, Object rhs) {
	return message + " lhs: " + lhs + " rhs: " + rhs;
    }

    public void confirmBoolean(String message, boolean val) {
	logln(message, val, !val);
    }

    public void confirmEqual(String message, Object lhs, Object rhs) {
	logln(lrmsg(message, lhs, rhs), lhs == null ? rhs == null : lhs.equals(rhs));
    }

    public void confirmIdentical(String message, Object lhs, Object rhs) {
	logln(lrmsg(message, lhs, rhs), lhs == rhs);
    }

    public void confirmIdentical(String message, int lhs, int rhs) {
	logln(message + " lhs: " + lhs + " rhs: " + rhs, lhs == rhs);
    }

    // use locale keys
    static final class TestService extends ICUService {
	protected Key createKey(String id) {
	    return LocaleKey.createWithCanonicalFallback(id, null); // no fallback locale
	}
    }

    public void testAPI() {
	// create a service using locale keys,
	ICUService service = new TestService();

	// register an object with one locale, 
	// search for an object with a more specific locale
	// should return the original object
	Integer singleton0 = new Integer(0);
	service.registerObject(singleton0, "en_US");
 	Object result = service.get("en_US_FOO");
	confirmIdentical("1) en_US_FOO -> en_US", result, singleton0);

	// register a new object with the more specific locale
	// search for an object with that locale
	// should return the new object
	Integer singleton1 = new Integer(1);
	service.registerObject(singleton1, "en_US_FOO");
	result = service.get("en_US_FOO");
	confirmIdentical("2) en_US_FOO -> en_US_FOO", result, singleton1);

	// search for an object that falls back to the first registered locale
	result = service.get("en_US_BAR");
	confirmIdentical("3) en_US_BAR -> en_US", result, singleton0);

	// get a list of the factories, should be two
	List factories = service.factories();
	confirmIdentical("4) factory size", factories.size(), 2);

	// register a new object with yet another locale
	// original factory list is unchanged
	Integer singleton2 = new Integer(2);
	service.registerObject(singleton2, "en");
	confirmIdentical("5) factory size", factories.size(), 2);

	// search for an object with the new locale
	// stack of factories is now en, en_US_FOO, en_US
	// search for en_US should still find en_US object
	result = service.get("en_US_BAR");
	confirmIdentical("6) en_US_BAR -> en_US", result, singleton0);

	// register a new object with an old id, should hide earlier factory using this id, but leave it there
	Integer singleton3 = new Integer(3);
	service.registerObject(singleton3, "en_US");
	factories = service.factories();
	confirmIdentical("9) factory size", factories.size(), 4);

	// should get data from that new factory
	result = service.get("en_US_BAR");
	confirmIdentical("10) en_US_BAR -> (3)", result, singleton3);

	// remove new factory
	// should have fewer factories again
	service.unregisterFactory((Factory)factories.get(0));
	factories = service.factories();
	confirmIdentical("11) factory size", factories.size(), 3);

	// should get original data again after remove factory
	result = service.get("en_US_BAR");
	confirmIdentical("12) en_US_BAR -> 0", result, singleton0);

	// shouldn't find unregistered ids
	result = service.get("foo");
	confirmIdentical("13) foo -> null", result, null);

	// should find non-canonical strings
	String[] resultID = new String[1];
	result = service.get("EN_us_fOo", resultID);
	confirmEqual("14) find non-canonical", resultID[0], "en_US_FOO");

	// should be able to register non-canonical strings and get them canonicalized
	service.registerObject(singleton3, "eN_ca_dUde");
	result = service.get("En_Ca_DuDe", resultID);
	confirmEqual("15) register non-canonical", resultID[0], "en_CA_DUDE");

	// should be able to register invisible factories, these will not
	// be visible by default, but if you know the secret password you
	// can still access these services...
	Integer singleton4 = new Integer(4);
	service.registerObject(singleton4, "en_US_BAR", false);
	result = service.get("en_US_BAR");
	confirmIdentical("17) get invisible", result, singleton4);
	
	// should not be able to locate invisible services
	Set ids = service.getVisibleIDs();
	confirmBoolean("18) find invisible", !ids.contains("en_US_BAR"));

	service.reset();
	// an anonymous factory than handles all ids
	{
	    Factory factory = new Factory() {
		    public Object create(Key key) {
			return LocaleUtility.getLocaleFromName(key.currentID());
		    }

		    public void updateVisibleIDs(Map result) {
		    }

		    public String getDisplayName(String id, Locale l) {
			return null;
		    }
		};
	    service.registerFactory(factory);

	    // anonymous factory will still handle the id
	    result = service.get(Locale.US.toString());
	    confirmEqual("21) locale", result, Locale.US);

	    // still normalizes id
	    result = service.get("EN_US_BAR");
	    confirmEqual("22) locale", result, LocaleUtility.getLocaleFromName("en_US_BAR"));
	    
	    // we can override for particular ids
	    service.registerObject(singleton3, "en_US_BAR");
	    result = service.get("en_US_BAR");
	    confirmIdentical("23) override super", result, singleton3);
	}

	// empty service should not recognize anything 
	service.reset();
	result = service.get("en_US");
	confirmIdentical("24) empty", result, null);

	// create a custom multiple key factory
	{
	    String[] xids = { "en_US_VALLEY_GIRL", 
			      "en_US_VALLEY_BOY",
			      "en_US_SURFER_GAL",
			      "en_US_SURFER_DUDE"
	    };
	    service.registerFactory(new TestMultipleFactory(xids));
	}

	// iterate over the visual ids returned by the multiple factory
	{
	    Set vids = service.getVisibleIDs();
	    Iterator iter = vids.iterator();
	    int count = 0;
	    while (iter.hasNext()) {
	        ++count;
		logln("  " + iter.next());
	    }
	    // four visible ids
	    confirmIdentical("25) visible ids", count, 4);
	}

	// iterate over the display names
	{
	    Map dids = service.getDisplayNames(Locale.GERMANY);
	    Iterator iter = dids.entrySet().iterator();
	    int count = 0;
	    while (iter.hasNext()) {
	        ++count;
	        Entry e = (Entry)iter.next();
	        logln("  " + e.getKey() + " -- > " + e.getValue());
	    }
	    // four display names, in german
	    confirmIdentical("26) display names", count, 4);
	}

	// no valid display name
	confirmIdentical("27) get display name", service.getDisplayName("en_US_VALLEY_GEEK"), null);

	{
	    String name = service.getDisplayName("en_US_SURFER_DUDE", Locale.US);
	    confirmEqual("28) get display name", name, "English (United States,SURFER,DUDE)");
	}

	// register another multiple factory
	{
	    String[] xids = {
		"en_US_SURFER_GAL", "en_US_SILICON", "en_US_SILICON_GEEK", "en_US"
	    };
	    service.registerFactory(new TestMultipleFactory(xids, "Rad dude"));
	}

	// this time, we have seven display names (we replaced surfer gal)
	{
	    Map dids = service.getDisplayNames(LocaleUtility.getLocaleFromName("es"));
	    Iterator iter = dids.entrySet().iterator();
	    int count = 0;
	    while (iter.hasNext()) {
	        ++count;
	        Entry e = (Entry)iter.next();
	        logln("  " + e.getKey() + " -- > " + e.getValue());
	    }
	    // seven display names, in spanish
	    confirmIdentical("29) display names", count, 7);
	}

	// we should get the display name corresponding to the actual id
	// returned by the id we used.
	{
	    String[] actualID = new String[1];
	    String id = "en_us_silicon_dude";
	    String dude = (String)service.get(id, actualID);
	    if (dude != null) {
		String displayName = service.getDisplayName(actualID[0], Locale.US);
		logln("found actual: " + dude + " with display name: " + displayName);
		confirmBoolean("30) found display name for actual", displayName != null);

		displayName = service.getDisplayName(id, Locale.US);
		logln("found query: " + dude + " with display name: " + displayName);
		confirmBoolean("31) found display name for query", displayName == null);
	    } else {
		errln("30) service could not find entry for " + id);
	    }

	    id = "en_US_BOZO";
	    String bozo = (String)service.get(id, actualID);
	    if (bozo != null) {
		String displayName = service.getDisplayName(actualID[0], Locale.US);
		logln("found actual: " + bozo + " with display name: " + displayName);
		confirmBoolean("32) found display name for actual", displayName != null);

		displayName = service.getDisplayName(id, Locale.US);
		logln("found actual: " + bozo + " with display name: " + displayName);
		confirmBoolean("33) found display name for query", displayName == null);
	    } else {
		errln("32) service could not find entry for " + id);
	    }
	}

	// hiding factory should obscure 'sublocales'
	{
	    String[] xids = {
		"en_US_VALLEY", "en_US_SILICON"
	    };
	    service.registerFactory(new TestHidingFactory(xids));
	}

	{
	    Map dids = service.getDisplayNames();
	    Iterator iter = dids.entrySet().iterator();
	    int count = 0;
	    while (iter.hasNext()) {
	        ++count;
	        Entry e = (Entry)iter.next();
	        logln("  " + e.getKey() + " -- > " + e.getValue());
	    }
	    confirmIdentical("31 hiding factory", count, 5);
	}

	{
	    Set xids = service.getVisibleIDs();
	    Iterator iter = xids.iterator();
	    while (iter.hasNext()) {
		String xid = (String)iter.next();
		logln(xid + "?  " + service.get(xid));
	    }

	    logln("valleygirl?  " + service.get("en_US_VALLEY_GIRL"));
	    logln("valleyboy?   " + service.get("en_US_VALLEY_BOY"));
	    logln("valleydude?  " + service.get("en_US_VALLEY_DUDE"));
	    logln("surfergirl?  " + service.get("en_US_SURFER_GIRL"));
	}

	// resource bundle factory.
	service.reset();
	service.registerFactory(new ICUResourceBundleFactory("Countries;Languages", true));

	// list all of the resources that really define Countries;Languages
	// this takes a long time to build the visible id list
	{
	    Set xids = service.getVisibleIDs();
	    StringBuffer buf = new StringBuffer("{");
	    boolean notfirst = false;
	    Iterator iter = xids.iterator();
	    while (iter.hasNext()) {
		String xid = (String)iter.next();
		if (notfirst) {
		    buf.append(", ");
		} else {
		    notfirst = true;
		}
		buf.append(xid);
	    }
	    buf.append("}");
	    logln(buf.toString());
	}

	// get all the display names of these resources
	// this should be fast since the display names were cached.
	{
	    Map names = service.getDisplayNames(LocaleUtility.getLocaleFromName("de_DE"));
	    StringBuffer buf = new StringBuffer("{");
	    Iterator iter = names.entrySet().iterator();
	    while (iter.hasNext()) {
		Entry e = (Entry)iter.next();
		String name = (String)e.getKey();
		String id = (String)e.getValue();
		buf.append("\n   " + name + " --> " + id);
	    }
	    buf.append("\n}");
	    logln(buf.toString());
	}

	service.registerFactory(new CalifornioLanguageFactory());
	// get all the display names of these resources
	{
	    Map names = service.getDisplayNames(LocaleUtility.getLocaleFromName("en_US_CA_SURFER"));
	    StringBuffer buf = new StringBuffer("{");
	    Iterator iter = names.entrySet().iterator();
	    while (iter.hasNext()) {
		Entry e = (Entry)iter.next();
		String name = (String)e.getKey();
		String id = (String)e.getValue();
		buf.append("\n   " + name + " --> " + id);
	    }
	    buf.append("\n}");
	    logln(buf.toString());
	}

	// test notification
	// simple registration
	{
	    ICULocaleService ls = new ICULocaleService();
	    ServiceListener l1 = new ServiceListener() {
		    private int n;
		    public void serviceChanged(ICUService s) {
			logln("listener 1 report " + n++ + " service changed: " + s);
		    }
		}; 
	    ls.addListener(l1);
	    ServiceListener l2 = new ServiceListener() {
		    private int n;
		    public void serviceChanged(ICUService s) {
			logln("listener 2 report " + n++ + " service changed: " + s);
		    }
		};
	    ls.addListener(l2);
	    logln("registering foo... ");
	    ls.registerObject("Foo", "en_FOO");
	    logln("registering bar... ");
	    ls.registerObject("Bar", "en_BAR");
	    logln("getting foo...");
	    logln((String)ls.get("en_FOO"));
	    logln("removing listener 2...");
	    ls.removeListener(l2);
	    logln("registering baz...");
	    ls.registerObject("Baz", "en_BAZ");
	    logln("removing listener 1");
	    ls.removeListener(l1);
	    logln("registering burp...");
	    ls.registerObject("Burp", "en_BURP");

	    // should only get one notification even if register multiple times
	    logln("... trying multiple registration");
	    ls.addListener(l1);
	    ls.addListener(l1);
	    ls.addListener(l1);
	    ls.addListener(l2);
	    ls.registerObject("Foo", "en_FOO");
	    logln("... registered foo");

	    // since in a separate thread, we can callback and not deadlock
	    ServiceListener l3 = new ServiceListener() {
		    private int n;
		    public void serviceChanged(ICUService s) {
			logln("listener 3 report " + n++ + " service changed...");
			if (s.get("en_BOINK") == null) { // don't recurse on ourselves!!!
			    logln("registering boink...");
			    s.registerObject("boink", "en_BOINK");
			}
		    }
		};
	    ls.addListener(l3);
	    logln("registering boo...");
	    ls.registerObject("Boo", "en_BOO");
	    logln("...done");

	    try {
		Thread.sleep(100);
	    }
	    catch (InterruptedException e) {
	    }
	}
    }

    
    static class TestMultipleFactory extends MultipleKeyFactory {
	protected final String[] ids;
	protected final String factoryID;

	public TestMultipleFactory(String[] ids) {
	    this(ids, "");
	}

	public TestMultipleFactory(String[] ids, String factoryID) {
	    this.ids = (String[])ids.clone();

	    if (factoryID == null || factoryID.length() == 0) {
		this.factoryID = "";
	    } else {
		this.factoryID = factoryID + ": ";
	    }
	}
    
	protected Object handleCreate(Key key) {
	    for (int i = 0; i < ids.length; ++i) {
		if (key.currentID().equalsIgnoreCase(ids[i])) {
		    return factoryID + key.canonicalID();
		}
	    }
	    return null;
	}

	protected Set handleGetSupportedIDs() {
            return new HashSet(Arrays.asList(ids));
	}

	protected String handleGetDisplayName(String id, Locale locale) {
	    return factoryID + LocaleUtility.getLocaleFromName(id).getDisplayName(locale);
	}
    }

    static class TestHidingFactory implements ICUService.Factory {
	protected final String[] ids;
	protected final String factoryID;

	public TestHidingFactory(String[] ids) {
	    this(ids, "Hiding");
	}

	public TestHidingFactory(String[] ids, String factoryID) {
	    this.ids = (String[])ids.clone();

	    if (factoryID == null || factoryID.length() == 0) {
		this.factoryID = "";
	    } else {
		this.factoryID = factoryID + ": ";
	    }
	}

	public Object create(Key key) {
	    for (int i = 0; i < ids.length; ++i) {
		if (LocaleUtility.isFallbackOf(ids[i], key.currentID())) {
		    return factoryID + key.canonicalID();
		}
	    }
	    return null;
	}

	public void updateVisibleIDs(Map result) {
	    for (int i = 0; i < ids.length; ++i) {
		String id = ids[i];
		Iterator iter = result.keySet().iterator();
		while (iter.hasNext()) {
		    if (LocaleUtility.isFallbackOf(id, (String)iter.next())) {
			iter.remove();
		    }
		}
		result.put(id, this);
	    }
	}

	public String getDisplayName(String id, Locale locale) {
	    return factoryID + LocaleUtility.getLocaleFromName(id).getDisplayName(locale);
	}
    }

    static class CalifornioLanguageFactory extends ICUResourceBundleFactory {
	CalifornioLanguageFactory() {
	    super("Countries;Languages", true);
	}
	
	private static String californio = "en_US_CA";
	private static String valley = californio + "_VALLEY";
	private static String surfer = californio + "_SURFER";
	private static String geek = californio + "_GEEK";

	public Set handleGetSupportedIDs() {
	    Set result = super.handleGetSupportedIDs();

	    result.add(californio);
	    result.add(valley);
	    result.add(surfer);
	    result.add(geek);

            return result;
	}

	protected String handleGetDisplayName(String id, Locale locale) {
	    String prefix = "";
	    String suffix = "";
	    String ls = locale.toString();
	    if (LocaleUtility.isFallbackOf(californio, ls)) {
		if (ls.equalsIgnoreCase(valley)) {
		    prefix = "Like, you know, it's so totally ";
		} else if (ls.equalsIgnoreCase(surfer)) {
		    prefix = "Dude, its ";
		} else if (ls.equalsIgnoreCase(geek)) {
		    prefix = "I'd estimate it's approximately ";
		} else {
		    prefix = "Huh?  Maybe ";
		}
	    }
	    if (LocaleUtility.isFallbackOf(californio, id)) {
		if (id.equalsIgnoreCase(valley)) {
		    suffix = "like the Valley, you know?  Let's go to the mall!";
		} else if (id.equalsIgnoreCase(surfer)) {
		    suffix = "time to hit those gnarly waves, Dude!!!";
		} else if (id.equalsIgnoreCase(geek)) {
		    suffix = "all systems go.  T-Minus 9, 8, 7...";
		} else {
		    suffix = "No Habla Englais";
		}
	    } else {
		suffix = super.handleGetDisplayName(id, locale);
	    }
		
	    return prefix + suffix;
	}
    }

    public void TestLocale() {
	ICULocaleService service = new ICULocaleService();
	service.registerObject("root", "");
	service.registerObject("german", "de");
	service.registerObject("german_Germany", "de_DE");
	service.registerObject("japanese", "ja");
	service.registerObject("japanese_Japan", "ja_JP");

	Object target = service.get("de_US");
	confirmEqual("test de_US", "german", target);

	target = service.get("za_PPP");
	confirmEqual("test zappp", "root", target);

	Locale loc = Locale.getDefault();
	Locale.setDefault(Locale.JAPANESE);
	target = service.get("za_PPP");
	confirmEqual("test with ja locale", "japanese", target);

	Set ids = service.getVisibleIDs();
	for (Iterator iter = ids.iterator(); iter.hasNext();) {
	    logln("id: " + iter.next());
	}

	Locale.setDefault(loc);
	ids = service.getVisibleIDs();
	for (Iterator iter = ids.iterator(); iter.hasNext();) {
	    logln("id: " + iter.next());
	}

	target = service.get("za_PPP");
	confirmEqual("test with en locale", "root", target);
    }

    public void errln(String msg) {
        System.out.println(msg);
        (new String[0])[1] = "foo";
    }

    // misc coverage tests
    public void TestCoverage() {
	// Key
	Key key = new Key("foobar");
	logln("ID: " + key.id());
	logln("canonicalID: " + key.canonicalID());
	logln("currentID: " + key.currentID());
	logln("has fallback: " + key.fallback());

	// SimpleFactory
	Object obj = new Object();
	SimpleFactory sf = new SimpleFactory(obj, "object");
	try {
	    sf = new SimpleFactory(null, null);
	    errln("didn't throw exception");
	}
	catch (IllegalArgumentException e) {
	    logln("OK: " + e.getMessage());
	}
	catch (Exception e) {
	    errln("threw wrong exception" + e);
	}
	logln(sf.getDisplayName("object", null));

	// ICUService
	ICUService service = new ICUService();
	service.registerFactory(sf);

	try {
	    service.get(null, null);
	    errln("didn't throw exception");
	}
	catch (NullPointerException e) {
	    logln("OK: " + e.getMessage());
	}
        /*
	catch (Exception e) {
	    errln("threw wrong exception" + e);
	}
        */
	try {
	    service.registerFactory(null);
	    errln("didn't throw exception");
	}
	catch (NullPointerException e) {
	    logln("OK: " + e.getMessage());
	}
	catch (Exception e) {
	    errln("threw wrong exception" + e);
	}

	try {
	    service.unregisterFactory(null);
	    errln("didn't throw exception");
	}
	catch (NullPointerException e) {
	    logln("OK: " + e.getMessage());
	}
	catch (Exception e) {
	    errln("threw wrong exception" + e);
	}

	logln("object is: " + service.get("object"));

	logln("stats: " + service.stats());

	// ICURWLock

	ICURWLock rwlock = new ICURWLock();
	rwlock.acquireRead();
	rwlock.releaseRead();

	rwlock.acquireWrite();
	rwlock.releaseWrite();
	logln("stats: " + rwlock.getStats());
	logln("stats: " + rwlock.clearStats());
	rwlock.acquireRead();
	rwlock.releaseRead();
	rwlock.acquireWrite();
	rwlock.releaseWrite();
	logln("stats: " + rwlock.getStats());

	try {
	    rwlock.releaseRead();
	    errln("no error thrown");
	}
	catch (InternalError e) {
	    logln("OK: " + e.getMessage());
	}

	try {
	    rwlock.releaseWrite();
	    errln("no error thrown");
	}
	catch (InternalError e) {
	    logln("OK: " + e.getMessage());
	}

	// LocaleKey
	// LocaleKey lkey = LocaleKey.create("en_US", "ja_JP");
	// lkey = LocaleKey.create(null, null);
	LocaleKey lkey = LocaleKey.createWithCanonicalFallback("en_US", "ja_JP");

	// MultipleKeyFactory 
	MultipleKeyFactory mkf = new MKFSubclass(false);
	logln("obj: " + mkf.create(lkey));
	logln(mkf.getDisplayName("foo", null));
	logln(mkf.getDisplayName("bar", null));
	mkf.updateVisibleIDs(new HashMap());

	MultipleKeyFactory invisibleMKF = new MKFSubclass(false);
	logln("obj: " + invisibleMKF.create(lkey));
	logln(invisibleMKF.getDisplayName("foo", null));
	logln(invisibleMKF.getDisplayName("bar", null));
	invisibleMKF.updateVisibleIDs(new HashMap());

	// ResourceBundleFactory
	ICUResourceBundleFactory rbf = new ICUResourceBundleFactory(true);
	logln("RB: " + rbf.create(lkey));
	// LocaleKey nokey = LocaleKey.create(null, null);
	// logln("RB: " + rbf.create(nokey));

	rbf = new ICUResourceBundleFactory("foobar", true);
	logln("RB: " + rbf.create(lkey));

	// ICUNotifier
	ICUNotifier nf = new ICUNSubclass();
	try {
	    nf.addListener(null);
	    errln("added null listener");
	}
	catch (NullPointerException e) {
	    logln(e.getMessage());
	}
	catch (Exception e) {
	    errln("got wrong exception");
	}

	try {
	    nf.addListener(new WrongListener());
	    errln("added wrong listener");
	}
	catch (InternalError e) {
	    logln(e.getMessage());
	}
	catch (Exception e) {
	    errln("got wrong exception");
	}

	try {
	    nf.removeListener(null);
	    errln("removed null listener");
	}
	catch (NullPointerException e) {
	    logln(e.getMessage());
	}
	catch (Exception e) {
	    errln("got wrong exception");
	}

	nf.removeListener(new MyListener());
	nf.notifyChanged();
	nf.addListener(new MyListener());
	nf.removeListener(new MyListener());
    }

    static class MyListener implements EventListener {
	public void heyMan() {
	}
    }

    static class WrongListener implements EventListener {
	public void sayWhat() {
	}
    }

    static class ICUNSubclass extends ICUNotifier {
	public boolean acceptsListener(EventListener l) {
	    return l instanceof MyListener;
	}

	public void notifyListener(EventListener l) {
	    ((MyListener)l).heyMan();
	}
    }

    static class MKFSubclass extends MultipleKeyFactory {
	MKFSubclass(boolean visible) {
	    super(visible);
	}

	public Object handleCreate(Key key) {
	    return null;
	}

	public Set handleGetSupportedIDs() {
            return null;
	}
    }
}
