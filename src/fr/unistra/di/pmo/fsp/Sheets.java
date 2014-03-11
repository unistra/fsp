package fr.unistra.di.pmo.fsp;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;

import fr.unistra.di.pmo.fsp.exception.ParameterException;
import fr.unistra.di.pmo.fsp.parametres.FSPType;
import fr.unistra.di.pmo.fsp.parametres.ParametersType;

/**
 * List of sheets.
 * 
 * @author virgile
 */
public class Sheets
{
	// Application parameters
	private ParametersType parameters;
	// Accepted projects
	private Hashtable<String, FSPType> projects;
	// Spreadsheets for projects
	private Hashtable<String, Sheet> sheets;
	// Number of files referencing projets
	private Hashtable<String, Integer> occurencies;

	/**
	 * Constructs list of sheets. Only the most recent and accepted projects are
	 * kept.
	 * 
	 * @param parameters application parameters containing list of accepted
	 *            projects.
	 * @param files existing files
	 * @throws ParameterException if any problem using application parameters
	 * @throws IOException Error during file access
	 */
	public Sheets(ParametersType parameters, Hashtable<String, File> files) throws ParameterException, IOException
	{
		this.parameters = parameters;
		if (this.parameters == null)
			throw new ParameterException("No parameters found"); //$NON-NLS-1$
		if (this.parameters.sizeOfFspArray() < 1)
			throw new ParameterException("No project found"); //$NON-NLS-1$
		if (this.parameters.getTemplate() == null)
			throw new ParameterException("No template defined"); //$NON-NLS-1$

		// Initializations
		projects = new Hashtable<String, FSPType>();
		sheets = new Hashtable<String, Sheet>();
		occurencies = new Hashtable<String, Integer>();
		// List all projects
		for (FSPType project : this.parameters.getFspArray())
		{
			if (project.getSheetName() != null)
			{
				projects.put(project.getSheetName().toLowerCase(), project);
			}
		}

		// Find accepted sheets
		if (files != null)
		{
			Enumeration<String> e = files.keys();
			while (e.hasMoreElements())
			{
				File f = files.get(e.nextElement());
				SpreadSheet spreadSheet = SpreadSheet.createFromFile(f);
				for (int i = 0; i < spreadSheet.getSheetCount(); i++)
				{
					Sheet sheet = spreadSheet.getSheet(i);
					// Known project
					if (projects.containsKey(sheet.getName().toLowerCase()))
					{
						if (sheets.containsKey(sheet.getName().toLowerCase()))
						{
							// Keep most recent
							Sheet actual = sheets.get(sheet.getName().toLowerCase());
							Date sheetDate = lastDate(sheet);
							Date actualDate = lastDate(actual);
							if (sheetDate != null)
							{
								if ((actualDate == null) || (actual == null) || (actualDate.before(sheetDate)) || (actualDate.equals(sheetDate)))
								{
									String name = sheet.getName().toLowerCase();
									sheets.put(name, sheet);
									occurencies.put(name, occurencies.get(name) + 1);
								}
							}
						} else
						{
							String name = sheet.getName().toLowerCase();
							sheets.put(name, sheet);
							occurencies.put(name, 1);
						}
					}
				}
			}
		}
		System.out.println(projects.size() + " project(s) accepted"); //$NON-NLS-1$
		System.out.println(sheets.size() + " sheet(s) found"); //$NON-NLS-1$
		System.out.println(toString());
	}

	private Date lastDate(Sheet sheet)
	{
		Date date = null;
		for (int i = sheet.getRowCount() - 1; i > 0; i--)
		{
			Object o = sheet.getValueAt(0, i);
			if (o instanceof Date)
			{
				Date d = (Date) o;
				if (date == null)
					date = d;
				else
				{
					if (d.after(date))
						date = d;
				}
			}
		}
		return date;
	}

	/**
	 * Affichage des noms et dates.
	 */
	@Override
	public String toString()
	{
		if (sheets.size() < 1)
			return ""; //$NON-NLS-1$
		String s = ""; //$NON-NLS-1$
		final Enumeration<String> e = sheets.keys();
		while (e.hasMoreElements())
		{
			final String key = e.nextElement();
			s += "\t" + key + " : " + lastDate(sheets.get(key)) +  " / " + occurencies.get(key) + " suivis\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		return s;
	}

	/**
	 * Get the value.
	 * 
	 * @return the sheets
	 */
	public Hashtable<String, Sheet> getSheets()
	{
		return sheets;
	}

	/**
	 * Get the value.
	 * 
	 * @return the projects
	 */
	public Hashtable<String, FSPType> getProjects()
	{
		return projects;
	}

}
