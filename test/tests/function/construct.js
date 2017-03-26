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

var Assert = importClass("org.junit.Assert");
var System = importClass("java.lang.System");

var test = new Function("return \"Toasted Wheat\"");
Assert.assertTrue(test() === "Toasted Wheat");
test = new Function("a, b, c", "return a * b() / c.value");
Assert.assertTrue(test(23, function() {
	return 10;
}, {
	value: 55
}) === 4.181818181818182);
test = new Function("a", "b", "c", "return a * b() / c.value");
Assert.assertTrue(test(55, function() {
	return 12;
}, {
	value: 4.5
}) === 146.66666666666666);
test = new Function("return eval(\"new Error()\").stack");
System.out.println(test());