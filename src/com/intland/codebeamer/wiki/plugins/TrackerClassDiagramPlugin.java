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
 *
 */
package com.intland.codebeamer.wiki.plugins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;

import org.nascif.jspwiki.plugin.imagegen.GraphvizIsNotAvailableException;
import org.nascif.jspwiki.plugin.imagegen.graphviz.GraphVizPlugin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.HtmlUtils;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.ecyrd.jspwiki.plugin.PluginManager;
import com.ecyrd.jspwiki.plugin.WikiPlugin;
import com.intland.codebeamer.controller.ControllerUtils;
import com.intland.codebeamer.manager.TrackerManager;
import com.intland.codebeamer.persistence.dao.impl.EntityCache;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.ProjectPermission;
import com.intland.codebeamer.persistence.dto.TrackerDto;
import com.intland.codebeamer.persistence.dto.TrackerLayoutDto;
import com.intland.codebeamer.persistence.dto.TrackerLayoutLabelDto;
import com.intland.codebeamer.persistence.dto.TrackerTypeDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.util.PersistenceUtils;
import com.intland.codebeamer.persistence.util.TrackerItemFieldHandler;
import com.intland.codebeamer.ui.view.table.TrackerSimpleLayoutDecorator;
import com.intland.codebeamer.wiki.plugins.util.WikiPluginUtils;

/**
 * This plugin generates a UML class diagram for all trackers in a project in graphviz dot notation
 * and delegates to the {@link GraphVizPlugin} for rendering
 * <p>
 * 		e.g. <code>[{TrackerClassDiagramPlugin projectId=123}]</code>
 * </p>
 * <p>
 * Parameters:
 * <ul>
 *   <li><b>projectId</b> (optional) is the project ID , or current project if not specified</li>
 *   <li><b>distance</b> (optional) is the number of reference layers to follow/show, default is 1</li>
 * </ul>
 * </p>
 * @since CB-6.0
 * @author <a href="mailto:klaus.mehling@intland.com">Klaus Mehling</a>
 * @version $Id: $
 */
public class TrackerClassDiagramPlugin implements WikiPlugin {
	private static final Logger log = Logger.getLogger(TrackerClassDiagramPlugin.class);

	private static final GraphVizPlugin renderer = new GraphVizPlugin();

	@Autowired
	private TrackerManager trackerManager;

	public String execute(WikiContext context, Map params) throws PluginException {
		HttpServletRequest request = context.getHttpRequest();
		if (request == null) {
			return "";
		}
		ControllerUtils.autoWire(this, request);
		UserDto user = ControllerUtils.getCurrentUser(request);
		TrackerSimpleLayoutDecorator decorator = new TrackerSimpleLayoutDecorator(request);
		Integer projectId = NumberUtils.createInteger(WikiPluginUtils.getParameter(params, "projectId"));
		int distance = Math.max(1, NumberUtils.toInt(WikiPluginUtils.getParameter(params, "distance"), 1));

		ProjectDto project = null;
		if (projectId == null) {
			project = ControllerUtils.getCurrentProject(request);
		} else {
			project = EntityCache.getInstance(user).getProject(projectId);
		}

		String result = StringUtils.EMPTY;

		if (project != null && !project.isDeleted()) {
			List<TrackerDto> trackers = trackerManager.findByProject(user, project);

			CollectionUtils.filter(trackers, new Predicate() {
				public boolean evaluate(Object object) {
					return showInClassDiagram((TrackerDto)object);
				}
			});

			if (trackers != null && trackers.size() > 0) {
				String chart = createClassDiagram(user, request.getContextPath(), project, trackers, distance, decorator);

				try {
					Map<String,String> renderParams = new HashMap<String,String>(8);
					renderParams.put("title",  "ProjectClassDiagram_" + projectId);
					renderParams.put("filter", "dot");
					renderParams.put("imap",   "true");
					renderParams.put(PluginManager.PARAM_BODY, chart);

					result = renderer.execute(context, renderParams);
				} catch (Throwable ex) {
					log.warn(result, ex);

					if (ex.getCause() instanceof GraphvizIsNotAvailableException) {
						result = decorator.getText("tracker.class.diagram.notAvailable", "Tracker Class Diagram is not available");
					} else {
						result = decorator.getText("errors.message", "{0}", StringEscapeUtils.escapeHtml(ex.toString()));
					}
				}
			} else {
				result = decorator.getText("tracker.class.diagram.empty", "User {0} has no access to trackers in project {1} ({2})", user.getName(), project.getName(), projectId);
			}
		} else {
			result = "<span class=\"invalidfield\">" +
						decorator.getText("error.access.to.entity.denied", "User {0} has no permission to access {1} with id = {2}",
											user.getName(), decorator.getText("project.label", "Project"), String.valueOf(projectId))
				     + "</span>";
		}

		return result;
	}

