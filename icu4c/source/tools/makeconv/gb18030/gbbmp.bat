rem   Copyright (C) 2000, International Business Machines
rem   Corporation and others.  All Rights Reserved.

rem gbbmp.bat takes gbkuni30.txt and generates a mapping file for all BMP
rem code points, as well as unidirectional files covering the BMP

gbmake4 <gbkuni30.txt >allfour.txt
copy gbkuni30.txt+allfour.txt gbkwith4.txt
sort <gbkwith4.txt >gbk2kbmp.txt

rem output: gbk2kbmp.txt

gbsingle <gbk2kbmp.txt >ucs2togb.txt
gbsingle gb <gbk2kbmp.txt >gbtoucs2.txt

rem output: gbtoucs2.txt, ucs2togb.txt
