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
public interface Compiler {
	public Script eval(String source, boolean inFunction);
	public Script eval(Reader source, boolean inFunction);
}
