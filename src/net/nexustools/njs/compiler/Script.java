/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs.compiler;

import net.nexustools.njs.Global;
import net.nexustools.njs.BaseObject;
import net.nexustools.njs.Scope;

/**
 *
 * @author kate
 */
public interface Script {
	public BaseObject exec(Global global, Scope scope);
}
