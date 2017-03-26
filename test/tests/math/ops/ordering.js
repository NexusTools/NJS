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

var first = 3+4*5;
Assert.assertTrue(first === 23);
Assert.assertTrue((54-first-1+200-first) === 207);
Assert.assertTrue((54+first+1-200+first) === -99);
Assert.assertTrue((first+54/first*88-first) === 206.60869565217394);
Assert.assertTrue((2000/54*44/first-first) === 47.853462157809986);
Assert.assertTrue((2000/54*44/first-first) === 47.853462157809986);
Assert.assertTrue(34%84*12/66 === 6.181818181818182);