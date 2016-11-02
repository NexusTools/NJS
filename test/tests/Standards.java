/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests;

import java.io.InputStreamReader;
import net.nexustools.njs.JSHelper;
import net.nexustools.njs.compiler.Compiler;
import net.nexustools.njs.compiler.JavaTranspiler;
import net.nexustools.njs.compiler.RuntimeCompiler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author kate
 */
public class Standards {
	
	Compiler[] compilers;
	
	@Before
	public void setUp() {
		compilers = new Compiler[]{
			new JavaTranspiler(),
			new RuntimeCompiler()
		};
	}
	
	@After
	public void tearDown() {
		compilers = null;
	}
	
	@Test
	public void inheritance() {
		test("inheritance");
	}
	
	@Test
	public void __proto__() {
		test("__proto__");
	}
	
	@Test
	public void valueOf() {
		test("valueOf");
	}

	public void test(java.lang.String name) {
		for(Compiler compiler : compilers) {
			try {
				compiler.compile(new InputStreamReader(Standards.class.getResourceAsStream("/tests/standards/" + name + ".js")), name + ".js", false).exec(JSHelper.createExtendedGlobal(), null);
			} catch(net.nexustools.njs.Error.ConvertedException re) {
				re.printStackTrace();
				throw re;
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
