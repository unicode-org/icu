fs = import('fs')
sources = fs.read('sources.txt').split()

icupkg = executable(
  'icupkg',
  sources,
  dependencies: toolutil_dep,
  install: true,
)
