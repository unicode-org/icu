# Copyright (c) 2002 IBM, Inc. and others
# sample code rules for a single-target simple sample

all: $(TARGET)

$(TARGET): $(OBJECTS)

# list of targets that aren't actually created
.PHONY: all clean distclean check report

# clean out files
distclean clean:
	-test -z "$(CLEANFILES)" || rm -f $(CLEANFILES)
	-rm $(OBJECTS) $(TARGET)

# Make check: simply runs the sample, logged to a file
check: $(TARGET)
	$(INVOKE) ./$(TARGET) | tee $(TARGET).out



