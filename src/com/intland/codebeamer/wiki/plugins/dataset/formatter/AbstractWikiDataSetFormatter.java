package com.intland.codebeamer.wiki.plugins.dataset.formatter;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.jfree.data.general.Dataset;
import org.nascif.jspwiki.plugin.imagegen.jfreechart.reader.ChartReader;

import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.utils.FancyDate;

/**
 * This mixin provides utility functionality for concrete formatters.
 *
 * @author <a href="mailto:aron.gombas@simbirsoft.com">Aron Gombas</a>
 * @version $Id$
 */
public abstract class AbstractWikiDataSetFormatter {
	final static protected String TIME_STAMP_FORMAT = "yyyy/MM/dd-HH:mm:ss:SSS";
	final static protected String LINE_BREAK = "\r\n";

	private UserDto user;

	public UserDto getUser() {
		return user;
	}

	public void setUser(UserDto user) {
		this.user = user;
	}

	/**
	 * Returns the message to show when a formatter does not support
	 * the type of the passed {@link Dataset}.
	 */
	protected String getUnsupportedDatasetTypeMessage(Dataset dataSet) {
		return StringUtils.substringAfterLast(this.getClass().getName(), ".") +
				" does not support data set type " +
				StringUtils.substringAfterLast(dataSet.getClass().getName(), ".");
	}

	/**
	 * Returns the value formatted for human reading.
	 */
	protected String formatValue(Object value) {
		if(value == null) {
			return "";
		} else if(value instanceof Number) {
			return formatNumber((Number)value);
		} else if(value instanceof Date) {
			return formatDate((Date)value);
		}

		// fall back to String
		return value.toString();
	}

	/**
	 * Returns the value formatted for chart input.
	 */
	protected String formatValueForCharts(Object value) {
		if(value == null) {
			return "";
		} else if(value instanceof Number) {
			return formatNumberForCharts((Number)value);
		} else if(value instanceof Date) {
			return formatDateForCharts((Date)value);
		}

		// fall back to String
		return escapeValue(value.toString());
	}

	/**
	 * Returns number formatted for human reading: "10,000.23".
	 */
	protected String formatNumber(Number value) {
		if(value == null) {
			return "0";
		}

		NumberFormat formatter = NumberFormat.getInstance();
		return formatter.format(value);
	}

	/**
	 * Returns number formatted for chart input: "10000.23".
	 */
	protected String formatNumberForCharts(Number value) {
		if(value == null) {
			return "0";
		}

		return value.toString();
	}

	/**
	 * Returns percentage formatted for human reading: "78%".
	 */
	protected String formatPercentage(Number value) {
		if(value == null) {
			return "0%";
		}

		NumberFormat formatter = NumberFormat.getPercentInstance();
		return formatter.format(value);
	}

	/**
	 * Returns percentage formatted for chart input: "0.78".
	 */
	protected String formatPercentageForCharts(Number value) {
		return formatNumberForCharts(value);
	}

	/**
	 * Returns date formatted for human reading: "Yesterday".
	 */
	protected String formatDate(Date date) {
		return new FancyDate(date, user).toString();
	}

	/**
	 * Returns date formatted for chart input: "12/20/2008".
	 */
	protected String formatDateForCharts(Date date) {
		return new SimpleDateFormat("M/d/yyyy").format(date);
	}

	/**
	 * Returns date formatted for chart input: "12/20/2008".
	 */
	protected String formatTimeStampForCharts(Date date) {
		return new SimpleDateFormat(TIME_STAMP_FORMAT).format(date);
	}

	/**
	 * Escape a dataset value, so it will be correctly rendered by ImageGen plugin.
	 *
	 * Escaping is done if the string value contains any "'"-es or "\" -es, so:
	 * - the whole string is wrapped to apostrophes
	 * - all apostrophe or escape chars inside are escaped by adding an escape char before.
	 *
	 * @param value
	 * @return The escaped value.
	 */
	public String escapeValue(String value) {
		if (value == null) {
			return null;
		}

		String escaped = escapeValue(value, "" + ChartReader.TEXT_DELIMITER, ChartReader.ESCAPE);
		boolean isEscaped = escaped.length() > value.length();
		if (isEscaped) {
			// because the string contained chars which were escaped, add the apostrophe wrapper around
			escaped = ChartReader.TEXT_DELIMITER + escaped + ChartReader.TEXT_DELIMITER;
		}
		return escaped;
	}

	/**
	 * Escapes the given string value, so:
	 * - if a special character appears it will be preceeded by the escape character
	 * - if the escape character appears it will be double-escaped.
	 *
	 * @param value
	 * @param characterToEscape String contains the characters to escape.
	 * @param escape The escape character.
	 * @return The escaped string
	 */
	public String escapeValue(String value, String charactersToEscape, char escape) {
		if (value == null) {
			return null;
		}

		StringBuffer escaped = new StringBuffer(value.length() + 10);
		for (int i=0; i< value.length(); i++) {
			char c = value.charAt(i);
			// add an escape before the special chars
			boolean shouldEscape = charactersToEscape.indexOf(c) != -1;
			if (shouldEscape || c == escape) {
				escaped.append(escape);
			}
			escaped.append(c);
		}
		return escaped.toString();
	}
}
