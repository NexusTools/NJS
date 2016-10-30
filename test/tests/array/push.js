var Assert = importClass("org.junit.Assert");

var data = [];
Assert.assertTrue(!data.length);
data.push(23);
Assert.assertTrue(data.length == 1);
Assert.assertTrue(data[data.length-1] == 23);
Assert.assertTrue(!!data.length);
Assert.assertTrue(data.length === 1);
data.push(23);
Assert.assertTrue(data.length === 2);