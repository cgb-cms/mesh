package com.gentics.mesh.core.endpoint.project;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;

import com.gentics.mesh.auth.MeshAuthChainImpl;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.router.route.AbstractProjectEndpoint;

import io.vertx.ext.web.Route;

/**
 * Endpoint for /api/v1/languages
 * 
 * NOTE: This endpoint is currently not active
 */
public class LanguageEndpoint extends AbstractProjectEndpoint {

	@Inject
	public LanguageEndpoint(MeshAuthChainImpl chain, BootstrapInitializer boot, LocalConfigApi localConfigApi, Database db, MeshOptions options) {
		super("languages", chain, boot, localConfigApi, db, options);
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which allow the manipulation of languages.";
	}

	@Override
	public void registerEndPoints() {
		secureAll();

		// TODO Add method that allows assigning languages from and to the project
		Route createRoute = route("/:projectUuid/languages").method(POST).produces(APPLICATION_JSON);
		createRoute.handler(rc -> {
			throw new NotImplementedException("not implemented");
		});

		Route deleteRoute = route("/:projectUuid/languages").method(DELETE).produces(APPLICATION_JSON);
		deleteRoute.handler(rc -> {
			// Unassign languages should cause a batch process that removes the FieldContainers for the given language.
			throw new NotImplementedException("not implemented");
		});

		Route getRoute = route("/:projectUuid/languages").method(GET).produces(APPLICATION_JSON);
		getRoute.handler(rc -> {
			throw new NotImplementedException("not implemented");
		});
	}

}
