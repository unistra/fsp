package fr.unistra.di.pmo.fsp.aggregation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

import javax.mail.MessagingException;

import org.apache.xmlbeans.XmlException;

import com.taskadapter.redmineapi.bean.Project;

import fr.unistra.di.FileUtils;
import fr.unistra.di.pmo.fsp.MailSender;
import fr.unistra.di.pmo.fsp.Main;
import fr.unistra.di.pmo.fsp.exception.ParameterException;
import fr.unistra.di.pmo.fsp.historique.AlertType;
import fr.unistra.di.pmo.fsp.historique.HistoryType;
import fr.unistra.di.pmo.fsp.historique.ProjectHistoryType;
import fr.unistra.di.pmo.fsp.parametres.ParametersType;
import fr.unistra.di.pmo.fsp.project.ProjectPortfolio;

/**
 * Description of differential log for projects activity.
 * 
 * @author virgile
 */
public class Diff
{
	private static final String PLAIN_SUFFIX = ".txt"; //$NON-NLS-1$
	private static final String HTML_SUFFIX = ".html"; //$NON-NLS-1$

	private ParametersType pt;

	private HashMap<String, ArrayList<String>> elementsHtml;
	private HashMap<String, ArrayList<String>> elementsPlain;

	private String diffNamePrefix;

	private static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy"); //$NON-NLS-1$

	private final static int RED = 0;
	private final static int CHANGES = 1;
	private final static int NEWS = 2;

	/**
	 * Constructor.
	 * 
	 * @param pt parameters
	 */
	public Diff(ParametersType pt)
	{
		super();
		this.pt = pt;

		diffNamePrefix = pt.getOutputFolder();
		if (!diffNamePrefix.endsWith("/"))diffNamePrefix += "/"; //$NON-NLS-1$//$NON-NLS-2$
		diffNamePrefix += "diff-"; //$NON-NLS-1$
	}

	/**
	 * Adds an item in the activity
	 * 
	 * @param project project
	 * @param projectHistoryType history of project
	 */
	public void addItem(Project project, ProjectHistoryType projectHistoryType)
	{
		HistoryType oldHistoryType = null;
		HistoryType newHistoryType = null;

		HashMap<String, HistoryType> hts = new HashMap<String, HistoryType>();

		if ((projectHistoryType.getHistoryArray() != null) && (projectHistoryType.sizeOfHistoryArray() > 0))
		{
			GregorianCalendar firstDate = new GregorianCalendar();
			firstDate.setTimeInMillis(projectHistoryType.getHistoryArray(0).getDate().getTimeInMillis());

			for (HistoryType historyType : projectHistoryType.getHistoryArray())
			{
				oldHistoryType = newHistoryType;
				newHistoryType = historyType;

				hts.put(key(newHistoryType), newHistoryType);
				if ((oldHistoryType == null) || (!oldHistoryType.getWeather().toLowerCase().equals(newHistoryType.getWeather().toLowerCase())))
					addChange(project.getName(), oldHistoryType, newHistoryType);
				addNew(project.getName(), oldHistoryType, newHistoryType);
			}

			GregorianCalendar lastDate = new GregorianCalendar();
			HistoryType lastHistory = projectHistoryType.getHistoryArray(0);

			while (firstDate.before(lastDate))
			{
				String k = key(firstDate);
				if (hts.containsKey(k))
				{
					lastHistory = hts.get(k);
					Main.redmineLogger.debug("Existing history for week " + k + " : " + lastHistory.getWeather()); //$NON-NLS-1$ //$NON-NLS-2$
				} else
				{
					Main.redmineLogger.debug("Filling holes history for week " + k + " : " + lastHistory.getWeather()); //$NON-NLS-1$ //$NON-NLS-2$
				}
				if (lastHistory.getWeather().toLowerCase().equals(WikiAggregation.RED))
				{
					addRed(project.getName(), lastHistory, firstDate);
				}
				firstDate.add(Calendar.WEEK_OF_MONTH, 1);
			}

		}
	}

