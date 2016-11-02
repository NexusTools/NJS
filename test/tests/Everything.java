/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 *
 * @author kate
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	Blocks.class,
	Comparisons.class,
	MathOperations.class,
	Standards.class,
	
	Function.class,
	String.class,
	Array.class,
	JSON.class,
	
	Extended.class,
	Eval.class})
public class Everything {
}
