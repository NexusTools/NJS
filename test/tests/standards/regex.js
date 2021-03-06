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

var reg = /^\[object (.+)\]$/;
Assert.assertTrue(reg.test("[object Test]"));
Assert.assertTrue('"[object Test]".match(reg)[1] === "Test"', "[object Test]".match(reg)[1] === "Test");
Assert.assertTrue('"[object Test]".match(reg).length === 2', "[object Test]".match(reg).length === 2);