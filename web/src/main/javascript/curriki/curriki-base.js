// vim: ts=4:sw=4
/*global Ext */
/*global _ */

Ext.BLANK_IMAGE_URL = '/xwiki/skins/curriki8/extjs/resources/images/default/s.gif';

Ext.Ajax.defaultHeaders = {
	 'Accept': 'application/json'
	,'Content-Type': 'application/json; charset=utf-8'
};
Ext.Ajax.disableCaching=false;
Ext.Ajax.timeout=120000;


if (!('console' in window) || !('firebug' in console)){
	var names = ["log", "debug", "info", "warn", "error", "assert", "dir",
	             "dirxml", "group", "groupEnd", "time", "timeEnd", "count",
	             "trace", "profile", "profileEnd"];
	window.console = {};
	for (var i = 0; i < names.length; ++i)
		window.console[names[i]] = Ext.emptyFn
}
console.log('initing Curriki');

Ext.onReady(function(){
	Ext.QuickTips.init();
	Ext.apply(Ext.QuickTips.getQuickTip(), {
		dismissDelay:10000
		,hideDelay: 0
	});
});

/*
 * Example of dynamically loading javascript
function initLoader() {
  var script = document.createElement("script");
  script.src = "http://www.google.com/jsapi?key=ABCDEFG&callback=loadMaps";
  script.type = "text/javascript";
  document.getElementsByTagName("head")[0].appendChild(script);
}
*/

Ext.ns('Curriki');
Ext.ns('Curriki.module');

Ext.onReady(function(){
	Curriki.loadingCount = 0;
	Curriki.loadingMask = new Ext.LoadMask(Ext.getBody(), {msg:_('loading.loading_msg')});

    Ext.Ajax.on('beforerequest', function(conn, options){
console.log('beforerequest', conn, options);
		Curriki.showLoading(options.waitMsg);
	});
    Ext.Ajax.on('requestcomplete', function(conn, response, options){
console.log('requestcomplete', conn, response, options);
		Curriki.hideLoading();
	});
    Ext.Ajax.on('requestexception', function(conn, response, options){
console.log('requestexception', conn, response, options);
		Curriki.hideLoading(true);
	});
});


Curriki.id = function(prefix){
	return Ext.id('', prefix+':');
};

Curriki.showLoading = function(msg, multi){
	if (multi === true) {
		Curriki.loadingCount++;
	}
	if (!Ext.isEmpty(Curriki.loadingMask)){
		msg = msg||'loading.loading_msg';
		Curriki.loadingMask.msg = _(msg);
		Curriki.loadingMask.enable();
		Curriki.loadingMask.show();
	}
}

Curriki.hideLoading = function(multi){
	if (multi === true) {
		Curriki.loadingCount--;
	}
	if (Curriki.loadingCount == 0 && !Ext.isEmpty(Curriki.loadingMask)){
		Curriki.loadingMask.hide();
		Curriki.loadingMask.disable();
	} else if (Curriki.loadingCount < 0) {
		Curriki.loadingCount = 0;
	}
}

Curriki.logView = function(page){
	// Usage in site example:
	// <a onClick="javascript:Curriki.logView('/Download/attachment/${space}/${name}/${attach.filename}');"> .. </a>
	if (window.pageTracker) {
		pageTracker._trackPageview(page);
	} else {
		console.info('Would track: ', page);
	}
}

Curriki.validEmail = function(emailStr){
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
};

Curriki.start = function(callback){
console.log('Start Callback: ', callback);
	var args = {};

	if ("object" === typeof callback){
		if (callback.args){
			args = callback.args;
		}
		if (callback.callback){
			callback = callback.callback;
		} else if (callback.module){
			callback = callback.module;
		}
	}

	if ("string" === typeof callback){
		var module = eval('(Curriki.module.'+callback.toLowerCase()+')');

		if (module && "function" === typeof module.init){
			// callback is the name of a module
			module.init(args);
			if ("function" === typeof module.start) {
				callback = module.start;
			} else {
				callback = Ext.emptyFn;
			}
		} else {
			// callback is a known string
			switch(callback){
				default:
					callback = Ext.emptyFn;
					break;
			}
		}
	}

	if ("function" === typeof callback) {
		callback(args);
	}
};

Curriki.init = function(callback){
console.log('Curriki.init: ', callback);
	if (Ext.isEmpty(Curriki.initialized)) {
		Curriki.data.user.GetUserinfo(function(){Curriki.start(callback);});
		Curriki.initialized = true;
	} else {
		Curriki.start(callback);
	}
};
