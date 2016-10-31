var Assert = importClass("org.junit.Assert");
var System = importClass("java.lang.System");

function hornets(a, b, c) {
	return [
		this, a, b, c, arguments.length
	];
}

Assert.assertTrue(hornets.call(null, 12, 23, 24, 52)[0] === null);
Assert.assertTrue(hornets.call(null, 12, 23, 24, 52)[1] === 12);
Assert.assertTrue(hornets.call(null, 12, 23, 24, 52)[2] === 23);
Assert.assertTrue(hornets.call(null, 12, 23, 24, 52)[3] === 24);
Assert.assertTrue(hornets.call(null, 12, 23, 24, 52)[4] === 4);
