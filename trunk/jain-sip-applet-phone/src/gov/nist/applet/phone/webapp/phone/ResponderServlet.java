/*
 * Created on Apr 14, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package gov.nist.applet.phone.webapp.phone;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author root
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ResponderServlet extends HttpServlet {	

	public void doGet (HttpServletRequest request,HttpServletResponse repsonse) throws ServletException,IOException{
		//use of the responder
		//count the users
	  	ServletContext applicationContext=this.getServletContext();
	  	if(applicationContext.getAttribute("users")==null)		 
	  		applicationContext.setAttribute("users",new Integer(0));
	  	int users=((Integer)applicationContext.getAttribute("users")).intValue()+1;
	  	applicationContext.setAttribute("users",new Integer(users));		
	  	PrintWriter out=repsonse.getWriter();
		out.println(
		"<HTML>" +		"	<HEAD>" +		"		<TITLE>>Jain Sip Applet Phone</TITLE>" +		"	</HEAD>" +		"       <table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" width=\"100%\">   " +		"         <tr>" +		"			<td width=\"554\" ><img src=\"nisthome_banner.jpg\" ></td>" +		"			<td background=\"nisthome-bg.jpg\"> &nbsp;</td>" +		"		  </tr>" +		"		  <tr>" +		"			<td width=\"554\" > <img src=\"main-image.jpg\" ></td>" +		"			<td background=\"main-bg.jpg\"> &nbsp;</td>" +		"		  </tr>" +		"		  <tr>" +		"			<td width=\"554\" > <img src=\"bottom-bar.jpg\" border=\"0\" ></td>" +		"			<td background=\"bottom-bg.jpg\"> &nbsp;</td>" +		"	  	  </tr>" +		"		</table>" +		"<BODY bgcolor=\"#333333\" text=\"#FFFFFF\" leftmargin=\"0\" topmargin=\"0\" marginwidth=\"0\" marginheight=\"0\"" +		"link=\"white\" alink=\"white\" vlink=\"red\">" +		"	<center>" +
		"		<p>This application requires JMF, if you don't have it please install it from this there :<br>" +
		"		<a href=\"http://java.sun.com/products/java-media/jmf/2.1.1/download.html\">" +
		"				http://java.sun.com/products/java-media/jmf/2.1.1/download.html</a><br>" +		"		<p>If you're experiencing some problems viewing the applet,check your java plug-in control panel :<br>" +		"		- In the Proxies tab, uncheck the \"Use browser Settings\" box <br>" +		"		- In the Cache tab, uncheck the \"Enable Caching\" box<br>" +		"		<br><br>" +		"		<APPLET NAME=\"SipResponder\"" +		"				CODE=\"gov/nist/applet/phone/ua/gui/NISTMessengerApplet.class\"" +		"				ARCHIVE=\"applet-phone.jar\"" +		"				WIDTH=320 HEIGHT=520 MAYSCRIPT=\"true\">" +		"				<param name=\"PROXYADDRESS\" value=\""+request.getLocalAddr()+"\">" +		"				<param name=\"PROXYPORT\" value=\"4000\">" +		"				<param name=\"SIGNALINGTRANSPORT\" value=\"tcp\">" +		"			<param name=\"MYADDRESS\" value=\""+request.getRemoteAddr()+"\">" +		"			<param name=\"SERVERADDR\" value=\""+request.getLocalAddr()+"\">" +		"			<param name=\"SERVERPORT\" value=\""+request.getServerPort()+"\">" +		"			<param name=\"RESPONDER\" value=\"true\">" +		"			<param name=\"MEDIATRANSPORT\" value=\"tcp\">" +		"			<param name=\"USERURI\" value=\"user"+users+"@nist.gov\">" +		"				Your browser is completely ignoring the &lt;APPLET&gt; tag!" +		"		</APPLET>" +		"	</center>" +		"</BODY>" +		"</HTML>");

	  	  
	}

	public void doPost(HttpServletRequest request,HttpServletResponse repsonse) throws ServletException,IOException{
		//use of the responder
		//count the users
		ServletContext applicationContext=this.getServletContext();
		if(applicationContext.getAttribute("users")==null)		 
			applicationContext.setAttribute("users",new Integer(0));
		int users=((Integer)applicationContext.getAttribute("users")).intValue()+1;
		applicationContext.setAttribute("users",new Integer(users));		
		PrintWriter out=repsonse.getWriter();
		out.println(
		"<HTML>" +
		"	<HEAD>" +
		"		<TITLE>>Jain Sip Applet Phone</TITLE>" +
		"	</HEAD>" +
		"       <table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" width=\"100%\">   " +
		"         <tr>" +
		"			<td width=\"554\" ><img src=\"nisthome_banner.jpg\" ></td>" +
		"			<td background=\"nisthome-bg.jpg\"> &nbsp;</td>" +
		"		  </tr>" +
		"		  <tr>" +
		"			<td width=\"554\" > <img src=\"main-image.jpg\" ></td>" +
		"			<td background=\"main-bg.jpg\"> &nbsp;</td>" +
		"		  </tr>" +
		"		  <tr>" +
		"			<td width=\"554\" > <img src=\"bottom-bar.jpg\" border=\"0\" ></td>" +
		"			<td background=\"bottom-bg.jpg\"> &nbsp;</td>" +
		"	  	  </tr>" +
		"		</table>" +
		"<BODY bgcolor=\"#333333\" text=\"#FFFFFF\" leftmargin=\"0\" topmargin=\"0\" marginwidth=\"0\" marginheight=\"0\"" +
		"link=\"white\" alink=\"white\" vlink=\"red\">" +
		"	<center>" +		"		<p>This application requires JMF, if you don't have it please install it from this there :<br>" +		"		<a href=\"http://java.sun.com/products/java-media/jmf/2.1.1/download.html\">" +		"				http://java.sun.com/products/java-media/jmf/2.1.1/download.html</a><br>" +		"		<p>To try the responder, just double click on the responder@nist.gov contact and" +		"		on the chat frame window click on audio, then record your voice...<br><br> "+			
		"		<p>If you're experiencing some problems viewing the applet,check your java plug-in control panel :<br>" +
		"		- In the Proxies tab, uncheck the \"Use browser Settings\" box <br>" +
		"		- In the Cache tab, uncheck the \"Enable Caching\" box<br>" +
		"		<br><br>" +
		"		<APPLET NAME=\"SipResponder\"" +
		"				CODE=\"gov/nist/applet/phone/ua/gui/NISTMessengerApplet.class\"" +
		"				ARCHIVE=\"applet-phone.jar\"" +
		"				WIDTH=320 HEIGHT=520 MAYSCRIPT=\"true\">" +
		"				<param name=\"PROXYADDRESS\" value=\""+request.getLocalAddr()+"\">" +
		"				<param name=\"PROXYPORT\" value=\"4000\">" +
		"				<param name=\"SIGNALINGTRANSPORT\" value=\"tcp\">" +
		"			<param name=\"MYADDRESS\" value=\""+request.getRemoteAddr()+"\">" +
		"			<param name=\"SERVERADDR\" value=\""+request.getLocalAddr()+"\">" +
		"			<param name=\"SERVERPORT\" value=\""+request.getServerPort()+"\">" +
		"			<param name=\"RESPONDER\" value=\"true\">" +
		"			<param name=\"MEDIATRANSPORT\" value=\"tcp\">" +
		"			<param name=\"USERURI\" value=\"user"+users+"@nist.gov\">" +
		"				Your browser is completely ignoring the &lt;APPLET&gt; tag!" +
		"		</APPLET>" +
		"	</center>" +
		"</BODY>" +
		"</HTML>");

	}

}
