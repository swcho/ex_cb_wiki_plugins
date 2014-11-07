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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.ecyrd.jspwiki.plugin.PluginManager;
import com.intland.codebeamer.wiki.CodeBeamerWikiContext;
import com.intland.codebeamer.wiki.WikiInlinedResourceProvider;
import com.intland.codebeamer.wiki.plugins.base.AbstractCodeBeamerWikiPlugin;

/**
 * A JSPWiki plugin that displays a customized image button widget.
 *
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 * @version $Id$
 */
public class ImageButtonPlugin extends AbstractCodeBeamerWikiPlugin {
	private static final String PLUGIN_TEMPLATE = "image-button-plugin.vm";

	private static final String PARAM_ICON = "icon";
	private static final String PARAM_TITLE = "title";
	private static final String PARAM_WIDTH = "width";
	private static final String PARAM_LINK = "link";
	private static final String DEFAULT_WIDTH = "347px";

	public String execute(WikiContext context, Map params) throws PluginException {
		String text = getParameter(params, PluginManager.PARAM_BODY);
		if(text != null) {
			// render body text (that is in markup format)
			text = context.getEngine().textToHTML(context, text);
		}
		String title = getParameter(params, PARAM_TITLE);
		String icon = getParameter(params, PARAM_ICON);
		String link = getParameter(params, PARAM_LINK);
		String width = getParameter(params, PARAM_WIDTH);

		return renderTag(context, title, text, width, icon, link);
	}

	/**
	 * Renders the ImageButton HTML content according to input parameters.
	 */
	private String renderTag(WikiContext context, String title, String text, String width, String icon, String link) {
		text = StringUtils.trimToEmpty(text);
		title = StringUtils.trimToNull(title);
		width = StringUtils.defaultIfEmpty(width, DEFAULT_WIDTH);

		icon = StringUtils.trimToNull(icon);
		String iconUrl = null;
		String iconText = null;
		if (icon != null) {
			iconUrl = resolveIconURL(context, icon);

			if (iconUrl == null) {
				// the icon is text
				iconText = icon;
			}
		}

		link = StringUtils.trimToNull(link);
		String linkHref = null;
		String linkOnclick = null;
		if (link != null) {
			// resolve as external url first
			URL url = null;
			try {
				url = new URL(link);
				linkHref = url.toExternalForm();
			} catch (MalformedURLException ex) {
				linkOnclick = context.getURL(WikiContext.EDIT, link);
				linkHref = "#";
			}
		}

		VelocityContext velocityContext = getDefaultVelocityContextFromContext(context);

		velocityContext.put("title", title);
		velocityContext.put("text", text);
		velocityContext.put("width", width);
		velocityContext.put("iconUrl", iconUrl);
		velocityContext.put("iconText", iconText);
		velocityContext.put("linkHref", linkHref);
		velocityContext.put("linkOnclick", linkOnclick);

		return renderPluginTemplate(PLUGIN_TEMPLATE, velocityContext);
	}

	private final String resolveIconURL(WikiContext wcontext, String reference) {
		reference = StringUtils.removeEnd(StringUtils.removeStart(reference.trim(), "!"), "!").trim();
		String imageUrl;
		try {
			imageUrl = WikiInlinedResourceProvider.getInstance().getArtifactOrAttachmentUrlByReference((CodeBeamerWikiContext) wcontext, reference);
		} catch (Throwable e) {
			// try to process as URL
			URL url = null;
			try {
				url = new URL(reference);
			} catch (MalformedURLException ex) {
				return null;
			}
			imageUrl = url.toExternalForm();
		}

		return imageUrl;
	}
}
