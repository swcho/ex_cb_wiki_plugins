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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

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
import com.intland.codebeamer.manager.EntityReferenceManager;
import com.intland.codebeamer.persistence.dao.AssociationDao;
import com.intland.codebeamer.persistence.dao.TrackerItemDao;
import com.intland.codebeamer.persistence.dao.TrackerItemStatisticsDao;
import com.intland.codebeamer.persistence.dao.TrackerItemStatisticsDao.ItemStatsBuilder;
import com.intland.codebeamer.persistence.dao.TrackerLayoutDao;
import com.intland.codebeamer.persistence.dao.impl.EntityCache;
import com.intland.codebeamer.persistence.dto.ArtifactDto;
import com.intland.codebeamer.persistence.dto.AssociationDto;
import com.intland.codebeamer.persistence.dto.AssociationTypeDto;
import com.intland.codebeamer.persistence.dto.TrackerDto;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.TrackerItemReferenceFilterDto;
import com.intland.codebeamer.persistence.dto.TrackerItemRevisionDto;
import com.intland.codebeamer.persistence.dto.TrackerItemStatsDto;
import com.intland.codebeamer.persistence.dto.TrackerLayoutDto;
import com.intland.codebeamer.persistence.dto.TrackerLayoutLabelDto;
import com.intland.codebeamer.persistence.dto.TrackerTypeDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.dto.base.ProjectAwareDto;
import com.intland.codebeamer.persistence.dto.base.ReferableDto;
import com.intland.codebeamer.persistence.dto.base.ReferenceDto;
import com.intland.codebeamer.persistence.dto.base.VersionReferenceDto;
import com.intland.codebeamer.persistence.dto.base.VersionedReferableDto;
import com.intland.codebeamer.persistence.util.Baseline;
import com.intland.codebeamer.persistence.util.PersistenceUtils;
import com.intland.codebeamer.persistence.util.TrackerItemDeadlines;
import com.intland.codebeamer.persistence.util.TrackerItemFieldHandler;
import com.intland.codebeamer.security.ProjectRequestWrapper;
import com.intland.codebeamer.ui.view.IssueStatusStyles;
import com.intland.codebeamer.ui.view.IssueStatusStyles.Style;
import com.intland.codebeamer.ui.view.table.TrackerSimpleLayoutDecorator;
import com.intland.codebeamer.wiki.plugins.util.WikiPluginUtils;
import com.intland.codebeamer.utils.Base64;

/**
 * This plugin generates a graph of dependencies originating at the specified tracker item in graphviz dot notation
 * and delegates to the {@link GraphVizPlugin} for rendering
 * <p>
 * 		e.g. <code>[{TrackerItemDependencyGraph itemId=123 distance=4}]</code>
 * </p>
 * <p>
 * Parameters:
 * <ul>
 *   <li><b>itemId</b> comma-separated list of originating items for the dependency graph</li>
 *   <li><b>revision</b> baseline id, or null (head revision)</li>
 *   <li><b>distance</b> (optional) is the number of reference layers to follow/show, default is 1</li>
 *   <li><b>references</b> (optional) is the comma-separated list of issue references to expand, default is 1</li>
 * </ul>
 * </p>
 * @since CB-6.0
 * @author <a href="mailto:klaus.mehling@intland.com">Klaus Mehling</a>
 * @version $Id: $
 */
public class TrackerItemDependencyGraphPlugin implements WikiPlugin {
	private static final Logger log = Logger.getLogger(TrackerItemDependencyGraphPlugin.class);

	private static final String PARENT_FIELD = "Parent";

	private static final GraphVizPlugin renderer = new GraphVizPlugin();

	@Autowired
	private AssociationDao associationDao;
	@Autowired
	private TrackerItemDao trackerItemDao;
	@Autowired
	private TrackerLayoutDao trackerLayoutDao;
	@Autowired
	private TrackerItemStatisticsDao trackerItemStatisticsDao;
	@Autowired
	private IssueStatusStyles issueStatusStyles;


