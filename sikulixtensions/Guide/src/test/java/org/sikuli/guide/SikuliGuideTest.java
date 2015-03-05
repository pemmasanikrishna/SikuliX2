/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sikuli.guide;

import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import javax.swing.JComponent;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.sikuli.script.Pattern;
import org.sikuli.script.Region;
import org.sikuli.script.Screen;
import org.sikuli.util.EventSubject;

/**
 *
 * @author rhocke
 */
public class SikuliGuideTest {

	SikuliGuide guide = null;
	static Screen scr;

	public SikuliGuideTest() {
	}

	@BeforeClass
	public static void setUpClass() {
		scr = new Screen();
	}

	@AfterClass
	public static void tearDownClass() {
	}

	@Before
	public void setUp() {
		guide = new SikuliGuide();
	}

	@After
	public void tearDown() {
		guide.showNow(2f);
		guide = null;
	}

	/**
	 * Test of rectangle method, of class SikuliGuide.
	 */
	@Test
	public void testRectangle() {
		SikuliGuideComponent result = guide.rectangle();
		result.setActualSize(100, 100);
		result.setTarget(scr.getCenter().grow(100));
		assertNotNull(result);
		// TODO review the generated test code and remove the default call to fail.
		//fail("The test case is a prototype.");
	}

	/**
	 * Test of circle method, of class SikuliGuide.
	 */
	@Ignore
	@Test
	public void testCircle() {
		System.out.println("circle");
		SikuliGuide instance = new SikuliGuide();
		SikuliGuideComponent expResult = null;
		SikuliGuideComponent result = instance.circle();
		assertEquals(expResult, result);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

	/**
	 * Test of text method, of class SikuliGuide.
	 */
	@Ignore
	@Test
	public void testText() {
		System.out.println("text");
		String text = "";
		SikuliGuide instance = new SikuliGuide();
		SikuliGuideComponent expResult = null;
		SikuliGuideComponent result = instance.text(text);
		assertEquals(expResult, result);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

	/**
	 * Test of flag method, of class SikuliGuide.
	 */
	@Ignore
	@Test
	public void testFlag() {
		System.out.println("flag");
		String text = "";
		SikuliGuide instance = new SikuliGuide();
		SikuliGuideComponent expResult = null;
		SikuliGuideComponent result = instance.flag(text);
		assertEquals(expResult, result);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

	/**
	 * Test of callout method, of class SikuliGuide.
	 */
	@Ignore
	@Test
	public void testCallout() {
		System.out.println("callout");
		String text = "";
		SikuliGuide instance = new SikuliGuide();
		SikuliGuideComponent expResult = null;
		SikuliGuideComponent result = instance.callout(text);
		assertEquals(expResult, result);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

	/**
	 * Test of button method, of class SikuliGuide.
	 */
	@Ignore
	@Test
	public void testButton() {
		System.out.println("button");
		String name = "";
		SikuliGuide instance = new SikuliGuide();
		SikuliGuideComponent expResult = null;
		SikuliGuideComponent result = instance.button(name);
		assertEquals(expResult, result);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

	/**
	 * Test of bracket method, of class SikuliGuide.
	 */
	@Ignore
	@Test
	public void testBracket() {
		System.out.println("bracket");
		SikuliGuide instance = new SikuliGuide();
		SikuliGuideComponent expResult = null;
		SikuliGuideComponent result = instance.bracket();
		assertEquals(expResult, result);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

	/**
	 * Test of image method, of class SikuliGuide.
	 */
	@Ignore
	@Test
	public void testImage() {
		System.out.println("image");
		Object img = null;
		SikuliGuide instance = new SikuliGuide();
		SikuliGuideComponent expResult = null;
		SikuliGuideComponent result = instance.image(img);
		assertEquals(expResult, result);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

	/**
	 * Test of arrow method, of class SikuliGuide.
	 */
	@Ignore
	@Test
	public void testArrow() {
		System.out.println("arrow");
		Object from = null;
		Object to = null;
		SikuliGuide instance = new SikuliGuide();
		SikuliGuideComponent expResult = null;
		SikuliGuideComponent result = instance.arrow(from, to);
		assertEquals(expResult, result);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

}
