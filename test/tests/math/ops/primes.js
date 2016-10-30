var Assert = importClass("org.junit.Assert");

function getPrimes(x){
  var ar = [];
  for (var counter = 0; counter <= x; counter++) {
      var notPrime = false;
      for (var i = 2; (i <= ar.length) && !notPrime; i++) {
          if ((counter%ar[i])===0) {
              notPrime = true;
          }
      }
      if (notPrime === false) ar.push(counter);
  }
  return ar;
}

Assert.assertTrue(getPrimes(1000).length === 170);