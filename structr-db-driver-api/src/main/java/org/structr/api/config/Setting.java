/**
 * Copyright (C) 2010-2017 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.api.config;

import org.structr.api.util.html.Tag;

/**
 * A configuration setting with a key and a type.
 */
public abstract class Setting<T> {

	private String key   = null;
	private T value      = null;

	public abstract void render(final Tag parent);
	public abstract void fromString(final String source);

	public Setting(final SettingsGroup group, final String key, final T value) {

		this.key   = key;
		this.value = value;

		group.registerSetting(this);
		Settings.registerSetting(this);
	}

	public String getKey() {
		return key;
	}

	public T getValue() {
		return value;
	}

	public T getValue(final T defaultValue) {

		if (value == null) {

			return defaultValue;
		}

		return value;
	}

	public void setValue(final T value) {
		this.value = value;
	}
}