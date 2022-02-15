package com.gentics.mesh.core.data.node.field.nesting;

import java.util.Optional;
import java.util.stream.Stream;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibTransformableField;
import com.gentics.mesh.core.rest.common.ReferenceType;
import com.gentics.mesh.core.rest.node.field.NodeField;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public interface HibNodeField extends HibListableField, HibTransformableField<NodeField> {

	Logger log = LoggerFactory.getLogger(HibNodeField.class);

	/**
	 * Returns the node for this field.
	 *
	 * @return Node for this field when set, otherwise null.
	 */
	HibNode getNode();

	/**
	 * Loads the content that is referencing another node.
	 * @return
	 */
	Stream<? extends HibNodeFieldContainer> getReferencingContents();

	/**
	 * Gets the name of the field where the node reference originated.
	 * @return
	 */
	String getFieldName();

	/**
	 * Gets the name of the field in the micronode where the node reference originated.
	 * Empty if the reference did not originate from a micronode.
	 * @return
	 */
	Optional<String> getMicronodeFieldName();

	/**
	 * Get an origin that this node field is referenced from.
	 * @return
	 */
	ReferenceType getReferenceType();
}
