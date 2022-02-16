package com.gentics.mesh.core.schema.field;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;

import com.gentics.mesh.context.impl.MicronodeMigrationContextImpl;
import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.PersistingBranchDao;
import com.gentics.mesh.core.data.dao.PersistingMicroschemaDao;
import com.gentics.mesh.core.data.dao.PersistingSchemaDao;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.schema.HibAddFieldChange;
import com.gentics.mesh.core.data.schema.HibFieldTypeChange;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibRemoveFieldChange;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.HibUpdateFieldChange;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.DataAsserter;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;
import com.gentics.mesh.core.field.FieldSchemaCreator;
import com.gentics.mesh.core.field.FieldTestHelper;
import com.gentics.mesh.core.migration.MicronodeMigration;
import com.gentics.mesh.core.migration.NodeMigration;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.UUIDUtil;
import com.gentics.mesh.util.VersionNumber;

import io.reactivex.exceptions.CompositeException;

/**
 * Base class for all field migration tests
 */
public abstract class AbstractFieldMigrationTest extends AbstractMeshTest implements FieldMigrationTestcases {

	protected final static String NEWFIELD = "New field";
	protected final static String NEWFIELDVALUE = "New field value";
	protected final static String OLDFIELD = "Old field";
	protected final static String OLDFIELDVALUE = "Old field value";

	protected final static String INVALIDSCRIPT = "this is an invalid script";

	protected final static String KILLERSCRIPT = "function migrate(node, fieldname) {var System = Java.type('java.lang.System'); System.exit(0);}";

	protected NodeMigration nodeMigrationHandler;

	protected MicronodeMigration micronodeMigrationHandler;

	@Before
	public void setupDeps() {
		this.nodeMigrationHandler = meshDagger().nodeMigrationHandler();
		this.micronodeMigrationHandler = meshDagger().micronodeMigrationHandler();
	}

