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
	MathOperations.class,
	Comparisons.class,
	Blocks.class,
	
	Array.class,
	
	RedefineGlobal.class})
public class TestSuite {
}
