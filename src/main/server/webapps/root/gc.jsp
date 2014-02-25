<%@page %>
<html>
<body>
Requesting garbage collection...
<%
Runtime rt = Runtime.getRuntime();
long total = rt.totalMemory();
long free = rt.freeMemory();
long diff = (total - free);
out.print("<br />Memory - total: " + total + " free: " + free + " dif: " + diff);
out.flush();
System.gc();
try {
	out.print("<br />Sleep for a couple ticks...");
	Thread.sleep(2000);
} catch(Exception ex) {
}
total = rt.totalMemory();
free = rt.freeMemory();
diff = (total - free);
out.print("<br />Memory - total: " + total + " free: " + free + " dif: " + diff);
%>
</body>
</html>

