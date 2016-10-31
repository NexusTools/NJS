var Assert = importClass("org.junit.Assert");

Assert.assertTrue(typeof java === "package");
Assert.assertTrue(typeof javax === "package");
Assert.assertTrue(typeof PackageRoot === "package");
Assert.assertTrue(typeof PackageRoot.java === "package");
Assert.assertTrue(typeof PackageRoot.javax === "package");
Assert.assertTrue(java.lang.Object instanceof Function);
Assert.assertTrue(new java.lang.Throwable() instanceof java.lang.Object);
Assert.assertTrue(new java.lang.RuntimeException() instanceof java.lang.Throwable);
Assert.assertTrue(new java.lang.RuntimeException() instanceof java.lang.Object);
Assert.assertTrue(!(new java.lang.NullPointerException() instanceof Function));
Assert.assertTrue(java.lang.NullPointerException instanceof Function);

Assert.assertTrue(isJavaPackage(java));
Assert.assertTrue(isJavaPackage(javax));
Assert.assertTrue(isJavaClass(java.lang.Throwable));
Assert.assertTrue(isJavaObject(new java.lang.Throwable()));