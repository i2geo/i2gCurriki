function hideForm(form){
  form.getElementsByTagName("fieldset").item(0).className = "collapsed";
}

function toggleForm(form){
  var fieldset = form.getElementsByTagName("fieldset").item(0);
  if(fieldset.className == "collapsed"){
    fieldset.className = "expanded";
  }
  else{
    fieldset.className = "collapsed";
  }
}

function togglePanelVisibility(element){
  if(element.className.indexOf("expanded") >= 0){
    element.className = element.className.replace('expanded', 'collapsed');
  }
  else{
    element.className = element.className.replace('collapsed', 'expanded');
  }
}

function showsubmenu(element){
  if(element.lastChild.tagName.toLowerCase() == "span"){
    if(window.hidetimer){
      if(window.hideelement == element.lastChild){
        clearTimeout(window.hidetimer);
        window.hidetimer = null;
        window.hideelement = null;
      }
      else{
        doHide();
      }
    }
    var coords = Position.positionedOffset(element);
    element.lastChild.style.left = (coords[0]  - 10) + "px";
    element.lastChild.style.top = (coords[1] + element.offsetHeight) + "px";
    element.lastChild.className = element.lastChild.className.replace("hidden", "visible");
  }
}

function hidesubmenu(element){
  if(element.lastChild.tagName.toLowerCase() == "span"){
    window.hideelement = element.lastChild;
    window.hidetimer = setTimeout(doHide, 100);
  }
}

function setDirty(dirty){
  if(dirty === undefined){
    dirty = true;
  }
  window.docdirty = dirty;
}

function doHide(){
  window.hideelement.className = window.hideelement.className.replace("visible", "hidden");
  clearTimeout(window.hidetimer);
  window.hidetimer = null;
  window.hideelement = null;
}

function updateAttachName(form, msg) {
  var fname = form.filepath.value;

  if (fname == "") {
    return false;
  }

  var i = fname.lastIndexOf('\\');
  if (i == -1){
    i = fname.lastIndexOf('/');
  }

  fname = fname.substring(i + 1);
  if (form.filename.value == fname){
    return true;
  }

  if (form.filename.value == ""){
    form.filename.value = fname;
  }
  else {
    if (confirm(msg + " '" + fname + "' ?")){
      form.filename.value = fname;
    }
  }
  return true;
}

function toggleClass(o, className){
  if(!eltHasClass(o,className)) {
    o.className += ' ' + className
  }
  else {
    rmClass(o, className);
  }
}

function addClass(o, className){
  if(!eltHasClass(o,className))
    o.className += ' ' + className
}

function eltHasClass(o,className){
  if(!o.className)
    return false;
  return new RegExp('\\b' + className + '\\b').test(o.className)
}

function rmClass(o, className){
  o.className = o.className.replace(new RegExp('\\s*\\b' + className + '\\b'),'')
}

function openURL(url) {
  win = open( url, "win", "titlebar=0,width=750,height=480,resizable,scrollbars");
  if( win ) {
    win.focus();
  }
}

function openHelp() {
  win = open( "http://platform.xwiki.org/xwiki/bin/view/Main/XWikiSyntax?xpage=print", "XWikiSyntax", "titlebar=0,width=750,height=480,resizable,scrollbars");
  if( win ) {
    win.focus();
  }
}

var XWiki = {
  Version: '0.8_pre1',
  require: function(libraryName) {
    // inserting via DOM fails in Safari 2.0, so brute force approach
    document.write('<script type="text/javascript" src="'+libraryName+'"></script>');
  },
  addLibrary: function(scriptLibraryName) {
    JSfileName = 'xwiki.js'; // This should be added in a xwiki.js file 'xwiki.js'
    if(scriptLibraryName=='scriptaculous') {
	libraries = ['prototype.js', 'util.js', 'effects.js', 'dragdrop.js', 'controls.js'];
    }
    if(scriptLibraryName=='rico') {
	libraries = ['rico.js'];
    }
    var scriptTags = document.getElementsByTagName("script");
    for(var i=0;i<scriptTags.length;i++) {
      if(scriptTags[i].src && scriptTags[i].src.match(JSfileName)) {
        var path = scriptTags[i].src.replace(JSfileName,scriptLibraryName) + '/';
	  for (var j=0;j<libraries.length;j++) {
	    this.require (path + libraries[j]);
	  }
      }
    }
  }
};

