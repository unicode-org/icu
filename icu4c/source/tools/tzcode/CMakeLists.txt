add_executable(icuzdump icuzdump.cpp)
target_link_libraries(icuzdump PRIVATE
    icu_defaults icuuc toolutil icui18n icuio)