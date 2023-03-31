add_executable(genrb)
icu_add_sources(genrb)
target_link_libraries(genrb PRIVATE icu_defaults icuuc toolutil icui18n)

add_executable(derb derb.cpp)
target_link_libraries(derb PRIVATE icu_defaults icuuc toolutil icuio icui18n)