var Assert = importClass("org.junit.Assert");

var first = 3+4*5;
Assert.assertTrue(first === 23);
Assert.assertTrue((54-first-1+200-first) === 207);
Assert.assertTrue((54+first+1-200+first) === -99);
Assert.assertTrue((first+54/first*88-first) === 206.60869565217394);
Assert.assertTrue((2000/54*44/first-first) === 47.853462157809986);