package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.HibFieldContainer;
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

public class RestGetters {

	public static FieldGetter<HibStringField> STRING_GETTER = HibFieldContainer::getString;

	public static FieldGetter<HibStringFieldList> STRING_LIST_GETTER = HibFieldContainer::getStringList;

	public static FieldGetter<HibNumberField> NUMBER_GETTER = HibFieldContainer::getNumber;

	public static FieldGetter<HibNumberFieldList> NUMBER_LIST_GETTER = HibFieldContainer::getNumberList;

	public static FieldGetter<HibDateField> DATE_GETTER = HibFieldContainer::getDate;

	public static FieldGetter<HibDateFieldList> DATE_LIST_GETTER = HibFieldContainer::getDateList;

	public static FieldGetter<HibBooleanField> BOOLEAN_GETTER = HibFieldContainer::getBoolean;

	public static FieldGetter<HibBooleanFieldList> BOOLEAN_LIST_GETTER = HibFieldContainer::getBooleanList;

	public static FieldGetter<HibHtmlField> HTML_GETTER = HibFieldContainer::getHtml;

	public static FieldGetter<HibHtmlFieldList> HTML_LIST_GETTER = HibFieldContainer::getHTMLList;

	public static FieldGetter<HibMicronodeField> MICRONODE_GETTER = HibFieldContainer::getMicronode;

	public static FieldGetter<HibMicronodeFieldList> MICRONODE_LIST_GETTER = HibFieldContainer::getMicronodeList;

	public static FieldGetter<HibNodeField> NODE_GETTER = HibFieldContainer::getNode;

	public static FieldGetter<HibNodeFieldList> NODE_LIST_GETTER = HibFieldContainer::getNodeList;

	public static FieldGetter<HibBinaryField> BINARY_GETTER = HibFieldContainer::getBinary;

	public static FieldGetter<S3HibBinaryField> S3_BINARY_GETTER = HibFieldContainer::getS3Binary;
}
