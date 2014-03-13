package fr.unistra.di.utils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for strings.
 * 
 * @author virgile
 */
public class StringUtils
{

	/**
	 * Split words.
	 * 
	 * @param s input string
	 * @return words in a vector
	 */
	public static ArrayList<String> split(String s)
	{
		if (s == null)
			return null;
		Pattern p = Pattern.compile("[a-zA-Z][A-Z]*[a-z0-9]*"); //$NON-NLS-1$
		Matcher m = p.matcher(s);
		ArrayList<String> result = new ArrayList<String>();
		while (m.find())
		{
			result.add(m.group());
		}
		return result;
	}

	/**
	 * String to upper case.
	 * 
	 * @param s input string
	 * @return Upper case words separated by underscores
	 */
	public static String fullUpper(String s)
	{
		if (s == null)
			return null;
		String result = ""; //$NON-NLS-1$
		for (String item : split(s))
		{
			if (!result.equals(""))result += "_"; //$NON-NLS-1$//$NON-NLS-2$
			result += item.toUpperCase();
		}
		if (result.equals("")) //$NON-NLS-1$
			return null;
		return result;
	}

	/**
	 * String to lower case.
	 * 
	 * @param s input string
	 * @return lower case words separated by underscores
	 */
	public static String fullLower(String s)
	{
		if (s == null)
			return null;
		return fullUpper(s).toLowerCase();
	}

	/**
	 * Sting to camel case.
	 * 
	 * @param s input string
	 * @return Camel case string (all lower case, each word begins with an upper
	 *         case)
	 */
	public static String camelCase(String s)
	{
		if (s == null)
			return null;
		s = deaccent(s);
		String result = ""; //$NON-NLS-1$
		for (String item : split(s))
		{
			if (result.equals(""))result += firstLetterLowercase(item); //$NON-NLS-1$
			else
				result += firstLetterUppercase(item);
		}
		if (result.equals(""))return null; //$NON-NLS-1$
		return result;
	}

	/**
	 * Changes the first letter to upper case.
	 * 
	 * @param s input string
	 * @return changed string
	 */
	public static String firstLetterUppercase(String s)
	{
		if (s == null)
			return null;
		if (s.length() <= 1)
			return s.toUpperCase();
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	/**
	 * Changes the first letter to lower case.
	 * 
	 * @param s input string
	 * @return changed string
	 */
	public static String firstLetterLowercase(String s)
	{
		if (s == null)
			return null;
		if (s.length() <= 1)
			return s.toLowerCase();
		return s.substring(0, 1).toLowerCase() + s.substring(1);
	}

	/**
	 * Keeps only characters before underscore.
	 * 
	 * @param s input string
	 * @return output string
	 */
	public static String noSuffix(String s)
	{
		if (s == null)
			return null;
		String[] items = s.split("_"); //$NON-NLS-1$
		return items[0];
	}

	/**
	 * Remove accents.
	 * 
	 * @param s string to transform
	 * @return string without accents
	 */
	public static String deaccent(String s)
	{
		if (s == null)
			return null;
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

	/**
	 * Tests if string contents pertinent data (add corrects some Word
	 * artefacts).
	 * 
	 * @param s string to test
	 * @return null if empty, else corrected content
	 */
	public static String minInfo(String s)
	{
		if (s == null)
			return null;
		s = veryClean(s);
		if (s == null)
			return null;
		if (dropAllTags(s).trim().length() < 5)
			return null;
		return s;
	}

	/**
	 * Methode de suppression de balises non voulues et de verification du code
	 * HTML.
	 * 
	 * @param s cha&icirc;ne &agrave; nettoyer
	 * @return HTML modifi&eacute;
	 */
	public static String veryClean(String s)
	{
		if (s == null)
			return null;

		char apostrophe = 146;
		char pointsSuspension = 133;
		char oe = 156;
		char euro = 128;
		s = s.replace(apostrophe, '\'');
		s = s.replaceAll(pointsSuspension + "", "..."); //$NON-NLS-1$ //$NON-NLS-2$
		s = s.replaceAll(oe + "", "oe"); //$NON-NLS-1$ //$NON-NLS-2$
		s = s.replaceAll(euro + "", "&euro;"); //$NON-NLS-1$ //$NON-NLS-2$
		s = s.replaceAll("€", "&euro;"); //$NON-NLS-1$ //$NON-NLS-2$

		apostrophe = 8217;
		pointsSuspension = 8230;
		oe = 339;
		s = s.replace(apostrophe, '\'');
		s = s.replaceAll(pointsSuspension + "", "..."); //$NON-NLS-1$ //$NON-NLS-2$
		s = s.replaceAll(oe + "", "oe"); //$NON-NLS-1$ //$NON-NLS-2$

		if (dropAllTags(s).trim().equals(""))return null; //$NON-NLS-1$
		return s;
	}

	/**
	 * Methode de suppression de toutes les balises.
	 * 
	 * @param s cha&icirc;ne &agrave; nettoyer
	 * @return cha&icirc;ne de caract&egrave;res
	 */
	private static String dropAllTags(String s)
	{
		if (s == null)
			return null;
		s = s.replaceAll("<[.]*[^>]+>", ""); //$NON-NLS-1$//$NON-NLS-2$
		return s;
	}


}
