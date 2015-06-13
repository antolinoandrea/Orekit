/* Copyright 2002-2015 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.estimation.measurements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.Parameter;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** Abstract class handling measurements boilerplate.
 * @author Luc Maisonobe
 * @since 7.1
 */
public abstract class AbstractMeasurement implements Measurement {

    /** List of the supported parameters. */
    private SortedSet<Parameter> supportedParameters;

    /** Date of the measurement. */
    private final AbsoluteDate date;

    /** Observed value. */
    private final double[] observed;

    /** Theoretical standard deviation. */
    private final double[] sigma;

    /** Weight. */
    private final double[] weight;

    /** Modifiers that apply to the measurement.*/
    private final List<EvaluationModifier> modifiers;

    /** Enabling status. */
    private boolean enabled;

    /** Simple constructor for mono-dimensional measurements.
     * <p>
     * At construction, a measurement is enabled.
     * </p>
     * @param date date of the measurement
     * @param observed observed value
     * @param sigma theoretical standard deviation
     */
    public AbstractMeasurement(final AbsoluteDate date, final double observed, final double sigma) {
        this.supportedParameters = new TreeSet<Parameter>();
        this.date       = date;
        this.observed   = new double[] {
            observed
        };
        this.sigma      = new double[] {
            sigma
        };
        this.weight     = new double[] {
            1.0
        };
        this.modifiers = new ArrayList<EvaluationModifier>();
        setEnabled(true);
    }

    /** Simple constructor, for multi-dimensional measurements.
     * <p>
     * At construction, a measurement is enabled.
     * </p>
     * @param date date of the measurement
     * @param observed observed value
     * @param sigma theoretical standard deviation
     */
    public AbstractMeasurement(final AbsoluteDate date, final double[] observed, final double[] sigma) {
        this.supportedParameters = new TreeSet<Parameter>();
        this.date       = date;
        this.observed   = observed.clone();
        this.sigma      = sigma.clone();
        this.weight     = new double[observed.length];
        Arrays.fill(weight, 1.0);
        this.modifiers = new ArrayList<EvaluationModifier>();
        setEnabled(true);
    }

    /** Add a supported parameter.
     * @param parameter supported parameter
     * @exception OrekitException if a parameter with the same name already exists
     */
    protected void addSupportedParameter(final Parameter parameter)
        throws OrekitException {
        if (supportedParameters.contains(parameter)) {
            // a parameter with this name already exists in the set,
            // check if it is really the same parameter or a duplicated name
            if (supportedParameters.tailSet(parameter).first() != parameter) {
                // we have two different parameters sharing the same name
                throw new OrekitException(OrekitMessages.DUPLICATED_PARAMETER_NAME,
                                          parameter.getName());
            }
        } else {
            supportedParameters.add(parameter);
        }
    }

    /** {@inheritDoc} */
    public SortedSet<Parameter> getSupportedParameters() {
        return Collections.unmodifiableSortedSet(supportedParameters);
    }

    /** {@inheritDoc} */
    @Override
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /** {@inheritDoc} */
    @Override
    public int getDimension() {
        return observed.length;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getTheoreticalStandardDeviation() {
        return sigma.clone();
    }

    /** {@inheritDoc} */
    @Override
    public double[] getWeight() {
        return weight.clone();
    }

    /** {@inheritDoc} */
    @Override
    public void setWeight(final double ... weight) {
        System.arraycopy(weight, 0, this.weight, 0, getDimension());
    }

    /** Compute the theoretical value.
     * <p>
     * The theoretical value does not have <em>any</em> modifiers applied.
     * </p>
     * @param state orbital state at measurement date
     * @return theoretical value
     * @exception OrekitException if value cannot be computed
     * @see #evaluate(SpacecraftState, SortedSet)
     */
    protected abstract Evaluation theoreticalEvaluation(final SpacecraftState state)
        throws OrekitException;

    /** {@inheritDoc} */
    @Override
    public Evaluation evaluate(final SpacecraftState state)
        throws OrekitException {

        // compute the theoretical value
        final Evaluation evaluation = theoreticalEvaluation(state);

        // apply the modifiers
        for (final EvaluationModifier modifier : modifiers) {
            modifier.modify(evaluation);
        }

        return evaluation;

    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return date;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getObservedValue() {
        return observed;
    }

    /** {@inheritDoc} */
    @Override
    public void addModifier(final EvaluationModifier modifier) {
        modifiers.add(modifier);
    }

    /** {@inheritDoc} */
    @Override
    public List<EvaluationModifier> getModifiers() {
        return Collections.unmodifiableList(modifiers);
    }

}
