package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;

/**
 * Fetcher for fields from a content.
 */
@FunctionalInterface
public interface FieldGetter<T extends HibField & HibTransformableField<?>> {

	/**
	 * Fetch the graph field from the given container which is identified using the key.
	 * 
	 * @param container
	 * @param key
	 * @return
	 */
	T get(HibFieldContainer container, String key);
}
