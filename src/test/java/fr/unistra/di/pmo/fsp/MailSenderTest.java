package fr.unistra.di.pmo.fsp;

import static org.junit.Assert.*;

import java.io.File;

import javax.mail.MessagingException;

import org.junit.Before;
import org.junit.Test;

import fr.unistra.di.pmo.fsp.exception.ParameterException;
import fr.unistra.di.pmo.fsp.parametres.ParametersDocument;
import fr.unistra.di.pmo.fsp.parametres.ParametersType;

/**
 * Tests for mail features.
 * 
 * @author virgile
 */
public class MailSenderTest
{
	private ParametersType pt;

	/**
	 * Tests setup.
	 * 
	 * @throws Exception exception
	 */
	@Before
	public void setUp() throws Exception
	{
		String s = System.getProperty("config"); //$NON-NLS-1$
		File f = new File(s);
		ParametersDocument doc = ParametersDocument.Factory.parse(f);
		pt = doc.getParameters();
	}

	/**
	 * Test.
	 * 
	 * @throws ParameterException exception
	 */
	@Test
	public void testMailSender() throws ParameterException
	{
		MailSender ms = new MailSender(pt.getSend());
		assertNotNull(ms);
	}

	/**
	 * Test.
	 */
	@Test
	public void testSendMailStringArrayStringString()
	{
		MailSender ms;
		try
		{
			ms = new MailSender(pt.getSend());
			assertNotNull(ms);
			String[] recipients =
			{ pt.getErrorRecipient() };
			try
			{
				ms.sendMail(recipients, "Standard - Accents \u00E9\u00E8\u00E0\u00F9\u00F4 - Activit\u00E9 projet semaine", "Accents \u00E9\u00E8\u00E0\u00F9\u00F4"); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (MessagingException e)
			{
				fail(e.getMessage());
			} catch (ParameterException e)
			{
				fail(e.getMessage());
			}
		} catch (ParameterException e1)
		{
			fail(e1.getMessage());
		}

	}

	/**
	 * Test.
	 */
	@Test
	public void testSendMailStringArrayStringStringString()
	{
		MailSender ms;
		try
		{
			ms = new MailSender(pt.getSend());
			assertNotNull(ms);
			String[] recipients =
			{ pt.getErrorRecipient() };
			try
			{
				ms.sendMail(recipients, "HTML - Accents \u00E9\u00E8\u00E0\u00F9\u00F4 - Activit\u00E9 projet semaine", "<h1>Accents \u00E9\u00E8\u00E0\u00F9\u00F4</h1>", "Accents \u00E9\u00E8\u00E0\u00F9\u00F4"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			} catch (MessagingException e)
			{
				fail(e.getMessage());
			} catch (ParameterException e)
			{
				fail(e.getMessage());
			}
		} catch (ParameterException e1)
		{
			fail(e1.getMessage());
		}

	}

}
