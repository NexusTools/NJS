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
package tests;

import java.io.InputStreamReader;
import net.nexustools.njs.Utilities;
import net.nexustools.njs.compiler.Compiler;
import net.nexustools.njs.compiler.JavaTranspiler;
import net.nexustools.njs.compiler.RuntimeCompiler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Katelyn Slater <kate@nexustools.com>
 */
public class MathOperations {

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
    public void plusplus() {
        test("plusplus");
    }

    @Test
    public void parseIntFloat() {
        test("parseIntFloat");
    }

    @Test
    public void percent() {
        test("percent");
    }

    @Test
    public void or() {
        test("or");
    }

    @Test
    public void and() {
        test("and");
    }

    @Test
    public void plus() {
        test("plus");
    }

    @Test
    public void minus() {
        test("minus");
    }

    @Test
    public void multiply() {
        test("multiply");
    }

    @Test
    public void divide() {
        test("divide");
    }

    @Test
    public void primes() {
        test("primes");
    }

    @Test
    public void ordering() {
        test("ordering");
    }

    public void test(java.lang.String name) {
        for (Compiler compiler : compilers) {
            try {
                compiler.compile(new InputStreamReader(MathOperations.class.getResourceAsStream("/tests/math/ops/" + name + ".js")), name + ".js", false).exec(Utilities.createExtendedGlobal(), null);
            } catch (java.lang.RuntimeException re) {
                System.err.println(Utilities.extractStack(re.toString(), re));
                throw re;
            } catch (java.lang.Error e) {
                System.err.println(Utilities.extractStack(e.toString(), e));
                throw e;
            }
        }
    }

}
