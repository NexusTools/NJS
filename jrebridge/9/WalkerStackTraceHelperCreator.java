package net.nexustools.njs.jrebridge;

import java.util.function.Consumer;
import net.nexustools.njs.Utilities;

public class WalkerStackTraceHelperCreator implements Utilities.StackTraceHelperCreator {
  @Override
	public Utilities.StackTraceHelper create() {
		final StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
		return new Utilities.StackTraceHelper() {
			@Override
			public StackTraceElement get(final int index) {
				walker.forEach(new Consumer<StackWalker.StackFrame>() {
					int until = index;
					@Override
					public void accept(StackWalker.StackFrame t) {
						
					}
				});
			}
			@Override
			public int size() {
				final int[] size = {0};
				walker.forEach(new Consumer<StackWalker.StackFrame>() {
					@Override
					public void accept(StackWalker.StackFrame t) {
						size[0]++;
					}
				});
				return size[0];
			}
		};
	}
}
