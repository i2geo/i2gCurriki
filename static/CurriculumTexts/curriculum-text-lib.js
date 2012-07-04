function tryChooseNode(fragId) {
    if(typeof(fragId)=="undefined" || fragId==null || fragId=="" || typeof(fragId)!="string")
        return true;
    if(!fragId.match("^\\#.*")) fragId="#"+fragId;
    var op = window.i2geoWindow;
    if(typeof(op)=="undefined" && typeof(window.opener)=="object" && window.opener!=null)
        op = window.opener;
    if(typeof(op)=="object" && op!=null && 
            typeof(op.choose)=="function") {
        op.choose(fragId);
    } else {
        window.i2geoWindow = window.open("/xwiki/bin/view/Search/Simple?terms="+
                encodeURIComponent(fragId));
    }
    return false;
}