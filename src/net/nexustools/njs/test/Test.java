/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs.test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.Arrays;
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
		Global global = JSHelper.createExtendedGlobal();
		global.setStorage("print", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				System.out.println(params[0]);
				return Undefined.INSTANCE;
			}
		}, false);
		
		//global.compiler.eval("var test=frank;function frank(){return 24};console.log(test())", false).exec(global, null);
		
		global.compiler.eval(new InputStreamReader(Test.class.getResourceAsStream("/net/nexustools/njs/test/test.js")), false).exec(global, null);
		//global.compiler.eval(new FileReader("/home/kate/Projects/SnappFu/JNode12/node/node.js"), false).exec(global, null);
	}
}
