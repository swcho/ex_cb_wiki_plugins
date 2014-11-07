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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.DataBinder;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.persistence.dao.ProjectDao;
import com.intland.codebeamer.persistence.dao.ScmRepositoryDao;
import com.intland.codebeamer.wiki.plugins.base.AbstractCommandWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.DefaultWebBindingInitializer;
import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;
import com.intland.codebeamer.wiki.plugins.command.SourceCodeSummaryCommand;

/**
 * Plugin to display a summary statistic on source code related to the project.
 *
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 * @version $Id$
 * @deprecated since CB-5.7 source files are not parsed/analyzed any more
 */
public class SourceCodeSummaryPlugin extends AbstractCommandWikiPlugin<SourceCodeSummaryCommand> {

//	private final static Logger logger = Logger.getLogger(SourceCodeSummaryPlugin.class);

	@Autowired
	ProjectDao projectDao;

	@Autowired
	ScmRepositoryDao scmRepositoryDao;

	private static final String RENDER_TEMPLATE = "sourcecodesummary-plugin.vm";

	protected boolean checkIfRepositoryIdsAreValid = true;

	public SourceCodeSummaryPlugin() {
		setWebBindingInitializer(new DefaultWebBindingInitializer());
	}

	@Override
	protected String getTemplateFilename() {
		return RENDER_TEMPLATE;
	}

	@Override
	public SourceCodeSummaryCommand createCommand() throws PluginException {
		return new SourceCodeSummaryCommand();
	}

	@Override
	protected void validate(DataBinder binder, SourceCodeSummaryCommand command, Map params) throws NamedPluginException {
		RepositoryDiscovery discovery = new RepositoryDiscovery(this);
		discovery.setCheckIfRepositoryIdsAreValid(checkIfRepositoryIdsAreValid);
		command.setRepositoryId(discovery.discoverRepositoryIds(binder, params, command.getRepositoryId()));
		super.validate(binder, command, params);
	}

	// comparator sorting the statistics by percentage, first the "text" file-groups followed by the "binary" file-groups
//	private final static Comparator<SourceFilesStatsDto> STATS_SORTER = new Comparator<SourceFilesStatsDto>() {
//		public int compare(SourceFilesStatsDto o1, SourceFilesStatsDto o2) {
//			// the "text" FileGroups coming first
//			boolean o1Text = isText(o1);
//			boolean o2Text = isText(o2);
//			if (o1Text != o2Text) {
//				return o1Text ? -1 : 1;	// the text groups coming first
//			}
//
//			if (o1Text) {
//				// both are "text" filegroups
//				int c = 0;
//				if (o1.getTotalLines() != null) {
//					c = - o1.getTotalLines().compareTo(o2.getTotalLines());
//				}
//				if (c != 0) {
//					return c;
//				}
//			}
//
//			// secondary order: the files sizes; or both are "binary" filegroups
//			if (o1.getLength() != null) {
//				return - o1.getLength().compareTo(o2.getLength());
//			}
//			return 0;
//		}
//
//		private boolean isText(SourceFilesStatsDto stats) {
//			return stats.getTotalLines() != null && stats.getTotalLines().intValue() > 0;
//		}
//	};

