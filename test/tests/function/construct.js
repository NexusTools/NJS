var Assert = importClass("org.junit.Assert");
var System = importClass("java.lang.System");

var test = new Function("return \"Toasted Wheat\"");
Assert.assertTrue(test() === "Toasted Wheat");
test = new Function("a, b, c", "return a * b() / c.value");
Assert.assertTrue(test(23, function() {
	return 10;
}, {
	value: 55
}) === 4.181818181818182);
test = new Function("a", "b", "c", "return a * b() / c.value");
Assert.assertTrue(test(55, function() {
	return 12;
}, {
	value: 4.5
}) === 146.66666666666666);
test = new Function("return eval(\"new Error()\").stack");
System.out.println(test());