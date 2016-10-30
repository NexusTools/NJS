/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs.test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;

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
		global.setHidden("require", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return global.compiler.compile(new InputStreamReader(Test.class.getResourceAsStream("/net/nexustools/njs/test/" + params[0].toString())), params[0].toString(), false).exec(global, null);
		
			}
		});
		
		//eval(new InputStreamReader(Test.class.getResourceAsStream("/net/nexustools/njs/test/test.js")), "test.js", global);
		//eval("/home/kate/Projects/SnappFu/JNode12/node/node.js", global);
		eval("/home/kate/Desktop/prime.js", global);
	}
	
	public static void eval(String fileName, Global global) throws FileNotFoundException {
		eval(new FileReader(fileName), fileName, global);
	}
	
	public static void eval(String source, String fileName, Global global) {
		System.out.println(global.compiler.compile(source, fileName, false).exec(global, null));
	}
	
	public static void eval(Reader reader, String fileName, Global global) {
		System.out.println(global.compiler.compile(reader, fileName, false).exec(global, null));
	}
	
}
