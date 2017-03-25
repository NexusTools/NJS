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

var test = {
	fish: 23
};
var testProto = {
	jesusFish: function() {
		return this.fish;
	}
};
test.__proto__ = testProto;
Assert.assertTrue(testProto === test.__proto__);
Object.defineProperty(test.__proto__, "jesusHorse", {
	get: function() {
		return this.fish;
	},
	set: function() {
		// ignored
	}
});

Assert.assertTrue(test.jesusFish() === test.fish);
Assert.assertTrue(test.jesusHorse === test.fish);

test.jesusHorse = 24;

Assert.assertTrue(test.jesusHorse === test.fish);