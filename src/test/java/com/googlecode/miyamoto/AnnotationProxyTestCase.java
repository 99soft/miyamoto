/*
 *    Copyright 2010 The Miyamoto Team
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.googlecode.miyamoto;

import java.util.Arrays;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * 
 *
 * @version $Id$
 */
@TestAnnotation(string="supercalifragilistichespiralidoso", integer=4)
public final class AnnotationProxyTestCase {

    private AnnotationProxy<TestAnnotation> proxy;

    private TestAnnotation current;

    private TestAnnotation expected;

    @BeforeSuite
    public void setUp() {
        this.proxy = AnnotationProxy.newProxy(TestAnnotation.class);
        this.proxy.setProperty("integer", new int[] { 4 });

        this.current = proxy.getProxedAnnotation();
        this.expected = AnnotationProxyTestCase.class.getAnnotation(TestAnnotation.class);
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void illegalSetProperty() {
        this.proxy.setProperty("integer", false);
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void missingSetProperty() {
        this.proxy.setProperty("doesnotexist", false);
    }

    @Test
    public void verifySetProperty() {
        assert Arrays.equals(this.expected.integer(), this.current.integer());
    }

    @Test
    public void verifyEquals() {
        assert this.current.equals(this.current);
        // assert this.current.equals(this.expected);
        assert this.expected.equals(this.current);
    }

    @Test
    public void verifyHashCode() {
        assert this.expected.hashCode() == this.current.hashCode();
    }

    @Test
    public void verifyToString() {
        assert this.expected.toString().equals(this.current.toString());
    }

}
