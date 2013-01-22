What is it?
===========

It is a GWT addon that lets you run GWT JUnit tests automatically within the
[PhantomJS](http://phantomjs.org/) engine.

When should I use it?
====================

GWT provides [support](https://developers.google.com/web-toolkit/doc/latest/DevGuideTesting)
for running automated JUnit tests on an integrated Java HTML/JS engine called
[HtmlUnit](http://htmlunit.sourceforge.net/), but also on external browsers which are
accessible via Selenium RC or [rmi](https://developers.google.com/web-toolkit/doc/latest/DevGuideTestingRemoteTesting#Remote_Web).

In cases where external browsers aren't practical, usually HtmlUnit is used. Sometimes though,
HtmlUnit will [fail](https://developers.google.com/web-toolkit/doc/latest/DevGuideTestingHtmlUnit) as
it doesn't completely support all HTML5 features yet and in general isn't suited for layout tests.

As an alternative to HtmlUnit, PhantomJS can be used, which is a complete and headless WebKit engine.
The purpose of this project is to provide a JUnit run style to let developers easily integrate
PhantomJS within the test phase of their GWT projects.

How does it work?
=================

Just before running tests, the new run style gets active and launches a local PhantomJS process.
PhantomJS is launched with the filename of a temporarily created Javascript file which consists of
`require('webpage').create().open('%url%', function (){});` where `%url%` is the usual test URL given
by GWT. Once tests are finished and the Maven process exits, the PhantomJS process is killed and
the temporary Javascript file is deleted.

How can I use it?
=================

1. Install [PhantomJS](http://phantomjs.org/download.html) on your machine and put its folder into PATH.
2. Add Maven dependency to your .pom:

		<dependency>
			<groupId>com.github.neothemachine</groupId>
			<artifactId>gwt-phantomjs-junit-runstyle</artifactId>
			<version>1.0.0-SNAPSHOT</version>
			<scope>test</scope>
		</dependency>

3. Add surefire config to your .pom:

		<build>
		 <plugins>
		  ...
		  <plugin>
		    <artifactId>maven-surefire-plugin</artifactId>
		    <version>2.13</version>
		    <configuration>
		      <additionalClasspathElements>
		        <additionalClasspathElement>${project.build.sourceDirectory}</additionalClasspathElement>
		        <additionalClasspathElement>${project.build.testSourceDirectory}</additionalClasspathElement>
		      </additionalClasspathElements>
		      <useManifestOnlyJar>false</useManifestOnlyJar>
		      <forkMode>always</forkMode>
		      <systemProperties>
		        <property>
		          <name>gwt.args</name>
		          <value>-prod -runStyle com.github.neothemachine.gwt.junit.RunStylePhantomJS -out ${project.build.directory}/${project.build.finalName}</value>
		        </property>
		      </systemProperties>
		    </configuration>
		  </plugin>
		 </plugins>
		</plugins>


Known issues
============

- [At the moment](http://jira.codehaus.org/browse/MGWT-351), the gwt-maven-plugin cannot
  be used for testing, as it is 
  [hardcoded](https://github.com/gwt-maven-plugin/gwt-maven-plugin/blob/master/src/main/java/org/codehaus/mojo/gwt/shell/TestMojo.java#L298)
  for the built-in JUnit run styles supported in GWT.
- Only production mode testing is supported, as PhantomJS doesn't support plugins. 