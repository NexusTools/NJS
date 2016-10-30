var Assert = importClass("org.junit.Assert");

var numbers = [], primes = [], maxNumber = 100;

var i = 2;
while(i<=maxNumber) {
	numbers.push(i);  
	i++; 
}

while(numbers.length) {
	var lastPrime;
	primes.push(lastPrime = numbers.shift());
	numbers = numbers.filter(function(i){
		return (i % lastPrime) !== 0;
	});
}

Assert.assertTrue(numbers.length === 0);
Assert.assertTrue(primes.length === 25);