package fr.unistra.di.pmo.fsp;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import javax.mail.MessagingException;

import org.apache.commons.codec.binary.Base64;
import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.common.TypeFactoryImpl;
import org.apache.xmlrpc.common.XmlRpcController;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.DateParser;
import org.apache.xmlrpc.parser.TypeParser;
import org.apache.xmlrpc.serializer.DateSerializer;
import org.apache.xmlrpc.serializer.TypeSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import fr.unistra.di.pmo.fsp.exception.ParameterException;
import fr.unistra.di.pmo.fsp.parametres.FSPType;
import fr.unistra.di.pmo.fsp.parametres.ParametersDocument;
import fr.unistra.di.pmo.fsp.parametres.ParametersType;
import fr.unistra.di.pmo.fsp.parametres.WikiOutputType;
import fr.unistra.di.pmo.fsp.project.Project;
import fr.unistra.di.pmo.fsp.project.ProjectList;

/**
 * Wiki interactions.
 * 
 * @author virgile
 */
public class XmlRpc
{
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd-HHmmssS"); //$NON-NLS-1$

	private String username;
	private String password;

	/**
	 * Constructor.
	 * 
	 * @param username authorized user name
	 * @param password authorized user password
	 * @throws ParameterException insufficient parameters for authentication
	 */
	public XmlRpc(String username, String password) throws ParameterException
	{
		if ((username == null) || (password == null))
			throw new ParameterException("Insufficient indentification parameters for Wiki actions"); //$NON-NLS-1$
		this.username = username;
		this.password = password;
	}

