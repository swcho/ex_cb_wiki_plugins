package com.intland.codebeamer.wiki.plugins.dataset.formatter;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.Dataset;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.wiki.plugins.dataset.WikiDataSetFormatter;

/**
 * Formats data to the format that ImageGen
 * <a href="http://www.jspwiki.org/wiki/PieChartPlugin">PieChart</a> expects.
 *
 * @author <a href="mailto:aron.gombas@simbirsoft.com">Aron Gombas</a>
 * @version $Id$
 */
public class PieChartDataSetFormatter extends AbstractWikiDataSetFormatter implements WikiDataSetFormatter {
	public String format(Dataset dataSet) throws PluginException {
		if(dataSet instanceof CategoryDataset) {
			StringBuffer buffer = new StringBuffer(LINE_BREAK);
			CategoryDataset categoryDataset = (CategoryDataset)dataSet;

			for(int y = 0; y < categoryDataset.getRowCount(); y++) {
				buffer.append(formatValueForCharts(categoryDataset.getRowKey(y)));
				buffer.append("," + formatValueForCharts(categoryDataset.getValue(y, 0)) + LINE_BREAK);
			}
			buffer.append(LINE_BREAK);

			String result = buffer.toString();
			return result;
		}

		throw new PluginException(getUnsupportedDatasetTypeMessage(dataSet));
	}
}
