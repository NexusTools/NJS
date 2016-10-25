/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs;

/**
 *
 * @author kate
 */
public class Arguments extends GenericObject {
	
	public Arguments(Global global, BaseFunction callee, final BaseObject[] args) {
		super(global);
		setStorage("callee", callee, false);
		for(int i=0; i<args.length; i++)
			setStorage(java.lang.String.valueOf(i), args[i], true);
		setStorage("length", global.wrap(args.length), false);
	}
	
}
