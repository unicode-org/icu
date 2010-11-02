/*
 ***********************************************************************
 * Copyright (C) 2005-2006, International Business Machines            *
 * Corporation and others. All Rights Reserved.                        *
 ***********************************************************************
 *
 */

package com.ibm.icu.dev.tool.charsetdet.sbcs;

/**
 * @author emader
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Checker implements NGramParser.NGramParserClient
{
    private NGramList ngrams;
    private int totalNGrams;
    private int totalHits;
    
    private String language;
    private String encoding;
    
    private int[] histogram;

    private static final int BUFFER_SIZE = 1024;
    
    private char[] buffer;
    private int bufIndex;
    private int bufMax;

    private NGramParser parser;

    /**
     * TODO This should take cumulative percent and the name...
     */
    public Checker(NGramList list, InputFile dataFile)
    {
        ngrams = list;
        ngrams.setMapper(dataFile);
        
        language = languageName(dataFile.getFilename());
        encoding = dataFile.getEncoding();
        
        buffer = new char[BUFFER_SIZE];
        parser = new NGramParser(this);
        resetCounts();
        
        histogram = new int[100];
        resetHistogram();
   }
    
    public void handleNGram(String key)
    {
        NGramList.NGram ngram = ngrams.get(key);
        
        totalNGrams += 1;
        
        if (ngram != null) {
            totalHits += 1;
            //ngram.incrementRefCount();
        }
    }
    
    private void resetCounts()
    {
        bufIndex = 0;
        totalNGrams = totalHits = 0;
    }
    
    private void resetHistogram()
    {
        for(int i = 0; i < 100; i += 1) {
            histogram[i] = 0;
        }
        
    }
    
    private static void exceptionError(Exception e)
    {
        System.err.println("ioError: " + e.toString());
    }

    private static String languageName(String filename)
    {
        return filename.substring(0, filename.indexOf('.'));
    }
    
    private boolean nextBuffer(InputFile inputFile)
    {
        try {
            bufMax = inputFile.read(buffer);
        } catch (Exception e) {
            bufMax = -1;
            exceptionError(e);
            
            return false;
        }
        
        bufIndex = 0;
        
        return bufMax >= 0;
    }
    
    private void parseBuffer()
    {
        resetCounts();
        parser.reset();
        parser.parse();
    }
    
    public char nextChar()
    {
        if (bufIndex >= bufMax) {
            return 0;
        }
        
        return buffer[bufIndex++];
    }
    
    public String getLanguage()
    {
        return language;
    }
    
    public void setMapper(InputFile file)
    {
        ngrams.setMapper(file);
    }
    
    public int checkBuffer(char[] theBuffer, int charCount)
    {
        buffer = theBuffer;
        bufMax = charCount;
        
        parseBuffer();
        
        return totalHits;
    }
    
    public void check(InputFile dataFile)
    {
        int minHist = 101, maxHist = -1;
        
        dataFile.open();
        
        String dataFilename = dataFile.getFilename();
        String fileEncoding = dataFile.getEncoding();
        
        System.out.println(language + "(" + encoding + ") stats, " + languageName(dataFilename) + "(" + fileEncoding + ") data:");
        
        setMapper(dataFile);
        resetHistogram();

        while (nextBuffer(dataFile)) {
            parseBuffer();
            
            double percentHits = (double) totalHits / totalNGrams * 100.0;
            int ph = (int) percentHits;
            
            if (ph < minHist) {
                minHist = ph;
            }
            
            if (ph > maxHist) {
                maxHist = ph;
            }
            
            histogram[ph] += 1;
        }
        
        for(int ph = minHist; ph <= maxHist; ph += 1) {
            System.out.println(ph + "\t" + histogram[ph]);
        }
        
        System.out.println();
        
        dataFile.close();
        
        return;
    }
}
