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


var array = [];
Assert.assertTrue(array.toString() == "");
array.length = 5;
array.fill();
Assert.assertTrue(array.toString() == "undefined,undefined,undefined,undefined,undefined");
array.fill(null);
Assert.assertTrue(array.toString() == "null,null,null,null,null");