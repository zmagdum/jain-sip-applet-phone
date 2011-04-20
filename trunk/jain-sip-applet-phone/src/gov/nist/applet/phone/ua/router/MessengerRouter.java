/*******************************************************************************
 * Product of NIST/ITL Advanced Networking Technologies Division (ANTD).       *
 ******************************************************************************/
package gov.nist.applet.phone.ua.router;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.sip.ListeningPoint;
import javax.sip.SipStack;
import javax.sip.address.Hop;
import javax.sip.address.Router;
import javax.sip.message.Request;


/** 
 * This is the router of the application. When the implementation wants to forward
 * a request and  had run out of othe options, then it calls this method
 * to figure out where to send the request. The default router implements
 * a simple "default routing algorithm" which just forwards to the configured
 * proxy address. Use this for UAC/UAS implementations and use the GatewayRouter
 * for proxies.
 *
 * @version  JAIN-SIP-1.1 $Revision: 1.5 $ $Date: 2004/04/19 18:10:38 $
 *
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 */
public class MessengerRouter implements Router {

	protected SipStack sipStack;
	protected MessengerHop defaultRoute;
	private   boolean checked;
	private   boolean localLoopDetected;

	/**
	 * Constructor.
	 */
	public MessengerRouter(SipStack sipStack, String defaultRoute) {
		this.sipStack =  sipStack;
		if (defaultRoute != null) {
			this.defaultRoute = new MessengerHop(defaultRoute);
		}
	}

	/**
	 *  Return true if I have a listener listening here.
	 */
	private boolean hopsBackToMe(String host, int port) {
		Iterator it =sipStack.getListeningPoints();
		while (it.hasNext()) {
			ListeningPoint lp = (ListeningPoint) it.next();
			if ( sipStack.getIPAddress().equalsIgnoreCase(host)
				&& lp.getPort() == port)
				return true;
		}
		return false;

	}

	/**
	 * Return  addresses for default proxy to forward the request to.
	 * The list is organized in the following priority.
	 * If the requestURI refers directly to a host, the host and port
	 * information are extracted from it and made the next hop on the
	 * list.
	 * If the default route has been specified, then it is used
	 * to construct the next element of the list.
	 * Bug reported by Will Scullin -- maddr was being ignored when
	 * routing requests. Bug reported by Antonis Karydas - the RequestURI can
	 * be a non-sip URI.
	 *
	 * @param request is the sip request to route.
	 *
	 */
	public ListIterator getNextHops(Request request) {		

		LinkedList ll = null;

		if (! checked) {
			checked = true;
			if ( defaultRoute != null) {
				localLoopDetected = hopsBackToMe(defaultRoute.getHost(), defaultRoute.getPort());
			}
		}

		if (defaultRoute != null   && !localLoopDetected ) {
			ll = new LinkedList();
			ll.add(defaultRoute);			
		}

		return ll == null ? null : ll.listIterator();

	}

	/** 
	 * Get the default hop.
	 * 
	 * @return defaultRoute is the default route.
	 * public java.util.Iterator getDefaultRoute(Request request)
	 * { return this.getNextHops((SIPRequest)request); }
	 */

	public javax.sip.address.Hop getOutboundProxy() {
		return this.defaultRoute;
	}

	/** 
	 * Get the default route (does the same thing as getOutboundProxy).
	 *
	 * @return the default route.
	 */
	public Hop getDefaultRoute() {
		return this.defaultRoute;
	}
}
