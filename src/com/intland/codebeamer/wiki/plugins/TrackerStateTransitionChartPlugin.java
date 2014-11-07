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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

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
import com.intland.codebeamer.persistence.dao.WorkflowTransitionDao;
import com.intland.codebeamer.persistence.dao.impl.EntityCache;
import com.intland.codebeamer.persistence.dto.ForeignTrackerItemFilterDto;
import com.intland.codebeamer.persistence.dto.ProjectPermission;
import com.intland.codebeamer.persistence.dto.TrackerChoiceOptionDto;
import com.intland.codebeamer.persistence.dto.TrackerDto;
import com.intland.codebeamer.persistence.dto.TrackerLayoutLabelDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.dto.WorkflowTransitionDto;
import com.intland.codebeamer.persistence.dto.base.IdentifiableDto;
import com.intland.codebeamer.persistence.dto.base.IdentifiableFieldValueDto;
import com.intland.codebeamer.persistence.dto.base.NamedDto;
import com.intland.codebeamer.persistence.util.TrackerItemFieldHandler;
import com.intland.codebeamer.ui.view.IssueStatusStyles;
import com.intland.codebeamer.ui.view.IssueStatusStyles.Style;
import com.intland.codebeamer.ui.view.table.TrackerSimpleLayoutDecorator;
import com.intland.codebeamer.wiki.plugins.util.WikiPluginUtils;

/**
 * This plugin generates a state transition diagram for a specific tracker in graphviz dot notation
 * and delegates to the {@link GraphVizPlugin} for rendering
 * <p>
 * 		e.g. <code>[{TrackerStateTransitionChart trackerId=123}]</code>
 * </p>
 * @author <a href="mailto:klaus.mehling@intland.com">Klaus Mehling</a>
 * @version $Id: $
 */
public class TrackerStateTransitionChartPlugin implements WikiPlugin {
	private static final Logger log = Logger.getLogger(TrackerStateTransitionChartPlugin.class);

	private static final GraphVizPlugin renderer = new GraphVizPlugin();

	/**
	 * This class stores information about a StateDiagram to render
	 */
	public static class StateDiagram {
		private TrackerItemFieldHandler 	resolver;
		private TrackerDto 					tracker;
		private List<WorkflowTransitionDto>	transitions  = Collections.emptyList();
		private List<NamedDto> 				states       = Collections.emptyList();
		private Set<Integer> 				activeStates = Collections.emptySet();
		private Set<Integer> 				activeTrans  = Collections.emptySet();
		private int                         maxTransId   = 0;
		private int							distance     = 0;
		public  boolean                     showUnset    = false;

		public StateDiagram(TrackerDto tracker, List<WorkflowTransitionDto> transitions, TrackerItemFieldHandler resolver, int distance) {
			this.resolver = resolver;
			this.tracker  = tracker;
			this.distance = distance;

			if (transitions != null && transitions.size() > 0) {
				this.transitions  = transitions;
				this.states       = new ArrayList<NamedDto>(transitions.size() * 2);
				this.activeStates = new HashSet<Integer>(transitions.size() * 2);
				this.activeTrans  = new HashSet<Integer>(transitions.size());

				for (WorkflowTransitionDto transition : transitions) {
					addState(transition.getFromStatus());
					addState(transition.getToStatus());

					if (transition.getFromStatus() == null || transition.getToStatus() == null) {
						showUnset = true;
					}

					maxTransId = Math.max(maxTransId, transition.getId().intValue());
				}

				if (transitions.size() > 1) {
					Collections.sort(transitions, new WorkflowTransitionDto.SourceStatusComparator());
				}
			}
		}

		public int getDistance() {
			return distance;
		}

		protected void addState(NamedDto state) {
			if (state != null && state.getId() != null && state.getId().intValue() >= 0 && !states.contains(state)) {
				states.add(state);
			}
		}

		public void addAllStates() {
			// add those states which has no transition to/from yet, so they will still appear on the chart
			Collection<TrackerChoiceOptionDto> allStates = resolver.getFieldChoiceOptions(tracker, TrackerItemFieldHandler.STATUS_LABEL_ID).values();
			for (TrackerChoiceOptionDto status: allStates) {
				addState (status);
			}

			for (NamedDto status : states) {
				setActiveStatus(status.getId());
			}

			for (WorkflowTransitionDto transition : transitions) {
				if (!IdentifiableDto.equals(transition.getFromStatus(), WorkflowTransitionDto.ANY_STATUS)) {
					setActive(transition);
				}
			}
		}

