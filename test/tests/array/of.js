var Assert = importClass("org.junit.Assert");

Assert.assertTrue(Array.of(12, 13, 14).length === 3);
Assert.assertTrue(Array.of(12, 13).length === 2);
Assert.assertTrue(Array.of(12).length === 1);