	public String execute(WikiContext context, Map params) throws PluginException {
		HttpServletRequest request = context.getHttpRequest();
		if (request == null) {
			return "";
		}
		ControllerUtils.autoWire(this, request);
		UserDto user = ControllerUtils.getCurrentUser(request);
		TrackerItemFieldHandler resolver = TrackerItemFieldHandler.getInstance(user);
		TrackerSimpleLayoutDecorator decorator = new TrackerSimpleLayoutDecorator(request);
		List<TrackerItemRevisionDto> items = Collections.emptyList();

		String itemIdString = WikiPluginUtils.getParameter(params, "itemId", null);
		if (itemIdString != null) {
			Set<Integer> itemIds = new HashSet<Integer>();
			Integer revision = NumberUtils.createInteger(StringUtils.trimToNull(WikiPluginUtils.getParameter(params, "revision", null)));

			for (StringTokenizer parser = new StringTokenizer(itemIdString, ",;: "); parser.hasMoreTokens(); ) {
				String token = StringUtils.trimToNull(parser.nextToken());
				if (token != null) {
					try {
						itemIds.add(NumberUtils.createInteger(token));
					} catch (Throwable ex) {
						log.warn("Invalid itemId=" + token, ex);
					}
				}
			}

			if (itemIds.size() > 0) {
				itemIdString = StringUtils.join(itemIds, '_');

				if (revision != null) {
					List<VersionReferenceDto<TrackerItemDto>> vrefs = new ArrayList<VersionReferenceDto<TrackerItemDto>>(itemIds.size());
					for (Integer itemId : itemIds) {
						vrefs.add(new VersionReferenceDto<TrackerItemDto>(EntityCache.TRACKER_ITEM_TYPE, itemId, revision));
					}

					items = trackerItemDao.findRevisions(user, vrefs);
					itemIdString = itemIdString + "_" + revision;

				} else {
					List<TrackerItemDto> found = trackerItemDao.findById(user, itemIds);
					if (found != null && found.size() > 0) {
						items = new ArrayList<TrackerItemRevisionDto>(found.size());

						for (TrackerItemDto item : found) {
							items.add(new TrackerItemRevisionDto(item, null, null));
						}
					}
				}
			}
		} else {
			ProjectAwareDto target = (ProjectAwareDto)request.getAttribute(ProjectRequestWrapper.PROJECT_AWARE_DTO);
			if (target instanceof TrackerItemDto) {
				TrackerItemDto item = (TrackerItemDto)target;
				items = Collections.singletonList(new TrackerItemRevisionDto(item, null, null));
				itemIdString = item.getId().toString();
			}
		}

		String result = StringUtils.EMPTY;

		if (items != null && items.size() > 0) {
			String chartUrl = StringUtils.trimToNull(WikiPluginUtils.getParameter(params, "chartUrl"));
			int distance = Math.max(1, NumberUtils.toInt(WikiPluginUtils.getParameter(params, "distance"), 1));

			Map<Integer,List<TrackerItemReferenceFilterDto>> itemRefs = new HashMap<Integer,List<TrackerItemReferenceFilterDto>>();

			String references = StringUtils.trimToNull(WikiPluginUtils.getParameter(params, "references"));
			if (references != null) {
				for (StringTokenizer parser = new StringTokenizer(references, ","); parser.hasMoreTokens(); ) {
					String token = StringUtils.trimToNull(parser.nextToken());
					if (token != null) {
						try {
							TrackerItemReferenceFilterDto refFilter = new TrackerItemReferenceFilterDto(Base64.decodeToString(token));
							if (refFilter.getFlags() != null && refFilter.getName() != null && refFilter.getTypeIds() != null) {
								List<TrackerItemReferenceFilterDto> refFilters = itemRefs.get(refFilter.getFlags());
								if (refFilters == null) {
									itemRefs.put(refFilter.getFlags(), refFilters = new ArrayList<TrackerItemReferenceFilterDto>());
								}
								refFilter.setFlags(null);
								refFilters.add(refFilter);
							}
						} catch (Exception ex) {
							log.warn("Decoding reference=" + token + " failed", ex);
						}
					}
				}
			}

			String chart = createItemDependencyGraph(user, request.getContextPath(), chartUrl, items, itemRefs, distance, resolver, decorator);

			try {
				Map<String,String> renderParams = new HashMap<String,String>(8);
				renderParams.put("title",  "ItemDependencyGraph_" + itemIdString);
				renderParams.put("filter", "dot");
				renderParams.put("imap",   "true");
				renderParams.put(PluginManager.PARAM_BODY, chart);

				result = renderer.execute(context, renderParams);
			} catch (Throwable ex) {
				log.warn(result, ex);

				if (ex.getCause() instanceof GraphvizIsNotAvailableException) {
					result = decorator.getText("tracker.item.dependency.graph.notAvailable", "TrackerItem dependency graph is not available");
				} else {
					result = decorator.getText("errors.message", "{0}", StringEscapeUtils.escapeHtml(ex.toString()));
				}
			}
		} else {
			result = "<span class=\"invalidfield\">" +
						decorator.getText("error.access.to.entity.denied", "User {0} has no permission to access {1} with id = {2}",
											user.getName(), decorator.getText("tracker.reference.choose.items", "Items/Issues"), itemIdString.replace('_', ','))
				     + "</span>";
		}

		return result;
	}


