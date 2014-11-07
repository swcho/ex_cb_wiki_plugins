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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.persistence.dao.ProjectDao;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.RoleDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.dto.WikiPageDto;
import com.intland.codebeamer.persistence.util.Criteria;
import com.intland.codebeamer.persistence.util.Criterion;
import com.intland.codebeamer.persistence.util.PersistenceUtils;
import com.intland.codebeamer.persistence.util.Restrictions;
import com.intland.codebeamer.utils.CompareToIgnoreCase;
import com.intland.codebeamer.wiki.WikiMarkupProcessor;
import com.intland.codebeamer.wiki.plugins.base.AutoWiringCodeBeamerPlugin;
import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;

/**
 * Plugin to display the list of project.
 *
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 * @version $Id$
 */
public class ProjectListPlugin extends AutoWiringCodeBeamerPlugin {
	final static private Logger logger = Logger.getLogger(ProjectListPlugin.class);

	private static final String RENDER_TEMPLATE = "projectlist-plugin.vm";

	public static final String PROJECTS = "projects";
	protected static final String NAME_PARAM = "name";
	protected static final String CATEGORY_PARAM = "category";
	protected static final String FORMAT_PARAM = "format";
	protected static final String NAME_FORMAT = "name";
	protected static final String BRIEF_FORMAT = "brief";
	protected static final String FULL_FORMAT = "full";

	/* Project action strings. */
	protected static final String LEAVE_ACTION = "leave";
	protected static final String JOIN_ACTION = "join";

	private final static String PROJECT_ID_EQ = "project.proj_id";
	private final static String PROJECT_PROPAGATION= "project.propagation";

	/* Error message strings. */
	private static final String INCORRECT_PROJECT_ID = "Error parsing id parameter";
	private static final String REGEXP_ERROR = "Error parsing regexp";

	private ProjectDao projectDao;

	@Override
	public String getTemplateFilename() {
		return RENDER_TEMPLATE;
	}

