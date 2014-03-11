package fr.unistra.di.pmo.fsp.project;

/**
 * Project description.
 * 
 * @author virgile
 */
public class Project
{
	private String name;
	private String path;
	private String phase;

	/**
	 * Constructor.
	 * 
	 * @param name name of the project
	 * @param path page path
	 * @param phase phase
	 */
	public Project(String name, String path, String phase)
	{
		super();
		this.name = name;
		this.phase = phase;
		this.path = path;
	}

	/**
	 * Project informations
	 * @return project details
	 */
	public String informations()
	{
		return "Nom : " + getName() + " / Phase : " + getPhase();  //$NON-NLS-1$//$NON-NLS-2$
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
	 * @return the phase
	 */
	public String getPhase()
	{
		return phase;
	}

	/**
	 * Set the value.
	 * 
	 * @param phase the phase to set
	 */
	public void setPhase(String phase)
	{
		this.phase = phase;
	}

	/**
	 * Get the value.
	 * @return the path
	 */
	public String getPath()
	{
		return path;
	}

	/**
	 * Set the value.
	 * @param path the path to set
	 */
	public void setPath(String path)
	{
		this.path = path;
	}

}
