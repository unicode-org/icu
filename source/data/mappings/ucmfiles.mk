# A list of UCM's to build
# Note: 
#
#   If you are thinking of modifying this file, READ THIS. 
#
# Instead of changing this file [unless you want to check it back in],
# you should consider creating a 'ucmlocal.mk' file in this same directory.
# Then, you can have your local changes remain even if you upgrade or re
# configure the ICU.
#
# Example 'ucmlocal.mk' files:
#
#  * To add an additional converter to the list: 
#    _____________________________________________________
#    |  UCM_SOURCE_LOCAL =  myconverter.ucm ...
#
#  * To REPLACE the default list and only build with a few
#     converters:
#    _____________________________________________________
#    |  UCM_SOURCE = ibm-913.ucm ibm-949.ucm ibm-37.ucm
#
# If you are planning to exclude EBCDIC mappings in you data then please delete
# ucmebcdic.mk from the <icu>/source/data directory
#

UCM_SOURCE_FILES = ibm-1253.ucm ibm-862.ucm\
ibm-367.ucm\
ibm-1254.ucm ibm-863.ucm\
ibm-1255.ucm ibm-864.ucm\
ibm-1256.ucm ibm-437.ucm ibm-865.ucm\
ibm-1051.ucm ibm-1257.ucm ibm-866.ucm\
ibm-1258.ucm ibm-867.ucm\
ibm-868.ucm\
ibm-869.ucm\
ibm-1275.ucm\
ibm-1276.ucm ibm-5104.ucm ibm-872.ucm\
ibm-1277.ucm ibm-874.ucm\
ibm-1280.ucm ibm-878.ucm\
ibm-1281.ucm ibm-901.ucm\
ibm-1282.ucm ibm-902.ucm\
ibm-1283.ucm\
ibm-9044.ucm\
ibm-9049.ucm\
ibm-1363_P110-2000.ucm\
ibm-9061.ucm\
ibm-808.ucm\
ibm-813.ucm\
ibm-921.ucm\
ibm-922.ucm\
ibm-848.ucm\
ibm-9238.ucm\
ibm-17248.ucm ibm-849.ucm\
ibm-850.ucm\
ibm-851.ucm\
ibm-852.ucm\
ibm-855.ucm ibm-942_P120-2000.ucm\
ibm-942_P12A-2000.ucm\
ibm-856.ucm\
ibm-943_P130-2000.ucm\
ibm-857.ucm\
ibm-858.ucm\
ibm-949_P110-2000.ucm\
ibm-1250.ucm ibm-859.ucm ibm-950.ucm\
ibm-1251.ucm ibm-860.ucm\
ibm-1162.ucm\
jisx-201.ucm jisx-208.ucm jisx-212.ucm gb_2312_80-1.ucm\
ksc_5601_1.ucm\
iso-ir-165.ucm cns-11643-1992.ucm ibm-1252.ucm ibm-861.ucm\
lmb-excp.ucm ibm-964.ucm icu-internal-25546.ucm\
ibm-1006_P100-2000.ucm ibm-1006_X100-2000.ucm\
ibm-1098_P100-2000.ucm ibm-1098_X100-2000.ucm\
ibm-1124_P100-2000.ucm ibm-1125_P100-2000.ucm\
ibm-1129_P100-2000.ucm ibm-1131_P100-2000.ucm\
ibm-1133_P100-2000.ucm\
ibm-1381_P110-2000.ucm ibm-806_P100-2000.ucm\
ibm-9066_P100-2000.ucm\
ibm-33722_P120-2000.ucm

