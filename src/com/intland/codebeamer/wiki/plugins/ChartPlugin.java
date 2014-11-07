/*
 * Copyright by Intland Software
 *
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Intland Software. ("Confidential Information"). You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Intland.
 */
package com.intland.codebeamer.wiki.plugins;

import java.awt.Color;
import java.awt.Paint;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.category.IntervalCategoryDataset;
import org.jfree.data.general.Dataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.data.xy.XYDataset;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginManager;
import com.intland.codebeamer.chart.ChartBean;
import com.intland.codebeamer.chart.renderer.AbstractChartRendererTemplate;
import com.intland.codebeamer.controller.ControllerUtils;
import com.intland.codebeamer.controller.tempstorage.TempStorageService;
import com.intland.codebeamer.utils.Common;
import com.intland.codebeamer.wiki.CodeBeamerWikiContext;
import com.intland.codebeamer.wiki.plugins.base.AbstractArtifactAwareWikiPlugin;

/**
 * This class should be a general plugin to integrate jfreechart. The plugin is not complete and mustn't be used.
 *
 * @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 * @version $Id$
 */
public class ChartPlugin extends AbstractArtifactAwareWikiPlugin {
	private static Logger logger = Logger.getLogger(ChartPlugin.class);

	private Integer docId;
	private String src;
	private String body;
	private String encoding = "ISO-8859-1";

	private String type;
	private String title;
	private int width;
	private int height;
	private boolean legend = false;
	private boolean tooltips = false;
	private boolean urls = false;
	private boolean is3d = false;

	private Paint backgroundPaint = null;
	private PlotOrientation orientation;
	private String xAxisLabel;
	private String yAxisLabel;

	private TempStorageService tempStorageService;

	public String execute(WikiContext wcontext, Map params) {
		ControllerUtils.autoWire(this, getApplicationContext(wcontext));
		processParameters(params);

		CodeBeamerWikiContext context = (CodeBeamerWikiContext)wcontext;

		try {
			if (docId != null) {
				// Using CodeBeamer document.
				InputStream inputStream = getInputStream(context);
				body = Common.readFileToString(inputStream, encoding);
			}
			body = StringUtils.remove(body, (char)0);

			return processChart(context);

		} catch (Throwable ex) {
			String msg = ex.toString();
			logger.warn(msg, ex);

			return StringEscapeUtils.escapeHtml(msg);
		}
	}

	protected List<String> splitLine(String line) {
		List<String> values = new ArrayList<String>();
		if (line != null) {
			for (StringTokenizer st = new StringTokenizer(line); st.hasMoreTokens(); ) {
				values.add(st.nextToken());
			}
		}

		return values;
	}

	protected Dataset processDataset() throws IOException {
		BufferedReader reader = new BufferedReader(new StringReader(body));

		String header = reader.readLine();

		List<String> columns = splitLine(header);

		Dataset dataset = null;
		if (type.indexOf("pie") != -1) {
			DefaultPieDataset ds = new DefaultPieDataset();
			dataset = ds;

			List<String> values = splitLine(reader.readLine());
			for (Iterator<String> cit = columns.iterator(), vit = values.iterator(); cit.hasNext() && vit.hasNext(); ) {
				String column = cit.next();
				Number value = parseNumber(vit.next());

				ds.setValue(column, value);
			}
		} else if (type.indexOf("bar") != -1) {
			DefaultCategoryDataset ds = new DefaultCategoryDataset();
			dataset = ds;

			for (String line = null; (line = reader.readLine()) != null; ) {
				List<String> values = splitLine(line);

				Iterator<String> vit = values.iterator();
				String columnKey = vit.next();
				for (Iterator<String> cit = columns.iterator(); cit.hasNext() && vit.hasNext(); ) {
					Number value = parseNumber(vit.next());

					ds.addValue(value, columnKey, cit.next());
				}
			}
		}

		return dataset;
	}

	protected String processChart(CodeBeamerWikiContext context) throws IOException {
		Dataset dataset = processDataset();

		JFreeChart chart = null;
		if (type.indexOf("gant") != -1) {
			chart = createGantChart((IntervalCategoryDataset)dataset);
		} else if (type.indexOf("time") != -1) {
			chart = createTimeSeriesChart((XYDataset)dataset);
		} else if (type.indexOf("xy") != -1) {
			chart = createXYChart((XYDataset)dataset);
		} else if (type.indexOf("line") != -1) {
			chart = createLineChart((CategoryDataset)dataset);
		} else if (type.indexOf("pie") != -1) {
			chart = createPieChart((PieDataset)dataset);
		} else {
			chart = createBarChart((CategoryDataset)dataset);
		}

		AbstractChartRendererTemplate.setupDefaultNoDataMessage(chart);

		if (backgroundPaint != null) {
			chart.setBackgroundPaint(backgroundPaint);
		}

		ChartBean chartBean = new ChartBean();
		chartBean.setChart(chart);
		chartBean.setWidth(width);
		chartBean.setHeight(height);

		String html = chartBean.generateChartWithHTML(getContextPath(context), tempStorageService, tooltips || urls, null, title);
		return html;
	}

