package fr.unistra.di.pmo.fsp.aggregation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jopendocument.dom.spreadsheet.MutableCell;
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;

import fr.unistra.di.pmo.fsp.Sheets;
import fr.unistra.di.pmo.fsp.StringUtils;
import fr.unistra.di.pmo.fsp.exception.ParameterException;
import fr.unistra.di.pmo.fsp.parametres.AggregateType;
import fr.unistra.di.pmo.fsp.parametres.FSPType;
import fr.unistra.di.pmo.fsp.parametres.ParametersType;
import fr.unistra.di.pmo.fsp.parametres.WikiOutputType;

/**
 * Aggregation into Dokuwiki format.
 * 
 * @author virgile
 */
public class WikiAggregation extends DataAggregation
{
	private static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy"); //$NON-NLS-1$
	private static NumberFormat nf = new DecimalFormat("##"); //$NON-NLS-1$

	protected static final String RED = "rouge"; //$NON-NLS-1$;
	protected static final String ORANGE = "orange"; //$NON-NLS-1$;
	protected static final String GREEN = "vert"; //$NON-NLS-1$;

	/**
	 * Constructor.
	 * 
	 * @param parameters application parameters containing list of accepted
	 *            projects.
	 * @param sheets fsps sheets
	 * @throws ParameterException if any problem using application parameters
	 * @throws IOException Error during file access
	 */
	public WikiAggregation(ParametersType parameters, Sheets sheets) throws ParameterException, IOException
	{
		super(parameters, sheets);
	}

