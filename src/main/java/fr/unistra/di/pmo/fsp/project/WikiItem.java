package fr.unistra.di.pmo.fsp.project;

import java.net.MalformedURLException;

import org.apache.xmlrpc.XmlRpcException;

import fr.unistra.di.pmo.fsp.Main;
import fr.unistra.di.pmo.fsp.XmlRpc;
 /**
  * Description of a project page in wiki
  * @author virgile
  *
  */
public class WikiItem
{
	private static final String ID_CHRONOS_ID = "idChronos_id"; //$NON-NLS-1$
	private static final String ID_CHRONOS_ID_BAD = "idChronos"; //$NON-NLS-1$
	private static final String CHEF_DE_PROJET = "Chef-de-Projet"; //$NON-NLS-1$
	private static final String DEPARTEMENT = "Departement"; //$NON-NLS-1$
	private static final String COMMANDITAIRE = "Commanditaire"; //$NON-NLS-1$
	private static final String PROGRAMME = "Programme"; //$NON-NLS-1$

	private static final String DATAENTRY = "---- dataentry"; //$NON-NLS-1$
	
	private String wikiPath;
	private String name;
	private String id;
	private String redmineId;
	
	private String programme;
	private String commanditaire;
	private String departement;
	private String chefDeProjet;

	/**
	 * Constructor. Uses the XML/RPC connection to get data from the project page.
	 * @param xmlRpc wiki connection
	 * @param wikiPath path to the page
	 * @param name name of the project
	 * @param id identifier
	 * @throws XmlRpcException exception 
	 * @throws MalformedURLException exception
	 */
	public WikiItem(XmlRpc xmlRpc, String wikiPath, String name, String id) throws MalformedURLException, XmlRpcException
	{
		super();
		this.wikiPath = wikiPath;
		this.name = name;
		this.id = id;
		
		Main.wikiLogger.info("Reading " + name + " at " + wikiPath); //$NON-NLS-1$ //$NON-NLS-2$
		
		String data = xmlRpc.read(wikiPath);
		fromWiki(data);
	}
	
	private void fromWiki(String data)
	{
		if (data != null)
		{
			String[] global = data.split("======"); //$NON-NLS-1$
			if(name == null) name = global[1].trim();
			String[] parts = global[2].split("====="); //$NON-NLS-1$
			for (int i = 0; i < parts.length; i++)
			{
				String s = parts[i].trim();

				// Metadata
				if (s.startsWith(DATAENTRY))
				{
					String[] meta = s.split("----"); //$NON-NLS-1$
					meta = meta[2].split("\n"); //$NON-NLS-1$
					for (int j = 0; j < meta.length; j++)
					{
						String line[] = meta[j].split(":"); //$NON-NLS-1$
						if (line.length > 1)
						{
							String valueName = line[0].trim();
							String value = line[1].trim();
							if (valueName.toLowerCase().equals(PROGRAMME.toLowerCase()))
								programme = clean(value);
							if (valueName.toLowerCase().equals(COMMANDITAIRE.toLowerCase()))
								commanditaire = clean(value);
							if (valueName.toLowerCase().equals(DEPARTEMENT.toLowerCase()))
								departement = clean(value);
							if (valueName.toLowerCase().equals(CHEF_DE_PROJET.toLowerCase()))
								chefDeProjet = clean(value);
							if (valueName.toLowerCase().equals(ID_CHRONOS_ID.toLowerCase()) || valueName.toLowerCase().equals(ID_CHRONOS_ID_BAD.toLowerCase()))
								redmineId = clean(value);
						}
					}
				}

				// Textual data

			}
		}
		if(redmineId == null || redmineId.trim().equals("")) //$NON-NLS-1$
		{
			Main.wikiLogger.warn("No Redmine id for " + wikiPath); //$NON-NLS-1$
		}
	}
	
	private String clean(String s)
	{
		if (s == null)
			return null;
		s = s.replaceAll(" *#[^#]*", "");  //$NON-NLS-1$//$NON-NLS-2$
		s = s.trim();
		return s;
	}

	/**
	 * Get the value.
	 * 
	 * @return the wikiPath
	 */
	public String getWikiPath()
	{
		return wikiPath;
	}

	/**
	 * Set the value.
	 * 
	 * @param wikiPath the wikiPath to set
	 */
	public void setWikiPath(String wikiPath)
	{
		this.wikiPath = wikiPath;
	}

	/**
	 * Get the value.
	 * 
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Set the value.
	 * 
	 * @param name the name to set
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * Get the value.
	 * 
	 * @return the id
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * Set the value.
	 * 
	 * @param id the id to set
	 */
	public void setId(String id)
	{
		this.id = id;
	}

	/**
	 * Get the value.
	 * 
	 * @return the redmineId
	 */
	public String getRedmineId()
	{
		return redmineId;
	}

	/**
	 * Set the value.
	 * 
	 * @param redmineId the redmineId to set
	 */
	public void setRedmineId(String redmineId)
	{
		this.redmineId = redmineId;
	}

	/**
	 * Get the value.
	 * @return the programme
	 */
	public String getProgramme()
	{
		return programme;
	}

	/**
	 * Set the value.
	 * @param programme the programme to set
	 */
	public void setProgramme(String programme)
	{
		this.programme = programme;
	}

	/**
	 * Get the value.
	 * @return the commanditaire
	 */
	public String getCommanditaire()
	{
		return commanditaire;
	}

	/**
	 * Set the value.
	 * @param commanditaire the commanditaire to set
	 */
	public void setCommanditaire(String commanditaire)
	{
		this.commanditaire = commanditaire;
	}

	/**
	 * Get the value.
	 * @return the departement
	 */
	public String getDepartement()
	{
		return departement;
	}

	/**
	 * Set the value.
	 * @param departement the departement to set
	 */
	public void setDepartement(String departement)
	{
		this.departement = departement;
	}

	/**
	 * Get the value.
	 * @return the chefDeProjet
	 */
	public String getChefDeProjet()
	{
		return chefDeProjet;
	}

	/**
	 * Set the value.
	 * @param chefDeProjet the chefDeProjet to set
	 */
	public void setChefDeProjet(String chefDeProjet)
	{
		this.chefDeProjet = chefDeProjet;
	}
}
