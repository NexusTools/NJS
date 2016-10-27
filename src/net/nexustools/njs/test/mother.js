(function mother(muffin) {
	(function daughter() {
		(function sister() {
			(function () {
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