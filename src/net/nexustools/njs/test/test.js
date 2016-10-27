(function main(global) {
	'strict';
	
	print("T".charCodeAt(0));

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
	
	function cheese() {
		return tester;
	}

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
		}, cheese];
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
	
	try {
		Symbol.iterator * 25;
	} catch(e) {
		print(e.stack);
		importClass("javax.swing.JOptionPane").showMessageDialog(null, e);
	}
	
	if(true)
		print("if true works!");
	
	if(false)
		print("WRONG!");
	else
		print("If Else Right!");
	
	print(global.JSON.stringify({
		"judas": (new tuna).horse(),
		tuna: "fish",
		farmer: 23
	}));
	
	print(Throwable.prototype.constructor);
	
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
	
	var key = "sausage";
	print((function() {
		return {
			sausage: (function() {
				return this.hamster;
			}).call({
				hamster: "Jesus"
			})
		};
	})()[key]);
	
	var solid = {
		horses: 23,
		loops: 43
	};
	
	var horses = "horses";
	
	print(JSON.stringify(solid));
	print(delete solid[horses]);
	print(JSON.stringify(solid));
	
	print((function() {
		return {
			further: function seasoned() {
				return 2000;
			}
		}
	})().further());
	
	var JOptionPane = importClass("javax.swing.JOptionPane");
	try {
		print(new Uint8Array(JOptionPane.showInputDialog("How Many?")*1));
	} catch(e) {
		print(e.stack);
		JOptionPane.showMessageDialog(null, e);
	}
	print(JSON.stringify(this));
	
	print(true);
	
	function muffin() {
		cheese.call(this);
		throw new Error("Farmers Dress");
	}
	
	try {
		require("mother.js")(muffin, JOptionPane);
	} catch(e) {
		print(e.stack);
		JOptionPane.showMessageDialog(null, e);
	}
	
	function 侷() {
		throw new Error("Tuba Fish");
	}
	
	侷();
	
	
	
	/*while(1) {
		ExecutorService.execute(function() {
			while(1) {
				System.out.println("Hello World");
			}
		});
	}*/
}).call(this, this);
