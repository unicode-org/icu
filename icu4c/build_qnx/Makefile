# © 2023 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
INSTALL_TARGETS =

ifeq ($(strip $(CPULIST)),)
  INSTALL_TARGETS += install-aarch64 install-x86_64
else

ifeq ($(filter aarch64,$(CPULIST)), aarch64)
  INSTALL_TARGETS += install-aarch64
endif

ifeq ($(filter x86_64,$(CPULIST)), x86_64)
  INSTALL_TARGETS += install-x86_64
endif
endif

linux_CLEANFILES_LIST := $(filter-out linux-x86_64-o/GNUmakefile, $(wildcard linux-x86_64-o/*))
aarch64_CLEANFILES_LIST := $(filter-out nto-aarch64-le/GNUmakefile, $(wildcard nto-aarch64-le/*))
x86_64_CLEANFILES_LIST := $(filter-out nto-x86_64-o/GNUmakefile, $(wildcard nto-x86_64-o/*))

all: install

install: install-linux $(INSTALL_TARGETS)

hinstall:

clean: clean-linux clean-aarch64 clean-x86_64

clean-linux:
ifneq ($(strip $(linux_CLEANFILES_LIST)),)
	rm -rf $(linux_CLEANFILES_LIST)
endif

clean-aarch64:
ifneq ($(strip $(aarch64_CLEANFILES_LIST)),)
	rm -rf $(aarch64_CLEANFILES_LIST)
endif

clean-x86_64:
ifneq ($(strip $(x86_64_CLEANFILES_LIST)),)
	rm -rf  $(x86_64_CLEANFILES_LIST)
endif

install-linux:
	$(MAKE) -C linux-x86_64-o -f GNUmakefile

install-aarch64: install-linux
	$(MAKE) -C nto-aarch64-le -f GNUmakefile install

install-x86_64: install-linux
	$(MAKE) -C nto-x86_64-o -f GNUmakefile install


