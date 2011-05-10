/*
 * MediaManager.java
 *
 * Created on December 2, 2003, 8:56 AM
 */
/**
 * 测试中文问题
 */
package gov.nist.applet.phone.media;

import javax.sdp.SessionDescription;
import javax.sdp.SdpFactory;
import javax.sdp.SdpException;
import javax.sdp.SdpParseException;
import javax.sdp.Origin;
import javax.sdp.Connection;
import javax.sdp.SdpConstants;
import javax.sdp.MediaDescription;
import javax.sdp.Media;

import javax.media.*;
import javax.media.protocol.*;
import javax.media.protocol.DataSource;
import javax.media.format.*;
import javax.media.control.TrackControl;

import gov.nist.applet.phone.ua.MessageListener;
import gov.nist.applet.phone.media.receiver.Receiver;
import gov.nist.applet.phone.media.transmitter.Transmit;
import gov.nist.applet.phone.media.transmitter.StateListener;

import java.util.List;
import java.util.Iterator;
import java.util.Vector;
import java.util.Random;
import java.io.IOException;

/**
 * This class will handle the media part of a call
 * Opening the receiver and transmitter, close them,...
 * 
 * @author Jean Deruelle <jean.deruelle@nist.gov>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class MediaManager {
	//Media transmitter
	private Transmit transmit;
	//Media receiver
	private Receiver receiver;
	//Codec supported by the user agent
	public static List audioCodecSupportedList = null;
	public static List videoCodecSupportedList = null;
	//Remote Address to connect to
	private String remoteAddress;
	//Ports chosen
	private int remoteAudioPort;
	private int localAudioPort = -1;
	private int remoteVideoPort;
	private int localVideoPort = -1;
	//Codecs chosen
	private String negotiatedAudioCodec;
	private String negotiatedVideoCodec;
	//SdpFactory to create or parse Sdp Content 
	private SdpFactory sdpFactory;
	//callListener
	private MessageListener callListener;
	//flag to know if the session has been started
	private boolean started;
	//flag to know if the media session is going through the proxy
	private boolean proxyEnabled;

	/** 
	 * Creates a new instance of MediaManager 
	 * @param callListener - the sipListener of the application
	 */
	public MediaManager(MessageListener callListener) {
		try {
			sdpFactory = SdpFactory.getInstance();
		} catch (Exception se) {
			se.printStackTrace();
		}
		this.callListener = callListener;
		started = false;
		proxyEnabled = false;
	}

	/**
	 * get the supported audio codecs in the sdp format
	 * @return array of the audio supported codecs
	 */
	public static String[] getSdpAudioSupportedCodecs() {
		Vector sdpSupportedCodecsList = new Vector();
		for (int i = 0; i < audioCodecSupportedList.size(); i++) {
			String sdpFormat =
				findCorrespondingSdpFormat(
					((Format) audioCodecSupportedList.get(i)).getEncoding());
			boolean redundant = false;
			for (int j = 0; j < sdpSupportedCodecsList.size(); j++) {
				if (sdpFormat == null){
					redundant = true;
					break;
				}					
				else if (sdpFormat
					.equalsIgnoreCase((String) sdpSupportedCodecsList.get(j))){
					redundant = true;
					break;
				}
					
			}
			if (!redundant)
				sdpSupportedCodecsList.addElement(sdpFormat);
		}
		for (int i = 0; i < sdpSupportedCodecsList.size(); i++)
			System.out.println(sdpSupportedCodecsList.get(i));
		return (String[]) sdpSupportedCodecsList.toArray(
			new String[sdpSupportedCodecsList.size()]);
	}

	/**
	 * get the supported video codecs in the sdp format
	 * @return array of the video supported codecs
	 */
	public static String[] getSdpVideoSupportedCodecs() {
		Vector sdpSupportedCodecsList = new Vector();
		for (int i = 0; i < videoCodecSupportedList.size(); i++) {
			String sdpFormat =
				findCorrespondingSdpFormat(
					((Format) videoCodecSupportedList.get(i)).getEncoding());
			boolean redundant = false;
			for (int j = 0; j < sdpSupportedCodecsList.size(); j++) {
				if (sdpFormat == null){
					redundant = true;
					break;
				}					
				else if (sdpFormat
					.equalsIgnoreCase((String) sdpSupportedCodecsList.get(j))){
					redundant = true;
					break;
				}
					
			}
			if (!redundant)
				sdpSupportedCodecsList.addElement(sdpFormat);
		}
		return (String[]) sdpSupportedCodecsList.toArray(
			new String[sdpSupportedCodecsList.size()]);
	}

	/**
	 * Detects the supported codecs of the user agent depending of 
	 * the devices connected to the computer
	 */
	public static void detectSupportedCodecs() {
		audioCodecSupportedList = new Vector();
		videoCodecSupportedList = new Vector();
		MediaLocator audioLocator = null;
		MediaLocator videoLocator = null;
		CaptureDeviceInfo videoCDI = null;
		CaptureDeviceInfo audioCDI = null;
		Vector captureDevices = null;
		captureDevices = CaptureDeviceManager.getDeviceList(null);
		System.out.println(
			"- number of capture devices: " + captureDevices.size());
		CaptureDeviceInfo cdi = null;
		for (int i = 0; i < captureDevices.size(); i++) {
			cdi = (CaptureDeviceInfo) captureDevices.elementAt(i);
			System.out.println(
				"    - name of the capture device: " + cdi.getName());
			Format[] formatArray = cdi.getFormats();
			for (int j = 0; j < formatArray.length; j++) {
				Format format = formatArray[j];
				if (format instanceof VideoFormat) {
					System.out.println(
						"         - format accepted by this VIDEO device: "
							+ format.toString().trim());
					if (videoCDI == null) {
						videoCDI = cdi;
					}
				} else if (format instanceof AudioFormat) {
					System.out.println(
						"         - format accepted by this AUDIO device: "
							+ format.toString().trim());
					if (audioCDI == null) {
						audioCDI = cdi;
					}
				} else
					System.out.println("         - format of type UNKNOWN");
			}
		}
		if (videoCDI != null)
			videoLocator = videoCDI.getLocator();
		if (audioCDI != null)
			audioLocator = audioCDI.getLocator();

		DataSource audioDS = null;
		DataSource videoDS = null;
		DataSource mergeDS = null;
		StateListener stateListener = new StateListener();
		//create the DataSource
		//it can be a 'video' DataSource, an 'audio' DataSource
		//or a combination of audio and video by merging both
		if (videoLocator == null && audioLocator == null)
			return;
		if (audioLocator != null) {
			try {
				//create the 'audio' DataSource
				audioDS = javax.media.Manager.createDataSource(audioLocator);
			} catch (Exception e) {
				System.out.println(
					"-> Couldn't connect to audio capture device");
			}
		}
		if (videoLocator != null) {
			try {
				//create the 'video' DataSource
				videoDS = javax.media.Manager.createDataSource(videoLocator);
			} catch (Exception e) {
				System.out.println(
					"-> Couldn't connect to video capture device");
			}
		}
		Processor processor = null;
		if (videoDS != null && audioDS != null) {
			try {
				//create the 'audio' and 'video' DataSource
				mergeDS =
					javax.media.Manager.createMergingDataSource(
						new DataSource[] { audioDS, videoDS });
			} catch (Exception e) {
				System.out.println(
					"-> Couldn't connect to audio or video capture device");
			}
			try {
				//Create the processor from the merging DataSource
				processor = javax.media.Manager.createProcessor(mergeDS);
			} catch (NoProcessorException npe) {
				npe.printStackTrace();
				return;
			} catch (IOException ioe) {
				ioe.printStackTrace();
				return;
			}
		}
		//if the processor has not been created from the merging DataSource
		if (processor == null) {
			try {
				if (audioDS != null)
					//Create the processor from the 'audio' DataSource
					processor = javax.media.Manager.createProcessor(audioDS);
				else
					//Create the processor from the 'video' DataSource
					processor = javax.media.Manager.createProcessor(videoDS);
			} catch (NoProcessorException npe) {
				npe.printStackTrace();
				return;
			} catch (IOException ioe) {
				ioe.printStackTrace();
				return;
			}
		}
		// Wait for it to configure
		boolean result =
			stateListener.waitForState(processor, Processor.Configured);
		if (result == false) {
			System.out.println("Couldn't configure processor");
			return;
		}

		// Get the tracks from the processor
		TrackControl[] tracks = processor.getTrackControls();

		// Do we have atleast one track?
		if (tracks == null || tracks.length < 1) {
			System.out.println("Couldn't find tracks in processor");
			return;
		}
		// Set the output content descriptor to RAW_RTP
		// This will limit the supported formats reported from
		// Track.getSupportedFormats to only valid RTP formats.
		ContentDescriptor cd = new ContentDescriptor(ContentDescriptor.RAW_RTP);
		processor.setContentDescriptor(cd);

		Format supported[];
		Format chosen = null;
		boolean atLeastOneTrack = false;

		// Program the tracks.
		for (int i = 0; i < tracks.length; i++) {
			Format format = tracks[i].getFormat();
			if (tracks[i].isEnabled()) {
				supported = tracks[i].getSupportedFormats();
				// We've set the output content to the RAW_RTP.
				// So all the supported formats should work with RTP.            
				if (supported.length > 0) {
					for (int j = 0; j < supported.length; j++) {
						System.out.println(
							"Supported format : "
								+ supported[j].toString().toLowerCase());
						//Add to the list of video supported codec
						if (supported[j] instanceof VideoFormat) {
							videoCodecSupportedList.add(supported[j]);
						}
						//Add to the list of audio supported codec
						else {
							audioCodecSupportedList.add(supported[j]);
						}
					}
				}
			}
		}
		processor.stop();
		processor.close();
	}

	/**
	 * Start the media Session i.e. transmitting and receiving RTP stream
	 * TODO : handle the sendonly, recvonly, sendrcv attributes of the SDP session
	 */
	public void startMediaSession(boolean transmitFirst) {
		if (!started) {
			startReceiving();
			startTransmitting();
			started = true;
		}
	}

	/**
	 * Stop the media Session i.e. transmitting and receiving RTP stream
	 * TODO : handle the sendonly, recvonly, sendrcv attributes of the SDP session
	 */
	public void stopMediaSession() {
		stopReceiving();
		stopTransmitting();
		started = false;
	}

	/**
	 * Start receiving RTP stream
	 */
	protected void startReceiving() {
		gov.nist.applet.phone.media.util.SessionDescription sessionDescription =
			new gov.nist.applet.phone.media.util.SessionDescription();
		sessionDescription.setAddress(remoteAddress);
		sessionDescription.setDestinationPort(remoteAudioPort);
		sessionDescription.setLocalPort(localAudioPort);
		sessionDescription.setTransportProtocol("udp");
		sessionDescription.setAudioFormat(negotiatedAudioCodec);
		sessionDescription.setVideoFormat(negotiatedVideoCodec);
		receiver = new Receiver(sessionDescription, transmit);
		receiver.receive(callListener.getConfiguration().contactIPAddress);
	}

	/**
	 * start transmitting RTP stream
	 */
	protected void startTransmitting() {
		gov.nist.applet.phone.media.util.SessionDescription sessionDescription =
			new gov.nist.applet.phone.media.util.SessionDescription();
		sessionDescription.setAddress(remoteAddress);
		sessionDescription.setLocalPort(remoteAudioPort);
		sessionDescription.setDestinationPort(remoteAudioPort);
		sessionDescription.setTransportProtocol("udp");
		/*if(mediaTransport.equalsIgnoreCase("tcp"))
		    sessionDescription.setAudioFormat("99");        
		else*/
		sessionDescription.setAudioFormat(negotiatedAudioCodec);
		sessionDescription.setVideoFormat(negotiatedVideoCodec);
		transmit = new Transmit(sessionDescription, receiver);
		// Start the media transmission
		String result =
			transmit.start(callListener.getConfiguration().contactIPAddress);
		System.out.println("Media Transmission started!!!");
		// result will be non-null if there was an error. The return
		// value is a String describing the possible error. Print it.
		if (result != null) {
			System.err.println("Error : " + result);
			//System.exit(0);
		}
	}

	/**
	 * Stop transmitting RTP stream
	 */
	protected void stopTransmitting() {
		if (transmit != null) {
			System.out.println("Media Transmitter stopped!!!");
			transmit.stop();
		}
	}

	/**
	 * Stop receiving RTP stream
	 */
	protected void stopReceiving() {
		if (transmit != null) {
			System.out.println("Media Receiver stopped!!!");
			receiver.stop();
		}
	}

	/**
	 * Extracts from the sdp all the information to initiate the media session
	 * @param incomingSdpBody - the sdp Body of the incoming call to negotiate the media session
	 */
	public void prepareMediaSession(String incomingSdpBody) {
		SessionDescription sessionDescription = null;
		try {
			sessionDescription =
				sdpFactory.createSessionDescription(incomingSdpBody);
			//Get the remote address where the user agent has to connect to
			Connection remoteConnection = sessionDescription.getConnection();
			remoteAddress = remoteConnection.getAddress();
		} catch (SdpParseException spe) {
			
			spe.printStackTrace();
		}
		localAudioPort = getAudioPort();
		localVideoPort = getVideoPort();
		System.out.println("Local listening audio port : " + localAudioPort);
		//Extract the codecs from the sdp session description
		List audioCodecList = extractAudioCodecs(sessionDescription);
		remoteAudioPort = getAudioPort(sessionDescription);
		//printCodecs(audioCodecList);        
		System.out.println("Remote listening audio port : " + remoteAudioPort);
		List videoCodecList = extractVideoCodecs(sessionDescription);
		remoteVideoPort = getVideoPort(sessionDescription);
		//printCodecs(videoCodecList);
		//System.out.println("Remote listening video port : "+remoteVideoPort);
		negotiatedAudioCodec = negotiateAudioCodec(audioCodecList);
		negotiatedVideoCodec = negotiateVideoCodec(videoCodecList);
	}

	/**
	 * Getting the sdp body for creating the response to an incoming call
	 * @param incomingSdpBody - the sdp Body of the incoming call to negotiate the media session
	 * @return The sdp body that will present what codec has been chosen
	 * and on which port every media will be received
	 */
	public Object getResponseSdpBody(String incomingSdpBody) {
		prepareMediaSession(incomingSdpBody);
		SessionDescription sessionDescription = null;
		try {
			
			sessionDescription =
				sdpFactory.createSessionDescription(incomingSdpBody);
			//Get the remote address where the user agent has to connect to
			Connection remoteConnection = sessionDescription.getConnection();
			remoteAddress = remoteConnection.getAddress();
		} catch (SdpParseException spe) {
			spe.printStackTrace();
		}
		//Constructing the sdp response body
		SessionDescription responseSessionDescription = null;
		try {
			responseSessionDescription =
				(SessionDescription) sessionDescription.clone();
		} catch (CloneNotSupportedException cnse) {
			cnse.printStackTrace();
		}
		
		try {
			//Connection
			Connection connection =
				sdpFactory.createConnection(
					callListener.getConfiguration().contactIPAddress);
			responseSessionDescription.setConnection(connection);
			//Owner
			long sdpSessionId=(long)(Math.random() * 1000000);		
			Origin origin =
				sdpFactory.createOrigin(
					callListener.sipMeetingManager.getUserURI().getUser(),
					sdpSessionId,
					sdpSessionId+1369,
					"IN",
					"IP4",
				callListener.getConfiguration().contactIPAddress);			
			responseSessionDescription.setOrigin(origin);
		} catch (SdpException se) {
			se.printStackTrace();
		}
		//Media Description        
		Vector mediaDescriptions = new Vector();
		if (negotiatedAudioCodec != null) {
			System.out.println(
				"Negotiated audio codec "
					+ negotiatedAudioCodec
					+ " on Port "
					+ localAudioPort);
			MediaDescription mediaDescription =
				sdpFactory.createMediaDescription(
					"audio",
					localAudioPort,
					1,
					"RTP/AVP",
					new String[] { negotiatedAudioCodec });
			mediaDescriptions.add(mediaDescription);
		} else {
			System.out.println(
				"No Negotiated audio codec,"
					+ "so no audio media descriptions will be added to the sdp body");
		}
		if (negotiatedVideoCodec != null) {
			System.out.println(
				"Negotiated video codec "
					+ negotiatedVideoCodec
					+ " on Port "
					+ localVideoPort);
			MediaDescription mediaDescription =
				sdpFactory.createMediaDescription(
					"video",
					localVideoPort,
					1,
					"RTP/AVP",
					new String[] { negotiatedVideoCodec });
			mediaDescriptions.add(mediaDescription);
		} else {
			System.out.println(
				"No Negotiated video codec,"
					+ "so no video media descriptions will be added to the sdp body");
		}
		try {
			responseSessionDescription.setMediaDescriptions(mediaDescriptions);
		} catch (SdpException se) {
			se.printStackTrace();
		}

		return responseSessionDescription;
	}

	/**
	 * Extracts all the audio codecs from the description of the media session of the incoming request
	 * @param sessionDescription - the description of the media session of the incoming request
	 * @return List of all the audio codecs from the description of the media session of the incoming request
	 */
	public List extractAudioCodecs(SessionDescription sessionDescription) {
		List audioCodecList = new Vector();
		Vector mediaDescriptionList = null;
		try {
			mediaDescriptionList =
				sessionDescription.getMediaDescriptions(true);
		} catch (SdpException se) {
			se.printStackTrace();
		}
		try {
			for (int i = 0; i < mediaDescriptionList.size(); i++) {
				MediaDescription mediaDescription =
					(MediaDescription) mediaDescriptionList.elementAt(i);
				Media media = mediaDescription.getMedia();
				if (media.getMediaType().equals("audio"))
					audioCodecList = media.getMediaFormats(true);
			}
		} catch (SdpParseException spe) {
			spe.printStackTrace();
		}
		return audioCodecList;
	}

	/**
	 * Extracts all the video codecs from the description of the media session of the incoming request
	 * @param sessionDescription - the description of the media session of the incoming request
	 * @return List of all the audio codecs from the description of the media session of the incoming request
	 */
	public List extractVideoCodecs(SessionDescription sessionDescription) {
		List videoCodecList = new Vector();
		Vector mediaDescriptionList = null;
		try {
			mediaDescriptionList =
				sessionDescription.getMediaDescriptions(true);
		} catch (SdpException se) {
			se.printStackTrace();
		}
		try {
			for (int i = 0; i < mediaDescriptionList.size(); i++) {
				MediaDescription mediaDescription =
					(MediaDescription) mediaDescriptionList.elementAt(i);
				Media media = mediaDescription.getMedia();
				if (mediaDescription.getMedia().getMediaType().equals("video"))
					videoCodecList = media.getMediaFormats(true);
			}
		} catch (SdpParseException spe) {
			spe.printStackTrace();
		}
		return videoCodecList;
	}

	/**
	 * Extracts the audio port from the description of the media session of the incoming request
	 * @param sessionDescription - the description of the media session of the incoming request
	 * @return the audio port on which is listening the remote user agent
	 */
	public int getAudioPort(SessionDescription sessionDescription) {
		Vector mediaDescriptionList = null;
		try {
			mediaDescriptionList =
				sessionDescription.getMediaDescriptions(true);
		} catch (SdpException se) {
			se.printStackTrace();
		}
		try {
			for (int i = 0; i < mediaDescriptionList.size(); i++) {
				MediaDescription mediaDescription =
					(MediaDescription) mediaDescriptionList.elementAt(i);
				if (mediaDescription.getMedia().getMediaType().equals("audio"))
					return mediaDescription.getMedia().getMediaPort();
			}
		} catch (SdpParseException spe) {
			spe.printStackTrace();
		}
		return -1;
	}

	/**
	 * Extracts the video port from the description of the media session of the incoming request
	 * @param sessionDescription - the description of the media session of the incoming request
	 * @return the video port on which is listening the remote user agent
	 */
	public int getVideoPort(SessionDescription sessionDescription) {
		Vector mediaDescriptionList = null;
		try {
			mediaDescriptionList =
				sessionDescription.getMediaDescriptions(true);
		} catch (SdpException se) {
			se.printStackTrace();
		}
		try {
			for (int i = 0; i < mediaDescriptionList.size(); i++) {
				MediaDescription mediaDescription =
					(MediaDescription) mediaDescriptionList.elementAt(i);
				if (mediaDescription.getMedia().getMediaType().equals("video"))
					return mediaDescription.getMedia().getMediaPort();
			}
		} catch (SdpParseException spe) {
			spe.printStackTrace();
		}
		return -1;
	}

	/**
	 * Find the best codec between our own supported codecs 
	 * and the remote supported codecs to initiate the media session
	 * Currently, take the first one to match
	 * @param audioCodecList - the list of the remote audio supported codecs
	 * @return the negotiated audio codec
	 */
	public String negotiateAudioCodec(List audioCodecList) {
		//Find the mapping of the jmf format to the sdp format
		List audioCodecSupportedSdpFormat = new Vector();
		Iterator it = audioCodecSupportedList.iterator();
		while (it.hasNext()) {
			String sdpCodecValue =
				findCorrespondingSdpFormat(((Format) it.next()).getEncoding());
			if (sdpCodecValue != null)
				audioCodecSupportedSdpFormat.add(sdpCodecValue);
		}
		//find the best codec(currently the first one which is in both list)
		Iterator iteratorSupportedCodec =
			audioCodecSupportedSdpFormat.iterator();
		while (iteratorSupportedCodec.hasNext()) {
			String supportedCodec = (String) iteratorSupportedCodec.next();
			Iterator iteratorRemoteCodec = audioCodecList.iterator();
			while (iteratorRemoteCodec.hasNext()) {
				String remoteCodec = iteratorRemoteCodec.next().toString();
				if (remoteCodec.equals(supportedCodec))
					return remoteCodec;
			}
		}
		return null;
	}

	/**
	 * Find the best codec between our own supported codecs 
	 * and the remote supported codecs to initiate the media session
	 * Currently, take the first one to match
	 * @param videoCodecList - the list of the remote video supported codecs
	 * @return the negotiated video codec
	 */
	public String negotiateVideoCodec(List videoCodecList) {
		//Find the mapping of the jmf format to the sdp format
		List videoCodecSupportedSdpFormat = new Vector();
		Iterator it = videoCodecSupportedList.iterator();
		while (it.hasNext()) {
			String sdpCodecValue =
				findCorrespondingSdpFormat(((Format) it.next()).getEncoding());
			if (sdpCodecValue != null)
				videoCodecSupportedSdpFormat.add(sdpCodecValue);
		}
		//find the best codec(currently the first one which is in both list)        
		Iterator iteratorSupportedCodec =
			videoCodecSupportedSdpFormat.iterator();
		while (iteratorSupportedCodec.hasNext()) {
			String supportedCodec = (String) iteratorSupportedCodec.next();
			Iterator iteratorRemoteCodec = videoCodecList.iterator();
			while (iteratorRemoteCodec.hasNext()) {
				String remoteCodec = iteratorRemoteCodec.next().toString();
				if (remoteCodec.equals(supportedCodec))
					return remoteCodec;
			}
		}
		return null;
	}

	/**
	 * Utility method to print the remote codecs
	 */
	public void printCodecs(List codecList) {
		System.out.println("List of codecs: ");
		Iterator it = codecList.iterator();
		while (it.hasNext()) {
			String att = it.next().toString();
			System.out.println(att);
		}
	}

	/**
	 * Utility method to print the supported codecs
	 */
	public void printSupportedCodecs() {
		System.out.println("List of supported audio codecs: ");
		Iterator it = audioCodecSupportedList.iterator();
		while (it.hasNext()) {
			System.out.println(((Format) it.next()).toString());
		}
		System.out.println("List of supported video codecs: ");
		it = videoCodecSupportedList.iterator();
		while (it.hasNext()) {
			System.out.println(((Format) it.next()).toString());
		}
	}

	/**
	 * Utility method to print the supported codecs in parameter
	 * @param codecList - the list of codec to print out
	 */
	public void printSupportedCodecs(List codecList) {
		System.out.println("List of codecs: ");
		Iterator it = codecList.iterator();
		while (it.hasNext()) {
			System.out.println(it.next());
		}
	}

	/**
	 * Retrieve the audio port for this media session,
	 * if no audio port has been allowed it will choose one and return it
	 * @return the audio port for this media session
	 */
	public int getAudioPort() {
		if (localAudioPort == -1) {
			localAudioPort = new Random().nextInt(8885);
			if (localAudioPort % 2 == 0)
				localAudioPort += 1024;
			else
				localAudioPort += 1025;
		}
		return localAudioPort;
	}

	/**
	 * Retrieve the video port for this media session,
	 * if no video port has been allowed it will choose one and return it
	 * @return the video port for this media session
	 */
	public int getVideoPort() {
		if (localVideoPort == -1) {
			localVideoPort = new Random().nextInt(8885);
			if (localVideoPort % 2 == 0)
				localVideoPort += 1024;
			else
				localVideoPort += 1025;
		}
		return localVideoPort;
	}

	/**
	 * Map a jmf format to a sdp format
	 * @param jmfFormat - the jmf Format
	 * @return the corresponding sdp format
	 */
	public static String findCorrespondingSdpFormat(String jmfFormat) {
		if (jmfFormat == null) {
			return null;
		} else if (jmfFormat.equals(AudioFormat.ULAW_RTP)) {
			return Integer.toString(SdpConstants.PCMU);
		} else if (jmfFormat.equals(AudioFormat.GSM_RTP)) {
			return Integer.toString(SdpConstants.GSM);
		} else if (jmfFormat.equals(AudioFormat.G723_RTP)) {
			return Integer.toString(SdpConstants.G723);
		} else if (jmfFormat.equals(AudioFormat.DVI_RTP)) {
			return Integer.toString(SdpConstants.DVI4_8000);
		} else if (jmfFormat.equals(AudioFormat.DVI_RTP)) {
			return Integer.toString(SdpConstants.DVI4_16000);
		} else if (jmfFormat.equals(AudioFormat.ALAW)) {
			return Integer.toString(SdpConstants.PCMA);
		} else if (jmfFormat.equals(AudioFormat.G728_RTP)) {
			return Integer.toString(SdpConstants.G728);
		} else if (jmfFormat.equals(AudioFormat.G729_RTP)) {
			return Integer.toString(SdpConstants.G729);
		} else if (jmfFormat.equals(VideoFormat.H263_RTP)) {
			return Integer.toString(SdpConstants.H263);
		} else if (jmfFormat.equals(VideoFormat.JPEG_RTP)) {
			return Integer.toString(SdpConstants.JPEG);
		} else if (jmfFormat.equals(VideoFormat.H261_RTP)) {
			return Integer.toString(SdpConstants.H261);
		} else {
			return null;
		}
	}

	/**
	 * Map a sdp format to a jmf format
	 * @param sdpFormatStr - the sdp Format
	 * @return the corresponding jmf format
	 */
	public static String findCorrespondingJmfFormat(String sdpFormatStr) {
		int sdpFormat = -1;
		try {
			sdpFormat = Integer.parseInt(sdpFormatStr);
		} catch (NumberFormatException ex) {
			return null;
		}
		switch (sdpFormat) {
			case SdpConstants.PCMU :
				return AudioFormat.ULAW_RTP;
			case SdpConstants.GSM :
				return AudioFormat.GSM_RTP;
			case SdpConstants.G723 :
				return AudioFormat.G723_RTP;
			case SdpConstants.DVI4_8000 :
				return AudioFormat.DVI_RTP;
			case SdpConstants.DVI4_16000 :
				return AudioFormat.DVI_RTP;
			case SdpConstants.PCMA :
				return AudioFormat.ALAW;
			case SdpConstants.G728 :
				return AudioFormat.G728_RTP;
			case SdpConstants.G729 :
				return AudioFormat.G729_RTP;
			case SdpConstants.H263 :
				return VideoFormat.H263_RTP;
			case SdpConstants.JPEG :
				return VideoFormat.JPEG_RTP;
			case SdpConstants.H261 :
				return VideoFormat.H261_RTP;
			case 99 :
				return "mpegaudio/rtp, 48000.0 hz, 16-bit, mono";
			default :
				return null;
		}
	}
	/*PCMU 		javax.media.format.AudioFormat.ULAW_RTP;
	1016
	G721
	GSM 		javax.media.format.AudioFormat.GSM_RTP;
	G723		javax.media.format.AudioFormat.G723_RTP
	DVI4_8000           javax.media.format.AudioFormat.DVI_RTP;
	DVI4_16000          javax.media.format.AudioFormat.DVI_RTP;
	LPC
	PCMA		javax.media.format.AudioFormat.ALAW;
	G722		javax.media.format.AudioFormat.ALAW;
	L16_2CH
	L16_1CH
	QCELP
	CN
	MPA
	G728		javax.media.format.AudioFormat.G728_RTP;
	DVI4_11025
	DVI4_22050
	G729		javax.media.format.AudioFormat.G729_RTP
	CN_DEPRECATED
	H263		javax.media.format.VideoFormat.H263_RTP
	CelB
	JPEG		javax.media.format.VideoFormat.JPEG_RTP
	nv
	H261		javax.media.format.VideoFormat.H261_RTP
	MPV*/
}
