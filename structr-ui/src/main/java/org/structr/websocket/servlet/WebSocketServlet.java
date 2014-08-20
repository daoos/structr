/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.servlet;

import com.google.gson.GsonBuilder;
import java.util.logging.Logger;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.TransactionCommand;
import org.structr.rest.service.HttpServiceServlet;
import org.structr.rest.service.StructrHttpServiceConfig;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.StructrWebSocketCreator;
import org.structr.websocket.SynchronizationController;
import org.structr.websocket.WebSocketDataGSONAdapter;
import org.structr.websocket.message.WebSocketMessage;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 * @author Axel Morgner
 */
public class WebSocketServlet extends org.eclipse.jetty.websocket.servlet.WebSocketServlet implements HttpServiceServlet {

	private static final Logger logger           = Logger.getLogger(WebSocketServlet.class.getName());

	private static final int MAX_TEXT_MESSAGE_SIZE = 1024 * 1024;

	private final StructrHttpServiceConfig config = new StructrHttpServiceConfig();

	@Override
	public StructrHttpServiceConfig getConfig() {
		return config;
	}

	@Override
	public void configure(final WebSocketServletFactory factory) {

		// create GSON serializer
		final GsonBuilder gsonBuilderWithDepth1 = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(WebSocketMessage.class, new WebSocketDataGSONAdapter(config.getDefaultIdProperty(), 1));
		final GsonBuilder gsonBuilderWithDepth2 = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(WebSocketMessage.class, new WebSocketDataGSONAdapter(config.getDefaultIdProperty(), 2));

		final boolean lenient = Boolean.parseBoolean(StructrApp.getConfigurationValue("json.lenient", "false"));
		if (lenient) {
			// Serializes NaN, -Infinity, Infinity, see http://code.google.com/p/google-gson/issues/detail?id=378
			gsonBuilderWithDepth2.serializeSpecialFloatingPointValues();

		}

		final SynchronizationController syncController = new SynchronizationController(gsonBuilderWithDepth1.create());

		// register (Structr) transaction listener
		TransactionCommand.registerTransactionListener(syncController);

		factory.getPolicy().setIdleTimeout(61000);
		factory.setCreator(new StructrWebSocketCreator(syncController, gsonBuilderWithDepth2.create(), config.getDefaultIdProperty(), config.getAuthenticator()));
		factory.register(StructrWebSocket.class);

		// Disable compression (experimental features)
		factory.getExtensionFactory().unregister("x-webkit-deflate-frame");
		factory.getExtensionFactory().unregister("permessage-deflate");

		factory.getPolicy().setMaxTextMessageSize(MAX_TEXT_MESSAGE_SIZE);

	}
}
