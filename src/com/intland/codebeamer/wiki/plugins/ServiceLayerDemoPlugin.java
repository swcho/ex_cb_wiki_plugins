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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.velocity.VelocityContext;

import com.ecyrd.jspwiki.WikiContext;
import com.intland.codebeamer.manager.TrackerItemManager;
import com.intland.codebeamer.persistence.dao.TrackerItemDao;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.TrackerDto;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.dto.WikiPageDto;
import com.intland.codebeamer.persistence.dto.TrackerItemDto.Flag;
import com.intland.codebeamer.persistence.util.Criteria;
import com.intland.codebeamer.wiki.plugins.base.AbstractCodeBeamerWikiPlugin;

/**
 * Plugin to demonstrate using the Service Layer in CodeBeamer.
 * Only for education purposes.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 * @version $Id: zsolt 2009-11-27 19:54 +0100 23955:cdecf078ce1f  $
 */
public class ServiceLayerDemoPlugin extends AbstractCodeBeamerWikiPlugin {
	public String execute(WikiContext context, Map params) {
		// get beans from wiki context
		UserDto user = getUserFromContext(context);
		WikiPageDto page = getPageFromContext(context);

		// parse params (they default to false)
		List<Integer> fieldsIds = new ArrayList<Integer>(3);
		if (Boolean.parseBoolean((String)params.get("assigedTo"))) {
			fieldsIds.addAll(TrackerItemDao.ASSIGNED_TO);
		}
		if (Boolean.parseBoolean((String)params.get("submittedBy"))) {
			fieldsIds.addAll(TrackerItemDao.SUBMITTED_BY);
		}
		if (Boolean.parseBoolean((String)params.get("supervisedBy"))) {
			fieldsIds.addAll(TrackerItemDao.SUPERVISED_BY);
		}

		// find all trackers in the current project
		List<ProjectDto> projects = Collections.singletonList(page.getProject());
		List<TrackerDto> trackers = null; //TrackerManager.getInstance().findByProject(user, page.getProject());
		Boolean cmdb = Boolean.FALSE; // Only show tracker items, no CMDB configuration items
		Set<Flag> flags = EnumSet.of(Flag.Deleted); // Do not show deleted items
		Criteria criteria = null; // No additional criteria

		// find all tracker items in those trackers that are assigned-to or submitted-by by the current user
		List<TrackerItemDto> trackerItems = TrackerItemManager.getInstance().findByOwner(user, fieldsIds, user, false, projects, trackers, cmdb, flags, criteria, TrackerItemDao.TF_NOFILTER);

		// set up Velocity context
		VelocityContext velocityContext = getDefaultVelocityContextFromContext(context);
		velocityContext.put("trackerItems", trackerItems);

		// render template
		return renderPluginTemplate("servicelayerdemo-plugin.vm", velocityContext);
	}
}
