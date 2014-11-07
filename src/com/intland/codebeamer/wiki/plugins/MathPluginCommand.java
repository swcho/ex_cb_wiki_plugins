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

/**
 * Command bean for {@link MathPlugin}
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public class MathPluginCommand extends LatexPluginCommand {

	// TODO: these are not used anywhere?
	private String align;
	private String fontsize = "16";

	// backwards compatibility, inline parameter for old Math plugin
	public String getInline() {
		return getText();
	}

	public void setInline(String inline) {
		setText(inline);
	}

	public String getAlign() {
		return align;
	}

	public void setAlign(String align) {
		this.align = align;
	}

	public String getFontsize() {
		return fontsize;
	}
	public void setFontsize(String fontsize) {
		this.fontsize = fontsize;
	}

}