	/* (non-Javadoc)
	 * @see com.intland.codebeamer.wiki.plugins.base.AutoWiringCodeBeamerPlugin#populateContext(org.apache.velocity.VelocityContext, java.util.Map)
	 */
	@Override
	public void populateContext(VelocityContext velocityContext, Map params) throws PluginException {
		// get beans from wiki context
		UserDto user = getUser();
		WikiPageDto page = getPage();
		HttpServletRequest httpRequest = getWikiContext().getHttpRequest();

		// Get all the projects available
		List<Integer> projectIds = new ArrayList();
		if (user != null) {
			List<ProjectDto> projects = projectManager.findAll(user, Boolean.FALSE);
			projectIds = PersistenceUtils.grabIds(projects);
		}
		// construct criteria
		Criteria criteria = new Criteria();
		if (projectIds != null && !projectIds.isEmpty()) {
			// fetch projects in the user is already a member and public ones
			List<Criterion> eitherInIdOrPublic = new ArrayList();
			eitherInIdOrPublic.add(Restrictions.in(PROJECT_ID_EQ, projectIds));
			eitherInIdOrPublic.add(Restrictions.isNotNull(PROJECT_PROPAGATION));

			criteria.addDisjunction(eitherInIdOrPublic);
		} else {
			criteria.add(Restrictions.isNotNull(PROJECT_PROPAGATION));
		}
		// search and group by category
		List<ProjectDto> projectsAvailable = projectDao.findByCriteria(criteria);

		// parse parameters
		String format = getParameter(params, FORMAT_PARAM, NAME_FORMAT);
		if (!format.equalsIgnoreCase(BRIEF_FORMAT) && !format.equalsIgnoreCase(FULL_FORMAT) && !format.equalsIgnoreCase(NAME_FORMAT)) {
			format = NAME_FORMAT;
		}
		String projectNameFilter = getStringParameter(params, NAME_PARAM, null);
		String projectCatFilter = getStringParameter(params, CATEGORY_PARAM, null);
		String projectIdFilter = getStringParameter(params, "id", null);

		// Prepare the projects list
		List<ProjectDto> projectsList = new ArrayList();
		if (projectIdFilter != null) {
			String[] ids = projectIdFilter.split(",");
			List<Integer> projectIdsToFilter = new ArrayList<Integer>();
			for (int i = 0; i < ids.length; i++) {
				try {
					projectIdsToFilter.add(Integer.valueOf(ids[i].trim()));
				} catch (Throwable ex) {
					logger.warn(ex);
				}
			}
			if (projectIdsToFilter.isEmpty()) {
				throw new NamedPluginException(this, INCORRECT_PROJECT_ID);
			}

			projectsList = projectManager.findById(user, projectIdsToFilter);
		} else if (projectNameFilter != null || projectCatFilter != null) {
			for (ProjectDto dto : projectsAvailable) {
				try {
					if (projectNameFilter != null ? dto.getName().matches(projectNameFilter)
							: false || projectCatFilter != null ? dto.getCategory().matches(projectCatFilter) : false) {
						projectsList.add(dto);
					}
				} catch (PatternSyntaxException e) {
					throw new NamedPluginException(this, REGEXP_ERROR, e);
				}
			}
		} else {
			projectsList.addAll(projectsAvailable);
		}

		Map<Integer,Map<UserDto,Set<RoleDto>>> projectMembers = projectManager.findMembersAndRoles(projectsList, null, null, false);

		// Prepare projects data
		List<ProjectEntry> projects = new ArrayList();
		for (ProjectDto project : projectsList) {
			// Format the project description
			String projectDescription = null;
			String df = project.getDescriptionFormat(); // Is null for plain text!
			if (WikiMarkupProcessor.TEXT_TYPE_WIKI.equals(df)) {
				projectDescription = WikiMarkupProcessor.getInstance().transformToHtml(httpRequest, project.getDescription(),
						WikiMarkupProcessor.TEXT_TYPE_WIKI, false, false, page, user);
			} else {
				projectDescription = project.getDescription();
			}

			// Find members
			Map<UserDto,Set<RoleDto>> members = projectMembers.get(project.getId());
			int membersCount = (members != null ? members.size() : 0);
			String action = JOIN_ACTION;

			if (membersCount > 0) {
				Set<RoleDto> userRoles = members.get(user);
				if (userRoles != null && !userRoles.isEmpty()) {
					action = LEAVE_ACTION;
					// check if user is project's admin
					for (RoleDto role : userRoles) {
						if (RoleDto.PROJECT_ADMIN.equals(role.getName())) {
							action = null; // admin can not leave the project
							break;
						}
					}
				}
			}

			projects.add(new ProjectEntry(project, projectDescription, membersCount, action));
		}

		Collections.sort(projects, new CompareToIgnoreCase());

		// set up Velocity context
		velocityContext.put(PROJECTS, projects);
		velocityContext.put("format", format);
		velocityContext.put("contextPath", httpRequest.getContextPath());
	}

	/**
	 * Helper class to collect project-related data.
	 */
	public class ProjectEntry {
		private ProjectDto project;
		private String description;
		private String action;
		private int membersCnt;

		public ProjectEntry(ProjectDto project, String description, int membersCnt, String action) {
			this.project = project;
			this.description = description;
			this.membersCnt = membersCnt;
			this.action = action;
		}

		public ProjectDto getProject() {
			return project;
		}

		public String getDescription() {
			return description;
		}

		public int getMembersCnt() {
			return membersCnt;
		}

		public String getAction() {
			return action;
		}

		public String toString() {
			return (project != null ? project.getName() : "") + " [" + StringUtils.defaultString(description) + "]";
		}
	}

	public void setProjectDao(ProjectDao projectDao) {
		this.projectDao = projectDao;
	}
}
