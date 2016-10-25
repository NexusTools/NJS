/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs.test;

import java.io.InputStreamReader;
import java.util.Arrays;
import net.nexustools.njs.AbstractFunction;
import net.nexustools.njs.BaseFunction;
import net.nexustools.njs.Global;
import net.nexustools.njs.JSHelper;
import net.nexustools.njs.Undefined;
import net.nexustools.njs.BaseObject;
import net.nexustools.njs.Uint8Array;

/**
 *
 * @author kate
 */
public class Test {
	public static void main(String... args) {
		Global global = JSHelper.createExtendedGlobal();
		global.setStorage("print", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				System.out.println(params[0]);
				return Undefined.INSTANCE;
			}
		}, false);
		
		int amnt = 0;
		Uint8Array.Instance array = new Uint8Array.Instance(global, 8);
		for(int i=0; i<8; i++) {
			amnt += Math.random() * Byte.MAX_VALUE;
			array.set("" + i, global.wrap(amnt));
		}
		System.out.println(array);
		
		System.out.println(global.compiler.eval("", false).exec(global, null));
		System.out.println(global.compiler.eval("\"Tuna Fish \\\" Parade \\\"!\"", false).exec(global, null));
		System.out.println(global.compiler.eval("432.23", false).exec(global, null));
		System.out.println(global.compiler.eval("1.23e-2", false).exec(global, null));
		System.out.println(global.compiler.eval("123e-4", false).exec(global, null));
		System.out.println(global.compiler.eval("1e54", false).exec(global, null));
		System.out.println(global.compiler.eval("54", false).exec(global, null));
		System.out.println(global.compiler.eval("NaN", false).exec(global, null));
		System.out.println(global.compiler.eval("this", false).exec(global, null));
		System.out.println(global.compiler.eval("Date", false).exec(global, null));
		System.out.println(global.compiler.eval("new Date", false).exec(global, null));
		System.out.println(global.compiler.eval("new Uint8Array(8)", false).exec(global, null));
		System.out.println(global.compiler.eval("JSON", false).exec(global, null));
		System.out.println(global.compiler.eval("JSON.stringify", false).exec(global, null));
		System.out.println(global.compiler.eval("JSON.parse", false).exec(global, null));
		System.out.println(global.compiler.eval("JSON.toString()", false).exec(global, null));
		System.out.println(global.compiler.eval("JSON.munchkin = 23", false).exec(global, null));
		System.out.println(global.compiler.eval("JSON.stringify(JSON)", false).exec(global, null));
		System.out.println(global.compiler.eval("Object", false).exec(global, null));
		System.out.println(global.compiler.eval("Object.prototype", false).exec(global, null));
		System.out.println(global.compiler.eval("Object.prototype.toString.call(32)", false).exec(global, null));
		System.out.println(global.compiler.eval("toString.call(new Date)", false).exec(global, null));
		System.out.println(global.compiler.eval("toString.call(new Date(1000 * 60 * 60 * 30))", false).exec(global, null));
		System.out.println(global.compiler.eval("JSON.stringify(new Array(2))", false).exec(global, null));
		System.out.println(global.compiler.eval("JSON.stringify(Object)", false).exec(global, null));
		System.out.println(global.compiler.eval("toString.call(null)", false).exec(global, null));
		System.out.println(global.compiler.eval("toString()", false).exec(global, null));
		System.out.println(global.compiler.eval("(new Date)", false).exec(global, null));
		System.out.println(global.compiler.eval("+(new Date)", false).exec(global, null));
		System.out.println(global.compiler.eval("(new Date).valueOf", false).exec(global, null));
		System.out.println(global.compiler.eval("(new Date).toString", false).exec(global, null));
		System.out.println(global.compiler.eval("new (Date)()", false).exec(global, null));
		System.out.println(global.compiler.eval("new (Date)(5435435435345)", false).exec(global, null));
		System.out.println(global.compiler.eval("var array = new Uint8Array(8)", false).exec(global, null));
		System.out.println(global.compiler.eval("Object.keys(array)", false).exec(global, null));
		System.out.println(global.compiler.eval("Object.getOwnPropertyNames(array)", false).exec(global, null));
		System.out.println(global.compiler.eval("JSON.stringify(array)", false).exec(global, null));
		System.out.println(global.compiler.eval("array[0] = 255", false).exec(global, null));
		System.out.println(global.compiler.eval("array[1] = 200", false).exec(global, null));
		System.out.println(global.compiler.eval("array[2] = 150", false).exec(global, null));
		System.out.println(global.compiler.eval("array[3] = 100", false).exec(global, null));
		for(int i=4; i<8; i++)
			System.out.println(global.compiler.eval("array[" + i + "] = Math.random()*255", false).exec(global, null));
		System.out.println(global.compiler.eval("JSON.stringify(array)", false).exec(global, null));
		System.out.println(global.compiler.eval("array", false).exec(global, null));
		System.out.println(global.compiler.eval("(array) = 23", false).exec(global, null));
		System.out.println(global.compiler.eval("JSON.stringify(array)", false).exec(global, null));
		System.out.println(global.compiler.eval("[2, 3, 4]", false).exec(global, null));
		
		BaseFunction func = (BaseFunction)global.compiler.eval("(function horses(a, b, c){return a*b*c;})", false).exec(global, null);
		System.out.println(func);
		System.out.println(func.call(global, global.wrap(1), global.wrap(2), global.wrap(3)));
		
		//array = (Uint8Array.Instance)global.compiler.eval("((function test() {var array = new Uint8Array(8); var a=0; for(var i=0; i<8; i++) {array[i] = a = a + Math.random()*128;} return array;})())", false).exec(global, null);
		//System.out.println(array);
		
		AbstractFunction function = (AbstractFunction)global.Function.construct(global.wrap("a, b, c"), global.wrap("var val = Math.random()\nval *= a\nval *= b\nval *= c\nreturn val;"));
		System.out.println(function);
		System.out.println(function.call(global, global.wrap(23), global.wrap(24), global.wrap(25)));
		
		global.compiler.eval(new InputStreamReader(Test.class.getResourceAsStream("/net/nexustools/njs/test/test.js")), false).exec(global, null);
	}
}
