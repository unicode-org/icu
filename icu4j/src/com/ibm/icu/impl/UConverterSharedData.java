/**
*******************************************************************************
* Copyright (C) 2006, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
*******************************************************************************
*/ 
package com.ibm.icu.impl;

import java.nio.ByteBuffer;


/*
 * Defines the UConverterSharedData struct,
 * the immutable, shared part of UConverter.
 */
public class UConverterSharedData {
    //uint32_t structSize;            /* Size of this structure */
	public int structSize;            /* Size of this structure */
    //uint32_t referenceCounter;      /* used to count number of clients, 0xffffffff for static SharedData */
	public int referenceCounter;      /* used to count number of clients, 0xffffffff for static SharedData */
	public static final int MAX_VERSION_LENGTH=4;
	//agljport:todo const void *dataMemory;         /* from udata_openChoice() - for cleanup */
	//agljport:todo void *table;                    /* Unused. This used to be a UConverterTable - Pointer to conversion data - see mbcs below */

    //const UConverterStaticData *staticData; /* pointer to the static (non changing) data. */
	public UConverterStaticData staticData; /* pointer to the static (non changing) data. */

    //UBool                sharedDataCached;   /* TRUE:  shared data is in cache, don't destroy on close() if 0 ref.  FALSE: shared data isn't in the cache, do attempt to clean it up if the ref is 0 */
	public boolean                sharedDataCached;   /* TRUE:  shared data is in cache, don't destroy on close() if 0 ref.  FALSE: shared data isn't in the cache, do attempt to clean it up if the ref is 0 */
	/*UBool               staticDataOwned;   TRUE if static data owned by shared data & should be freed with it, NEVER true for udata() loaded statics. This ignored variable was removed to make space for sharedDataCached.   */

    //const UConverterImpl *impl;     /* vtable-style struct of mostly function pointers */
	//public UConverterImpl impl;     /* vtable-style struct of mostly function pointers */

	/*initial values of some members of the mutable part of object */
    //uint32_t toUnicodeStatus;
	public long toUnicodeStatus;

	/*
	 * Shared data structures currently come in two flavors:
	 * - readonly for built-in algorithmic converters
	 * - allocated for MBCS, with a pointer to an allocated UConverterTable
	 *   which always has a UConverterMBCSTable
	 *
	 * To eliminate one allocation, I am making the UConverterMBCSTable
	 * a member of the shared data. It is the last member so that static
	 * definitions of UConverterSharedData work as before.
	 * The table field above also remains to avoid updating all static
	 * definitions, but is now unused.
	 *
	 * markus 2003-nov-07
	 */
	public UConverterMBCSTable mbcs;

	public UConverterSharedData()
	{
		mbcs = new UConverterMBCSTable();
	}
	
	public UConverterSharedData(int structSize_, int referenceCounter_, UConverterStaticData staticData_, boolean sharedDataCached_,/* UConverterImpl impl_,*/ long toUnicodeStatus_)
	{
		this();
		structSize = structSize_;
		referenceCounter = referenceCounter_;
		staticData = staticData_;
		sharedDataCached = sharedDataCached_;
		//impl = impl_;
		toUnicodeStatus = toUnicodeStatus_;
	}

	/**
	 * UConverterImpl contains all the data and functions for a converter type.
	 * Its function pointers work much like a C++ vtable.
	 * Many converter types need to define only a subset of the functions;
	 * when a function pointer is NULL, then a default action will be performed.
	 *
	 * Every converter type must implement toUnicode, fromUnicode, and getNextUChar,
	 * otherwise the converter may crash.
	 * Every converter type that has variable-length codepage sequences should
	 * also implement toUnicodeWithOffsets and fromUnicodeWithOffsets for
	 * correct offset handling.
	 * All other functions may or may not be implemented - it depends only on
	 * whether the converter type needs them.
	 *
	 * When open() fails, then close() will be called, if present.
	 */
	//public class UConverterImpl {
	    //UConverterType type;
	    //UConverterToUnicode toUnicode;
/*	protected void doToUnicode(UConverterToUnicodeArgs args, int[] pErrorCode)
	{
	}
	
	public final void toUnicode(UConverterToUnicodeArgs args, int[] pErrorCode)
	{
		doToUnicode(args, pErrorCode);
	}
	
	//UConverterFromUnicode fromUnicode;
	protected void doFromUnicode(UConverterFromUnicodeArgs args, int[] pErrorCode)
	{
	}
	
	public final void fromUnicode(UConverterFromUnicodeArgs args, int[] pErrorCode)
	{
		doFromUnicode(args, pErrorCode);
	}
	
	protected int doGetNextUChar(UConverterToUnicodeArgs args, int[] pErrorCode)
	{
		return 0;
	}
	
	//UConverterGetNextUChar getNextUChar;
	public final int getNextUChar(UConverterToUnicodeArgs args, int[] pErrorCode)
	{
		return doGetNextUChar(args, pErrorCode);
	}
	
	//public interface UConverterImplLoadable extends UConverterImpl
	protected void doLoad(UConverterLoadArgs pArgs, short[] raw, int[] pErrorCode)
	{
	}
	
*/	
	protected void doUnload()
	{
	}

