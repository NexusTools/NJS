var Assert = importClass("org.junit.Assert");

try {
	throw new Error();
} catch(e) {
	Assert.assertTrue(e);
}

try {
	Assert.fail();
} catch(e) {
	Assert.assertTrue(e);
}

try {
	java.lang.System.out.println(Object.keys(this));
	var tuna = new Error("Tuna Fish");
	try {
		try {
			
		} finally {
			Assert.fail();
		}
	} catch(e) {
		throw tuna;
	}
	Assert.fail();
} catch(e) {
	if(e !== tuna)
		throw e;
} finally {
	Assert.assertTrue(true);
}