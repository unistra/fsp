package fr.unistra.di.pmo.fsp.redmine;

import com.taskadapter.redmineapi.RedmineManager;

import fr.unistra.di.pmo.fsp.Main;
import fr.unistra.di.pmo.fsp.exception.ParameterException;
import fr.unistra.di.pmo.fsp.parametres.RedmineSourceType;

/**
 * Redmine connection management
 * @author virgile
 *
 */
public class Connection
{
	private static RedmineManager redmineManager;

	/**
	 * Gets connection to redmine server
	 * @return connection
	 * @throws ParameterException exception
	 */
	public static RedmineManager getInstance() throws ParameterException
	{
		if(redmineManager == null) throw new ParameterException("Redmine connection not initialized"); //$NON-NLS-1$
		return redmineManager;
	}

	/**
	 * Initialize Redmine connection
	 * @param rst connection parameters
	 */
	public static void init(RedmineSourceType rst)
	{
		if (rst != null)
		{
			Main.redmineLogger.info("Initialising connection to " + rst.getHost()); //$NON-NLS-1$
			redmineManager = new RedmineManager(rst.getHost(), rst.getAccessKey());
		}
	}
}
