package com.gentics.mesh.core.graphql.javafilter;

import com.gentics.mesh.core.graphql.GraphQLReferencedByTest;
import com.gentics.mesh.test.MeshOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

@MeshTestSetting(testSize = TestSize.FULL, startServer = true, optionChanger = MeshOptionChanger.GRAPHQL_FORCE_JAVA_FILTER)
public class JavaGraphQLReferencedByTest extends GraphQLReferencedByTest {

}
