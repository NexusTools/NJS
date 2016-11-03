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