package com.intland.codebeamer.wiki.plugins.dataset.formatter;

import java.util.Iterator;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.Dataset;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.wiki.plugins.dataset.WikiDataSetFormatter;

/**
 * Formats data to regular wiki tables.
 *
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 * @version $Id$
 */
public class WikiTableDataSetFormatter extends AbstractWikiDataSetFormatter implements WikiDataSetFormatter {
	public String format(Dataset dataSet) throws PluginException {
		if(dataSet instanceof CategoryDataset) {
			StringBuilder buffer = new StringBuilder(LINE_BREAK + "|| ");
			CategoryDataset categoryDataset = (CategoryDataset)dataSet;

			// format header
			for (Iterator it = categoryDataset.getColumnKeys().iterator(); it.hasNext();) {
				buffer.append("||");
				buffer.append(formatWikiTableValue(it.next()));
			}
			buffer.append(LINE_BREAK);

			// format body
			for(int y = 0; y < categoryDataset.getRowCount(); y++) {
				buffer.append("||");
				buffer.append(formatWikiTableValue(categoryDataset.getRowKey(y)));
				for(int x = 0; x < categoryDataset.getColumnCount(); x++) {
					Number value = categoryDataset.getValue(y, x);
					buffer.append("|");
					buffer.append(formatWikiTableValue(value));
				}
				buffer.append(LINE_BREAK);
			}

			String result = buffer.toString();
			return result;
		} else if(dataSet instanceof TimeSeriesCollection) {
			StringBuilder buffer = new StringBuilder(LINE_BREAK + "||");
			TimeSeriesCollection timeSeriesCollection = (TimeSeriesCollection)dataSet;

			// format header
			for (Iterator it = timeSeriesCollection.getSeries().iterator(); it.hasNext();) {
				TimeSeries timeSeries = (TimeSeries)it.next();
				buffer.append("||");
				buffer.append(formatWikiTableValue(timeSeries.getKey()));
			}
			buffer.append(LINE_BREAK);

			// format body
			if(timeSeriesCollection.getSeriesCount() > 0) {
				int size = timeSeriesCollection.getSeries(0).getItemCount(); // this assumes that all series have the same size and date values
				for(int y = 0; y < size; y++) {
					buffer.append("||");
					buffer.append(formatWikiTableValue(timeSeriesCollection.getSeries(0).getDataItem(y).getPeriod().getStart()));
					for(int x = 0; x < timeSeriesCollection.getSeriesCount(); x++) {
						Number value = (y < timeSeriesCollection.getItemCount(x)) ? timeSeriesCollection.getY(x, y) : Integer.valueOf(0);
						buffer.append("|");
						buffer.append(formatWikiTableValue(value));
					}
					buffer.append(LINE_BREAK);
				}
			}

			String result = buffer.toString();
			return result;
		}

		throw new PluginException(getUnsupportedDatasetTypeMessage(dataSet));
	}

	/**
	 * Formats and escapes the text value the way WIKI escaping expects it.
	 */
	private String formatWikiTableValue(Object value) {
		return escapeValue(formatValue(value), "|", '~');
	}
}
