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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.manager.TrackerItemManager;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.TrackerItemRevisionDto;
import com.intland.codebeamer.persistence.dto.base.ProjectAwareDto;
import com.intland.codebeamer.security.ProjectRequestWrapper;
import com.intland.codebeamer.wiki.plugins.base.AutoWiringCodeBeamerPlugin;

/**
 * Plugin to create a TeamTrack like state change history graph for a task.
 *
 * @author <a href="mailto:Klaus.Mehling@intland.com">Klaus Mehling</a>
 */
public class TrackerItemStateHistoryPlugin extends AutoWiringCodeBeamerPlugin {
	public static final  Logger log = Logger.getLogger(TrackerItemStateHistoryPlugin.class);

	private static final String PLUGIN_TEMPLATE = "tracker-item-state-history-plugin.vm";

	/**
	 * Required parameter for the id of the issue
	 */
	public static final String PARAM_ID = "id";
	public static final String REVISION = "revision";

	/**
	 * Boolean parameter if the header is shown. Optional, defaults to true.
	 */
	public static final String PARAM_SHOW_HEADER = "showHeader";

	private TrackerItemManager trackerItemManager;

	/* (non-Javadoc)
	 * @see com.intland.codebeamer.wiki.plugins.base.AutoWiringCodeBeamerPlugin#populateContext(org.apache.velocity.VelocityContext, java.util.Map)
	 */
	@Override
	public void populateContext(VelocityContext velocityContext, Map params) throws PluginException {
		TrackerItemDto trackerItem = resolveTrackerItem(params);

		try {
			Integer revision = NumberUtils.createInteger(StringUtils.trimToNull(getParameter(params, REVISION)));
			List<TrackerItemRevisionDto> history = trackerItemManager.getStateChangeHistory(getUser(), trackerItem.getId(), false);
			
			if (revision != null && history != null && history.size() > 0) {
				for (Iterator<TrackerItemRevisionDto> it = history.iterator(); it.hasNext();) {
					if (it.next().getVersion().compareTo(revision) > 0) {
						it.remove();
					}
				}
			}

// since CB-5.8 the state itself is not initialized, so no assignees
//			if (history != null && history.size() > 0) {
//				HttpServletRequest request = getWikiContext().getHttpRequest();
//				RoleDecorator decorator = new RoleDecorator(request);
//
//				for (TrackerItemRevisionDto state : history) {
//					List<? extends NamedDto> assignees = state.getDto().getAssignedTo();
//					if (assignees != null && assignees.size() > 0) {
//						List<NamedDto> localized = new ArrayList<NamedDto>(assignees.size());
//						for (NamedDto assignee : assignees) {
//							if (assignee instanceof RoleDto) {
//								RoleDto role = decorator.localize((RoleDto)assignee);
//								role.setName(role.getDescription()); // We show the name !!
//								localized.add(role);
//							} else {
//								localized.add(assignee);
//							}
//						}
//						state.getTrackerItem().setAssignedTo(localized);
//					}
//				}
//			}

			// set up Velocity context
			velocityContext.put("item", trackerItem);
			velocityContext.put("history", history);

			boolean showHeader = true;
			if (params.get(PARAM_SHOW_HEADER) != null) {
				showHeader= Boolean.valueOf(getParameter(params, PARAM_SHOW_HEADER)).booleanValue();
			}
			velocityContext.put(PARAM_SHOW_HEADER, Boolean.valueOf(showHeader));
		} catch (Throwable ex) {
			log.error("Execute failed", ex);
			throw new PluginException("TrackerItemStateHistoryPlugin execute failed, because:" + ex.getMessage(), ex);
		}
	}

	/**
	 * Resolve the tracker-item's id from the params or find automatically if on a tracker-item page.
	 *
	 * @param params
	 * @return The tracker-item loaded
	 * @throws PluginException
	 */
	private TrackerItemDto resolveTrackerItem(Map params) throws PluginException {
		String trackerItemId = getParameter(params, PARAM_ID);
		if (StringUtils.isEmpty(trackerItemId)) {
			log.debug("Trying to automatically discover current issue");
			ProjectAwareDto projectAware = (ProjectAwareDto) getWikiContext().getHttpRequest().getAttribute(ProjectRequestWrapper.PROJECT_AWARE_DTO);
			if (projectAware instanceof TrackerItemDto) {
				trackerItemId = String.valueOf(((TrackerItemDto) projectAware).getId());
			}

			if (StringUtils.isEmpty(trackerItemId)) {
				throw new PluginException("Missing required " + PARAM_ID + " parameter for the issue id.");
			}
		}

		try {
			Integer id = Integer.valueOf(trackerItemId);
			TrackerItemDto trackerItem = trackerItemManager.findById(getUser(), id);
			if (trackerItem == null) {
				throw new PluginException("Can not find task with id=" + id);
			}
			return trackerItem;
		} catch (NumberFormatException ex) {
			throw new PluginException("Invalid " + PARAM_ID + " parameter value: "+ trackerItemId, ex);
		}
	}

	/* (non-Javadoc)
	 * @see com.intland.codebeamer.wiki.plugins.base.AutoWiringCodeBeamerPlugin#getTemplateFilename()
	 */
	@Override
	public String getTemplateFilename() {
		return PLUGIN_TEMPLATE;
	}

	/**
	 * @param trackerItemManager the trackerItemManager to set
	 */
	public void setTrackerItemManager(TrackerItemManager trackerItemManager) {
		this.trackerItemManager = trackerItemManager;
	}

}