	/*
	//public interface UConverterImplOpenable extends UConverterImpl
	protected void doOpen(UConverter cnv, String name, String locale, long options, int[] pErrorCode)
	{
	}
    
	//UConverterOpen open;
	public final void open(UConverter cnv, String name, String locale, long options, int[] pErrorCode)
	{
		doOpen(cnv, name, locale, options, pErrorCode);
	}
	
	protected void doClose(UConverter cnv)
	{
	}
	
    //UConverterClose close;
	public final void close(UConverter cnv)
	{
		doClose(cnv);
	}
	
	protected void doReset(UConverter cnv, int choice)
	{
	}
	
	//typedef void (*UConverterReset) (UConverter *cnv, UConverterResetChoice choice);
	//UConverterReset reset;
	public final void reset(UConverter cnv, int choice)
	{
		doReset(cnv, choice);
	}

	//public interface UConverterImplVariableLength extends UConverterImpl
	protected void doToUnicodeWithOffsets(UConverterToUnicodeArgs args, int[] pErrorCode)
	{
	}
	
    //UConverterToUnicode toUnicodeWithOffsets;
	public final void toUnicodeWithOffsets(UConverterToUnicodeArgs args, int[] pErrorCode)
	{
		doToUnicodeWithOffsets(args, pErrorCode);
	}
	
	protected void doFromUnicodeWithOffsets(UConverterFromUnicodeArgs args, int[] pErrorCode)
	{
	}
	
    //UConverterFromUnicode fromUnicodeWithOffsets;
	public final void fromUnicodeWithOffsets(UConverterFromUnicodeArgs args, int[] pErrorCode)
	{
		doFromUnicodeWithOffsets(args, pErrorCode);
	}

	//public interface UConverterImplMisc extends UConverterImpl
	protected void doGetStarters(UConverter converter, boolean starters[], int[] pErrorCode)
	{
	}
	
    //UConverterGetStarters getStarters;
	public final void getStarters(UConverter converter, boolean starters[], int[] pErrorCode)
	{
		doGetStarters(converter, starters, pErrorCode);
	}
	
	protected String doGetName(UConverter cnv)
	{
		return "";
	}
	
    //UConverterGetName getName;
	public final String getName(UConverter cnv)
	{
		return doGetName(cnv);
	}
	
	protected void doWriteSub(UConverterFromUnicodeArgs pArgs, long offsetIndex, int[] pErrorCode)
	{
	}
	
	//UConverterWriteSub writeSub;
	public final void writeSub(UConverterFromUnicodeArgs pArgs, long offsetIndex, int[] pErrorCode)
	{
		doWriteSub(pArgs, offsetIndex, pErrorCode);
	}
	
	protected UConverter doSafeClone(UConverter cnv, byte[] stackBuffer, int[] pBufferSize, int[] status)
	{
		return new UConverter();
	}

    //UConverterSafeClone safeClone;
	public final UConverter  safeClone(UConverter cnv, byte[] stackBuffer, int[] pBufferSize, int[] status)
	{
		return doSafeClone(cnv, stackBuffer, pBufferSize, status);
	}
	
	protected void doGetUnicodeSet(UConverter cnv, UnicodeSet /*USetAdder* / sa, int /*UConverterUnicodeSet* / which, int[] pErrorCode)
	{
	}
	
    //UConverterGetUnicodeSet getUnicodeSet;
	//public final void getUnicodeSet(UConverter cnv, UnicodeSet /*USetAdder* / sa, int /*UConverterUnicodeSet* / which, int[] pErrorCode)
	//{
	//	doGetUnicodeSet(cnv, sa, which, pErrorCode);
	//}

	//}

	static final String DATA_TYPE = "cnv";
	private static final int CNV_DATA_BUFFER_SIZE = 25000;
	public static final int sizeofUConverterSharedData = 100;
	
	//static UDataMemoryIsAcceptable isCnvAcceptable;

	/**
	 * Load a non-algorithmic converter.
	 * If pkg==NULL, then this function must be called inside umtx_lock(&cnvCacheMutex).
	 
	// UConverterSharedData * load(UConverterLoadArgs *pArgs, UErrorCode *err)
	public static final UConverterSharedData load(UConverterLoadArgs pArgs, int[] err)
	{
	    UConverterSharedData mySharedConverterData = null;
	
	    if(err == null || ErrorCode.isFailure(err[0])) {
	        return null;
	    }
	
	    if(pArgs.pkg != null && pArgs.pkg.length() != 0) {
	         application-provided converters are not currently cached 
	        return UConverterSharedData.createConverterFromFile(pArgs, err);
	    }
	
	    //agljport:fix mySharedConverterData = getSharedConverterData(pArgs.name);
	    if (mySharedConverterData == null)
	    {
	        Not cached, we need to stream it in from file 
	        mySharedConverterData = UConverterSharedData.createConverterFromFile(pArgs, err);
	        if (ErrorCode.isFailure(err[0]) || (mySharedConverterData == null))
	        {
	            return null;
	        }
	        else
	        {
	             share it with other library clients 
	            //agljport:fix shareConverterData(mySharedConverterData);
	        }
	    }
	    else
	    {
	         The data for this converter was already in the cache.            
	         Update the reference counter on the shared data: one more client 
	        mySharedConverterData.referenceCounter++;
	    }
	
	    return mySharedConverterData;
	}
	
	Takes an alias name gets an actual converter file name
	 *goes to disk and opens it.
	 *allocates the memory and returns a new UConverter object
	 
	//static UConverterSharedData *createConverterFromFile(UConverterLoadArgs *pArgs, UErrorCode * err)
	public static final UConverterSharedData createConverterFromFile(UConverterLoadArgs pArgs, int[] err)
	{
	    UDataMemory data = null;
	    UConverterSharedData sharedData = null;
	
	    //agljport:todo UTRACE_ENTRY_OC(UTRACE_LOAD);
	
	    if (err == null || ErrorCode.isFailure(err[0])) {
	        //agljport:todo UTRACE_EXIT_STATUS(*err);
	        return null;
	    }
	
	    //agljport:todo UTRACE_DATA2(UTRACE_OPEN_CLOSE, "load converter %s from package %s", pArgs->name, pArgs->pkg);
	
	    //agljport:fix data = udata_openChoice(pArgs.pkgArray, DATA_TYPE.getBytes(), pArgs.name, isCnvAcceptable, null, err);
	    if(ErrorCode.isFailure(err[0]))
	    {
	        //agljport:todo UTRACE_EXIT_STATUS(*err);
	        return null;
	    }
	
	    sharedData = data_unFlattenClone(pArgs, data, err);
	    if(ErrorCode.isFailure(err[0]))
	    {
	        //agljport:fix udata_close(data);
	        //agljport:todo UTRACE_EXIT_STATUS(*err);
	        return null;
	    }
	
	    
	     * TODO Store pkg in a field in the shared data so that delta-only converters
	     * can load base converters from the same package.
	     * If the pkg name is longer than the field, then either do not load the converter
	     * in the first place, or just set the pkg field to "".
	     
	
	    return sharedData;
	}
*/
	UConverterDataReader dataReader = null;
	


