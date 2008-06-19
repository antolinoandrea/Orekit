/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.time;

import org.orekit.time.TTScale;
import org.orekit.time.TimeScale;

import junit.framework.*;

public class TTScaleTest
extends TestCase {

    public TTScaleTest(String name) {
        super(name);
    }

    public void testSymetry() {
        // the loop is around the 1977-01-01 leap second introduction
        double tLeap = 220924815;
        TimeScale scale = TTScale.getInstance();
        assertEquals("TT", scale.toString());
        for (double taiTime = tLeap - 60; taiTime < tLeap + 60; taiTime += 0.3) {
            double dt1 = scale.offsetFromTAI(taiTime);
            double dt2 = scale.offsetToTAI(taiTime + dt1);
            assertEquals( 0.0, dt1 + dt2, 1.0e-10);
        }
    }

    public static Test suite() {
        return new TestSuite(TTScaleTest.class);
    }

}