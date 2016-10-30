/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests;

import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
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
public class Array {
	
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
	public void construct() {
		test("construct");
	}
	
	@Test
	public void from() {
		test("from");
	}
	
	@Test
	public void of() {
		test("of");
	}
	
	@Test
	public void fill() {
		test("fill");
	}
	
	@Test
	public void reverse() {
		test("reverse");
	}
	
	@Test
	public void sort() {
		test("sort");
	}
	
	@Test
	public void push() {
		test("push");
	}
	
	@Test
	public void pop() {
		test("pop");
	}
	
	@Test
	public void shift() {
		test("shift");
	}

	public void test(String name) {
		for(Compiler compiler : compilers) {
			try {
				compiler.compile(new InputStreamReader(Array.class.getResourceAsStream("/tests/array/" + name + ".js")), name + ".js", false).exec(JSHelper.createExtendedGlobal(), null);
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
