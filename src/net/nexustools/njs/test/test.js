print((function() {
	'strict';
	
	var test = 0;
	//while(test < 10) {
		print(test);
		print(++test);
		print(test++);
	//}
	print(test);

	var Throwable = importClass("java.lang.Throwable");
	print(Throwable);

	print("Tuna Fish");
	var throwable = new Throwable("Muffin Cow");
	print(throwable.hashCode);
	print(throwable.hashCode());
	print(throwable.toString());
	
	var System = importClass("java.lang.System");
	System.out.println(4332432);

	var tester = new Uint16Array(2);
	tester[0] = Math.random()*65535;
	tester[1] = Math.random()*65535;

	function jesus(size) {
		var horses = new Uint8Array(size || 10);
		return [function fish(jesus, $) {
			horses[0] = $()*255;
			horses[1] = $()*255;
			horses[2] = $()*255;
			horses[3] = $()*255;
			horses[4] = $()*255;
			horses[5] = $()*255;
			horses[6] = $()*255;
			horses[7] = $()*255;
			horses[8] = $()*255;
			horses[9] = $()*255;
			return horses;
		}, function cheese() {
			return tester;
		}];
	}
	
	var solid = [jesus(5)[0](null, Math.random), jesus()[1]()];
	solid['tuna'] = 23;
	solid['oranges'] = Math.random()*433;
	solid['bubblegum'] = Math.random()*653;
	
	print(JSON.stringify(solid));
	
	print(jesus()[0](null, function() {
		return 1;
	}));
	
	print(throwable.hashCode());
	
	
	function tuna() {
		
	}
	
	tuna.prototype.horse = function() {
		return "Farmers";
	};
	
	System.out.println(tuna);
	System.out.println((new tuna));
	System.out.println((new tuna).horse());
	
	(function() {
		while(test < 10) {
			test++;
		}
		return test;
	})();
	
	print(JSON.stringify({
		"judas": (new tuna).horse(),
		tuna: "fish",
		farmer: 23
	}));
	
	/*if(throwable instanceof Throwable.prototype.constructor) {
		print("tuna is a function!");
	} else if (true) {
		print(true);
	} else {
		print("tuna is not a function...");
	}*/
	
	var ExecutorService = importClass("java.util.concurrent.Executors").newCachedThreadPool();
	print(ExecutorService.execute);
	
	print((function() {
		return 52;
	})() * (function() {
		return 2;
	})());
	
	while(true) {
		while(test < 200)
			++test;
		return test;
	}
	
	/*while(1) {
		ExecutorService.execute(function() {
			while(1) {
				System.out.println("Hello World");
			}
		});
	}*/
})());
