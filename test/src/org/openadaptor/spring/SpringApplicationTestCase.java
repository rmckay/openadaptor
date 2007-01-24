/*
 * [[
 * Copyright (C) 2001 - 2006 The Software Conservancy as Trustee. All rights
 * reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Nothing in this notice shall be deemed to grant any rights to
 * trademarks, copyrights, patents, trade secrets or any other intellectual
 * property of the licensor or any contributor except as expressly stated
 * herein. No patent license is granted separate from the Software, for
 * code that you delete from the Software, or for combinations of the
 * Software with other software or hardware.
 * ]]
 */

package org.openadaptor.spring;

import org.openadaptor.spring.SpringApplication;
import org.openadaptor.util.ResourceUtil;

import junit.framework.TestCase;

public class SpringApplicationTestCase extends TestCase {

	public static String result;
	
	public void setUp() {
		result = "";
	}
	
	public void testNoProps() {
		try {
			SpringApplication.runXml(ResourceUtil.getResourcePath(this, "test/src/", "test.xml"), null, "Test");
			fail();
		} catch (RuntimeException e) {
		}
	}
	
	public void testPropsFile() {
		SpringApplication.runXml(ResourceUtil.getResourcePath(this, "test/src/", "test.xml"), 
        ResourceUtil.getResourcePath(this, "test/src/", "test.properties"), "Test");
		assertTrue(result.equals("foo"));
	}
	
	public void testSystemProps() {
		System.setProperty("message", "foobar");
		SpringApplication.runXml(ResourceUtil.getResourcePath(this, "test/src/", "test.xml"), null, "Test");
		assertTrue(result.equals("foobar"));
	}
	
	public void testSystemPropsOverride() {
		System.setProperty("message", "foobar");
		SpringApplication.runXml(ResourceUtil.getResourcePath(this, "test/src/", "test.xml"), ResourceUtil.getResourcePath(this, "test/src/", "test.properties"), "Test");
		assertTrue(result.equals("foo"));
	}
	
	public void testRunner() {
		SpringApplication.runXml(ResourceUtil.getResourcePath(this, "test/src/", "test.xml"), 
        ResourceUtil.getResourcePath(this, "test/src/", "test.properties"), "Runnables");
		assertTrue(result.equals("run1run2") || result.equals("run2run1"));
	}
	
	public static final class Test implements Runnable {

		private String mMessage;

		public Test() {}
		
		public Test(String message) {
			mMessage = message;
		}

		public void run() {
			System.err.println(mMessage);
			result += mMessage;
		}
	}
}