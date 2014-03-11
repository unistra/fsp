package fr.unistra.di.pmo.fsp.aggregation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import javax.mail.MessagingException;

import org.apache.xmlbeans.XmlOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.taskadapter.redmineapi.NotAuthorizedException;
import com.taskadapter.redmineapi.bean.CustomField;
import com.taskadapter.redmineapi.bean.Issue;
import com.taskadapter.redmineapi.bean.News;
import com.taskadapter.redmineapi.bean.Project;
import com.taskadapter.redmineapi.bean.TimeEntry;

import fr.unistra.di.pmo.fsp.Main;
import fr.unistra.di.pmo.fsp.exception.ParameterException;
import fr.unistra.di.pmo.fsp.historique.AlertType;
import fr.unistra.di.pmo.fsp.historique.HistoryType;
import fr.unistra.di.pmo.fsp.historique.ProjectDocument;
import fr.unistra.di.pmo.fsp.historique.ProjectHistoryType;
import fr.unistra.di.pmo.fsp.i18n.Messages;
import fr.unistra.di.pmo.fsp.parametres.ParametersType;
import fr.unistra.di.pmo.fsp.parametres.WikiOutputType;
import fr.unistra.di.pmo.fsp.project.WikiItem;
import fr.unistra.di.pmo.fsp.redmine.Connection;
import fr.unistra.di.utils.StringUtils;

/**
 * Aggregation into Dokuwiki format.
 * 
 * @author virgile
 */
public class WikiAggregation
{
	private static final String LAST_UPDATE = "Date mise à jour FSP"; //$NON-NLS-1$
	private static final String WEATHER = "Météo"; //$NON-NLS-1$

	private final Logger logger = LoggerFactory.getLogger(WikiAggregation.class);

	private static final String OLD = "old"; //$NON-NLS-1$
	private static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy"); //$NON-NLS-1$
	private static SimpleDateFormat invertedDate = new SimpleDateFormat("yyyy-MM-dd"); //$NON-NLS-1$
	private static final double HOURS_IN_DAY = 7.5;
	private static NumberFormat nf = new DecimalFormat("##"); //$NON-NLS-1$

	public static final String RED = "rouge"; //$NON-NLS-1$;
	public static final String ORANGE = "orange"; //$NON-NLS-1$;
	public static final String GREEN = "vert"; //$NON-NLS-1$;

	private ParametersType parameters;
	private HashMap<String, WikiItem> fsps;
	private HashMap<String, Project> redmineProjects;

	/**
	 * Constructor.
	 * 
	 * @param parameters application parameters containing list of accepted
	 *            projects
	 * @param fsps global list of accepted projects
	 * @param redmineProjects link to redmine project
	 * @throws ParameterException if any problem using application parameters
	 * @throws IOException Error during file access
	 */
	public WikiAggregation(ParametersType parameters, HashMap<String, WikiItem> fsps, HashMap<String, Project> redmineProjects) throws ParameterException, IOException
	{
		this.parameters = parameters;
		this.fsps = fsps;
		this.redmineProjects = redmineProjects;
	}

