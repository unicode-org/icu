//**************************************************************************
// Copyright (C) 1997-2004, International Business Machines Corporation and
// others. All Rights Reserved.
//**************************************************************************

var columnBits = 8;	// 8

var columnCount = Math.pow(2,columnBits);
var columnMask = columnCount-1;
var columnShift = Math.floor(columnBits/4);
var columnGap = repeat('_', columnShift);
var columnPad = 4-columnShift;

var gotLast = true;
var current = 0;
var haveFirst = false;

function top(count) {
  document.writeln("<th></th>");
  for (var i = 0; i < columnCount; ++i) {
    document.writeln("<th>", hex(i,2), "</th>");
  }
}

function writeCell(cellclass, value) {
 if (!gotLast) cellclass = 'd' + cellclass;
 if (value) {
   value = current <= 0xFFFF 
   		? String.fromCharCode(current) 
   		: String.fromCharCode(0xD800 + (current >> 10), 0xDC00 + (current & 0x3FF));
 } else {
   value = '\u00A0'
 }
 if (cellclass!="") cellclass = " class='" + cellclass + "'";
 document.writeln("<td", cellclass, ">", value, "</td>");
 ++current;
}

function writeCells(count,cellclass,value) {
  for (var i = 0; i < count; ++i) {
    if ((current & columnMask) == 0) {
      if (cellclass!='u' || count - i < columnCount) {
   	gotLast = true
      } else {
   	gotLast = false;
        var rem = (count - i) & ~columnMask;
   	current += rem;
        i += rem;
        if (i == count) break;
      }
      newRow();
    }
    writeCell(cellclass,value);
  }
}

function newRow() {
   if (haveFirst) document.write("</tr>");
   else haveFirst = true;
   var hclass = (gotLast) ? "" : " class='d'";
   document.writeln("<tr><th", hclass, ">", hex(current>>(columnShift*4),columnPad), columnGap, "</th>");
}

// Utilities

function hex(value, pad) {
  var result = value.toString(16).toUpperCase();
  while (result.length < pad) result = '0' + result;
  return result;
}

function repeat(str, count) {
 var result = "";
 for (var i = 0; i < count; ++i) result += str;
 return result;
}

// used in the body. single letters to save bytes.
function u(count) { // undefined, private use, or surrogates
	writeCells(count,'u',false);
}
function n(count) { // noncharacter
	writeCells(count,'n',false);
}
function i(count) { // ignorable
	writeCells(count,'i',false);
}
function w(count) { // whitespace
	writeCells(count,'',false);
}
function v(count) { // normal visible graphic
	writeCells(count,'',true);
}