	/**
	 * Gets key from year and week number.
	 * @param gc date
	 * @return key
	 */
	public static String key(GregorianCalendar gc)
	{
		if (gc == null)
			return null;
		int w = gc.get(Calendar.WEEK_OF_YEAR);
		int y = gc.get(Calendar.YEAR);
		int m = gc.get(Calendar.MONTH);
		if ((w == 1) && (m == 11))
			y++;
		return y + "-" + w; //$NON-NLS-1$
	}

	private String key(HistoryType historyType)
	{
		if (historyType == null)
			return null;
		GregorianCalendar start = new GregorianCalendar();
		start.setTime(historyType.getDate().getTime());

		return key(start);
	}

	private void addRed(String projectName, HistoryType newHistoryType, GregorianCalendar date)
	{
		if (newHistoryType != null)
		{
			if (newHistoryType.getWeather().toLowerCase().equals(WikiAggregation.RED))
			{
				String html = "<li>" + projectName + "</li>\n"; //$NON-NLS-1$ //$NON-NLS-2$
				String plain = "  * " + projectName + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
				add(key(date), html, plain, RED);
			}
		}
	}

	private void addChange(String projectName, HistoryType oldHistoryType, HistoryType newHistoryType)
	{
		if (newHistoryType != null)
		{
			if ((oldHistoryType == null) || (!oldHistoryType.getWeather().toLowerCase().equals(newHistoryType.getWeather().toLowerCase())))
			{
				String html = "<li>" + projectName + " : "; //$NON-NLS-1$ //$NON-NLS-2$ 
				String plain = "  * " + projectName + " : "; //$NON-NLS-1$ //$NON-NLS-2$ 
				if (oldHistoryType == null)
				{
					html += " nouveau suivi "; //$NON-NLS-1$
				} else
				{
					html += oldHistoryType.getWeather() + " -> "; //$NON-NLS-1$
				}
				html += newHistoryType.getWeather() + "</li>\n"; //$NON-NLS-1$
				plain += newHistoryType.getWeather() + "\n"; //$NON-NLS-1$
				add(key(newHistoryType), html, plain, CHANGES);
			}
		}
	}