	/*returns a converter type from a string
	 */
	// static const UConverterSharedData * getAlgorithmicTypeFromName(const char *realName)
	public static final UConverterSharedData getAlgorithmicTypeFromName(String realName)
	{
	    long mid, start, limit;
	    long lastMid;
	    int result;
	    StringBuffer strippedName = new StringBuffer(UConverterConstants.MAX_CONVERTER_NAME_LENGTH);
	
	    /* Lower case and remove ignoreable characters. */
	    UConverterAlias.io_stripForCompare(strippedName, realName);
	
	    /* do a binary search for the alias */
	    start = 0;
	    limit = cnvNameType.length;
	    mid = limit;
	    lastMid = UConverterAlias.UINT32_MAX;
	
	    for (;;) {
	        mid = (long)((start + limit) / 2);
	        if (lastMid == mid) {   /* Have we moved? */
	            break;  /* We haven't moved, and it wasn't found. */
	        }
	        lastMid = mid;
	        result = strippedName.substring(0).compareTo(cnvNameType[(int)mid].name);
	
	        if (result < 0) {
	            limit = mid;
	        } else if (result > 0) {
	            start = mid;
	        } else {
	            return converterData[cnvNameType[(int)mid].type];
	        }
	    }
	
	    return null;
	}

	/**
	 * Fallbacks to Unicode are stored outside the normal state table and code point structures
	 * in a vector of items of this type. They are sorted by offset.
	 */
	public final class MBCSToUFallback {
	    int offset;
	    int codePoint;
	}
	
