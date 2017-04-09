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

switch(24) {
    case 24:
        Assert.assertTrue(true);
        break;
        
    default:
        Assert.assertTrue(false);
}

switch(27) {
    case 24:
        Assert.assertTrue(false);
        break;
        
    default:
        Assert.assertTrue(true);
}

function go(val) {
    switch(val) {
        case "5":
        case "tuna":
        case undefined:
            Assert.assertTrue(true);
            break;
            
        default:
            Assert.assertTrue(false);
    }
}

go("5");
go("tuna");
go(undefined);

switch(2.8) {
    case 2.8:
        Assert.assertTrue(true);
        break;
        
    default:
        Assert.assertTrue(false);
}