	protected JFreeChart createTimeSeriesChart(XYDataset dataset) {
		return ChartFactory.createTimeSeriesChart(title, xAxisLabel, yAxisLabel, dataset, legend, tooltips, urls);
	}

	protected JFreeChart createPieChart(PieDataset dataset) {
		if (is3d) {
			return ChartFactory.createPieChart3D(title, dataset, legend, tooltips, urls);
		}
		return ChartFactory.createPieChart(title, dataset, legend, tooltips, urls);
	}

	protected JFreeChart createLineChart(CategoryDataset dataset) {
		return ChartFactory.createLineChart(title, xAxisLabel, yAxisLabel, dataset, orientation, legend, tooltips, urls);
	}

	protected JFreeChart createXYChart(XYDataset dataset) {
		if (type.indexOf("line") != -1) {
			return ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, dataset, orientation, legend, tooltips, urls);
		}

		return ChartFactory.createXYAreaChart(title, xAxisLabel, yAxisLabel, dataset, orientation, legend, tooltips, urls);
	}

	protected JFreeChart createGantChart(IntervalCategoryDataset dataset) {
		return ChartFactory.createGanttChart(title, xAxisLabel, yAxisLabel, dataset, legend, tooltips, urls);
	}

	protected JFreeChart createBarChart(CategoryDataset dataset) {
		if (type.indexOf("line") != -1) {
			return ChartFactory.createLineChart(title, xAxisLabel, yAxisLabel, dataset, orientation, legend, tooltips, urls);
		} else if (type.indexOf("stackedbar") != -1) {
			if (is3d) {
				return ChartFactory.createStackedBarChart3D(title, xAxisLabel, yAxisLabel, dataset, orientation, legend, tooltips, urls);
			}
			return ChartFactory.createStackedBarChart(title, xAxisLabel, yAxisLabel, dataset, orientation, legend, tooltips, urls);
		}

		if (is3d) {
			return ChartFactory.createBarChart3D(title, xAxisLabel, yAxisLabel, dataset, orientation, legend, tooltips, urls);
		}
		return ChartFactory.createBarChart(title, xAxisLabel, yAxisLabel, dataset, orientation, legend, tooltips, urls);
	}

	protected void processParameters(Map params) {
		try {
			docId = Integer.valueOf(getParameter(params, "id"));
		} catch (Exception ex) {
		}

		encoding = getParameter(params, "encoding", encoding);
		src = getParameter(params, "src");

		body = StringUtils.trimToEmpty((String)params.get(PluginManager.PARAM_BODY));

		type = getParameter(params, "type", "pie").toLowerCase();
		title = getParameter(params, "title", "-");
		width = NumberUtils.toInt(getParameter(params, "width"), 350);
		height = NumberUtils.toInt(getParameter(params, "height"), 350);
		legend = Boolean.parseBoolean(getParameter(params, "legend"));
		tooltips = Boolean.parseBoolean(getParameter(params, "tooltips"));
		urls = Boolean.parseBoolean(getParameter(params, "urls"));
		is3d = Boolean.parseBoolean(getParameter(params, "threed"));

		orientation = "horizontal".equals(getParameter(params, "orientation", "vertical")) ? PlotOrientation.HORIZONTAL : PlotOrientation.VERTICAL;

		backgroundPaint = getColor(getParameter(params, "backgroundPaint"));
	}

	/**
	 * Colors mapped by their name string
	 */
	public final static Map<String,Color> COLORS_BY_NAME = new HashMap<String, Color>();
	static {
		COLORS_BY_NAME.put("red", Color.RED);
		COLORS_BY_NAME.put("blue", Color.BLUE);
		COLORS_BY_NAME.put("green", Color.GREEN);
		COLORS_BY_NAME.put("gray", Color.GRAY);
		COLORS_BY_NAME.put("lightGray", Color.LIGHT_GRAY);
		COLORS_BY_NAME.put("darkGray", Color.DARK_GRAY);
		COLORS_BY_NAME.put("white", Color.WHITE);
		COLORS_BY_NAME.put("black", Color.BLACK);
		COLORS_BY_NAME.put("cyan", Color.CYAN);
		COLORS_BY_NAME.put("magenta", Color.MAGENTA);
		COLORS_BY_NAME.put("orange", Color.ORANGE);
		COLORS_BY_NAME.put("pink", Color.PINK);
		COLORS_BY_NAME.put("yellow", Color.YELLOW);
	}

	public static Paint getColor(String color) {
		Paint paint = null;
		if (color != null) {
			paint = COLORS_BY_NAME.get(color.toLowerCase());
			if (paint == null) {
				paint = Color.decode(color);
			}
		}
		return paint;
	}

	protected Number parseNumber(String string) {
		return Double.valueOf(string);
	}

	protected Integer getArtifactId() {
		return docId;
	}

	protected String getIconName() {
		return null;
	}

	protected String getSrc() {
		return src;
	}

	// Spring setters
	/**
	 * @param tempStorageService the tempStorageService to set
	 */
	public void setTempStorageService(TempStorageService tempStorageService) {
		this.tempStorageService = tempStorageService;
	}
}
