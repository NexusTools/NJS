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

function multiply(val, multi) {
    return val * multi;
}

function check(num, ident) {
    return ident(num) === num;
}

function checkneg(num, multi, ident) {
    return ident(num, multi) === -num;
}

Assert.assertTrue(check(77, val => multiply(val, 1)));
Assert.assertTrue(checkneg(77, -1, (val, multi) => multiply(val, multi)));
Assert.assertTrue(check(77, val => {
    System.out.println((new Error).stack);
    return val;
}));
