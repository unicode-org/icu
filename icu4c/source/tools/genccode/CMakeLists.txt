add_executable(genccode genccode.c)
target_link_libraries(genccode PRIVATE icu_defaults icuuc toolutil)