/**
 * Code that executes tests based on the test model.
 *
 * <p>
 * {@link Executor}s are JUnit tests and
 * {@link TestDescriptor#build(ApplicationContainer, TestSuite)}
 * will put them in the right order in {@link TestSuite},
 * then calling {@link TestRunner#run(Test)} will execute those tests.
 */
package com.sun.xml.ws.test.exec;

import com.sun.xml.ws.test.container.ApplicationContainer;
import com.sun.xml.ws.test.model.TestDescriptor;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;