		public TrackerDto getTracker() {
			return tracker;
		}

		public List<NamedDto> getStates() {
			Collections.sort(states);
			return states;
		}

		public List<WorkflowTransitionDto> getTransitions() {
			return transitions;
		}

		public WorkflowTransitionDto getTransition(Integer fromStatusId, Integer toStatusId) {
			WorkflowTransitionDto wildcard = null;

			for (WorkflowTransitionDto transition : transitions) {
				if (IdentifiableDto.compareInteger(fromStatusId, transition.getFromStatusId()) == 0 &&
					IdentifiableDto.compareInteger(toStatusId,   transition.getToStatusId()) == 0) {
					return transition;
				} else if (fromStatusId != null &&
						   IdentifiableDto.compareInteger(WorkflowTransitionDto.ANY_STATUS.getId(), transition.getFromStatusId()) == 0 &&
						   IdentifiableDto.compareInteger(fromStatusId, transition.getToStatusId()) != 0 &&
						   IdentifiableDto.compareInteger(toStatusId,   transition.getToStatusId()) == 0) {
					wildcard = transition;
				}
			}

			if (wildcard != null) {
				TrackerChoiceOptionDto status     = resolver.getFieldChoiceOption(tracker, TrackerItemFieldHandler.STATUS_LABEL_ID, fromStatusId);
				WorkflowTransitionDto  transition = wildcard.clone();

				transition.setId(Integer.valueOf(++maxTransId));
				transition.setFromStatus(status != null ? status : new NamedDto(fromStatusId, "Status-" + fromStatusId));
				transitions.add(transition);

				return transition;
			}

			return null;
		}

		public boolean isActive(WorkflowTransitionDto transition) {
			return transition != null && activeTrans.contains(transition.getId());
		}

		public void setActive(WorkflowTransitionDto transition) {
			if (transition != null && transition.getId() != null) {
				activeTrans.add(transition.getId());

				setActiveStatus(transition.getFromStatusId());
				setActiveStatus(transition.getToStatusId());
			}
		}

		public boolean isActiveStatus(Integer statusId) {
			return activeStates.contains(statusId);
		}

		public void setActiveStatus(Integer statusId) {
			if (statusId != null && statusId.intValue() >= 0 && activeStates.add(statusId)) {
				for (NamedDto status : states) {
					if (statusId.equals(status.getId())) {
						return;
					}
				}

				TrackerChoiceOptionDto status = resolver.getFieldChoiceOption(tracker, TrackerItemFieldHandler.STATUS_LABEL_ID, statusId);
				states.add(status != null ? status : new NamedDto(statusId, "Status-" + statusId));
			}
		}

	}

	@Autowired
	private TrackerManager trackerManager;
	@Autowired
	private IssueStatusStyles issueStatusStyles;
	@Autowired
	private WorkflowTransitionDao workflowTransitionDao;

