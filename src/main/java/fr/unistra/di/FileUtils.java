package fr.unistra.di;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Utils for files.
 * @author virgile
 *
 */
public class FileUtils
{
	/**
	 * Read content of a text file.
	 * @param fileName full name of file
	 * @return string content if file exists, null otherwise
	 */
	public static String read(String fileName)
	{
		return read(new File(fileName));
	}
	
	/**
	 * Read content of a text file.
	 * @param f file
	 * @return string content if file exists, null otherwise
	 */
	public static String read(File f)
	{
		if (f.exists())
		{		
			// Read file
			StringBuilder contents = new StringBuilder();
			String contentString = null;

			try
			{
				BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8")); //$NON-NLS-1$
				String line = null;
				while ((line = input.readLine()) != null)
				{
					contents.append(line);
					contents.append(System.getProperty("line.separator")); //$NON-NLS-1$
				}
				contentString = contents.toString();
				input.close();

			} catch (IOException ex)
			{
				ex.printStackTrace();
			}
			return contentString;
		}
		return null;
	}
}
