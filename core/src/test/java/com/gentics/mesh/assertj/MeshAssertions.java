package com.gentics.mesh.assertj;

import org.assertj.core.api.Assertions;

import com.gentics.mesh.assertj.impl.FieldMapAssert;
import com.gentics.mesh.assertj.impl.FieldSchemaContainerAssert;
import com.gentics.mesh.assertj.impl.GroupResponseAssert;
import com.gentics.mesh.assertj.impl.JsonArrayAssert;
import com.gentics.mesh.assertj.impl.JsonObjectAssert;
import com.gentics.mesh.assertj.impl.MicronodeAssert;
import com.gentics.mesh.assertj.impl.MicronodeResponseAssert;
import com.gentics.mesh.assertj.impl.NavigationResponseAssert;
import com.gentics.mesh.assertj.impl.NodeAssert;
import com.gentics.mesh.assertj.impl.NodeGraphFieldContainerAssert;
import com.gentics.mesh.assertj.impl.NodeResponseAssert;
import com.gentics.mesh.assertj.impl.ProjectResponseAssert;
import com.gentics.mesh.assertj.impl.PublishStatusModelAssert;
import com.gentics.mesh.assertj.impl.PublishStatusResponseAssert;
import com.gentics.mesh.assertj.impl.ReleaseAssert;
import com.gentics.mesh.assertj.impl.ReleaseResponseAssert;
import com.gentics.mesh.assertj.impl.RoleResponseAssert;
import com.gentics.mesh.assertj.impl.SchemaAssert;
import com.gentics.mesh.assertj.impl.SchemaChangeModelAssert;
import com.gentics.mesh.assertj.impl.SchemaContainerAssert;
import com.gentics.mesh.assertj.impl.SchemaContainerVersionAssert;
import com.gentics.mesh.assertj.impl.SearchQueueBatchAssert;
import com.gentics.mesh.assertj.impl.TagFamilyResponseAssert;
import com.gentics.mesh.assertj.impl.TagResponseAssert;
import com.gentics.mesh.assertj.impl.UserResponseAssert;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainer;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.search.DummySearchProvider;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class MeshAssertions extends Assertions {

	public static DummySearchProviderAssert assertThat(DummySearchProvider actual) {
		return new DummySearchProviderAssert(actual);
	}

	public static NodeResponseAssert assertThat(NodeResponse actual) {
		return new NodeResponseAssert(actual);
	}

	public static SearchQueueBatchAssert assertThat(SearchQueueBatch actual) {
		return new SearchQueueBatchAssert(actual);
	}

	public static GroupResponseAssert assertThat(GroupResponse actual) {
		return new GroupResponseAssert(actual);
	}

	public static UserResponseAssert assertThat(UserResponse actual) {
		return new UserResponseAssert(actual);
	}

	public static RoleResponseAssert assertThat(RoleResponse actual) {
		return new RoleResponseAssert(actual);
	}

	public static ProjectResponseAssert assertThat(ProjectResponse actual) {
		return new ProjectResponseAssert(actual);
	}

	public static TagFamilyResponseAssert assertThat(TagFamilyResponse actual) {
		return new TagFamilyResponseAssert(actual);
	}

	public static TagResponseAssert assertThat(TagResponse actual) {
		return new TagResponseAssert(actual);
	}

	public static SchemaAssert assertThat(Schema actual) {
		return new SchemaAssert(actual);
	}

	public static JsonArrayAssert assertThat(JsonArray actual) {
		return new JsonArrayAssert(actual);
	}

	public static JsonObjectAssert assertThat(JsonObject actual) {
		return new JsonObjectAssert(actual);
	}

	public static MicronodeResponseAssert assertThat(MicronodeResponse actual) {
		return new MicronodeResponseAssert(actual);
	}

	public static NavigationResponseAssert assertThat(NavigationResponse actual) {
		return new NavigationResponseAssert(actual);
	}

	public static NodeAssert assertThat(Node actual) {
		return new NodeAssert(actual);
	}

	public static FieldSchemaContainerAssert assertThat(FieldSchemaContainer actual) {
		return new FieldSchemaContainerAssert(actual);
	}

	public static SchemaContainerAssert assertThat(GraphFieldSchemaContainer<?, ?, ?, ?> actual) {
		return new SchemaContainerAssert(actual);
	}

	public static SchemaContainerVersionAssert assertThat(GraphFieldSchemaContainerVersion<?, ?, ?, ?> actual) {
		return new SchemaContainerVersionAssert(actual);
	}

	public static SchemaChangeModelAssert assertThat(SchemaChangeModel actual) {
		return new SchemaChangeModelAssert(actual);
	}

	public static MicronodeAssert assertThat(Micronode actual) {
		return new MicronodeAssert(actual);
	}

	public static ReleaseAssert assertThat(Release actual) {
		return new ReleaseAssert(actual);
	}

	public static ReleaseResponseAssert assertThat(ReleaseResponse actual) {
		return new ReleaseResponseAssert(actual);
	}

	public static NodeGraphFieldContainerAssert assertThat(NodeGraphFieldContainer actual) {
		return new NodeGraphFieldContainerAssert(actual);
	}

	public static FieldMapAssert assertThat(FieldMap actual) {
		return new FieldMapAssert(actual);
	}

	public static PublishStatusResponseAssert assertThat(PublishStatusResponse actual) {
		return new PublishStatusResponseAssert(actual);
	}

	public static PublishStatusModelAssert assertThat(PublishStatusModel actual) {
		return new PublishStatusModelAssert(actual);
	}
}
