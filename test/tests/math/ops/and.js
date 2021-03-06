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

Assert.assertTrue('(54 & 12) == "4"', (54 & 12) == "4");
Assert.assertTrue(("54" & 12) == "4");
Assert.assertTrue((54 & "12") == "4");
Assert.assertTrue(("54" & "12") == "4");
Assert.assertTrue((54 & 12) === 4);
Assert.assertTrue(("54" & 12) === 4);
Assert.assertTrue((54 & "12") === 4);
Assert.assertTrue(("54" & "12") === 4);

var test = 54;
test &= 12;
Assert.assertTrue('test === 4', test === 4);