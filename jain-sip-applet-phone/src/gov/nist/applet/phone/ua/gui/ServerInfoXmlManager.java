package gov.nist.applet.phone.ua.gui;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

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

public class ServerInfoXmlManager {
	Document doc;
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	DocumentBuilder builder;
	NodeList numbers;
	String path;
	Map map = null;

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
	public ServerInfoXmlManager(String path) {
		super();
		this.path = path;
		// System.out.println(System.getProperty("user.dir"));
	}

	/**
	 * 解析XML
	 * 
	 * @param path
	 */
	public String getInfo(String nodename) {
		String string="";
		Map map = this.readXml();
		Iterator it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Entry) it.next();
			Object key = entry.getKey();
			Object value = entry.getValue();
			if (key.toString().equals(nodename)) {
			string=value.toString();
			}
		}
		return string;
	}

	public Map readXml() {
		if (map == null) {
			map = new HashMap();
		}
		try {
			builder = factory.newDocumentBuilder();
			Document doc = builder.parse(path);
			doc.normalize();
			NodeList numbers = doc.getElementsByTagName("info");
			this.setNumbers(numbers);
			for (int i = 0; i < numbers.getLength(); i++) {
				Element link = (Element) numbers.item(i);
				map.put(link.getAttribute("value"), link.getTextContent());
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
	public void addXmlCode(String nodename, String nodevalue) {
		try {
			builder = factory.newDocumentBuilder();
			Document doc = builder.parse(path);
			doc.normalize();
			Text textseg;
			Element imag = doc.createElement("info");
			imag.setAttribute("value", nodename);
			textseg = doc.createTextNode(nodevalue);
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
	public void delXmlCode() {
		try {
			builder = factory.newDocumentBuilder();
			doc = builder.parse(path);
			doc.normalize();
			NodeList imags = doc.getElementsByTagName("info");
//System.out.println(imags.getLength());

//int len = xndList.Count;                                //定义节点总数
//for(int i = 0 ; i <  len; i ++)            //循环节点总数,保证能遍历节点
//{
// XmlElement xn = (XmlElement)xndList[0];
//   XmlNode x_ID = xn.SelectSingleNode("ID");        
//   if (x_ID.InnerText != FriendID) 
//  {
//     root.RemoveChild(xn);                        
//       
//   }
//   int xx = xndList.Count;                //在这里再次获取节点总数,防止节点泄漏
//  if (xx == 0) {                        //如果没有子节点则退出
//       break;
//  }
//}

	for (int i = 0; i <imags.getLength(); i++) {
		Element link = (Element) imags.item(i);
		doc.getFirstChild().removeChild(link);
		// System.out.println(link.getTextContent()+"--->"+link.getAttribute("value"));
	}
	
//			for (int i = 0; i < imags.getLength(); i++) {
//				Element link = (Element) imags.item(i);
//				doc.getFirstChild().removeChild(link);
//				// System.out.println(link.getTextContent()+"--->"+link.getAttribute("value"));
//			}

			// Element elink = (Element) imags.item(0);
			// elink.removeChild(elink.getElementsByTagName("imgsrc").item(0));
			// elink.removeChild(elink.getElementsByTagName("title").item(0));
			// elink.removeChild(elink.getElementsByTagName("url").item(0));
			// doc.getFirstChild().removeChild(elink);

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
