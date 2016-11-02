/* 
 * Copyright (c) 2016 NexusTools.
 * 
 * This program is free software: you can redistribute it and/or modify  
 * it under the terms of the GNU Lesser General Public License as   
 * published by the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * Lesser General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.nexustools.njs.compiler;

import java.io.Reader;

/**
 *
 * @author kate
 */
public class NullCompiler implements Compiler {

	@Override
	public Script compile(String source, String fileName, boolean inFunction) {
		throw new net.nexustools.njs.Error.JavaException("EvalError", "Compilation at runtime is not supported");
	}

	@Override
	public Script compile(Reader source, String fileName, boolean inFunction) {
		throw new net.nexustools.njs.Error.JavaException("EvalError", "Compilation at runtime is not supported");
	}
	
}
