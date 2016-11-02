var Assert = importClass("org.junit.Assert");
var System = importClass("java.lang.System");

var test = {
	fish: 23
};
var testProto = {
	jesusFish: function() {
		return this.fish;
	}
};
test.__proto__ = testProto;
Assert.assertTrue(testProto === test.__proto__);
Object.defineProperty(test.__proto__, "jesusHorse", {
	get: function() {
		return this.fish;
	},
	set: function() {
		// ignored
	}
});

Assert.assertTrue(test.jesusFish() === test.fish);
Assert.assertTrue(test.jesusHorse === test.fish);

test.jesusHorse = 24;

Assert.assertTrue(test.jesusHorse === test.fish);