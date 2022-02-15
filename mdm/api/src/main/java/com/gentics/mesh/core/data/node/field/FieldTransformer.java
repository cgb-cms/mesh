package com.gentics.mesh.core.data.node.field;

import java.util.function.Function;
import java.util.function.Supplier;

import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.rest.node.field.Field;

/**
 * Transformer for domain models of fields into REST models.
 * 
 * @param <T>
 *            REST model type of the field
 */
@FunctionalInterface
public interface FieldTransformer<T extends Field, R extends HibTransformableField<T>> {

	/**
	 * Load the field with the given key from the container and transform it to its rest representation.
	 * 
	 * @param fieldProvider
	 *            Provide the entity field
	 * @param transformParameters
	 * 			  Holds rest transform parameters
	 * @param fieldKey
	 *            Key of the field to be transformed
	 * @param parentNode
	 * @return
	 */
	T transform(Supplier<R> fieldProvider, FieldTransformParameters transformParameters, String fieldKey, Supplier<HibNode> parentNode);


	static <T extends Field, R extends HibTransformableField<T>> FieldTransformer<T, R> nullSafe() {
		return (supplier, transformParameters, fieldKey, parentNode) -> {
			R field = supplier.get();
			if (field == null) {
				return null;
			} else {
				return field.transformToRest(transformParameters);
			}
		};
	}
}
