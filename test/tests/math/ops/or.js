var Assert = importClass("org.junit.Assert");

Assert.assertTrue((43 | 21) == "63");
Assert.assertTrue(("43" | 21) == "63");
Assert.assertTrue((43 | "21") == "63");
Assert.assertTrue(("43" | "21") == "63");
Assert.assertTrue((43 | 21) === 63);
Assert.assertTrue(("43" | 21) === 63);
Assert.assertTrue((43 | "21") === 63);
Assert.assertTrue(("43" | "21") === 63);