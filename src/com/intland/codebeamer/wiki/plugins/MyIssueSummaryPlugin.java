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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.tools.generic.RenderTool;
import org.springframework.context.MessageSource;

import com.ecyrd.jspwiki.WikiContext;
import com.intland.codebeamer.controller.ControllerUtils;
import com.intland.codebeamer.controller.TrackerItemFlagsOption;
import com.intland.codebeamer.manager.WorkingSetManager;
import com.intland.codebeamer.persistence.dao.TrackerItemDao;
import com.intland.codebeamer.persistence.dao.TrackerItemStatisticsDao;
import com.intland.codebeamer.persistence.dao.impl.TrackerItemStatisticsDaoImpl;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.dto.UserItemStatsDto;
import com.intland.codebeamer.persistence.dto.UserTrackerItemStatsDto;
import com.intland.codebeamer.persistence.dto.UserTrackerItemsGroupedByProjectDto;
import com.intland.codebeamer.persistence.dto.WorkingSetDto;
import com.intland.codebeamer.persistence.util.Criteria;
import com.intland.codebeamer.persistence.util.TrackerItemFieldHandler;
import com.intland.codebeamer.persistence.util.TrackerItemRestrictions;
import com.intland.codebeamer.servlet.bugs.BrowseTrackerAction;
import com.intland.codebeamer.taglib.actionmenu.model.ActionItem;
import com.intland.codebeamer.ui.view.actionmenubuilder.ProjectListPageActionMenuBuilder;
import com.intland.codebeamer.wiki.plugins.base.AbstractCodeBeamerWikiPlugin;

/**
 * A plugin to display a summary of issues submitted by, assigned to or supervised by the current user and
 * matching specified criteria per project.
 *
 * Any necessary selectors/filters can be added/removed in java part without modifying the template.
 *
 * @author <a href="mailto:klaus.mehling@intland.com">Klaus Mehling</a>
 * @version $Id$
 */
@SuppressWarnings("deprecation")
public class MyIssueSummaryPlugin extends AbstractCodeBeamerWikiPlugin {
	private final static Logger logger = Logger.getLogger(MyIssueSummaryPlugin.class);

	public  static final String ITEMS_FILTER 			= "itemsFilter";
	public  static final String ONLY_DIRECT_USER_ITEMS 	= "onlyDirectUserItems";

	private static final String LAYOUT_MATRIX 			= "myopentrackeritems-matrix-plugin.vm";
	private static final String LAYOUT_SELECT 			= "myopentrackeritems-plugin.vm";

	private final static List<Integer> 	MEMBER_FIELDS 	= Arrays.asList(TrackerItemFieldHandler.SUBMITTED_BY_LABEL_ID, TrackerItemFieldHandler.ASSIGNED_TO_LABEL_ID, TrackerItemFieldHandler.SUPERVISOR_LABEL_ID);

	/**
	 * Parameters storage class for both selectors and filters
	 */
	public class Selection {
		/** Title to be displayed in form */
		private String name;

		/**
		 * prefix field is used in Velocity template to combine all the
		 * properties stated in UserTrackerItemStatsDto
		 */
		private String prefix;

		/** Selection parameters as key-vaule pairs */
		private Map<String,Object> parameters = new HashMap<String,Object>(8);

		public String getName() {
			return name;
		}

		Selection(String name, String prefix) {
			this.name = name;
			this.prefix = prefix;
		}

		public String getPrefix() {
			return prefix;
		}

		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}

		public Set<Map.Entry<String,Object>> getParametersSet() {
			return parameters.entrySet();
		}

		public void addParametersPair(String key, Object value) {
			this.parameters.put(key, value);
		}

