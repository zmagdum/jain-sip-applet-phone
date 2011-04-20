package gov.nist.applet.phone.ua.gui;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

public class ConnectionXmlManager {
	Document doc;
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	DocumentBuilder builder;
	NodeList numbers;
	String path;
	Map map=null;

	public NodeList getNumbers() {
		return numbers;
	}

	public void setNumbers(NodeList numbers) {
		this.numbers = numbers;
	}

	/**
	 * 构造方法
	 * 
	 * @param path
	 *            :xml文件的路径
	 * @param nodes
	 *            ：要解析的xml节点名称
	 */
	public ConnectionXmlManager(String path) {
		super();
		this.path = path;
//		System.out.println(System.getProperty("user.dir"));
	}

	/**
	 * 解析XML
	 * 
	 * @param path
	 */
	public Map readXml() {
		if(map==null){
			map=new HashMap();
		}
		try {
			builder = factory.newDocumentBuilder();
			Document doc = builder.parse(path);
			doc.normalize();
			NodeList numbers = doc.getElementsByTagName("number");
			this.setNumbers(numbers);
			for (int i = 0; i < numbers.getLength(); i++) {
				Element link = (Element) numbers.item(i);
				map.put( link.getTextContent(),link.getAttribute("value"));
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}

	/**
	 * addCode
	 * 
	 * @param path
	 */
	public void addXmlCode(String number, String value) {
		try {
			builder = factory.newDocumentBuilder();
			Document doc = builder.parse(path);
			doc.normalize();
			Text textseg;
			Element imag = doc.createElement("number");
			imag.setAttribute("value", value);
			textseg=doc.createTextNode(number);
			imag.appendChild(textseg);
			doc.getDocumentElement().appendChild(imag);
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer;
			transformer = tFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new java.io.File(path));
			transformer.transform(source, result);
		} catch (Exception e) {
		}
	}

	/**
	 * delete xml code
	 * 
	 * @param path
	 */
	public void delXmlCode(String number,String value) {
		try {
			builder = factory.newDocumentBuilder();
			doc = builder.parse(path);
			doc.normalize();
			NodeList imags = doc.getElementsByTagName("number");
			
			for (int i = 0; i < imags.getLength(); i++) {
				Element link = (Element) imags.item(i);
				if(link.getTextContent().equals(number)&&link.getAttribute("value").equals(value)){
				doc.getFirstChild().removeChild(link);
					
				}
				//System.out.println(link.getTextContent()+"--->"+link.getAttribute("value"));
			}
			
//			Element elink = (Element) imags.item(0);
//			elink.removeChild(elink.getElementsByTagName("imgsrc").item(0));
//			elink.removeChild(elink.getElementsByTagName("title").item(0));
//			elink.removeChild(elink.getElementsByTagName("url").item(0));
//			doc.getFirstChild().removeChild(elink);
			
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new java.io.File(path));
			transformer.transform(source, result);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}
}