	/**
	 * Generic method to test migration where a field has been removed from the schema/microschema
	 * 
	 * @param creator
	 *            creator implementation
	 * @param dataProvider
	 *            data provider implementation
	 * @param fetcher
	 *            field fetcher implementation
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	protected void removeField(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher) throws InterruptedException,
		ExecutionException, TimeoutException {
		try (Tx tx = tx()) {
			if (getClass().isAnnotationPresent(MicroschemaTest.class)) {
				removeMicroschemaField(creator, dataProvider, fetcher);
			} else {
				removeSchemaField(creator, dataProvider, fetcher);
			}
		}
	}

	/**
	 * Generic method to test node migration where a field has been removed from the schema
	 * 
	 * @param creator
	 *            creator implementation
	 * @param dataProvider
	 *            data provider implementation
	 * @param fetcher
	 *            field fetcher implementation
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void removeSchemaField(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher) throws InterruptedException,
		ExecutionException, TimeoutException {
		NodeDao nodeDao = boot().nodeDao();
		PersistingSchemaDao schemaDao = (PersistingSchemaDao) boot().schemaDao();
		PersistingBranchDao branchDao = (PersistingBranchDao) boot().branchDao();

		String removedFieldName = "toremove";
		String persistentFieldName = "persistent";
		String schemaName = "migratedSchema";

		// create version 1 of the schema
		HibSchema container = schemaDao.createPersisted(UUIDUtil.randomUUID());
		container.generateBucketId();
		container.setName(container.getUuid());
		container.setCreated(user());
		HibSchemaVersion versionA = createSchemaVersion(Tx.get(), container, v -> {
			fillSchemaVersion(v, container, schemaName, "1.0", creator.create(persistentFieldName), creator.create(
						removedFieldName));
			container.setLatestVersion(v);
		});
		schemaDao.mergeIntoPersisted(container);		

		// create version 2 of the schema (with one field removed)
		HibSchemaVersion versionB = createSchemaVersion(container, schemaName, "2.0", creator.create(persistentFieldName));

		// link the schemas with the change in between
		HibRemoveFieldChange change = (HibRemoveFieldChange) schemaDao.createPersistedChange(versionA, SchemaChangeOperation.REMOVEFIELD);
		change.setFieldName(removedFieldName);
		change.setPreviousContainerVersion(versionA);
		change.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);
		versionA.setNextChange(change);

		// create a node based on the old schema
		EventQueueBatch batch = createBatch();
		branchDao.assignSchemaVersion(project().getLatestBranch(), user(), versionA, batch);
		HibUser user = user();
		String english = english();
		HibNode parentNode = folder("2015");
		HibNode node = nodeDao.create(parentNode, user, versionA, project());
		HibNodeFieldContainer englishContainer = boot().contentDao().createFieldContainer(node, english, node.getProject().getLatestBranch(),
			user);
		dataProvider.set(englishContainer, persistentFieldName);
		dataProvider.set(englishContainer, removedFieldName);

		// migrate the node
		branchDao.assignSchemaVersion(project().getLatestBranch(), user(), versionB, batch);
		CommonTx.get().commit();

		NodeMigrationActionContextImpl context = new NodeMigrationActionContextImpl();
		context.setProject(project());
		context.setBranch(project().getLatestBranch());
		context.setFromVersion(versionA);
		context.setToVersion(versionB);
		context.setStatus(DummyMigrationStatus.get());

		nodeMigrationHandler.migrateNodes(context).blockingAwait();

		// assert that migration worked
		assertThat(node).as("Migrated Node").isOf(container).hasTranslation("en");
		assertThat(boot().contentDao().getFieldContainer(node, "en")).as("Migrated field container").isOf(versionB);
		assertThat(fetcher.fetch(boot().contentDao().getFieldContainer(node, "en"), persistentFieldName))
			.as("Field '" + persistentFieldName + "'").isNotNull();
		assertThat(fetcher.fetch(boot().contentDao().getFieldContainer(node, "en"), removedFieldName)).as("Field '" + removedFieldName + "'")
			.isNull();
	}

	/**
	 * Generic method to test micronode migration where a field has been removed from the microschema
	 * 
	 * @param creator
	 *            creator implementation
	 * @param dataProvider
	 *            data provider implementation
	 * @param fetcher
	 *            field fetcher implementation
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void removeMicroschemaField(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher) throws InterruptedException,
		ExecutionException, TimeoutException {
		NodeDao nodeDao = boot().nodeDao();
		PersistingBranchDao branchDao = (PersistingBranchDao) boot().branchDao();
		PersistingMicroschemaDao microschemaDao = (PersistingMicroschemaDao) boot().microschemaDao();

		String removedFieldName = "toremove";
		String persistentFieldName = "persistent";
		String microschemaName = UUIDUtil.randomUUID();
		String micronodeFieldName = "micronodefield";

		// create version 1 of the microschema
		HibMicroschema container = microschemaDao.createPersisted(null);
		container.generateBucketId();
		container.setName(microschemaName);
		container.setCreated(user());
		HibMicroschemaVersion versionA = createMicroschemaVersion(container, microschemaName, "1.0", creator.create(persistentFieldName),
			creator.create(removedFieldName));

		// create version 2 of the microschema (with one field removed)
		HibMicroschemaVersion versionB = createMicroschemaVersion(container, microschemaName, "2.0", creator.create(persistentFieldName));

		// link the microschemas with the change in between
		HibRemoveFieldChange change = (HibRemoveFieldChange) microschemaDao.createPersistedChange(versionA, SchemaChangeOperation.REMOVEFIELD);
		change.setFieldName(removedFieldName);
		change.setPreviousContainerVersion(versionA);
		change.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a micronode based on the old microschema
		HibNode node = nodeDao.create(folder("2015"), user(), schemaContainer("content").getLatestVersion(), project());
		HibMicronodeField micronodeField = createMicronodefield(node, micronodeFieldName, versionA, dataProvider, persistentFieldName,
			removedFieldName);
		HibNodeFieldContainer oldContainer = boot().contentDao().getFieldContainer(node, "en");
		VersionNumber oldVersion = oldContainer.getVersion();

		// migrate the node
		branchDao.assignMicroschemaVersion(project().getLatestBranch(), user(), versionB, createBatch());
		CommonTx.get().commit();
		MicronodeMigrationContextImpl context = new MicronodeMigrationContextImpl();
		context.setBranch(project().getLatestBranch());
		context.setFromVersion(versionA);
		context.setToVersion(versionB);
		context.setStatus(DummyMigrationStatus.get());
		micronodeMigrationHandler.migrateMicronodes(context).blockingAwait(10, TimeUnit.SECONDS);

		// old container must be unchanged
		assertThat(oldContainer).as("Old container").hasVersion(oldVersion.toString());
		assertThat(micronodeField.getMicronode()).as("Old Micronode").isOf(versionA);

		// assert that migration worked
		HibNodeFieldContainer newContainer = boot().contentDao().getFieldContainer(node, "en");
		assertThat(newContainer).as("New container").hasVersion(oldVersion.nextDraft().toString());
		HibMicronodeField newMicronodeField = newContainer.getMicronode(micronodeFieldName);
		assertThat(newMicronodeField.getMicronode()).as("Migrated Micronode").isOf(versionB);
		assertThat(fetcher.fetch(newMicronodeField.getMicronode(), persistentFieldName)).as("Field '" + persistentFieldName + "'").isNotNull();
		assertThat(fetcher.fetch(newMicronodeField.getMicronode(), removedFieldName)).as("Field '" + removedFieldName + "'").isNull();
	}

	/**
	 * Generic method to test node migration where a field is renamed. Actually a new field is added (with the new name) and the old field is removed. Data
	 * Migration is done with a custom migration script
	 *
	 * @param creator
	 *            creator implementation
	 * @param dataProvider
	 *            data provider implementation
	 * @param fetcher
	 *            data fetcher implementation
	 * @param asserter
	 *            asserter implementation
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	protected void renameField(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher, DataAsserter asserter)
		throws InterruptedException, ExecutionException, TimeoutException {
		try (Tx tx = tx()) {
			if (getClass().isAnnotationPresent(MicroschemaTest.class)) {
				renameMicroschemaField(creator, dataProvider, fetcher, asserter);
			} else {
				renameSchemaField(creator, dataProvider, fetcher, asserter);
			}
		}
	}

	/**
	 * Generic method to test node migration where a field is renamed. Actually a new field is added (with the new name) and the old field is removed. Data
	 * Migration is done with a custom migration script
	 *
	 * @param creator
	 *            creator implementation
	 * @param dataProvider
	 *            data provider implementation
	 * @param fetcher
	 *            data fetcher implementation
	 * @param asserter
	 *            asserter implementation
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private void renameSchemaField(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher, DataAsserter asserter)
		throws InterruptedException, ExecutionException, TimeoutException {
		NodeDao nodeDao = boot().nodeDao();
		PersistingBranchDao branchDao = (PersistingBranchDao) boot().branchDao();
		PersistingSchemaDao schemaDao = ((PersistingSchemaDao) boot().schemaDao());

		String oldFieldName = "oldname";
		String newFieldName = "newname";
		String schemaName = "migratedSchema";

		// create version 1 of the schema
		HibSchema container = schemaDao.createPersisted(UUIDUtil.randomUUID());
		container.generateBucketId();
		container.setName(container.getUuid());
		container.setCreated(user());
		schemaDao.mergeIntoPersisted(container);
		HibSchemaVersion versionA = createSchemaVersion(Tx.get(), container, v -> {
			fillSchemaVersion(v, container, schemaName, "1.0", creator.create(oldFieldName));
			container.setLatestVersion(v);
		});

		// create version 2 of the schema (with the field renamed)
		FieldSchema newField = creator.create(newFieldName);
		HibSchemaVersion versionB = createSchemaVersion(container, schemaName, "2.0", newField);

		// link the schemas with the changes in between
		HibAddFieldChange addFieldChange = (HibAddFieldChange) schemaDao.createPersistedChange(versionB, SchemaChangeOperation.ADDFIELD);
		addFieldChange.setFieldName(newFieldName);
		addFieldChange.setType(newField.getType());

		HibRemoveFieldChange removeFieldChange = (HibRemoveFieldChange) schemaDao.createPersistedChange(versionA, SchemaChangeOperation.REMOVEFIELD);
		removeFieldChange.setFieldName(oldFieldName);

		addFieldChange.setPreviousContainerVersion(versionA);
		addFieldChange.setNextChange(removeFieldChange);
		removeFieldChange.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a node based on the old schema
		HibUser user = user();
		EventQueueBatch batch = createBatch();
		branchDao.assignSchemaVersion(project().getLatestBranch(), user, versionA, batch);
		String english = english();
		HibNode parentNode = folder("2015");
		HibNode node = nodeDao.create(parentNode, user, versionA, project());
		HibNodeFieldContainer englishContainer = boot().contentDao().createFieldContainer(node, english, node.getProject().getLatestBranch(),
			user);
		dataProvider.set(englishContainer, oldFieldName);

		// migrate the node
		branchDao.assignSchemaVersion(project().getLatestBranch(), user, versionB, batch);
		CommonTx.get().commit();

		NodeMigrationActionContextImpl context = new NodeMigrationActionContextImpl();
		context.setProject(project());
		context.setBranch(project().getLatestBranch());
		context.setFromVersion(versionA);
		context.setToVersion(versionB);
		context.setStatus(DummyMigrationStatus.get());
		nodeMigrationHandler.migrateNodes(context).blockingAwait();

		// assert that migration worked
		assertThat(node).as("Migrated Node").isOf(container).hasTranslation("en");
		assertThat(boot().contentDao().getFieldContainer(node, "en")).as("Migrated field container").isOf(versionB);
		assertThat(fetcher.fetch(boot().contentDao().getFieldContainer(node, "en"), oldFieldName)).as("Field '" + oldFieldName + "'").isNull();
		asserter.assertThat(boot().contentDao().getFieldContainer(node, "en"), newFieldName);
	}

	/**
	 * Generic method to test micronode migration where a field is renamed. Actually a new field is added (with the new name) and the old field is removed. Data
	 * Migration is done with a custom migration script
	 *
	 * @param creator
	 *            creator implementation
	 * @param dataProvider
	 *            data provider implementation
	 * @param fetcher
	 *            data fetcher implementation
	 * @param asserter
	 *            asserter implementation
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private void renameMicroschemaField(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher, DataAsserter asserter)
		throws InterruptedException, ExecutionException, TimeoutException {
		NodeDao nodeDao = boot().nodeDao();
		PersistingBranchDao branchDao = (PersistingBranchDao) boot().branchDao();
		PersistingMicroschemaDao microschemaDao = (PersistingMicroschemaDao) boot().microschemaDao();
		String oldFieldName = "oldname";
		String newFieldName = "newname";
		String microschemaName = UUIDUtil.randomUUID();
		String micronodeFieldName = "micronodefield";

		// create version 1 of the microschema
		HibMicroschema container = microschemaDao.createPersisted(null);
		container.generateBucketId();
		container.setName(microschemaName);
		container.setCreated(user());
		HibMicroschemaVersion versionA = createMicroschemaVersion(container, microschemaName, "1.0", creator.create(oldFieldName));

		// create version 2 of the microschema (with the field renamed)
		FieldSchema newField = creator.create(newFieldName);
		HibMicroschemaVersion versionB = createMicroschemaVersion(container, microschemaName, "2.0", newField);

		// link the microschemas with the changes in between
		HibAddFieldChange addFieldChange = (HibAddFieldChange) microschemaDao.createPersistedChange(versionA, SchemaChangeOperation.ADDFIELD);
		addFieldChange.setFieldName(newFieldName);
		addFieldChange.setType(newField.getType());

		HibRemoveFieldChange removeFieldChange = (HibRemoveFieldChange) microschemaDao.createPersistedChange(versionA, SchemaChangeOperation.REMOVEFIELD);
		removeFieldChange.setFieldName(oldFieldName);

		addFieldChange.setPreviousContainerVersion(versionA);
		addFieldChange.setNextChange(removeFieldChange);
		removeFieldChange.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a node based on the old schema
		HibNode node = nodeDao.create(folder("2015"), user(), schemaContainer("content").getLatestVersion(), project());
		HibMicronodeField micronodeField = createMicronodefield(node, micronodeFieldName, versionA, dataProvider, oldFieldName);
		HibNodeFieldContainer oldContainer = boot().contentDao().getFieldContainer(node, "en");
		VersionNumber oldVersion = oldContainer.getVersion();

		// migrate the micronode
		HibJob job = branchDao.assignMicroschemaVersion(project().getLatestBranch(), user(), versionB, createBatch());
		CommonTx.get().commit();
		if (job != null) {
			triggerAndWaitForJob(job.getUuid());
		} else {
			MicronodeMigrationContextImpl context = new MicronodeMigrationContextImpl();
			context.setBranch(project().getLatestBranch());
			context.setFromVersion(versionA);
			context.setToVersion(versionB);
			context.setStatus(DummyMigrationStatus.get());
			micronodeMigrationHandler.migrateMicronodes(context).blockingAwait(10,
				TimeUnit.SECONDS);
		}

		// old container must be unchanged
		assertThat(oldContainer).as("Old container").hasVersion(oldVersion.toString());
		assertThat(micronodeField.getMicronode()).as("Old Micronode").isOf(versionA);

		// assert that migration worked
		HibNodeFieldContainer newContainer = boot().contentDao().getFieldContainer(node, "en");
		assertThat(newContainer).as("New container").hasVersion(oldVersion.nextDraft().toString());
		HibMicronodeField newMicronodeField = newContainer.getMicronode(micronodeFieldName);
		assertThat(newMicronodeField.getMicronode()).as("Migrated Micronode").isOf(versionB);
		assertThat(fetcher.fetch(newMicronodeField.getMicronode(), oldFieldName)).as("Field '" + oldFieldName + "'").isNull();
		asserter.assertThat(newMicronodeField.getMicronode(), newFieldName);
	}

	/**
	 * Generic method to test node migration where the type of a field is changed
	 * 
	 * @param oldField
	 *            creator for the old field
	 * @param dataProvider
	 *            data provider for the old field
	 * @param oldFieldFetcher
	 *            field fetcher for the old field
	 * @param newField
	 *            creator for the new field
	 * @param asserter
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	protected void changeType(FieldSchemaCreator oldField, DataProvider dataProvider, FieldFetcher oldFieldFetcher, FieldSchemaCreator newField,
		DataAsserter asserter) throws InterruptedException, ExecutionException, TimeoutException {
		try (Tx tx = tx()) {
			if (getClass().isAnnotationPresent(MicroschemaTest.class)) {
				changeMicroschemaType(oldField, dataProvider, oldFieldFetcher, newField, asserter);
			} else {
				changeSchemaType(oldField, dataProvider, oldFieldFetcher, newField, asserter);
			}
		}
	}

	/**
	 * Generic method to test node migration where the type of a field is changed
	 * 
	 * @param oldField
	 *            creator for the old field
	 * @param dataProvider
	 *            data provider for the old field
	 * @param oldFieldFetcher
	 *            field fetcher for the old field
	 * @param newField
	 *            creator for the new field
	 * @param asserter
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void changeSchemaType(FieldSchemaCreator oldField, DataProvider dataProvider, FieldFetcher oldFieldFetcher, FieldSchemaCreator newField,
		DataAsserter asserter) throws InterruptedException, ExecutionException, TimeoutException {
		NodeDao nodeDao = boot().nodeDao();
		PersistingBranchDao branchDao = (PersistingBranchDao) boot().branchDao();
		PersistingSchemaDao schemaDao = (PersistingSchemaDao) boot().schemaDao();

		String fieldName = "changedfield";
		String schemaName = "schema_" + System.currentTimeMillis();

		// create version 1 of the schema
		FieldSchema oldFieldSchema = oldField.create(fieldName);
		HibSchema container = schemaDao.createPersisted(UUIDUtil.randomUUID());
		container.generateBucketId();
		container.setName(schemaName);
		container.setCreated(user());
		HibSchemaVersion versionA = createSchemaVersion(Tx.get(), container, v -> {
			fillSchemaVersion(v, container, schemaName, "1.0", oldFieldSchema);
			container.setLatestVersion(v);
		});
		schemaDao.mergeIntoPersisted(container);

		// create version 2 of the schema (with the field modified)
		FieldSchema newFieldSchema = newField.create(fieldName);
		HibSchemaVersion versionB = createSchemaVersion(container, schemaName, "2.0", newFieldSchema);

		// link the schemas with the change in between
		HibFieldTypeChange change = (HibFieldTypeChange) schemaDao.createPersistedChange(versionA, SchemaChangeOperation.CHANGEFIELDTYPE);
		change.setFieldName(fieldName);
		change.setType(newFieldSchema.getType());
		if (newFieldSchema instanceof ListFieldSchema) {
			change.setListType(((ListFieldSchema) newFieldSchema).getListType());
		}
		change.setPreviousContainerVersion(versionA);
		change.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);
		versionA.setNextChange(change);

		// create a node based on the old schema
		EventQueueBatch batch = createBatch();
		branchDao.assignSchemaVersion(project().getLatestBranch(), user(), versionA, batch);
		HibUser user = user();
		String english = english();
		HibNode parentNode = folder("2015");
		HibNode node = nodeDao.create(parentNode, user, versionA, project());
		HibNodeFieldContainer englishContainer = boot().contentDao().createFieldContainer(node, english, node.getProject().getLatestBranch(),
			user);
		dataProvider.set(englishContainer, fieldName);
		assertThat(englishContainer).isOf(versionA).hasVersion("0.1");

		if (dataProvider == FieldTestHelper.NOOP) {
			assertThat(oldFieldFetcher.fetch(englishContainer, fieldName)).as(OLDFIELD).isNull();
		} else {
			assertThat(oldFieldFetcher.fetch(englishContainer, fieldName)).as(OLDFIELD).isNotNull();
		}

		// migrate the node
		branchDao.assignSchemaVersion(project().getLatestBranch(), user(), versionB, batch);
		CommonTx.get().commit();

		NodeMigrationActionContextImpl context = new NodeMigrationActionContextImpl();
		context.setProject(project());
		context.setBranch(project().getLatestBranch());
		context.setFromVersion(versionA);
		context.setToVersion(versionB);
		context.setStatus(DummyMigrationStatus.get());

		nodeMigrationHandler.migrateNodes(context).blockingAwait();
		// old container must not be changed
		assertThat(englishContainer).isOf(versionA).hasVersion("0.1");
		// assert that migration worked
		HibNodeFieldContainer migratedContainer = boot().contentDao().getFieldContainer(node, "en");
		assertThat(migratedContainer).isOf(versionB).hasVersion("0.2");
		assertThat(node).as("Migrated Node").isOf(container).hasTranslation("en");

		if (!StringUtils.equals(oldFieldSchema.getType(), newFieldSchema.getType())) {
			assertThat(oldFieldFetcher.fetch(migratedContainer, fieldName)).as(OLDFIELD).isNull();
		}
		if ((oldFieldSchema instanceof ListFieldSchema) && (newFieldSchema instanceof ListFieldSchema) && !StringUtils.equals(
			((ListFieldSchema) oldFieldSchema).getListType(), ((ListFieldSchema) newFieldSchema).getListType())) {
			assertThat(oldFieldFetcher.fetch(migratedContainer, fieldName)).as(OLDFIELD).isNull();
		}
		asserter.assertThat(migratedContainer, fieldName);
	}

	/**
	 * Generic method to test micronode migration where the type of a field is changed
	 * 
	 * @param oldField
	 *            creator for the old field
	 * @param dataProvider
	 *            data provider for the old field
	 * @param oldFieldFetcher
	 *            field fetcher for the old field
	 * @param newField
	 *            creator for the new field
	 * @param asserter
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void changeMicroschemaType(FieldSchemaCreator oldField, DataProvider dataProvider, FieldFetcher oldFieldFetcher,
		FieldSchemaCreator newField, DataAsserter asserter) throws InterruptedException, ExecutionException, TimeoutException {
		NodeDao nodeDao = boot().nodeDao();
		PersistingMicroschemaDao microschemaDao = (PersistingMicroschemaDao) boot().microschemaDao();
		
		String fieldName = "changedfield";
		String microschemaName = UUIDUtil.randomUUID();
		String micronodeFieldName = "micronodefield";

		// create version 1 of the microschema
		FieldSchema oldFieldSchema = oldField.create(fieldName);
		HibMicroschema container = microschemaDao.createPersisted(null);
		container.setName(microschemaName);
		container.setCreated(user());
		HibMicroschemaVersion versionA = createMicroschemaVersion(container, microschemaName, "1.0", oldFieldSchema);

		// create version 2 of the microschema (with the field modified)
		FieldSchema newFieldSchema = newField.create(fieldName);
		HibMicroschemaVersion versionB = createMicroschemaVersion(container, microschemaName, "2.0", newFieldSchema);

		// link the schemas with the change in between
		HibFieldTypeChange change = (HibFieldTypeChange) microschemaDao.createPersistedChange(versionA, SchemaChangeOperation.CHANGEFIELDTYPE);
		change.setFieldName(fieldName);
		change.setType(newFieldSchema.getType());
		if (newFieldSchema instanceof ListFieldSchema) {
			change.setListType(((ListFieldSchema) newFieldSchema).getListType());
		}
		change.setPreviousContainerVersion(versionA);
		change.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		microschemaDao.assign(container, project(), user(), createBatch());
		HibNode node = nodeDao.create(folder("2015"), user(), schemaContainer("content").getLatestVersion(), project());

		// create a node based on the old schema
		HibMicronodeField micronodeField = createMicronodefield(node, micronodeFieldName, versionA, dataProvider, fieldName);
		HibNodeFieldContainer oldContainer = boot().contentDao().getFieldContainer(node, "en");
		VersionNumber oldVersion = oldContainer.getVersion();

		if (dataProvider == FieldTestHelper.NOOP) {
			assertThat(oldFieldFetcher.fetch(micronodeField.getMicronode(), fieldName)).as(OLDFIELD).isNull();
		} else {
			assertThat(oldFieldFetcher.fetch(micronodeField.getMicronode(), fieldName)).as(OLDFIELD).isNotNull();
		}

		// migrate the micronode
		MicronodeMigrationContextImpl context = new MicronodeMigrationContextImpl();
		context.setBranch(project().getLatestBranch());
		context.setFromVersion(versionA);
		context.setToVersion(versionB);
		context.setStatus(DummyMigrationStatus.get());
		micronodeMigrationHandler.migrateMicronodes(context).blockingAwait(10,
			TimeUnit.SECONDS);

		// old container must be untouched
		micronodeField = oldContainer.getMicronode(micronodeFieldName);
		assertThat(oldContainer).as("Old container").hasVersion(oldVersion.toString());
		assertThat(micronodeField.getMicronode()).as("Old micronode").isOf(versionA);

		// assert that migration worked
		HibNodeFieldContainer newContainer = boot().contentDao().getFieldContainer(node, "en");
		assertThat(newContainer).as("New container").hasVersion(oldVersion.nextDraft().toString());
		HibMicronodeField newMicronodeField = newContainer.getMicronode(micronodeFieldName);
		assertThat(newMicronodeField.getMicronode()).as("Migrated Micronode").isOf(versionB);

		if (!StringUtils.equals(oldFieldSchema.getType(), newFieldSchema.getType())) {
			assertThat(oldFieldFetcher.fetch(newMicronodeField.getMicronode(), fieldName)).as(OLDFIELD).isNull();
		}
		if ((oldFieldSchema instanceof ListFieldSchema) && (newFieldSchema instanceof ListFieldSchema) && !StringUtils.equals(
			((ListFieldSchema) oldFieldSchema).getListType(), ((ListFieldSchema) newFieldSchema).getListType())) {
			assertThat(oldFieldFetcher.fetch(newMicronodeField.getMicronode(), fieldName)).as(OLDFIELD).isNull();
		}
		asserter.assertThat(newMicronodeField.getMicronode(), fieldName);
	}

	/**
	 * Generic test for migrating an existing field with a custom migration script
	 * 
	 * @param creator
	 *            creator implementation
	 * @param dataProvider
	 *            data provider implementation
	 * @param fetcher
	 *            fetcher implementation
	 * @param migrationScript
	 *            migration script to test
	 * @param asserter
	 *            assert implementation
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	protected void customMigrationScript(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher, String migrationScript,
		DataAsserter asserter) throws InterruptedException, ExecutionException, TimeoutException {
		try (Tx tx = tx()) {
			if (getClass().isAnnotationPresent(MicroschemaTest.class)) {
				customMicroschemaMigrationScript(creator, dataProvider, fetcher, migrationScript, asserter);
			} else {
				customSchemaMigrationScript(creator, dataProvider, fetcher, migrationScript, asserter);
			}
		}
	}

	/**
	 * Generic test for migrating an existing field with a custom migration script
	 * 
	 * @param creator
	 *            creator implementation
	 * @param dataProvider
	 *            data provider implementation
	 * @param fetcher
	 *            fetcher implementation
	 * @param migrationScript
	 *            migration script to test
	 * @param asserter
	 *            assert implementation
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void customSchemaMigrationScript(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher, String migrationScript,
		DataAsserter asserter) throws InterruptedException, ExecutionException, TimeoutException {
		NodeDao nodeDao = boot().nodeDao();
		PersistingBranchDao branchDao = (PersistingBranchDao) boot().branchDao();
		PersistingSchemaDao schemaDao = (PersistingSchemaDao) boot().schemaDao();

		String fieldName = "migratedField";
		String schemaName = "migratedSchema";

		// create version 1 of the schema
		FieldSchema oldField = creator.create(fieldName);
		HibSchema container = schemaDao.createPersisted(UUIDUtil.randomUUID());
		container.generateBucketId();
		container.setName(UUIDUtil.randomUUID());
		container.setCreated(user());
		schemaDao.mergeIntoPersisted(container);
		HibSchemaVersion versionA = createSchemaVersion(Tx.get(), container, v -> {
			fillSchemaVersion(v, container, schemaName, "1.0", oldField);
			container.setLatestVersion(v);
		});

		// Create version 2 of the schema (with the field renamed)
		FieldSchema newField = creator.create(fieldName);
		HibSchemaVersion versionB = createSchemaVersion(container, schemaName, "2.0", newField);

		// Link the schemas with the changes in between
		HibUpdateFieldChange updateFieldChange = (HibUpdateFieldChange) schemaDao.createPersistedChange(versionA, SchemaChangeOperation.UPDATEFIELD);
		updateFieldChange.setFieldName(fieldName);

		updateFieldChange.setPreviousContainerVersion(versionA);
		updateFieldChange.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a node based on the old schema
		EventQueueBatch batch = createBatch();
		branchDao.assignSchemaVersion(project().getLatestBranch(), user(), versionA, batch);
		HibUser user = user();
		String english = english();
		HibNode parentNode = folder("2015");
		HibNode node = nodeDao.create(parentNode, user, versionA, project());
		HibNodeFieldContainer englishContainer = boot().contentDao().createFieldContainer(node, english, node.getProject().getLatestBranch(),
			user);
		dataProvider.set(englishContainer, fieldName);

		// migrate the node
		branchDao.assignSchemaVersion(project().getLatestBranch(), user(), versionB, batch);
		CommonTx.get().commit();
		NodeMigrationActionContextImpl context = new NodeMigrationActionContextImpl();
		context.setProject(project());
		context.setBranch(project().getLatestBranch());
		context.setFromVersion(versionA);
		context.setToVersion(versionB);
		context.setStatus(DummyMigrationStatus.get());

		nodeMigrationHandler.migrateNodes(context).blockingAwait();

		// assert that migration worked
		assertThat(node).as("Migrated Node").isOf(container).hasTranslation("en");
		assertThat(boot().contentDao().getFieldContainer(node, "en")).as("Migrated field container").isOf(versionB);
		asserter.assertThat(boot().contentDao().getFieldContainer(node, "en"), fieldName);
	}

	/**
	 * Generic test for migrating an existing field with a custom migration script
	 * 
	 * @param creator
	 *            creator implementation
	 * @param dataProvider
	 *            data provider implementation
	 * @param fetcher
	 *            fetcher implementation
	 * @param migrationScript
	 *            migration script to test
	 * @param asserter
	 *            assert implementation
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void customMicroschemaMigrationScript(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher, String migrationScript,
		DataAsserter asserter) throws InterruptedException, ExecutionException, TimeoutException {
		PersistingMicroschemaDao microschemaDao = (PersistingMicroschemaDao) boot().microschemaDao();
		PersistingBranchDao branchDao = (PersistingBranchDao) boot().branchDao();
		NodeDao nodeDao = boot().nodeDao();

		String fieldName = "migratedField";
		String microschemaName = UUIDUtil.randomUUID();
		String micronodeFieldName = "micronodefield";

		// create version 1 of the microschema
		FieldSchema oldField = creator.create(fieldName);
		HibMicroschema container = microschemaDao.createPersisted(null);
		container.generateBucketId();
		container.setName(microschemaName);
		container.setCreated(user());
		HibMicroschemaVersion versionA = createMicroschemaVersion(container, microschemaName, "1.0", oldField);

		// create version 2 of the schema (with the field renamed)
		FieldSchema newField = creator.create(fieldName);
		HibMicroschemaVersion versionB = createMicroschemaVersion(container, microschemaName, "2.0", newField);

		// link the schemas with the changes in between
		HibUpdateFieldChange updateFieldChange = (HibUpdateFieldChange) microschemaDao.createPersistedChange(versionA, SchemaChangeOperation.UPDATEFIELD);
		updateFieldChange.setFieldName(fieldName);

		updateFieldChange.setPreviousContainerVersion(versionA);
		updateFieldChange.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a micronode based on the old schema
		microschemaDao.assign(container, project(), user(), createBatch());
		HibNode node = nodeDao.create(folder("2015"), user(), schemaContainer("content").getLatestVersion(), project());
		HibMicronodeField micronodeField = createMicronodefield(node, micronodeFieldName, versionA, dataProvider, fieldName);
		HibNodeFieldContainer oldContainer = boot().contentDao().getFieldContainer(node, "en");
		VersionNumber oldVersion = oldContainer.getVersion();

		// migrate the micronode
		branchDao.assignMicroschemaVersion(project().getLatestBranch(), user(), versionB, createBatch());
		CommonTx.get().commit();
		MicronodeMigrationContextImpl context = new MicronodeMigrationContextImpl();
		context.setBranch(project().getLatestBranch());
		context.setFromVersion(versionA);
		context.setToVersion(versionB);
		context.setStatus(DummyMigrationStatus.get());
		micronodeMigrationHandler.migrateMicronodes(context).blockingAwait(10,
			TimeUnit.SECONDS);

		// old container must be unchanged
		assertThat(oldContainer).as("Old container").hasVersion(oldVersion.toString());
		assertThat(micronodeField.getMicronode()).as("Old Micronode").isOf(versionA);

		// assert that migration worked
		HibNodeFieldContainer newContainer = boot().contentDao().getFieldContainer(node, "en");
		assertThat(newContainer).as("New container").hasVersion(oldVersion.nextDraft().toString());
		HibMicronodeField newMicronodeField = newContainer.getMicronode(micronodeFieldName);
		asserter.assertThat(newMicronodeField.getMicronode(), fieldName);
	}

	/**
	 * Generic method to test migration failure when using an invalid migration script
	 *
	 * @param creator
	 *            creator implementation
	 * @param dataProvider
	 *            data provider implementation
	 * @param script
	 *            migration script
	 * @throws Throwable
	 */
	protected void invalidMigrationScript(FieldSchemaCreator creator, DataProvider dataProvider, String script) throws Throwable {
		try (Tx tx = tx()) {
			try {
				if (getClass().isAnnotationPresent(MicroschemaTest.class)) {
					invalidMicroschemaMigrationScript(creator, dataProvider, script);
				} else {
					invalidSchemaMigrationScript(creator, dataProvider, script);
				}
			} catch (CompositeException e) {
				Throwable firstError = e.getExceptions().get(0);
				if (firstError instanceof javax.script.ScriptException) {
					throw firstError;
				} else {
					Throwable nestedError = firstError.getCause();
					nestedError.printStackTrace();
					throw nestedError;
				}
			}
		}
	}

