package com.intland.codebeamer.wiki.plugins;

import org.apache.commons.lang.StringUtils;

import com.ecyrd.jspwiki.plugin.PluginException;

/**
 * This plugin needs "latex" and "convert" (included on Redhat with ImageMagic) applications.
 *
 * Usage:
 *	[{MathPlugin title='Optional title rendered as a tooltip on the image'
 *			$math formula body$
 *	}]
 *
	[{MathPlugin
		sqrt{1-e^2}}]

	[{MathPlugin
		$\alpha$
	}]

	[{Math align='center' fontsize='16'
		2\sum_{i=1}^n a_i \;\int_a^b f_i(x)g_i(x)\,dx
	}]
 *
 * @author <a href="calderoni@streamsim.com">Max Calderoni</a>
 * @author <a href="digiovinazzo@streamsim.com">Matteo Di Giovinazzo</a>
 * @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 * @version $Id$
 */
public class MathPlugin extends LatexPlugin {

	@Override
	public MathPluginCommand createCommand() throws PluginException {
		return new MathPluginCommand();
	}

	@Override
	protected String decorateLatexTextForRendering(LatexPluginCommand command, String latexText) {
		latexText = StringUtils.trimToNull(latexText);
		if (latexText == null) {
			return "";
		}

		// build the standard latex wrapper for math-plugin
		StringBuilder buf = new StringBuilder();
		//buf.append("\\documentclass[12pt]{article}\n");
		buf.append("\\documentclass{article}\n");
		buf.append("\\usepackage{amsmath}\n");
		buf.append("\\usepackage{amsfonts}\n");
		buf.append("\\usepackage{amssymb}\n");
		buf.append("\\pagestyle{empty}\n");

		// \DeclareMathSizes{10}{18}{12}{8}   % For size 10 text
		// \DeclareMathSizes{11}{19}{13}{9}   % For size 11 text
		// \DeclareMathSizes{12}{20}{14}{10}  % For size 12 text

		buf.append("\\begin{document}\n");
		buf.append(latexText);
		buf.append("\\end{document}\n");
		return buf.toString();
	}

}

