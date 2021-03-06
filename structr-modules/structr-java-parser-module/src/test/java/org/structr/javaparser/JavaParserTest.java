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
package org.structr.javaparser;

import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.InputStream;
import org.asciidoctor.internal.IOUtils;
import static org.junit.Assert.assertEquals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Test;

/**
 * Tests for Java parser.
 */
public class JavaParserTest extends JavaParserModuleTest {

	private static final Logger logger = LoggerFactory.getLogger(JavaParserTest.class.getName());

	@Test
	public void test01ParseSimpleClassFile() {

		final String targetJson = IOUtils.readFull(JavaParserTest.class.getResourceAsStream("/simple-test-class.json"));
		//System.out.println("JSON representation (target): " + targetJson);
		
		try (final InputStream javaCode = JavaParserTest.class.getResourceAsStream("/SimpleTestClass.java")) {
			
			final String resultJson = new GsonBuilder().setPrettyPrinting().create()
					.toJson(new JavaParserModule().parse(javaCode).get());
			
			System.out.println("Result JSON: " + resultJson);
			
			assertEquals(targetJson, resultJson);
			
		} catch (final IOException ex) {
				logger.debug("Error in Java parser test", ex);
		}
	}

}
