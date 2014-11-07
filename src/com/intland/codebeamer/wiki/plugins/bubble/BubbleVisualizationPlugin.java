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
package com.intland.codebeamer.wiki.plugins.bubble;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.validation.DataBinder;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.chart.renderer.AbstractChartRendererTemplate;
import com.intland.codebeamer.chart.renderer.BubbleVisualizationRenderer;
import com.intland.codebeamer.wiki.plugins.ajax.AbstractAjaxRefreshingWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.AbstractTimeIntervalCommandWikiPlugin.TimeIntervalPropertyEditorRegistrar;
import com.intland.codebeamer.wiki.plugins.base.ChartSupport;
import com.intland.codebeamer.wiki.plugins.base.DefaultWebBindingInitializer;
import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;
import com.intland.codebeamer.wiki.plugins.command.enums.DisplayType;

/**
 * @author <a href="mailto:attila.banfi@intland.com">Attila Banfi</a>
 */
public class BubbleVisualizationPlugin extends AbstractAjaxRefreshingWikiPlugin<BubbleVisualizationCommand> {
	public BubbleVisualizationPlugin() {
		setWebBindingInitializer(new BubbleVisualizationBindingInitializer());
	}

	public static class BubbleVisualizationPropertyEditorRegistrar extends TimeIntervalPropertyEditorRegistrar implements PropertyEditorRegistrar {
		public void registerCustomEditors(PropertyEditorRegistry registry) {
			super.registerCustomEditors(registry);
		}
	}

	public static class BubbleVisualizationBindingInitializer extends DefaultWebBindingInitializer {
		public BubbleVisualizationBindingInitializer() {
			addPropertyEditorRegistrar(new BubbleVisualizationPropertyEditorRegistrar());
		}
	}

	@Override
	protected void validate(DataBinder binder, BubbleVisualizationCommand command, Map params) throws NamedPluginException {
		command.setDisplay(DisplayType.BOTH);
		super.validate(binder, command, params);
	}

	@Override
	public BubbleVisualizationCommand createCommand() throws PluginException {
		return new BubbleVisualizationCommand();
	}

	@Override
	protected Map populateModelInternal(DataBinder binder, BubbleVisualizationCommand command, Map params) throws PluginException {
		Map model = new HashMap();
		model.put("command", command);
		model.put("chartSupport", new ChartSupport(this, command, getChartRenderer()));
		return model;
	}

	/**
	 * This is a template method to return the chart renderer.
	 */
	protected AbstractChartRendererTemplate<BubbleVisualizationCommand, ?> getChartRenderer() {
		return new BubbleVisualizationRenderer();
	}

	@Override
	protected String getTemplateFilename() {
		return "bubble-visualization-plugin.vm";
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}
}
