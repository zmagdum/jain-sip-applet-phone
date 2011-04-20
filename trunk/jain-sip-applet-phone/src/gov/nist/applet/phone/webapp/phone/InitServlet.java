/*
 * InitServlet.java
 *
 * Created on June 30, 2003, 4:19 PM
 */

package gov.nist.applet.phone.webapp.phone;

import javax.servlet.*;
import javax.servlet.http.*;

/** Initializes the web appplication
 * @author DERUELLE Jean
 */
public class InitServlet extends HttpServlet {
    
    /**
     * Initializes the servlet.
     * @param config - The ServletConfig of the webapp
     * @throws ServletException - a servletException can be thrown is something mess up
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);        
		/**
		InetAddress ipAddress=null;
	        try{
				ipAddress=InetAddress.getByName(config.getInitParameter("ip-address"));
	        }
			catch(UnknownHostException uhe){
				uhe.printStackTrace();
				System.out.println("Application not started");
				return;
			}
			int port;
			try{
				port=Integer.parseInt(config.getInitParameter("socket-manager-port"));
			}
			catch(NumberFormatException nfe){
				nfe.printStackTrace();
				System.out.println("Application not started");
				return;
			}
	        IPAddressSocketManager ipAddressSocketManager=new IPAddressSocketManager(ipAddress,port);
	        ipAddressSocketManager.start();
		**/
    }               
}
