add_executable(genccode)
icu_add_sources(genccode)
target_link_libraries(genccode PRIVATE icu_defaults icuuc toolutil)