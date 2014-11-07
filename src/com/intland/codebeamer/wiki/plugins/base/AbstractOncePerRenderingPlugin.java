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
package com.intland.codebeamer.wiki.plugins.base;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.ecyrd.jspwiki.WikiContext;

/**
 * Wiki plugin that cannot be executed recursively. This is to prevent stack overflow
 * problems in nasty situations like using the ProjectInfo plugin in a project descriptor.
 * <p>
 * Impl. note: it is using the request object to store the "guard variable", because
 * the wiki context can possibly be different(!) at each call level when rendering recursively.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 * @version $Id$
 */
public abstract class AbstractOncePerRenderingPlugin extends AbstractCodeBeamerWikiPlugin {
	private final String RECURSIVE_RENDERING_GUARD_VARIABLE = "_recursiveRenderingGuard-" + getClass().getName();

	public String execute(WikiContext context, Map params) {
		HttpServletRequest request = context.getHttpRequest();

		// return a blank string when recursively called
		if(request.getAttribute(RECURSIVE_RENDERING_GUARD_VARIABLE) != null) {
			return "";
		}

		request.setAttribute(RECURSIVE_RENDERING_GUARD_VARIABLE, "xxx");
		String result = executeOncePerRendering(context, params);
		request.removeAttribute(RECURSIVE_RENDERING_GUARD_VARIABLE); // FIXME should be in finally{}

		return result;
	}

	protected abstract String executeOncePerRendering(WikiContext context, Map params);
}
