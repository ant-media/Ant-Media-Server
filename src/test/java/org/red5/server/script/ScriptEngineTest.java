/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2013 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.script;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple script engine tests. Some of the hello world scripts found here:
 * http://www.roesler-ac.de/wolfram/hello.htm
 *
 * @author paul.gregoire
 */
public class ScriptEngineTest {

	private static final Logger log = LoggerFactory.getLogger(ScriptEngineTest.class);

	// ScriptEngine manager
	private static boolean java15;

	private static ScriptEngineManager mgr;
	
	private static List<String> scriptExts = new ArrayList<String>();

	@Before
	public void setUp() throws Exception {
		String javaVersion = System.getProperty("java.version");
		String javaMajorRev = javaVersion.substring(0, 3);
		if (javaVersion != null && javaMajorRev.compareTo("1.5") == 0) {
			//should disable tests
			java15 = true;
		} else {
			java15 = false;
			mgr = new ScriptEngineManager();
		}
		Map<String, ScriptEngineFactory> engineFactories = new HashMap<String, ScriptEngineFactory>(7);
		//List<ScriptEngineFactory> factories = mgr.getEngineFactories(); //jdk6
		//ScriptEngineFactory[] factories = mgr.getEngineFactories(); //jdk5
		for (ScriptEngineFactory factory : mgr.getEngineFactories()) {
			try {
				System.out.println("\n--------------------------------------------------------------");
				String engName = factory.getEngineName();
				String engVersion = factory.getEngineVersion();
				String langName = factory.getLanguageName();
				String langVersion = factory.getLanguageVersion();
				System.out.printf("Script Engine: %s (%s) Language: %s (%s)", engName, engVersion, langName, langVersion);
				engineFactories.put(engName, factory);
				System.out.print("\nEngine Alias(es):");
				for (String name : factory.getNames()) {
					System.out.printf("%s ", name);
				}
				System.out.printf("\nExtension: ");
				for (String name : factory.getExtensions()) {
					System.out.printf("%s ", name);
					scriptExts.add(name);
				}
			} catch (Throwable e) {
				log.error("{}", e);
			}
		}
	}

	// Javascript
	@Test
	public void testJavascriptHelloWorld() {
		if (java15 || !scriptExts.contains("js")) {
			return;
		}
		ScriptEngine jsEngine = null;
		for (ScriptEngineFactory factory : mgr.getEngineFactories()) {
			if (factory.getEngineName().toLowerCase().matches(".*(rhino|javascript|ecma).*")) {
				jsEngine = factory.getScriptEngine();
			}
		}
		if (null == jsEngine) {
			log.error("Javascript is not supported in this build");
		}
		try {
			jsEngine.eval("print('Javascript - Hello, world!\\n')");
		} catch (Throwable ex) {
			System.err.println("Get by name failed for: javascript");
		}
	}

	// Ruby
	@Test
	@Ignore
	public void testRubyHelloWorld() {
		if (java15 || !scriptExts.contains("rb")) {
			return;
		}
		ScriptEngine rbEngine = mgr.getEngineByName("ruby");
		if (rbEngine == null) {
			rbEngine = mgr.getEngineByExtension("rb");
		}
		try {
			rbEngine.eval("puts 'Ruby - Hello, world!'");
		} catch (Exception ex) {
			ex.printStackTrace();
			assertFalse(true);
		}
	}

	// Python
	@Test
	@Ignore
	// Python support seems to not be in tree anymore; aclarke 2008-10-01
	public void testPythonHelloWorld() {
		if (java15 || !scriptExts.contains("py")) {
			return;
		}
		ScriptEngine pyEngine = mgr.getEngineByName("python");
		try {
			pyEngine.eval("print \"Python - Hello, world!\"");
		} catch (Exception ex) {
			//ex.printStackTrace();
			fail("could not start Python");
		}
	}

	// Groovy
	@Test
	public void testGroovyHelloWorld() {
		if (java15 || !scriptExts.contains("groovy")) {
			return;
		}
		ScriptEngine gvyEngine = mgr.getEngineByName("groovy");
		try {
			gvyEngine.eval("println  \"Groovy - Hello, world!\"");
		} catch (Exception ex) {
			//ex.printStackTrace();
			fail("could not start Groovy");
		}
	}

	// Judoscript
	//	@Test
	//	public void testJudoscriptHelloWorld() {
	//		ScriptEngine jdEngine = mgr.getEngineByName("judo");
	//		try {
	//			jdEngine.eval(". \'Judoscript - Hello World\';");
	//		} catch (Exception ex) {
	//			//ex.printStackTrace();
	//			assertFalse(true);
	//		}
	//	}