var XWikiAjax = {
  requests: new Array(),
  start: function(status) {
    this.status = $(status);
    ajaxEngine.registerRequest ('setValue', 'SetValueResponse?xpage=rdf');
    ajaxEngine.registerAjaxElement (this.status.id);
  },
  addRequest: function(dName, cName, field, value) {
    var request = Object.extend({
      type: 'set'
    });
    request.className = cName;
    request.document = dName;
    request.field = field;
    request.value = value;
    this.requests.push(request);
  },
  end: function() {
    this.requests.reverse();
    var req = this.requests.pop();
    while (req) {
      if (req.type=='set') {
        ajaxEngine.sendRequest ('setValue', 'status='+this.status.id, 'doc='+req.document, 
	    'typedoc='+req.className, 'field='+req.field, 'value='+req.value);
      };
	req = this.requests.pop();
    }
    this.status.innerHtml = 'updated';
  }
};

function updateName(field1, field2, removeclass) {
  var name = field1.value;
  name = noaccent(name);
  if (removeclass!=false) {
    name = name.replace(/class$/gi,"");
  }
  if (field2 == null) {
    field1.value = name;
  } else {
    field2.value = name;
  }
  if (name=="") {
    return false;
  }
  return true;
}

function noaccent(txt) {
  temp = txt.replace(/[\u00c0\u00c1\u00c2\u00c3\u00c4\u00c5\u0100\u0102\u0104\u01cd\u01de\u01e0\u01fa\u0200\u0202\u0226]/g,"A");
  temp = temp.replace(/[\u00e0\u00e1\u00e2\u00e3\u00e4\u00e5\u0101\u0103\u0105\u01ce\u01df\u01e1\u01fb\u0201\u0203\u0227]/g,"a");
  temp = temp.replace(/[\u00c6\u01e2\u01fc]/g,"AE");
  temp = temp.replace(/[\u00e6\u01e3\u01fd]/g,"ae");
  temp = temp.replace(/[\u008c\u0152]/g,"OE");
  temp = temp.replace(/[\u009c\u0153]/g,"oe");
  temp = temp.replace(/[\u00c7\u0106\u0108\u010a\u010c]/g,"C");
  temp = temp.replace(/[\u00e7\u0107\u0109\u010b\u010d]/g,"c");
  temp = temp.replace(/[\u00d0\u010e\u0110]/g,"D");
  temp = temp.replace(/[\u00f0\u010f\u0111]/g,"d");
  temp = temp.replace(/[\u00c8\u00c9\u00ca\u00cb\u0112\u0114\u0116\u0118\u011a\u0204\u0206\u0228]/g,"E");
  temp = temp.replace(/[\u00e8\u00e9\u00ea\u00eb\u0113\u0115\u0117\u0119\u011b\u01dd\u0205\u0207\u0229]/g,"e");
  temp = temp.replace(/[\u011c\u011e\u0120\u0122\u01e4\u01e6\u01f4]/g,"G");
  temp = temp.replace(/[\u011d\u011f\u0121\u0123\u01e5\u01e7\u01f5]/g,"g");
  temp = temp.replace(/[\u0124\u0126\u021e]/g,"H");
  temp = temp.replace(/[\u0125\u0127\u021f]/g,"h");
  temp = temp.replace(/[\u00cc\u00cd\u00ce\u00cf\u0128\u012a\u012c\u012e\u0130\u01cf\u0208\u020a]/g,"I");
  temp = temp.replace(/[\u00ec\u00ed\u00ee\u00ef\u0129\u012b\u012d\u012f\u0131\u01d0\u0209\u020b]/g,"i");
  temp = temp.replace(/[\u0132]/g,"IJ");
  temp = temp.replace(/[\u0133]/g,"ij");
  temp = temp.replace(/[\u0134]/g,"J");
  temp = temp.replace(/[\u0135]/g,"j");
  temp = temp.replace(/[\u0136\u01e8]/g,"K");
  temp = temp.replace(/[\u0137\u0138\u01e9]/g,"k");
  temp = temp.replace(/[\u0139\u013b\u013d\u013f\u0141]/g,"L");
  temp = temp.replace(/[\u013a\u013c\u013e\u0140\u0142\u0234]/g,"l");
  temp = temp.replace(/[\u00d1\u0143\u0145\u0147\u014a\u01f8]/g,"N");
  temp = temp.replace(/[\u00f1\u0144\u0146\u0148\u0149\u014b\u01f9\u0235]/g,"n");
  temp = temp.replace(/[\u00d2\u00d3\u00d4\u00d5\u00d6\u00d8\u014c\u014e\u0150\u01d1\u01ea\u01ec\u01fe\u020c\u020e\u022a\u022c\u022e\u0230]/g,"O");
  temp = temp.replace(/[\u00f2\u00f3\u00f4\u00f5\u00f6\u00f8\u014d\u014f\u0151\u01d2\u01eb\u01ed\u01ff\u020d\u020f\u022b\u022d\u022f\u0231]/g,"o");
  temp = temp.replace(/[\u0156\u0158\u0210\u0212]/g,"R");
  temp = temp.replace(/[\u0157\u0159\u0211\u0213]/g,"r");
  temp = temp.replace(/[\u015a\u015c\u015e\u0160\u0218]/g,"S");
  temp = temp.replace(/[\u015b\u015d\u015f\u0161\u0219]/g,"s");
  temp = temp.replace(/[\u00de\u0162\u0164\u0166\u021a]/g,"T");
  temp = temp.replace(/[\u00fe\u0163\u0165\u0167\u021b\u0236]/g,"t");
  temp = temp.replace(/[\u00d9\u00da\u00db\u00dc\u0168\u016a\u016c\u016e\u0170\u0172\u01d3\u01d5\u01d7\u01d9\u01db\u0214\u0216]/g,"U");
  temp = temp.replace(/[\u00f9\u00fa\u00fb\u00fc\u0169\u016b\u016d\u016f\u0171\u0173\u01d4\u01d6\u01d8\u01da\u01dc\u0215\u0217]/g,"u");
  temp = temp.replace(/[\u0174]/g,"W");
  temp = temp.replace(/[\u0175]/g,"w");
  temp = temp.replace(/[\u00dd\u0176\u0178\u0232]/g,"Y");
  temp = temp.replace(/[\u00fd\u00ff\u0177\u0233]/g,"y");
  temp = temp.replace(/[\u0179\u017b\u017d]/g,"Z");
  temp = temp.replace(/[\u017a\u017c\u017e]/g,"z");
  temp = temp.replace(/[\u00df]/g,"SS");
  temp = temp.replace(/[^a-zA-Z0-9_]/g,"");
  return temp;
}

