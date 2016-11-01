var Assert = importClass("org.junit.Assert");

Assert.assertTrue(isJavaPackage(java));
Assert.assertTrue(isJavaPackage(javax));
Assert.assertTrue(isJavaClass(java.lang.Object));
Assert.assertTrue(isJavaClass(java.lang.Throwable));
Assert.assertTrue(isJavaObject(new java.lang.Throwable()));

Assert.assertTrue(typeof java === "package");
Assert.assertTrue(typeof javax === "package");
Assert.assertTrue(typeof PackageRoot === "package");
Assert.assertTrue(typeof PackageRoot.java === "package");
Assert.assertTrue(typeof PackageRoot.javax === "package");

Assert.assertTrue(java === PackageRoot.java);
Assert.assertTrue(java.lang === PackageRoot.java.lang);
Assert.assertTrue(java.lang.Object === PackageRoot.java.lang.Object);