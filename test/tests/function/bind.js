var Assert = importClass("org.junit.Assert");
var System = importClass("java.lang.System");

function hornets(a, b, c) {
	return [
		this, a, b, c, arguments.length
	];
}

var fishies = hornets.bind({farmer:true});
Assert.assertTrue(fishies()[0].farmer === true);