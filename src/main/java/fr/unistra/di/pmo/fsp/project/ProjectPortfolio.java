package fr.unistra.di.pmo.fsp.project;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

import fr.unistra.di.pmo.fsp.aggregation.Diff;
import fr.unistra.di.pmo.fsp.parametres.ParametersType;
import fr.unistra.di.pmo.fsp.parametres.ProjectListDocument;
import fr.unistra.di.pmo.fsp.parametres.ProjectListType;
import fr.unistra.di.pmo.fsp.parametres.ProjectType;

/**
 * Description of a list of projects.
 * 
 * @author virgile
 */
public class ProjectPortfolio
{
	private static final String FILENAME = "existingProjects"; //$NON-NLS-1$
	private static final String FILESUFFIX = ".xml"; //$NON-NLS-1$
	private Hashtable<String, PortfolioItem> list;

	/**
	 * Add project to the list.
	 * 
	 * @param p project to add
	 */
	public void addProject(PortfolioItem p)
	{
		if (p != null)
		{
			if (list == null)
				list = new Hashtable<String, PortfolioItem>();
			list.put(p.getPath(), p);
		}
	}

	/**
	 * Compare two list to find differences (new, removed and modified projects)
	 * 
	 * @param pl list to compare to
	 * @return differences (text format)
	 */
	public String compare(ProjectPortfolio pl)
	{
		String diff = new String();
		if (pl != null)
		{
			if (pl.list != null)
			{
				// Compare to new list
				Enumeration<String> e = pl.list.keys();
				while (e.hasMoreElements())
				{
					String k = e.nextElement();
					// New element
					if ((list == null) || (!list.containsKey(k)))
					{
						diff += "Nouveau projet / " + pl.list.get(k).informations() + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
					}
					else
					{
						// Phase difference
						if (!pl.list.get(k).getPhase().equals(list.get(k).getPhase()))
						{
							diff += "Changement de phase / " + pl.list.get(k).informations() + " (anciennement " + list.get(k).getPhase() + ")\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
						if(!pl.list.get(k).getName().equals(list.get(k).getName()))
						{
							diff += "Projet renommé / " + pl.list.get(k).getName() + " (anciennement " + list.get(k).getName() + ")\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
					}
				}
			}
		}
		if (list != null)
		{
			// Compare to existing list
			Enumeration<String> e = list.keys();
			while (e.hasMoreElements())
			{
				String k = e.nextElement();
				// Deleted element
				if ((pl == null) || (pl.list == null) || (!pl.list.containsKey(k)))
				{
					diff += "Projet disparu / " + list.get(k).informations() + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		if (diff.equals(""))return null; //$NON-NLS-1$
		return diff;
	}

	/**
	 * Load project list from file
	 * 
	 * @param filePath file path
	 * @throws XmlException XML bug
	 * @throws IOException file bug
	 */
	public void load(String filePath) throws XmlException, IOException
	{
		if (filePath != null)
		{
			File f = new File(filePath);
			if (f.exists() && f.canRead())
			{
				ProjectListDocument doc = ProjectListDocument.Factory.parse(new File(filePath));
				if ((doc != null) && (doc.getProjectList() != null) && (doc.getProjectList().sizeOfProjectArray() > 0))
				{
					for (int i = 0; i < doc.getProjectList().sizeOfProjectArray(); i++)
					{
						ProjectType pt = doc.getProjectList().getProjectArray(i);
						addProject(new PortfolioItem(pt.getName(), pt.getPath(), pt.getPhase()));
					}
				}
			}
		}
	}

	/**
	 * Save project list in XML format
	 * 
	 * @param path file path
	 * @throws IOException file bug
	 */
	public void save(String path) throws IOException
	{
		if (path == null)
			throw new IOException();
		ProjectListDocument doc = ProjectListDocument.Factory.newInstance();
		ProjectListType pl = doc.addNewProjectList();
		if ((list != null) && (list.size() > 0))
		{
			Enumeration<String> e = list.keys();
			while (e.hasMoreElements())
			{
				String k = e.nextElement();
				PortfolioItem p = list.get(k);
				ProjectType pt = pl.addNewProject();
				pt.setName(p.getName());
				pt.setPhase(p.getPhase());
				pt.setPath(p.getPath());
			}
			FileOutputStream fos = new FileOutputStream(new File(path));
			XmlOptions opts = new XmlOptions();
			opts.setSaveAggressiveNamespaces();
			opts.setSavePrettyPrint();
			fos.write(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + doc.xmlText(opts)).getBytes("utf-8")); //$NON-NLS-1$ //$NON-NLS-2$
			fos.close();
		}
	}
	
	/**
	 * Creates backup.
	 * @param pt parameters
	 * @throws XmlException exception
	 * @throws IOException exception
	 */
	public static void backup(ParametersType pt) throws XmlException, IOException
	{
		ProjectPortfolio pf = new ProjectPortfolio();
		pf.load(path(pt));
		String path = pt.getOutputFolder();
		if (!path.endsWith(File.separator))
			path += File.separator;
		path += FILENAME + "-" + Diff.key(new GregorianCalendar()) + FILESUFFIX; //$NON-NLS-1$
		pf.save(path);
	}
	
	/**
	 * Gives path from parameters.
	 * @param pt parameters
	 * @return path
	 */
	public static String path(ParametersType pt)
	{
		String path = pt.getOutputFolder();
		if (!path.endsWith(File.separator))
			path += File.separator;
		path += FILENAME + FILESUFFIX;
		return path;
	}
}
