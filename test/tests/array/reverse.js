var Assert = importClass("org.junit.Assert");

Assert.assertTrue([4, 3].reverse()[0] === 3);
Assert.assertTrue([4, 3, 5].reverse()[0] === 5);
Assert.assertTrue([1, 2, 3, 4, 5].reverse().toString() == "5,4,3,2,1");