	protected boolean showInClassDiagram(TrackerDto tracker) {
		return tracker != null && !tracker.isDeleted() && tracker.getProject() != null && !tracker.getProject().isDeleted() &&
				!(TrackerTypeDto.isRepositoryType(tracker.getType()) ||
				  TrackerTypeDto.WORKLOG.isInstance(tracker) ||
				  TrackerTypeDto.PULL.isInstance(tracker) ||
				  TrackerTypeDto.FORUM_POST.isInstance(tracker));
	}

	protected String createClassDiagram(UserDto user, String contextPath, ProjectDto project, List<TrackerDto> trackers, int distance, TrackerSimpleLayoutDecorator decorator) {
		TrackerItemFieldHandler resolver = TrackerItemFieldHandler.getInstance(user);
		Map<TrackerDto,TrackerDto> trackerTemplates = new HashMap<TrackerDto,TrackerDto>();
		Map<TrackerDto,Map<TrackerLayoutLabelDto,List<TrackerDto>>> trackerRefFields = new HashMap<TrackerDto,Map<TrackerLayoutLabelDto,List<TrackerDto>>>();
		Map<ProjectDto,Map<Integer,TrackerDto>> projectTrackers = new HashMap<ProjectDto,Map<Integer,TrackerDto>>(8);
		projectTrackers.put(project, PersistenceUtils.createLookupMap(trackers));

		for (int i = 0; i < distance; ++i) {
			if (trackers != null && trackers.size() > 0) {
				List<TrackerDto> nextLayer = new ArrayList<TrackerDto>();

				for (TrackerDto tracker : trackers) {
					if (showInClassDiagram(tracker)) {
						TrackerLayoutDto layout = resolver.getBasicLayout(tracker.getId());
						if (layout != null) {
							if (tracker.getTemplateId() != null) {
								TrackerDto template = resolver.getEntityCache().getTracker(tracker.getTemplateId());
								if (showInClassDiagram(template)) {
									Map<Integer,TrackerDto> domain = projectTrackers.get(template.getProject());
									if (domain == null) {
										projectTrackers.put(template.getProject(), domain = new HashMap<Integer,TrackerDto>());
									}

									if (!domain.containsKey(template.getId())) {
										domain.put(template.getId(), template);
										nextLayer.add(template);
									}

									trackerTemplates.put(tracker, template);
								}
							}

							for (TrackerLayoutLabelDto field : layout.getFlatFieldList()) {
								if (field.getReferenceFilters() != null && field.getReferenceFilters().size() > 0) {
									List<TrackerDto> references = trackerManager.evaluate(user, field.getReferenceFilters());
									if (references != null && references.size() > 0) {
										Map<TrackerLayoutLabelDto,List<TrackerDto>> refFields = trackerRefFields.get(tracker);
										if (refFields == null) {
											trackerRefFields.put(tracker, refFields = new LinkedHashMap<TrackerLayoutLabelDto,List<TrackerDto>>());
										}
										refFields.put(field, references);

										for (TrackerDto reference : references) {
											if (showInClassDiagram(reference)) {
												Map<Integer,TrackerDto> domain = projectTrackers.get(reference.getProject());
												if (domain == null) {
													projectTrackers.put(reference.getProject(), domain = new HashMap<Integer,TrackerDto>());
												}

												if (!domain.containsKey(reference.getId())) {
													domain.put(reference.getId(), reference);
													nextLayer.add(reference);
												}
											}
										}
									}
								}
							}
						}
					}
				}

				trackers = nextLayer;
			}
		}

		Map<Integer,String> colorMap = new HashMap<Integer,String>();
		for (TrackerTypeDto type : TrackerTypeDto.TYPES) {
			colorMap.put(type.getId(), type.getColor());
		}

		StringBuilder chart = new StringBuilder(1024);

		chart.append("graph [nodesep=\".25\", ranksep=\"1\", rankdir=\"LR\", charset=\"UTF-8\"]\n");

		for (Map.Entry<ProjectDto,Map<Integer,TrackerDto>> cluster : projectTrackers.entrySet()) {
			if (projectTrackers.size() > 1) {
				chart.append("subgraph cluster_").append(cluster.getKey().getId()).append(" {\n");
				chart.append("label = \"").append(escapeLabel(cluster.getKey().getName())).append("\";\n");
				chart.append("color=blue;\n");
				chart.append("style=dashed;\n");
				chart.append("fontcolor=blue;\n");
			}

			chart.append("\n");

			for (TrackerDto tracker : cluster.getValue().values()) {
				TrackerDto template = trackerTemplates.get(tracker);
				Map<TrackerLayoutLabelDto,List<TrackerDto>> refFields = trackerRefFields.get(tracker);
				String type  = escapeLabel(decorator.getText("tracker.type." + tracker.getItemName(), tracker.getItemName()));
				String label = escapeLabel(trackerManager.getName(decorator.getTextLocale(), tracker));
				String color = StringUtils.defaultIfEmpty(colorMap.get(tracker.getType().getId()), "lightGrey");

				chart.append("tracker_").append(tracker.getId());
				chart.append(" [shape=none, margin=0, fontcolor=black, fontname=\"Arial\", fontsize=10, ");
				if (template != null) {
					chart.append("color=blue, ");
				}
				chart.append("tooltip=\"").append(escapeLabel(tracker.getInterwikiLink())).append("\", URL=\"").append(contextPath);
				if (resolver.getEntityCache().hasPermission(tracker.getProject(), tracker.isCategory() ? ProjectPermission.cmdb_admin : ProjectPermission.tracker_admin)) {
					chart.append("/proj/tracker/configuration.spr?tracker_id=").append(tracker.getId()).append("&orgDitchnetTabPaneId=tracker-customize-field-properties");
				} else {
					chart.append(tracker.getUrlLink());
				}
				chart.append("\", ");
				chart.append("  label=<");
				chart.append(	"<table border=\"1\" cellborder=\"0\" cellspacing=\"0\" cellpadding=\"2\">");
				chart.append(		"<tr>");
				chart.append(			"<td bgcolor=\"").append(color).append("\" valign=\"middle\">");
				chart.append(			 	"<font point-size=\"8\">");
				chart.append(					"&#171;").append(type).append("&#187;");
				chart.append(				"</font>");
				chart.append(			"</td>");
				chart.append(		"</tr>");
				chart.append(		"<tr>");
				chart.append(			"<td bgcolor=\"").append(color).append("\" >");
				chart.append(			 	"<font face=\"Arial Bold\" >");
				chart.append(					label);
				chart.append(				"</font>");
				chart.append(			"</td>");
				chart.append(		"</tr>");

				if (refFields != null && refFields.size() > 0) {
					chart.append("<tr><td></td></tr>");

					for (TrackerLayoutLabelDto field : refFields.keySet()) {
						chart.append("<tr>");
						chart.append(	"<td align=\"left\" port=\"").append(field.getId()).append("\">");
						chart.append( escapeLabel(decorator.getText("tracker.field." + field.getLabel() + ".label", field.getLabel())));
						chart.append(	"</td>");
						chart.append("</tr>");
					}
				}

				chart.append(   "</table>>");
				chart.append(" ];\n");
			}

			if (projectTrackers.size() > 1) {
				chart.append("}\n\n");
			}
		}

		if (trackerTemplates.size() > 0) {
			chart.append("\n");

			for (Map.Entry<TrackerDto,TrackerDto> template : trackerTemplates.entrySet()) {
				chart.append("tracker_").append(template.getKey().getId());
				chart.append(" -> ");
				chart.append("tracker_").append(template.getValue().getId());
				chart.append(" [arrowhead=onormal, color=blue];\n");
			}
		}

		if (trackerRefFields.size() > 0) {
			chart.append("\n");

			for (Map.Entry<TrackerDto,Map<TrackerLayoutLabelDto,List<TrackerDto>>> trackerRefField : trackerRefFields.entrySet()) {
				TrackerDto tracker = trackerRefField.getKey();

				for (Map.Entry<TrackerLayoutLabelDto,List<TrackerDto>> refField : trackerRefField.getValue().entrySet()) {
					TrackerLayoutLabelDto field = refField.getKey();

					for (TrackerDto reference : refField.getValue()) {
						chart.append("tracker_").append(tracker.getId()).append(":").append(field.getId()).append(":e");
						chart.append(" -> ");
						chart.append("tracker_").append(reference.getId());
						chart.append(" [arrowhead=vee");
						if (reference.isVersionCategory()) {
							chart.append(", color=lightGrey");
						} else if (TrackerTypeDto.PLATFORM.isInstance(reference)) {
							chart.append(", color=grey");
						}
						chart.append("];\n");
					}
				}
			}
		}

		return chart.toString();
	}

	protected String escapeLabel(String string) {
		String label = HtmlUtils.htmlEscapeHex(StringUtils.defaultString(StringUtils.trimToNull(string), "--"));
		return label;
	}
}
