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
package com.intland.codebeamer.wiki.plugins.base;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import com.intland.codebeamer.chart.renderer.AbstractChartRendererTemplate;
import com.intland.codebeamer.controller.ControllerUtils;

/**
 * Chart support mixin.
 * Helps populating the model with chart-urls
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public class ChartSupport<Command> {

	private AbstractCommandWikiPlugin plugin;
	private Command command;
	// The real renderer used for rendering the html markup on the initial non-ajax page render
	private AbstractChartRendererTemplate<Command, ?> renderer;

	private String chartType = null;

	/**
	 * Optional url parameters
	 */
	private String urlParams = null;

	/**
	 * Constructor
	 * @param plugin
	 * @param command
	 * @param renderer The real renderer used for rendering the html markup on the initial non-ajax page render
	 */
	public ChartSupport(AbstractCommandWikiPlugin<Command> plugin, Command command, AbstractChartRendererTemplate<Command, ?> renderer) {
		this.plugin = plugin;
		this.command = command;
		this.renderer = renderer;
		ControllerUtils.autoWire(renderer, plugin.getWikiContext().getHttpRequest());
	}

	/**
	 * Set the chart type
	 * @param chartType the chartType to set
	 */
	public void setChartType(String chartType) {
		this.chartType = chartType;
	}

	/**
	 * Automatically figure out the chartType-name from the plugin's name
	 * @return
	 */
	protected String getChartType() {
		if (chartType != null) {
			return chartType;
		}
		String className = plugin.getClass().getSimpleName();
		String autoChartType = Character.toLowerCase(className.charAt(0)) + className.substring(1, className.length() - "Plugin".length());
		return autoChartType;
	}

	private String getBaseURL() {
		String baseURL = getContextPath() + "/chartRenderer.spr?chartType=" + getChartType();
		if (urlParams != null) {
			baseURL += urlParams;
		}
		return baseURL;
	}

	private String getContextPath() {
		final String contextPath = getRequest().getContextPath();
		return contextPath;
	}

	private HttpServletRequest getRequest() {
		return plugin.getWikiContext().getHttpRequest();
	}

	/**
	 * Get the simple chart url used for building the chart urls by adding the variable chart urls
	 * @return
	 */
	public String getChartURL() {
		String url = getBaseURL() + "&" + plugin.getRequestParamsByCommand(command, false);
		return url;
	}

	/**
	 * Get the chart markup to be rendered during initial render
	 * @return
	 * @throws IOException
	 */
	public String getChartMarkup() throws IOException {
		String html = renderer.render(getRequest(), command);
		return html;
	}
}
