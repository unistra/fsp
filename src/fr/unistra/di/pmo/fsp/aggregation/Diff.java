package fr.unistra.di.pmo.fsp.aggregation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import fr.unistra.di.pmo.fsp.FileUtils;
import fr.unistra.di.pmo.fsp.historique.AlertType;
import fr.unistra.di.pmo.fsp.historique.HistoryType;

/**
 * Description of differential log for projects activity.
 * @author virgile
 *
 */
public class Diff
{

	private String prefix;

	/**
	 * Constructor.
	 * @param prefix prefix for diff files (the week number will be added).
	 */
	public Diff(String prefix)
	{
		super();
		this.prefix = prefix;
	}

	/**
	 * Adds an item in the activity
	 * @param projectName name of the project
	 * @param oldHistoryType previous activity
	 * @param newHistoryType last activity. The week number will be used to identify the right activity file.
	 */
	public void addItem(String projectName, HistoryType oldHistoryType, HistoryType newHistoryType)
	{
		if (newHistoryType != null)
		{
			GregorianCalendar start = new GregorianCalendar();
			start.setTime(newHistoryType.getDate().getTime());
			String filename = prefix + start.get(Calendar.WEEK_OF_YEAR);
			File f = new File(filename);

			String content = ""; //$NON-NLS-1$

			// Existing content
			if (f.exists())
			{
				content += FileUtils.read(f);
			}

			content += "===== " + projectName + " =====";  //$NON-NLS-1$//$NON-NLS-2$

			if ((oldHistoryType == null) || (!oldHistoryType.getWeather().equals(newHistoryType.getWeather())))
			{
				content += "\nEtat passé de " + oldHistoryType.getWeather() + " à " + newHistoryType.getWeather();  //$NON-NLS-1$//$NON-NLS-2$
			}
			ArrayList<String> oldAlertsIds = new ArrayList<String>();
			if(oldHistoryType != null)
			{
				for (AlertType alertType : oldHistoryType.getAlertArray())
				{
					oldAlertsIds.add(alertType.getId());
				}
			}
			
			for (AlertType alertType : newHistoryType.getAlertArray())
			{
				if(! oldAlertsIds.contains(alertType.getId()))
				{
					content += "\nNouvelle " + alertType.getTitle() + "\n" + alertType.getDescription(); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			
			content += "\n\n"; //$NON-NLS-1$
			
			FileOutputStream fos;
			try
			{
				fos = new FileOutputStream(f);
				fos.write(content.getBytes());
				fos.close();
			} catch (FileNotFoundException e)
			{
				e.printStackTrace();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
