TARGETS = en.res root.res sr.res
GENRB = $(MAKEDIR)\..\..\tools\genrb\debug\genrb.exe
GENRBOPT = -s. -d.

all : $(TARGETS)
    @echo All targets are up to date

clean : 
    -erase $(TARGETS)


en.res : en.txt
    $(GENRB) $(GENRBOPT) $?

root.res : root.txt
    $(GENRB) $(GENRBOPT) $?

sr.res : sr.txt
    $(GENRB) $(GENRBOPT) --encoding cp1251 $?

