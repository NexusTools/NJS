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

var testSymbol = Symbol("test");

class Empty {
    get nothing() {
        return 0;
    }
    [testSymbol]() {
        return "orange";
    }
}

class Test extends Empty {
    constructor(value) {
        this.value = value;
    }
    farmer(a, b, ...c) {
        return this.value / a * b - c.length;
    }
}
class Test2 extends Test {
    constructor() {
        super(27);
    }
    farmer(a, b, ...c) {
        return super.farmer(c[0], c[1]);
    }
    [testSymbol]() {
        return "yellow";
    }
}

Assert.assertTrue((new Empty) instanceof Empty);
Assert.assertTrue((new Empty)[testSymbol] === "orange");
Assert.assertTrue((new Test1)[testSymbol] === "orange");
Assert.assertTrue((new Test2) instanceof Empty);
Assert.assertTrue((new Test2) instanceof Test);
Assert.assertTrue((new Test2).farmer(null, null, 1, 2) == 54);
Assert.assertTrue((new Test2).nothing === 0);
Assert.assertTrue((new Test2)[testSymbol] === "yellow");