	/**
	 * Aggregate data from FSP for projects defined in parameters
	 * 
	 * @param at aggregate to create
	 * @param outputPath destination file
	 * @return name of output file
	 * @throws IOException error during read / write operations
	 */
	@Override
	public String aggregateSheet(AggregateType at, String outputPath) throws IOException
	{
		if ((at == null) || (at.getName() == null))
			return null;
		if ((getSheets() == null) || (getSheets().size() < 1))
			return null;

		float totalTime = 0;
		int totalWarnings = 0;

		String result = "===== Suivi =====\n"; //$NON-NLS-1$
		result += newLine(true, "Projet", "M\u00E9t\u00E9o", "Historique", "Temps ((Temps consomm\u00E9 en jours))", "Alertes", "Date de mise sous contr\u00F4le", "\u00C2ge ((\u00C2ge du dernier rapport en jours))"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$

		// Warnings
		String warningParagraphs = "==== Alertes ====\n"; //$NON-NLS-1$

		for (String fsp : at.getFspArray())
		{
			if (getSheets().containsKey(fsp))
			{
				try
				{
					FSPType fspt = null;
					String title = fsp;
					if (getProjects().containsKey(fsp))
					{
						fspt = getProjects().get(fsp);
						if (fspt.isSetName())
							title = fspt.getName();
					}

//					float time = 0;
					int warnings = 0;
					String weather = null;
					String previousWeather = null;
					Date last = null;
					Date first = null;
					Sheet from = getSheets().get(fsp);
					SparkLine sl = new SparkLine();

					int offset = offset(from);

					boolean holidays = false;
					// Content
					for (int i = from.getRowCount() - 1; i > 0; i--)
					{
//						Object timeUsed = from.getValueAt(2, i);
//						Object managementTimeUsed = from.getValueAt(3, i);
						
//						if (timeUsed instanceof String)
//						{
//							try
//							{
//								String s = ((String) timeUsed).replaceAll(",", "."); //$NON-NLS-1$ //$NON-NLS-2$
//								s = s.replaceAll("[^0-9.]", "");  //$NON-NLS-1$//$NON-NLS-2$
//								timeUsed = new Float(s);
//							}
//							catch (Exception e) {
//								// Conversion failed
//							}
//						}
//						if (managementTimeUsed instanceof String)
//						{
//							try
//							{
//								String s = ((String) managementTimeUsed).replaceAll(",", "."); //$NON-NLS-1$ //$NON-NLS-2$
//								s = s.replaceAll("[^0-9.]", "");  //$NON-NLS-1$//$NON-NLS-2$
//								
//								managementTimeUsed = new Float(s);
//							}
//							catch (Exception e) {
//								// Conversion failed
//							}
//						}
						
//						if ((timeUsed instanceof Number) || (managementTimeUsed instanceof Number))
//						{
							boolean currentHolidays = false;
							Object o = from.getValueAt(0, i);
							Date temporaryDate = holidays(o);
							if (temporaryDate != null)
							{
								currentHolidays = true;
								o = temporaryDate;
							}
							Object todo = from.getValueAt(7 + offset, i);
							temporaryDate = holidays(todo);
							if (temporaryDate != null)
							{
								currentHolidays = true;
								o = temporaryDate;
							}
							
							if(o instanceof String)
							{
								Date d = null;
								try
								{
									d = sdf.parse((String) o);
								}
								catch (Exception e) {
									// Conversion failed
								}
								if(d != null) o = d;
							}
							
							if (o instanceof Date)
							{
								Date d = (Date) o;
								// Last (in time) FSP
								if (weather == null)
								{
									weather = (String) from.getValueAt(1, i);
									if (weather != null)
									{
										weather = weather.trim().toLowerCase();
										sl.addValue(weather);

										// Warnings
										Object w = from.getValueAt(4 + offset, i);
										if (w instanceof Float)
										{
											warnings = Math.round((Float) w);
										}
										if (w instanceof Integer)
										{
											warnings = (Integer) w;
										}
										if (w instanceof String)
										{
											warnings = 0;
											Pattern pattern = Pattern.compile("([0-9])+"); //$NON-NLS-1$
											Matcher matcher = pattern.matcher(getText(from.getCellAt(4 + offset, i)));
											while (matcher.find())
											{
												warnings += new Integer(matcher.group());
											}
										}
										if (warnings > 0)
										{
											warningParagraphs += "=== " + title + " ===\n"; //$NON-NLS-1$ //$NON-NLS-2$
											String s = getWikiText(from.getCellAt(5 + offset, i));
											warningParagraphs += s + "\n"; //$NON-NLS-1$
										}
										totalWarnings += warnings;
										last = d;
										holidays = currentHolidays;
									}
								}
								// Previous weather
								if ((last != null) && d.before(last) && (weather != null))
								{
									String s = (String) from.getValueAt(1, i);
									if (s != null)
									{
										s = s.trim().toLowerCase();
										sl.addValue(s);
									}
									if (previousWeather == null)
									{
										previousWeather = s;
									}
								}

								// Control date
								if (first == null)
									first = d;
								else if (d.before(first))
									first = d;
							}
							// Time used
//							if (timeUsed instanceof Float)
//							{
//								Float newTime = ((Number) timeUsed).floatValue();
//								time += newTime;
//								totalTime += newTime;
//							}
//							if (managementTimeUsed instanceof Float)
//							{
//								Float newTime = ((Number) managementTimeUsed).floatValue();
//								time += newTime;
//								totalTime += newTime;
//							}

						}
//					}
					if (weather != null)
					{
						String w = "" + warnings; //$NON-NLS-1$
						if (warnings > 0)
						{
							w = "[[" + ((WikiOutputType) at).getWikiPath() + "#" + getWikiPath(title) + "|" + warnings + "]]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						}
						result += newLine(false, fsp, smiley(weather), sl, 0, "" + w, first, last, holidays); //$NON-NLS-1$
					} else
					{
						System.out.println("Erreur : " + fsp); //$NON-NLS-1$
					}
				} catch (Exception e)
				{
					result += newLine(false, fsp, "Erreur dans la fiche", " ", " ", " ", " ", " "); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
					System.out.println("Erreur : " + fsp + " : " + e.getClass().getName() + " : " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					e.printStackTrace();
				}
			} else
			{
				result += newLine(false, fsp, " ", " ", " ", " ", " ", " "); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
			}
		}
		result += newLine(true, "Total", " ", "", "" + nf.format(totalTime), "" + totalWarnings, " ", ""); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
		result += warningParagraphs;
		String path = outputPath;
		if (!path.endsWith(File.separator))
			path += File.separator;
		String fileName = path + at.getName() + ".wiki"; //$NON-NLS-1$
		FileOutputStream fos = new FileOutputStream(new File(fileName));
		fos.write(result.getBytes());
		fos.close();
		return fileName;
	}

	private Date holidays(Object o) throws ParseException
	{
		if (o instanceof String)
		{
			// check holiday
			String s = ((String) o).toLowerCase();
			if ((s.indexOf("cong") < 0) && (s.indexOf("vacances") < 0) && (s.indexOf("holiday") < 0) && (s.indexOf("glande") < 0))return null; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			// find date
			Pattern p = Pattern.compile("(0[1-9]|[12][0-9]|3[01])[- /\\.](0[1-9]|1[012])[- /\\.]20[0-9][0-9]"); //$NON-NLS-1$
			Matcher m = p.matcher((String) o);
			while (m.find())
			{
				DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy"); //$NON-NLS-1$
				return formatter.parse(m.group());
			}
		}
		return null;
	}

	private String newLine(boolean heading, String projectName, String weather, SparkLine sl, float time, String warnings, Date controlDate, Date lastReport, boolean holidays)
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
			if (getProjects().containsKey(projectName))
			{
				FSPType fspt = getProjects().get(projectName);
				if (fspt.isSetName())
					title = fspt.getName();
				if (fspt.isSetWikiPath())
					title = "[[" + fspt.getWikiPath() + "|" + title + "]]"; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		if (forecast == null)
			forecast = " "; //$NON-NLS-1$
		String result = separator + title + separator + weather + separator + lastReport + separator + forecast + separator + warnings + separator + controlDate + separator + "\n"; //$NON-NLS-1$
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

	private String getWikiText(MutableCell<SpreadSheet> cell)
	{
		if (cell == null)
			return null;
		String result = cell.toString().replaceAll("<text:p[^>]*>", "  * "); //$NON-NLS-1$ //$NON-NLS-2$
		result = result.replaceAll("</text:p[^>]*>", "\n"); //$NON-NLS-1$//$NON-NLS-2$
		result = result.replaceAll("<[^>]*>", ""); //$NON-NLS-1$//$NON-NLS-2$
		result = result.replaceAll("[^\\*]{1}[ ]+[0-9]+[|–|-]+", "\n  * "); //$NON-NLS-1$//$NON-NLS-2$
		result = result.replaceAll("  [*]{1}[ ]+[0-9]+[|–|-]+", "  * "); //$NON-NLS-1$//$NON-NLS-2$
		result = result.replaceAll("\n\n", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		return result;
	}

	private String getText(MutableCell<SpreadSheet> cell)
	{
		if (cell == null)
			return null;
		String result = cell.toString().replaceAll("</text:p[^>]*>", "\n"); //$NON-NLS-1$//$NON-NLS-2$
		result = result.replaceAll("<[^>]*>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		return result;
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
				if (value.equals(RED))
					values.add("98"); //$NON-NLS-1$
				if (value.equals(ORANGE))
					values.add("99"); //$NON-NLS-1$
				if (value.equals(GREEN))
					values.add("100"); //$NON-NLS-1$
			}
		}

		/**
		 * Generate sparkline code ( {@link
		 * "http://www.dokuwiki.org/plugin:sparkline"}
		 */
		@Override
		public String toString()
		{
			if (values.size() < 1)
				return ""; //$NON-NLS-1$
			String result = "{{spark>"; //$NON-NLS-1$
			for (int i = values.size() - 1; i >= 0; i--)
			{
				result += values.elementAt(i);
				if (i > 0)
					result += ","; //$NON-NLS-1$
			}
			result += "}}"; //$NON-NLS-1$
			return result;
		}
	}

}
