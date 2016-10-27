/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs.test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;

import net.nexustools.njs.Global;
import net.nexustools.njs.JSHelper;
import net.nexustools.njs.Undefined;
import net.nexustools.njs.BaseObject;
import net.nexustools.njs.AbstractFunction;

/**
 *
 * @author kate
 */
public class Test {
	public static void main(String... args) throws FileNotFoundException {
		final Global global = JSHelper.createExtendedGlobal();
		global.setHidden("print", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				System.out.println(params[0]);
				return Undefined.INSTANCE;
			}
		});
		global.setHidden("require", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return global.compiler.eval(new InputStreamReader(Test.class.getResourceAsStream("/net/nexustools/njs/test/" + params[0].toString())), params[0].toString(), false).exec(global, null);
		
			}
		});
		global.setHidden("complex", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				try {
					return ((net.nexustools.njs.BaseFunction)params[0]).call(_this);
				} catch(Throwable t) {
					throw new net.nexustools.njs.Error.JavaException("HorseError", "Fiery Death Awaits!", t);
				}
			}
		});
		
		global.compiler.eval(new InputStreamReader(Test.class.getResourceAsStream("/net/nexustools/njs/test/test.js")), "test.js", false).exec(global, null);
		//global.compiler.eval(new FileReader("/home/kate/Projects/SnappFu/JNode12/node/node.js"), null, false).exec(global, null);
	}
}
