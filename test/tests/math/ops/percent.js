var Assert = importClass("org.junit.Assert");

Assert.assertTrue((230 % 25) == "5");
Assert.assertTrue(("230" % 25) == "5");
Assert.assertTrue((230 % "25") == "5");
Assert.assertTrue(("230" % "25") == "5");
Assert.assertTrue((230 % 25) === 5);
Assert.assertTrue(("230" % 25) === 5);
Assert.assertTrue((230 % "25") === 5);
Assert.assertTrue(("230" % "25") === 5);