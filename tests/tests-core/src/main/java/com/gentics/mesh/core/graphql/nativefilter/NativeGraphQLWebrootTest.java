package com.gentics.mesh.core.graphql.nativefilter;

import com.gentics.mesh.core.graphql.GraphQLWebrootTest;
import com.gentics.mesh.test.MeshOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

@MeshTestSetting(testSize = TestSize.FULL, startServer = true, optionChanger = MeshOptionChanger.GRAPHQL_FORCE_NATIVE_FILTER)
public class NativeGraphQLWebrootTest extends GraphQLWebrootTest {
}
