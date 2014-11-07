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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.DataBinder;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.controller.support.CommaStringToIntegerListPropertyEditor;
import com.intland.codebeamer.manager.ScmRepositoryManager;
import com.intland.codebeamer.persistence.dao.ScmChangeSetDao;
import com.intland.codebeamer.persistence.dto.ScmChangeSetStatsDto;
import com.intland.codebeamer.persistence.dto.ScmRepositoryDto;
import com.intland.codebeamer.wiki.plugins.base.AbstractCommandWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;

/**
 * Plugin to display a short summary on repository commits.
 *
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 */
public class CommitStatisticsPlugin extends AbstractCommandWikiPlugin<CommitStatisticsPluginCommand> {

	// constant for column layout
	private static final String LAYOUT_COLUMN = "column";

	@Autowired
	private ScmChangeSetDao scmChangeSetDao;
	@Autowired
	private ScmRepositoryManager scmRepositoryManager;

	@Override
	public CommitStatisticsPluginCommand createCommand() throws PluginException {
		return new CommitStatisticsPluginCommand();
	}

	@Override
	protected String getTemplateFilename() {
		return "commitstatistics-plugin.vm";
	}

	@Override
	protected void initBinder(DataBinder binder, CommitStatisticsPluginCommand command, Map params) {
		super.initBinder(binder, command, params);
		binder.registerCustomEditor(List.class, "repositoryId", new CommaStringToIntegerListPropertyEditor());
	}

	@Override
	protected void validate(DataBinder binder, CommitStatisticsPluginCommand command, Map params) throws NamedPluginException {
		super.validate(binder, command, params);

		RepositoryDiscovery discovery = new RepositoryDiscovery(this);
		command.setRepositoryId(discovery.discoverRepositoryIds(binder, params, command.getRepositoryId()));
	}

	@Override
	protected Map populateModel(DataBinder binder, CommitStatisticsPluginCommand command, Map params) throws PluginException {
		Map model = new HashMap();
		if (! CollectionUtils.isEmpty(command.getRepositoryId())) {
			// parse params
			boolean columnLayout = LAYOUT_COLUMN.equalsIgnoreCase(command.getLayout());

			Map<Integer, ScmChangeSetStatsDto> stats = findChangeSetStats(command.getRepositoryId());
			ScmChangeSetStatsDto commitStatsTotal = caclulateChangeSetsTotal(stats);

			if (command.isDetailed() || command.getRepositoryId().size() == 1) {
				// fetch all repos, and put their stats into the model too
				List<ScmRepositoryDto> repositories = scmRepositoryManager.findById(getUser(), command.getRepositoryId());
				model.put("repositories", repositories);
				model.put("stats", stats);
			}

			model.put("isCurrentProject", isEnclosingProject());
			model.put("commitStatsTotal", commitStatsTotal);
			model.put("columnLayout", Boolean.valueOf(columnLayout));
		} else {
			model.put("emptyMessage", RepositoryDiscovery.getMissingRepositoryIdReasonCode(this, params));
		}
		return model;
	}

	// TODO: suboptimal: this does 1 query per repo, but fine now
	private Map<Integer, ScmChangeSetStatsDto> findChangeSetStats(List<Integer> repositoryIds) {
		Map<Integer, ScmChangeSetStatsDto> result = new LinkedHashMap<Integer, ScmChangeSetStatsDto>();
		for (Integer repoId: repositoryIds) {
			ScmChangeSetStatsDto commitStats = scmChangeSetDao.findChangeSetStatsByRepository(repoId);
			result.put(repoId, commitStats);
		}
		return result;
	}

	private ScmChangeSetStatsDto caclulateChangeSetsTotal(Map<Integer, ScmChangeSetStatsDto> stats) {
		ScmChangeSetStatsDto total = new ScmChangeSetStatsDto();
		for (ScmChangeSetStatsDto commitStats: stats.values()) {
			if (commitStats != null) {
				// add up to the total
				total.setYesterday(total.getYesterday() + commitStats.getYesterday());
				total.setToday(total.getToday() + commitStats.getToday());
				total.setThisWeek(total.getThisWeek() + commitStats.getThisWeek());
				total.setThisMonth(total.getThisMonth() + commitStats.getThisMonth());
				total.setLast30Days(total.getLast30Days() + commitStats.getLast30Days());
				total.setAll(total.getAll() + commitStats.getAll());
			}
		}
		return total;
	}

}

