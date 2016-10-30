/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests;

import java.io.InputStreamReader;

import net.nexustools.njs.JSHelper;
import net.nexustools.njs.compiler.Compiler;
import net.nexustools.njs.compiler.JavaCompiler;
import net.nexustools.njs.compiler.RuntimeCompiler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author kate
 */
public class RedefineGlobal {
	
	Compiler[] compilers;
	
	@Before
	public void setUp() {
		compilers = new Compiler[]{
			new JavaCompiler(),
			new RuntimeCompiler()
		};
	}
	
	@After
	public void tearDown() {
		compilers = null;
	}
	
	@Test
	public void test() {
		for(Compiler compiler : compilers) {
			try {
				compiler.compile(new InputStreamReader(RedefineGlobal.class.getResourceAsStream("/tests/evaltests.js")), "evaltests.js", false).exec(JSHelper.createExtendedGlobal(), null);
			} catch(java.lang.RuntimeException re) {
				System.err.println(JSHelper.extractStack(re.toString(), re));
				throw re;
			} catch(java.lang.Error e) {
				System.err.println(JSHelper.extractStack(e.toString(), e));
				throw e;
			}
		}
	}
	
}
