add_executable(gennorm2)
icu_add_sources(gennorm2)
target_link_libraries(gennorm2 PRIVATE icu_defaults icuuc toolutil)