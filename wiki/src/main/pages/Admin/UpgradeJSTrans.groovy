//<%
println "1. refresh JStrans"
if(xwiki.user.hasAdminRights()) {
  println "Starting process."
  println ()
  int status = ("/export/home/intergeo/platform/tomcat/webapps/static/JStrans/recreate.sh".execute().waitFor())
  println "Process ran. Result: " + status;
  println ();
  if(status!=0) println "Please join the chatroom (under report a bug) to discuss how to fix."
} else {
  println "please be admin."
}

//%>