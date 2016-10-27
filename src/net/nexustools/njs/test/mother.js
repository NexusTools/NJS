(function mother(muffin) {
	(function daughter() {
		(function sister() {
			(function child() {
				try {
					new muffin();
				} catch(e) {
					print(e.stack);
					
					complex(function() {
						(function germans() {
							jewish.summer();
						})();
					});
				}
			})();
		})();
	})();
});