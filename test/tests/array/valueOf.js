var Assert = importClass("org.junit.Assert");
var System = importClass("java.lang.System");

Assert.assertTrue([] + 1 === 1);
Assert.assertTrue([5] + [12] === 17);
Assert.assertTrue([5] + [12, 13] === "512,13");