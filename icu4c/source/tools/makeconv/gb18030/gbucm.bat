rem Copyright (C) 2000, International Business Machines
rem Corporation and others.  All Rights Reserved.


rem gbucm.bat takes gbkuni30.txt and ranges.txt and generates a .ucm file with
rem all mappings except for the ones listed in ranges.txt

copy gbkuni30.txt+ranges.txt gbku30r.txt
gbmake4 <gbku30r.txt >fournor.txt
copy gbkuni30.txt+fournor.txt gb4nor.txt
sort <gb4nor.txt | gbtoucm >gb18030.ucm

rem output: gb18030.ucm