	/**
	 * Aggregate data from FSP for projects defined in parameters
	 * 
	 * @param at aggregate to create
	 * @param outputPath destination file
	 * @return name of output file
	 * @throws IOException error during read / write operations
	 */
	public String aggregateSheet(WikiOutputType at, String outputPath) throws IOException
	{
		if ((at == null) || (at.getName() == null))
			return null;
		if (outputPath == null)
			return null;

		String result = Messages.getString("WikiAggregation.MainHeader"); //$NON-NLS-1$
		result += newLine(
				true,
				Messages.getString("WikiAggregation.Header.Project"), Messages.getString("WikiAggregation.Header.Weather"), Messages.getString("WikiAggregation.Header.History"), Messages.getString("WikiAggregation.Header.ConsumedTime"), Messages.getString("WikiAggregation.Header.News"), Messages.getString("WikiAggregation.Header.ControlDate"), Messages.getString("WikiAggregation.Header.LastReport")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$

		// Warnings
		String warningParagraphs = Messages.getString("WikiAggregation.NewsParagraph"); //$NON-NLS-1$

		if (at.sizeOfFspArray() == 0)
		{
			ArrayList<String> list = new ArrayList<String>(new TreeSet<String>(fsps.keySet()));
			Collections.sort(list);
			for (String string : list)
			{
				Main.wikiLogger.debug(string);
				Main.wikiLogger.debug(fsps.get(string).getRedmineId());
				at.addFsp(string);
			}
		}

		if (!outputPath.endsWith("/"))outputPath += "/"; //$NON-NLS-1$//$NON-NLS-2$

		for (String fspName : at.getFspArray())
		{
			Main.wikiLogger.info("************* Processing FSP with name " + fspName); //$NON-NLS-1$
			if (fsps.containsKey(fspName))
			{
				WikiItem fsp = fsps.get(fspName);
				Main.redmineLogger.info("Will use redmine project with id '" + fsp.getRedmineId() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
				if (redmineProjects.containsKey(fsp.getRedmineId()))
				{
					try
					{
						Project project = redmineProjects.get(fsp.getRedmineId());

						Main.redmineLogger.info("Processing " + project.getName() + " (" + project.getIdentifier() + " / " + project.getId() + ")"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$

						// History
						ProjectDocument projectDocument = null;
						File f = new File(outputPath + fsp.getId() + ".xml"); //$NON-NLS-1$
						if (f.exists())
						{
							projectDocument = ProjectDocument.Factory.parse(f);
						} else
						{
							projectDocument = ProjectDocument.Factory.newInstance();
							projectDocument.addNewProject();
						}

						ProjectHistoryType projectHistoryType = projectDocument.getProject();
						projectHistoryType.setId("" + project.getId()); //$NON-NLS-1$
						projectHistoryType.setName(fsp.getName());
						projectHistoryType.setIdentifier(project.getIdentifier());

						String weather = null;
						String lastUpdate = null;

						List<CustomField> customFields = project.getCustomFields();
						for (CustomField customField : customFields)
						{
							Main.redmineLogger.debug("Custom field : " + customField.getName() + " / " + customField.getValue());  //$NON-NLS-1$//$NON-NLS-2$
							if (customField.getName().equals(WEATHER))
							{
								weather = customField.getValue();
							}
							if (customField.getName().equals(LAST_UPDATE))
							{
								lastUpdate = customField.getValue();
							}
						}

						// Check if new history item
						Date lu = null;
						if ((lastUpdate != null) && (!lastUpdate.trim().equals(""))) //$NON-NLS-1$
							lu = invertedDate.parse(lastUpdate);
						boolean update = ((lu != null) && ((projectHistoryType.sizeOfHistoryArray() == 0) || (projectHistoryType.getHistoryArray(projectHistoryType.sizeOfHistoryArray() - 1).getDate().getTimeInMillis() < lu.getTime())));

						String lus = "null"; //$NON-NLS-1$
						if (lu != null)
							lus = invertedDate.format(lu);
						Calendar pu = null;
						if (projectHistoryType.sizeOfHistoryArray() != 0)
							pu = projectHistoryType.getHistoryArray(projectHistoryType.sizeOfHistoryArray() - 1).getDate();
						String pus = "null"; //$NON-NLS-1$
						if (pu != null)
							pus = invertedDate.format(pu.getTime());
						Main.redmineLogger.info("Last update : " + lus + " / Previous update : " + pus); //$NON-NLS-1$ //$NON-NLS-2$

						HistoryType historyType = null;
						if (update)
						{
							historyType = projectHistoryType.addNewHistory();
							GregorianCalendar gc = new GregorianCalendar();
							gc.setTime(lu);
							historyType.setDate(gc);
							historyType.setWeather(weather);
						} else
						{
							if (projectHistoryType.sizeOfHistoryArray() > 0)
							{
								historyType = projectHistoryType.getHistoryArray(projectHistoryType.sizeOfHistoryArray() - 1);
								weather = historyType.getWeather();
								lu = historyType.getDate().getTime();
							}
						}

						int warnings = 0;

						Date controlDate = project.getCreatedOn();

						// Sparkline
						SparkLine sl = new SparkLine();
						for (HistoryType ht : projectHistoryType.getHistoryArray())
						{
							sl.addValue(ht.getWeather().toLowerCase());
						}

						// Consumed time
						HashMap<String, String> map = new HashMap<String, String>();
						// Need to fetch closed issues too
						map.put("project_id", project.getId() + ""); //$NON-NLS-1$ //$NON-NLS-2$
						map.put("status_id", "*"); //$NON-NLS-1$ //$NON-NLS-2$
						List<Issue> issues = Connection.getInstance().getIssues(map);
						float projectTime = 0;
						long nbIssues = 0;
						long nbTimeEntries = 0;
						// Need to reject duplicate time entries
						HashMap<Integer, TimeEntry> accountedTimeEntries = new HashMap<Integer, TimeEntry>();
						for (Issue issue : issues)
						{
							List<TimeEntry> timeEntries = Connection.getInstance().getTimeEntriesForIssue(issue.getId());
							Main.redmineLogger.debug("Found " + timeEntries.size() + " time entries (before filter) for issue" + issue.getId());  //$NON-NLS-1$//$NON-NLS-2$
							nbIssues++;
							for (TimeEntry timeEntry : timeEntries)
							{
								if (!accountedTimeEntries.containsKey(timeEntry.getId()))
								{
									Main.redmineLogger.debug("Time entry " + timeEntry.getId() + " created on " + timeEntry.getCreatedOn());  //$NON-NLS-1$//$NON-NLS-2$
									if ((lu != null) && (timeEntry.getCreatedOn().getTime() < lu.getTime()))
									{
										projectTime += timeEntry.getHours();
										accountedTimeEntries.put(timeEntry.getId(), timeEntry);
										nbTimeEntries++;
									}
								}
							}
						}

						Main.redmineLogger.info("Found " + nbTimeEntries + " time entries in " + nbIssues + " issues"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

						// Warnings
						if (update)
						{
							try
							{
								List<News> news = Connection.getInstance().getNews(project.getIdentifier());
								{
									for (News news2 : news)
									{
										if (!news2.getTitle().toLowerCase().startsWith(OLD))
										{
											warnings++;
											warningParagraphs += addWarning(warnings, fsp.getName(), news2.getCreatedOn(), news2.getTitle(), news2.getDescription(), weather);

											AlertType alertType = historyType.addNewAlert();
											GregorianCalendar gc = new GregorianCalendar();
											gc.setTime(news2.getCreatedOn());
											alertType.setDate(gc);
											alertType.setTitle(news2.getTitle());
											alertType.setDescription(news2.getDescription());
											alertType.setId("" + news2.getId()); //$NON-NLS-1$
										}
									}
								}
							} catch (NotAuthorizedException e)
							{
								Main.redmineLogger.warn("News not activated for " + project.getIdentifier()); //$NON-NLS-1$
								warnings++;
								AlertType alertType = historyType.addNewAlert();
								GregorianCalendar gc = new GregorianCalendar();
								alertType.setDate(gc);
								alertType.setTitle("Connecteur"); //$NON-NLS-1$
								alertType.setDescription(Messages.getString("WikiAggregation.NotActivated.Warning")); //$NON-NLS-1$
								warningParagraphs += addWarning(warnings, fsp.getName(), new Date(), alertType.getTitle(), alertType.getDescription(), weather);
							}

							// Record changes
							FileOutputStream fos = new FileOutputStream(f);
							XmlOptions xmlOptions = new XmlOptions();
							xmlOptions.setSaveAggressiveNamespaces();
							xmlOptions.setSavePrettyPrint();
							fos.write(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + projectDocument.xmlText(xmlOptions)).getBytes("UTF-8")); //$NON-NLS-1$ //$NON-NLS-2$
							fos.close();
						} else if ((historyType != null) && (historyType.sizeOfAlertArray() > 0))
						{
							for (AlertType alertType : historyType.getAlertArray())
							{
								warnings++;
								warningParagraphs += addWarning(warnings, fsp.getName(), alertType.getDate().getTime(), alertType.getTitle(), alertType.getDescription(), weather);
							}
						}

						if (weather != null)
						{
							String w = "" + warnings; //$NON-NLS-1$
							if (warnings > 0)
							{
								w = "[[#" + getWikiPath(fspName) + "|" + warnings + "]]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							}
							result += newLine(false, fspName, smiley(weather.toLowerCase()), sl, projectTime, w, controlDate, lu, false);
						} else
							result += newLine(false, fspName, "Pas de FSP", " ", " ", " ", " ", " "); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
						
						Main.diff.addItem(project, projectHistoryType);

					} catch (Exception e)
					{
						result += newLine(false, fspName, "Erreur dans la fiche", " ", " ", " ", " ", " "); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
						logger.error("Erreur : " + fspName + " : " + e.getClass().getName() + " : " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						e.printStackTrace();
						try
						{
							HashMap<String, String> contextElements = new HashMap<String, String>();
							contextElements.put("fspName", fspName); //$NON-NLS-1$
							contextElements.put("redmineId", fsp.getRedmineId()); //$NON-NLS-1$
							Main.errorMail(parameters, e, contextElements);
						} catch (ParameterException e1)
						{
							e1.printStackTrace();
						} catch (MessagingException e1)
						{
							e1.printStackTrace();
						}
					}
				} else
				{
					result += newLine(false, fspName, " ", Messages.getString("WikiAggregation.NotActivated.Replacement"), " ", " ", " ", " "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
				}
			}
		}
		result += warningParagraphs;
		String path = outputPath;
		if (!path.endsWith(File.separator))
			path += File.separator;
		String fileName = path + at.getName() + ".wiki"; //$NON-NLS-1$
		FileOutputStream fos = new FileOutputStream(new File(fileName));
		fos.write(result.getBytes("UTF-8")); //$NON-NLS-1$
		fos.close();
		return fileName;
	}

	private String addWarning(int nb, String projectName, Date d, String title, String description, String weather)
	{
		String s = ""; //$NON-NLS-1$
		if (nb == 1)
			s += "\n=== " + projectName + " ==="; //$NON-NLS-1$ //$NON-NLS-2$
		s += "\n==[" + sdf.format(d) + "] "; //$NON-NLS-1$ //$NON-NLS-2$
		//		s += title + "==\n" + note(weather) + description + "</note>"; //$NON-NLS-1$ //$NON-NLS-2$
		s += title + "==\n" + description + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
		return s;
	}

	private String newLine(boolean heading, String projectName, String weather, SparkLine sl, double time, String warnings, Date controlDate, Date lastReport, boolean holidays)
	{
		String forecast = " "; //$NON-NLS-1$
		if (sl != null)
			forecast = sl.toString();
		String c = " "; //$NON-NLS-1$
		if (controlDate != null)
			c = sdf.format(controlDate);
		String l = " "; //$NON-NLS-1$
		if (lastReport != null)
		{
			long delay = (new Date()).getTime() - lastReport.getTime();
			if (!holidays)
			{
				l = ((Double) Math.floor(delay / 86400000)).intValue() + ""; //$NON-NLS-1$
			} else
			{
				if (delay < 0)
					l = "Congés"; //$NON-NLS-1$
				else
					l = ((Double) Math.floor(delay / 86400000)).intValue() + ""; //$NON-NLS-1$
			}
		}
		time = time / HOURS_IN_DAY;
		return newLine(heading, projectName, weather, forecast, nf.format(time), warnings, c, l);
	}

	private String newLine(boolean heading, String projectName, String weather, String forecast, String time, String warnings, String controlDate, String lastReport)
	{
		String separator = "|"; //$NON-NLS-1$
		String title = projectName;
		if (heading)
		{
			separator = "^"; //$NON-NLS-1$
		} else
		{
			if (getFsps().containsKey(projectName))
			{
				WikiItem fspt = getFsps().get(projectName);
				if (fspt.getName() != null)
					title = fspt.getName();
				if (fspt.getWikiPath() != null)
					title = "[[" + fspt.getWikiPath() + "|" + title + "]]"; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		if (forecast == null)
			forecast = " "; //$NON-NLS-1$
		String result = separator + title + separator + weather + separator + lastReport + separator + time + separator + forecast + separator + warnings + separator + controlDate + separator + "\n"; //$NON-NLS-1$
		return result;
	}

	private String smiley(String name)
	{
		if (name == null)
			return " "; //$NON-NLS-1$
		if (name.equals(RED))
			return "{{:red.png|Probl\u00E8me grave}}"; //$NON-NLS-1$
		if (name.equals(ORANGE))
			return "{{:orange.png|Situation sensible}}"; //$NON-NLS-1$
		if (name.equals(GREEN))
			return "{{:green.png|Tout va bien}}"; //$NON-NLS-1$
		return " "; //$NON-NLS-1$
	}

	private String note(String name)
	{
		if (name == null)
			return " "; //$NON-NLS-1$
		name = name.toLowerCase();
		String s = "<note "; //$NON-NLS-1$
		if (name.equals(RED))
			s += "warning"; //$NON-NLS-1$
		if (name.equals(ORANGE))
			s += "important"; //$NON-NLS-1$
		if (name.equals(GREEN))
			s += "tip"; //$NON-NLS-1$
		s += ">"; //$NON-NLS-1$
		return s;
	}

	private String getWikiPath(String title)
	{
		if (title == null)
			return null;
		String s = title.toLowerCase();
		s = StringUtils.deaccent(s);
		return s.replaceAll("\\W", "_"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Graphical presentation.
	 * 
	 * @author virgile
	 */
	public class SparkLine
	{
		private Vector<String> values;

		/**
		 * Constructor.
		 */
		public SparkLine()
		{
			values = new Vector<String>();
		}

		/**
		 * Add a value to graph.
		 * 
		 * @param value new value
		 */
		public void addValue(String value)
		{
			if (value != null)
			{
				if (value.equals(RED.toLowerCase()))
					values.add("98"); //$NON-NLS-1$
				if (value.equals(ORANGE.toLowerCase()))
					values.add("99"); //$NON-NLS-1$
				if (value.equals(GREEN.toLowerCase()))
					values.add("100"); //$NON-NLS-1$
			}
		}

		/**
		 * Generate sparkline code (
		 * {@link "http://www.dokuwiki.org/plugin:sparkline"}
		 */
		@Override
		public String toString()
		{
			if (values.size() < 1)
				return ""; //$NON-NLS-1$
			String result = "{{spark>"; //$NON-NLS-1$
			for (int i = 0; i < values.size(); i++)
			{
				result += values.elementAt(i);
				if (i < values.size() - 1)
					result += ","; //$NON-NLS-1$
			}
			result += "}}"; //$NON-NLS-1$
			return result;
		}
	}

	/**
	 * Get the value.
	 * 
	 * @return the parameters
	 */
	public ParametersType getParameters()
	{
		return parameters;
	}

	/**
	 * Set the value.
	 * 
	 * @param parameters the parameters to set
	 */
	public void setParameters(ParametersType parameters)
	{
		this.parameters = parameters;
	}

	/**
	 * Get the value.
	 * 
	 * @return the fsps
	 */
	public HashMap<String, WikiItem> getFsps()
	{
		return fsps;
	}

	/**
	 * Set the value.
	 * 
	 * @param fsps the fsps to set
	 */
	public void setFsps(HashMap<String, WikiItem> fsps)
	{
		this.fsps = fsps;
	}

	/**
	 * Get the value.
	 * 
	 * @return the redmineProjects
	 */
	public HashMap<String, Project> getRedmineProjects()
	{
		return redmineProjects;
	}

	/**
	 * Set the value.
	 * 
	 * @param redmineProjects the redmineProjects to set
	 */
	public void setRedmineProjects(HashMap<String, Project> redmineProjects)
	{
		this.redmineProjects = redmineProjects;
	}

}
