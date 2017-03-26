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

var data = [23, 24, 25];
Assert.assertTrue(data.shift() === 23);
Assert.assertTrue(data.length === 2);
Assert.assertTrue(data.shift() === 24);
Assert.assertTrue(data.length === 1);
Assert.assertTrue(data.shift() === 25);
Assert.assertTrue(!data.length);