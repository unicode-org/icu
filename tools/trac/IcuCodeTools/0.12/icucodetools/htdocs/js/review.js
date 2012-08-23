// Copyright (C) 2007-2012 IBM and Others. All Rights Reserved

  var setBranchNames = function() {
     var cst = document.getElementById("changesettable");
     var trs=cst.getElementsByTagName("tr");
     for( i=1 ; i < trs.length ; i++ ) {
	var sec = trs[i].getElementsByTagName('td')[1]; // [0] is 'author', [1] is section.
        var brk = sec.getElementsByTagName('a');
        for(j=0;j<brk.length;j++) {
           var bri = brk[j];
           var str = bri.innerHTML;
           if(str.indexOf("trunk")>-1) {
              bri.className = 'branch-trunk';
           } else if(str.indexOf("branches")>-1) {
              bri.className = 'branch-branches';
           } else if(str.indexOf("tags")>-1) {
	      bri.className = 'branch-tags';
           }
        }
     }
  }

// http://ckon.wordpress.com/2008/07/25/stop-using-windowonload-in-javascript/
if (window.attachEvent) {window.attachEvent('onload', setBranchNames);}
else if (window.addEventListener) {window.addEventListener('load', setBranchNames, false);}
else {document.addEventListener('load', setBranchNames, false);}

