var Assert = importClass("org.junit.Assert");

while("")
	Assert.fail();

var i = 0;
while(i < 10)
	i++;

Assert.assertTrue(i === 10);

while(i <= 10)
	i++;

Assert.assertTrue(i === 11);

while(i > 0)
	i--;

Assert.assertTrue(i === 0);

while(i >= 0)
	i--;

Assert.assertTrue(i === -1);