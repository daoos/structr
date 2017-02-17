/*
 * Copyright (C) 2010-2016 Structr GmbH
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
var _Widgets = {
	url: 'https://widgets.structr.org/structr/rest/widgets',
	remoteWidgetData: [],
	remoteWidgetFilter: undefined,
	remoteWidgetsEl: undefined,
	localWidgetsEl: undefined,

	reloadWidgets: function() {
		widgetsSlideout.find(':not(.compTab)').remove();
		widgetsSlideout.append(
			'<div class="ver-scrollable"><h2>Local Widgets</h2><button class="add_widgets_icon button"><i title="Add Widget" class="' + _Icons.getFullSpriteClass(_Icons.add_widget_icon) + '" /> Add Widget</button>' +
			'<div id="widgets"></div><h2>Remote Widgets</h2><input placeholder="Filter..." id="remoteWidgetsFilter"><div id="remoteWidgets"></div></div>');
		_Widgets.localWidgetsEl = $('#widgets', widgetsSlideout);

		$('.add_widgets_icon', widgetsSlideout).on('click', function(e) {
			e.stopPropagation();
			Command.create({type: 'Widget'});
		});

		_Widgets.localWidgetsEl.droppable({
			drop: function(e, ui) {
				e.preventDefault();
				e.stopPropagation();
				dropBlocked = true;
				var sourceId = Structr.getId($(ui.draggable));
				var source = StructrModel.obj(sourceId);

				if (source && source.isWidget) {
					if (source.treePath) {
						_Logger.log(_LogType.WIDGETS, 'Copying remote widget', source);

						Command.create({ type: 'Widget', name: source.name + ' (copied)', source: source.source }, function(entity) {
							_Logger.log(_LogType.WIDGETS, 'Copied remote widget successfully', entity);
						});
					}
				} else {
					$.ajax({
						url: viewRootUrl + sourceId + '?edit=1',
						contentType: 'text/html',
						statusCode: {
							200: function(data) {
								Command.createLocalWidget(sourceId, 'New Widget (' + sourceId + ')', data, function(entity) {
									_Logger.log(_LogType.WIDGETS, 'Created widget successfully', entity);
								});
							}
						}
					});
				}
			}
		});

		_Pager.initPager('local-widgets', 'Widget', 1, 25);
		_Pager.addPager('local-widgets', _Widgets.localWidgetsEl, true, 'Widget', 'public', function(entities) {
			entities.forEach(function (entity) {
				StructrModel.create(entity, null, false);
				_Widgets.appendWidgetElement(entity, false, _Widgets.localWidgetsEl);
			});
		});

		_Widgets.remoteWidgetsEl = $('#remoteWidgets', widgetsSlideout);

		$('#remoteWidgetsFilter').keyup(function (e) {
			if (e.keyCode === 27) {
				$(this).val('');
			}

			_Widgets.repaintRemoteWidgets($(this).val());
		});

		_Widgets.refreshRemoteWidgets();

	},
	refreshRemoteWidgets: function() {
		_Widgets.remoteWidgetFilter = undefined;

		if (!_Widgets.url.startsWith(document.location.origin)) {

			_Widgets.getRemoteWidgets(_Widgets.url, function(entity) {
				var obj = StructrModel.create(entity, null, false);
				obj.srcUrl = _Widgets.url + '/' + entity.id;
				_Widgets.remoteWidgetData.push(obj);
			}, function () {
				_Widgets.repaintRemoteWidgets('');
			});

		}
	},
	repaintRemoteWidgets: function (search) {
		if (search !== _Widgets.remoteWidgetFilter) {

			_Widgets.remoteWidgetFilter = search;
			_Widgets.remoteWidgetsEl.empty();

			if (search && search.length > 0) {

				search = search.toLowerCase();

				_Widgets.remoteWidgetData.forEach(function (obj) {
					if (obj.name.toLowerCase().indexOf(search) !== -1) {
						_Widgets.appendWidgetElement(obj, true, _Widgets.remoteWidgetsEl);
					}
				});

			} else {

				_Widgets.remoteWidgetData.forEach(function (obj) {
					_Widgets.appendWidgetElement(obj, true, _Widgets.remoteWidgetsEl);
				});

			}

		}
	},
	getRemoteWidgets: function(baseUrl, callback, finishCallback) {
		$.ajax({
			url: baseUrl + '?sort=treePath',
			type: 'GET',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			statusCode: {
				200: function(data) {
					if (callback) {
						$.each(data.result, function(i, entity) {
							callback(entity);
						});
						if (finishCallback) {
							finishCallback();
						}
					}
				},
				400: function(data, status, xhr) {
					console.log(data, status, xhr);
				},
				401: function(data, status, xhr) {
					console.log(data, status, xhr);
				},
				403: function(data, status, xhr) {
					console.log(data, status, xhr);
				},
				404: function(data, status, xhr) {
					console.log(data, status, xhr);
				},
				422: function(data, status, xhr) {
					console.log(data, status, xhr);
				},
				500: function(data, status, xhr) {
					console.log(data, status, xhr);
				}
			}
		});
	},
	getTreeParent: function(element, treePath, suffix) {

		var parent = element;

		if (treePath) {

			var parts = treePath.split('/');
			var num = parts.length;
			var i = 0;

			for (i = 0; i < num; i++) {

				var part = parts[i];
				if (part) {

					var lowerPart = part.toLowerCase().replace(/ /g, '');
					var idString = lowerPart + suffix;
					var newParent = $('#' + idString);

					if (newParent.size() === 0) {
						_Widgets.appendFolderElement(parent, idString, _Icons.folder_icon, part);
						newParent = $('#' + idString);
					}

					parent = newParent;
				}
			}

		} else {

			var idString = 'other' + suffix;
			var newParent = $('#' + idString);

			if (newParent.size() === 0) {
				_Widgets.appendFolderElement(parent, idString, _Icons.folder_icon, 'Uncategorized');
				newParent = $('#' + idString);
			}

			parent = newParent;
		}

		return parent;
	},
	appendFolderElement: function(parent, id, icon, name) {

		var expanded = Structr.isExpanded(id);

		parent.append('<div id="' + id + '_folder" class="widget node">'
			+ '<i class="typeIcon ' + _Icons.getFullSpriteClass(icon) + '" />'
			+ '<b title="' + name + '" class="name">' + fitStringToWidth(name, 200) + '</b>'
			+ '<div id="' + id + '" class="node' + (expanded ? ' hidden' : '') + '"></div>'
			+ '</div>');

		var div = $('#' + id + '_folder');

		_Widgets.appendVisualExpandIcon(div, id, name, true, false);
	},
	appendWidgetElement: function(widget, remote, el) {

		_Logger.log(_LogType.WIDGETS, 'Widgets.appendWidgetElement', widget, remote);

		var icon = _Icons.widget_icon;
		var parent = _Widgets.getTreeParent(el ? el : (remote ? _Widgets.remoteWidgetsEl : _Widgets.localWidgetsEl), widget.treePath, remote ? '_remote' : '_local');
		var delIcon, newDelIcon;
		var div = Structr.node(widget.id);
		if (div && div.length) {

			var formerParent = div.parent();

			if (!Structr.containsNodes(formerParent)) {
				_Entities.removeExpandIcon(formerParent);
				Structr.enableButton($('.delete_icon', formerParent)[0]);
			}

		} else {

			parent.append('<div id="id_' + widget.id + '" class="node widget">'
				+ '<i class="typeIcon ' + _Icons.getFullSpriteClass(icon) + '" />'
				+ '<b title="' + widget.name + '" class="name_">' + fitStringToWidth(widget.name, 200) + '</b> <span class="id">' + widget.id + '</span>'
				+ '</div>');
			div = Structr.node(widget.id);

			var typeIcon = $('.typeIcon', div);
			typeIcon.on('click', function() {
				Structr.dialog('Source code of ' + widget.name, function() {}, function() {});
				var text = widget.source || '';
				var div = dialogText.append('<div class="editor"></div>');
				_Logger.log(_LogType.WIDGETS, div);
				var contentBox = $('.editor', dialogText);
				editor = CodeMirror(contentBox.get(0), {
					value: unescapeTags(text),
					mode: 'text/html',
					lineNumbers: true,
					indentUnit: 4,
					tabSize:4,
					indentWithTabs: true
				});
				editor.focus();
				Structr.resize();
			});

		}

		if (!div)
			return;

		if (!remote) {
			_Entities.appendAccessControlIcon(div, widget);

			delIcon = div.children('.delete_icon');

			newDelIcon = '<i title="Delete widget ' + widget.name + '\'" class="delete_icon button ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" />';
			div.append(newDelIcon);
			delIcon = div.children('.delete_icon');
			div.children('.delete_icon').on('click', function(e) {
				e.stopPropagation();
				_Entities.deleteNode(this, widget);
			});

		}

		div.draggable({
			iframeFix: true,
			revert: 'invalid',
			containment: 'body',
			helper: 'clone',
			appendTo: '#main',
			stack: '.node',
			zIndex: 99
		});

		if (!remote) {
			div.append('<i title="Edit widget ' + widget.name + '" class="edit_icon button ' + _Icons.getFullSpriteClass(_Icons.edit_icon) + '" />');
			$('.edit_icon', div).on('click', function(e) {
				e.stopPropagation();
				Structr.dialog('Edit widget "' + widget.name + '"', function() {
					_Logger.log(_LogType.WIDGETS, 'Widget source saved');
				}, function() {
					_Logger.log(_LogType.WIDGETS, 'cancelled');
				});
				if (!widget.id) {
					return false;
				}
				Command.get(widget.id, function(entity) {
					_Widgets.editWidget(this, entity, dialogText);
				});

			});
			_Entities.appendEditPropertiesIcon(div, widget);
		}

		_Entities.setMouseOver(div, false);
		if (remote) {
			div.children('b.name_').off('click').css({cursor: 'move'});
		}

		return div;
	},
	editWidget: function(button, entity, element) {
		if (Structr.isButtonDisabled(button)) {
			return;
		}
		var text = entity.source || '';
		var div = element.append('<div class="editor"></div>');
		_Logger.log(_LogType.WIDGETS, div);
		var contentBox = $('.editor', element);
		editor = CodeMirror(contentBox.get(0), {
			value: unescapeTags(text),
			mode: 'text/html',
			lineNumbers: true,
			indentUnit: 4,
			tabSize:4,
			indentWithTabs: true
		});
		editor.focus();
		Structr.resize();

		dialogBtn.append('<button id="editorSave" disabled="disabled" class="disabled">Save Widget</button>');
		dialogBtn.append('<button id="saveAndClose" disabled="disabled" class="disabled"> Save and close</button>');

		dialogSaveButton = $('#editorSave', dialogBtn);
		saveAndClose = $('#saveAndClose', dialogBtn);

		text1 = text;

		editor.on('change', function(cm, change) {

			text2 = editor.getValue();

			if (text1 === text2) {
				dialogSaveButton.prop("disabled", true).addClass('disabled');
				saveAndClose.prop("disabled", true).addClass('disabled');
			} else {
				dialogSaveButton.prop("disabled", false).removeClass('disabled');
				saveAndClose.prop("disabled", false).removeClass('disabled');
			}
		});

		saveAndClose.on('click', function(e) {
			e.stopPropagation();
			dialogSaveButton.click();
			setTimeout(function() {
				dialogSaveButton.remove();
				saveAndClose.remove();
				dialogCancelButton.click();
			}, 500);
		});

		dialogSaveButton.on('click', function() {

			var newText = editor.getValue();

			if (text1 === newText) {
				return;
			}

			var successCallback = function () {
				Structr.showAndHideInfoBoxMessage('Widget source saved.', 'success', 2000, 200);
				text1 = newText;
				dialogSaveButton.prop("disabled", true).addClass('disabled');
				saveAndClose.prop("disabled", true).addClass('disabled');
			};

			if (entity.srcUrl) {
				var data = JSON.stringify({source: newText});
				_Logger.log(_LogType.WIDGETS, 'update remote widget', entity.srcUrl, data);
				$.ajax({
					url: entity.srcUrl,
					type: 'PUT',
					dataType: 'json',
					data: data,
					contentType: 'application/json; charset=utf-8',
					statusCode: {
						200: function(data) {
							successCallback();
						},
						400: function(data, status, xhr) {
							console.log(data, status, xhr);
						},
						401: function(data, status, xhr) {
							console.log(data, status, xhr);
						},
						403: function(data, status, xhr) {
							console.log(data, status, xhr);
						},
						404: function(data, status, xhr) {
							console.log(data, status, xhr);
						},
						422: function(data, status, xhr) {
							console.log(data, status, xhr);
						},
						500: function(data, status, xhr) {
							console.log(data, status, xhr);
						}
					}

				});

			} else {

				Command.setProperty(entity.id, 'source', newText, false, function() {
					successCallback();
				});

			}

		});

		editor.id = entity.id;
	},
	appendVisualExpandIcon: function(el, id, name, hasChildren, expand) {

		if (hasChildren) {

			_Logger.log(_LogType.WIDGETS, 'appendExpandIcon hasChildren?', hasChildren, 'expand?', expand);

			var typeIcon = $(el.children('.typeIcon').first());
			var icon = $(el).children('.node').hasClass('hidden') ? _Icons.collapsed_icon : _Icons.expanded_icon;

			typeIcon.css({
				paddingRight: 0 + 'px'
			}).after('<i title="Expand \'' + name + '\'" class="expand_icon ' + _Icons.getFullSpriteClass(icon) + '" />');

			var expandIcon = el.children('.expand_icon').first();

			var expandClickHandler = function (e) {
				e.stopPropagation();
				var body = $('#' + id);
				body.toggleClass('hidden');
				var collapsed = body.hasClass('hidden');
				if (collapsed) {
					Structr.addExpandedNode(id);
					expandIcon.removeClass(_Icons.getSpriteClassOnly(_Icons.expanded_icon)).addClass(_Icons.getSpriteClassOnly(_Icons.collapsed_icon));

				} else {
					Structr.removeExpandedNode(id);
					expandIcon.removeClass(_Icons.getSpriteClassOnly(_Icons.collapsed_icon)).addClass(_Icons.getSpriteClassOnly(_Icons.expanded_icon));
				}
			};

			$(el).on('click', expandClickHandler);

			button = $(el.children('.expand_icon').first());

			if (button) {

				button.on('click', expandClickHandler);

				// Prevent expand icon from being draggable
				button.on('mousedown', function(e) {
					e.stopPropagation();
				});

			}

		} else {
			el.children('.typeIcon').css({
				paddingRight: '11px'
			});
		}

	},
	insertWidgetIntoPageOLD: function(source, target, pageId) {

		var pattern = /\[[a-zA-Z:,]+\]/g;
		var widgetSource = source.source;
		if (widgetSource) {

			var rawMatches = widgetSource.match(pattern);

			if (rawMatches) {

				var matches = $.unique(rawMatches);

				if (matches && matches.length) {

					Structr.dialog('Configure Widget', function() {}, function() {});

					dialogText.append('<p>Fill out the following parameters to correctly configure the widget.</p><table class="props"></table>');
					var table = $('table', dialogText);

					$.each(matches, function(i, match) {

						var propertyKey = match.replace(/\[/, '').replace(/\]/, '');
						var options = '';
						var hasOptions = false;

						if (propertyKey.contains(":")) {

							var parts = propertyKey.split(":");

							if (parts.length === 2) {
								propertyKey = parts[0];
								options = parts[1];
								hasOptions = true;
							}
						}

						var label = propertyKey;

						if (hasOptions) {

							var buffer = '';

							buffer += '<tr><td><label>' + label + '</label></td><td><select id="' + propertyKey + '" class="input-field" >';

							$.each(options.split(","), function(i, option) {
								buffer += '<option>' + option + '</option>';
							});

							buffer += '</select>';
							buffer += '</td></tr>';

							table.append(buffer);

						} else {

							table.append('<tr><td><label for="' + label + '">' + label + '</label></td><td><input class="input-field" type="text" id="' + propertyKey + '" placeholder="' + label + '"></td></tr>');
						}

					});

					dialog.append('<button id="appendWidget">Append Widget</button>');
					var attrs = {};
					$('#appendWidget').on('click', function(e) {

						$.each(matches, function(i, match) {

							$.each($('.input-field', table), function(i, m) {
								var key = $(m).prop('id').replace(/\[/, '').replace(/\]/, '');
								attrs[key] = $(this).val();
							});

						});

						e.stopPropagation();
						Command.appendWidget(widgetSource, target.id, pageId, _Widgets.url, attrs);

						dialogCancelButton.click();
						return false;
					});

				}

			} else {

				// If no matches, directly append widget
				Command.appendWidget(widgetSource, target.id, pageId, _Widgets.url);

			}

		}

	},
	insertWidgetIntoPage: function(widget, target, pageId) {

		var pattern = /\[[^\]]+\]/g;
		var widgetSource = widget.source;
		var widgetDescription = widget.description;
		var widgetConfig = widget.configuration;

		if (widgetConfig) {
			try {
				widgetConfig = JSON.parse(widgetConfig);
			} catch (e) {
				new MessageBuilder().error("Cannot parse Widget configuration").show();
				return;
			}
		}

		if (widgetSource) {

			var matches = [];
			var rawMatches = widgetSource.match(pattern);
			if (rawMatches) {
				matches = $.unique(rawMatches);
			}

			if (widgetDescription !== null || (matches.length > 0 && widgetConfig) ) {

				Structr.dialog('Configure Widget', function() {}, function() {});

				dialogText.append('<p>Fill out the following parameters to correctly configure the widget.</p>');

				if (widgetDescription !== null) {
					dialogText.append(widgetDescription);
				}

				dialogText.append('<table class="props"></table>');

				var table = $('table', dialogText);

				matches.forEach(function(m) {

					var label = m.replace(/^\[/, '').replace(/\]$/, '');

					if (widgetConfig[label]) {

						var fieldConfig = widgetConfig[label];
						var fieldType = fieldConfig.type;
						var defaultValue = fieldConfig.default || '';

						switch (fieldType) {
							case "select":
								var options = fieldConfig.options || ["-"];

								var buffer = '<tr><td><label>' + label + '</label></td><td><select id="' + label + '" class="form-field">';

								if (Object.prototype.toString.call(options) === '[object Array]') {
									options.forEach(function (option) {
										buffer += '<option' + ((option === defaultValue) ? ' selected' : '') + '>' + option + '</option>';
									});

								} else if (Object.prototype.toString.call(options) === '[object Object]') {

									Object.keys(options).forEach(function (option) {
										buffer += '<option' + ((option === defaultValue) ? ' selected' : '') + ' value="' + option + '">' + options[option] + '</option>';
									});

								}

								buffer += '</select></td></tr>';

								table.append(buffer);
								break;

							case "textarea":
								table.append('<tr><td><label>' + label + '</label></td><td><textarea rows=5 class="form-field" id="' + label + '" placeholder="' + label + '">' + defaultValue + '</textarea></td></tr>');
								break;

							case "input":
							default:
								table.append('<tr><td><label>' + label + '</label></td><td><input class="form-field" type="text" id="' + label + '" placeholder="' + label + '" value="' + defaultValue + '"></td></tr>');

						}
					}

				});

				dialog.append('<button id="appendWidget">Append Widget</button>');
				var attrs = {};
				$('#appendWidget').on('click', function(e) {

					$('.form-field', table).each(function(i, m) {
						var key = $(m).prop('id');
						if (widgetConfig[key]) {
							attrs[key] = $(this).val();
						}
					});

					e.stopPropagation();
					Command.appendWidget(widgetSource, target.id, pageId, _Widgets.url, attrs);

					dialogCancelButton.click();
					return false;
				});

			} else {

				// If no matches, directly append widget
				Command.appendWidget(widgetSource, target.id, pageId, _Widgets.url);

			}

		} else {
			new MessageBuilder().warning("Ignoring empty Widget").show();
		}

	}
};
