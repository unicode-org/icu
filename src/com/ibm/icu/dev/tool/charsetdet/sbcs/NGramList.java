/*
 ***********************************************************************
 * Copyright (C) 2005-2006, International Business Machines            *
 * Corporation and others. All Rights Reserved.                        *
 ***********************************************************************
 *
 */

package com.ibm.icu.dev.tool.charsetdet.sbcs;

import java.util.Collection;
import java.util.TreeMap;

/**
 * @author emader
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class NGramList
{
    public interface NGramKeyMapper
    {
        Object mapKey(String key);
    }
    
    public static final class NGram implements Comparable
    {
        private String value;
        private int refCount;
        
        public NGram(String theValue, int theRefCount)
        {
            value    = theValue;
            refCount = theRefCount;
        }
        
        public NGram(String theValue)
        {
            this(theValue, 1);
        }
        
        public NGram(NGram other)
        {
            this(other.getValue(), other.getRefCount());
        }
        
        public final String getValue()
        {
            return value;
        }
        
        public final int getRefCount()
        {
            return refCount;
        }
        
        public final void incrementRefCount()
        {
            refCount += 1;
        }
        
        // Note: This makes higher refCounts come *before* lower refCounts...
        public int compareTo(Object o)
        {
            NGram ng = (NGram) o;
            
            return ng.getRefCount() - refCount;
        }
    }
    
    protected TreeMap ngrams;
    protected int totalNGrams;
    protected int uniqueNGrams;

    protected final int N_GRAM_SIZE = 3;
    
    private NGramKeyMapper keyMapper;

    /**
     * 
     */
    public NGramList(NGramKeyMapper theMapper)
    {
        keyMapper = theMapper;
        
        ngrams = new TreeMap();
        totalNGrams = uniqueNGrams = 0;
    }
    
    public void setMapper(NGramKeyMapper nGramKeyMapper)
    {
        keyMapper = nGramKeyMapper;
    }
    
    public NGram get(Object mappedKey)
    {
        return (NGram) ngrams.get(mappedKey);
    }
    
    public NGram get(String key)
    {
        Object mappedKey = keyMapper.mapKey(key);
        
        return get(mappedKey);
    }
    
    public void put(String key)
    {
        Object mappedKey = keyMapper.mapKey(key);
        NGram ngram = get(mappedKey);
        
        totalNGrams += 1;
        
        if (ngram == null) {
            uniqueNGrams += 1;
            ngrams.put(mappedKey, new NGram(key));
        } else {
            ngram.incrementRefCount();
        }
    }
    
    public Collection values()
    {
        return ngrams.values();
    }
    
    public Collection keys()
    {
        return ngrams.keySet();
    }
    
    public int getTotalNGrams()
    {
        return totalNGrams;
    }
    
    public int getUniqueNGrams()
    {
        return uniqueNGrams;
    }
}
