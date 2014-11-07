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
package com.intland.codebeamer.wiki.plugins.util;

import java.io.Serializable;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.StringTransmutator;
import com.ecyrd.jspwiki.WikiContext;
import com.intland.codebeamer.Config;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.utils.FancyDate;
import com.intland.codebeamer.wiki.CodeBeamerWikiContext;


/**
 * @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 * @version $Id$
 */
public class UserStringTransmutator implements StringTransmutator, Serializable
{
	private static final Logger log = Logger.getLogger(UserStringTransmutator.class);

	protected String formatDate(WikiContext context, Date date)
	{
		if (date == null)
		{
			return "";
		}

		HttpServletRequest request = context.getHttpRequest();
		if (request != null)
		{
			UserDto user = (UserDto)request.getUserPrincipal();
			if (user != null)
			{
				if (date instanceof FancyDate)
				{
					FancyDate fd = (FancyDate)date;

					if (fd.isOnlyDate())
					{
						return user.getDateFormat().format(date);
					}
				}
				if (date instanceof java.sql.Date)
				{
					return user.getDateFormat().format(date);
				}
				return user.getDateTimeFormat().format(date);
			}
		}

		return date.toString();
	}

	public String mutate(WikiContext context, String source)
	{
		boolean useExternalLinks = ((CodeBeamerWikiContext)context).isUseExternalLinks();
		if (useExternalLinks && source.startsWith("/"))
		{
			String url = Config.getCodeBeamerBaseUrl(false);
			url += source;

			if (log.isDebugEnabled())
			{
				log.debug("MUTATE in: <" + source + "> URL: <" + url + ">");
			}

			source = url;
		}
		return source;
	}
}