function prepareName(form) {
  var fname = form.register_first_name.value;
  var lname = form.register_last_name.value;
  var cxwikiname = form.xwikiname;
  if (fname != "") {
    fname = fname.substring(0,1).toUpperCase() + fname.substring(1);
    fname.replace(/ /g,"");
  }
  if (lname != "") {
    lname = lname.substring(0,1).toUpperCase() + lname.substring(1)
    lname.replace(/ /g,"");
  }
  if (cxwikiname.value == "") {
    cxwikiname.value = noaccent(fname + lname);
  }
}

function createCookie(name,value,days) {
  if (days) {
    var date = new Date();
    date.setTime(date.getTime()+(days*24*60*60*1000));
    var expires = "; expires="+date.toGMTString();
  }
  else var expires = "";
  document.cookie = name+"="+value+expires+"; path=/";
}

function readCookie(name) {
  var nameEQ = name + "=";
  var ca = document.cookie.split(';');
  for(var i=0;i < ca.length;i++) {
    var c = ca[i];
    while (c.charAt(0)==' ') {
      c = c.substring(1,c.length);
    }
    if (c.indexOf(nameEQ) == 0) {
      return c.substring(nameEQ.length,c.length);
    }
  }
  return null;
}

function eraseCookie(name) {
  createCookie(name,"",-1);
}

