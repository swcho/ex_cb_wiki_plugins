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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.datatransfer.MimeTargetWindow;
import com.intland.codebeamer.manager.ArtifactManager;
import com.intland.codebeamer.persistence.dao.PaginatedDtoList;
import com.intland.codebeamer.persistence.dto.ArtifactDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.dto.WikiPageDto;
import com.intland.codebeamer.persistence.util.ArtifactRevision;
import com.intland.codebeamer.remoting.ArtifactType;
import com.intland.codebeamer.taglib.MimeIcon;
import com.intland.codebeamer.utils.FancyDate;
import com.intland.codebeamer.wiki.plugins.base.AbstractCodeBeamerWikiPlugin;

/**
 * A plugin that displays a list of recent page comments.
 *
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 * @version $Id: zsolt 2009-11-27 19:54 +0100 23955:cdecf078ce1f  $
 */
public class CommentsPlugin extends AbstractCodeBeamerWikiPlugin {
	private final static Logger log = Logger.getLogger(CommentsPlugin.class);

	private static final String PLUGIN_TEMPLATE = "comments-plugin.vm";

	private static final String PARAM_LIMIT = "limit";
	private static final String PARAM_ATTACHMENT = "attachment";

	// Not more that five comments by default.
	private Integer limit = new Integer(5);
	private Boolean showAttachments = Boolean.FALSE;
	private boolean limited = false;

	public String execute(WikiContext context, Map params) throws PluginException {
		String paramLimit = getParameter(params, PARAM_LIMIT);
		showAttachments = Boolean.valueOf(getParameter(params, PARAM_ATTACHMENT));

		if (NumberUtils.isNumber(paramLimit)) {
			if (log.isDebugEnabled()) {
				log.debug("Use limit: " + paramLimit);
			}
			limit = NumberUtils.createInteger(paramLimit);
		}

		UserDto user = getUserFromContext(context);
		WikiPageDto page = getPageFromContext(context);
		List<ArtifactRevision<ArtifactDto>> comments = getPageComments(user, page);

		if (comments == null || comments.isEmpty()) {
			if (log.isDebugEnabled()) {
				log.debug("No page comments.");
			}
			return null;
		}

		List<Map<String,Object>> processed = processComments(context, comments);

		VelocityContext velocityContext = getDefaultVelocityContextFromContext(context);
		velocityContext.put("page", page);
		velocityContext.put("comments", processed);
		velocityContext.put("limited", Boolean.valueOf(limited));
		velocityContext.put("wikiContext", context);
		return renderPluginTemplate(PLUGIN_TEMPLATE, velocityContext);
	}

	/**
	 * Returns a list of comments processed to show in the template
	 */
	private List<Map<String,Object>> processComments(WikiContext context, List<ArtifactRevision<ArtifactDto>> comments) {
		List<Map<String,Object>> result = new ArrayList<Map<String,Object>>(comments.size());
		UserDto user = getUserFromContext(context);

		for (ArtifactRevision<ArtifactDto> comment : comments) {
			ArtifactDto dto = comment.getDto();

			Map<String,Object> data = new HashMap<String,Object>();

			data.put("id", dto.getId());
			data.put("description", dto.getDescription());
			data.put("descriptionFormat", dto.getDescriptionFormat());
			data.put("submitter",   dto.getOwner());
			data.put("submittedAt", new FancyDate(dto.getCreatedAt(), user, false));
			if (showAttachments.booleanValue() && dto.getFileSize() != null) {
				ServletContext servlet = getServletContext(context);
				String mimeType = (servlet != null ? servlet.getMimeType(StringUtils.defaultString(dto.getName()).toLowerCase()) : dto.getMimeType());

				data.put("icon",     MimeIcon.getIconName(mimeType));
				data.put("target",   MimeTargetWindow.getTarget(context.getHttpRequest(), mimeType));
				data.put("filename", dto.getName());
				data.put("size",     dto.getFileSize());
			}
			result.add(data);
		}

		return result;
	}

	/**
	 * Returns the limited list of page comments.
	 */
	private List<ArtifactRevision<ArtifactDto>> getPageComments(UserDto user, WikiPageDto parent) {
		PaginatedDtoList<ArtifactDto> page = new PaginatedDtoList<ArtifactDto>(1, limit.intValue(), Collections.EMPTY_LIST, 0);
		PaginatedDtoList<ArtifactRevision<ArtifactDto>> result = ArtifactManager.getInstance().findChildRevisions(user, null, parent, Collections.singletonList(Integer.valueOf(ArtifactType.ATTACHMENT)), null, page);
		if (result != null) {
			limited = result.getList().size() < result.getFullListSize();
			return result.getList();
		}
		return Collections.emptyList();
	}
}
