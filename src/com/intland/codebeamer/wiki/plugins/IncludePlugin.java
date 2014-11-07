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

import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.ecyrd.jspwiki.plugin.PluginManager;
import com.intland.codebeamer.manager.WikiPageManager;
import com.intland.codebeamer.persistence.dao.ArtifactDao;
import com.intland.codebeamer.persistence.dao.impl.ArtifactDaoImpl;
import com.intland.codebeamer.persistence.dto.ArtifactDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.dto.WikiPageDto;
import com.intland.codebeamer.remoting.DescriptionFormat;
import com.intland.codebeamer.utils.Common;
import com.intland.codebeamer.utils.TemplateRenderer.Parameters;
import com.intland.codebeamer.wiki.CodeBeamerWikiContext;
import com.intland.codebeamer.wiki.WikiMarkupProcessor;
import com.intland.codebeamer.wiki.plugins.base.AbstractArtifactAwareWikiPlugin;
import com.intland.codebeamer.wiki.refs.ImageReference;

/**
 * Plugin to include wiki markup from an external source in a wiki page.
 * <p>
 * External sources can be either codeBeamer documents
 * (see the parameter <code>id</code>), or URLs (see the parameter <code>src</code>)
 * that point to wiki markup resources.
 *
 * @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 * @version $Id$
 */
public class IncludePlugin extends AbstractArtifactAwareWikiPlugin {
	private static final Logger logger = Logger.getLogger(IncludePlugin.class);

	private final static String INCLUDED_DOC_IDS = IncludePlugin.class.getName() + "-included_doc_ids";
	private final static String SHOW_CONTROL = IncludePlugin.class.getName() + "-show-control";
	public final static String MANDATORY_PARAM_MESSAGE = "Either <tt>id</tt> or <tt>src</tt> must be provided";
	public final static String DOCUMENT_ID = "id";
	public final static String SRC = "src";

	private final WikiMarkupProcessor wikiMarkupProcessor = WikiMarkupProcessor.getInstance();
	private final ArtifactDao artifactDao = ArtifactDaoImpl.getInstance();
	private final WikiPageManager wikiPageManager = WikiPageManager.getInstance();

	private Integer pageId;
	private String srcUrl;

	public String execute(WikiContext wcontext, Map params) throws PluginException {
		String body = null;
		String pageIdString = getParameter(params, DOCUMENT_ID);

		if (pageIdString != null) {
			try {
				pageId = Integer.valueOf(pageIdString);
			} catch (Throwable ex) {
				String msg = ex.toString();
				logger.warn(msg);
				return StringEscapeUtils.escapeHtml(msg);
			}
		}

		if (pageId == null) {
			if ((srcUrl = getParameter(params, SRC)) != null) {
				try {
					File path = new File(wcontext.getEngine().getServletContext().getRealPath(srcUrl));
					if (path.canRead()) {
						srcUrl = path.getAbsolutePath();
					}
				} catch (Throwable ex) {
				}
			} else if ((srcUrl = getParameter(params, "template")) != null) {
				body = wikiPageManager.getTemplateContent(wcontext.getHttpRequest(), srcUrl);
				body = templateRenderer.render(new StringReader(body), null, new Parameters(wcontext.getHttpRequest().getLocale(), false));

			} else if ((srcUrl = getParameter(params, "message")) != null) {
				body = StringUtils.trimToNull((String)params.get(PluginManager.PARAM_BODY));
				body = wikiPageManager.getText(wcontext.getHttpRequest().getLocale(), srcUrl, body);
			}
		}

		Object source = pageId == null ? srcUrl : pageId;
		if (source == null) {
			return MANDATORY_PARAM_MESSAGE;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Source <" + source + ">");
		}

		CodeBeamerWikiContext context = (CodeBeamerWikiContext)wcontext;

		HttpServletRequest request = context.getHttpRequest();

		// Request might be null in case of email notifications.
		if (request == null) {
			logger.info("No request is available");

			String img = "<img src=\"" + ImageReference.EXTERNAL_URL + "\" />";
			return img;
		}

		UserDto user = getUserFromContext(context);
		boolean showControl = false;

		try {
			showControl = ((Boolean)request.getAttribute(SHOW_CONTROL)).booleanValue();
		} catch (Throwable ex) {
			try {
				showControl = Boolean.parseBoolean(getParameter(params, "control", "false"));
			} catch (Throwable ex2) {
			}
			request.setAttribute(SHOW_CONTROL, Boolean.valueOf(showControl));
		}

		Map<Object,String> processedIncludes = (Map<Object,String>)request.getAttribute(INCLUDED_DOC_IDS);
		if (processedIncludes == null) {
			processedIncludes = new HashMap<Object, String>();
			request.setAttribute(INCLUDED_DOC_IDS, processedIncludes);
		} else if (processedIncludes.containsKey(source)) {
			logger.warn("Source <" + source + "> has been already processed");

			return processedIncludes.get(source);
		}

		StringBuilder html = new StringBuilder();

		if (showControl && pageId != null) {
			if (!writeControls(context.getHttpRequest().getContextPath(), context.getUser(),  pageId, html)) {
				String result = "";
				processedIncludes.put(source, result);

				return result;
			}
		}

		try {
			if (body == null) {
				InputStream inputStream = getInputStream(context);
				body = Common.readFileToString(inputStream, null);
				body = StringUtils.remove(body, (char)0);
			}

			WikiPageDto includedWikiPage = (pageId != null) ? wikiPageManager.findById(user, pageId) : null;
			if(includedWikiPage != null) {
				String includedHtml = wikiMarkupProcessor.transformToHtml(context.getHttpRequest(), body, DescriptionFormat.WIKI, false, includedWikiPage, user);
				html.append(includedHtml);
			} else {
				String includedHtml = wikiMarkupProcessor.transformToHtml(context.getHttpRequest(), body, DescriptionFormat.WIKI);
				html.append(includedHtml);
			}

			String result = html.toString();
			processedIncludes.put(source, result);

			return result;
		} catch (Throwable ex) {
			String msg = ex.toString();

			logger.warn(msg, ex);

			return StringEscapeUtils.escapeHtml(msg);
		}
	}

	protected boolean writeControls(String contextPath, UserDto user, Integer id, StringBuilder html) {
		ArtifactDto doc = artifactDao.findById(user, id);

		if (doc == null) {
			return false;
		}

		html.append("<a href=\"" + contextPath + doc.getUrlLink() + "\">" + StringEscapeUtils.escapeHtml(doc.getName()) + "</a>");
//		html.append(" <img src=\"" + contextPath +  doc.getIconUrl() + "\" />");
		html.append("<br />");

		return true;
	}

	/**
	 * Unused.
	 */
	@Override
	protected Integer getArtifactId() {
		return pageId;
	}

	/**
	 * Unused.
	 */
	@Override
	protected String getIconName() {
		return null;
	}

	/**
	 * Unused.
	 */
	@Override
	protected String getSrc() {
		return srcUrl;
	}
}