/* Open links marked with rel="external" in an external window and sets the target attribute to any
   rel attribute starting with "_". Note that We need to do this in Javascript
   as opposed to using target="_blank" since the target attribute is not valid XHTML.
 */
function externalLinks() {
  if (!document.getElementsByTagName) return;
  var anchors = document.getElementsByTagName("a");
  for (var i=0; i<anchors.length; i++) {
    var anchor = anchors[i];
    if (anchor.getAttribute("href") && anchor.getAttribute("rel")) { 
      // Since the rel attribute can have other values we need to only take into account the ones
      // starting with "_"
      var values = anchor.getAttribute("rel").split(" ");
      for(var j = 0; j<values.length; j++) {
        if (values[j].charAt(0) == "_") {
          anchor.target = values[j].substring(1);
          break;
        } else if (values[j] == "external") {
          anchor.target = "_blank";
          break;
        }
      }
    }
  }
}

function createAccordion(params) {
    var acc = new accordion(params.div, { 
      resizeSpeed:10,
      classNames: {
      toggle: "accordionTabTitleBar",
      content: "accordionTabContentBox"
      },
      defaultSize: {
        width: ('width' in params ? params.width : null),
        height: ('height' in params ? params.height : null)
      }
    });
    acc.activate($$('#'+params.div+' .accordionTabTitleBar')[params.no]);
}

