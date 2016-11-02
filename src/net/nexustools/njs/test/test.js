var methods = (function(global) {
	var System = importClass("java.lang.System");
	var Executor = importClass("java.util.concurrent.Executors").newSingleThreadScheduledExecutor();
	var TimeUnit = importClass("java.util.concurrent.TimeUnit");
	
	for(var key in Executor) {
		System.out.println(key);
	}
	
	return {
		print: function(what) {
			System.out.println(what.toString());
		},
		setTimeout: function(cb, timeout) {
			if(timeout)
				Executor.schedule(cb, timeout, TimeUnit.MILLISECONDS);
			else
				Executor.execute(cb);
		},
		exit: function(code) {
			System.exit(code || 0);
		}
	};
})();

this.global = this;
Object.keys(methods).forEach(function(key) {
	global[key] = methods[key];
});

delete methods;

setTimeout(function main() {
	print("Tuna Fish");
	print(new Error().stack);
	exit();
});