	private void addNew(String projectName, HistoryType oldHistoryType, HistoryType newHistoryType)
	{
		if (newHistoryType != null)
		{
			ArrayList<String> oldNewsId = new ArrayList<String>();
			if (oldHistoryType != null)
			{
				for (AlertType alertType : oldHistoryType.getAlertArray())
				{
					oldNewsId.add(alertType.getId());
				}
			}
			String html = ""; //$NON-NLS-1$
			String plain = ""; //$NON-NLS-1$
			for (AlertType alertType : newHistoryType.getAlertArray())
			{
				if (!oldNewsId.contains(alertType.getId()))
				{
					if (html.equals(""))html += "<h3>" + projectName + "</h3>\n"; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
					if (plain.equals(""))plain += "==== " + projectName + " ====\n"; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
					html += "<h4>" + sdf.format(alertType.getDate().getTime()) + " : " + alertType.getTitle() + "</h4>\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					String s = alertType.getDescription().replaceAll("<", "&lt;").replaceAll("\n", "<br/>");  //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$
					html += s + "\n"; //$NON-NLS-1$
					plain += "=== " + sdf.format(alertType.getDate().getTime()) + " : " + alertType.getTitle() + "===\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					plain += alertType.getDescription() + "\n"; //$NON-NLS-1$
				}
			}
			if (!html.equals(""))add(key(newHistoryType), html + "\n", plain + "\n", NEWS); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	private void add(String key, String htmlContent, String plainContent, int type)
	{
		if (elementsHtml == null)
		{
			elementsHtml = new HashMap<String, ArrayList<String>>();
			elementsPlain = new HashMap<String, ArrayList<String>>();
		}

		if (!elementsHtml.containsKey(key))
		{
			ArrayList<String> list = new ArrayList<String>();
			list.add("<h2>Projets en difficulté (rouge)</h2>\n<ul>\n"); //$NON-NLS-1$
			list.add("<h2>Changements de météo</h2>\n<ul>\n"); //$NON-NLS-1$
			list.add("<h2>Annonces</h2>\n"); //$NON-NLS-1$
			elementsHtml.put(key, list);

			list = new ArrayList<String>();
			list.add("===== Projets en difficulté (rouge) =====\n"); //$NON-NLS-1$
			list.add("===== Changements de météo =====\n"); //$NON-NLS-1$
			list.add("===== Annonces =====\n"); //$NON-NLS-1$
			elementsPlain.put(key, list);
		}

		ArrayList<String> list = elementsHtml.get(key);
		String actualContent = list.get(type);
		list.set(type, actualContent + htmlContent);
		elementsHtml.put(key, list);

		list = elementsPlain.get(key);
		actualContent = list.get(type);
		list.set(type, actualContent + plainContent);
		elementsPlain.put(key, list);
	}

	private void createDiffs() throws UnsupportedEncodingException, IOException
	{
		for (String key : elementsHtml.keySet())
		{
			ArrayList<String> list = elementsHtml.get(key);

			String content = "<h1>Activité projet semaine " + key + "</h1>\n"; //$NON-NLS-1$ //$NON-NLS-2$
			int i = 0;
			// List of projects
			for (String s : list)
			{
				content += s;
				if (i < 2)
					content += "</ul>"; //$NON-NLS-1$
				content += "\n\n"; //$NON-NLS-1$
				i++;
			}
			String diffName = diffNamePrefix + key + HTML_SUFFIX;

			File fd = new File(diffName);
			FileOutputStream fos = new FileOutputStream(fd);
			fos.write(content.getBytes("UTF-8")); //$NON-NLS-1$
			fos.close();
		}

		for (String key : elementsPlain.keySet())
		{
			ArrayList<String> list = elementsPlain.get(key);

			String content = "====== Activité projet semaine " + key + " ======\n"; //$NON-NLS-1$ //$NON-NLS-2$
			// List of projects
			for (String s : list)
			{
				content += s;
				content += "\n\n"; //$NON-NLS-1$
			}
			String diffName = diffNamePrefix + key + PLAIN_SUFFIX;

			File fd = new File(diffName);
			FileOutputStream fos = new FileOutputStream(fd);
			fos.write(content.getBytes("UTF-8")); //$NON-NLS-1$
			fos.close();
		}
	}

	/**
	 * Sends report if necessary (no report was sent last week).
	 * 
	 * @throws ParameterException exception
	 * @throws UnsupportedEncodingException exception
	 * @throws IOException exception
	 * @throws MessagingException exception
	 * @throws XmlException exception
	 */
	public void sendReport() throws ParameterException, UnsupportedEncodingException, IOException, MessagingException, XmlException
	{
		String htmlDiffName = diffNamePrefix + key(new GregorianCalendar()) + HTML_SUFFIX;
		File hfd = new File(htmlDiffName);
		
		boolean sendDiff = !hfd.exists();

		createDiffs();

		if (sendDiff)
		{
			// Weekly backup of portfolio
			ProjectPortfolio.backup(pt);
			
			// Prepare mail features
			MailSender ms = new MailSender(pt.getSend());

			if (!hfd.exists())
			{
				FileOutputStream fos = new FileOutputStream(hfd);
				fos.write("".getBytes("UTF-8")); //$NON-NLS-1$ //$NON-NLS-2$
				fos.close();
			}
			GregorianCalendar lastWeek = new GregorianCalendar();
			lastWeek.add(Calendar.WEEK_OF_YEAR, -1);
			String oldHtmlDiffName = diffNamePrefix + (key(lastWeek)) + HTML_SUFFIX;
			String oldPlainDiffName = diffNamePrefix + (key(lastWeek)) + PLAIN_SUFFIX;

			File oldHtml = new File(oldHtmlDiffName);
			File oldPlain = new File(oldPlainDiffName);
			if (oldHtml.exists())
			{
				String[] recipients = new String[pt.sizeOfReportRecipientArray()];
				for(int i=0; i<pt.sizeOfReportRecipientArray();i++)
				{
					recipients[i] = pt.getReportRecipientArray(i);
				}
				String htmlContent = FileUtils.read(oldHtml);
				String plainContent = FileUtils.read(oldPlain);
				ms.sendMail(recipients, "Activité projet semaine " + lastWeek.get(Calendar.WEEK_OF_YEAR), htmlContent, plainContent); //$NON-NLS-1$
			}
		}
	}

}
