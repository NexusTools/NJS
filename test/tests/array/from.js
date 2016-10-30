var Assert = importClass("org.junit.Assert");

function munchkin() {
	return Array.from(arguments);
};

Assert.assertTrue(munchkin(12, 13, 14).length === 3);