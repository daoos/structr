/*
 *  Copyright (C) 2011 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.websocket.command;

import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Folder;
import org.structr.web.common.RelationshipHelper;
import org.structr.web.entity.Content;
import org.structr.web.entity.Group;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to return the children of the given node
 * in the context of the given resourceId
 *
 * @author Axel Morgner
 */
public class ChildrenCommand extends AbstractCommand {

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		String resourceId               = (String) webSocketData.getNodeData().get("resourceId");
		String componentId              = (String) webSocketData.getNodeData().get("componentId");
		AbstractNode node               = getNode(webSocketData.getId());
		List<AbstractRelationship> rels = node.getOutgoingRelationships(RelType.CONTAINS);
		Map<Long, GraphObject> sortMap  = new TreeMap<Long, GraphObject>();
		Set<String> nodesWithChildren   = new HashSet<String>();
		List<GraphObject> result        = new LinkedList<GraphObject>();

		for (AbstractRelationship rel : rels) {

			AbstractNode endNode = rel.getEndNode();

			if ((node instanceof Group) || (node instanceof Folder)) {

				result.add(endNode);
				nodesWithChildren.addAll(RelationshipHelper.getChildrenInResource(endNode, null));

				continue;

			}

			Long pos = null;

			if (rel.getLongProperty(resourceId) != null) {

				pos = rel.getLongProperty(resourceId);

			} else {

				// Try "*"
				pos = rel.getLongProperty("*");
			}

			String relCompId = rel.getStringProperty("componentId");

			if (pos != null) {

				if ((componentId == null) || (!endNode.getType().equals(Content.class.getSimpleName()) || ((relCompId != null) && relCompId.equals(componentId)))) {

					nodesWithChildren.addAll(RelationshipHelper.getChildrenInResource(endNode, resourceId));
					sortMap.put(pos, endNode);

				}

			}

		}

		if (!sortMap.isEmpty()) {

			result = new ArrayList<GraphObject>(sortMap.values());

		}

		webSocketData.setView(PropertyView.Ui);
		webSocketData.setResult(result);
		webSocketData.setNodesWithChildren(nodesWithChildren);

		// send only over local connection
		getWebSocket().send(webSocketData, true);
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {
		return "CHILDREN";
	}
}
