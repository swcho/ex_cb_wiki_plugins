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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.Config;
import com.intland.codebeamer.manager.support.Crypter;
import com.intland.codebeamer.persistence.dao.impl.TrackerItemDaoImpl;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.scm.provider.svn.SvnJavaScmProvider;
import com.intland.codebeamer.wiki.plugins.base.AbstractCodeBeamerWikiPlugin;

/**
 * Plugin to show all issues referenced in commits to a specific Subversion repository branch (during a specific period).
 *
 * @author <a href="mailto:klaus.mehling@intland.com">Klaus Mehling</a>
 * @version $Id$
 */
public class SVNCommittedIssuesPlugin extends AbstractCodeBeamerWikiPlugin {
	public static final Logger log = Logger.getLogger(SVNCommittedIssuesPlugin.class);
	private static final String PLUGIN_TEMPLATE = "svn-committed-issues-plugin.vm";

	public final static String REPOSITORY_URL_PARAM = "repository";

	public String execute(WikiContext context, Map params) throws PluginException {
		UserDto user = getUserFromContext(context);

		String repository = getParameter(params, REPOSITORY_URL_PARAM);
		if (StringUtils.isNotBlank(repository)) {
			try {
				Date since = getDate(params, "since");
				Date until = getDate(params, "until");
				String username = getParameter(params, "user");
				String password = getParameter(params, "password");

				if (StringUtils.isBlank(username)) {
					username = user.getName();
				}

				if (StringUtils.isBlank(password) && !StringUtils.isBlank(user.getUnused1())) {
					password = Crypter.getInstance().decrypt(user.getUnused1(), Config.getCryptographyKey());
				}

				Set<Integer> issueIds = SvnJavaScmProvider.getCommittedIssues(repository, username, password, since, until);
				if (!issueIds.isEmpty()) {
					// Load committed issues
					List<TrackerItemDto> issues = TrackerItemDaoImpl.getInstance().findById(null, issueIds);

					// set up Velocity context
					VelocityContext velocityContext = getDefaultVelocityContextFromContext(context);
					velocityContext.put("contextPath", getContextPath(context)); // TODO why not using this? velocityContext.put("contextPath", getApplicationContextPath(context));
					velocityContext.put("repository", repository);
					velocityContext.put("since", since != null ? user.getDateTimeFormat().format(since) : null);
					velocityContext.put("until", until != null ? user.getDateTimeFormat().format(until) : null);
					velocityContext.put("issues", issues);

					// render template
					return renderPluginTemplate(PLUGIN_TEMPLATE, velocityContext);
				}
			} catch (Exception ex) {
				log.error(ex);
			}
		}

		return "<b>No 'repository' specified'</b>";

	}

	protected Date getDate(Map params, String param) throws ParseException {
		Date result = null;
		String strdate = StringUtils.trimToNull(getParameter(params, param));
		if (strdate != null) {
			if (strdate.length() == 10) {
				result = new SimpleDateFormat("yyyy-MM-dd").parse(strdate);
			} else if (strdate.length() == 16) {
				result = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(strdate);
			}
		}
		return result;
	}
}
