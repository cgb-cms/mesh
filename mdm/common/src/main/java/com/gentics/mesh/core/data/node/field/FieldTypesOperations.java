package com.gentics.mesh.core.data.node.field;

import static com.gentics.mesh.core.data.node.field.RestGetters.BINARY_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.BOOLEAN_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.BOOLEAN_LIST_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.DATE_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.DATE_LIST_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.HTML_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.HTML_LIST_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.MICRONODE_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.MICRONODE_LIST_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.NODE_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.NODE_LIST_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.NUMBER_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.NUMBER_LIST_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.S3_BINARY_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.STRING_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.STRING_LIST_GETTER;
import static com.gentics.mesh.core.data.node.field.RestTransformers.HTML_TRANSFORMER;
import static com.gentics.mesh.core.data.node.field.RestTransformers.STRING_TRANSFORMER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.BINARY_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.BOOLEAN_LIST_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.BOOLEAN_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.DATE_LIST_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.DATE_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.HTML_LIST_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.HTML_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.MICRONODE_LIST_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.MICRONODE_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.NODE_LIST_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.NODE_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.NUMBER_LIST_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.NUMBER_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.S3_BINARY_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.STRING_LIST_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.STRING_UPDATER;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.list.HibBooleanFieldList;
import com.gentics.mesh.core.data.node.field.list.HibDateFieldList;
import com.gentics.mesh.core.data.node.field.list.HibHtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.HibMicronodeFieldList;
import com.gentics.mesh.core.data.node.field.list.HibNodeFieldList;
import com.gentics.mesh.core.data.node.field.list.HibNumberFieldList;
import com.gentics.mesh.core.data.node.field.list.HibStringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.S3BinaryField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.google.common.collect.ImmutableMap;

/**
 * List of all graph field types.
 */
public class FieldTypesOperations<T extends Field, R extends HibField & HibTransformableField<T>> {

	public static final FieldTypesOperations<StringField, HibStringField> STRING = new FieldTypesOperations<>(STRING_TRANSFORMER, STRING_UPDATER, STRING_GETTER);

	public static final FieldTypesOperations<StringFieldListImpl, HibStringFieldList> STRING_LIST = new FieldTypesOperations<>(FieldTransformer.noTransform(), STRING_LIST_UPDATER, STRING_LIST_GETTER);

	public static final FieldTypesOperations<NumberField, HibNumberField> NUMBER = new FieldTypesOperations<>(FieldTransformer.noTransform(), NUMBER_UPDATER, NUMBER_GETTER);

	public static final FieldTypesOperations<NumberFieldListImpl, HibNumberFieldList> NUMBER_LIST = new FieldTypesOperations<>(FieldTransformer.noTransform(), NUMBER_LIST_UPDATER, NUMBER_LIST_GETTER);

	public static final FieldTypesOperations<DateField, HibDateField> DATE = new FieldTypesOperations<>(FieldTransformer.noTransform(), DATE_UPDATER, DATE_GETTER);

	public static final FieldTypesOperations<DateFieldListImpl, HibDateFieldList> DATE_LIST = new FieldTypesOperations<>(FieldTransformer.noTransform(), DATE_LIST_UPDATER, DATE_LIST_GETTER);

	public static final FieldTypesOperations<BooleanField, HibBooleanField> BOOLEAN = new FieldTypesOperations<>(FieldTransformer.noTransform(), BOOLEAN_UPDATER, BOOLEAN_GETTER);

	public static final FieldTypesOperations<BooleanFieldListImpl, HibBooleanFieldList> BOOLEAN_LIST = new FieldTypesOperations<>(FieldTransformer.noTransform(), BOOLEAN_LIST_UPDATER, BOOLEAN_LIST_GETTER);

	public static final FieldTypesOperations<HtmlField, HibHtmlField> HTML = new FieldTypesOperations<>(HTML_TRANSFORMER, HTML_UPDATER, HTML_GETTER);

	public static final FieldTypesOperations<HtmlFieldListImpl, HibHtmlFieldList> HTML_LIST = new FieldTypesOperations<>(FieldTransformer.noTransform(), HTML_LIST_UPDATER, HTML_LIST_GETTER);

	public static final FieldTypesOperations<MicronodeField, HibMicronodeField> MICRONODE = new FieldTypesOperations<>(FieldTransformer.noTransform(), MICRONODE_UPDATER, MICRONODE_GETTER);

	public static final FieldTypesOperations<MicronodeFieldList, HibMicronodeFieldList> MICRONODE_LIST = new FieldTypesOperations<>(FieldTransformer.noTransform(), MICRONODE_LIST_UPDATER, MICRONODE_LIST_GETTER);

	public static final FieldTypesOperations<NodeField, HibNodeField> NODE = new FieldTypesOperations<>(FieldTransformer.noTransform(), NODE_UPDATER, NODE_GETTER);

	public static final FieldTypesOperations<NodeFieldList, HibNodeFieldList> NODE_LIST = new FieldTypesOperations<>(FieldTransformer.noTransform(), NODE_LIST_UPDATER, NODE_LIST_GETTER);

