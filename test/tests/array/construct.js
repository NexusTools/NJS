var Assert = importClass("org.junit.Assert");

Assert.assertTrue([].length == 0);
Assert.assertTrue([12].length == 1);
Assert.assertTrue([12, 23].length == 2);

Assert.assertTrue(Array(8).length == 8);
Assert.assertTrue(Array(23, 24, 25).length == 3);
Assert.assertTrue(new Array(23, 24, 25).length == 3);
