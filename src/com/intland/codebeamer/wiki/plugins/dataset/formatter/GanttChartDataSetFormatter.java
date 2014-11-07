package com.intland.codebeamer.wiki.plugins.dataset.formatter;

import org.jfree.data.gantt.Task;
import org.jfree.data.gantt.TaskSeries;
import org.jfree.data.gantt.TaskSeriesCollection;
import org.jfree.data.general.Dataset;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.wiki.plugins.dataset.WikiDataSetFormatter;

/**
 * Formats data to the format that ImageGen
 * <a href="http://www.jspwiki.org/wiki/GanttChartPlugin">GanttChart</a> expects.
 *
 * @author <a href="mailto:aron.gombas@simbirsoft.com">Aron Gombas</a>
 * @version $Id$
 */
public class GanttChartDataSetFormatter extends AbstractWikiDataSetFormatter implements WikiDataSetFormatter {
	public String format(Dataset dataSet) throws PluginException {
		if(dataSet instanceof TaskSeriesCollection) {
			StringBuffer buffer = new StringBuffer(LINE_BREAK + ":Name|String, Start|Day|M/d/yyyy, End|Day|M/d/yyyy, Percentage" + LINE_BREAK);
			TaskSeriesCollection taskSeriesCollection = (TaskSeriesCollection)dataSet;

			if(taskSeriesCollection.getSeriesCount() != 0) {
				TaskSeries taskSeries = taskSeriesCollection.getSeries(0);
				for(int i = 0; i < taskSeries.getItemCount(); i++) {
					Task task = taskSeries.get(i);

					String startMonth = formatDateForCharts(task.getDuration().getStart());
					String endMonth = formatDateForCharts(task.getDuration().getEnd());
					Number percentageValue = task.getPercentComplete();
					String percentage = (percentageValue != null && percentageValue.doubleValue() > 1.0) ? "1.0" : formatPercentageForCharts(percentageValue);

					buffer.append("*");
					buffer.append(formatValueForCharts(task.getDescription()));
					buffer.append(",");
					buffer.append(startMonth);
					buffer.append(",");
					buffer.append(endMonth);
					buffer.append(",");
					buffer.append(percentage);
					buffer.append(LINE_BREAK);
				}
			}

			String result = buffer.toString();
			return result;
		}

		throw new PluginException(getUnsupportedDatasetTypeMessage(dataSet));
	}
}
