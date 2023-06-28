package com.gentics.mesh.core.data.node;

import java.util.Collections;
import java.util.List;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.HibNodeFieldContainerEdge;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;

/**
 * Container object for handling nodes in combination with a specific known container.
 */
public class NodeContent {

	private HibNode node;
	private HibNodeFieldContainer container;
	private List<String> languageFallback;
	private ContainerType type;

	/**
	 * Create a new node content from content definition parts.
	 * 
	 * @param node
	 * @param container
	 * @param languageFallback
	 *            Language fallback list which was used to load the content
	 */
	public NodeContent(HibNode node, HibNodeFieldContainer container, List<String> languageFallback, ContainerType type) {
		this.node = node;
		this.container = container;
		this.languageFallback = languageFallback;
		this.type = type;
	}

	/**
	 * Create a new node content from content edge.
	 * 
	 * @param node
	 * @param edge
	 */
	public NodeContent(HibNode node, HibNodeFieldContainerEdge edge) {
		this.node = node;
		this.container = edge.getNodeContainer();
		this.languageFallback = Collections.singletonList(edge.getLanguageTag());
		this.type = edge.getType();
	}

	/**
	 * Return the node of the content.
	 * 
	 * @return
	 */
	public HibNode getNode() {
		ContentDao contentDao = Tx.get().contentDao();
		if (node == null && container != null) {
			node = contentDao.getNode(container);
		}
		return node;
	}

	public HibNodeFieldContainer getContainer() {
		return container;
	}

	public List<String> getLanguageFallback() {
		return languageFallback;
	}

	public ContainerType getType() {
		return type;
	}

}
