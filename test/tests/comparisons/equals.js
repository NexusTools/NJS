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

Assert.assertTrue(4 == 4);
Assert.assertTrue(4 == "4");
Assert.assertTrue(true == 1);
Assert.assertTrue(true != 0);
Assert.assertTrue(true != 2);
Assert.assertTrue(4 === 4);
Assert.assertTrue(4 == new Number(4));
Assert.assertTrue(4 !== new Number(4));
Assert.assertTrue("tuna" === "tu" + "na");
Assert.assertTrue("tuna" !== new String("tuna"));
Assert.assertTrue("23" !== new String(23));
Assert.assertTrue("23" == new String(23));
Assert.assertTrue(-Infinity === -Infinity);
Assert.assertTrue(-Infinity !== Infinity);
Assert.assertTrue(Infinity === Infinity);
Assert.assertTrue(undefined !== null);
Assert.assertTrue(undefined == null);
//Assert.assertTrue(NaN !== NaN);
//Assert.assertTrue(NaN != NaN);