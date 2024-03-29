/*
	JSPWiki - a JSP-based WikiWiki clone.

	Copyright (C) 2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

	This program is free software; you can redistribute it and/or modify
	it under the terms of the GNU Lesser General Public License as published by
	the Free Software Foundation; either version 2.1 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public License
	along with this program; if not, write to the Free Software
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.intland.codebeamer.wiki.plugins;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.attachment.AttachmentManager;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.ecyrd.jspwiki.plugin.WikiPlugin;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.intland.codebeamer.controller.ControllerUtils;
import com.intland.codebeamer.wiki.CodeBeamerWikiContext;
import com.intland.codebeamer.wiki.WikiMarkupProcessor;

/**
 * <strong> This plugin was derived from the original JSPWiki plugin called
 * "Image". The only addition is that it supports new syntaxes in its "src"
 * parameter. </strong>
 *
 * Provides an image plugin for better control than is possible with a simple
 * image inclusion.
 *
 * @author Janne Jalkanen
 * @since 2.1.4.
 */
// FIXME: It is not yet possible to do wiki internal links. In order to
// do this cleanly, a TranslatorReader revamp is needed.

public class ImagePlugin implements WikiPlugin {

	private final static Logger logger = Logger.getLogger(ImagePlugin.class);

	public static final String PARAM_SRC = "src";
	public static final String PARAM_ALIGN = "align";
	public static final String PARAM_CAPTION = "caption";
	public static final String PARAM_LINK = "link";
	public static final String PARAM_STYLE = "style";
	public static final String PARAM_CLASS = "class";

	// Instead of an image url the Image-plugin can use an image defined by this wiki markup.
	// For example [!apple.png!] will be resolved by the current entity's attached [!apple.png!]
	public static final String PARAM_WIKI = "wiki";

	@Autowired
	private WikiMarkupProcessor wikiMarkupProcessor;

	/**
	 * This method is used to clean away things like quotation marks which a
	 * malicious user could use to stop processing and insert javascript.
	 */
	private static final String getCleanParameter(Map params, String paramId) {
		return TextUtil.replaceEntities((String) params.get(paramId));
	}

	@Override
	public String execute(WikiContext context, Map params) throws PluginException {
		ControllerUtils.autoWire(this, context.getHttpRequest());

		// make a copy of params map, because we'll modify this
		params = new LinkedHashMap(params);

		WikiEngine engine = context.getEngine();
		String src = getCleanParameter(params, PARAM_SRC);
		String align = getCleanParameter(params, PARAM_ALIGN);
		String caption = getCleanParameter(params, PARAM_CAPTION);
		String link = getCleanParameter(params, PARAM_LINK);
		String style = getCleanParameter(params, PARAM_STYLE);
		String cssClass = getCleanParameter(params, PARAM_CLASS);

		String wiki = (String) params.get(PARAM_WIKI);
		String wikiImageAttributes = null;
		if (wiki != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Resolving wiki reference to an image url. wiki:<" + wiki +">");
			}
			try {
				wikiImageAttributes = resolveWikiRerenceOfImage(context, wiki);

				if (logger.isDebugEnabled()) {
					logger.debug("Resolved wiki reference to an image with attributes <" + wikiImageAttributes +">");
				}
			} catch (Throwable th) {
				logger.warn("Can not resolve wiki reference to an image: <" + wiki +">", th);
			}
		}

		if (src == null && wikiImageAttributes == null) {
			throw new PluginException("Parameter 'src' or 'wiki' is required for Image plugin");
		}

		if (src != null) {
			// CB-SPE
			if (StringUtils.indexOf(src, "CB:") == 0) {
				// resolve CB: interwiki references
				src = StringUtils.replace(src, "CB:", context.getHttpRequest().getContextPath());
			} else if (StringUtils.indexOf(src, "../..") == 0) {
				// resolve "local" URLs generated by #24201
				src = StringUtils.replace(src, "../..", context.getHttpRequest().getContextPath());
			}
			// CB-SPEC

			try {
				AttachmentManager mgr = engine.getAttachmentManager();
				Attachment att = mgr.getAttachmentInfo(context, src);

				if (att != null) {
					src = context.getURL(WikiContext.ATTACH, att.getName());
				}
			} catch (ProviderException e) {
				throw new PluginException("Attachment info failed: " + e.getMessage());
			}
			params.put(PARAM_SRC, src); // write back
		}

		if (cssClass == null) {
			cssClass = "imageplugin";
			params.put(PARAM_CLASS, cssClass); // write back, so pass-through will work below
		}

		StringBuffer result = new StringBuffer(200);

		// only wrap in a table if really necessary for the caption or alignment properties
		final boolean centerAligned = "center".equals(align);
		final boolean tableRequired = caption != null || centerAligned;

		if (tableRequired) {
			result.append("<table border=\"0\" class=\"" + cssClass + "\" ");
		}
		// if( align != null ) result.append(" align=\""+align+"\"");
		// if( style != null ) result.append(" style=\""+style+"\"");

		//
		// Do some magic to make sure centering also work on FireFox
		//
		if (style != null && style.length() > 0) {
			// Make sure that we add a ";" to the end of the style string
			if (style.charAt(style.length() - 1) != ';') {
				style += ";";
			}
		}