	/**
	 * Generic method to test migration failure when using an invalid migration script
	 *
	 * @param creator
	 *            creator implementation
	 * @param dataProvider
	 *            data provider implementation
	 * @param script
	 *            migration script
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void invalidSchemaMigrationScript(FieldSchemaCreator creator, DataProvider dataProvider, String script) throws InterruptedException,
		ExecutionException, TimeoutException {
		NodeDao nodeDao = boot().nodeDao();
		PersistingBranchDao branchDao = (PersistingBranchDao) boot().branchDao();
		PersistingSchemaDao schemaDao = (PersistingSchemaDao) boot().schemaDao();

		String fieldName = "migratedField";
		String schemaName = "migratedSchema";

		// create version 1 of the schema
		FieldSchema oldField = creator.create(fieldName);
		HibSchema container = schemaDao.createPersisted(null);
		container.setName(UUIDUtil.randomUUID());
		container.setCreated(user());

		schemaDao.mergeIntoPersisted(container);
		HibSchemaVersion versionA = createSchemaVersion(Tx.get(), container, v -> {
			fillSchemaVersion(v, container, schemaName, "1.0", oldField);
			container.setLatestVersion(v);
		});

		// create version 2 of the schema (with the field renamed)
		FieldSchema newField = creator.create(fieldName);
		HibSchemaVersion versionB = createSchemaVersion(container, schemaName, "2.0", newField);

		// link the schemas with the changes in between
		HibUpdateFieldChange updateFieldChange = (HibUpdateFieldChange) schemaDao.createPersistedChange(versionA, SchemaChangeOperation.UPDATEFIELD);
		updateFieldChange.setFieldName(fieldName);

		updateFieldChange.setPreviousContainerVersion(versionA);
		updateFieldChange.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a node based on the old schema
		HibUser user = user();
		EventQueueBatch batch = createBatch();
		branchDao.assignSchemaVersion(project().getLatestBranch(), user, versionA, batch);
		String english = english();
		HibNode parentNode = folder("2015");
		HibNode node = nodeDao.create(parentNode, user, versionA, project());
		HibNodeFieldContainer englishContainer = boot().contentDao().createFieldContainer(node, english, node.getProject().getLatestBranch(),
			user);
		dataProvider.set(englishContainer, fieldName);

		// migrate the node
		branchDao.assignSchemaVersion(project().getLatestBranch(), user, versionB, batch);
		CommonTx.get().commit();
		NodeMigrationActionContextImpl context = new NodeMigrationActionContextImpl();
		context.setProject(project());
		context.setBranch(project().getLatestBranch());
		context.setFromVersion(versionA);
		context.setToVersion(versionB);
		context.setStatus(DummyMigrationStatus.get());
		nodeMigrationHandler.migrateNodes(context).blockingAwait();
	}

	/**
	 * Generic method to test migration failure when using an invalid migration script
	 *
	 * @param creator
	 *            creator implementation
	 * @param dataProvider
	 *            data provider implementation
	 * @param script
	 *            script
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void invalidMicroschemaMigrationScript(FieldSchemaCreator creator, DataProvider dataProvider, String script) throws InterruptedException,
		ExecutionException, TimeoutException {
		String fieldName = "migratedField";
		String microschemaName = UUIDUtil.randomUUID();
		String micronodeFieldName = "micronodefield";
		PersistingBranchDao branchDao = (PersistingBranchDao) boot().branchDao();
		PersistingMicroschemaDao microschemaDao = (PersistingMicroschemaDao) boot().microschemaDao();

		// create version 1 of the microschema
		FieldSchema oldField = creator.create(fieldName);
		HibMicroschema container = microschemaDao.createPersisted(null);
		container.setName(microschemaName);
		container.setCreated(user());
		HibMicroschemaVersion versionA = createMicroschemaVersion(container, microschemaName, "1.0", oldField);

		// create version 2 of the microschema
		FieldSchema newField = creator.create(fieldName);
		HibMicroschemaVersion versionB = createMicroschemaVersion(container, microschemaName, "2.0", newField);

		// link the schemas with the changes in between
		HibUpdateFieldChange updateFieldChange = (HibUpdateFieldChange) microschemaDao.createPersistedChange(versionA, SchemaChangeOperation.UPDATEFIELD);
		updateFieldChange.setFieldName(fieldName);

		updateFieldChange.setPreviousContainerVersion(versionA);
		updateFieldChange.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a micronode based on the old schema
		createMicronodefield(folder("2015"), micronodeFieldName, versionA, dataProvider, fieldName);

		// migrate the node
		branchDao.assignMicroschemaVersion(project().getLatestBranch(), user(), versionB, createBatch());
		CommonTx.get().commit();
		MicronodeMigrationContextImpl context = new MicronodeMigrationContextImpl();
		context.setBranch(project().getLatestBranch());
		context.setFromVersion(versionA);
		context.setToVersion(versionB);
		context.setStatus(DummyMigrationStatus.get());
		micronodeMigrationHandler.migrateMicronodes(context).blockingAwait(10,
			TimeUnit.SECONDS);
	}

	/**
	 * Create a schema.
	 * 
	 * @param container
	 *            Parent schema container for versions
	 * @param name
	 *            schema name
	 * @param version
	 *            schema version
	 * @param fields
	 *            list of schema fields
	 * @return schema container
	 */
	protected HibSchemaVersion createSchemaVersion(HibSchema container, String name, String version, FieldSchema... fields) {
		return CommonTx.get().schemaDao().createPersistedVersion(container, v -> fillSchemaVersion(v, container, name, version, fields));
	}

