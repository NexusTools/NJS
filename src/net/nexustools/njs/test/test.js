/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

(function() {
	'strict';
	
	var test = 0;
	while(test < 10) {
		test+=1;
	}

	var Throwable = importClass("java.lang.Throwable");
	print(Throwable);

	print("Tuna Fish");
	var throwable = new Throwable("Muffin Cow");
	print(throwable.hashCode);
	print(throwable.hashCode());
	print(throwable.toString());
	
	var System = importClass("java.lang.System");
	System.out.println(4332432.0);

	var tester = new Uint16Array(10);

	function jesus(size) {
		var horses = new Uint8Array(size || 10);
		return function fish(jesus, $) {
			return horses;
		}
	}
	
	var solid = [jesus(5)(), jesus()()];
	solid['tuna'] = 23;
	solid['oranges'] = Math.random()*433;
	solid['bubblegum'] = Math.random()*653;
	
	print(JSON.stringify(solid));
	
	print(jesus()());
})();
