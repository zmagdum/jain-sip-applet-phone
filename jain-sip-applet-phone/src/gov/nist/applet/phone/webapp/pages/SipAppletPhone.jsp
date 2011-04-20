<HTML>
<HEAD>
<TITLE>Jain Sip Applet Phone</TITLE>
</HEAD>
<table border="0" cellspacing="0" cellpadding="0" width="100%">
  <tr> 
    <td width="554" ><img src="nisthome_banner.jpg" ></td>
    <td background="nisthome-bg.jpg"> &nbsp;</td>
  </tr>  
  <tr> 
    <td width="554" > <img src="main-image.jpg" ></td>
    <td background="main-bg.jpg"> &nbsp;</td>
  </tr>
  <tr> 
    <td width="554" > <img src="bottom-bar.jpg" border="0" ></td> 
    <td background="bottom-bg.jpg"> &nbsp;</td>
  </tr>
</table>
<body bgcolor="#333333" text="#FFFFFF" leftmargin="0" topmargin="0" marginwidth="0" marginheight="0"
	link="white" alink="white" vlink="red"
  >
<center>
<p>This application requires JMF. If you don't have it installed, please install it from this URL :<br>
<a href="http://java.sun.com/products/java-media/jmf/2.1.1/download.html">
http://java.sun.com/products/java-media/jmf/2.1.1/download.html</a><br>
<p>If you're experiencing some problems viewing the applet,check your java plug-in control panel :<br>
- In the Proxies tab, uncheck the "Use browser Settings" box <br>
- In the Cache tab, uncheck the "Enable Caching" box<br>
<br><br>
<APPLET NAME="SipAppletPhone"        
        CODE="gov/nist/applet/phone/ua/gui/NISTMessengerApplet.class"
        ARCHIVE="applet-phone.jar"
	WIDTH=320 HEIGHT=520 MAYSCRIPT="true">
        <param name="PROXYADDRESS" value="<%=request.getLocalAddr()%>">
        <param name="PROXYPORT" value="4000">
        <param name="SIGNALINGTRANSPORT" value="tcp">
	<param name="MYADDRESS" value="<%=request.getRemoteAddr()%>">
	<param name="SERVERADDR" value="<%=request.getLocalAddr()%>">
	<param name="SERVERPORT" value="<%=request.getLocalPort()%>">
        <param name="MEDIATRANSPORT" value="<%=request.getParameter("mtransport")%>">    
        <param name="USERURI" value="<%=request.getParameter("uri")%>">        
        Your browser is completely ignoring the &lt;APPLET&gt; tag!       
</APPLET>
</center>
</BODY>
</HTML>
