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

Assert.assertTrue([].length == 0);
Assert.assertTrue([12].length == 1);
Assert.assertTrue([12, 23].length == 2);

Assert.assertTrue(Array(8).length == 8);
Assert.assertTrue(Array(23, 24, 25).length == 3);
Assert.assertTrue(new Array(23, 24, 25).length == 3);
