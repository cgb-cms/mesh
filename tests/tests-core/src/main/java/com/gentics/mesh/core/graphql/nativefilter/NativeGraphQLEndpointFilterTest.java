package com.gentics.mesh.core.graphql.nativefilter;

import org.junit.Test;

import com.gentics.mesh.core.graphql.GraphQLEndpointFilterTest;
import com.gentics.mesh.test.MeshOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

@MeshTestSetting(testSize = TestSize.FULL, startServer = true, optionChanger = MeshOptionChanger.GRAPHQL_FORCE_NATIVE_FILTER)
public class NativeGraphQLEndpointFilterTest extends GraphQLEndpointFilterTest {

	/**
	 * IsContainer filter is not supported natively, so we expect a NPE at parsing out the results.
	 */
	@Test(expected = NullPointerException.class)
	@Override
	public void testIsContainerFilter() {
		super.testIsContainerFilter();
	}
}
