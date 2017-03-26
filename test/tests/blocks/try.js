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

try {
	throw new Error();
} catch(e) {
	Assert.assertTrue(e);
}

try {
	Assert.fail();
} catch(e) {
	Assert.assertTrue(e);
}

try {
	var tuna = new Error("Tuna Fish");
	try {
		try {
			
		} finally {
			Assert.fail();
		}
	} catch(e) {
		throw tuna;
	}
	Assert.fail();
} catch(e) {
	if(e !== tuna)
		throw e;
} finally {
	Assert.assertTrue(true);
}