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
	tests.MathOperations.class,
	
	tests.Array.class,
	
	tests.Comparisons.class})
public class TestSuite {
}
