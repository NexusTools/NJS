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

var a = 23;
var b = 24;
var c = 25;

var group = {a, b, c};
Assert.assertTrue(group.a == a);
Assert.assertTrue(group.b == b);
Assert.assertTrue(group.c == c);

group = {a:c, b, c:a};
Assert.assertTrue(group.a == c);
Assert.assertTrue(group.b == b);
Assert.assertTrue(group.c == a);


var {a:_a,b:_b,c:_c} = group;
Assert.assertTrue(_a == group.a);
Assert.assertTrue(_b == group.b);
Assert.assertTrue(_c == group.c);