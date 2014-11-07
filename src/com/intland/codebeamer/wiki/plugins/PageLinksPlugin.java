/*
 * Copyright by Inland Software
 *
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Intland Software. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Intland.
 */
package com.intland.codebeamer.wiki.plugins;

import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.manager.EntityReferenceManager;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.dto.WikiPageDto;
import com.intland.codebeamer.persistence.dto.base.ReferenceDto;
import com.intland.codebeamer.wiki.plugins.base.AbstractCodeBeamerWikiPlugin;

/**
 * A JSPWiki plugin that shows a list of incoming and outgoung references
 * relative to wiki page.
 *
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 * @version $Id$
 */
public class PageLinksPlugin extends AbstractCodeBeamerWikiPlugin {
	private final static Logger log = Logger.getLogger(PageLinksPlugin.class);

	private static final String PLUGIN_TEMPLATE = "pagelinks-plugin.vm";

	private static final String PARAM_INCOMING = "showIncoming";
	private static final String PARAM_OUTGOING = "showOutgoing";

	private Boolean showIncoming = Boolean.TRUE;
	private Boolean showOutgoing = Boolean.TRUE;

	public String execute(WikiContext context, Map params) throws PluginException {
		// read parameters
		if (params.get(PARAM_INCOMING) != null) {
			showIncoming = Boolean.valueOf(getParameter(params, PARAM_INCOMING));
		}
		if (params.get(PARAM_OUTGOING) != null) {
			showOutgoing = Boolean.valueOf(getParameter(params, PARAM_OUTGOING));
		}

		if (!showIncoming.booleanValue() && !showOutgoing.booleanValue()) {
			if (log.isInfoEnabled()) {
				log.info("Nothing to display");
			}
			return null;
		}

		UserDto user = getUserFromContext(context);
		WikiPageDto page = getPageFromContext(context);
		ReferenceDto<WikiPageDto> pageRef = ReferenceDto.of(page);

		VelocityContext velocityContext = getDefaultVelocityContextFromContext(context);
		velocityContext.put("page", page);

		if (showIncoming.booleanValue()) {
			velocityContext.put("incoming", EntityReferenceManager.getInstance().findAndResolveByTarget(user, pageRef, null));
		}

		if (showOutgoing.booleanValue()) {
			velocityContext.put("outgoing", EntityReferenceManager.getInstance().findAndResolveBySource(user, pageRef, null));
		}

		return renderPluginTemplate(PLUGIN_TEMPLATE, velocityContext);
	}
}
