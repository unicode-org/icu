add_executable(icuinfo)
icu_add_sources(icuinfo)
target_link_libraries(icuinfo PRIVATE icu_defaults icuuc toolutil icui18n)

add_library(testplug testplug.c)
target_link_libraries(testplug PRIVATE icu_lib_defaults icuuc)
