var Assert = importClass("org.junit.Assert");
var System = importClass("java.lang.System");

Object.getOwnPropertyNames(this).forEach((function (key) {
    new (eval("(function " + key + "() {})"));
}).bind(this));

try {
    throw new Error("Muffin Tuffin");
} catch (e) {
    Assert.assertTrue(eval("e").message === "Muffin Tuffin");
}