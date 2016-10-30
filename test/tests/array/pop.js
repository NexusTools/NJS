var Assert = importClass("org.junit.Assert");

var data = [23, 24, 25];
Assert.assertTrue(data.pop() === 25);
Assert.assertTrue(data.length === 2);
Assert.assertTrue(data.pop() === 24);
Assert.assertTrue(data.length === 1);
Assert.assertTrue(data.pop() === 23);
Assert.assertTrue(!data.length);