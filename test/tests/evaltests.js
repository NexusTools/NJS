var System = importClass("java.lang.System");

Object.getOwnPropertyNames(this).forEach((function(key) {
	new (eval("(function " + key + "() {})"));
}).bind(this));