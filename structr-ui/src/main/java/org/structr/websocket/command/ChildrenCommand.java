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
package org.structr.websocket.command;

import org.structr.common.PropertyView;
import org.structr.web.common.RelType;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;
import org.neo4j.graphdb.Direction;
import org.structr.core.IterableAdapter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipFactory;
import org.structr.core.graph.RelationshipInterface;
import org.structr.websocket.StructrWebSocket;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to return the children of the given node
 *
 * @author Axel Morgner
 */
public class ChildrenCommand extends AbstractCommand {

	static {

		StructrWebSocket.addCommand(ChildrenCommand.class);

	}

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final RelationshipFactory factory = new RelationshipFactory(this.getWebSocket().getSecurityContext());
		final AbstractNode node           = getNode(webSocketData.getId());

		if (node == null) {

			return;
		}

		final Iterable<RelationshipInterface> rels = new IterableAdapter<>(node.getNode().getRelationships(RelType.CONTAINS, Direction.OUTGOING), factory);
		final List<GraphObject> result             = new LinkedList();
		int count                                  = 0;

		for (RelationshipInterface rel : rels) {

			NodeInterface endNode = rel.getTargetNode();
			if (endNode == null) {

				continue;
			}

			if (count++ == 100) {
				break;
			}

			result.add(endNode);
		}

		webSocketData.setView(PropertyView.Ui);
		webSocketData.setResult(result);

		// send only over local connection
		getWebSocket().send(webSocketData, true);

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {

		return "CHILDREN";

	}

}
