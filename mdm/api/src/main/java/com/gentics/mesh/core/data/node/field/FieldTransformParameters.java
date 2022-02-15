package com.gentics.mesh.core.data.node.field;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;

/**
 * A wrapper class for parameters that influence entity to rest model transformation
 */
public interface FieldTransformParameters {

	InternalActionContext ac();

	String fieldKey();

	List<String> languageTags();

	int level();
}
