/* 
 * Copyright (C) 2017 NexusTools.
 *
 * This library is free software: you can redistribute it and/or modify  
 * it under the terms of the GNU Lesser General Public License as   
 * published by the Free Software Foundation, version 3.0.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

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

Assert.assertTrue(java.lang.NullPointerException instanceof Function);
var nullException = new java.lang.NullPointerException();
Assert.assertTrue(nullException instanceof java.lang.Object);
Assert.assertTrue(nullException instanceof java.lang.Throwable);
Assert.assertTrue(nullException instanceof java.lang.RuntimeException);