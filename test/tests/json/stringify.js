var Assert = importClass("org.junit.Assert");

Assert.assertTrue(JSON.stringify({}) === "{}");
Assert.assertTrue(JSON.stringify([]) === "[]");
Assert.assertTrue(JSON.stringify() === undefined);
Assert.assertTrue(JSON.stringify(undefined) === undefined);
Assert.assertTrue(JSON.stringify(null) === "null");