		public String toString() {
			return (new ToStringBuilder(this)).toString();
		}
	}

	public class ProjectEntry {
		private WorkingSetDto workingSet;
		private ProjectDto project = null;
		private String projectName;
		private UserTrackerItemStatsDto stats;

		ProjectEntry(WorkingSetDto workingSet, ProjectDto project, UserTrackerItemStatsDto stats) {
			setWorkingSet(workingSet);
			setProject(project);
			setStats(stats);
		}

		public WorkingSetDto getWorkingSet() {
			return workingSet;
		}

		public void setWorkingSet(WorkingSetDto workingSet) {
			this.workingSet = workingSet;
		}

		public ProjectDto getProject() {
			return project;
		}

		public void setProject(ProjectDto project) {
			this.project = project;
			projectName = project != null ? project.getName() : "";
		}

		public String getProjectName() {
			return projectName;
		}

		public void setProjectName(String projectName) {
			this.projectName = projectName;
		}

		public UserTrackerItemStatsDto getStats() {
			return stats;
		}

		public void setStats(UserTrackerItemStatsDto stats) {
			this.stats = stats;
		}

		public int getAll() {
			return stats != null ? stats.getAll() : 0;
		}

		public String toString() {
			return "MyIssueSummaryPlugin$ProjectEntry[project=" + projectName +", stats=" + stats +"]";
		}

	}

	private static final WorkingSetManager 		  workingSetManager = WorkingSetManager.getInstance();
	private static final TrackerItemStatisticsDao trackerItemStatisticsDao = TrackerItemStatisticsDaoImpl.getInstance();

	/** List of filters applied to each selector. */
	/** List of selectors to be displayed. */
	private List<Selection> 	selectors    = Collections.emptyList();
	private List<ProjectEntry> 	projectsList = new ArrayList<ProjectEntry>();



	public String execute(WikiContext context, Map params) {
		HttpServletRequest request = context.getHttpRequest();
		UserDto user = getUserFromContext(context);
		Locale locale = request.getLocale();
		MessageSource messageSource = ControllerUtils.getMessageSource(request);

		long 	startTime 	 = System.currentTimeMillis();
		boolean showNewIssue = getBooleanParameter(params, "showNewIssue");

		boolean onlyDirectUserItems = Boolean.parseBoolean(getStringParameter(params, ONLY_DIRECT_USER_ITEMS, "true"));
		makeSelectors(messageSource, locale, onlyDirectUserItems);
		List<Selection> filters = makeFilters(messageSource, locale);

		Criteria criteria = null;
		TrackerItemFlagsOption show = getFlagsParameter(params, "show", "Unresolved");
		if (show != null) {
			if (show.getIncludes() != null && show.getIncludes().size() > 0) {
				criteria = new Criteria();
				criteria.add(TrackerItemRestrictions.getFlagsCriterion(show.getIncludes(), true, show.isAll()));
			}
			addSelectionParameter("show", show.getName());
		}

		// get beans from wiki context
		WorkingSetDto workingSet = null;
		if (workingSetManager.getConfiguration(user) == WorkingSetManager.CONFIGURATION_WORKINGSET) {
			List<WorkingSetDto> workingSets = workingSetManager.findByUser(user, true);
			if(workingSets != null && !workingSets.isEmpty()) {
				workingSet = workingSets.get(0);
			}
		}

		// Insert an entry for the all-projects in workingset
		UserTrackerItemStatsDto total = new UserTrackerItemStatsDto();
		ProjectEntry allProjects = new ProjectEntry(workingSet, null, total);
		allProjects.setProjectName(messageSource.getMessage("my.open.issues.all.projects", null, "All Projects", locale));
		projectsList.add(allProjects);

		// Get all projects in current user working set
		Set<ProjectDto> workingSetProjects = new HashSet<ProjectDto>(workingSetManager.getSelectedWorkingsetProjects(user));

		long timeToGetWorkingSets = System.currentTimeMillis();
		if (logger.isDebugEnabled()) {
			logger.debug("Took " + (timeToGetWorkingSets - startTime) + " ms. to get working-sets, user: " + user);
		}

		// Get the tracker issue statistics for the user
		Map<ProjectDto,Map<Integer,UserItemStatsDto>> itemStatsPerProjectAndField = trackerItemStatisticsDao.getUserItemStatsByProjectAndField(user, MEMBER_FIELDS, user, onlyDirectUserItems, workingSetProjects, Boolean.FALSE, show != null ? show.getExcludes() : null, criteria);
		for (UserTrackerItemsGroupedByProjectDto proj : UserTrackerItemsGroupedByProjectDto.convert(itemStatsPerProjectAndField)) {
			// Only show projects' statistics in current working set
			if (workingSetProjects.contains(proj.getProject())) {
				projectsList.add(new ProjectEntry(null, proj.getProject(), proj));
				total.add(proj);
			}
		}

		long timeToGetStatistics = System.currentTimeMillis();
		if (logger.isDebugEnabled()) {
			logger.debug("Took " + (timeToGetStatistics - timeToGetWorkingSets) + " ms. to get statistics, user: " + user);
		}

		// set up Velocity context
		VelocityContext velocityContext = getDefaultVelocityContextFromContext(context);
		velocityContext.put("title", getTitle(messageSource, locale, params, show));
		velocityContext.put("defaultProjectSelection", Integer.valueOf(0));  //'All Projects' selection by default
		velocityContext.put("userId", user.getId());
		velocityContext.put("projectsList", projectsList);
		velocityContext.put("numProjects", Integer.valueOf(projectsList.size() -1));
		velocityContext.put("selectors", selectors);
		velocityContext.put("filters", filters);
		velocityContext.put("contextPath", getApplicationContextPath(context));
		velocityContext.put("onlyDirectUserItems", Boolean.valueOf(onlyDirectUserItems));
		velocityContext.put("showNewIssue", Boolean.valueOf(showNewIssue));

		putAll(velocityContext, buildSelectedWorkingSetVars(context, user));

		// Necessary only for Velocity 1.5 and older
		velocityContext.put("ctx", velocityContext);
		velocityContext.put("render", new RenderTool());

		// render template
		String template = (getParameter(params, "layout", "").equalsIgnoreCase("select")) ? LAYOUT_SELECT : LAYOUT_MATRIX;
		String html = renderPluginTemplate(template, velocityContext);

		long timeToRender = System.currentTimeMillis();
		if (logger.isDebugEnabled()) {
			logger.debug("Took " + (timeToRender - timeToGetStatistics) + " ms. to render, user: " + user);
		}

		return html;
	}

	protected TrackerItemFlagsOption getFlagsParameter(Map params, String paramId, String defaultValue) {
		TrackerItemFlagsOption flags = TrackerItemFlagsOption.decode(getParameter(params, paramId, defaultValue));
		return flags != null ? flags : TrackerItemFlagsOption.decode(defaultValue);
	}

	private String getTitle(MessageSource messageSource, Locale locale, Map params, TrackerItemFlagsOption show) {
		String title = StringUtils.trimToNull(getParameter(params, "title"));
		if (title == null) {
			String what = (show != null ? show.getName() : "Open");
			what = messageSource.getMessage("issue.flags." + what + ".label", null, what, locale);
			title = messageSource.getMessage("my.open.issues.title", new Object[]{what}, "My {0} Issues", locale);
		}
		return title;
	}

	/**
	 * Put the selected working-set and its link as url to context.
	 * @param context The current wiki context
	 * @param user The current user
	 *
	 * @return The variables for the current selected working-set
	 */
	private Map buildSelectedWorkingSetVars(WikiContext context, UserDto user) {
		ProjectListPageActionMenuBuilder projectListPageActionMenuBuilder = (ProjectListPageActionMenuBuilder) getApplicationContext(context).getBean("projectListPageActionMenuBuilder");

		ActionItem action = projectListPageActionMenuBuilder.buildWorkingSetAction(context.getHttpRequest(), user);

		Map vars = new HashMap();
		if (action != null) {
			vars.put("selectedWorkingSetName", action.getLabel());
			vars.put("selectedWorkingSetURL" , action.getUrl());
		}
		return vars;
	}

	/**
	 * Prepare selectors.
	 */
	private void makeSelectors(MessageSource messageSource, Locale locale, boolean onlyDirectUserItems) {
		selectors = new ArrayList<Selection>(4);
		Selection sel;

		sel = new Selection(messageSource.getMessage("user.issues.all", null, "All", locale), "all");
		sel.addParametersPair(BrowseTrackerAction.ONLY_ASSIGNED_TO_USER, Boolean.TRUE);
		sel.addParametersPair(BrowseTrackerAction.ONLY_SUBMITTED_BY_USER, Boolean.TRUE);
		sel.addParametersPair(BrowseTrackerAction.ONLY_OWNED_BY_USER, Boolean.TRUE);
		sel.addParametersPair(ONLY_DIRECT_USER_ITEMS, Boolean.valueOf(onlyDirectUserItems));
		selectors.add(sel);

		sel = new Selection(messageSource.getMessage("user.issues.assignedTo", null, "Assigned to me", locale), "assigned");
		sel.addParametersPair(BrowseTrackerAction.ONLY_ASSIGNED_TO_USER, Boolean.TRUE);
		sel.addParametersPair(ONLY_DIRECT_USER_ITEMS, Boolean.valueOf(onlyDirectUserItems));
		selectors.add(sel);

		sel = new Selection(messageSource.getMessage("user.issues.submittedBy", null, "Submitted by me", locale), "submitted");
		sel.addParametersPair(BrowseTrackerAction.ONLY_SUBMITTED_BY_USER, Boolean.TRUE);
		selectors.add(sel);

		sel = new Selection(messageSource.getMessage("user.issues.supervisedBy", null, "Owned by me", locale), "supervised");
		sel.addParametersPair(BrowseTrackerAction.ONLY_OWNED_BY_USER, Boolean.TRUE);
		sel.addParametersPair(ONLY_DIRECT_USER_ITEMS, Boolean.valueOf(onlyDirectUserItems));
		selectors.add(sel);
	}

	private void addSelectionParameter(String name, String value) {
		if (selectors != null && name != null && value != null) {
			for (Selection selector : selectors) {
				selector.addParametersPair(name, value);
			}
		}
	}

	/**
	 * Prepare filters.
	 */
	protected List<Selection> makeFilters(MessageSource messageSource, Locale locale) {
		List<Selection> result = new ArrayList<Selection>();
		Selection sel;

		sel = new Selection(messageSource.getMessage("issue.filter.all.label", null, "All", locale), "");
		sel.addParametersPair(ITEMS_FILTER, Integer.valueOf(TrackerItemDao.TF_NOFILTER));
		result.add(sel);

		sel = new Selection(messageSource.getMessage("issue.filter.overdue.label", null, "Overdue", locale), "Overdue");
		sel.addParametersPair(ITEMS_FILTER, Integer.valueOf(TrackerItemDao.TF_OVERDUE));
		result.add(sel);

		return result;
	}
}
