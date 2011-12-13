@rem *****************************************************************************
@rem * Copyright (C) 2000-2004, International Business Machines Corporation and  *
@rem * others. All Rights Reserved.                                              *
@rem *****************************************************************************
mkdir docs\api
@set DOC_TYPE=-private
javadoc -d docs/api -classpath lib/xerces.jar -sourcepath ../../../ -windowTitle "RBManager" -bottom "Copyright IBM 2000-2004" %DOC_TYPE% com.ibm.rbm com.ibm.rbm.gui
@if errorlevel 1 pause