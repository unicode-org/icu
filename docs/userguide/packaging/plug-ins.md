---
layout: default
title: Plug-ins
nav_order: 4
parent: ICU Data
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Plug-ins
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

This page documents the ICU4C DLL Plug-in capability.
This feature is a Technology Preview which first appeared in ICU4C version
4.3.4. It may be altered or removed in subsequent releases, and feedback is
appreciated.

## Off by default

As per ticket [ICU-11763](https://unicode-org.atlassian.net/browse/ICU-11763), the plugin
mechanism discussed here is disabled by default as of ICU 56. Use
**--enable-plugins** and/or define **UCONFIG_ENABLE_PLUGINS=1** to enable the
mechanism.

## Background

ICU4C has functionality for registering services, setting
mutex/allocation handlers, etc. But they must be installed 'before any
ICU services are used'
The ICU plugin mechanism allows small code modules, called plugins, to be loaded
automatically when ICU starts.

## How it works

At u_init time, ICU will read from a list of DLLs and entrypoints, and
attempt to load plugins found in the list. plugins are called and can
perform any ICU related function, such as registering or unregistering
service objects. At u_cleanup time, plugins have the opportunity to
uninstall themselves before they are removed from memory and unloaded.

## Plugin API

The current plugin API is documented as
[icuplug.h](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/icuplug_8h.html)
Some sample plugins are available at:
[testplug.c](https://github.com/unicode-org/icu/blob/master/icu4c/source/tools/icuinfo/testplug.c)
Here is a simple, trivial plugin:

```c
U_CAPI
UPlugTokenReturn U_EXPORT2 myPlugin (UPlugData *data, UPlugReason reason, UErrorCode *status) {
    if(reason==UPLUG_REASON_QUERY) {
        uplug_setPlugName(data, "Simple Plugin"); /* optional */
        uplug_setPlugLevel(data, UPLUG_LEVEL_HIGH); /* Mandatory */
    } else if(reason==UPLUG_REASON_LOAD) {
        /* ... load ... */
        /* Set up some ICU things here. */
    } else if(reason==UPLUG_REASON_UNLOAD) {
        /* ... unload ... */
    }
    return UPLUG_TOKEN; /* Mandatory. */
}
```

The `UPlugData*` is an opaque pointer to the plugin-specific data,
and is used in all other API calls.

The API contract is:

1. the plugin MUST always return UPLUG_TOKEN as a return value- to
indicate that it is a valid plugin.

2. when the 'reason' parameter is set to UPLUG_REASON_QUERY, the
plugin MUST call `uplug_setPlugLevel()` to indicate whether it is a high
level or low level plugin.

3. when the 'reason' parameter is UPLUG_REASON_QUERY, the plugin
SHOULD call `uplug_setPlugName` to indicate a human readable plugin name.

## Configuration

You can see a sample configuration file here:
[icuplugins_windows_sample.txt](https://github.com/unicode-org/icu/blob/master/icu4c/source/tools/icuinfo/icuplugins_windows_sample.txt)
At ICU startup time, the environment variable "ICU_PLUGINS" will be
queried for a directory name. If it is not set, the #define
`DEFAULT_ICU_PLUGINS` will be checked for a default value.
`DEFAULT_ICU_PLUGINS` will be set, on autoconf'ed and installed ICU
versions, to "$(prefix)/lib/icu" if not set otherwise by the build
environment.
Within the above-named directory, the file "icuplugins##.txt" will be
opened, if present, where _##_ is the major+minor number of the currently
running ICU (such as, 44 for ICU 4.4).
So, for example, by default, ICU 4.4 would attempt to open
`$(prefix)/lib/icu/icuplugins44.txt`
The configuration file has the following format:
1. Hash (#) begins a comment line
2. Non-comment lines have two or three components:
   > `LIBRARYNAME ENTRYPOINT [ CONFIGURATION .. ]`
3. Tabs or spaces separate the three items.
4. _LIBRARYNAME_ is the name of a shared library, either a short name if
it is on the loader path, or a full pathname.
5. _ENTRYPOINT_ is the short (undecorated) symbol name of the plugin's
entrypoint, as above.
6. _CONFIGURATION_ is the entire rest of the line. It's passed as-is to
the plugin.

An example configuration file is, in its entirety:

```
# this is icuplugins44.txt
testplug.dll myPlugin hello=world
```

The DLL testplug.dll is opened, and searched for the entrypoint
"myPlugin", which must meet the API contract above.
The string "hello=world" is passed to the plugin verbatim.

## Load Order

Plugins are categorized as "high" or "low" level. Low level are those
which must be run BEFORE high level plugins, and before any operations
which cause ICU to be 'initialized'. If a plugin is low level but
causes ICU to allocate memory or become initialized, that plugin is said
to cause a 'level change'.
At load time, ICU first queries all plugins to determine their level,
then loads all 'low' plugins first, and then loads all 'high' plugins.
Plugins are otherwise loaded in the order listed in the configuration file.

## User interface and troubleshooting

The new command line utility, `icuinfo`, will not only print out ICU
version information, but will also give information on the load status
of plugins, with the "-L" option. It will list all loaded or
possibly-loaded plugins, give their level, and list any errors
encountered which prevented them from loading. Thus, the end user can
validate their plugin configuration file to determine if plugins are
missing, unloadable, or loaded in the wrong order.
For example the following run shows that the plugin named
"myPluginFailQuery" did not call `uplug_setPlugLevel()` and thus failed to
load.

```
$ icuinfo -v -L
Compiled against ICU 4.3.4, currently running ICU 4.3.4
ICUDATA is icudt43l
plugin file is: /lib/plugins/icuplugins43.txt
Plugins:
# Level Name
Library:Symbol 
config| (configuration string)
>>> Error | Explanation
-----------------------------------

#1 HIGH Just a Test High-Level Plugin
plugin| /lib/plugins/libplugin.dylib:myPlugin 
config| x=4

#2 HIGH High Plugin
plugin| /lib/plugins/libplugin.dylib:myPluginHigh
config| x=4

#3 INVALID this plugin did not call uplug_setPlugName()
plugin| /lib/plugins/libplugin.dylib:myPluginFailQuery
config| uery
\\\ status| U_PLUGIN_DIDNT_SET_LEVEL
/// Error: This plugin did not call uplug_setPlugLevel during QUERY.

#4 LOW Low Plugin
plugin| /lib/plugins/libplugin.dylib:myPluginLow
config| x=4
Default locale is en_US
Default converter is UTF-8.
```
