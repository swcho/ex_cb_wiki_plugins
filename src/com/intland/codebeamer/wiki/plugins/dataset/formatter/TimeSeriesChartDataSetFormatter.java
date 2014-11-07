package com.intland.codebeamer.wiki.plugins.dataset.formatter;

import java.util.Iterator;

import org.jfree.data.general.Dataset;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.wiki.plugins.dataset.WikiDataSetFormatter;

/**
 * Formats data to the format that ImageGen
 * <a href="http://www.jspwiki.org/wiki/TimeSeriesChartPlugin">TimeSeriesChart</a> expects.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 * @version $Id$
 */
public class TimeSeriesChartDataSetFormatter extends AbstractWikiDataSetFormatter implements WikiDataSetFormatter {
	private final static String HEADER = "|Millisecond|" + AbstractWikiDataSetFormatter.TIME_STAMP_FORMAT + ",actualvalue";

	public String format(Dataset dataSet) throws PluginException {
		if(dataSet instanceof TimeSeriesCollection) {
			StringBuilder buffer = new StringBuilder();
			TimeSeriesCollection timeSeriesCollection = (TimeSeriesCollection)dataSet;

			if(timeSeriesCollection.getSeriesCount() > 0) {
				boolean firstTime = true;
				for (Iterator it = timeSeriesCollection.getSeries().iterator(); it.hasNext();) {
					TimeSeries timeSeries = (TimeSeries)it.next();

					if (firstTime) {
						firstTime = false;
						//:DB Connections|Millisecond|yyyy/MM/dd-HH:mm:ss:SS,actualvalue
						buffer.append(':');
						buffer.append(timeSeries.getKey());
						buffer.append(HEADER);
					} else {
						buffer.append(timeSeries.getKey());
					}
					buffer.append(LINE_BREAK);

					for(int i = 0; i < timeSeries.getItemCount(); i++) {
						buffer.append("*");
						buffer.append(formatTimeStampForCharts(timeSeries.getDataItem(i).getPeriod().getStart()));
						buffer.append(",");
						buffer.append(formatValueForCharts(timeSeries.getValue(i)));
						buffer.append(LINE_BREAK);
					}
					buffer.append(LINE_BREAK);
				}
			} else {
				buffer.append(":Value");
				buffer.append(HEADER);
			}

			String result = buffer.toString();
			return result;
		}

		throw new PluginException(getUnsupportedDatasetTypeMessage(dataSet));
	}
}
