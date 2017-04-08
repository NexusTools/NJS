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

function test(a, b, ...c) {
    return c.length;
}
function test2(a, b, ...c) {
    return c[a] * b;
}

Assert.assertTrue('test(22, 23, 24, 25, 26) === 3', test(22, 23, 24, 25, 26) === 3);
Assert.assertTrue('test2(2, -1, 24, 25, 26) === -26', test2(2, -1, 24, 25, 26) === -26);
