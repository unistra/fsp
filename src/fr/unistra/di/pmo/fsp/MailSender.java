package fr.unistra.di.pmo.fsp;

import java.io.File;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import fr.unistra.di.pmo.fsp.exception.ParameterException;
import fr.unistra.di.pmo.fsp.parametres.MailSenderType;

/**
 * Utilities to send mails.
 * 
 * @author virgile
 */
public class MailSender
{
	private String host;
	private String username;
	private String password;
	private String port;
	private String sender;

	/**
	 * Constructor.
	 * 
	 * @param mst mail sender parameters
	 * @throws ParameterException if any parameter is missing
	 */
	public MailSender(MailSenderType mst) throws ParameterException
	{
		if (mst == null)
			throw new ParameterException("No smtp parameters provided"); //$NON-NLS-1$
		if (mst.getHost() == null)
			throw new ParameterException("No smtp host provided"); //$NON-NLS-1$
		host = mst.getHost();
		if (mst.getPassword() == null)
			throw new ParameterException("No smtp password provided"); //$NON-NLS-1$
		password = mst.getPassword();
		if (mst.getPort() == null)
			throw new ParameterException("No smtp port provided"); //$NON-NLS-1$
		port = mst.getPort();
		if (mst.getSender() == null)
			throw new ParameterException("No smtp sender provided"); //$NON-NLS-1$
		sender = mst.getSender();
		if (mst.getUsername() == null)
			throw new ParameterException("No smtp username provided"); //$NON-NLS-1$
		username = mst.getUsername();
	}

	/**
	 * Send a mail.
	 * 
	 * @param emailList recipients list
	 * @param subject mail subject
	 * @param text text content
	 * @throws MessagingException error during mail creation
	 * @throws ParameterException parameters error
	 */
	@SuppressWarnings("synthetic-access")
	public void sendMail(String[] emailList, String subject, String text) throws MessagingException, ParameterException
	{
		if (emailList == null)
			throw new ParameterException("No recipients found"); //$NON-NLS-1$
		boolean debug = false;

		// Define properties
		Properties props = new Properties();

		props.put("mail.smtp.host", host); //$NON-NLS-1$
		props.put("mail.smtp.auth", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		props.put("mail.debug", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		props.put("mail.smtp.user", username); //$NON-NLS-1$
		props.put("mail.smtp.password", password); //$NON-NLS-1$
		props.put("mail.smtp.port", port); //$NON-NLS-1$
		props.put("mail.smtp.starttls.enable", "true"); //$NON-NLS-1$ //$NON-NLS-2$

		Authenticator auth = new SMTPAuthenticator();

		System.getSecurityManager();

		Session session = Session.getInstance(props, auth);

		session.setDebug(debug);

		// Creating message
		Message msg = new MimeMessage(session);

		InternetAddress addressFrom = new InternetAddress(sender);
		msg.setFrom(addressFrom);

		// Recipients
		InternetAddress[] addressTo = new InternetAddress[emailList.length];

		for (int i = 0; i < emailList.length; i++)
		{
			addressTo[i] = new InternetAddress(emailList[i]);
		}

		msg.setRecipients(Message.RecipientType.TO, addressTo);

		// Subject
		msg.setSubject(subject);
		msg.setContent(text, "text/html; charset=UTF-8"); //$NON-NLS-1$
		msg.setHeader("Content-Type", "text/html; charset=UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$
		msg.saveChanges();

		Transport.send(msg);

	}
	
	/**
	 * Send a mail.
	 * 
	 * @param emailList recipients list
	 * @param subject mail subject
	 * @param text text content
	 * @throws MessagingException error during mail creation
	 * @throws ParameterException parameters error
	 */
	@SuppressWarnings("synthetic-access")
	public void sendMail(String[] emailList, String subject, String htmlText, String plainText) throws MessagingException, ParameterException
	{
		if (emailList == null)
			throw new ParameterException("No recipients found"); //$NON-NLS-1$
		boolean debug = false;

		// Define properties
		Properties props = new Properties();

		props.put("mail.smtp.host", host); //$NON-NLS-1$
		props.put("mail.smtp.auth", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		props.put("mail.debug", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		props.put("mail.smtp.user", username); //$NON-NLS-1$
		props.put("mail.smtp.password", password); //$NON-NLS-1$
		props.put("mail.smtp.port", port); //$NON-NLS-1$
		props.put("mail.smtp.starttls.enable", "true"); //$NON-NLS-1$ //$NON-NLS-2$

		Authenticator auth = new SMTPAuthenticator();

		System.getSecurityManager();

		Session session = Session.getInstance(props, auth);

		session.setDebug(debug);

		// Creating message
		Message msg = new MimeMessage(session);

		InternetAddress addressFrom = new InternetAddress(sender);
		msg.setFrom(addressFrom);

		// Recipients
		InternetAddress[] addressTo = new InternetAddress[emailList.length];

		for (int i = 0; i < emailList.length; i++)
		{
			addressTo[i] = new InternetAddress(emailList[i]);
		}

		msg.setRecipients(Message.RecipientType.TO, addressTo);
		
		msg.setSubject(subject);
		
		// HTML
		MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlText, "text/html; charset=UTF-8"); //$NON-NLS-1$
		// Plain
		MimeBodyPart textPart = new MimeBodyPart();
        textPart.setContent(plainText, "text/plain; charset=UTF-8"); //$NON-NLS-1$

        Multipart mp = new MimeMultipart("alternative"); //$NON-NLS-1$
        mp.addBodyPart(textPart);
        mp.addBodyPart(htmlPart);
        
        msg.setContent(mp);

		Transport.send(msg);

	}

	private class SMTPAuthenticator extends javax.mail.Authenticator
	{
		@SuppressWarnings("synthetic-access")
		@Override
		public PasswordAuthentication getPasswordAuthentication()
		{
			return new PasswordAuthentication(username, password);
		}
	}
}
