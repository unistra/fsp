package fr.unistra.di.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for dokuwiki content.
 * 
 * @author virgile
 */
public class DokuwikiUtils
{
	private static final String DATAENTRY = "---- dataentry"; //$NON-NLS-1$

	/**
	 * Gets data entry content
	 * 
	 * @param data page content
	 * @return key/value content
	 */
	public static HashMap<String, String> dataentry(String data)
	{
		if (data == null)
			return null;

		String[] global = data.split("======"); //$NON-NLS-1$
		String[] parts = global[2].split("====="); //$NON-NLS-1$

		HashMap<String, String> results = new HashMap<String, String>();
		for (int i = 0; i < parts.length; i++)
		{
			String s = parts[i].trim();

			// Metadata
			if (s.indexOf(DATAENTRY) >= 0)
			{
				Pattern p = Pattern.compile("^([a-zA-Z0-9_-]+) *: *([^#\\n]*)", Pattern.MULTILINE); //$NON-NLS-1$
				Matcher m = p.matcher(s);

				while (m.find())
				{
					String key = m.group(1);
					String value = m.group(2);
					if (value != null)
						value = value.trim();
					results.put(key, value);
				}
			}
		}

		return results;
	}

	/**
	 * Gets content of paragraph.
	 * 
	 * @param data full page content
	 * @param header header name
	 * @return content
	 */
	public static String contentOf(String data, String header)
	{
		if (data == null || header == null)
			return null;
		Pattern p = Pattern.compile("={4,5} *" + header + " *={4,5}([^=]*)", Pattern.DOTALL); //$NON-NLS-1$ //$NON-NLS-2$
		Matcher m = p.matcher(data);

		while (m.find())
		{
			return m.group(1);
		}

		return null;
	}

	/**
	 * Gets page title
	 * 
	 * @param data full page content
	 * @return value of first header
	 */
	public static String firstHeader(String data)
	{
		if (data == null)
			return null;
		Pattern p = Pattern.compile("={6} *([^=]+) *={6}", Pattern.DOTALL); //$NON-NLS-1$
		Matcher m = p.matcher(data);

		while (m.find())
		{
			return m.group(1).trim();
		}

		return null;
	}

	/**
	 * Makes the string wiki compliant for tables.
	 * 
	 * @param s input
	 * @return modified string
	 */
	public static String wikiTableCompliant(String s)
	{
		if (s == null)
			return s;
		return s = s.replaceAll("\\[", "(").replaceAll("\\]", ")").replaceAll("\\|", "%%|%%").replaceAll("(cifs://[^ ]*) ", "[[$1]] "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
	}

	/**
	 * Cuts data into paragraphs
	 * @param data content
	 * @param level level of title for cut
	 * @return array of couples header / content
	 */
	public static ArrayList<ArrayList<String>> cut(String data, int level)
	{
		if (data == null)
			return null;
		ArrayList<ArrayList<String>> results = new ArrayList<ArrayList<String>>();
		Pattern p = Pattern.compile("(={" + level + ",6}[^=]+={" + level + ",6})", Pattern.MULTILINE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		Matcher m = p.matcher(data);
		
		int start = 0;
		int end = 0;
		
		String header = null;
		
		while (m.find())
		{
			if(header != null)
			{
				end = m.start();
				ArrayList<String> al = new ArrayList<String>();
				al.add(header);
				al.add(data.substring(start,end));
				results.add(al);
			}
			
			start = m.end();
			header =  m.group(1).trim();
		}
		
		if(start > 0)
		{
			ArrayList<String> al = new ArrayList<String>();
			al.add(header);
			al.add(data.substring(start));
			results.add(al);
		}
		
		return results;
	}
}
