REM Batch file for building the ICU4J source files.
REM Please make sure that your addition does not generate any errors.
REM Check the class dependencies.

REM make new class directory
mkdir classes
REM math packages
javac -d classes -classpath classes; src/com/ibm/math/*.java 

REM text and util packages, they are tightly dependent on each other
c:\work\jdk1.3\bin\javac -d classes -classpath classes src/com/ibm/text/*.java src/com/ibm/text/resources/*.java src/com/ibm/util/*.java src/com/ibm/util/resources/*.java 

REM textlayout packages 
javac -d classes -classpath classes src/com/ibm/textlayout/*.java src/com/ibm/textlayout/attributes/*.java 

REM UInfo class to be shifted in next release
javac -d classes -classpath classes src/com/ibm/icu/internal/*.java 

REM tool packages
javac -d classes -classpath classes src/com/ibm/tools/compression/*.java src/com/ibm/tools/normalizer/*.java src/com/ibm/tools/rbbi/*.java src/com/ibm/tools/translit/*.java 

REM richtext packages 
javac -d classes -classpath classes src/com/ibm/richtext/awtui/*.java src/com/ibm/richtext/print/*.java src/com/ibm/richtext/styledtext/*.java src/com/ibm/richtext/swingui/*.java src/com/ibm/richtext/textformat/*.java src/com/ibm/richtext/textpanel/*.java src/com/ibm/richtext/uiimpl/*.java src/com/ibm/richtext/uiimpl/resources/*.java 
javac -d classes -classpath classes src/com/ibm/richtext/textapps/*.java src/com/ibm/richtext/textapps/resources/*.java src/com/ibm/richtext/demo/*.java src/com/ibm/richtext/swingdemo/*.java src/com/ibm/richtext/tests/*.java  
javac -d classes -classpath classes src/com/ibm/demo/*.java src/com/ibm/demo/calendar/*.java src/com/ibm/demo/holiday/*.java src/com/ibm/demo/rbbi/*.java src/com/ibm/demo/rbnf/*.java src/com/ibm/demo/translit/*.java src/com/ibm/icu/demo/components/*.java 

REM test packages 
javac -d classes -classpath classes src/com/ibm/test/TestFmwk.java src/com/ibm/test/TestLog.java src/com/ibm/test/bigdec/*.java src/com/ibm/test/bnf/*.java src/com/ibm/test/calendar/*.java src/com/ibm/test/compression/*.java 
javac -d classes -classpath classes src/com/ibm/test/normalizer/*.java src/com/ibm/test/rbbi/*.java src/com/ibm/test/rbnf/*.java src/com/ibm/test/richtext/*.java src/com/ibm/test/search/*.java src/com/ibm/test/timezone/*.java src/com/ibm/test/translit/*.java
javac -d classes -classpath classes src/com/ibm/icu/test/format/*.java src/com/ibm/icu/test/text/*.java 
javac -d classes -classpath classes src/com/ibm/test/TestAll.java

mkdir doc
javadoc -d doc -sourcepath src com.ibm.util com.ibm.util.resources com.ibm.text com.ibm.text.resources com.ibm.tools.translit com.ibm.tools.compression com.ibm.tools.normalizer com.ibm.tools.rbbi com.ibm.math com.ibm.textlayout com.ibm.textlayout.attributes com.ibm.richtext.awtui com.ibm.richtext.print com.ibm.richtext.styledtext com.ibm.richtext.swingui com.ibm.richtext.textapps com.ibm.richtext.textformat com.ibm.richtext.textpanel com.ibm.richtext.uiimpl com.ibm.richtext.uiimpl.resources com.ibm.richtext.textapps.resources
