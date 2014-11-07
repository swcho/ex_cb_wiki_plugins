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
import com.intland.codebeamer.persistence.dto.ArtifactDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.dto.base.VersionReferenceDto;
import com.intland.codebeamer.persistence.util.PersistenceUtils;

import com.intland.codebeamer.wiki.CodeBeamerWikiContext;
import com.intland.codebeamer.wiki.config.InterWikiLinkTemplate;
import com.intland.codebeamer.wiki.config.WikiConfig;
import com.intland.codebeamer.wiki.plugins.base.AbstractCodeBeamerWikiPlugin;
import com.intland.codebeamer.wiki.refs.ArtifactReference;

/**
 * Plugin to generate a configurable link to a document.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 * @version $Id$
 */
public class DocumentPlugin extends AbstractCodeBeamerWikiPlugin {
	public String execute(WikiContext context, Map params) throws PluginException {
		UserDto user = getUserFromContext(context);

		String id = getParameter(params, "id");
		String artifactIds[] = StringUtils.split(id, ',');
		if(artifactIds.length == 0) {
			throw new PluginException("'id' is required");
		}

		String prefixText = getParameter(params, "label");
		InterWikiLinkTemplate template = WikiConfig.getInterWikiLinkTemplate("doc");
		String popupText  = getParameter(params, "title", template.getPopup());
		String linkText   = getParameter(params, "link",  template.getLink());
		String suffixText = getParameter(params, "info");

		InterWikiLinkTemplate templateFromParams = new InterWikiLinkTemplate(null, linkText, popupText, null);

		List<ArtifactDto> docs = EntityCache.getInstance(user).get(ArtifactDto.class, PersistenceUtils.grabIds(Arrays.asList(artifactIds)));
		StringBuilder html = new StringBuilder(docs.size() * 200);

		for (Iterator<ArtifactDto> it = docs.iterator(); it.hasNext();) {
			ArtifactDto doc = it.next();
			ArtifactReference docLink = new ArtifactReference(null, new VersionReferenceDto<ArtifactDto>(doc, null), null);
			docLink.render((CodeBeamerWikiContext)context, doc, templateFromParams, prefixText, suffixText);
			html.append(elementToXmlString(docLink));
			if (it.hasNext()) {
				html.append("<br>");
			}
		}

		return html.toString();
	}
}
