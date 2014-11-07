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

import com.intland.codebeamer.persistence.dto.WikiPageDto;

/**
 * Convenience subclass for {@link BestContentPlugin} shows only WIKI pages.
 *
 * @see BestContentPlugin
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 * $Id$
 */
public class BestWikiPagesPlugin extends BestContentPlugin {
	public BestWikiPagesPlugin() {
		forcedEntityType = WikiPageDto.INTERWIKI_LINK_TYPE;
	}
}