	/**
	 * This is the MBCS part of the UConverterTable union (a runtime data structure).
	 * It keeps all the per-converter data and points into the loaded mapping tables.
	 */
	public final class UConverterMBCSTable {
	    /* toUnicode */
	    short countStates;
			byte dbcsOnlyState;
			boolean stateTableOwned;
	    int countToUFallbacks;
	
	    int stateTable[/*countStates*/][/*256*/];
	    int swapLFNLStateTable[/*countStates*/][/*256*/]; /* for swaplfnl */
	    char unicodeCodeUnits[/*countUnicodeResults*/];
	    MBCSToUFallback toUFallbacks[/*countToUFallbacks*/];
	
	    /* fromUnicode */
	    char fromUnicodeTable[];
	    byte fromUnicodeBytes[];
	    byte swapLFNLFromUnicodeBytes[]; /* for swaplfnl */
	    int fromUBytesLength;
	    short outputType, unicodeMask;
	
	    /* converter name for swaplfnl */
	    String swapLFNLName;
	
	    /* extension data */
	    UConverterSharedData baseSharedData;
	    //int extIndexes[];
		ByteBuffer extIndexes; // create int[] view etc. as needed

		UConverterMBCSTable()
		{
		}

		UConverterMBCSTable(UConverterMBCSTable t)
		{
			countStates = t.countStates;
			dbcsOnlyState = t.dbcsOnlyState;
			stateTableOwned = t.stateTableOwned;
			countToUFallbacks = t.countToUFallbacks;
			stateTable = t.stateTable;
			swapLFNLStateTable = t.swapLFNLStateTable;
			unicodeCodeUnits = t.unicodeCodeUnits;
			toUFallbacks = t.toUFallbacks;
			fromUnicodeTable = t.fromUnicodeTable;
			fromUnicodeBytes = t.fromUnicodeBytes;
			swapLFNLFromUnicodeBytes = t.swapLFNLFromUnicodeBytes;
			fromUBytesLength = t.fromUBytesLength;
			outputType = t.outputType;
			unicodeMask = t.unicodeMask;
			swapLFNLName = t.swapLFNLName;
			baseSharedData = t.baseSharedData;
			extIndexes = t.extIndexes;
		}			
	}

	/**
	 * MBCS data header. See data format description above.
	 */
	public final class MBCSHeader {
	    byte version[/*U_MAX_VERSION_LENGTH*/];
	    int countStates, countToUFallbacks, offsetToUCodeUnits, offsetFromUTable, offsetFromUBytes;
		int flags;
		int fromUBytesLength;

		public MBCSHeader()
		{
			version = new byte[MAX_VERSION_LENGTH];
		}
	}

	/**
	 * Enum for specifying basic types of converters
	 * @see getType
	 * @draft ICU 3.6
	 */
	public static final class UConverterType {
	    public static final int UNSUPPORTED_CONVERTER = -1;
	    public static final int SBCS = 0;
	    public static final int DBCS = 1;
	    public static final int MBCS = 2;
	    public static final int LATIN_1 = 3;
	    public static final int UTF8 = 4;
	    public static final int UTF16_BigEndian = 5;
	    public static final int UTF16_LittleEndian = 6;
	    public static final int UTF32_BigEndian = 7;
	    public static final int UTF32_LittleEndian = 8;
	    public static final int EBCDIC_STATEFUL = 9;
	    public static final int ISO_2022 = 10;
	
