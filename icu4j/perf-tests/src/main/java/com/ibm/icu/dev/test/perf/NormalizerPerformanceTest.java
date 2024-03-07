// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
**********************************************************************
* Copyright (c) 2002-2009, International Business Machines           *
* Corporation and others.  All Rights Reserved.                      *
**********************************************************************
*/
package com.ibm.icu.dev.test.perf;

import com.ibm.icu.text.Normalizer;

public class NormalizerPerformanceTest extends PerfTest {
    
    String[] NFDFileLines;
    String[] NFCFileLines;
    String[] fileLines;
    
    
    public static void main(String[] args) throws Exception {
        new NormalizerPerformanceTest().run(args);
    }
    
    protected void setup(String[] args) {
        fileLines = readLines(fileName, encoding, bulk_mode);
        NFDFileLines = normalizeInput(fileLines, Normalizer.NFD);
        NFCFileLines = normalizeInput(fileLines, Normalizer.NFC);
    }
    
    // Test NFC Performance
    PerfTest.Function TestICU_NFC_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFDFileLines.length; i++) {
                    Normalizer.normalize(NFDFileLines[i], Normalizer.NFC);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFDFileLines.length; i++) {
                    totalChars = totalChars + NFDFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestICU_NFC_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFCFileLines.length; i++) {
                    Normalizer.normalize(NFCFileLines[i], Normalizer.NFC);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFCFileLines.length; i++) {
                    totalChars = totalChars + NFCFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestICU_NFC_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < fileLines.length; i++) {
                    Normalizer.normalize(fileLines[i], Normalizer.NFC);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < fileLines.length; i++) {
                    totalChars = totalChars + fileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    // Test NFD Performance
    PerfTest.Function TestICU_NFD_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFDFileLines.length; i++) {
                    Normalizer.normalize(NFDFileLines[i], Normalizer.NFD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFDFileLines.length; i++) {
                    totalChars = totalChars + NFDFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestICU_NFD_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFCFileLines.length; i++) {
                    Normalizer.normalize(NFCFileLines[i], Normalizer.NFD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFCFileLines.length; i++) {
                    totalChars = totalChars + NFCFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestICU_NFD_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < fileLines.length; i++) {
                    Normalizer.normalize(fileLines[i], Normalizer.NFD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < fileLines.length; i++) {
                    totalChars = totalChars + fileLines[i].length();
                }
                return totalChars;
            }
        };
    }

    // Test NFC Performance
    PerfTest.Function TestJDK_NFC_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFDFileLines.length; i++)
                    normalizerTest(NFDFileLines[i], true);
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFDFileLines.length; i++)
                    totalChars = totalChars + NFDFileLines[i].length();
                return totalChars;
            }
        };
    }
  
    PerfTest.Function TestJDK_NFC_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFCFileLines.length; i++)
                    normalizerTest(NFCFileLines[i], true);
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFCFileLines.length; i++)
                    totalChars = totalChars + NFCFileLines[i].length();
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestJDK_NFC_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < fileLines.length; i++)
                    normalizerTest(fileLines[i], true);
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < fileLines.length; i++)
                    totalChars = totalChars + fileLines[i].length();
                return totalChars;
            }
        };
    }
    
    // Test NFD Performance
    PerfTest.Function TestJDK_NFD_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFDFileLines.length; i++)
                    normalizerTest(NFDFileLines[i], false);
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFDFileLines.length; i++)
                    totalChars = totalChars + NFDFileLines[i].length();
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestJDK_NFD_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFCFileLines.length; i++)
                    normalizerTest(NFCFileLines[i], false);
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFCFileLines.length; i++)
                    totalChars = totalChars + NFCFileLines[i].length();
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestJDK_NFD_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < fileLines.length; i++)
                    normalizerTest(fileLines[i], false);
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < fileLines.length; i++)
                    totalChars = totalChars + fileLines[i].length();
                return totalChars;
            }
        };
    }
    // Test FCD Performance
    PerfTest.Function TestICU_FCD_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFDFileLines.length; i++) {
                    Normalizer.normalize(NFDFileLines[i], Normalizer.FCD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFDFileLines.length; i++) {
                    totalChars = totalChars + NFDFileLines[i].length();
                }
                return totalChars;
            }
        };
    }

    PerfTest.Function TestICU_FCD_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFCFileLines.length; i++) {
                    Normalizer.normalize(NFCFileLines[i], Normalizer.FCD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFCFileLines.length; i++) {
                    totalChars = totalChars + NFCFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestICU_FCD_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < fileLines.length; i++) {
                    Normalizer.normalize(fileLines[i], Normalizer.FCD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < fileLines.length; i++) {
                    totalChars = totalChars + fileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    // Test Quick Check Performance
    PerfTest.Function TestQC_NFC_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFDFileLines.length; i++) {
                    Normalizer.quickCheck(NFDFileLines[i], Normalizer.NFC,0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFDFileLines.length; i++) {
                    totalChars = totalChars + NFDFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestQC_NFC_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFCFileLines.length; i++) {
                    Normalizer.quickCheck(NFCFileLines[i], Normalizer.NFC,0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFCFileLines.length; i++) {
                    totalChars = totalChars + NFCFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestQC_NFC_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < fileLines.length; i++) {
                    Normalizer.quickCheck(fileLines[i], Normalizer.NFC,0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < fileLines.length; i++) {
                    totalChars = totalChars + fileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestQC_NFD_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFDFileLines.length; i++) {
                    Normalizer.quickCheck(NFDFileLines[i], Normalizer.NFD,0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFDFileLines.length; i++) {
                    totalChars = totalChars + NFDFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestQC_NFD_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFCFileLines.length; i++) {
                     Normalizer.quickCheck(NFCFileLines[i], Normalizer.NFD,0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFCFileLines.length; i++) {
                    totalChars = totalChars + NFCFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestQC_NFD_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < fileLines.length; i++) {
                     Normalizer.quickCheck(fileLines[i], Normalizer.NFD,0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < fileLines.length; i++) {
                    totalChars = totalChars + fileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestQC_FCD_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFDFileLines.length; i++) {
                     Normalizer.quickCheck(NFDFileLines[i], Normalizer.FCD,0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFDFileLines.length; i++) {
                    totalChars = totalChars + NFDFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestQC_FCD_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFCFileLines.length; i++) {
                     Normalizer.quickCheck(NFCFileLines[i], Normalizer.FCD,0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFCFileLines.length; i++) {
                    totalChars = totalChars + NFCFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestQC_FCD_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < fileLines.length; i++) {
                     Normalizer.quickCheck(fileLines[i], Normalizer.FCD,0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < fileLines.length; i++) {
                    totalChars = totalChars + fileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    // Test isNormalized Performance
    PerfTest.Function TestIsNormalized_NFC_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFDFileLines.length; i++) {
                     Normalizer.isNormalized(NFDFileLines[i], Normalizer.NFC, 0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFDFileLines.length; i++) {
                    totalChars = totalChars + NFDFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestIsNormalized_NFC_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFCFileLines.length; i++) {
                    Normalizer.isNormalized(NFCFileLines[i], Normalizer.NFC, 0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFCFileLines.length; i++) {
                    totalChars = totalChars + NFCFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestIsNormalized_NFC_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < fileLines.length; i++) {
                    Normalizer.isNormalized(fileLines[i], Normalizer.NFC, 0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < fileLines.length; i++) {
                    totalChars = totalChars + fileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestIsNormalized_NFD_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFDFileLines.length; i++) {
                     Normalizer.isNormalized(NFDFileLines[i], Normalizer.NFD, 0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFDFileLines.length; i++) {
                    totalChars = totalChars + NFDFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestIsNormalized_NFD_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFCFileLines.length; i++) {
                     Normalizer.isNormalized(NFCFileLines[i], Normalizer.NFD, 0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFCFileLines.length; i++) {
                    totalChars = totalChars + NFCFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestIsNormalized_NFD_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < fileLines.length; i++) {
                    Normalizer.isNormalized(fileLines[i], Normalizer.NFD, 0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < fileLines.length; i++) {
                    totalChars = totalChars + fileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestIsNormalized_FCD_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFDFileLines.length; i++) {
                     Normalizer.isNormalized(NFDFileLines[i], Normalizer.FCD, 0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFDFileLines.length; i++) {
                    totalChars = totalChars + NFDFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestIsNormalized_FCD_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < NFCFileLines.length; i++) {
                     Normalizer.isNormalized(NFCFileLines[i], Normalizer.FCD, 0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < NFCFileLines.length; i++) {
                    totalChars = totalChars + NFCFileLines[i].length();
                }
                return totalChars;
            }
        };
    }
    
    PerfTest.Function TestIsNormalized_FCD_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                for (int i = 0; i < fileLines.length; i++) {
                     Normalizer.isNormalized(fileLines[i], Normalizer.FCD, 0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                for (int i = 0; i < fileLines.length; i++) {
                    totalChars = totalChars + fileLines[i].length();
                }
                return totalChars;
            }
        };
    }
      
    /*
      private void printUsage() {
        System.out.println("Usage: " + this.getClass().getName() + " [OPTIONS] fileName\n"
                            + "\t-f or --fileName  \tfile to be used as test data\n"
                            + "\t-s or --sourceDir \tsource directory for files followed by path\n"
                            + "\t-e or --encoding  \tencoding of source files\n"
                            + "\t-b or --bulkMode  \tnormalize whole file at once\n"
                            + "\t-l or --lineMode  \tnormalize file one line at a time\n"
            );
        System.exit(1);
    }
    */
    
    String[] normalizeInput(String[] src, Normalizer.Mode mode) {
        String[] dest = new String[src.length];
        for (int i = 0; i < src.length; i++) {
            dest[i] = Normalizer.normalize(src[i], mode);
        }
        
        return dest;
    }
    
    /*
    void normalizerInit(boolean compose) {
        Class normalizer;
        boolean sun;
        
        try {
            normalizer = Class.forName("java.text.Normalizer");
            sun = false;
        } catch (ClassNotFoundException ex) {
            try {
                normalizer = Class.forName("sun.text.Normalizer");
                sun = true;
            } catch (ClassNotFoundException ex2) {
                throw new RuntimeException(
                        "Could not find sun.text.Normalizer nor java.text.Normalizer and their required subclasses");
            }
        }
        
        try {
            if (sun) {
                normalizerArgs = new Object[] { null, null, 0 };
                normalizerArgs[1] = normalizer.getField(compose ? "COMPOSE" : "DECOMP").get(null);
                normalizerMethod = normalizer.getMethod("normalize", new Class[] { String.class, normalizerArgs[1].getClass(), int.class });
                // sun.text.Normalizer.normalize(line, compose
                //   ? sun.text.Normalizer.COMPOSE
                //   : sun.text.Normalizer.DECOMP, 0);
            } else {
                normalizerArgs = new Object[] { null, null };
                normalizerArgs[1] = Class.forName("java.text.Normalizer$Form").getField(compose ? "NFC" : "NFD").get(null);
                normalizerMethod = normalizer.getMethod("normalize", new Class[] { CharSequence.class, normalizerArgs[1].getClass()});
                // java.text.Normalizer.normalize(line, compose
                //   ? java.text.Normalizer.Form.NFC
                //   : java.text.Normalizer.Form.NFD);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Reflection error -- could not load the JDK normalizer (" + normalizer.getName() + ")");
        }
    }
    
    void normalizerTest(String line) {
        try {
            normalizerArgs[0] = line;
            normalizerMethod.invoke(line, normalizerArgs);
        } catch (Exception ex) {
            if (ex instanceof InvocationTargetException) {
                Throwable cause = ex.getCause();
                cause.printStackTrace();
                throw new RuntimeException(cause.getMessage());
            } else {
                throw new RuntimeException("Reflection error -- could not run the JDK normalizer");
            }
        }
    }
    */

    void normalizerTest(String line, boolean compose) {
//        sun.text.Normalizer.normalize(line, compose
//            ? sun.text.Normalizer.COMPOSE
//            : sun.text.Normalizer.DECOMP, 0);
        java.text.Normalizer.normalize(line, compose
            ? java.text.Normalizer.Form.NFC
            : java.text.Normalizer.Form.NFD);
    }
}
