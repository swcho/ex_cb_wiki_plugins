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

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginManager;
import com.intland.codebeamer.manager.WikiPageManager;
import com.intland.codebeamer.text.html.HtmlCleaner;
import com.intland.codebeamer.utils.Common;
import com.intland.codebeamer.wiki.CodeBeamerWikiContext;
import com.intland.codebeamer.wiki.plugins.base.AbstractArtifactAwareWikiPlugin;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import java.io.InputStream;
import java.util.Map;

/**
 * Usage:
[{Html

<b>bold</b> normal <em>italic</em> end
}]

 * or [{Html id=2124}] to get the content of document 2124.
 * <strong>WARNING: this plugin might cause serious security problems because for example XSS attack is possible.</strong>
 *
 * @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 */
public class HtmlPlugin extends AbstractArtifactAwareWikiPlugin {
	private Integer docId;
	protected String src;
	private String body;
	private String encoding = "ISO-8859-1";

	public String execute(WikiContext wcontext, Map params) {
		processParameters(params);

		CodeBeamerWikiContext context = (CodeBeamerWikiContext)wcontext;

		try {
			if (docId != null || src != null) {
				// Using CodeBeamer document.
				InputStream inputStream = getInputStream(context);

				if (isYoutubeLink(src)) {
					body = "<iframe src=\"" + src + "\"></iframe>";
				} else {
					body = Common.readFileToString(inputStream, encoding);
				}
			} else if ((src = getParameter(params, "message")) != null) {
				body = WikiPageManager.getInstance().getText(wcontext.getHttpRequest().getLocale(), src, body);
			}
			body = StringUtils.remove(body, (char)0);

			if (context.isCleanupHtml()) {
				body = HtmlCleaner.dropSpecialHtmlTags(body);
			}

			return StringUtils.defaultString(body);
		} catch (Exception ex) {
			return StringEscapeUtils.escapeHtml(ex.getMessage());
		}
	}

	protected void processParameters(Map params) {
		try {
			docId = Integer.valueOf(getParameter(params, "id"));
		} catch (Exception ex) {
		}

		encoding = getParameter(params, "encoding", encoding);
		src = StringUtils.trimToNull(getParameter(params, "src"));
		src = StringUtils.replace(src, "&amp;", "&");

		body = StringUtils.trimToEmpty((String)params.get(PluginManager.PARAM_BODY));
	}

	private boolean isYoutubeLink(String url) {
		return url != null && url.matches(HtmlCleaner.YOUTUBE_URL_PATTERN);
	}

	protected Integer getArtifactId() {
		return docId;
	}

	protected String getIconName() {
		return "html.gif";
	}

	protected String getSrc() {
		return src;
	}
}
