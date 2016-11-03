/* 
 * Copyright (C) 2016 NexusTools.
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
 *
 * @author Katelyn Slater <ktaeyln@gmail.com>
 */
package tests;

import net.nexustools.njs.JSHelper;
import net.nexustools.njs.compiler.Compiler;
import net.nexustools.njs.compiler.JavaTranspiler;
import net.nexustools.njs.compiler.RuntimeCompiler;

import java.io.InputStreamReader;

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
			new JavaTranspiler(),
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

	@Test
	public void indexes() {
		test("indexes");
	}

	@Test
	public void valueOf() {
		test("valueOf");
	}

	public void test(java.lang.String name) {
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
