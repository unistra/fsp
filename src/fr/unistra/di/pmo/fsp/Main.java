package fr.unistra.di.pmo.fsp;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Hashtable;

import javax.mail.MessagingException;

import org.apache.xmlbeans.XmlException;

import fr.unistra.di.pmo.fsp.aggregation.DataAggregation;
import fr.unistra.di.pmo.fsp.aggregation.OdtAggregation;
import fr.unistra.di.pmo.fsp.aggregation.WikiAggregation;
import fr.unistra.di.pmo.fsp.exception.ParameterException;
import fr.unistra.di.pmo.fsp.parametres.AggregateType;
import fr.unistra.di.pmo.fsp.parametres.OpenDocumentOutputType;
import fr.unistra.di.pmo.fsp.parametres.ParametersDocument;
import fr.unistra.di.pmo.fsp.parametres.ParametersType;
import fr.unistra.di.pmo.fsp.parametres.WikiOutputType;

/**
 * Main class.
 * 
 * @author virgile
 */
public class Main
{
	private static boolean simulation = false;

	/**
	 * Launch FSP operations.
	 * 
	 * @param args command line parameters
	 * @throws ParameterException error getting parameters
	 * @throws IOException error reading files
	 * @throws XmlException error during Xml operations
	 * @throws MessagingException error during mail fetching
	 */
	public static void main(String[] args) throws ParameterException, XmlException, MessagingException, IOException
	{
		if (args == null)
			throw new ParameterException("No file parameters provided"); //$NON-NLS-1$
		File f = new File(args[0]);
		if ((!f.exists()) || (!f.canRead()))
			throw new ParameterException("Inexistant or unreadeable file"); //$NON-NLS-1$ 
		ParametersDocument doc = ParametersDocument.Factory.parse(f);

		// Get project list from wiki
		doc = XmlRpc.getProjectList(doc);

		ParametersType pt = doc.getParameters();
		if (pt.isSetSimulation())
			simulation = pt.getSimulation();

		// Fetch mails
		FetchMail fm = new FetchMail(pt);
		Hashtable<String, File> fsps = fm.retrieve(false);
		Sheets sheets = new Sheets(pt, fsps);
		
		// Aggregates treatment
		if (pt.sizeOfOdsArray() > 0)
		{
			aggregate(pt, pt.getOdsArray(), sheets);
		}
		if (pt.sizeOfWikiArray() > 0)
		{
			aggregate(pt, pt.getWikiArray(), sheets);
		}
		// delete temporary files
		if ((fsps != null) && (fsps.size() > 0))
		{
			File file = fsps.elements().nextElement();
			File parent = file.getParentFile();
			for (File tmpFile : fsps.values())
			{
				tmpFile.delete();
			}
			parent.delete();
		}
	}

	private static void aggregate(ParametersType pt, AggregateType[] ag, Sheets sheets)
	{
		for (AggregateType at : ag)
		{
			try
			{
				DataAggregation da = null;
				// Wiki
				if (at instanceof WikiOutputType)
				{
					WikiOutputType wot = (WikiOutputType) at;
					da = new WikiAggregation(pt, sheets);
					String filePath = da.aggregateSheet(at, pt.getOutputFolder());

					// Put ODT in wiki
					da = new OdtAggregation(pt, sheets);
					String odt = da.aggregateSheet(at, pt.getOutputFolder());

					XmlRpc xr = new XmlRpc(wot.getUsername(), wot.getPassword());
					String attachmentWikiPath = null;
					if (wot.isSetWikiAttachmentPath())
					{
						attachmentWikiPath = wot.getWikiAttachmentPath();
					}
					xr.write(wot.getXmlRpcService(), wot.getWikiPath(), filePath, attachmentWikiPath, odt);
				}
				// ODT
				if (at instanceof OpenDocumentOutputType)
				{
					OpenDocumentOutputType odot = (OpenDocumentOutputType) at;
					da = new OdtAggregation(pt, sheets);
					String attachment = da.aggregateSheet(at, pt.getOutputFolder());
					MailSender ms = new MailSender(pt.getSend());
					String text = "FSP : " + at.getName() + "\nDélai de prise en compte : " + at.getDelay() + " jours\n\nProjets :\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					for (String fsp : at.getFspArray())
					{
						text += "\t" + fsp + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
					}
					if (!isSimulation())
						ms.sendMail(odot.getRecipientArray(), "Bureau des projets : " + at.getName(), text, attachment); //$NON-NLS-1$
				}
			} catch (Exception e)
			{
				e.printStackTrace();
				try
				{
					MailSender ms = new MailSender(pt.getSend());
					StringWriter result = new StringWriter();
					PrintWriter printWriter = new PrintWriter(result);
					e.printStackTrace(printWriter);
					String text = result.toString();
					String[] recipients = new String[1];
					recipients[0] = "do_not_reply@unistra.fr"; //$NON-NLS-1$
					ms.sendMail(recipients, "Exception in FSP", text, null); //$NON-NLS-1$
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
	 * Get the value.
	 * 
	 * @return the simulation
	 */
	public static boolean isSimulation()
	{
		return simulation;
	}
}
