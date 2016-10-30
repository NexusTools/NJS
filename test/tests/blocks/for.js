var Assert = importClass("org.junit.Assert");

for(var i=0; i<10; i++)
	Assert.assertTrue(i >= 0);

for(; i>0;) {
	Assert.assertTrue(--i >= 0);
}