var CurrikiJS = {

	jt_ShowNoneElm: function(elm, showIt, showStyle) {
		if (elm) {elm.style.display = showIt ? (showStyle ? showStyle : 'block') : "none";}
	},

	jt_valPx: function(pixels) {
		return isNaN(pixels) ? 0 : pixels + "px";
	},

	jt_moveTo: function(obj, x, y) {
		obj.style.left = CurrikiJS.jt_valPx(x);
		obj.style.top = CurrikiJS.jt_valPx(y);
	},

	jt_winW: function() {
		if (document.documentElement && (document.documentElement.clientWidth > 0)) {return document.documentElement.clientWidth;}
		else if (window.innerWidth) {return window.innerWidth;}
		else {return document.body.clientWidth;}
	},

	jt_winH: function() {
		if (window.innerHeight) {return window.innerHeight;}
		else if (document.documentElement && (document.documentElement.clientHeight > 0)) {return document.documentElement.clientHeight;}
		else {return document.body.clientHeight;}
	},

	jt_scrollLeft: function() {
		if (window.pageXOffset) {return window.pageXOffset;}
		else if (document.documentElement && (document.documentElement.scrollLeft > 0)) {return document.documentElement.scrollLeft;}
		else {return document.body.scrollLeft;}
	},

	jt_scrollTop: function() {
		if (window.pageYOffset) {return window.pageYOffset;}
		else if (document.documentElement && (document.documentElement.scrollTop > 0)) {return document.documentElement.scrollTop;}
		else {return document.body.scrollTop;}
	},

	jt_AddListener: function(obj, evType, fn) {
		if (obj.addEventListener) {obj.addEventListener(evType, fn, false);}
		else if (obj.attachEvent) {obj.attachEvent('on' + evType, fn);}
	},

	jt_RemListener: function(obj, evType, fn) {
		if (obj.removeEventListener) {obj.removeEventListener(evType, fn, false);}
		else if (obj.detachEvent) {obj.detachEvent('on' + evType, fn);}
	},

	jt_fixE: function(ev) {
		var e = ev ? ev : window.event;
		return e;
	},

	msgOn: function(msg, numSecs) {
		if (!CurrikiJS.msgDIV) {
			CurrikiJS.msgDIV = document.createElement('div');
			CurrikiJS.msgDIV.className = "Curriki_ScrnMsg";
			CurrikiJS.msgDIV.style.display = 'none';
			CurrikiJS.msgDIV.style.position = Ext.isIE6 ? 'absolute' : 'fixed';
			document.body.appendChild(CurrikiJS.msgDIV);
		}
		CurrikiJS.msgDIV.innerHTML = msg;
		var x = Math.round((CurrikiJS.jt_winW() - CurrikiJS.msgDIV.offsetWidth) / 2) + (Ext.isIE6 ? CurrikiJS.jt_scrollLeft() : 0);
		var y = 40 + (Ext.isIE6 ? CurrikiJS.jt_scrollTop() : 0);
		CurrikiJS.jt_moveTo(CurrikiJS.msgDIV, x, y);
		CurrikiJS.jt_ShowNoneElm(CurrikiJS.msgDIV, true);
        clearTimeout(CurrikiJS.msgOnTimer);
	    if (numSecs) {
			CurrikiJS.msgOnTimer = setTimeout(function() {CurrikiJS.jt_ShowNoneElm(CurrikiJS.msgDIV);}, Math.Round(numSecs * 1000));
		}
	},

	msgOff: function() {
		CurrikiJS.jt_ShowNoneElm(CurrikiJS.msgDIV);
		CurrikiJS.msgDIV.innerHTML = '';
	},

	trimFields: function(aForm) {
		for (var i=0; i<aForm.elements.length; i++) {
			if ((aForm.elements[i].type == 'text') || (aForm.elements[i].type == 'textarea')) {
				aForm.elements[i].value = aForm.elements[i].value.trim();
			}
		}
	},

	errFocusOn: false,

	errHighlight: function(elName, elmFocus) {
		$(elName).addClassName('highlight');
		if (elmFocus && !CurrikiJS.errFocusOn) {
			CurrikiJS.errFocusOn = true;
			elmFocus.focus();
			window.scrollTo(0, CurrikiJS.jt_scrollTop() - 100);
		}
	},

	errMsg: '',

	errMsgReset: function() {
		CurrikiJS.errMsg = '';
		CurrikiJS.errFocusOn = false;
	},

	errMsgAdd: function(msg) {
		CurrikiJS.errMsg += " - " + msg + "\n";
	},

	errMsgShow: function() {
		if (CurrikiJS.errMsg) {
			var tmp = CurrikiJS.errMsg;
			CurrikiJS.errMsg = '';
			alert(tmp);
		}
	},

	addThisLink: function() {
		document.writeln('<a href="http://www.addthis.com/bookmark.php" onmouseover="return addthis_open(this, \'\', \'[URL]\', \'[TITLE]\')" onmouseout="addthis_close()" onclick="return addthis_sendto()" title="' + _('rve.title.function.email.icon.rollover') + '" class="addThisLink"><img src="http://s7.addthis.com/static/btn/sm-share-en.gif" width="83" height="16" alt="Bookmark and Share" style="border:0"/></a>');
	},

	validEmail: function(emailStr) {
		// return true if 'emailStr' has valid email address format; from jt_.js, wingo.com
		var emailPat=/^(.+)@(.+)$/
		var specialChars="\\(\\)<>@,;:\\\\\\\"\\.\\[\\]"
		var validChars="\[^\\s" + specialChars + "\]"
		var quotedUser="(\"[^\"]*\")"
		var ipDomainPat=/^\[(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})\]$/
		var atom=validChars + '+'
		var word="(" + atom + "|" + quotedUser + ")"
		var userPat=new RegExp("^" + word + "(\\." + word + ")*$")
		var domainPat=new RegExp("^" + atom + "(\\." + atom +")*$")
		var matchArray=emailStr.match(emailPat)
		if (matchArray==null) return false
		var user=matchArray[1]
		var domain=matchArray[2]
		if (user.match(userPat)==null) return false
		var IPArray=domain.match(ipDomainPat)
		if (IPArray!=null) {
			for (var i=1;i<=4;i++) {
				if (IPArray[i]>255) return false
			}
			return true
		}
		var domainArray=domain.match(domainPat)
		if (domainArray==null) return false
		var atomPat=new RegExp(atom,"g")
		var domArr=domain.match(atomPat)
		var len=domArr.length
		if (domArr[domArr.length-1].length<2 || 
		domArr[domArr.length-1].length>4) return false
		if (len<2) return false
		return true;
	},

	formOk: false

}
