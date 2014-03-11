package fr.unistra.di.pmo.fsp;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.xmlbeans.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.bean.Project;

import fr.unistra.di.pmo.fsp.aggregation.Diff;
import fr.unistra.di.pmo.fsp.aggregation.WikiAggregation;
import fr.unistra.di.pmo.fsp.exception.ParameterException;
import fr.unistra.di.pmo.fsp.parametres.ParametersDocument;
import fr.unistra.di.pmo.fsp.parametres.ParametersType;
import fr.unistra.di.pmo.fsp.parametres.WikiOutputType;
import fr.unistra.di.pmo.fsp.project.WikiItem;
import fr.unistra.di.pmo.fsp.redmine.Connection;

/**
 * Main class.
 * 
 * @author virgile
 */
public class Main
{
	/**
	 * Log4j element for Redmine related elements.
	 */
	public static Logger redmineLogger = LoggerFactory.getLogger("Redmine"); //$NON-NLS-1$
	/**
	 * Log4j element for Dokuwiki related elements.
	 */
	public static Logger wikiLogger = LoggerFactory.getLogger("Wiki"); //$NON-NLS-1$
	public static Diff diff;
	private static boolean simulation = false;

	/**
	 * Launch FSP operations.
	 * 
	 * @param args command line parameters
	 * @throws ParameterException error getting parameters
	 * @throws IOException error reading files
	 * @throws XmlException error during Xml operations
	 * @throws MessagingException error during mail fetching
	 * @throws RedmineException error with redmine
	 */
	public static void main(String[] args) throws ParameterException, XmlException, MessagingException, IOException, RedmineException
	{
		if (args == null)
			throw new ParameterException("No file parameters provided"); //$NON-NLS-1$
		File f = new File(args[0]);
		if ((!f.exists()) || (!f.canRead()))
			throw new ParameterException("Inexistant or unreadeable file"); //$NON-NLS-1$ 
		ParametersDocument doc = ParametersDocument.Factory.parse(f);

		ParametersType pt = doc.getParameters();

		// Get project list from wiki
				
		if (pt.isSetSimulation())
			simulation = pt.getSimulation();

		// Check diff
		diff = new Diff(pt);

		XmlRpc xmlRpc = WikiConnection.getXmlRpc(pt);
		HashMap<String, WikiItem> fsps = xmlRpc.getProjectList(doc.getParameters());

		// Initialize redmine connection
		Connection.init(pt.getSource());

		// Get project list from redmine
		List<Project> pl = Connection.getInstance().getProjects();
		HashMap<String, Project> redmineProjects = new HashMap<String, Project>();
		redmineLogger.info("Found " + pl.size() + " projects"); //$NON-NLS-1$//$NON-NLS-2$
		for (Project project : pl)
		{
			redmineProjects.put(project.getIdentifier(), project);
		}

		// Aggregates treatment
		if (pt.sizeOfWikiOutputArray() > 0)
		{
			aggregate(pt, fsps, redmineProjects);
		}
		
		diff.sendReport();


	}

	private static void aggregate(ParametersType pt, HashMap<String, WikiItem> fsps, HashMap<String, Project> redmineProjects)
	{
		for (WikiOutputType at : pt.getWikiOutputArray())
		{
			try
			{
				WikiAggregation da = new WikiAggregation(pt, fsps, redmineProjects);
				String filePath = da.aggregateSheet(at, pt.getOutputFolder());

				XmlRpc xr = WikiConnection.getXmlRpc(pt);
				xr.write(at.getWikiPath(), filePath);
			} catch (Exception e)
			{
				e.printStackTrace();
				try
				{
					errorMail(pt, e, null);
				} catch (ParameterException e1)
				{
					e1.printStackTrace();
				} catch (MessagingException e1)
				{
					e1.printStackTrace();
				}

			}
		}
	}

	/**
	 * Sends mail if an exception is raised
	 * 
	 * @param pt parameters
	 * @param e raised exception
	 * @param contextElements elements allowing exception investigation
	 * @throws ParameterException exception
	 * @throws MessagingException exception
	 */
	public static void errorMail(ParametersType pt, Exception e, HashMap<String, String> contextElements) throws ParameterException, MessagingException
	{
		MailSender ms = new MailSender(pt.getSend());
		StringWriter result = new StringWriter();
		PrintWriter printWriter = new PrintWriter(result);
		e.printStackTrace(printWriter);
		String text = result.toString();
		if (contextElements != null)
		{
			for (String key : contextElements.keySet())
			{
				text = key + " : " + contextElements.get(key) + "\n" + text; //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		String[] recipients = new String[1];
		recipients[0] = pt.getErrorRecipient();
		ms.sendMail(recipients, "Exception in FSP", text); //$NON-NLS-1$
	}

	/**
	 * Get the value.
	 * 
	 * @return the simulation
	 */
	public static boolean isSimulation()
	{
		return simulation;
	}
}
