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
package com.intland.codebeamer.wiki.plugins.dataset.producer;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jfree.data.general.Dataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.persistence.dao.impl.ArtifactDaoImpl;
import com.intland.codebeamer.persistence.dao.impl.ChartDaoImpl;
import com.intland.codebeamer.persistence.dto.ArtifactDto;
import com.intland.codebeamer.persistence.dto.ChartArtifactTrendDto;
import com.intland.codebeamer.persistence.dto.ChartArtifactsTrendDto;
import com.intland.codebeamer.wiki.plugins.dataset.WikiDataSetProducer;
import com.intland.codebeamer.wiki.plugins.util.WikiPluginUtils;

/**
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 * @version $Id$
 */
public class DocumentAccessTrendProducer extends AbstractWikiDataSetProducer implements WikiDataSetProducer {
	public static final String DOCUMENT_ACCESS_READ = "read";
	public static final String DOCUMENT_ACCESS_WRITE = "write";
	public static final String DOCUMENT_ACCESS_READ_WRITE = "readWrite";

	private static final Set<String> allowedTypes = new HashSet();
	static {
		allowedTypes.add(DOCUMENT_ACCESS_READ);
		allowedTypes.add(DOCUMENT_ACCESS_WRITE);
		allowedTypes.add(DOCUMENT_ACCESS_READ_WRITE);
	}

	public Dataset produce(Map<String, String> params) throws PluginException  {
		List<Integer> artifactIds = parseCommaSeparatedIds(params.get("documentId"));
		if (artifactIds.isEmpty()) {
			throw new PluginException("Parameter 'documentId' is required");
		}
		String accessType = WikiPluginUtils.getStringParameter(params, "type", allowedTypes, DOCUMENT_ACCESS_READ_WRITE);

		// gather
		List<ChartArtifactsTrendDto> chartArtifactsTrends = ChartDaoImpl.getInstance().getArtifactsTrend(artifactIds, getStartDate(), getEndDate());

		// convert
		TimeSeriesCollection timeSeriesCollection = new TimeSeriesCollection();
		for (ChartArtifactsTrendDto chartArtifactsTrend : chartArtifactsTrends) {
			Day day = new Day(chartArtifactsTrend.getDate());

			for(ChartArtifactTrendDto chartArtifactTrend : chartArtifactsTrend.getValues()) {
				Integer artifactId = chartArtifactTrend.getArtifact().getId();
				ArtifactDto artifact = ArtifactDaoImpl.getInstance().findById(artifactId); // FIXME should respect security
				if(artifact != null) {
					String key = artifact.getId() + " " + artifact.getName();

					TimeSeries timeSeries = timeSeriesCollection.getSeries(key);
					if(timeSeries == null) {
						timeSeries = new TimeSeries(key);
						timeSeriesCollection.addSeries(timeSeries);
					}

					Long count = null;
					if (DOCUMENT_ACCESS_READ.equals(accessType)) {
						count = chartArtifactTrend.getReads();
					} else if(DOCUMENT_ACCESS_WRITE.equals(accessType)) {
						count = chartArtifactTrend.getWrites();
					} else {
						count = new Long(chartArtifactTrend.getWrites().longValue() + chartArtifactTrend.getReads().longValue());
					}

					timeSeries.add(day, count);
				}
			}
		}

		return timeSeriesCollection;
	}
}
