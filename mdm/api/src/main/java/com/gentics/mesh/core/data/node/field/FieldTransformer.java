package com.gentics.mesh.core.data.node.field;

import java.util.function.Supplier;

import javax.annotation.CheckForNull;

import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.rest.node.field.Field;

/**
 * Apply some additional mapping to the provided rest field
 *
 * 
 * @param <T>
 *            REST model type of the field
 */
@FunctionalInterface
public interface FieldTransformer<T extends Field> {

	/**
	 * Apply some additional transformation to the rest field
	 * 
	 * @param field
	 *            The field
	 * @param transformParameters
	 * 			  Holds rest transform parameters
	 * @param parentNode
	 * 			  The parent node
	 * @return
	 */
	T transform(@CheckForNull T field, FieldTransformParameters transformParameters, Supplier<HibNode> parentNode);

	static <T extends Field> FieldTransformer<T> noTransform() {
		return (field, transformParameters, parentNode) -> field;
	}
}