	public static final FieldTypesOperations<BinaryField, HibBinaryField> BINARY = new FieldTypesOperations<>(FieldTransformer.noTransform(), BINARY_UPDATER, BINARY_GETTER);

	public static final FieldTypesOperations<S3BinaryField, S3HibBinaryField> S3BINARY = new FieldTypesOperations<>(FieldTransformer.noTransform(), S3_BINARY_UPDATER, S3_BINARY_GETTER);

	public static final Map<String, FieldTypesOperations<?, ?>> combinedTypeMap = ImmutableMap.<String, FieldTypesOperations<?, ?>>builder()
			.put("string", STRING)
			.put("number", NUMBER)
			.put("date", DATE)
			.put("boolean", BOOLEAN)
			.put("html", HTML)
			.put("micronode", MICRONODE)
			.put("node", NODE)
			.put("binary", BINARY)
			.put("s3binary", S3BINARY)
			.put("list.string", STRING_LIST)
			.put("list.number", NUMBER_LIST)
			.put("list.date", DATE_LIST)
			.put("list.boolean", BOOLEAN_LIST)
			.put("list.html", HTML_LIST)
			.put("list.micronode", MICRONODE_LIST)
			.put("list.node", NODE_LIST)
			.build();

	private final FieldTransformer<T> transformer;
	private final FieldUpdater updater;
	private final FieldGetter<R> getter;

	FieldTypesOperations(FieldTransformer<T> transformer, FieldUpdater updater, FieldGetter<R> getter) {
		this.transformer = transformer;
		this.updater = updater;
		this.getter = getter;
	}

	/**
	 * Return the specific internal field type for the given field schema.
	 * 
	 * @param schema
	 * @return
	 */
	public static FieldTypesOperations<?, ?> valueByFieldSchema(FieldSchema schema) {
		String combinedType = schema.getType();
		if (schema instanceof ListFieldSchema) {
			combinedType += "." + ((ListFieldSchema) schema).getListType();
		}

		return combinedTypeMap.getOrDefault(combinedType, null);
	}

	/**
	 * Invoke the type specific field transformer using the provided information.
	 * 
	 * @param container
	 *            Field container which will be used to load the fields
	 * @param ac
	 *            Action context
	 * @param fieldKey
	 *            Field key
	 * @param languageTags
	 *            Language tags used to apply language fallback
	 * @param level
	 *            Current level of transformation
	 * @param parentNode
	 * @return
	 */
	public Field getRestFieldFromEntity(HibFieldContainer container, InternalActionContext ac, String fieldKey,
										List<String> languageTags, int level, Supplier<HibNode> parentNode) {
		FieldTransformParameters transformParameters = fromParameters(ac, fieldKey, languageTags, level);
		R entityField = getter.get(container, fieldKey);
		T restField = entityField == null ? null : entityField.transformToRest(transformParameters);

		return transformer.transform(restField, transformParameters, parentNode);
	}

	/**
	 * Invoke the type specific field transformer using the provided information.
	 *
	 * @param restField
	 *            The rest field
	 * @param ac
	 *            Action context
	 * @param fieldKey
	 *            Field key
	 * @param languageTags
	 *            Language tags used to apply language fallback
	 * @param level
	 *            Current level of transformation
	 * @param parentNode
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Field transformField(Field restField, InternalActionContext ac, String fieldKey,
										List<String> languageTags, int level, Supplier<HibNode> parentNode) {
		FieldTransformParameters transformParameters = fromParameters(ac, fieldKey, languageTags, level);
		return transformer.transform((T) restField, transformParameters, parentNode);
	}

	/**
	 * Invoke the type specific field updater using the provided information.
	 * 
	 * @param container
	 *            Field container which will be used to load the fields
	 * @param ac
	 *            Action context
	 * @param fieldMap
	 *            Fieldmap which contains the source
	 * @param fieldKey
	 *            Field key
	 * @param fieldSchema
	 *            Field schema to be used to identify the type of the field
	 * @param schema
	 */
	public void updateField(HibFieldContainer container, InternalActionContext ac, FieldMap fieldMap, String fieldKey,
		FieldSchema fieldSchema, FieldSchemaContainer schema) {
		updater.update(container, ac, fieldMap, fieldKey, fieldSchema, schema);
	}

	/**
	 * Invoke the type specific field getter using the provided information.
	 * 
	 * @param container
	 *            Container from which the field should be loaded
	 * @param fieldSchema
	 *            Field schema which will be used to identify the field
	 * @return
	 */
	public HibField getField(HibFieldContainer container, FieldSchema fieldSchema) {
		return getter.get(container, fieldSchema.getName());
	}

	private FieldTransformParameters fromParameters(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {
		return new FieldTransformParameters() {
			@Override
			public InternalActionContext ac() {
				return ac;
			}

			@Override
			public String fieldKey() {
				return fieldKey;
			}

			@Override
			public List<String> languageTags() {
				return languageTags;
			}

			@Override
			public int level() {
				return level;
			}
		};
	}
}