	protected String createItemDependencyGraph(UserDto user, String contextPath, String graphUrl, List<TrackerItemRevisionDto> items, Map<Integer,List<TrackerItemReferenceFilterDto>> references, int distance, TrackerItemFieldHandler resolver, TrackerSimpleLayoutDecorator decorator) {
		Baseline baseline = null;
		final Set<Integer> nextItemIds = new HashSet<Integer>();
		final Set<ReferenceDto<?>> nodes = new HashSet<ReferenceDto<?>>();
		Map<TrackerItemDto,Map<TrackerLayoutLabelDto,List<ReferenceDto<?>>>> itemFieldRefs = new HashMap<TrackerItemDto,Map<TrackerLayoutLabelDto,List<ReferenceDto<?>>>>();
		Map<Integer,AssociationDto<?,?>> assocMap = new HashMap<Integer,AssociationDto<?,?>>();
		Map<String,Map<String,Map<Integer,TrackerItemStatsDto>>> types = new HashMap<String,Map<String,Map<Integer,TrackerItemStatsDto>>>();
		Map<String,Integer> typeIds = new HashMap<String,Integer>();
		Set<Integer> expandIds = new HashSet<Integer>();
		Set<Integer> targetIds = new HashSet<Integer>();

		for (TrackerTypeDto type : TrackerTypeDto.TYPES) {
			typeIds.put(type.getName(), type.getId());
		}

		for (int i = 0; i < 99; ++i) {
			if (items != null && items.size() > 0) {
				for (TrackerItemRevisionDto item : items) {
					if (item != null && item.getDto() != null) {
						ReferenceDto<TrackerItemDto> itemRef = new ReferenceDto(item);
						nodes.remove(itemRef);
						nodes.add(itemRef);

						if (i == 0) {
							baseline = item.getBaseline();
						}
					}
				}

				for (TrackerItemRevisionDto itemRev : items) {
					if (itemRev != null && itemRev.getDto() != null) {
						TrackerItemDto item = itemRev.getDto();
						List<TrackerItemReferenceFilterDto> expand = references.get(item.getId());

						TrackerItemDto parent = item.getParentItem();
						if (parent != null && parent.getId() != null) {
							ReferenceDto<TrackerItemDto> parentRef = ReferenceDto.of(parent);
							if (nodes.add(parentRef)) {
								nextItemIds.add(parent.getId());
							}
						}

						if (item.getChildren() != null && item.getChildren().size() > 0) {
							boolean expandChildren = false;

							if (expand != null && expand.size() > 0) {
								for (TrackerItemReferenceFilterDto filter : expand) {
									if (filter != null && PARENT_FIELD.equals(filter.getName())) {
										expandChildren = true;
										break;
									}
								}
							}

							if (i < distance || expandChildren) {
								for (TrackerItemDto child : item.getChildren()) {
									ReferenceDto<TrackerItemDto> childRef = ReferenceDto.of(child);
									if (nodes.add(childRef)) {
										nextItemIds.add(child.getId());
									}
								}
							} else {
								TrackerItemStatsDto children = new TrackerItemStatsDto();
								children.setAllItems(item.getChildren().size());

								Map<Integer,TrackerItemStatsDto> refFld = null;
								Map<String,Map<Integer,TrackerItemStatsDto>> type = types.get(item.getTypeName());
								if (type == null) {
									types.put(item.getTypeName(), type = new HashMap<String,Map<Integer,TrackerItemStatsDto>>());
								} else {
									refFld = type.get(PARENT_FIELD);
								}

								if (refFld == null) {
									type.put(PARENT_FIELD, refFld = new HashMap<Integer,TrackerItemStatsDto>());
								}

								refFld.put(item.getId(), children);
							}
						}

						TrackerLayoutDto layout = resolver.getItemLayout(item);
						if (layout != null) {
							for (TrackerLayoutLabelDto field : layout.getFields()) {
								if (field.getReferenceFilters() != null && field.getReferenceFilters().size() > 0) {
									Object value = field.getValue(item);
									if (value instanceof Collection) {
										Collection values = (Collection) value;
										if (values.size() > 0) {
											List<ReferenceDto<?>> refs = new ArrayList<ReferenceDto<?>>(values.size());

											for (Object vobj : values) {
												if (vobj instanceof ReferableDto) {
													ReferenceDto<?> ref = ReferenceDto.of((ReferableDto)vobj);
													refs.add(ref);

													if (nodes.add(ref) && EntityCache.TRACKER_ITEM_TYPE.equals(ref.getTypeId())) {
														nextItemIds.add(ref.getId());
													}
												}
											}

											if (refs.size() > 0) {
												Map<TrackerLayoutLabelDto,List<ReferenceDto<?>>> fieldRefs = itemFieldRefs.get(item);
												if (fieldRefs == null) {
													itemFieldRefs.put(item, fieldRefs = new LinkedHashMap<TrackerLayoutLabelDto,List<ReferenceDto<?>>>());
												}
												fieldRefs.put(field, refs);
											}
										}
									}
								}
							}
						}


						if (i < distance && !(item.getTracker().isVersionCategory() || TrackerTypeDto.PLATFORM.isInstance(item.getTracker()))) {
							if (itemRev.getBaseline() == null) {
								expandIds.add(item.getId());
							} else {
								trackerItemStatisticsDao.buildHistoricReferringIssueStats(user, itemRev, null, null, null, new ItemStatsBuilder() {
									public void init(TrackerItemDeadlines deadlines) {
									}

									public void add(TrackerItemDto referer, TrackerItemFieldHandler handler) {
										ReferenceDto<TrackerItemDto> ref = ReferenceDto.of(referer);
										if (nodes.add(ref)) {
											nextItemIds.add(ref.getId());
										}
									}
								});
							}
						} else {
							if (expand != null && expand.size() > 0) {
								for (TrackerItemReferenceFilterDto filter : expand) {
									if (filter != null && !PARENT_FIELD.equals(filter.getName())) {
										Map<Integer,List<Integer>> refFieldsInTrackers = trackerLayoutDao.findReferencingFields(item.getTracker().getId()).get(filter.getName());
										if (refFieldsInTrackers != null && refFieldsInTrackers.size() > 0) {
											trackerItemStatisticsDao.buildHistoricReferringIssueStats(user, itemRev, refFieldsInTrackers, filter.getTypeIds(), null, new ItemStatsBuilder() {
												public void init(TrackerItemDeadlines deadlines) {
												}

												public void add(TrackerItemDto referer, TrackerItemFieldHandler handler) {
													ReferenceDto<TrackerItemDto> ref = ReferenceDto.of(referer);
													if (nodes.add(ref)) {
														nextItemIds.add(ref.getId());
													}
												}
											});
										}
									}
								}
							}

							if (itemRev.getBaseline() == null) {
								targetIds.add(item.getId());
							} else {
								Map<String,Map<String,TrackerItemStatsDto>> itemRefStats = trackerItemStatisticsDao.getReferringIssueStatsGroupedByFieldAndIssueType(user, itemRev, null, null, null, true);
								if (itemRefStats != null && itemRefStats.size() > 0) {
									for (Map.Entry<String,Map<String,TrackerItemStatsDto>> fieldRefs : itemRefStats.entrySet()) {
										String fieldName = fieldRefs.getKey();
										if (!TrackerItemStatisticsDao.TOTAL_STATS_NAME.equals(fieldName)) {
											for (Map.Entry<String,TrackerItemStatsDto> typeStats : fieldRefs.getValue().entrySet()) {
												if (typeIds.containsKey(typeStats.getKey())) {
													Map<Integer,TrackerItemStatsDto> field = null;

													Map<String,Map<Integer,TrackerItemStatsDto>> type = types.get(typeStats.getKey());
													if (type == null) {
														types.put(typeStats.getKey(), type = new HashMap<String,Map<Integer,TrackerItemStatsDto>>());
													} else {
														field = type.get(fieldName);
													}

													if (field == null) {
														type.put(fieldName, field = new HashMap<Integer,TrackerItemStatsDto>());
													}

													field.put(item.getId(), typeStats.getValue());
												}
											}
										}
									}
								}
							}
						}
					}
				}

				// References
				if (targetIds.size() > 0) {
					Map<Integer,Map<String,Map<String,TrackerItemStatsDto>>> itemRefStats = trackerItemStatisticsDao.getReferringIssueStatsGroupedByFieldAndIssueType(user, targetIds, null, null, null, null, true);
					if (itemRefStats != null && itemRefStats.size() > 0) {
						for (Map.Entry<Integer,Map<String,Map<String,TrackerItemStatsDto>>> itemRefs : itemRefStats.entrySet()) {
							for (Map.Entry<String,Map<String,TrackerItemStatsDto>> fieldRefs : itemRefs.getValue().entrySet()) {
								String fieldName = fieldRefs.getKey();
								if (!TrackerItemStatisticsDao.TOTAL_STATS_NAME.equals(fieldName)) {
									for (Map.Entry<String,TrackerItemStatsDto> typeStats : fieldRefs.getValue().entrySet()) {
										if (typeIds.containsKey(typeStats.getKey())) {
											Map<Integer,TrackerItemStatsDto> field = null;

											Map<String,Map<Integer,TrackerItemStatsDto>> type = types.get(typeStats.getKey());
											if (type == null) {
												types.put(typeStats.getKey(), type = new HashMap<String,Map<Integer,TrackerItemStatsDto>>());
											} else {
												field = type.get(fieldName);
											}

											if (field == null) {
												type.put(fieldName, field = new HashMap<Integer,TrackerItemStatsDto>());
											}

											field.put(itemRefs.getKey(), typeStats.getValue());
										}
									}
								}
							}
						}
					}
					targetIds.clear();
				}

				if (expandIds.size() > 0) {
					trackerItemStatisticsDao.buildReferringIssueStats(user, expandIds, null, null, null, null, new ItemStatsBuilder() {
						public void init(TrackerItemDeadlines deadlines) {
						}

						public void add(TrackerItemDto referer, TrackerItemFieldHandler handler) {
							ReferenceDto<TrackerItemDto> ref = ReferenceDto.of(referer);
							if (nodes.add(ref)) {
								nextItemIds.add(ref.getId());
							}
						}
					});

					expandIds.clear();
				}

				// Associations are not baselined !!
				if (baseline == null) {
					List<AssociationDto<?,?>> assocs = associationDao.findByItems(user, items, EntityCache.TRACKER_ITEM_TYPE.intValue(), true);
					if (assocs != null && assocs.size() > 0) {
						for (AssociationDto<?,?> assoc : assocs) {
							if (!assoc.isUrlReference()) {
								ReferenceDto<ReferableDto> from = new ReferenceDto<ReferableDto>(assoc.getFrom());
								if (nodes.add(from) && EntityCache.TRACKER_ITEM_TYPE.equals(from.getTypeId()) && i < distance) {
									nextItemIds.add(from.getId());
								}

								ReferenceDto<ReferableDto> to = new ReferenceDto<ReferableDto>(assoc.getTo());
								if (nodes.add(to) && EntityCache.TRACKER_ITEM_TYPE.equals(to.getTypeId())) {
									nextItemIds.add(to.getId());
								}

								assocMap.put(assoc.getId(), assoc);
							}
						}
					}
				}


				if (nextItemIds.size() > 0) {
					if (baseline != null) {
						List<VersionReferenceDto<TrackerItemDto>> vrefs = new ArrayList<VersionReferenceDto<TrackerItemDto>>(nextItemIds.size());
						for (Integer itemId : nextItemIds) {
							vrefs.add(new VersionReferenceDto<TrackerItemDto>(EntityCache.TRACKER_ITEM_TYPE, itemId, baseline.getId()));
						}

						items = trackerItemDao.findRevisions(user, items);

					} else {
						List<TrackerItemDto> found = trackerItemDao.findById(user, nextItemIds);
						if (found != null && found.size() > 0) {
							items = new ArrayList<TrackerItemRevisionDto>(found.size());

							for (TrackerItemDto item : found) {
								items.add(new TrackerItemRevisionDto(item, null, null));
							}
						}
					}

					nextItemIds.clear();

				} else {
					items = Collections.emptyList();
				}
			} else {
				break;
			}
		}

		// Subtract direct references from reference stats
		for (Map.Entry<TrackerItemDto,Map<TrackerLayoutLabelDto,List<ReferenceDto<?>>>> itemRefField : itemFieldRefs.entrySet()) {
			TrackerItemDto item = itemRefField.getKey();

			for (Map.Entry<TrackerLayoutLabelDto,List<ReferenceDto<?>>> refField : itemRefField.getValue().entrySet()) {
				TrackerLayoutLabelDto field = refField.getKey();

				for (ReferenceDto<?> reference : refField.getValue()) {
					if (EntityCache.TRACKER_ITEM_TYPE.equals(reference.getTypeId())) {
						Map<String,Map<Integer,TrackerItemStatsDto>> typeStats = types.get(item.getTypeName());
						if (typeStats != null) {
							Map<Integer,TrackerItemStatsDto> fieldStats = typeStats.get(field.getLabel());
							if (fieldStats != null) {
								TrackerItemStatsDto stats = fieldStats.get(reference.getId());
								if (stats != null) {
									int count = stats.getAllItems() - 1;
									if (count > 0) {
										stats.setAllItems(count);
									} else {
										fieldStats.remove(reference.getId());
									}
								}

								if (fieldStats.isEmpty()) {
									typeStats.remove(field.getLabel());
								}
							}

							if (typeStats.isEmpty()) {
								types.remove(item.getTypeName());
							}
						}
					}
				}
			}
		}

		StringBuilder chart = new StringBuilder(1024);

		chart.append("graph [nodesep=\".25\", ranksep=\"1\", rankdir=\"LR\", charset=\"UTF-8\"]\n");

		for (ReferenceDto<?> node : nodes) {
			chart.append("node_").append(node.getTypeId()).append("_").append(node.getId());

			if (node.getDto() instanceof TrackerItemDto) {
				TrackerItemDto item = (TrackerItemDto) node.getDto();
				TrackerDto tracker = item.getTracker();

				// Subtract direct children from parent reference stats
				TrackerItemDto parent = item.getParentItem();
				if (parent != null && parent.getId() != null) {
					Map<String,Map<Integer,TrackerItemStatsDto>> typeStats = types.get(item.getTypeName());
					if (typeStats != null) {
						Map<Integer,TrackerItemStatsDto> fieldStats = typeStats.get(PARENT_FIELD);
						if (fieldStats != null) {
							TrackerItemStatsDto stats = fieldStats.get(parent.getId());
							if (stats != null) {
								int count = stats.getAllItems() - 1;
								if (count > 0) {
									stats.setAllItems(count);
								} else {
									fieldStats.remove(parent.getId());
								}
							}

							if (fieldStats.isEmpty()) {
								typeStats.remove(PARENT_FIELD);
							}
						}

						if (typeStats.isEmpty()) {
							types.remove(item.getTypeName());
						}
					}
				}

				String type = escapeLabel(decorator.getText("tracker.type." + tracker.getItemName(), tracker.getItemName()));
				Style style = issueStatusStyles.getStyle(item.getStatus(), true);


				chart.append(" [shape=none, margin=0, fontcolor=black, fontname=\"Arial\", fontsize=10, ");
				chart.append("  URL=\"").append(contextPath).append(baseline != null ? item.getUrlLinkVersioned(baseline.getId()) : item.getUrlLink()).append("\", ");
				chart.append("  tooltip=\"").append(escapeLabel(item.toString())).append("\", ");
				chart.append("  label=<");
				chart.append(	"<table border=\"1\" cellborder=\"0\" cellspacing=\"0\" cellpadding=\"2\">");
				chart.append(		"<tr>");
				chart.append(			"<td bgcolor=\"").append(style.backgroundColor).append("\" valign=\"middle\">");
				chart.append(			 	"<font color=\"").append(style.color).append("\" point-size=\"8\">");
				chart.append(					"&#171;").append(type).append("&#187;");
				chart.append(				"</font>");
				chart.append(			"</td>");
				chart.append(		"</tr>");
				chart.append(		"<tr>");
				chart.append(			"<td bgcolor=\"").append(style.backgroundColor).append("\" valign=\"middle\">");
				chart.append(			 	"<font face=\"Arial Bold\" color=\"").append(style.color).append("\" >");
				chart.append(					escapeLabel(StringUtils.abbreviate(item.getName(), 25)));
				chart.append(				"</font>");
				chart.append(			"</td>");
				chart.append(		"</tr>");

				Map<TrackerLayoutLabelDto,List<ReferenceDto<?>>> fieldRefs = itemFieldRefs.get(item);
				if (fieldRefs != null && fieldRefs.size() > 0) {
					chart.append("<tr><td></td></tr>");

					for (TrackerLayoutLabelDto field : fieldRefs.keySet()) {
						chart.append("<tr>");
						chart.append(	"<td align=\"left\" port=\"").append(field.getId()).append("\">");
						chart.append( escapeLabel(decorator.getText("tracker.field." + field.getLabel() + ".label", field.getLabel())));
						chart.append(	"</td>");
						chart.append("</tr>");
					}
				}

				chart.append(   "</table>>");
				chart.append(" ];\n");

			} else {
				String type = EntityReferenceManager.getReferencedObjectsGeneralType(node.getTypeId().intValue()) + ".label";
				String desc = EntityReferenceManager.getReferencedObjectsType(node.getTypeId().intValue());
				String shape = "box";

				type = escapeLabel(decorator.getText(type, desc));

				if (node.getDto() instanceof ArtifactDto) {
					ArtifactDto artifact = (ArtifactDto) node.getDto();
					if (artifact.isDirectory()) {
						shape = "folder";
					} else if (artifact.isFile()) {
						shape = "note";
					}
				}

				chart.append(" [shape=\"").append(shape).append("\", style=filled, fillcolor=lightGrey, fontcolor=black, fontname=\"Arial\", fontsize=10, label=\"");
				chart.append("&#171;").append(type).append("&#187;\\n");
				chart.append(escapeLabel(StringUtils.abbreviate(node.getShortDescription(), 25)));
				chart.append("\", URL=\"").append(contextPath).append(baseline != null && node.getDto() instanceof VersionedReferableDto ? ((VersionedReferableDto)node.getDto()).getUrlLinkVersioned(baseline.getId()) : node.getUrlLink());
				chart.append("\", tooltip=\"").append(escapeLabel(node.getInterwikiLink() + " - " + node.getShortDescription()));
				chart.append("\" ];\n");
			}
		}

		Collator collator = Collator.getInstance(decorator.getTextLocale());
		Map<String,TreeMap<String,String>> sortedTypeFields = new HashMap<String,TreeMap<String,String>>(types.size());

		for (Map.Entry<String,Map<String,Map<Integer,TrackerItemStatsDto>>> tnode : types.entrySet()) {
			Integer typeId   = typeIds.get(tnode.getKey());
			String  typeName = escapeLabel(decorator.getText("tracker.type." + tnode.getKey() + ".plural", tnode.getKey()));

			chart.append("type_").append(typeId);

			chart.append(" [shape=none, margin=0, fontcolor=black, fontname=\"Arial\", fontsize=10, ");
			chart.append("  label=<");
			chart.append(	"<table border=\"1\" cellborder=\"0\" cellspacing=\"0\" cellpadding=\"2\">");
			chart.append(		"<tr>");
			chart.append(			"<td bgcolor=\"lightGrey\" valign=\"middle\">");
//			chart.append(			 	"<font point-size=\"12\">");
			chart.append(					"&#171;").append(typeName).append("&#187;");
//			chart.append(				"</font>");
			chart.append(			"</td>");
			chart.append(		"</tr>");
			chart.append("<tr><td></td></tr>");

			TreeMap<String,String> fields = new TreeMap<String,String>(collator);
			for (String field : tnode.getValue().keySet()) {
				fields.put(decorator.getText("tracker.field." + field + ".label", field), field);
			}

			sortedTypeFields.put(tnode.getKey(), fields);

			int port = 1;
			for (Map.Entry<String,String> field : fields.entrySet()) {
				if (!PARENT_FIELD.equals(field.getValue())) {
					chart.append("<tr>");
					chart.append(	"<td align=\"left\" port=\"").append(port).append("\">");
					chart.append( 		escapeLabel(field.getKey()));
					chart.append(	"</td>");
					chart.append("</tr>");
				}
				++port;
			}

			chart.append(   "</table>>");
			chart.append(" ];\n");
		}

		chart.append("\n");

		for (ReferenceDto<?> node : nodes) {
			if (node.getDto() instanceof TrackerItemDto) {
				TrackerItemDto item = (TrackerItemDto) node.getDto();
				TrackerItemDto parent = item.getParentItem();
				if (parent != null && parent.getId() != null) {
					chart.append("node_").append(node.getTypeId()).append("_").append(node.getId());
					chart.append(" -> ");
					chart.append("node_9_").append(parent.getId());
					chart.append(" [arrowhead=diamond, color=blue];\n");
				}
			}
		}

		if (itemFieldRefs.size() > 0) {
			chart.append("\n");

			for (Map.Entry<TrackerItemDto,Map<TrackerLayoutLabelDto,List<ReferenceDto<?>>>> itemRefField : itemFieldRefs.entrySet()) {
				TrackerItemDto item = itemRefField.getKey();

				for (Map.Entry<TrackerLayoutLabelDto,List<ReferenceDto<?>>> refField : itemRefField.getValue().entrySet()) {
					TrackerLayoutLabelDto field = refField.getKey();

					for (ReferenceDto<?> reference : refField.getValue()) {
						chart.append("node_9_").append(item.getId()).append(":").append(field.getId()).append(":e");
						chart.append(" -> ");
						chart.append("node_").append(reference.getTypeId()).append("_").append(reference.getId());
						chart.append(" [arrowhead=vee");
						chart.append("];\n");
					}
				}
			}
		}

		if (sortedTypeFields.size() > 0) {
			chart.append("\n");

			for (Map.Entry<String,TreeMap<String,String>> type : sortedTypeFields.entrySet()) {
				int port = 1;

				for (Map.Entry<String,String> field : type.getValue().entrySet()) {
					Map<Integer,TrackerItemStatsDto> itemStats = types.get(type.getKey()).get(field.getValue());

					for (Map.Entry<Integer,TrackerItemStatsDto> item : itemStats.entrySet()) {
						if (item.getValue().getAllItems() > 0) {
							Integer typeId = typeIds.get(type.getKey());

							if (PARENT_FIELD.equals(field.getValue())) {
								chart.append("type_").append(typeId);
								chart.append(" -> ");
								chart.append("node_9_").append(item.getKey());
								chart.append(" [arrowhead=diamond, color=blue, fontcolor=blue, fontname=\"Arial\", fontsize=10, taillabel=\"").append(item.getValue().getAllItems()).append("\"");
							} else {
								chart.append("type_").append(typeId).append(":").append(port).append(":e");
								chart.append(" -> ");
								chart.append("node_9_").append(item.getKey());
								chart.append(" [arrowhead=vee, fontcolor=black, fontname=\"Arial\", fontsize=10, taillabel=\"").append(item.getValue().getAllItems()).append("\"");
							}

							if (graphUrl != null) {
								chart.append(" URL=\"").append(graphUrl);
								if (graphUrl.contains("&references=")) {
									chart.append(",");
								} else {
									chart.append("&references=");
								}
								chart.append(Base64.encode(new TrackerItemReferenceFilterDto(field.getValue(), Collections.singleton(typeId), item.getKey(), Integer.valueOf(0)).toString()).replaceAll("\\s", ""));
								chart.append("\"");
							}
							chart.append("];\n");
						}
					}

					++port;
				}
			}
		}

		if (assocMap.size() > 0) {
			chart.append("\n");

			Map<Integer,AssociationTypeDto> typeMap = PersistenceUtils.createLookupMap(associationDao.findTypes());

			for (AssociationDto<?,?> assoc : assocMap.values()) {
				AssociationTypeDto type = typeMap.get(assoc.getTypeId());
				String label = escapeLabel(decorator.getText("association.typeId." + type.getName(), type.getName()));

				chart.append("node_").append(assoc.getFrom().getTypeId()).append("_").append(assoc.getFrom().getId());
				chart.append(" -> ");
				chart.append("node_").append(assoc.getTo().getTypeId()).append("_").append(assoc.getTo().getId());

				if (assoc.isSuspected()) {
					chart.append(" [dir=both, arrowhead=\"vee:vee\", color=\"dimgray:red\", ");
				} else {
					chart.append(" [arrowhead=vee, color=dimgray, ");
				}
				chart.append("fontcolor=black, fontname=\"Arial\", fontsize=10, label=\"").append(label).append("\"];\n");
			}
		}

		return chart.toString();
	}

	protected String escapeLabel(String string) {
		String label = HtmlUtils.htmlEscapeHex(StringUtils.defaultString(StringUtils.trimToNull(string), "--"));
		return label;
	}
}
