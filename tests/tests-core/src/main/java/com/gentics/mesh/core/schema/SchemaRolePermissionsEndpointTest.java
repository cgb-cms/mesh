package com.gentics.mesh.core.schema;

import static com.gentics.mesh.test.TestSize.FULL;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.rest.common.ObjectPermissionRequest;
import com.gentics.mesh.core.rest.common.ObjectPermissionResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractRolePermissionTest;
import com.gentics.mesh.test.context.ClientHandler;

/**
 * Test cases for handling role permissions for schemas
 */
@MeshTestSetting(testSize = FULL, startServer = true)
public class SchemaRolePermissionsEndpointTest extends AbstractRolePermissionTest {

	@Override
	protected HibBaseElement getTestedElement() {
		return schemaContainer("content");
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> getRolePermissions() {
		return () -> client().getSchemaRolePermissions(getTestedUuid());
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> grantRolePermissions(ObjectPermissionRequest request) {
		// TODO Auto-generated method stub
		return null;
	}
}
