package com.gentics.mesh.core.data.node.impl;

import static com.gentics.mesh.core.data.BranchParentEntry.branchParentEntry;
import static com.gentics.mesh.core.data.GraphFieldContainerEdge.WEBROOT_INDEX_NAME;
import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.BRANCH_PARENTS_KEY_PROPERTY;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ITEM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROOT_NODE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.PARENTS_KEY_PROPERTY;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.PROJECT_KEY_PROPERTY;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.SCHEMA_CONTAINER_KEY_PROPERTY;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_REFERENCE_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_TAGGED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UNTAGGED;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.INITIAL;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.common.ContainerType.forVersion;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.event.Assignment.ASSIGNED;
import static com.gentics.mesh.event.Assignment.UNASSIGNED;
import static com.gentics.mesh.madl.field.FieldType.STRING;
import static com.gentics.mesh.madl.field.FieldType.STRING_SET;
import static com.gentics.mesh.madl.index.VertexIndexDefinition.vertexIndex;
import static com.gentics.mesh.madl.type.VertexTypeDefinition.vertexType;
import static com.gentics.mesh.util.StreamUtil.toStream;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
import org.apache.commons.lang3.NotImplementedException;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.BranchParentEntry;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.TagEdge;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.generic.AbstractGenericFieldContainerVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.TagEdgeImpl;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.data.node.field.impl.NodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicTransformablePageImpl;
import com.gentics.mesh.core.data.page.impl.DynamicTransformableStreamPageImpl;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.search.BucketableElementHelper;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.NodeVersionConflictException;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.MeshProjectElementEventModel;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.event.node.NodeTaggedEventModel;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.PublishParameters;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;
import com.gentics.mesh.path.impl.PathSegmentImpl;
import com.gentics.mesh.util.StreamUtil;
import com.gentics.mesh.util.VersionNumber;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see Node
 */
public class NodeImpl extends AbstractGenericFieldContainerVertex<NodeResponse, Node> implements Node {

	private static final Logger log = LoggerFactory.getLogger(NodeImpl.class);

	/**
	 * Initialize the node vertex type and indices.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createType(vertexType(NodeImpl.class, MeshVertexImpl.class)
			.withField(PARENTS_KEY_PROPERTY, STRING_SET)
			.withField(BRANCH_PARENTS_KEY_PROPERTY, STRING_SET)
			.withField(PROJECT_KEY_PROPERTY, STRING)
			.withField(SCHEMA_CONTAINER_KEY_PROPERTY, STRING));

		index.createIndex(vertexIndex(NodeImpl.class)
			.withPostfix("project")
			.withField(PROJECT_KEY_PROPERTY, STRING));

		index.createIndex(vertexIndex(NodeImpl.class)
			.withPostfix("uuid")
			.withField("uuid", STRING));

		index.createIndex(vertexIndex(NodeImpl.class)
			.withPostfix("uuid_project")
			.withField("uuid", STRING)
			.withField(PROJECT_KEY_PROPERTY, STRING));

		index.createIndex(vertexIndex(NodeImpl.class)
			.withPostfix("schema")
			.withField(SCHEMA_CONTAINER_KEY_PROPERTY, STRING));

		index.createIndex(vertexIndex(NodeImpl.class)
			.withPostfix("parents")
			.withField(PARENTS_KEY_PROPERTY, STRING_SET));

		index.createIndex(vertexIndex(NodeImpl.class)
			.withPostfix("branch_parents")
			.withField(BRANCH_PARENTS_KEY_PROPERTY, STRING_SET));
	}

	@Override
	public String getPathSegment(String branchUuid, ContainerType type, boolean anyLanguage, String... languageTag) {

		// Check whether this node is the base node.
		if (getParentNode(branchUuid) == null) {
			return "";
		}

		// Find the first matching container and fallback to other listed languages
		HibNodeFieldContainer container = null;
		for (String tag : languageTag) {
			if ((container = getFieldContainer(tag, branchUuid, type)) != null) {
				break;
			}
		}

		if (container == null && anyLanguage) {
			Result<? extends GraphFieldContainerEdgeImpl> traversal = getFieldContainerEdges(branchUuid, type);

			if (traversal.hasNext()) {
				container = traversal.next().getNodeContainer();
			}
		}

		if (container != null) {
			return container.getSegmentFieldValue();
		}
		return null;
	}

	/**
	 * Postfix the path segment for the container that matches the given parameters. This operation is not needed for basenodes (since segment must be / for
	 * those anyway).
	 * 
	 * @param branchUuid
	 * @param type
	 * @param languageTag
	 */
	public void postfixPathSegment(String branchUuid, ContainerType type, String languageTag) {

		// Check whether this node is the base node.
		if (getParentNode(branchUuid) == null) {
			return;
		}

		// Find the first matching container and fallback to other listed languages
		HibNodeFieldContainer container = getFieldContainer(languageTag, branchUuid, type);
		if (container != null) {
			container.postfixSegmentFieldValue();
		}
	}

