package net.nexustools.njs.jrebridge;

import net.nexustools.njs.Utilities;

import sun.misc.JavaLangAccess;
import sun.misc.SharedSecrets;

public class JLAStackTraceHelperCreator implements Utilities.StackTraceHelperCreator {
  private static final JavaLangAccess JavaLangAccess = SharedSecrets.getJavaLangAccess();
  @Override
	public Utilities.StackTraceHelper create() {
		final Throwable throwable = new Throwable();
		return new Utilities.StackTraceHelper() {
			@Override
			public StackTraceElement get(int index) {
				return JavaLangAccess.getStackTraceElement(throwable, index);
			}
			@Override
			public int size() {
				return JavaLangAccess.getStackTraceDepth(throwable);
			}
		};
	}
}