	protected HibSchemaVersion fillSchemaVersion(HibSchemaVersion containerVersion, HibSchema container, String name, String versionName, FieldSchema... fields) {
		SchemaVersionModel schema = new SchemaModelImpl();
		schema.setName(name);
		schema.setVersion(versionName);
		for (FieldSchema field : fields) {
			schema.addField(field);
		}
		schema.setContainer(false);
		// schema.setDisplayField("name");
		// schema.setSegmentField("name");
		schema.validate();

		containerVersion.setName(name);
		containerVersion.setSchema(schema);
		containerVersion.setSchemaContainer(container);
		return containerVersion;
	}

	/**
	 * Create a microschema
	 * 
	 * @param name
	 *            name
	 * @param version
	 *            version
	 * @param fields
	 *            list of schema fields
	 * @return microschema container
	 */
	protected HibMicroschemaVersion createMicroschemaVersion(HibMicroschema container, String name, String version,
		FieldSchema... fields) {
		MicroschemaVersionModel schema = new MicroschemaModelImpl();
		schema.setName(name);
		schema.setVersion(version);
		for (FieldSchema field : fields) {
			schema.addField(field);
		}
		return CommonTx.get().microschemaDao().createPersistedVersion(container, containerVersion -> {
			containerVersion.setSchema(schema);
			containerVersion.setName(name);
			containerVersion.setSchemaContainer(container);
			container.setLatestVersion(containerVersion);
		});
	}

