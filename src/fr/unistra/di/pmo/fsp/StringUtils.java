package fr.unistra.di.pmo.fsp;

/**
 * Utilities to deal with Strings
 * @author virgile
 *
 */
public class StringUtils
{
	/**
	 * Remove accents.
	 * 
	 * @param s string to transform
	 * @return string without accents
	 */
	public static String deaccent(String s)
	{
		s = s.replaceAll("[\u00E8\u00E9\u00EA\u00EB]", "e"); //$NON-NLS-1$ //$NON-NLS-2$
		s = s.replaceAll("[\u00FA\u00FB\u00FC\u00F9]", "u"); //$NON-NLS-1$ //$NON-NLS-2$
		s = s.replaceAll("[\u00EC\u00ED\u00EE\u00EF]", "i"); //$NON-NLS-1$ //$NON-NLS-2$
		s = s.replaceAll("[\u00E0\u00E1\u00E2\u00E3\u00E4\u00E5]", "a"); //$NON-NLS-1$ //$NON-NLS-2$
		s = s.replaceAll("[\u00F2\u00F3\u00F4\u00F5\u00F6]", "o"); //$NON-NLS-1$ //$NON-NLS-2$
		s = s.replaceAll("\u00E7", "c"); //$NON-NLS-1$ //$NON-NLS-2$

		s = s.replaceAll("[\u00C8\u00C9\u00CA\u00CB]", "E"); //$NON-NLS-1$ //$NON-NLS-2$
		s = s.replaceAll("[\u00DA\u00DB\u00DC\u00D9]", "U"); //$NON-NLS-1$ //$NON-NLS-2$
		s = s.replaceAll("[\u00CC\u00CD\u00CE\u00CF]", "I"); //$NON-NLS-1$ //$NON-NLS-2$
		s = s.replaceAll("[\u00C0\u00C1\u00C2\u00C3\u00C4\u00C5]", "A"); //$NON-NLS-1$ //$NON-NLS-2$
		s = s.replaceAll("[\u00D2\u00D3\u00D4\u00D5\u00D6]", "O"); //$NON-NLS-1$ //$NON-NLS-2$

		return s;
	}
}
