var Assert = importClass("org.junit.Assert");
var ByteArrayOutputStream = importClass("java.io.ByteArrayOutputStream");

var outStream = new ByteArrayOutputStream();
outStream.write("Test");
Assert.assertTrue(outStream.size() === 4);

var bytes = outStream.toByteArray();
Assert.assertTrue(bytes.toString() === "84,101,115,116");