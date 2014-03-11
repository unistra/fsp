package fr.unistra.di.pmo.fsp.aggregation;

import java.io.IOException;
import java.util.Hashtable;

import org.jopendocument.dom.spreadsheet.Sheet;

import fr.unistra.di.pmo.fsp.Sheets;
import fr.unistra.di.pmo.fsp.exception.ParameterException;
import fr.unistra.di.pmo.fsp.parametres.AggregateType;
import fr.unistra.di.pmo.fsp.parametres.FSPType;
import fr.unistra.di.pmo.fsp.parametres.ParametersType;

/**
 * Aggregate several FSP into generalists boards
 * @author virgile
 *
 */
public abstract class DataAggregation
{
	// Application parameters
	private ParametersType parameters;
	// Accepted projects
	private Hashtable<String, FSPType> projects;
	// Spreadsheets for projects
	private Hashtable<String, Sheet> sheets;
	// Back in time
	private long milis = 86400000;
	
	/**
	 * Constructor.
	 * 
	 * @param parameters application parameters containing list of accepted
	 *            projects.
	 * @param sheets sheets containing fsps
	 * @throws ParameterException if any problem using application parameters
	 * @throws IOException Error during file access
	 */
	public DataAggregation(ParametersType parameters, Sheets sheets) throws ParameterException, IOException
	{
		this.parameters = parameters;
		if (this.parameters == null)
			throw new ParameterException("No parameters found"); //$NON-NLS-1$
		if(this.parameters.sizeOfFspArray() < 1) throw new ParameterException("No project found"); //$NON-NLS-1$
		if(this.parameters.getTemplate() == null) throw new ParameterException("No template defined"); //$NON-NLS-1$
		if(sheets == null) throw new ParameterException("No sheet found"); //$NON-NLS-1$
		
		// Initializations
		projects = sheets.getProjects();
		this.sheets = sheets.getSheets();
	}
	
	/**
	 * Aggregate data from FSP for projects defined in parameters
	 * @param at aggregate to create
	 * @param outputPath destination file
	 * @return name of output file
	 * @throws IOException error during read / write operations
	 */
	public abstract String aggregateSheet(AggregateType at, String outputPath) throws IOException;

	/**
	 * Get the value.
	 * @return the milis
	 */
	protected long getMilis()
	{
		return milis;
	}

	/**
	 * Get the value.
	 * @return the sheets
	 */
	protected Hashtable<String, Sheet> getSheets()
	{
		return sheets;
	}

	/**
	 * Get the value.
	 * @return the projects
	 */
	protected Hashtable<String, FSPType> getProjects()
	{
		return projects;
	}
	
	/**
	 * Offset in colums depending en FSP version.
	 * @param from sheet
	 * @return offset
	 */
	protected int offset(Sheet from)
	{
		int offset = 0;
		if((from.getValueAt(4,0) instanceof String) && (from.getValueAt(4,0) != null) && (((String)from.getValueAt(4,0)).toLowerCase().indexOf("charge") >= 0)) //$NON-NLS-1$
		{
			offset = 1;
		}
		return offset;
	}
}
