/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
