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

Assert.assertTrue('(43 | 21) == "63"', (43 | 21) == "63");
Assert.assertTrue(("43" | 21) == "63");
Assert.assertTrue((43 | "21") == "63");
Assert.assertTrue(("43" | "21") == "63");
Assert.assertTrue((43 | 21) === 63);
Assert.assertTrue(("43" | 21) === 63);
Assert.assertTrue((43 | "21") === 63);
Assert.assertTrue(("43" | "21") === 63);

var test = 43;
test |= 21;
Assert.assertTrue(test === 63);