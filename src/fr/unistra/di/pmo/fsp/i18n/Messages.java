package fr.unistra.di.pmo.fsp.i18n;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * String externalization.
 * @author virgile
 *
 */
public class Messages
{
	private static final String BUNDLE_NAME = "fr.unistra.di.pmo.fsp.i18n.messages"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	private Messages()
	{
	}

	/**
	 * Gets externalized string.
	 * @param key key of wanted string
	 * @return externalised string if exist, !key! otherwise
	 */
	public static String getString(String key)
	{
		try
		{
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e)
		{
			return '!' + key + '!';
		}
	}
}
