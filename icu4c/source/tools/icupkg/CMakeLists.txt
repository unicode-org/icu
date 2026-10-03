add_executable(icupkg)
icu_add_sources(icupkg)
target_link_libraries(icupkg PRIVATE icu_defaults icuuc toolutil)