	public String execute(WikiContext context, Map params) throws PluginException {
		HttpServletRequest request = context.getHttpRequest();
		if (request == null) {
			return "";
		}
		ControllerUtils.autoWire(this, request);
		UserDto user = ControllerUtils.getCurrentUser(request);
		TrackerSimpleLayoutDecorator decorator = new TrackerSimpleLayoutDecorator(request);
		Integer trackerId = NumberUtils.createInteger(WikiPluginUtils.getParameter(params, "trackerId"));
		TrackerItemFieldHandler resolver = TrackerItemFieldHandler.getInstance(user);
		EntityCache cache = resolver.getEntityCache();
		TrackerDto tracker = cache.getTracker(trackerId);
		String result = StringUtils.EMPTY;

		if (tracker != null) {
			String name = trackerManager.getName(request.getLocale(), tracker);

			if (tracker.isUsingWorkflow()) {
				int distance = NumberUtils.toInt(WikiPluginUtils.getParameter(params, "distance"), 0);
				boolean admin = cache.hasPermission(tracker.getProject(), tracker.isCategory() ? ProjectPermission.cmdb_admin : ProjectPermission.tracker_admin);
				String chart = createStateTransitionGraph(tracker, admin ? request.getContextPath() : null, distance, resolver, decorator);

				try {
					Map<String,String> renderParams = new HashMap<String,String>(8);
					renderParams.put("title",  "TrackerFSM_" + trackerId);
					renderParams.put("filter", "dot");
					renderParams.put("imap",   "true");
					renderParams.put(PluginManager.PARAM_BODY, chart);

					result = renderer.execute(context, renderParams);
				} catch (Throwable ex) {
					log.warn(result, ex);

					if (ex.getCause() instanceof GraphvizIsNotAvailableException) {
						result = decorator.getText("tracker.state.transition.chart.notAvailable", "State Transition Chart is not available");
					} else {
						result = decorator.getText("errors.message", "{0}", StringEscapeUtils.escapeHtml(ex.toString()));
					}
				}
			} else {
				result = decorator.getText("tracker.state.transition.chart.undefined", "{0} - {1} doesn't use workflow", tracker.getProject().getName(), name);
			}
		} else {
			result = "<span class=\"invalidfield\">" +
						decorator.getText("error.access.to.entity.denied", "User {0} has no permission to access {1} with id = {2}",
											user.getName(), decorator.getText("tracker.label", "Tracker"), String.valueOf(trackerId))
				     + "</span>";
		}

		return result;
	}

