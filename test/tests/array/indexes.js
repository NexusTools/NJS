var Assert = importClass("org.junit.Assert");

var test = [23, 24, 25];
Assert.assertTrue(test[0] === test['0']);
Assert.assertTrue(test['0'] === test[0]);
Assert.assertTrue(test['0'] === 23);
Assert.assertTrue(test[0] === 23);