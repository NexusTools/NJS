var Assert = importClass("org.junit.Assert");

Assert.assertTrue(5 >= 5);
Assert.assertTrue(5 >= "5");
Assert.assertTrue("5" >= 5);
Assert.assertTrue("aab" > "aaa");
Assert.assertTrue("aaa" >= "aaa");
Assert.assertTrue("aaa" < "baz");