	    public static final int LMBCS_1 = 11;
	    public static final int LMBCS_2 = LMBCS_1 + 1; //12
	    public static final int LMBCS_3 = LMBCS_2 + 1; //13
	    public static final int LMBCS_4 = LMBCS_3 + 1; //14
	    public static final int LMBCS_5 = LMBCS_4 + 1; //15
	    public static final int LMBCS_6 = LMBCS_5 + 1; //16
	    public static final int LMBCS_8 = LMBCS_6 + 1; //17
	    public static final int LMBCS_11 = LMBCS_8 + 1; //18
	    public static final int LMBCS_16 = LMBCS_11 + 1; //19
	    public static final int LMBCS_17 = LMBCS_16 + 1; //20
	    public static final int LMBCS_18 = LMBCS_17 + 1; //21
	    public static final int LMBCS_19 = LMBCS_18 + 1; //22
	    public static final int LMBCS_LAST = LMBCS_19; //22
	    public static final int HZ =LMBCS_LAST + 1; //23
	    public static final int SCSU = HZ + 1; //24
	    public static final int ISCII = SCSU + 1; //25
	    public static final int US_ASCII = ISCII + 1; //26
	    public static final int UTF7 = US_ASCII + 1; //27
	    public static final int BOCU1 = UTF7 + 1; //28
	    public static final int UTF16 = BOCU1 + 1; //29
	    public static final int UTF32 = UTF16 + 1; //30
	    public static final int CESU8 = UTF32 + 1; //31
	    public static final int IMAP_MAILBOX = CESU8 + 1; //32
	    public static final int MAC_ARABIC = IMAP_MAILBOX + 1; //33
	    public static final int MAC_HEBREW = MAC_ARABIC + 1; //34
	
	    /* Number of converter types for which we have conversion routines. */
	    public static final int NUMBER_OF_SUPPORTED_CONVERTER_TYPES = MAC_HEBREW + 1;
	
	}
	
	/**
	 * Enum for specifying which platform a converter ID refers to.
	 * The use of platform/CCSID is not recommended. See openCCSID().
	 * @draft ICU 3.6
	 */
	public static final class UConverterPlatform {
	    public static final int UNKNOWN = -1;
	    public static final int IBM = 0;
	}

	static UConverterSharedData _MBCSData = null, /*_Latin1Data = null,*/ /*_UTF8Data = null,*/ /*_UTF16BEData = null,*/ /*_UTF16LEData = null,*/ /*_UTF32BEData = null,*/ /*_UTF32LEData = null,*/  /*_ISO2022Data = null,*/ _LMBCSData1 = null,_LMBCSData2 = null, _LMBCSData3 = null, _LMBCSData4 = null, _LMBCSData5 = null, _LMBCSData6 = null, _LMBCSData8 = null,_LMBCSData11 = null,_LMBCSData16 = null,_LMBCSData17 = null,_LMBCSData18 = null,_LMBCSData19 = null, _HZData = null, _SCSUData = null, /*_ISCIIData = null,*/ /*_ASCIIData = null,*/ _UTF7Data = null, _Bocu1Data = null, /*_UTF16Data = null, _UTF32Data = null,*/ _CESU8Data = null, _IMAPData = null;
	static UConverterSharedData[] converterData;
	static class cnvNameTypeClass {
	  String name;
		int type;
		cnvNameTypeClass(String name_, int type_) { name = name_; type = type_; }
	} 
	
	static cnvNameTypeClass cnvNameType[];
	
    static final String DATA_TYPE = "cnv";
    static final int CNV_DATA_BUFFER_SIZE = 25000;
    static final int SIZE_OF_UCONVERTER_SHARED_DATA = 100;

	static final int MAXIMUM_UCS2 =            0x0000FFFF;
	static final int MAXIMUM_UTF =             0x0010FFFF;
	static final int MAXIMUM_UCS4 =            0x7FFFFFFF;
	static final int HALF_SHIFT =              10;
	static final int HALF_BASE =               0x0010000;
	static final int HALF_MASK =               0x3FF;
	static final int SURROGATE_HIGH_START =    0xD800;
	static final int SURROGATE_HIGH_END =      0xDBFF;
	static final int SURROGATE_LOW_START =     0xDC00;
	static final int SURROGATE_LOW_END =       0xDFFF;
	
	/* -SURROGATE_LOW_START + HALF_BASE */
	static final int SURROGATE_LOW_BASE =      9216;
}
