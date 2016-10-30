var Assert = importClass("org.junit.Assert");

Assert.assertTrue(4 == 4);
Assert.assertTrue(4 === 4);
Assert.assertTrue(4 == new Number(4));
Assert.assertTrue(4 !== new Number(4));
Assert.assertTrue(Infinity == Infinity);
Assert.assertTrue(NaN == NaN);