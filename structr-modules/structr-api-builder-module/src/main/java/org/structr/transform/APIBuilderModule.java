/**
 * Copyright (C) 2010-2017 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.transform;

import com.google.gson.Gson;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.service.LicenseManager;
import org.structr.common.ResultTransformer;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.module.StructrModule;
import org.structr.module.api.APIBuilder;
import org.structr.schema.action.Actions;

/**
 *
 */
public class APIBuilderModule implements StructrModule, APIBuilder {

	private static final Logger logger = LoggerFactory.getLogger(APIBuilderModule.class.getName());

	@Override
	public void onLoad(final LicenseManager licenseManager) {
		// check and read configuration..
	}

	@Override
	public String getName() {
		return "api-builder";
	}

	@Override
	public Set<String> getDependencies() {
		return null;
	}

	@Override
	public Set<String> getFeatures() {
		return null;
	}

	@Override
	public void insertImportStatements(final AbstractSchemaNode schemaInfo, final StringBuilder buf) {
	}

	@Override
	public void insertSourceCode(final AbstractSchemaNode schemaInfo, final StringBuilder buf) {
	}

	@Override
	public Set<String> getInterfacesForType(final AbstractSchemaNode schemaInfo) {
		return null;
	}

	@Override
	public void insertSaveAction(final AbstractSchemaNode schemaInfo, final StringBuilder buf, final Actions.Type type) {
	}

	@Override
	public boolean hasDeploymentData () {
		return true;
	}

	@Override
	public void exportDeploymentData (final Path target, final Gson gson) throws FrameworkException {

		final App app                                = StructrApp.getInstance();
		final Path virtualTypesFile                  = target.resolve("virtual-types.json");
		final List<Map<String, Object>> virtualTypes = new LinkedList();

		try (final Tx tx = app.tx()) {

			for (final VirtualType virtualType : app.nodeQuery(VirtualType.class).sort(VirtualType.name).getAsList()) {

				final Map<String, Object> entry = new TreeMap<>();
				virtualTypes.add(entry);

				entry.put("name",             virtualType.getProperty(VirtualType.name));
				entry.put("sourceType",       virtualType.getProperty(VirtualType.sourceType));
				entry.put("position",         virtualType.getProperty(VirtualType.position));
				entry.put("filterExpression", virtualType.getProperty(VirtualType.filterExpression));

				final List<Map<String, Object>> properties = new LinkedList();
				entry.put("properties", properties);

				for (final VirtualProperty virtualProperty : virtualType.getProperty(VirtualType.properties)) {

					final Map<String, Object> virtualPropEntry = new TreeMap<>();
					properties.add(virtualPropEntry);

					virtualPropEntry.put("sourceName",     virtualProperty.getProperty(VirtualProperty.sourceName));
					virtualPropEntry.put("targetName",     virtualProperty.getProperty(VirtualProperty.targetName));
					virtualPropEntry.put("inputFunction",  virtualProperty.getProperty(VirtualProperty.inputFunction));
					virtualPropEntry.put("outputFunction", virtualProperty.getProperty(VirtualProperty.outputFunction));
					virtualPropEntry.put("position",       virtualProperty.getProperty(VirtualProperty.position));

				}
			}

			tx.success();
		}

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(virtualTypesFile.toFile()))) {

			gson.toJson(virtualTypes, fos);

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
	}

	@Override
	public void importDeploymentData (final Path source, final Gson gson) throws FrameworkException {

		final Path virtualTypesConf = source.resolve("virtual-types.json");
		if (Files.exists(virtualTypesConf)) {

			logger.info("Reading {}..", virtualTypesConf);

			try (final Reader reader = Files.newBufferedReader(virtualTypesConf, Charset.forName("utf-8"))) {

				final List<Map<String, Object>> virtualTypes = gson.fromJson(reader, List.class);

				final SecurityContext context = SecurityContext.getSuperUserInstance();
				context.setDoTransactionNotifications(false);

				final App app                 = StructrApp.getInstance(context);

				try (final Tx tx = app.tx()) {

					for (final VirtualType toDelete : app.nodeQuery(VirtualType.class).getAsList()) {
						app.delete(toDelete);
					}

					for (final VirtualProperty toDelete : app.nodeQuery(VirtualProperty.class).getAsList()) {
						app.delete(toDelete);
					}

					for (final Map<String, Object> entry : virtualTypes) {

						final PropertyMap map = PropertyMap.inputTypeToJavaType(context, VirtualType.class, entry);

						app.create(VirtualType.class, map);
					}

					tx.success();
				}

			} catch (IOException ioex) {
				logger.warn("", ioex);
			}
		}
	}

	// ----- interface APIBuilder -----
	@Override
	public ResultTransformer createMapping(final App app, final String sourceType, final String targetType, final Map<String, String> propertyMappings, final Map<String, String> transforms) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final VirtualType type = app.create(VirtualType.class,
				new NodeAttribute<>(VirtualType.sourceType, sourceType),
				new NodeAttribute<>(VirtualType.name, targetType)
			);

			int i = 0;

			for (final Entry<String, String> entry : propertyMappings.entrySet()) {

				final String sourceProperty = entry.getKey();
				final String targetProperty = entry.getValue();

				app.create(VirtualProperty.class,
					new NodeAttribute<>(VirtualProperty.virtualType, type),
					new NodeAttribute<>(VirtualProperty.sourceName, sourceProperty),
					new NodeAttribute<>(VirtualProperty.targetName, targetProperty),
					new NodeAttribute<>(VirtualProperty.position, i++),
					new NodeAttribute<>(VirtualProperty.inputFunction, transforms.get(sourceProperty))
				);
			}

			tx.success();

			return type;
		}
	}

	@Override
	public void removeMapping(final App app, final String sourceType, final String targetType) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final VirtualType type = app.nodeQuery(VirtualType.class).andName(targetType).getFirst();
			if (type != null) {

				for (final VirtualProperty property : type.getProperty(VirtualType.properties)) {

					app.delete(property);
				}

				app.delete(type);
			}

			tx.success();
		}
	}
}
