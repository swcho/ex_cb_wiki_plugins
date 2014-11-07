package com.intland.codebeamer.wiki.plugins.dataset;

import org.jfree.data.general.Dataset;

import com.ecyrd.jspwiki.plugin.PluginException;

/**
 * Implementation classes format a {@link WikiDataSet} to some textual output.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 * @version $Id$
 */
public interface WikiDataSetFormatter {
	String format(Dataset dataSet) throws PluginException;
}