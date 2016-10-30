var Assert = importClass("org.junit.Assert");

Assert.assertTrue((54 & 12) == "4");
Assert.assertTrue(("54" & 12) == "4");
Assert.assertTrue((54 & "12") == "4");
Assert.assertTrue(("54" & "12") == "4");
Assert.assertTrue((54 & 12) === 4);
Assert.assertTrue(("54" & 12) === 4);
Assert.assertTrue((54 & "12") === 4);
Assert.assertTrue(("54" & "12") === 4);