var Assert = importClass("org.junit.Assert");

Assert.assertTrue(JSON.parse("{\"tunaFish\": \"Master of the Government\"}").tunaFish === "Master of the Government");
try {
	JSON.parse("\\[]@#");
	Assert.fail();
} catch(e) {
	Assert.assertTrue(e.message === "Unexpected token");
}