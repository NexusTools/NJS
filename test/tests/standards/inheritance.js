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

Assert.assertTrue({} instanceof Object);
Assert.assertTrue([] instanceof Array);
Assert.assertTrue(23 instanceof Number);
Assert.assertTrue("" instanceof String);
Assert.assertTrue(Array instanceof Object);
Assert.assertTrue(Array instanceof Function);
Assert.assertTrue(Function instanceof Object);
Assert.assertTrue(GeneratorFunction instanceof Object);
Assert.assertTrue(GeneratorFunction instanceof Function);