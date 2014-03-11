package fr.unistra.di.pmo.fsp.aggregation;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.table.TableModel;

import org.jopendocument.dom.spreadsheet.ColumnStyle;
import org.jopendocument.dom.spreadsheet.MutableCell;
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;

import fr.unistra.di.pmo.fsp.Sheets;
import fr.unistra.di.pmo.fsp.exception.ParameterException;
import fr.unistra.di.pmo.fsp.parametres.AggregateType;
import fr.unistra.di.pmo.fsp.parametres.FSPType;
import fr.unistra.di.pmo.fsp.parametres.ParametersType;

/**
 * Aggregation into OpenDocument Spreadsheet.
 * 
 * @author virgile
 */
public class OdtAggregation extends DataAggregation
{
	// Template file
	private File template;

	/**
	 * Constructor.
	 * 
	 * @param parameters application parameters containing list of accepted
	 *            projects.
	 * @param sheets fsps sheets
	 * @throws ParameterException if any problem using application parameters
	 * @throws IOException Error during file access
	 */
	public OdtAggregation(ParametersType parameters, Sheets sheets) throws ParameterException, IOException
	{
		super(parameters, sheets);
		template = new File(parameters.getTemplate());
		if (!template.canRead())
			throw new ParameterException("Unreadeable template"); //$NON-NLS-1$
	}

	/**
	 * Aggregate data from FSP for projects defined in parameters
	 * 
	 * @param at aggregate to create
	 * @param outputPath destination file
	 * @return name of output file
	 * @throws IOException error during read / write operations
	 */
	@Override
	public String aggregateSheet(AggregateType at, String outputPath) throws IOException
	{
		if ((at == null) || (at.getName() == null))
			return null;
		if ((getSheets() == null) || (getSheets().size() < 1))
			return null;

		SpreadSheet spreadSheet = SpreadSheet.createFromFile(template);
		Sheet sheet = spreadSheet.getSheet(0);
		sheet.setName(at.getName());

		int bttf = 7;
		if (at.isSetDelay())
			bttf = at.getDelay();
		GregorianCalendar referenceDate = new GregorianCalendar();
		referenceDate.setTimeInMillis(referenceDate.getTimeInMillis() - (bttf * getMilis()));

		int count = 1;

		// Column titles and styles
		for (String fsp : at.getFspArray())
		{
			boolean valid = false;
			// Project title
			String title = fsp;
			if (getProjects().containsKey(fsp))
			{
				if (getProjects().get(fsp).isSetName())
					title = getProjects().get(fsp).getName();
			}
			// Sheets
			if (getSheets().containsKey(fsp))
			{
				Sheet from = getSheets().get(fsp);

				int offset = offset(from);

				// Content
				for (int i = from.getRowCount() - 1; i >= 0; i--)
				{
					Object o = from.getValueAt(0, i);
					if (o instanceof Date)
					{
						copyRow(from, i, sheet, count, offset);

						sheet.setValueAt(title, 0, count++);
						valid = true;
						i = -1;
					}
				}
				if (!valid)
					sheet.setValueAt(title, 0, count++);
				// Copy sheets
				if ((from.getColumnCount() > 0) && (from.getRowCount() > 0))
				{
					String sheetName = fsp;
					if (getProjects().containsKey(fsp))
					{
						FSPType fspt = getProjects().get(fsp);
						if (fspt.isSetName())
							sheetName = fspt.getName();
					}
					Sheet projectSheet = spreadSheet.addSheet(sheetName);
					TableModel fromContent = from.getTableModel(0, 0, from.getColumnCount(), from.getRowCount());
					projectSheet.merge(fromContent, 0, 0);
					// Style
					for (int i = 0; (i < projectSheet.getColumnCount()) && (i < 50); i++)
					{
						// Cells style and content
						for (int j = 0; (j < projectSheet.getRowCount()) && (j < 50); j++)
						{
							if (j == 0)
							{
								String style = sheet.getStyleNameAt(i, j);
								projectSheet.getCellAt(i, j).setStyleName(style);
							} else
							{
								String style = sheet.getStyleNameAt(i, 1);
								projectSheet.getCellAt(i, j).setStyleName(style);
							}
							// Be happy, the API has a bug
							MutableCell<SpreadSheet> cto = projectSheet.getCellAt(i, j);
							MutableCell<SpreadSheet> cfrom = from.getCellAt(i, j);
							cto.setValue(cfrom.toString().replaceAll("<[^>]*>", ""));  //$NON-NLS-1$//$NON-NLS-2$
						}

						// Column width // Please FIX ME
						ColumnStyle cs = from.getColumn(i).getStyle();
						projectSheet.getColumn(i).getStyle().getFormattingProperties().setAttribute("column-width", "50mm", cs.getFormattingProperties().getNamespace()); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			} else
			{
				sheet.setValueAt(title, 0, count++);
			}
		}
		String path = outputPath;
		if (!path.endsWith(File.separator))
			path += File.separator;
		String fileName = path + at.getName() + ".ods"; //$NON-NLS-1$
		spreadSheet.saveAs(new File(fileName));
		return fileName;
	}

//	private void copyRow(Sheet from, int numberFrom, Sheet to, int numberTo)
//	{
//		if (from.getRowCount() >= numberFrom + 1)
//		{
//			if (to.getRowCount() <= numberTo)
//				to.setRowCount(numberTo + 1);
//			int min = Math.min(from.getColumnCount(), to.getColumnCount() - 1);
//			for (int i = 0; i < min; i++)
//			{
//				MutableCell<SpreadSheet> cto = to.getCellAt(i + 1, numberTo);
//				MutableCell<SpreadSheet> cfrom = from.getCellAt(i, numberFrom);
//				cto.setValue(cfrom.getValue());
//			}
//		}
//	}

	private void copyRow(Sheet from, int numberFrom, Sheet to, int numberTo, int offset)
	{
		if (from.getRowCount() >= numberFrom + 1)
		{
			if (to.getRowCount() <= numberTo)
				to.setRowCount(numberTo + 1);
			int min = Math.min(from.getColumnCount(), to.getColumnCount() - 2) - offset;
			for (int i = 0; i < min; i++)
			{
				int j = i;
				if (i >= 4)
					j = i + offset;
				
				MutableCell<SpreadSheet> cto = to.getCellAt(i + 1, numberTo);
				MutableCell<SpreadSheet> cfrom = from.getCellAt(j, numberFrom);
				cto.setValue(cfrom.getValue());
			}
		}
	}

}
