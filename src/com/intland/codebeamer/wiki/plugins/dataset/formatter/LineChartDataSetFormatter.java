package com.intland.codebeamer.wiki.plugins.dataset.formatter;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.Dataset;
import org.jfree.data.time.TimeSeriesCollection;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.wiki.plugins.dataset.WikiDataSetFormatter;

/**
 * Formats data to the format that ImageGen
 * <a href="http://www.jspwiki.org/wiki/LineChartPlugin">LineChart</a> expects.
 * The same format is used also for MultiplePieChartPlugin.
 *
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 * @version $Id$
 */
public class LineChartDataSetFormatter extends AbstractWikiDataSetFormatter implements WikiDataSetFormatter {
	public String format(Dataset dataSet) throws PluginException {
		if(dataSet instanceof CategoryDataset) {
			StringBuffer buffer = new StringBuffer(LINE_BREAK + ":Label|String");
			CategoryDataset categoryDataset = (CategoryDataset)dataSet;

			for(int x = 0; x < categoryDataset.getColumnCount(); x++) {
				buffer.append(",");
				buffer.append(formatValueForCharts(categoryDataset.getColumnKey(x)));
			}
			buffer.append(LINE_BREAK);
			for(int y = 0; y < categoryDataset.getRowCount(); y++) {
				buffer.append(formatValueForCharts(categoryDataset.getRowKey(y)));
				for(int x = 0; x < categoryDataset.getColumnCount(); x++) {
					buffer.append(",");
					buffer.append(formatValueForCharts(categoryDataset.getValue(y, x)));
				}
				buffer.append(LINE_BREAK);
			}

			String result = buffer.toString();
			return result;
		} else if(dataSet instanceof TimeSeriesCollection) {
			StringBuilder buffer = new StringBuilder(LINE_BREAK + ":Label|String");
			TimeSeriesCollection timeSeriesCollection = (TimeSeriesCollection)dataSet;

			if(timeSeriesCollection.getSeriesCount() > 0) {
				for(int x = 0; x < timeSeriesCollection.getSeries(0).getItemCount(); x++) { // assumes that all series have the same size and dates
					buffer.append(",");
					buffer.append(formatDateForCharts(timeSeriesCollection.getSeries(0).getDataItem(x).getPeriod().getStart()));
				}
			}
			buffer.append(LINE_BREAK);
			for(int y = 0; y < timeSeriesCollection.getSeriesCount(); y++) {
				buffer.append(formatValueForCharts(timeSeriesCollection.getSeriesKey(y)));
				for(int x = 0; x < timeSeriesCollection.getSeries(y).getItemCount(); x++) {
					buffer.append(",");
					buffer.append(formatValueForCharts(timeSeriesCollection.getSeries(y).getValue(x)));
				}
				buffer.append(LINE_BREAK);
			}

			String result = buffer.toString();
			return result;
		}

		throw new PluginException(getUnsupportedDatasetTypeMessage(dataSet));
	}
}
