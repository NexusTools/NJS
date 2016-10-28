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
	
	print(Throwable.prototype.constructor);
	
	/*if(throwable instanceof Throwable.prototype.constructor) {
		print("tuna is a function!");
	} else if (true) {
		print(true);
	} else {
		print("tuna is not a function...");
	}*/
	
	var ExecutorService = importClass("java.util.concurrent.Executors").newCachedThreadPool();
	var Thread = importClass("java.lang.Thread");
	print(ExecutorService.execute);
	
	function setTimeout(cb, timeout) {
		ExecutorService.execute(function() {
			if(timeout)
				Thread.sleep(timeout);
			cb();
		});
	}
	
	print((function() {
		return 52;
	})() * (function() {
		return 2;
	})());
	
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
	
	function JSHelper() {
		var Thrown = importClass("net.nexustools.njs.Error$Thrown");
		print(new Thrown(true).stack);
		//new Thrown(true).printStackTrace();
	}
	
	try {
		require("mother.js")(muffin, JOptionPane);
	} catch(e) {
		print(eval("e.stack"));
		print(eval("new Error().stack"));
		JOptionPane.showMessageDialog(null, e);
	}
	
	new JSHelper();
	
	setTimeout(function exit() {
		importClass("java.lang.System").exit(0);
	}, 5000);
	
	
	
	/*while(1) {
		ExecutorService.execute(function() {
			while(1) {
				System.out.println("Hello World");
			}
		});
	}*/
})();