		if (centerAligned) {
			style = (style == null ? "" : style) + " margin-left: auto; margin-right: auto;";
			params.put(PARAM_STYLE, style);	// write back the changed, so pass-through will work below
		}

		if (tableRequired) {
			if (style != null) {
				result.append(" style=\"" + style +"\" ");
			}

			if (align != null && !(align.equals("center"))) {
				result.append(" align=\"" + align + "\"");
			}

			result.append(">\n");
		}

		if (tableRequired) {
			if (caption != null) {
				result.append("<caption align='bottom'>" + TextUtil.replaceEntities(caption) + "</caption>\n");
			}
			result.append("<tr><td>");
		}

		if (link != null) {
			result.append("<a href=\"" + link + "\">");
		}

		result.append("<img ");
		final List<String> NOT_EXPORTED_ATTRIBUTES = Arrays.asList(PARAM_WIKI, PARAM_CAPTION);
		// pass through all attributes of the plugin to the image tag
		for (Object key: params.keySet()) {
			if (! NOT_EXPORTED_ATTRIBUTES.contains(key)) {
				String value = getCleanParameter(params, (String) key);
				if (value != null) {
					result.append(" " + key + "=\"" + value + "\"");
				}
			}
		}

		if (! StringUtils.isBlank(wikiImageAttributes)) {
			// TODO: if the image plugin and the wiki both has same attributes: then we hope the browser will resolve this some way...
			result.append(" "  + wikiImageAttributes +" ");
		}

		result.append(" />"); // closing the image tag
		if (link != null) {
			result.append("</a>");
		}
		if (tableRequired) {
			result.append("</td></tr>\n");
			result.append("</table>\n");
		}

		return result.toString();
	}

	private final static Pattern EXTRACT_IMAGE = Pattern.compile("<img\\s?(.*?)/?>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

	/**
	 * Try to resolve the wiki reference
	 * @param context
	 * @param wiki
	 * @return The image attributes
	 */
	private String resolveWikiRerenceOfImage(WikiContext context, String wiki) {
		if (! (context instanceof CodeBeamerWikiContext)) {
			return null;
		}

		// TODO: bug here ?, should this create a new wiki context to avoid SectionParser confusion ?
		/*
		TODO: should use a new wiki context lik this ?
			use a separate wiki context so section parsing won't be broken
			CodeBeamerWikiContext newContext = new CodeBeamerWikiContext(_cbWikiContext.getHttpRequest(), _cbWikiContext.getProject(), _cbWikiContext.getOwner(), _cbWikiContext.getUser());
		 */

		CodeBeamerWikiContext cbWikiContext = (CodeBeamerWikiContext) context;
		final boolean useExternalLinks = cbWikiContext.isUseExternalLinks();
		final boolean useOutlinkImage = cbWikiContext.isUseOutLinkImage();
		try {
			// don't show any image decorations, this would result that wrong image is picked from the html
			cbWikiContext.setUseExternalLinks(false);
			cbWikiContext.setUseOutlinkImage(false);

			String html = wikiMarkupProcessor.transformToHtml(wiki, WikiMarkupProcessor.TEXT_TYPE_WIKI, false, false, cbWikiContext);

			if (logger.isDebugEnabled()) {
				logger.debug("Converted wiki to html:<" + html +">, will extract 1st image from that");
			}
			Matcher matcher = EXTRACT_IMAGE.matcher(html);
			if (matcher.find()) {
				String attributes = matcher.group(1);
				if (logger.isDebugEnabled()) {
					logger.debug("Extracted src of image:<" + attributes +">");
				}
				return attributes;
			}
			return null;
		} finally {
			// restore original settings was changed temporarily
			cbWikiContext.setUseExternalLinks(useExternalLinks);
			cbWikiContext.setUseOutlinkImage(useOutlinkImage);
		}
	}

	/**
	 * Builds the markup of the image plugin when it is used for keeping the size of an wiki-referenced image
	 * @param wikiRef The wiki reference of an image, typically like [!apple.jpg!]
	 * @param dimension The dimensions of the image
	 *
	 * @return The image plugin wiki markup contains all info above
	 */
	public static String wrapWikiRefToImagePlugin(String wikiRef, Dimension dimension) {
		if (wikiRef == null || dimension == null) {
			return wikiRef;
		}
		Map<String,String> attrs = new LinkedHashMap<String, String>();
		attrs.put(ImagePlugin.PARAM_WIKI, wikiRef);
		attrs.put("width", String.valueOf((int) dimension.getWidth()));
		attrs.put("height", String.valueOf((int) dimension.getHeight()));
		return buildImagePlugin(attrs);
	}

	/**
	 * Helps to build image plugin using attributes
	 * @param attrs
	 * @return The image plugin with the desired attributes. Null values are omitted.
	 */
	public static String buildImagePlugin(Map<String,String> attrs) {
		if (attrs == null || attrs.isEmpty()) {
			return null;
		}
		StringBuffer buf = new StringBuffer();
		for (String attr: attrs.keySet()) {
			String value = attrs.get(attr);
			if (value != null) {
				buf.append(" ").append(attr).append("='");
				value = value.replaceAll("'", "\\'"); // escape the '-s
				buf.append(value);
				buf.append("'");
			}
		}
		if (buf.length() == 0) {
			return null;
		}
		return "[{Image" + buf + " }]";
	}

}
