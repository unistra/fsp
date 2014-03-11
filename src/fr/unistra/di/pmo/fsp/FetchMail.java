package fr.unistra.di.pmo.fsp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Flags.Flag;
import javax.mail.internet.MimeMultipart;

import fr.unistra.di.pmo.fsp.exception.ParameterException;
import fr.unistra.di.pmo.fsp.parametres.MailSourceType;
import fr.unistra.di.pmo.fsp.parametres.ParametersType;

/**
 * Fetch attached files from mailbox.
 * 
 * @author virgile
 */
public class FetchMail
{
	private MailSourceType mailSource;
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd-HHmmssS"); //$NON-NLS-1$
	
	private String outputPath;
	private String host;
	private String username;
	private String password;
	private String type;

	/**
	 * Constructor.
	 * 
	 * @param parameters mailbox parameters
	 * @throws ParameterException if any problem using application parameters
	 */
	public FetchMail(ParametersType parameters) throws ParameterException
	{
		// Parameters verification
		if ((parameters == null) || (parameters.getSource() == null))
			throw new ParameterException("Empty parameters"); //$NON-NLS-1$
		mailSource = parameters.getSource();
		if (mailSource.getHost() == null)
			throw new ParameterException("Undefined server"); //$NON-NLS-1$
		host = mailSource.getHost();
		if (mailSource.getUsername() == null)
			throw new ParameterException("Undefined username"); //$NON-NLS-1$
		username = mailSource.getUsername();
		if (mailSource.getPassword() == null)
			throw new ParameterException("Undefined password"); //$NON-NLS-1$
		password = mailSource.getPassword();
		if (mailSource.getType() == null)
			throw new ParameterException("Undefined type"); //$NON-NLS-1$
		type = mailSource.getType().toString();

		// Output folder verifications
		if (parameters.getOutputFolder() == null)
			throw new ParameterException("Undefined output folder"); //$NON-NLS-1$
		outputPath = parameters.getOutputFolder();
		File f = new File(outputPath);
		if (!f.exists())
			throw new ParameterException("Inexistant output folder"); //$NON-NLS-1$
		if (!f.canRead())
			throw new ParameterException("Unreadeable output folder"); //$NON-NLS-1$
		if (!f.canWrite())
			throw new ParameterException("Unwritable output folder"); //$NON-NLS-1$
		if (!outputPath.endsWith(File.separator))
			outputPath += File.separator;
		outputPath += sdf.format(new Date());
	}

	/**
	 * Open box, retrieve all messages and save attached files.
	 * 
	 * @param flush if true, read messages are deleted
	 * @return list of written files with filename used as key
	 * @throws MessagingException problem during mail access
	 * @throws IOException saving attachments problems
	 */
	public Hashtable<String, File> retrieve(boolean flush) throws MessagingException, IOException
	{
		// Create empty properties
		Properties props = new Properties();

		// Get session
		Session session = Session.getDefaultInstance(props, null);

		// Default inbox folder
		String folderName = "INBOX"; //$NON-NLS-1$
		if (mailSource.getFolder() != null)
		{
			folderName = mailSource.getFolder();
		}

		// Get the store
		Store store = session.getStore(type);

		// Connect to store
		System.out.println("Connecting to " + host); //$NON-NLS-1$
		store.connect(host, username, password);

		// Get folder
		Folder folder = store.getFolder(folderName);

		if (flush)
		{
			// Write access
			folder.open(Folder.READ_WRITE);
		} else
		{
			// Open read-only
			folder.open(Folder.READ_ONLY);
		}

		// Get directory
		Message message[] = folder.getMessages();

		// Results
		Hashtable<String, File> files = new Hashtable<String, File>();

		if ((message != null) && (message.length > 0))
		{
			System.out.println("Found " + message.length + " messages"); //$NON-NLS-1$//$NON-NLS-2$

			// Creation du repertoire de sortie
			(new File(outputPath)).mkdir();
			System.out.println("Output path : " + outputPath); //$NON-NLS-1$

			// Messages loop
			for (int i = 0, n = message.length; i < n; i++)
			{
				Message m = message[i];
				// Only messages with attachments
				files.putAll(findNestedAttachments(m.getContent(), "" + i)); //$NON-NLS-1$
				// Delete message
				if (flush)
					m.setFlag(Flag.DELETED, true);
			}
		} else
			System.out.println("No message found"); //$NON-NLS-1$

		// Delete messages
		if (flush)
			folder.expunge();
		// Close connection
		folder.close(false);
		store.close();

		System.out.println(files.size() + " file(s) written"); //$NON-NLS-1$

		return files;
	}

	// Recursive attachments method
	// Useful for forwarded messages
	private Hashtable<String, File> findNestedAttachments(Object content, String prefixe) throws FileNotFoundException, MessagingException, IOException
	{
		Hashtable<String, File> files = new Hashtable<String, File>();
		if (content instanceof MimeMultipart)
		{
			MimeMultipart mm = (MimeMultipart) content;
			// Parts loop
			for (int j = 0, o = mm.getCount(); j < o; j++)
			{
				Part part = mm.getBodyPart(j);

				// If nested message
				if (part.getContent() instanceof MimeMultipart)
				{
					MimeMultipart partContent = (MimeMultipart) part.getContent();
					files.putAll(findNestedAttachments(partContent, (prefixe + "-" + j))); //$NON-NLS-1$
				}

				else
				{
					String disposition = part.getDisposition();

					if (((part.getFileName() != null) && (part.getFileName().toLowerCase().endsWith("ods"))) && (disposition != null) && ((disposition.equals(Part.ATTACHMENT) || (disposition.equals(Part.INLINE))))) //$NON-NLS-1$
					{
						// Write attached file with unique ID
						String fileName = outputPath + File.separator + prefixe + "-" + part.getFileName(); //$NON-NLS-1$
						File f = new File(fileName);
						FileOutputStream fos = new FileOutputStream(f);
						InputStream inputStream = part.getInputStream();
						byte buf[] = new byte[1024];
						int len;
						while ((len = inputStream.read(buf)) > 0)
						{
							fos.write(buf, 0, len);
						}
						fos.close();
						inputStream.close();
						files.put(fileName, f);
					}
				}
			}
		}
		return files;
	}

	/**
	 * @return the outputPath
	 */
	public String getOutputPath()
	{
		return outputPath;
	}

	/**
	 * @param outputPath the outputPath to set
	 */
	public void setOutputPath(String outputPath)
	{
		this.outputPath = outputPath;
	}
}