	/**
	 * Create a micronode field in an existing node
	 * 
	 * @param micronodeFieldName
	 *            name of the micronode field
	 * @param schemaVersion
	 *            Microschema container version
	 * @param dataProvider
	 *            data provider
	 * @param fieldNames
	 *            field names to fill
	 * @return micronode field
	 */
	protected HibMicronodeField createMicronodefield(HibNode node, String micronodeFieldName, HibMicroschemaVersion schemaVersion,
		DataProvider dataProvider, String... fieldNames) {
		String english = english();

		HibSchemaVersion latestVersion = node.getSchemaContainer().getLatestVersion();

		// Add a micronode field to the schema of the node.
		SchemaVersionModel schema = latestVersion.getSchema();
		if (schema.getField(micronodeFieldName) == null) {
			schema.addField(new MicronodeFieldSchemaImpl().setName(micronodeFieldName).setLabel("Micronode Field"));
		}
		schema.getField(micronodeFieldName, MicronodeFieldSchema.class).setAllowedMicroSchemas(schemaVersion.getName());
		latestVersion.setSchema(schema);

		HibNodeFieldContainer englishContainer = boot().contentDao().createFieldContainer(node, english, node.getProject().getLatestBranch(),
			user());
		HibMicronodeField micronodeField = englishContainer.createMicronode(micronodeFieldName, schemaVersion);
		for (String fieldName : fieldNames) {
			dataProvider.set(micronodeField.getMicronode(), fieldName);
		}
		return micronodeField;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE })
	protected @interface MicroschemaTest {
	}
}