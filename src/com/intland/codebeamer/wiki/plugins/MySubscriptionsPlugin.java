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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.velocity.VelocityContext;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.manager.TrackerManager;
import com.intland.codebeamer.persistence.dto.TrackerDto;
import com.intland.codebeamer.persistence.dto.TrackerTypeDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.wiki.plugins.base.AutoWiringCodeBeamerPlugin;

/**
 * Plugin to display a short summary on user's subscriptions.
 *
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 * @version $Id$
 */
public class MySubscriptionsPlugin extends AutoWiringCodeBeamerPlugin {
	private static final boolean ONLY_SUBSCRIBED = true;

	private static final String RENDER_TEMPLATE = "mysubscriptions-plugin.vm";

	private TrackerManager trackerManager;

	@Override
	public String getTemplateFilename() {
		return RENDER_TEMPLATE;
	}

	public void populateContext(VelocityContext velocityContext, Map params) throws PluginException {
		// get beans from wiki context
		UserDto user = super.getUser();

		// get subscribed entities
		// FIXME these subscriptions are temporarily turned off, see https://codebeamer.com/cb/issue/24032
		// List wikiPages = ArtifactDaoImpl.getInstance().findSubscribedBy(user, new Integer(ArtifactType.PROJECT_WIKIPAGE));
		// List documents = ArtifactDaoImpl.getInstance().findSubscribedBy(user, new Integer(ArtifactType.FILE));
		List<TrackerDto> trackers = trackerManager.findByUser(user, user.getId(), ONLY_SUBSCRIBED);
		// List trackerItems = TrackerItemDaoImpl.getInstance().findSubscribedBy(user);
		List<TrackerDto> forums = new ArrayList<TrackerDto>();

		if (trackers != null && trackers.size() > 0) {
			for (Iterator<TrackerDto> it = trackers.iterator(); it.hasNext();) {
				TrackerDto tracker = it.next();
				if (TrackerTypeDto.FORUM_POST.isInstance(tracker)) {
					it.remove();
					forums.add(tracker);
				}
			}
		}

		// velocityContext.put("wikiPages", wikiPages);
		// velocityContext.put("documents", documents);
		velocityContext.put("trackers", trackers);
		// velocityContext.put("trackerItems", trackerItems);
		velocityContext.put("forums", forums);
		velocityContext.put("contextPath", getApplicationContextPath(getWikiContext()));
	}

	public void setTrackerManager(TrackerManager trackerManager) {
		this.trackerManager = trackerManager;
	}
}
