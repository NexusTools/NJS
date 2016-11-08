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
 */
package tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 *
 * @author Katelyn Slater <kate@nexustools.com>
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	Blocks.class,
	Comparisons.class,
	MathOperations.class,
	Standards.class,
	
	Object.class,
	Function.class,
	String.class,
	Array.class,
	JSON.class,
	
	Extended.class,
	Eval.class})
public class Everything {
}
