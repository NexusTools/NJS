var Assert = importClass("org.junit.Assert");

Assert.assertTrue(4 == 4);
Assert.assertTrue(4 == "4");
Assert.assertTrue(true == 1);
Assert.assertTrue(true != 0);
Assert.assertTrue(true != 2);
Assert.assertTrue(4 === 4);
Assert.assertTrue(4 == new Number(4));
Assert.assertTrue(4 !== new Number(4));
Assert.assertTrue("tuna" === "tu" + "na");
Assert.assertTrue("tuna" !== new String("tuna"));
Assert.assertTrue(-Infinity === -Infinity);
Assert.assertTrue(-Infinity !== Infinity);
Assert.assertTrue(Infinity === Infinity);
Assert.assertTrue(NaN == NaN);