	@Override
	public void assertPublishConsistency(InternalActionContext ac, HibBranch branch) {

		String branchUuid = branch.getUuid();
		// Check whether the node got a published version and thus is published

		boolean isPublished = hasPublishedContent(branchUuid);

		// A published node must have also a published parent node.
		if (isPublished) {
			NodeImpl parentNode = getParentNode(branchUuid);

			// Only assert consistency of parent nodes which are not project
			// base nodes.
			if (parentNode != null && (!parentNode.getUuid().equals(getProject().getBaseNode().getUuid()))) {

				// Check whether the parent node has a published field container
				// for the given branch and language
				if (!parentNode.hasPublishedContent(branchUuid)) {
					log.error("Could not find published field container for node {" + parentNode.getUuid() + "} in branch {" + branchUuid + "}");
					throw error(BAD_REQUEST, "node_error_parent_containers_not_published", parentNode.getUuid());
				}
			}
		}

		// A draft node can't have any published child nodes.
		if (!isPublished) {

			for (HibNode node : getChildren(branchUuid)) {
				NodeImpl impl = (NodeImpl) node;
				if (impl.hasPublishedContent(branchUuid)) {
					log.error("Found published field container for node {" + node.getUuid() + "} in branch {" + branchUuid + "}. Node is child of {"
						+ getUuid() + "}");
					throw error(BAD_REQUEST, "node_error_children_containers_still_published", node.getUuid());
				}
			}
		}
	}

	@Override
	public Result<HibTag> getTags(HibBranch branch) {
		return new TraversalResult<>(TagEdgeImpl.getTagTraversal(this, branch).frameExplicit(TagImpl.class));
	}

	@Override
	public boolean hasTag(HibTag tag, HibBranch branch) {
		return TagEdgeImpl.hasTag(this, tag, branch);
	}

	private boolean hasPublishedContent(String branchUuid) {
		return GraphFieldContainerEdgeImpl.matchesBranchAndType(getId(), branchUuid, PUBLISHED);
	}

	@Override
	public Result<HibNodeFieldContainer> getFieldContainers(String branchUuid, ContainerType type) {
		Result<GraphFieldContainerEdgeImpl> it = GraphFieldContainerEdgeImpl.findEdges(this.getId(), branchUuid, type);
		Iterator<NodeGraphFieldContainer> it2 = it.stream().map(e -> e.getNodeContainer()).iterator();
		return new TraversalResult<>(it2);
	}

