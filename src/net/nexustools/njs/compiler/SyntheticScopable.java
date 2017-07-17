/*
 * Copyright (C) 2017 NexusTools.
 *
 * This library is free software: you can redistribute it and/or modify  
 * it under the terms of the GNU Lesser General Public License as   
 * published by the Free Software Foundation, version 3.0.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.nexustools.njs.compiler;

import net.nexustools.njs.BaseObject;
import net.nexustools.njs.Scopable;

/**
 *
 * @author Katelyn Slater <kate@nexustools.com>
 */
public abstract class SyntheticScopable implements Scopable {

	@Override
	public BaseObject get(String key) {
		return get(key, OR_UNDEFINED);
	}

	@Override
	public void set(String key, BaseObject val) {
		set(key, val, OR_VOID);
	}

	@Override
	public boolean delete(String key) {
		return delete(key, OR_FALSE);
	}
	
}
