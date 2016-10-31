var Assert = importClass("org.junit.Assert");
var System = importClass("java.lang.System");

Assert.assertTrue({} instanceof Object);
Assert.assertTrue([] instanceof Array);
Assert.assertTrue(23 instanceof Number);
Assert.assertTrue("" instanceof String);
Assert.assertTrue(Array instanceof Object);
Assert.assertTrue(Array instanceof Function);
Assert.assertTrue(Function instanceof Object);
Assert.assertTrue(GeneratorFunction instanceof Object);
Assert.assertTrue(GeneratorFunction instanceof Function);