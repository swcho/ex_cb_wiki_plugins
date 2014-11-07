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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.DataBinder;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.chart.ChartDataCalculator;
import com.intland.codebeamer.chart.renderer.CommitTrendsRenderer;
import com.intland.codebeamer.controller.support.CommaStringToIntegerListPropertyEditor;
import com.intland.codebeamer.wiki.plugins.ajax.AbstractAjaxRefreshingWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.AbstractTimeIntervalCommandWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.ChartSupport;
import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;
import com.intland.codebeamer.wiki.plugins.command.CommitTrendsPluginCommand;
import com.intland.codebeamer.wiki.plugins.command.base.AbstractTimeIntervalDisplayOptionCommand.StandardTimeIntervalsAndGrouping;

/**
 * Wiki-Plugin showing commit trends
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public class CommitTrendsPlugin extends AbstractAjaxRefreshingWikiPlugin<CommitTrendsPluginCommand>{

	@Autowired
	ChartDataCalculator chartDataCalculator;

	public CommitTrendsPlugin() {
		setWebBindingInitializer(new AbstractTimeIntervalCommandWikiPlugin.TimeIntervalBindingInitializer() {
			{
				addPropertyEditorRegistrar(new PropertyEditorRegistrar() {
					public void registerCustomEditors(PropertyEditorRegistry registry) {
						registry.registerCustomEditor(List.class, "repositoryId", new CommaStringToIntegerListPropertyEditor());
					}
				});
			}
		});
	}

	@Override
	public CommitTrendsPluginCommand createCommand() throws PluginException {
		return new CommitTrendsPluginCommand();
	}

	@Override
	protected String getTemplateFilename() {
		return "commitTrends-plugin.vm";
	}

	@Override
	protected void validate(DataBinder binder, CommitTrendsPluginCommand command, Map params) throws NamedPluginException {
		RepositoryDiscovery discovery = new RepositoryDiscovery(this);
		command.setRepositoryId(discovery.discoverRepositoryIds(binder, params, command.getRepositoryId()));
		super.validate(binder, command, params);
	}

	@Override
	protected Map populateModelInternal(DataBinder binder, CommitTrendsPluginCommand command, Map params) throws PluginException {
		Map model = new HashMap();
		model.put("command", command);

		if (! CollectionUtils.isEmpty(command.getRepositoryId())) {
			model.put("standardTimeIntervalsAndGrouping", StandardTimeIntervalsAndGrouping.values());

			model.put("chartSupport", new ChartSupport(this, command, new CommitTrendsRenderer()));

			if (command.isTable()) {
				SortedMap<Date, Integer> data = chartDataCalculator.getCommitTrends(getUser(), command);
				model.put("data", data);
			}
		} else {
			model.put("emptyMessage", RepositoryDiscovery.getMissingRepositoryIdReasonCode(this, params));
		}
		return model;
	}

}
