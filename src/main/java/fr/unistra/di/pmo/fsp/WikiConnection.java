package fr.unistra.di.pmo.fsp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.unistra.di.pmo.fsp.exception.ParameterException;
import fr.unistra.di.pmo.fsp.parametres.ParametersType;
import fr.unistra.di.pmo.fsp.parametres.WikiConfigurationType;

/**
 * 
 * @author virgile
 *
 */
public class WikiConnection
{
	private static XmlRpc xmlRpc;
	
	private final static Logger logger = LoggerFactory.getLogger("Wiki"); //$NON-NLS-1$

	/**s
	 * Get the value.
	 * @param parameters parameters
	 * 
	 * @return the xmlRpc
	 * @throws ParameterException exception
	 */
	public static XmlRpc getXmlRpc(ParametersType parameters) throws ParameterException
	{
		if(parameters == null) throw new ParameterException("Paramètres inexistants"); //$NON-NLS-1$
		if (xmlRpc == null)
		{
			WikiConfigurationType wiki = parameters.getWikiConfiguration();
			logger.info("Initialising connection to " + wiki.getXmlRpcService()); //$NON-NLS-1$
			xmlRpc = new XmlRpc(wiki.getXmlRpcService(), wiki.getUsername(), wiki.getPassword());
		}
		return xmlRpc;
	}

}
