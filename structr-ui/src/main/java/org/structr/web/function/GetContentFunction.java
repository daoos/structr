/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.web.function;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.web.entity.File;

public class GetContentFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_GET_CONTENT    = "Usage: ${get_content(file[, encoding = \"UTF-8\"])}. Example: ${get_content(first(find('File', 'name', 'test.txt')))}";
	public static final String ERROR_MESSAGE_GET_CONTENT_JS = "Usage: ${{Structr.getContent(file[, encoding = \"UTF-8\"])}}. Example: ${{Structr.getContent(fileNode)}}";

	@Override
	public String getName() {
		return "get_content()";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (!(arrayHasMinLengthAndAllElementsNotNull(sources, 1) && sources[0] instanceof File)) {
			return null;
		}

		final File file = (File)sources[0];
		final String encoding = (sources.length == 2 && sources[1] != null) ? sources[1].toString() : "UTF-8";

		try (final InputStream is = file.getInputStream()) {

			return new Scanner(is, encoding).useDelimiter("\\A").next();

		} catch (final IOException | IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_GET_CONTENT_JS : ERROR_MESSAGE_GET_CONTENT);
	}

	@Override
	public String shortDescription() {
		return "Returns the content of the given file";
	}
}
