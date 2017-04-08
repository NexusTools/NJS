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

var result = 2+2;
Assert.assertTrue("result === \"4\": " + result, result == "4");
var test = 5, result = test += 5;
Assert.assertTrue("test += 5: " + test, result === 10);
Assert.assertTrue("test === 10: " + test, test === 10);

test = "5";
test += "5";
Assert.assertTrue(test, test === "55");
result = test + "5";
Assert.assertTrue(result, result === "555");
test = 2;
result = (test + "5");
Assert.assertTrue("result === 25: " + result, result === "25");

result = +new Date;
Assert.assertTrue("+new Date > 0: " + result, result > 0);
Assert.assertTrue("+\"2\" === 2", +"2" === 2);