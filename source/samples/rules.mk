# Copyright (c) 2002 IBM, Inc. and others
# sample code rules for a single-target simple sample

# list of targets that aren't actually created
.PHONY: all clean distclean check report 

all: $(ALL_SUBDIR) $(RESTARGET) $(TARGET)  

$(TARGET): $(OBJECTS)
	$(LINK.cc) $^ $(LOADLIBES) $(LDLIBS) -o $@ $(XTRALIBS)

$(RESTARGET): $(RESFILES)
	$(PKGDATA) --name $(RESNAME) --mode $(RESMODE) $(PKGDATAOPTS) $(RESLIST)

res-install: $(RESTARGET)
	$(PKGDATA) --name $(RESNAME) --mode $(RESMODE) $(PKGDATAOPTS) $(RESLIST) --install $(shell icu-config --libdir)

# clean out files
distclean clean: $(CLEAN_SUBDIR)
	-test -z "$(CLEANFILES)" || rm -f $(CLEANFILES)
	-rm $(OBJECTS) $(TARGET) $(RESTARGET) $(RESFILES)

# Make check: simply runs the sample, logged to a file
check: $(TARGET)
	$(INVOKE) $(CHECK_VARS) ./$(TARGET) $(CHECK_ARGS) | tee $(TARGET).out

## resources
$(RESNAME)_%.res: %.txt
	@echo "generating $@"
	$(GENRB) $(GENRBOPT) $^