	protected String createStateTransitionGraph(TrackerDto tracker, String contextPath, int maxDistance, TrackerItemFieldHandler resolver, TrackerSimpleLayoutDecorator decorator) {
		StringBuilder chart = new StringBuilder(512);

		chart.append("graph [charset=\"UTF-8\"]\n");
		chart.append("node [shape=box, peripheries=1, fontcolor=black, fontsize=10, fontname=\"Arial Bold\" ];\n\n");

		List<WorkflowTransitionDto> transitions = workflowTransitionDao.findByTrackerId(tracker.getId(), true);
		if (transitions != null && transitions.size() > 0) {
			Map<Integer,StateDiagram> stateDiagrams = new LinkedHashMap<Integer,StateDiagram>();
			Map<IdentifiableFieldValueDto,WorkflowTransitionDto> crossDiagramLinks = Collections.emptyMap();

			StateDiagram diagram = new StateDiagram(tracker, transitions, resolver, 0);
			diagram.addAllStates();

			stateDiagrams.put(tracker.getId(), diagram);

			if (maxDistance > 0) {
				LinkedList<StateDiagram> queue = new LinkedList<StateDiagram>();
				queue.add(diagram);

				crossDiagramLinks = new LinkedHashMap<IdentifiableFieldValueDto,WorkflowTransitionDto>();

				while (queue.size() > 0) {
					diagram = queue.removeFirst();

					for (WorkflowTransitionDto transition : diagram.getTransitions()) {
						Map<ForeignTrackerItemFilterDto,List<TrackerLayoutLabelDto>> refFieldUpdates = workflowTransitionDao.getRefFieldUpdates(transition);
						if (refFieldUpdates != null && refFieldUpdates.size() > 0) {
							diagram.setActive(transition);

							for (Map.Entry<ForeignTrackerItemFilterDto,List<TrackerLayoutLabelDto>> reference : refFieldUpdates.entrySet()) {
								ForeignTrackerItemFilterDto filter = reference.getKey();
								Integer targetTrackerId = filter.getId();
								Integer targetStatusId  = filter.getStatusId();

								StateDiagram targetDiagram = stateDiagrams.get(targetTrackerId);
								if (targetDiagram == null) {
									TrackerDto targetTracker = resolver.getEntityCache().getTracker(targetTrackerId);
									if (targetTracker != null && diagram.getDistance() < maxDistance) {
										targetDiagram = new StateDiagram(targetTracker, workflowTransitionDao.findByTrackerId(targetTrackerId, true), resolver, diagram.getDistance() + 1);
										stateDiagrams.put(targetTrackerId, targetDiagram);
										queue.add(targetDiagram);
									} else {
										continue;
									}
								}

								WorkflowTransitionDto targetTrans = null;
								for (TrackerLayoutLabelDto update : reference.getValue()) {
									if (TrackerItemFieldHandler.STATUS_LABEL_ID.equals(update.getId())) {
										TrackerChoiceOptionDto newStatus = (TrackerChoiceOptionDto) resolver.getDefaultValue(update);
										if (newStatus != null) {
											targetTrans = targetDiagram.getTransition(targetStatusId, newStatus.getId());
										}
										break;
									}
								}

								if (targetTrans != null) {
									targetDiagram.setActive(targetTrans);
								} else {
									targetDiagram.setActiveStatus(targetStatusId);
								}

								TrackerLayoutLabelDto field = filter.getField();
								String fieldName = decorator.getText("tracker.field." + field.getLabel() + ".label", field.getLabel());
								String typeName = targetDiagram.getTracker().getType().getName();
								typeName = decorator.getText("tracker.type." + typeName + ".plural", typeName + "s");

								StringBuilder buf = new StringBuilder(50);
								buf.append(decorator.getText("tracker.transition." + transition.getName() + ".label", transition.getName()));
								buf.append(": ");
								if (filter.isOutgoing()) {
									buf.append(decorator.getText("issue.references.via.field.of.type", "{0} ({1})", fieldName, typeName));
								} else {
									buf.append(decorator.getText("issue.references.from.type.with.field", "{0} with {1}", typeName, fieldName));
								}

								if (targetTrans != null) {
									buf.append(" ").append(decorator.getText("tracker.transition." + targetTrans.getName() + ".label", targetTrans.getName()));
								}

								crossDiagramLinks.put(new IdentifiableFieldValueDto(diagram.getTracker(), transition.getToStatusId(), buf.toString(), targetTrackerId, targetStatusId), transition);
							}
						}
					}

					for (Integer refererId : workflowTransitionDao.getTrackersWithRefFieldUpdatesInTracker(diagram.getTracker().getId())) {
						StateDiagram referDiagram = stateDiagrams.get(refererId);
						if (referDiagram == null) {
							TrackerDto referingTracker = resolver.getEntityCache().getTracker(refererId);
							if (referingTracker != null && diagram.getDistance() < maxDistance) {
								referDiagram = new StateDiagram(referingTracker, workflowTransitionDao.findByTrackerId(refererId, true), resolver, diagram.getDistance() + 1);
								stateDiagrams.put(refererId, referDiagram);
								queue.add(referDiagram);
							}
						}
					}
				}
			}

			final Style inactive = new Style();
			inactive.color = "gray86";
			inactive.backgroundColor = "gray98";

			for (StateDiagram stateDiagram : stateDiagrams.values()) {
				if (!IdentifiableDto.equals(stateDiagram.getTracker(), tracker)) {
					String path = StringUtils.trimToNull(stateDiagram.getTracker().getScopeName());
					if (path == null) {
						path = stateDiagram.getTracker().isCategory() ? "CMDB" : "Trackers";
						path = decorator.getText(path, path);
					}

					chart.append("subgraph cluster_").append(stateDiagram.getTracker().getId()).append(" {\n");
					chart.append("label = \"").append(escapeLabel(tracker.getProject().getName())).append(" &raquo; ").append(escapeLabel(path)).append(" &raquo; ");
					chart.append(escapeLabel(trackerManager.getName(decorator.getTextLocale(), stateDiagram.getTracker())));
					chart.append("\";\n");
					chart.append("color=blue;\n");
					chart.append("style=dashed;\n");
					chart.append("fontcolor=blue;\n");
				} else if (stateDiagram.showUnset) {
					String label = decorator.getText("tracker.transition.unset.status", "Unset");
					String htmlLabel = escapeLabel(label);
					chart.append("state_").append(stateDiagram.getTracker().getId()).append("_null [shape=ellipse, fontsize=10, fontname=\"Arial Bold\", label=\"").append(htmlLabel).append("\" ]\n");
				}

				for (NamedDto state : stateDiagram.getStates()) {
					boolean active = stateDiagram.isActiveStatus(state.getId());
					Style style = active ? issueStatusStyles.getStyle(state, true) : inactive;

					chart.append("state_").append(stateDiagram.getTracker().getId()).append("_").append(state.getId());

					chart.append(" [shape=box, style=filled, fillcolor=\"").append(escapeLabel(style.backgroundColor)).append("\"");
					if (!active) {
						chart.append(", color=gray86");
					}
					chart.append(", fontcolor=\"").append(escapeLabel(style.color)).append("\" ");
//					if (contextPath != null) {
//						chart.append(", URL=\"").append(contextPath).append("/proj/tracker/choiceFieldOptions.spr?tracker_id=").append(stateDiagram.getTracker().getId()).append("&fieldId=7\" ");
//					}
					String label = decorator.getText("tracker.choice." + state.getName() + ".label", state.getName());
					String htmlLabel = escapeLabel(label);
					chart.append(", label=\"").append(htmlLabel);
					chart.append("\", fontsize=10, fontname=\"Arial Bold\" ]\n");
				}

				chart.append("\n");

				for (WorkflowTransitionDto transition : stateDiagram.getTransitions()) {
					if (!IdentifiableDto.equals(transition.getFromStatus(), WorkflowTransitionDto.ANY_STATUS) &&
						(IdentifiableDto.equals(stateDiagram.getTracker(), tracker) || (transition.getFromStatusId() != null && transition.getToStatusId() != null))) {

						chart.append("state_").append(stateDiagram.getTracker().getId()).append("_").append(transition.getFromStatusId());
						chart.append("->");
						chart.append("state_").append(stateDiagram.getTracker().getId()).append("_").append(transition.getToStatusId());

						if (stateDiagram.isActive(transition)) {
							String label = decorator.getText("tracker.transition." + transition.getName() + ".label", transition.getName());
							String htmlLabel = escapeLabel(label);
							chart.append(" [fontsize=10, label=\"").append(htmlLabel).append("\"");

							if (contextPath != null) {
								chart.append(", URL=\"").append(contextPath).append("/proj/tracker/stateTransition.spr?tracker_id=").append(stateDiagram.getTracker().getId()).append("&transitionId=").append(transition.getId()).append("\" ");
							}

							NamedDto guard = transition.getGuard();
							if (guard != null) {
								String taillabel = decorator.getText("tracker.view." + guard.getName() + ".label", guard.getName());
								String tailHtmlLabel = escapeLabel(taillabel);

								chart.append(", taillabel=\"[").append(tailHtmlLabel).append("]\"");

// This causes to much disturbance in the chart, especially if the same predicate is associated with multiple transitions !!!
//							} else {
//								StateTransitionPredicate condition = workflowTransitionDao.getTransitionPredicate(transition.getPredicateId());
//								if (condition != null) {
//									String taillabel = StringUtils.defaultIfEmpty(condition instanceof AbstractStateTransitionPredicate ? ((AbstractStateTransitionPredicate)condition).getLabel() : null, transition.getPredicateId());
//									String tailHtmlLabel = escapeLabel(taillabel);
//
//									chart.append(", taillabel=\"[").append(tailHtmlLabel).append("]\"");
//								}
							}

							String action = StringUtils.trimToNull(transition.getActionClass());
							if (action != null) {
								if (StringUtils.contains(action, "function ")) {
									action = "JavaScript";
								}
								String headlabel = decorator.getText("tracker.transition.action." + action + ".label", action);
								String headHtmlLabel = escapeLabel(headlabel);

								chart.append(", headlabel=\"/").append(headHtmlLabel).append("\"");
							}
						} else {
							chart.append(" [color=gray86");
						}
						chart.append("]\n");
					}
				}

				if (!IdentifiableDto.equals(stateDiagram.getTracker(), tracker)) {
					chart.append("}\n\n");
				}
			}

			if (crossDiagramLinks.size() > 0) {
				chart.append("\n\n");

				for (Map.Entry<IdentifiableFieldValueDto,WorkflowTransitionDto> crossDiagramLink : crossDiagramLinks.entrySet()) {
					IdentifiableFieldValueDto reference = crossDiagramLink.getKey();

					chart.append("state_").append(reference.getId()).append("_").append(reference.getFieldId());
					chart.append("->");
					chart.append("state_").append(reference.getTypeId()).append("_").append(reference.getOrdinal());

					chart.append(" [constraint=false, color=blue, fontsize=10, fontcolor=blue, label=\"").append(escapeLabel(reference.getStringValue())).append("\"");
					if (contextPath != null) {
						chart.append(", URL=\"").append(contextPath).append("/proj/tracker/stateTransition.spr?tracker_id=").append(reference.getId()).append("&transitionId=").append(crossDiagramLink.getValue().getId()).append("\"");
					}
					chart.append("]\n");
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
