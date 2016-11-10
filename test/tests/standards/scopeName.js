var Assert = importClass("org.junit.Assert");
var System = importClass("java.lang.System");

function test() {
	var _this = 23;
	var blockScope = 77;
	
	return _this - blockScope;
}

Assert.assertTrue(test() === -54);