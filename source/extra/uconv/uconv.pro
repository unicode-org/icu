TEMPLATE        = app
ICUHOME         = /home/jonas
INCLUDEPATH     = . $$ICUHOME/include
DEFINES         =
LIBS            = -L$$ICUHOME/lib -licu-uc -licu-i18n
CONFIG          = console warn_on debug
SOURCES         = uconv.cpp 
TARGET          = uconv
