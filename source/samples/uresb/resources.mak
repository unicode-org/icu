## Copyright (c) 2001-2003 International Business Machines
## Corporation and others. All Rights Reserved.
TARGETS = uresb_en.res uresb_root.res uresb_sr.res
GENRB = ..\..\..\bin\genrb.exe
GENRBOPT = -s. -d. --package-name uresb  

all : $(TARGETS)
    @echo All targets are up to date

clean : 
    -erase $(TARGETS)


uresb_en.res : en.txt
    $(GENRB) $(GENRBOPT) $?

uresb_root.res : root.txt
    $(GENRB) $(GENRBOPT) $?

uresb_sr.res : sr.txt
    $(GENRB) $(GENRBOPT) --encoding cp1251 $?

