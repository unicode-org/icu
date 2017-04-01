DASM=/usr/local/bin/dasm

ALL=aintltst
INCS=ustring.asm

-include Makefile.local

all: $(ALL:%=%.out)

%.out: %.asm $(INCS)
	$(DASM) $< -f3 -o$@ -l$*.lst -s$*.sym -Iinc
	od -v -t x1 < $*.out  | cut -c12- > $*.hex

