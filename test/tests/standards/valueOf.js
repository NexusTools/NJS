var Assert = importClass("org.junit.Assert");
var System = importClass("java.lang.System");

function TunaFish(value) {
	this.value = value || 0;
}
TunaFish.prototype.valueOf = function() {
	return this.value;
};

Assert.assertTrue(new TunaFish(5) > new TunaFish());
Assert.assertTrue(new TunaFish(5) >= new TunaFish(5));
Assert.assertTrue(new TunaFish(5) + new TunaFish(5) >= new TunaFish(10));
Assert.assertTrue(new TunaFish(5) + new TunaFish(5) <= new TunaFish(10));
Assert.assertTrue(new TunaFish(5) - new TunaFish(5) < new TunaFish(10));