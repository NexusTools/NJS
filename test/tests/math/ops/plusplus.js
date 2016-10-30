var Assert = importClass("org.junit.Assert");

var value = 0;
//Assert.assertTrue(value++ === 0);
Assert.assertTrue(++value === 1);
Assert.assertTrue(value++ === 1);
Assert.assertTrue((value++) === 2);
Assert.assertTrue((++value) === 4);