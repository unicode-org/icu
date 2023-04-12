project(
  'icu',
  'c',
  'cpp',
  version: '72.1',
  meson_version: '>=0.57.0',
  default_options: 'cpp_std=c++17',
)

U_ICU_VERSION = meson.project_version()
PACKAGE_ICU_DESCRIPTION = 'International Components for Unicode'
PACKAGE_ICU_URL = 'http://icu-project.org'

cpp = meson.get_compiler('cpp')
cpp_native = meson.get_compiler('cpp', native: true)

have_elf_h = cpp.has_header('elf.h')

if host_machine.system() == 'windows'
  add_project_arguments('-DU_PLATFORM_USES_ONLY_WIN32_API', language: 'cpp')
  add_project_arguments('-DWIN32', '-DWIN64', '-D_MBCS', language: 'cpp')
  add_project_arguments('-DU_PLATFORM_USES_ONLY_WIN32_API', language: 'c')
  add_project_arguments('-DWIN32', '-DWIN64', '-D_MBCS', language: 'c')
endif

if cpp.get_argument_syntax() == 'msvc'
  add_project_arguments('/utf-8', language: 'cpp')
  add_project_arguments('/utf-8', language: 'c')
endif

# per icudefs.mk.in:
# "U_ATTRIBUTE_DEPRECATED is defined to hide warnings about deprecated API warnings."
add_project_arguments('-DU_ATTRIBUTE_DEPRECATED=', language: 'cpp')
if meson.version().version_compare('>= 0.62')
  dl_dep = dependency('dl', required: false)
  dl_native_dep = dependency('dl', required: false, native: true)
else
  dl_dep = cpp.find_library('dl', required: false)
  dl_native_dep = cpp_native.find_library('dl', required: false, native: true)
endif

windows = import('windows')
pkg = import('pkgconfig')

# Compiler flags the users of this library must use.
usage_args = []

if get_option('default_library') == 'static'
  add_project_arguments('-DU_STATIC_IMPLEMENTATION', language: 'c')
  add_project_arguments('-DU_STATIC_IMPLEMENTATION', language: 'cpp')
  usage_args = ['-DU_STATIC_IMPLEMENTATION']
  if cpp.get_argument_syntax() == 'msvc'
    library_prefix = ''
    library_suffix = ''
  else
    library_prefix = 's'
    if get_option('buildtype') == 'debug'
      library_suffix = 'd'
    else
      library_suffix = ''
    endif
  endif
else
  library_prefix = ''
  library_suffix = ''
endif

subdir('source')
