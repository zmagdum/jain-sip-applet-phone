/*
 * RegisterStatus.java
 *
 * Created on November 25, 2003, 4:03 PM
 */

package gov.nist.applet.phone.ua;

import javax.sip.ClientTransaction;
import javax.sip.message.Response;
import javax.sip.header.Header;
import javax.sip.header.ProxyAuthenticateHeader;
import javax.sip.header.WWWAuthenticateHeader ;
import javax.sip.header.CSeqHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ProxyAuthorizationHeader;
import javax.sip.header.AuthorizationHeader;
import gov.nist.applet.phone.ua.authentication.DigestClientAuthenticationMethod; 
/**
 *	Represents the registration status of the application
 * @author  DERUELLE Jean
 */
public class RegisterStatus {
    public static String NOT_REGISTERED="Not Registered";
    public static String REGISTRATION_IN_PROGRESS="Registration in progress...";
	public static String PROXY_AUTHENTICATION_REQUIRED="Proxy Authentication required";
    public static String REGISTERED="Registered";    
    
    /**
     * Register Status of the user
     */
    private String registerStatus=null;
    
	/**
	 * Keep a trace of the register Transaction
	 */ 
	private ClientTransaction registerTransaction;
	
	/**
	 * Keep a trace of the register's response
	 */ 
	public Response registerResponse;
    
    /** Creates a new instance of RegisterStatus */
    public RegisterStatus() {
        registerStatus=NOT_REGISTERED;
    }
    
    /**
     * Retrieve the current status of the registration
     * @return the current status of the registration
     */
    public String getStatus(){
        return this.registerStatus;
    }
    
    /**
     * Set the current status of the registration
     * @param registerStatus - the current status of the registration
     */
    public void setStatus(String registerStatus){
        this.registerStatus=registerStatus;
    }
    
	/**
	 * Retrieve the current client transaction of the registration
	 * @return the current client transaction of the registration
	 */
	public ClientTransaction getRegisterTransaction(){
		return this.registerTransaction;
	}

	/**
	 * Set the current client transaction of the registration
	 * @param registerTransaction - the current client transaction of the registration
	 */
	public void setRegisterTransaction(ClientTransaction registerTransaction){
		this.registerTransaction=registerTransaction;
	}
	
	/**
	 * Retrieve the current response of the registration
	 * @return the current response of the registration
	 */
	public Response getRegisterResponse(){
		return this.registerResponse;
	}

	/**
	 * Set the current response of the registration
	 * @param registerStatus - the current response of the registration
	 */
	public void setRegisterResponse(Response registerResponse){
		this.registerResponse=registerResponse;
	}
	
	public Header getHeader(Response response, 
							String userName, 
							String password,
							String outBoundProxy,
							int proxyPort) {
		try {
        
			// Proxy-Authorization header:
			ProxyAuthenticateHeader authenticateHeader=(ProxyAuthenticateHeader)
			response.getHeader(
			ProxyAuthenticateHeader.NAME);
        
			WWWAuthenticateHeader wwwAuthenticateHeader=null;
			CSeqHeader cseqHeader=(CSeqHeader)response.getHeader(CSeqHeader.NAME);
        
			String cnonce=null;
			String uri="sip:"+outBoundProxy+":"+proxyPort;
			String method=cseqHeader.getMethod();
			String nonce=null;
			String realm=null;
			String qop=null;
        
			if (authenticateHeader==null) {
				wwwAuthenticateHeader=(WWWAuthenticateHeader)
				response.getHeader(WWWAuthenticateHeader.NAME);
            
				nonce=wwwAuthenticateHeader.getNonce();
				realm=wwwAuthenticateHeader.getRealm();
				if (realm==null) {
					System.out.println("AuthenticationProcess, getProxyAuthorizationHeader(),"+
					" ERROR: the realm is not part of the 401 response!");
					return null;
				}
				cnonce=wwwAuthenticateHeader.getParameter("cnonce");
				qop=wwwAuthenticateHeader.getParameter("qop");
			}
			else {
            
				nonce=authenticateHeader.getNonce();
				realm=authenticateHeader.getRealm();
				if (realm==null) {
					System.out.println("AuthenticationProcess, getProxyAuthorizationHeader(),"+
					" ERROR: the realm is not part of the 407 response!");
					return null;
				}
				cnonce=authenticateHeader.getParameter("cnonce");
				qop=authenticateHeader.getParameter("qop");
			}
        			
			HeaderFactory headerFactory=MessageListener.headerFactory;
       
			DigestClientAuthenticationMethod digest=new DigestClientAuthenticationMethod();
			digest.initialize(realm,userName,uri,nonce,password,method,cnonce,"MD5");
        
			if (authenticateHeader==null) {
				AuthorizationHeader header=headerFactory.createAuthorizationHeader("Digest");
				header.setParameter("username",userName);
				header.setParameter("realm",realm);
				header.setParameter("uri",uri);
				header.setParameter("algorithm","MD5");
				header.setParameter("opaque","");
				header.setParameter("nonce",nonce);
				header.setParameter("response",digest.generateResponse());
				if (qop!=null)
					header.setParameter("qop",qop);
            
            
				return header;
            
			}
			else {
				ProxyAuthorizationHeader header=headerFactory.createProxyAuthorizationHeader("Digest");
				header.setParameter("username",userName);
				header.setParameter("realm",realm);
				header.setParameter("uri",uri);
				header.setParameter("algorithm","MD5");
				header.setParameter("opaque","");
				header.setParameter("nonce",nonce);
				header.setParameter("response",digest.generateResponse());
				if (qop!=null)
					header.setParameter("qop",qop);
            
            
				return header;
            
			}
        

		}
		catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
}
