/* Copyright 2002-2011 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.propagation.precomputed;

import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math.ode.nonstiff.DormandPrince853Integrator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.numerical.PartialDerivativesEquations;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;


public class IntegratedEphemerisTest {

    @Test
    public void testNormalKeplerIntegration() throws OrekitException {

        // Keplerian propagator definition
        KeplerianPropagator keplerEx = new KeplerianPropagator(initialOrbit);

        // Integrated ephemeris

        // Propagation
        AbsoluteDate finalDate = initialOrbit.getDate().shiftedBy(Constants.JULIAN_DAY);
        numericalPropagator.setEphemerisMode();
        numericalPropagator.setInitialState(new SpacecraftState(initialOrbit));
        numericalPropagator.propagate(finalDate);
        Assert.assertTrue(numericalPropagator.getCalls() < 3200);
        BoundedPropagator ephemeris = numericalPropagator.getGeneratedEphemeris();

        // tests
        for (int i = 1; i <= Constants.JULIAN_DAY; i++) {
            AbsoluteDate intermediateDate = initialOrbit.getDate().shiftedBy(i);
            SpacecraftState keplerIntermediateOrbit = keplerEx.propagate(intermediateDate);
            SpacecraftState numericIntermediateOrbit = ephemeris.propagate(intermediateDate);
            Vector3D kepPosition = keplerIntermediateOrbit.getPVCoordinates().getPosition();
            Vector3D numPosition = numericIntermediateOrbit.getPVCoordinates().getPosition();
            Assert.assertEquals(0, kepPosition.subtract(numPosition).getNorm(), 0.06);
        }

        // test inv
        AbsoluteDate intermediateDate = initialOrbit.getDate().shiftedBy(41589);
        SpacecraftState keplerIntermediateOrbit = keplerEx.propagate(intermediateDate);
        SpacecraftState state = keplerEx.propagate(finalDate);
        numericalPropagator.setInitialState(state);
        numericalPropagator.setEphemerisMode();
        numericalPropagator.propagate(initialOrbit.getDate());
        BoundedPropagator invEphemeris = numericalPropagator.getGeneratedEphemeris();
        SpacecraftState numericIntermediateOrbit = invEphemeris.propagate(intermediateDate);
        Vector3D kepPosition = keplerIntermediateOrbit.getPVCoordinates().getPosition();
        Vector3D numPosition = numericIntermediateOrbit.getPVCoordinates().getPosition();
        Assert.assertEquals(0, kepPosition.subtract(numPosition).getNorm(), 10e-2);

    }

    @Test
    public void testPartialDerivativesIssue16() throws OrekitException {

        final String eqName = "derivatives";
        numericalPropagator.setEphemerisMode();
        numericalPropagator.setPropagationParametersType(NumericalPropagator.PropagationParametersType.CARTESIAN);
        final PartialDerivativesEquations derivatives =
            new PartialDerivativesEquations(eqName, numericalPropagator);
        derivatives.setInitialJacobians(6, 0);
        numericalPropagator.setInitialState(new SpacecraftState(initialOrbit));
        numericalPropagator.propagate(initialOrbit.getDate().shiftedBy(3600.0));
        BoundedPropagator ephemeris = numericalPropagator.getGeneratedEphemeris();
        ephemeris.setMasterMode(new OrekitStepHandler() {
            
            private static final long serialVersionUID = -5825020344303732268L;

            public void reset() {
            }
            
            public boolean requiresDenseOutput() {
                return false;
            }
            
            public void handleStep(OrekitStepInterpolator interpolator, boolean isLast)
            throws PropagationException {
                try {
                    double[] z = interpolator.getInterpolatedAdditionalState(eqName);
                    Assert.assertEquals(36, z.length);
                } catch (OrekitException oe) {
                    throw new PropagationException(oe);
                }
            }

        });

        ephemeris.propagate(initialOrbit.getDate().shiftedBy(1800.0));

    }

    @Before
    public void setUp() {
        // Definition of initial conditions with position and velocity
        Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        double mu = 3.9860047e14;

        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        initialOrbit =
            new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                 FramesFactory.getEME2000(), initDate, mu);

        // Numerical propagator definition
        double[] absTolerance = {
            0.0001, 1.0e-11, 1.0e-11, 1.0e-8, 1.0e-8, 1.0e-8, 0.001
        };
        double[] relTolerance = {
            1.0e-8, 1.0e-8, 1.0e-8, 1.0e-9, 1.0e-9, 1.0e-9, 1.0e-7
        };
        AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 500, absTolerance, relTolerance);
        integrator.setInitialStepSize(100);
        numericalPropagator = new NumericalPropagator(integrator);

    }

    private Orbit initialOrbit;
    private NumericalPropagator numericalPropagator;

}
