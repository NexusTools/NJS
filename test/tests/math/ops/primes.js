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

var start = +new Date;
function getPrimes(x){
  var ar = [];
  for (var counter = 0; counter <= x; counter++) {
      var notPrime = false;
      for (var i = 2; (i <= ar.length) && !notPrime; i++) {
          if (counter%ar[i]===0)
              notPrime = true;
      }
      if (notPrime === false) ar.push(counter);
  }
  return ar;
}

var primes = getPrimes(100);
Assert.assertTrue(primes.length === 27);
importClass("java.lang.System").out.println("Generated " + primes.length + " primes in " + ((+new Date) - start) + "ms");