	@Override
	public Result<HibNodeFieldContainer> getFieldContainers(ContainerType type) {
		return new TraversalResult<>(
			outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode()).inV()
				.frameExplicit(NodeGraphFieldContainerImpl.class));
	}

	@Override
	@SuppressWarnings("unchecked")
	public long getFieldContainerCount() {
		return outE(HAS_FIELD_CONTAINER).or(e -> e.traversal().has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, DRAFT.getCode()), e -> e.traversal()
			.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, PUBLISHED.getCode())).inV().count();
	}

	@Override
	public HibNodeFieldContainer getLatestDraftFieldContainer(String languageTag) {
		return getGraphFieldContainer(languageTag, getProject().getLatestBranch(), DRAFT, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public HibNodeFieldContainer getFieldContainer(String languageTag, HibBranch branch, ContainerType type) {
		return getGraphFieldContainer(languageTag, branch, type, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public HibNodeFieldContainer getFieldContainer(String languageTag) {
		return getGraphFieldContainer(languageTag, getProject().getLatestBranch().getUuid(), DRAFT, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public HibNodeFieldContainer getFieldContainer(String languageTag, String branchUuid, ContainerType type) {
		return getGraphFieldContainer(languageTag, branchUuid, type, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public HibNodeFieldContainer createFieldContainer(String languageTag, HibBranch branch, HibUser editor) {
		return createFieldContainer(languageTag, branch, editor, null, true);
	}

	@Override
	public HibNodeFieldContainer createFieldContainer(String languageTag, HibBranch branch, HibUser editor, HibNodeFieldContainer original,
		boolean handleDraftEdge) {
		NodeGraphFieldContainerImpl previous = null;
		EdgeFrame draftEdge = null;
		String branchUuid = branch.getUuid();

		// check whether there is a current draft version
		if (handleDraftEdge) {
			draftEdge = getGraphFieldContainerEdgeFrame(languageTag, branchUuid, DRAFT);
			if (draftEdge != null) {
				previous = draftEdge.inV().nextOrDefault(NodeGraphFieldContainerImpl.class, null);
			}
		}

		// Create the new container
		NodeGraphFieldContainerImpl newContainer = getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		newContainer.generateBucketId();
		if (original != null) {
			newContainer.setEditor(editor);
			newContainer.setLastEditedTimestamp();
			newContainer.setLanguageTag(languageTag);
			newContainer.setSchemaContainerVersion(original.getSchemaContainerVersion());
		} else {
			newContainer.setEditor(editor);
			newContainer.setLastEditedTimestamp();
			newContainer.setLanguageTag(languageTag);
			// We need create a new container with no reference. So use the latest version available to use.
			newContainer.setSchemaContainerVersion(branch.findLatestSchemaVersion(getSchemaContainer()));
		}
		if (previous != null) {
			// set the next version number
			newContainer.setVersion(previous.getVersion().nextDraft());
			previous.setNextVersion(newContainer);
		} else {
			// set the initial version number
			newContainer.setVersion(new VersionNumber());
		}

		// clone the original or the previous container
		if (original != null) {
			newContainer.clone(original);
		} else if (previous != null) {
			newContainer.clone(previous);
		}

		// remove existing draft edge
		if (draftEdge != null) {
			draftEdge.remove();
			newContainer.updateWebrootPathInfo(branchUuid, "node_conflicting_segmentfield_update");
		}
		// We need to update the display field property since we created a new
		// node graph field container.
		newContainer.updateDisplayFieldValue();

		if (handleDraftEdge) {
			// create a new draft edge
			GraphFieldContainerEdge edge = addFramedEdge(HAS_FIELD_CONTAINER, newContainer, GraphFieldContainerEdgeImpl.class);
			edge.setLanguageTag(languageTag);
			edge.setBranchUuid(branchUuid);
			edge.setType(DRAFT);
		}

		// if there is no initial edge, create one
		if (getGraphFieldContainerEdge(languageTag, branchUuid, INITIAL) == null) {
			GraphFieldContainerEdge initialEdge = addFramedEdge(HAS_FIELD_CONTAINER, newContainer, GraphFieldContainerEdgeImpl.class);
			initialEdge.setLanguageTag(languageTag);
			initialEdge.setBranchUuid(branchUuid);
			initialEdge.setType(INITIAL);
		}

		return newContainer;
	}

	@Override
	public EdgeFrame getGraphFieldContainerEdgeFrame(String languageTag, String branchUuid, ContainerType type) {
		return GraphFieldContainerEdgeImpl.findEdge(getId(), branchUuid, type.getCode(), languageTag);
	}

	/**
	 * Get all graph field edges.
	 *
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	protected Result<? extends GraphFieldContainerEdgeImpl> getFieldContainerEdges(String branchUuid, ContainerType type) {
		return GraphFieldContainerEdgeImpl.findEdges(getId(), branchUuid, type);
	}

	@Override
	public void addTag(HibTag tag, HibBranch branch) {
		removeTag(tag, branch);
		TagEdge edge = addFramedEdge(HAS_TAG, toGraph(tag), TagEdgeImpl.class);
		edge.setBranchUuid(branch.getUuid());
	}

	@Override
	public void removeTag(HibTag tag, HibBranch branch) {
		outE(HAS_TAG).has(TagEdgeImpl.BRANCH_UUID_KEY, branch.getUuid()).mark().inV().retain(toGraph(tag)).back().removeAll();
	}

	@Override
	public void removeAllTags(HibBranch branch) {
		outE(HAS_TAG).has(TagEdgeImpl.BRANCH_UUID_KEY, branch.getUuid()).removeAll();
	}

	@Override
	public void setSchemaContainer(HibSchema schema) {
		property(SCHEMA_CONTAINER_KEY_PROPERTY, schema.getUuid());
	}

	@Override
	public HibSchema getSchemaContainer() {
		String uuid = property(SCHEMA_CONTAINER_KEY_PROPERTY);
		if (uuid == null) {
			return null;
		}
		return db().index().findByUuid(SchemaContainerImpl.class, uuid);
	}

	@Override
	public Result<HibNode> getChildren() {
		return new TraversalResult<>(graph.frameExplicit(db().getVertices(
			NodeImpl.class,
			new String[] { PARENTS_KEY_PROPERTY },
			new Object[] { getUuid() }), NodeImpl.class));
	}

	@Override
	public Result<HibNode> getChildren(String branchUuid) {
		return new TraversalResult<>(graph.frameExplicit(getUnframedChildren(branchUuid), NodeImpl.class));
	}

	private Iterator<Vertex> getUnframedChildren(String branchUuid) {
		return db().getVertices(
			NodeImpl.class,
			new String[] { BRANCH_PARENTS_KEY_PROPERTY },
			new Object[] { branchParentEntry(branchUuid, getUuid()).encode() });
	}

	@Override
	public Stream<Node> getChildrenStream(InternalActionContext ac) {
		HibUser user = ac.getUser();
		Tx tx = GraphDBTx.getGraphTx();
		UserDao userDao = tx.userDao();
		return toStream(getUnframedChildren(tx.getBranch(ac).getUuid()))
			.filter(node -> {
				Object id = node.getId();
				return userDao.hasPermissionForId(user, id, READ_PERM) || userDao.hasPermissionForId(user, id, READ_PUBLISHED_PERM);
			}).map(node -> graph.frameElementExplicit(node, NodeImpl.class));
	}

	@Override
	public NodeImpl getParentNode(String branchUuid) {
		Set<String> parents = property(BRANCH_PARENTS_KEY_PROPERTY);
		if (parents == null) {
			return null;
		} else {
			return parents.stream()
				.map(BranchParentEntry::fromString)
				.filter(entry -> entry.getBranchUuid().equals(branchUuid))
				.findAny()
				.map(entry -> db().index().findByUuid(NodeImpl.class, entry.getParentUuid()))
				.orElse(null);
		}
	}

	@Override
	public void setParentNode(String branchUuid, HibNode parent) {
		String parentUuid = parent.getUuid();
		removeParent(branchUuid);
		addToStringSetProperty(PARENTS_KEY_PROPERTY, parentUuid);
		addToStringSetProperty(BRANCH_PARENTS_KEY_PROPERTY, branchParentEntry(branchUuid, parentUuid).encode());
	}

	@Override
	public HibProject getProject() {
		return db().index().findByUuid(ProjectImpl.class, property(PROJECT_KEY_PROPERTY));
	}

	@Override
	public void setProject(HibProject project) {
		property(PROJECT_KEY_PROPERTY, project.getUuid());
	}

	@Override
	public HibNode create(HibUser creator, HibSchemaVersion schemaVersion, HibProject project) {
		return create(creator, schemaVersion, project, project.getLatestBranch(), null);
	}

	/**
	 * Create a new node and make sure to delegate the creation request to the main node root aggregation node.
	 */
	@Override
	public HibNode create(HibUser creator, HibSchemaVersion schemaVersion, HibProject project, HibBranch branch, String uuid) {
		if (!isBaseNode() && !isVisibleInBranch(branch.getUuid())) {
			log.error(String.format("Error while creating node in branch {%s}: requested parent node {%s} exists, but is not visible in branch.",
				branch.getName(), getUuid()));
			throw error(NOT_FOUND, "object_not_found_for_uuid", getUuid());
		}

		Node node = toGraph(project).getNodeRoot().create(creator, schemaVersion, project, uuid);
		node.setParentNode(branch.getUuid(), this);
		node.setSchemaContainer(schemaVersion.getSchemaContainer());
		// setCreated(creator);
		return node;
	}

	/**
	 * Create a {@link NodeFieldListItem} that contains the reference to this node.
	 * 
	 * @param ac
	 * @param languageTags
	 * @return
	 */
	public NodeFieldListItem toListItem(InternalActionContext ac, String[] languageTags) {
		Tx tx = GraphDBTx.getGraphTx();
		// Create the rest field and populate the fields
		NodeFieldListItemImpl listItem = new NodeFieldListItemImpl(getUuid());
		String branchUuid = tx.getBranch(ac, getProject()).getUuid();
		ContainerType type = forVersion(new VersioningParametersImpl(ac).getVersion());
		if (ac.getNodeParameters().getResolveLinks() != LinkType.OFF) {
			listItem.setUrl(mesh().webRootLinkReplacer().resolve(ac, branchUuid, type, this, ac.getNodeParameters().getResolveLinks(),
				languageTags));
		}
		return listItem;
	}

	private void takeOffline(InternalActionContext ac, BulkActionContext bac, HibBranch branch, PublishParameters parameters) {
		// Handle recursion first to start at the leaves
		if (parameters.isRecursive()) {
			for (HibNode node : getChildren(branch.getUuid())) {
				NodeImpl impl = (NodeImpl) node;
				impl.takeOffline(ac, bac, branch, parameters);
			}
		}

		String branchUuid = branch.getUuid();

		Result<? extends GraphFieldContainerEdgeImpl> publishEdges = getFieldContainerEdges(branchUuid, PUBLISHED);

		// Remove the published edge for each found container
		publishEdges.forEach(edge -> {
			NodeGraphFieldContainer content = edge.getNodeContainer();
			bac.add(content.onTakenOffline(branchUuid));
			edge.remove();
			if (content.isAutoPurgeEnabled() && content.isPurgeable()) {
				content.purge(bac);
			}
		});

		assertPublishConsistency(ac, branch);

		bac.process();
	}

	@Override
	public void takeOffline(InternalActionContext ac, BulkActionContext bac) {
		Tx tx = GraphDBTx.getGraphTx();
		HibBranch branch = tx.getBranch(ac, getProject());
		PublishParameters parameters = ac.getPublishParameters();
		takeOffline(ac, bac, branch, parameters);
	}

	@Override
	public void takeOffline(InternalActionContext ac, BulkActionContext bac, HibBranch branch, String languageTag) {
		String branchUuid = branch.getUuid();

		// Locate the published container
		HibNodeFieldContainer published = getFieldContainer(languageTag, branchUuid, PUBLISHED);
		if (published == null) {
			throw error(NOT_FOUND, "error_language_not_found", languageTag);
		}
		bac.add(published.onTakenOffline(branchUuid));

		// Remove the "published" edge
		getGraphFieldContainerEdge(languageTag, branchUuid, PUBLISHED).remove();
		assertPublishConsistency(ac, branch);

		bac.process();
	}

	@Override
	public void setPublished(InternalActionContext ac, HibNodeFieldContainer container, String branchUuid) {
		String languageTag = container.getLanguageTag();
		boolean isAutoPurgeEnabled = container.isAutoPurgeEnabled();

		// Remove an existing published edge
		EdgeFrame currentPublished = getGraphFieldContainerEdgeFrame(languageTag, branchUuid, PUBLISHED);
		if (currentPublished != null) {
			// We need to remove the edge first since updateWebrootPathInfo will
			// check the published edge again
			NodeGraphFieldContainerImpl oldPublishedContainer = currentPublished.inV().nextOrDefaultExplicit(NodeGraphFieldContainerImpl.class, null);
			currentPublished.remove();
			oldPublishedContainer.updateWebrootPathInfo(branchUuid, "node_conflicting_segmentfield_publish");
			if (ac.isPurgeAllowed() && isAutoPurgeEnabled && oldPublishedContainer.isPurgeable()) {
				oldPublishedContainer.purge();
			}
		}

		if (ac.isPurgeAllowed()) {
			// Check whether a previous draft can be purged.
			HibNodeFieldContainer prev = container.getPreviousVersion();
			if (isAutoPurgeEnabled && prev != null && prev.isPurgeable()) {
				prev.purge();
			}
		}

		// create new published edge
		GraphFieldContainerEdge edge = addFramedEdge(HAS_FIELD_CONTAINER, toGraph(container), GraphFieldContainerEdgeImpl.class);
		edge.setLanguageTag(languageTag);
		edge.setBranchUuid(branchUuid);
		edge.setType(PUBLISHED);
		container.updateWebrootPathInfo(branchUuid, "node_conflicting_segmentfield_publish");
	}

	@Override
	public HibNodeFieldContainer findVersion(List<String> languageTags, String branchUuid, String version) {
		HibNodeFieldContainer fieldContainer = null;

		// TODO refactor the type handling and don't return INITIAL.
		ContainerType type = forVersion(version);

		for (String languageTag : languageTags) {

			// Don't start the version lookup using the initial version. Instead start at the end of the chain and use the DRAFT version instead.
			fieldContainer = getFieldContainer(languageTag, branchUuid, type == INITIAL ? DRAFT : type);

			// Traverse the chain downwards and stop once we found our target version or we reached the end.
			if (fieldContainer != null && type == INITIAL) {
				while (fieldContainer != null && !version.equals(fieldContainer.getVersion().toString())) {
					fieldContainer = fieldContainer.getPreviousVersion();
				}
			}

			// We found a container for one of the languages
			if (fieldContainer != null) {
				break;
			}
		}
		return fieldContainer;
	}

	@Override
	public Stream<HibNodeField> getInboundReferences() {
		return toStream(inE(HAS_FIELD, HAS_ITEM)
			.has(NodeGraphFieldImpl.class)
			.frameExplicit(NodeGraphFieldImpl.class));
	}

	@Override
	public void addReferenceUpdates(BulkActionContext bac) {
		Set<String> handledNodeUuids = new HashSet<>();

		getInboundReferences()
			.flatMap(HibNodeField::getReferencingContents)
			.forEach(nodeContainer -> {
				for (GraphFieldContainerEdgeImpl edge : toGraph(nodeContainer).inE(HAS_FIELD_CONTAINER, GraphFieldContainerEdgeImpl.class)) {
					ContainerType type = edge.getType();
					// Only handle published or draft contents
					if (type.equals(DRAFT) || type.equals(PUBLISHED)) {
						HibNode node = nodeContainer.getNode();
						String uuid = node.getUuid();
						String languageTag = nodeContainer.getLanguageTag();
						String branchUuid = edge.getBranchUuid();
						String key = uuid + languageTag + branchUuid + type.getCode();
						if (!handledNodeUuids.contains(key)) {
							bac.add(onReferenceUpdated(node.getUuid(), node.getSchemaContainer(), branchUuid, type, languageTag));
							handledNodeUuids.add(key);
						}
					}
				}
			});
	}

	@Override
	public void delete(BulkActionContext bac) {
		Tx.get().nodeDao().delete(this, bac, false, true);
	}

	@Override
	public void removeParent(String branchUuid) {
		Set<String> branchParents = property(BRANCH_PARENTS_KEY_PROPERTY);
		if (branchParents != null) {
			// Divide parents by branch uuid.
			Map<Boolean, Set<String>> partitions = branchParents.stream()
				.collect(Collectors.partitioningBy(
					parent -> BranchParentEntry.fromString(parent).getBranchUuid().equals(branchUuid),
					Collectors.toSet()));

			Set<String> removedParents = partitions.get(true);
			if (!removedParents.isEmpty()) {
				// If any parents were removed, we set the new set of parents back to the vertex.
				Set<String> newParents = partitions.get(false);
				property(BRANCH_PARENTS_KEY_PROPERTY, newParents);

				String removedParent = BranchParentEntry.fromString(removedParents.iterator().next()).getParentUuid();
				// If the removed parent is not parent of any other branch, remove it from the common parent set.
				boolean parentStillExists = newParents.stream()
					.anyMatch(parent -> BranchParentEntry.fromString(parent).getParentUuid().equals(removedParent));
				if (!parentStillExists) {
					Set<String> parents = property(PARENTS_KEY_PROPERTY);
					parents.remove(removedParent);
					property(PARENTS_KEY_PROPERTY, parents);
				}
			}
		}
	}

	private Stream<HibNode> getChildren(HibUser requestUser, String branchUuid, List<String> languageTags, ContainerType type) {
		InternalPermission perm = type == PUBLISHED ? READ_PUBLISHED_PERM : READ_PERM;
		UserDao userRoot = GraphDBTx.getGraphTx().userDao();
		ContentDao contentDao = Tx.get().contentDao();

		Predicate<HibNode> languageFilter = languageTags == null || languageTags.isEmpty()
			? item -> true
			: item -> languageTags.stream().anyMatch(languageTag -> contentDao.getFieldContainer(item, languageTag, branchUuid, type) != null);

		return getChildren(branchUuid).stream()
			.filter(languageFilter.and(item -> userRoot.hasPermission(requestUser, item, perm)));
	}

	@Override
	public Page<HibNode> getChildren(InternalActionContext ac, List<String> languageTags, String branchUuid, ContainerType type,
		PagingParameters pagingInfo) {
		return new DynamicTransformableStreamPageImpl<>(getChildren(ac.getUser(), branchUuid, languageTags, type), pagingInfo);
	}

	@Override
	public Page<? extends HibTag> getTags(HibUser user, PagingParameters params, HibBranch branch) {
		VertexTraversal<?, ?, ?> traversal = TagEdgeImpl.getTagTraversal(this, branch);
		return new DynamicTransformablePageImpl<>(user, traversal, params, READ_PERM, TagImpl.class);
	}

	/**
	 * Update the node language or create a new draft for the specific language. This method will also apply conflict detection and take care of deduplication.
	 *
	 *
	 * <p>
	 * Conflict detection: Conflict detection only occurs during update requests. Two diffs are created. The update request will be compared against base
	 * version graph field container (version which is referenced by the request). The second diff is being created in-between the base version graph field
	 * container and the latest version of the graph field container. This diff identifies previous changes in between those version. These both diffs are
	 * compared in order to determine their intersection. The intersection identifies those fields which have been altered in between both versions and which
	 * would now also be touched by the current request. This situation causes a conflict and the update would abort.
	 *
	 * <p>
	 * Conflict cases
	 * <ul>
	 * <li>Initial creates - No conflict handling needs to be performed</li>
	 * <li>Migration check - Nodes which have not yet migrated can't be updated</li>
	 * </ul>
	 *
	 *
	 * <p>
	 * Deduplication: Field values that have not been changed in between the request data and the last version will not cause new fields to be created in new
	 * version graph field containers. The new version graph field container will instead reference those fields from the previous graph field container
	 * version. Please note that this deduplication only applies to complex fields (e.g.: Lists, Micronode)
	 *
	 * @param ac
	 * @param batch
	 *            Batch which will be used to update the search index
	 * @return
	 */
	@Override
	public boolean update(InternalActionContext ac, EventQueueBatch batch) {
		NodeUpdateRequest requestModel = ac.fromJson(NodeUpdateRequest.class);
		if (isEmpty(requestModel.getLanguage())) {
			throw error(BAD_REQUEST, "error_language_not_set");
		}

		// Check whether the tags need to be updated
		List<TagReference> tags = requestModel.getTags();
		if (tags != null) {
			updateTags(ac, batch, requestModel.getTags());
		}

		// Set the language tag parameter here in order to return the updated language in the response
		String languageTag = requestModel.getLanguage();
		NodeParameters nodeParameters = ac.getNodeParameters();
		nodeParameters.setLanguages(languageTag);

		HibLanguage language = GraphDBTx.getGraphTx().languageDao().findByLanguageTag(languageTag);
		if (language == null) {
			throw error(BAD_REQUEST, "error_language_not_found", requestModel.getLanguage());
		}
		Tx tx = GraphDBTx.getGraphTx();
		NodeDao nodeDao = tx.nodeDao();
		HibBranch branch = tx.getBranch(ac, getProject());
		HibNodeFieldContainer latestDraftVersion = getFieldContainer(languageTag, branch, DRAFT);

		// Check whether this is the first time that an update for the given language and branch occurs. In this case a new container must be created.
		// This means that no conflict check can be performed. Conflict checks only occur for updates on existing contents.
		if (latestDraftVersion == null) {
			// Create a new field container
			latestDraftVersion = createFieldContainer(languageTag, branch, ac.getUser());

			// Check whether the node has a parent node in this branch, if not, the request is supposed to be a create request
			// and we get the parent node from this create request
			if (getParentNode(branch.getUuid()) == null) {
				NodeCreateRequest createRequest = JsonUtil.readValue(ac.getBodyAsString(), NodeCreateRequest.class);
				if (createRequest.getParentNode() == null || isEmpty(createRequest.getParentNode().getUuid())) {
					throw error(BAD_REQUEST, "node_missing_parentnode_field");
				}
				HibNode parentNode = nodeDao.loadObjectByUuid(getProject(), ac, createRequest.getParentNode().getUuid(), CREATE_PERM);
				Node graphParentNode = toGraph(parentNode);
				// check whether the parent node is visible in the branch
				if (!graphParentNode.isBaseNode() && !graphParentNode.isVisibleInBranch(branch.getUuid())) {
					log.error(
						String.format("Error while creating node in branch {%s}: requested parent node {%s} exists, but is not visible in branch.",
							branch.getName(), parentNode.getUuid()));
					throw error(NOT_FOUND, "object_not_found_for_uuid", createRequest.getParentNode().getUuid());
				}
				setParentNode(branch.getUuid(), graphParentNode);
			}

			latestDraftVersion.updateFieldsFromRest(ac, requestModel.getFields());
			batch.add(latestDraftVersion.onCreated(branch.getUuid(), DRAFT));
			return true;
		} else {
			String version = requestModel.getVersion();
			if (version == null) {
				log.debug("No version was specified. Assuming 'draft' for latest version");
				version = "draft";
			}

			// Make sure the container was already migrated. Otherwise the update can't proceed.
			HibSchemaVersion schemaVersion = latestDraftVersion.getSchemaContainerVersion();
			if (!latestDraftVersion.getSchemaContainerVersion().equals(branch.findLatestSchemaVersion(schemaVersion
				.getSchemaContainer()))) {
				throw error(BAD_REQUEST, "node_error_migration_incomplete");
			}

			// Load the base version field container in order to create the diff
			HibNodeFieldContainer baseVersionContainer = findVersion(requestModel.getLanguage(), branch.getUuid(), version);
			if (baseVersionContainer == null) {
				throw error(BAD_REQUEST, "node_error_draft_not_found", version, requestModel.getLanguage());
			}

			latestDraftVersion.getSchemaContainerVersion().getSchema().assertForUnhandledFields(requestModel.getFields());

			// TODO handle simplified case in which baseContainerVersion and
			// latestDraftVersion are equal
			List<FieldContainerChange> baseVersionDiff = baseVersionContainer.compareTo(latestDraftVersion);
			List<FieldContainerChange> requestVersionDiff = latestDraftVersion.compareTo(requestModel.getFields());

			// Compare both sets of change sets
			List<FieldContainerChange> intersect = baseVersionDiff.stream().filter(requestVersionDiff::contains).collect(Collectors.toList());

			// Check whether the update was not based on the latest draft version. In that case a conflict check needs to occur.
			if (!latestDraftVersion.getVersion().getFullVersion().equals(version)) {

				// Check whether a conflict has been detected
				if (intersect.size() > 0) {
					NodeVersionConflictException conflictException = new NodeVersionConflictException("node_error_conflict_detected");
					conflictException.setOldVersion(baseVersionContainer.getVersion().toString());
					conflictException.setNewVersion(latestDraftVersion.getVersion().toString());
					for (FieldContainerChange fcc : intersect) {
						conflictException.addConflict(fcc.getFieldCoordinates());
					}
					throw conflictException;
				}
			}

			// Make sure to only update those fields which have been altered in between the latest version and the current request. Remove
			// unaffected fields from the rest request in order to prevent duplicate references. We don't want to touch field that have not been changed.
			// Otherwise the graph field references would no longer point to older revisions of the same field.
			Set<String> fieldsToKeepForUpdate = requestVersionDiff.stream().map(e -> e.getFieldKey()).collect(Collectors.toSet());
			for (String fieldKey : requestModel.getFields().keySet()) {
				if (fieldsToKeepForUpdate.contains(fieldKey)) {
					continue;
				}
				if (log.isDebugEnabled()) {
					log.debug("Removing field from request {" + fieldKey + "} in order to handle deduplication.");
				}
				requestModel.getFields().remove(fieldKey);
			}

			// Check whether the request still contains data which needs to be updated.
			if (!requestModel.getFields().isEmpty()) {

				// Create new field container as clone of the existing
				HibNodeFieldContainer newDraftVersion = createFieldContainer(language.getLanguageTag(), branch, ac.getUser(),
					latestDraftVersion, true);
				// Update the existing fields
				newDraftVersion.updateFieldsFromRest(ac, requestModel.getFields());

				// Purge the old draft
				if (ac.isPurgeAllowed() && newDraftVersion.isAutoPurgeEnabled() && latestDraftVersion.isPurgeable()) {
					latestDraftVersion.purge();
				}

				latestDraftVersion = newDraftVersion;
				batch.add(newDraftVersion.onUpdated(branch.getUuid(), DRAFT));
				return true;
			}
		}
		return false;
	}

	@Override
	public Page<? extends HibTag> updateTags(InternalActionContext ac, EventQueueBatch batch) {
		Tx tx = GraphDBTx.getGraphTx();
		List<HibTag> tags = getTagsToSet(ac, batch);
		HibBranch branch = tx.getBranch(ac);
		applyTags(branch, tags, batch);
		HibUser user = ac.getUser();
		return getTags(user, ac.getPagingParameters(), branch);
	}

	@Override
	public void updateTags(InternalActionContext ac, EventQueueBatch batch, List<TagReference> list) {
		Tx tx = GraphDBTx.getGraphTx();
		List<HibTag> tags = getTagsToSet(list, ac, batch);
		HibBranch branch = tx.getBranch(ac);
		applyTags(branch, tags, batch);
	}

	private void applyTags(HibBranch branch, List<? extends HibTag> tags, EventQueueBatch batch) {
		List<HibTag> currentTags = getTags(branch).list();

		List<HibTag> toBeAdded = tags.stream()
			.filter(StreamUtil.not(new HashSet<>(currentTags)::contains))
			.collect(Collectors.toList());
		toBeAdded.forEach(tag -> {
			addTag(tag, branch);
			batch.add(onTagged(tag, branch, ASSIGNED));
		});

		List<HibTag> toBeRemoved = currentTags.stream()
			.filter(StreamUtil.not(new HashSet<>(tags)::contains))
			.collect(Collectors.toList());
		toBeRemoved.forEach(tag -> {
			removeTag(tag, branch);
			batch.add(onTagged(tag, branch, UNASSIGNED));
		});
	}

	@Override
	public void removeInitialFieldContainerEdge(HibNodeFieldContainer initial, String branchUUID) {
		toGraph(initial).inE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.BRANCH_UUID_KEY, branchUUID)
				.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.INITIAL.getCode()).removeAll();
	}

	private PathSegment getSegment(String branchUuid, ContainerType type, String segment) {
		NodeDao nodeDao = Tx.get().nodeDao();

		// Check the different language versions
		for (HibNodeFieldContainer container : nodeDao.getFieldContainers(this, branchUuid, type)) {
			SchemaModel schema = container.getSchemaContainerVersion().getSchema();
			String segmentFieldName = schema.getSegmentField();
			// First check whether a string field exists for the given name
			HibStringField field = container.getString(segmentFieldName);
			if (field != null) {
				String fieldValue = field.getString();
				if (segment.equals(fieldValue)) {
					return new PathSegmentImpl(container, field, container.getLanguageTag(), segment);
				}
			}

			// No luck yet - lets check whether a binary field matches the
			// segmentField
			HibBinaryField binaryField = container.getBinary(segmentFieldName);
			if (binaryField == null) {
				if (log.isDebugEnabled()) {
					log.debug("The node {" + getUuid() + "} did not contain a string or a binary field for segment field name {" + segmentFieldName
						+ "}");
				}
			} else {
				String binaryFilename = binaryField.getFileName();
				if (segment.equals(binaryFilename)) {
					return new PathSegmentImpl(container, binaryField, container.getLanguageTag(), segment);
				}
			}
			// No luck yet - lets check whether a S3 binary field matches the segmentField
			S3HibBinaryField s3Binary = container.getS3Binary(segmentFieldName);
			if (s3Binary == null) {
				if (log.isDebugEnabled()) {
					log.debug("The node {" + getUuid() + "} did not contain a string or a binary field for segment field name {" + segmentFieldName
							+ "}");
				}
			} else {
				String s3binaryFilename = s3Binary.getS3Binary().getFileName();
				if (segment.equals(s3binaryFilename)) {
					return new PathSegmentImpl(container, s3Binary, container.getLanguageTag(), segment);
				}
			}

		}
		return null;
	}

	@Override
	public Path resolvePath(String branchUuid, ContainerType type, Path path, Stack<String> pathStack) {
		if (pathStack.isEmpty()) {
			return path;
		}
		String segment = pathStack.pop();

		if (log.isDebugEnabled()) {
			log.debug("Resolving for path segment {" + segment + "}");
		}

		FramedGraph graph = GraphDBTx.getGraphTx().getGraph();
		String segmentInfo = GraphFieldContainerEdgeImpl.composeSegmentInfo(this, segment);
		Object key = GraphFieldContainerEdgeImpl.composeWebrootIndexKey(db(), segmentInfo, branchUuid, type);
		Iterator<? extends GraphFieldContainerEdge> edges = graph.getFramedEdges(WEBROOT_INDEX_NAME, key, GraphFieldContainerEdgeImpl.class)
			.iterator();
		if (edges.hasNext()) {
			GraphFieldContainerEdge edge = edges.next();
			NodeImpl childNode = (NodeImpl) edge.getNode();
			PathSegment pathSegment = childNode.getSegment(branchUuid, type, segment);
			if (pathSegment != null) {
				path.addSegment(pathSegment);
				return childNode.resolvePath(branchUuid, type, path, pathStack);
			}
		}
		return path;

	}

	@Override
	public HibUser getCreator() {
		return mesh().userProperties().getCreator(this);
	}

	@Override
	public MeshElementEventModel onDeleted() {
		throw new NotImplementedException("Use dedicated onDeleted method for nodes instead.");
	}

	@Override
	public MeshProjectElementEventModel createEvent(MeshEvent event) {
		NodeMeshEventModel model = new NodeMeshEventModel();
		model.setEvent(event);
		model.setProject(getProject().transformToReference());
		fillEventInfo(model);
		return model;
	}

	/**
	 * Create a new referenced element update event model.
	 * 
	 * @param uuid
	 *            Uuid of the referenced node
	 * @param schema
	 *            Schema of the referenced node
	 * @param branchUuid
	 *            Branch of the referenced node
	 * @param type
	 *            Type of the content that was updated (if known)
	 * @param languageTag
	 *            Language of the content that was updated (if known)
	 * @return
	 */
	private NodeMeshEventModel onReferenceUpdated(String uuid, HibSchema schema, String branchUuid, ContainerType type, String languageTag) {
		NodeMeshEventModel event = new NodeMeshEventModel();
		event.setEvent(NODE_REFERENCE_UPDATED);
		event.setUuid(uuid);
		event.setLanguageTag(languageTag);
		event.setType(type);
		event.setBranchUuid(branchUuid);
		event.setProject(getProject().transformToReference());
		if (schema != null) {
			event.setSchema(schema.transformToReference());
		}
		return event;
	}

	@Override
	public NodeTaggedEventModel onTagged(HibTag tag, HibBranch branch, Assignment assignment) {
		NodeTaggedEventModel model = new NodeTaggedEventModel();
		model.setTag(tag.transformToReference());

		model.setBranch(branch.transformToReference());
		model.setProject(getProject().transformToReference());
		model.setNode(transformToMinimalReference());

		switch (assignment) {
		case ASSIGNED:
			model.setEvent(NODE_TAGGED);
			break;

		case UNASSIGNED:
			model.setEvent(NODE_UNTAGGED);
			break;
		}

		return model;
	}

	@Override
	public boolean isBaseNode() {
		return inE(HAS_ROOT_NODE).hasNext();
	}

	@Override
	public boolean isVisibleInBranch(String branchUuid) {
		return GraphFieldContainerEdgeImpl.matchesBranchAndType(getId(), branchUuid, ContainerType.DRAFT);
	}

	@Override
	public void removeElement() {
		remove();
	}

	@Override
	public Integer getBucketId() {
		return BucketableElementHelper.getBucketId(this);
	}

	@Override
	public void setBucketId(Integer bucketId) {
		BucketableElementHelper.setBucketId(this, bucketId);
	}

	@Override
	public void generateBucketId() {
		BucketableElementHelper.generateBucketId(this);
	}
}