	/**
	 * Get project list from wiki.
	 * 
	 * @param doc parameters
	 * @return updated parameters
	 * @throws ParameterException error getting parameters
	 * @throws MalformedURLException malformed URL for XML RPC services
	 * @throws UnsupportedEncodingException bad encoding
	 */
	public static ParametersDocument getProjectList(ParametersDocument doc) throws ParameterException, MalformedURLException, UnsupportedEncodingException
	{
		if (doc == null)
			return null;
		// Get FSP main page
		ParametersType pt = doc.getParameters();
		WikiOutputType wot = null;
		if (pt.sizeOfWikiArray() > 0)
		{
			for (int i = 0; i < pt.sizeOfWikiArray(); i++)
			{
				WikiOutputType tmp = pt.getWikiArray(i);
				if (tmp.getName().equals("TousWiki")) //$NON-NLS-1$
				{
					wot = tmp;
					wot.setFspArray(null);
					i = pt.sizeOfWikiArray();
				}
			}
			if (wot == null)
				throw new ParameterException("Unable to find main FSP page"); //$NON-NLS-1$

			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			// Authentification
			config.setBasicUserName(wot.getUsername());
			config.setBasicPassword(wot.getPassword());
			config.setServerURL(new URL(wot.getXmlRpcService()));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);
			client.setTypeFactory((new XmlRpc(wot.getUsername(), wot.getPassword())).new CustomTypeFactory(client));

			pt.setFspArray(null);

			// Read Projects home page
			Vector<Object> params = new Vector<Object>();
			params.add("projets/start"); //$NON-NLS-1$
			String existingContent = null;
			Hashtable<String, String> h = new Hashtable<String, String>();
			try
			{
				existingContent = (String) client.execute("wiki.getPageHTML", params); //$NON-NLS-1$
				ByteArrayInputStream instream = new ByteArrayInputStream(existingContent.getBytes("iso-8859-1")); //$NON-NLS-1$
				Document document = (new Tidy()).parseDOM(instream, null);
				NodeList list = document.getElementsByTagName("table"); //$NON-NLS-1$
				ProjectList pl = new ProjectList();
				if (list.getLength() > 0)
				{
					if (list.item(0).getLocalName().equals("table")) //$NON-NLS-1$
						list = list.item(0).getChildNodes();
					for (int i = 0; i < list.getLength(); i++)
					{
						Node node = list.item(i);
						if (node.getLocalName().equals("tr")) //$NON-NLS-1$
						{
							if ((node.getChildNodes() != null) && (node.getChildNodes().getLength() > 0))
							{
								Node tdName = node.getChildNodes().item(0);
								String path = null;
								String title = null;
								if ((tdName.getChildNodes().getLength() > 0) && (tdName.getLocalName().equals("td"))) //$NON-NLS-1$
								{
									Node a = tdName.getChildNodes().item(0);
									if (a.getLocalName().equals("a")) //$NON-NLS-1$
									{
										if ((a.getAttributes().getLength() > 0) && (a.getAttributes().getNamedItem("title") != null)) //$NON-NLS-1$
										{
											path = a.getAttributes().getNamedItem("title").getNodeValue(); //$NON-NLS-1$
											title = a.getFirstChild().getNodeValue();
											String[] split = path.split(":"); //$NON-NLS-1$
											String sheetName = split[split.length - 2];
											FSPType ft = pt.addNewFsp();
											ft.setSheetName(sheetName);
											ft.setWikiPath(path);
											ft.setName(title);
											if (title != null)
												h.put(deaccent(title.toLowerCase()), sheetName);
										}
									}
								}
								// Project list comparison features
								Node tdPhase = node.getChildNodes().item(8);
								if (tdPhase.getLocalName().equals("td")) //$NON-NLS-1$
								{
									String phase = tdPhase.getFirstChild().getNodeValue();
									pl.addProject(new Project(title, path, phase));
								}
							}
						}
					}
					// Getting last project list
					ProjectList old = new ProjectList();
					String path = pt.getOutputFolder();
					if (!path.endsWith(File.separator))
						path += File.separator;
					path += "existingProjects.xml"; //$NON-NLS-1$
					old.load(path);
					// Compare to old list
					String diff = old.compare(pl);
					// Send diff by mail
					if (diff != null)
					{
						MailSender ms = new MailSender(pt.getSend());
						String text = diff;
						String[] recipients = new String[1];
						recipients[0] = pt.getReportRecipient();
						ms.sendMail(recipients, "Changements dans la liste des projets", text, null); //$NON-NLS-1$
					}
					// Save new list
					pl.save(path);
				}
			} catch (XmlRpcException e)
			{
				e.printStackTrace();
				System.out.println("Unable to get existing content"); //$NON-NLS-1$
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (XmlException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MessagingException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// Alphabetical sort
			Vector<String> v = new Vector<String>(h.keySet());
			Collections.sort(v);
			for (String string : v)
			{
				wot.addFsp(h.get(string));
			}
		} else
		{
			throw new ParameterException("Unable to find main FSP page"); //$NON-NLS-1$
		}
		return doc;
	}

	private static String deaccent(String s)
	{
		s = s.replaceAll("[\u00E8\u00E9\u00EA\u00EB]", "e"); //$NON-NLS-1$ //$NON-NLS-2$
		s = s.replaceAll("[\u00FA\u00FB\u00FC\u00F9]", "u"); //$NON-NLS-1$ //$NON-NLS-2$
		s = s.replaceAll("[\u00EC\u00ED\u00EE\u00EF]", "i"); //$NON-NLS-1$ //$NON-NLS-2$
		s = s.replaceAll("[\u00E0\u00E1\u00E2\u00E3\u00E4\u00E5]", "a"); //$NON-NLS-1$ //$NON-NLS-2$
		s = s.replaceAll("[\u00F2\u00F3\u00F4\u00F5\u00F6]", "o"); //$NON-NLS-1$ //$NON-NLS-2$
		s = s.replaceAll("\u00E7", "c"); //$NON-NLS-1$ //$NON-NLS-2$

		s = s.replaceAll("[\u00C8\u00C9\u00CA\u00CB]", "E"); //$NON-NLS-1$ //$NON-NLS-2$
		s = s.replaceAll("[\u00DA\u00DB\u00DC\u00D9]", "U"); //$NON-NLS-1$ //$NON-NLS-2$
		s = s.replaceAll("[\u00CC\u00CD\u00CE\u00CF]", "I"); //$NON-NLS-1$ //$NON-NLS-2$
		s = s.replaceAll("[\u00C0\u00C1\u00C2\u00C3\u00C4\u00C5]", "A"); //$NON-NLS-1$ //$NON-NLS-2$
		s = s.replaceAll("[\u00D2\u00D3\u00D4\u00D5\u00D6]", "O"); //$NON-NLS-1$ //$NON-NLS-2$

		return s;
	}

	/**
	 * Get existing content, parse result and update FSP content in wiki page.
	 * 
	 * @param xmlRpcService address of XML RPC service
	 * @param wikiPath path of page to update in wiki
	 * @param filePath source file with FSP contents
	 * @param attachmentWikiPath wiki path for attached file (optional)
	 * @param attachmentPath local file path of attached file (optional)
	 * @throws ParameterException problem with parameters
	 * @throws IOException problem during upload of attachment
	 * @throws XmlRpcException communication error with wiki
	 */
	public void write(String xmlRpcService, String wikiPath, String filePath, String attachmentWikiPath, String attachmentPath) throws ParameterException, IOException, XmlRpcException
	{
		if ((wikiPath == null) || (filePath == null))
			throw new ParameterException("Unable to find elements to write to wiki " + wikiPath + " " + filePath); //$NON-NLS-1$ //$NON-NLS-2$
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		// Authentification
		config.setBasicUserName(username);
		config.setBasicPassword(password);
		config.setServerURL(new URL(xmlRpcService));
		XmlRpcClient client = new XmlRpcClient();
		client.setConfig(config);
		client.setTypeFactory(new CustomTypeFactory(client));

		// Read file
		StringBuilder contents = new StringBuilder();
		String contentString = null;

		try
		{
			BufferedReader input = new BufferedReader(new FileReader(new File(filePath)));
			String line = null;
			while ((line = input.readLine()) != null)
			{
				contents.append(line);
				contents.append(System.getProperty("line.separator")); //$NON-NLS-1$
			}
			contentString = contents.toString();
			if ((attachmentPath != null) && (attachmentWikiPath != null))
			{
				contentString += "\n====Version tableur====\n{{:" + attachmentWikiPath + "}}\n"; //$NON-NLS-1$//$NON-NLS-2$
			}
			input.close();

		} catch (IOException ex)
		{
			ex.printStackTrace();
		}

		String existingContent;

		existingContent = getPage(client, wikiPath);
		String newContent = null;

		// Only update if some content exists
		if ((existingContent != null) && (!existingContent.trim().equals(""))) //$NON-NLS-1$
		{
			// Update content
			String title = "===== Suivi ====="; //$NON-NLS-1$
			int startIndex = existingContent.indexOf(title);
			if (startIndex >= 0)
			{
				String start = existingContent.substring(0, startIndex);
				int endIndex = existingContent.indexOf("=====", startIndex + title.length()); //$NON-NLS-1$
				String end = ""; //$NON-NLS-1$
				if (endIndex >= 0)
					end = existingContent.substring(endIndex);
				newContent = start + contentString + end;
			}
		}

		// Put page only if update
		if ((newContent != null) && (!newContent.equals(existingContent)) && (!Main.isSimulation()))
		{
			try
			{
				putPage(client, wikiPath, newContent);
			} catch (SAXParseException e)
			{
				e.printStackTrace();
			} catch (XmlRpcException e)
			{
				StringWriter result = new StringWriter();
				PrintWriter printWriter = new PrintWriter(result);
				e.printStackTrace(printWriter);
				String text = result.toString();
				if ((text == null) || (!text.contains("Failed to parse server's response: The processing instruction target matching")))throw e; //$NON-NLS-1$
			} finally
			{
				if ((attachmentPath != null) && (attachmentWikiPath != null))
				{
					putAttachment(client, attachmentWikiPath, attachmentPath);
				}
			}

		}

	}

	@SuppressWarnings("unused")
	private void putPage(XmlRpcClient client, String pageName, String content) throws XmlRpcException, SAXParseException
	{
		if ((client != null) && (pageName != null) && (content != null))
		{
			// Change logs
			HashMap<String, String> attrs = new HashMap<String, String>();
			attrs.put("sum", "Script FSP " + sdf.format(new Date())); //$NON-NLS-1$ //$NON-NLS-2$

			// Parameters
			Vector<Object> params = new Vector<Object>();
			params.add(pageName);
			params.add(content);
			params.add(attrs);
			client.execute("wiki.putPage", params); //$NON-NLS-1$
		}
	}

	private String getPage(XmlRpcClient client, String pageName) throws XmlRpcException
	{
		if ((client == null) || (pageName == null))
			return null;
		// Get existing page content
		Vector<Object> params = new Vector<Object>();
		params.add(pageName);
		String existingContent = null;
		existingContent = (String) client.execute("wiki.getPage", params); //$NON-NLS-1$
		return existingContent;
	}

	// private String getHtmlPage(XmlRpcClient client, String pageName) throws
	// XmlRpcException
	// {
	// if ((client == null) || (pageName == null))
	// return null;
	// // Get existing page content
	// Vector<Object> params = new Vector<Object>();
	// params.add(pageName);
	// String existingContent = null;
	//		existingContent = (String) client.execute("wiki.getPageHTML", params); //$NON-NLS-1$
	// return existingContent;
	// }

	private void putAttachment(XmlRpcClient client, String wikiPath, String fileName) throws IOException, XmlRpcException
	{
		if ((client != null) && (wikiPath != null) && (fileName != null))
		{
			File file = new File(fileName);
			FileInputStream fis = new FileInputStream(file);
			byte fileContent[] = new byte[(int) file.length()];
			fis.read(fileContent);
			fileContent = Base64.encodeBase64(fileContent);

			Vector<Object> params = new Vector<Object>();
			params.add(wikiPath);
			params.add(fileContent);
			HashMap<String, String> attrs2 = new HashMap<String, String>();
			// Option to overwrite existing file
			attrs2.put("ow", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			params.add(attrs2);
			client.execute("wiki.putAttachment", params); //$NON-NLS-1$
		}
	}

	/**
	 * Apache XmlRpc only support one of ISO 8601 form. Let's define a date
	 * format complying with dokuwiki output.
	 * 
	 * @author virgile
	 */
	public class CustomTypeFactory extends TypeFactoryImpl
	{
		/**
		 * Constructor.
		 * 
		 * @param pController ( @see {@link TypeFactoryImpl.#TypeFactoryImpl(XmlRpcController)}
		 */
		public CustomTypeFactory(XmlRpcController pController)
		{
			super(pController);
		}

		private DateFormat newFormat()
		{
			// Yeah baby, it's the good format
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); //$NON-NLS-1$
		}

		/**
		 * Date parser.
		 */
		@Override
		public TypeParser getParser(XmlRpcStreamConfig pConfig, NamespaceContextImpl pContext, String pURI, String pLocalName)
		{
			if (DateSerializer.DATE_TAG.equals(pLocalName))
			{
				return new DateParser(newFormat());
			}
			return super.getParser(pConfig, pContext, pURI, pLocalName);
		}

		/**
		 * Serializer.
		 */
		@Override
		public TypeSerializer getSerializer(XmlRpcStreamConfig pConfig, Object pObject) throws SAXException
		{
			if (pObject instanceof Date)
			{
				return new DateSerializer(newFormat());
			}
			return super.getSerializer(pConfig, pObject);
		}
	}

}