	// Haskell
	// @Test
	// public void testHaskellHelloWorld()
	// {
	// ScriptEngine hkEngine = mgr.getEngineByName("jaskell");
	// try
	// {
	// StringBuilder sb = new StringBuilder();
	// sb.append("module Hello where ");
	// sb.append("hello::String ");
	// sb.append("hello = 'Haskell - Hello World!'");
	// hkEngine.eval(sb.toString());
	// }
	// catch (Exception ex)
	// {
	// //ex.printStackTrace();
	// assertFalse(true);
	// }
	// }

	// Tcl
	//	@Test
	//	public void testTclHelloWorld() {
	//		ScriptEngine tEngine = mgr.getEngineByName("tcl");
	//		try {
	//			StringBuilder sb = new StringBuilder();
	//			sb.append("#!/usr/local/bin/tclsh\n");
	//			sb.append("puts \"Tcl - Hello World!\"");
	//			tEngine.eval(sb.toString());
	//		} catch (Exception ex) {
	//			//ex.printStackTrace();
	//			assertFalse(true);
	//		}
	//	}

	// Awk
	// @Test
	// public void testAwkHelloWorld()
	// {
	// ScriptEngine aEngine = mgr.getEngineByName("awk");
	// try
	// {
	// StringBuilder sb = new StringBuilder();
	// sb.append("BEGIN { print 'Awk - Hello World!' } END");
	// aEngine.eval(sb.toString());
	// }
	// catch (Exception ex)
	// {
	// //ex.printStackTrace();
	// assertFalse(true);
	// }
	// }

	// E4X
	//	@Test
	//	public void testE4XHelloWorld() {
	//		ScriptEngine eEngine = mgr.getEngineByName("rhino");
	//		try {
	//			//Compilable compiler = (Compilable) eEngine;
	//			//CompiledScript script = compiler.compile("var d = new XML('<d><item>Hello</item><item>World!</item></d>');print(d..item);");
	//			//Namespace ns = eEngine.createNamespace();
	//			//ns.put('d', "new XML('<d><item>Hello</item><item>World!</item></d>');");
	//			//System.out.println("E4X - " + script.eval(ns));
	//			eEngine
	//					.eval("var d = new XML('<d><item>Hello</item><item>World!</item></d>');print('E4X - ' + d..item);");
	//		} catch (Exception ex) {
	//			//ex.printStackTrace();
	//			assertFalse(true);
	//		}
	//	}

	// PHP
	// @Test
	// public void testPHPHelloWorld()
	// {
	// //have to add php lib to java env
	// //java.library.path
	// //System.setProperty("java.library.path", "C:\\PHP;" +
	// System.getProperty("java.library.path"));
	// ScriptEngine pEngine = mgr.getEngineByName("php");
	// try
	// {
	// pEngine.eval("<? echo 'PHP - Hello World'; ?>");
	// }
	// catch (Exception ex)
	// {
	// //ex.printStackTrace();
	// assertFalse(true);
	// }
	// }

	// @Test
	// public void testE4X()
	// {
	// // Javascript
	// ScriptEngine jsEngine = mgr.getEngineByName("rhino");
	// try
	// {
	// System.out.println("Engine: " + jsEngine.getClass().getName());
	// jsEngine.eval(new FileReader("samples/E4X/e4x_example.js"));
	// }
	// catch (Exception ex)
	// {
	// //ex.printStackTrace();
	// assertFalse(true);
	// }
	// }

	//	@Test
	//	public void testJavascriptApplication() {
	//		ScriptEngine jsEngine = mgr.getEngineByName("rhino");
	//		try {
	//			// jsEngine.eval(new FileReader("samples/application.js"));
	//			jsEngine.eval(new FileReader("samples/application2.js"));
	//		} catch (Exception ex) {
	//			//ex.printStackTrace();
	//			assertFalse(true);
	//		}
	//	}
	//
	//	@Test
	//	public void testRubyApplication() {
	//		ScriptEngine rbEngine = mgr.getEngineByName("ruby");
	//		try {
	//			rbEngine.eval(new FileReader("samples/application.rb"));
	//		} catch (Exception ex) {
	//			//ex.printStackTrace();
	//			assertFalse(true);
	//		}
	//	}
	//
	//	@Test
	//	public void testGroovyApplication() {
	//		ScriptEngine gvyEngine = mgr.getEngineByName("groovy");
	//		try {
	//			gvyEngine.eval(new FileReader("samples/application.groovy"));
	//			// gvyEngine.eval("def ap = new Application();println
	//			// ap.toString();");
	//		} catch (Exception ex) {
	//			//ex.printStackTrace();
	//			assertFalse(true);
	//		}
	//	}

}