	@Override
	protected Map populateModel(DataBinder binder, SourceCodeSummaryCommand command, Map params) throws PluginException {
		Map model = new HashMap<String, Object>();
/* since CB-5.7 source files are not parsed/analyzed any more
		List<ScmRepositoryDto> repositoriesVisibleForUser = scmRepositoryDao.findById(getUser(), command.getRepositoryId());
		command.setRepositoryId(PersistenceUtils.grabIds(repositoriesVisibleForUser));

		List<SourceFilesStatsDto> sourceCodeStats = sourceFileDao.findSourceFilesStatsByTypeId(command.getRepositoryId());
		fillSourceFileStatsWithRepositories(sourceCodeStats, repositoriesVisibleForUser);

		// collect the descriptions for all file-types/groups
		// the filegroup.id->filegroup's descriptions mapping. The descriptions are renderer on the html page
		Map<String, String> fileGroupDescriptions = collectFileGroupDescriptions(sourceCodeStats);

		// sum-up individial results of each repository by filegroups.
		// Note: this is not done in SQL, because I've to know the repository to get the filegroups for that repository
		sourceCodeStats = summarizeStatsByType(sourceCodeStats);

		SourceFilesStatsDto total = compulteTotal(sourceCodeStats);

		// sorting the statistics by percentage, first the "text" file-groups followed by the "binary" file-groups
		Collections.sort(sourceCodeStats, STATS_SORTER);

		// set up Velocity context
		model.put("sourceCodeStats", sourceCodeStats);
		model.put("fileGroupDescriptions", fileGroupDescriptions);
		model.put("total", total);
*/
		model.put("sourceCodeStats", Collections.EMPTY_LIST);
		model.put("fileGroupDescriptions", Collections.EMPTY_MAP);
		model.put("command", command);

		return model;
	}

//	/**
//	 * create the summary of the same filegroup-types of mulitple repositories
//	 */
//	private List<SourceFilesStatsDto> summarizeStatsByType(List<SourceFilesStatsDto> sourceCodeStats) {
//		// the totals indexed by file-type/group
//		Map<String, SourceFilesStatsDto> summary = new LinkedHashMap<String, SourceFilesStatsDto>();
//		for (SourceFilesStatsDto stats: sourceCodeStats) {
//			String fileTypeId = stats.getTypeId();
//			SourceFilesStatsDto sum = summary.get(fileTypeId);
//			if (sum == null) {
//				sum = new SourceFilesStatsDto();
//				sum.setTypeId(fileTypeId);
//				summary.put(fileTypeId, sum);
//			}
//			addStats(stats, sum);
//		}
//
//		return new ArrayList(summary.values());
//	}
//
//	private Map<String, String> collectFileGroupDescriptions(List<SourceFilesStatsDto> sourceCodeStats) {
//		HashMap<String, String> result = new HashMap<String, String>();
//		for (SourceFilesStatsDto stats: sourceCodeStats) {
//			String fileTypeId = stats.getTypeId();
//			if (!result.containsKey(fileTypeId)) {
//				FileGroupDto filegroup = findFileGroup(stats.getRepository(), fileTypeId);
//				result.put(fileTypeId, filegroup.getDescription());
//			}
//		}
//		return result;
//	}
//
//	/**
//	 * The SourceFileStatsDto only contains minimal repository information, fill it up with populated repositories
//	 */
//	private void fillSourceFileStatsWithRepositories(List<SourceFilesStatsDto> sourceCodeStats, List<ScmRepositoryDto> repositoriesVisibleForUser) {
//		Map<Integer, ScmRepositoryDto> repositoriesMap = PersistenceUtils.createLookupMap(repositoriesVisibleForUser);
//		for (SourceFilesStatsDto stats: sourceCodeStats) {
//			Integer id = stats.getRepository().getId();
//			ScmRepositoryDto repo = repositoriesMap.get(id);
//			if (repo == null) {
//				logger.warn("Repository with id " + id +" is not found or not available for user");
//			} else {
//				stats.setRepository(repo);
//			}
//		}
//	}
//
//	/**
//	 * Add up two statistics
//	 */
//	private void addStats(SourceFilesStatsDto stats, SourceFilesStatsDto total) {
//		if (total.getTotalLines() == null) {
//			total.setTotalLines(Integer.valueOf(0));
//		}
//		if (total.getLength() == null) {
//			total.setLength(Long.valueOf(0));
//		}
//
//		if (stats.getTotalLines() != null) {
//			total.setTotalLines(Integer.valueOf(total.getTotalLines().intValue() + stats.getTotalLines().intValue()));
//		}
//		if (stats.getLength() != null) {
//			total.setLength(Long.valueOf(total.getLength().longValue() + stats.getLength().longValue()));
//		}
//	}
//
//	private SourceFilesStatsDto compulteTotal(List<SourceFilesStatsDto> sourceCodeStats) {
//		SourceFilesStatsDto total = new SourceFilesStatsDto();
//		total.setTotalLines(Integer.valueOf(0));
//		total.setLength(Long.valueOf(0));
//		for (SourceFilesStatsDto stats: sourceCodeStats) {
//			addStats(stats, total);
//		}
//		return total;
//	}
//
//	// file groups per project cached
//	private Map<ProjectDto, List<FileGroupDto>> fileGroupsPerProject = new HashMap<ProjectDto, List<FileGroupDto>>();
//
//	/**
//	 * Lazy load/cache file groups per project
//	 */
//	private FileGroupDto findFileGroup(ScmRepositoryDto repository, String fileTypeId) {
//		ProjectDto project = repository.getProject();
//		List<FileGroupDto> filegroupsOfProject = fileGroupsPerProject.get(project);
//		if (filegroupsOfProject == null) {
//			filegroupsOfProject = loadFileGroups(project);
//			fileGroupsPerProject.put(project, filegroupsOfProject);
//		}
//
//		for (FileGroupDto fg: filegroupsOfProject) {
//			if (fg.getType() != null && fg.getType().equals(fileTypeId)) {
//				return fg;
//			}
//		}
//
//		return null;
//	}
//
//	private List<FileGroupDto> loadFileGroups(ProjectDto project) {
//		List<FileGroupDto> filegroupsOfProject;
//		ProjectPreferencesDto preferences = projectPreferencesDao.findByProjectId(project.getId());
//		filegroupsOfProject = new ArrayList<FileGroupDto>();
//		if (preferences != null) {
//			filegroupsOfProject.addAll(preferences.getSourceFileGroups());
//		}
//		return filegroupsOfProject;
//	}

}
