package com.gentics.mesh.core.graphql.nativefilter;

import com.gentics.mesh.core.graphql.GraphQLReferencedByTest;
import com.gentics.mesh.test.MeshCoreOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

@MeshTestSetting(testSize = TestSize.FULL, startServer = true, optionChanger = MeshCoreOptionChanger.GRAPHQL_FORCE_NATIVE_FILTER)
public class NativeGraphQLReferencedByTest extends GraphQLReferencedByTest {

}
