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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginException;

import com.intland.codebeamer.persistence.dao.impl.EntityCache;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.dto.base.VersionReferenceDto;
import com.intland.codebeamer.persistence.util.PersistenceUtils;

import com.intland.codebeamer.wiki.CodeBeamerWikiContext;
import com.intland.codebeamer.wiki.config.InterWikiLinkTemplate;
import com.intland.codebeamer.wiki.config.WikiConfig;
import com.intland.codebeamer.wiki.plugins.base.AbstractCodeBeamerWikiPlugin;
import com.intland.codebeamer.wiki.refs.TrackerItemReference;

/**
 * Plugin to generate a configurable link to an issue.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 * @version $Id$
 */
public class TaskPlugin extends AbstractCodeBeamerWikiPlugin {
	public String execute(WikiContext context, Map params) throws PluginException {
		UserDto user = getUserFromContext(context);

		String id = getParameter(params, "id");
		String trackerItemIds[] = StringUtils.split(id, ',');
		if (trackerItemIds.length == 0) {
			throw new PluginException("'id' is required");
		}

		// convert the legacy variable names to the new ones
		String prefixText = upgradeLegacyVariableName(getParameter(params, "label"));
		InterWikiLinkTemplate issueTemplate = WikiConfig.getInterWikiLinkTemplate("issue");
		String popupText  = upgradeLegacyVariableName(getParameter(params, "title", issueTemplate.getPopup()));
		String linkText   = upgradeLegacyVariableName(getParameter(params, "link",  issueTemplate.getLink()));

		InterWikiLinkTemplate templateFromParameters = new InterWikiLinkTemplate(null, linkText, popupText, null);

		String suffixText = upgradeLegacyVariableName(getParameter(params, "info"));

		List<TrackerItemDto> tasks = EntityCache.getInstance(user).get(TrackerItemDto.class, PersistenceUtils.grabIds(Arrays.asList(trackerItemIds)));
		StringBuilder html = new StringBuilder(tasks.size() * 200);

		for (Iterator<TrackerItemDto> it = tasks.iterator(); it.hasNext();) {
			TrackerItemDto task = it.next();
			TrackerItemReference taskLink = new TrackerItemReference(null, new VersionReferenceDto<TrackerItemDto>(task, null), null);
			taskLink.render((CodeBeamerWikiContext)context, task, templateFromParameters, prefixText, suffixText);
			html.append(elementToXmlString(taskLink));
			if (it.hasNext()) {
				html.append("<br>");
			}
		}

		return html.toString();
	}

	/**
	 * Replaces the old style variable names with the current ones
	 * supported by {@link InterwikiLinkRenderer} and returns the "upgraded" string.
	 */
	protected String upgradeLegacyVariableName(String s) {
		s = StringUtils.replace(s, "${start-date}", "${startDate}");
		s = StringUtils.replace(s, "${end-date}", "${endDate}");
		s = StringUtils.replace(s, "${estimated-hours}", "${estimatedHours}");
		s = StringUtils.replace(s, "${spent-hours}", "${spentHours}");

		return s;
	}
}
