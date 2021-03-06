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

if (true) {

} else
    Assert.fail();

function test(state) {
    if(state == 0)
        return "a";
    else if(state == 1)
        return "b";
    else if(state == 2)
        return "c";
    else
        return "d";
}

Assert.assertTrue(test(0) === "a");
Assert.assertTrue(test(1) === "b");
Assert.assertTrue(test(2) === "c");
Assert.assertTrue(test(3) === "d");