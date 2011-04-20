/*
 * Configuration.java
 *
 * Created on November 17, 2003, 6:07 PM
 */

package gov.nist.applet.phone.ua;

import gov.nist.applet.phone.ua.gui.ServerInfoXmlManager;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
/**
 * This class represents the configuration of the user agent
 * 
 * @author  DERUELLE Jean
 */
public class Configuration {
    // Stack:
    public static long latency4VoiceMessaging=5000;
    public String name;
    public String password;
    public String stackName;
    public String stackIPAddress;
    public String contactIPAddress;
    public String outboundProxy;
    public int proxyPort;
    public int listeningPort;
    public String signalingTransport;
    public String mediaTransport;
    public String retransmissionFilter;    
    public String httpBusy;
    public String userURI;
    public Map map;
    public ServerInfoXmlManager serverInfoManager;
    public String path;
    /** Creates a new instance of Configuration */
    public Configuration() {
        stackName="Jain-Sip-Meeting-User-Agent";   
        path="xmlSource/ServerInfo.xml";
        serverInfoManager=new ServerInfoXmlManager(path);
        map=serverInfoManager.readXml();
        
        try{
            stackIPAddress=InetAddress.getLocalHost().getHostAddress();
            contactIPAddress=stackIPAddress;            
	    	//stackIPAddress="192.168.2.14";
        }
        catch(UnknownHostException uhe){
            uhe.printStackTrace();
        }
        retransmissionFilter="false";
        
        Iterator it=map.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry entry=(Entry) it.next();
			Object key=entry.getKey();
			Object value=entry.getValue();
			if(key.toString().equals("proxyaddress")){
				outboundProxy=value.toString();
			}else if(key.toString().equals("proxyport")){
				proxyPort=Integer.parseInt(value.toString());
			}
			else if(key.toString().equals("protocol")){
				signalingTransport=value.toString();
		        mediaTransport=value.toString();
			}
			else if(key.toString().equals("name")){
				name=value.toString();
			}
			else if(key.toString().equals("password")){
				password=value.toString();
			}
		}
		
//        outboundProxy="172.16.200.181";
//        proxyPort=5060;
        listeningPort=new java.util.Random().nextInt(8975)+1024;
//        signalingTransport="udp";
//        mediaTransport="udp";
        System.out.println();
        userURI=serverInfoManager.getInfo("name")+"@"+serverInfoManager.getInfo("proxyaddress");
        System.out.println(userURI);
//        userURI="181@172.16.200.181";
		httpBusy="http://www.google.com";
    }
}
