/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sikuli.guide;

import java.awt.Color;
import org.sikuli.script.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

/**
 *
 * @author rhocke
 */
public class SikuliGuideTest {

	Guide guide = null;
	static Screen scr;

	public SikuliGuideTest() {
	}

	@BeforeClass
	public static void setUpClass() {
		scr = new Screen();
    ImagePath.add("org.sikuli.script.RunTime/ImagesAPI.sikuli");
	}

	@AfterClass
	public static void tearDownClass() {
	}

	@Before
	public void setUp() {
		guide = new Guide();
	}

	@After
	public void tearDown() {
		guide.showNow(2f);
		guide = null;
	}

	/**
	 * Test of rectangle method, of class Guide.
	 */
	@Test
	public void testRectangle() {
		Visual g = guide.rectangle();
		g.setTarget(scr.getCenter().grow(100));
    g.setActualSize(300, 300);
		assertNotNull(g);
	}

	/**
	 * Test of circle method, of class Guide.
	 */
	@Test
	public void testCircle() {
		System.out.println("circle");
		Visual g = guide.circle();
		g.setTarget(scr.getCenter().grow(100));
    g.setActualSize(300, 300);
		assertNotNull(g);
	}

	/**
	 * Test of text method, of class Guide.
	 */
  @Test
	public void testText() {
		String text = "some words<br>and more<br>and even more";
		Visual g = guide.text(text);
    g.setFont("Palatino", 24);
    g.updateComponent();
    g.setColor(Color.yellow);
    g.setTextColor(Color.blue);
    g.setLocationRelativeToRegion(scr.getCenter().grow(100), Visual.Layout.TOP);
		System.out.println("text: " + g.getStyleString());
		assertNotNull(g);
	}

	/**
	 * Test of flag method, of class Guide.
	 */
	@Test
	public void testFlag() {
		System.out.println("flag");
		String text = "tooltip: some words and more and even more";
		Visual g = guide.flag(text);
    g.setLocationRelativeToRegion(scr.getCenter().grow(100), Visual.Layout.TOP);
    g.setFontSize(12);
    g.setColor(Color.white);
    g.setTextColor(Color.black);
		assertNotNull(g);
	}

	/**
	 * Test of callout method, of class Guide.
	 */
	@Test
	public void testCallout() {
		System.out.println("callout");
		String text = "some words<br>and more<br>and even more";
		Visual g = guide.callout(text);
    g.setLocationRelativeToRegion(scr.getCenter().grow(100), Visual.Layout.LEFT);
    g.setFontSize(16);
    g.setColor(Color.black);
    g.setTextColor(Color.white);
		assertNotNull(g);
	}

	/**
	 * Test of bracket method, of class Guide.
	 */
	@Test
	public void testBracket() {
		System.out.println("bracket");
		Visual g = guide.bracket();
    g.setLocationRelativeToRegion(scr.getCenter().grow(100), Visual.Layout.RIGHT);
    g.setColor(Color.red);
		assertNotNull(g);
	}

	/**
	 * Test of image method, of class Guide.
	 */
	@Test
	public void testImage() {
		System.out.println("image");
		Visual g = guide.image("firefox");
    g.setLocationRelativeToRegion(scr.getCenter().grow(200), Visual.Layout.OVER);
		assertNotNull(g);
	}

	/**
	 * Test of button method, of class Guide.
	 */
	@Ignore
	@Test
	public void testButton() {
		System.out.println("button");
		String name = "";
		Guide instance = new Guide();
		Visual expResult = null;
		Visual result = instance.button(name);
		assertEquals(expResult, result);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

	/**
	 * Test of arrow method, of class Guide.
	 */
	@Ignore
	@Test
	public void testArrow() {
		System.out.println("arrow");
		Object from = null;
		Object to = null;
		Guide instance = new Guide();
		Visual expResult = null;
		Visual result = instance.arrow(from, to);
		assertEquals(expResult, result);
		// TODO review the generated test code and remove the default call to fail.
		fail("The test case is a prototype.");
	}

}
