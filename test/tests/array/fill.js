var Assert = importClass("org.junit.Assert");
var System = importClass("java.lang.System");


var array = [];
Assert.assertTrue(array.toString() == "");
array.length = 5;
array.fill();
Assert.assertTrue(array.toString() == "undefined,undefined,undefined,undefined,undefined");
array.fill(null);
Assert.assertTrue(array.toString() == "null,null,null,null,null");