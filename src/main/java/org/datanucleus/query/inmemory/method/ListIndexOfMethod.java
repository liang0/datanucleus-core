/**********************************************************************
Copyright (c) 2018 Andy Jefferson and others. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Contributors:
    ...
**********************************************************************/
package org.datanucleus.query.inmemory.method;

import java.util.List;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.query.QueryUtils;
import org.datanucleus.query.expression.InvokeExpression;
import org.datanucleus.query.expression.Literal;
import org.datanucleus.query.expression.ParameterExpression;
import org.datanucleus.query.expression.PrimaryExpression;
import org.datanucleus.query.expression.VariableExpression;
import org.datanucleus.query.inmemory.InMemoryExpressionEvaluator;
import org.datanucleus.query.inmemory.InvocationEvaluator;
import org.datanucleus.query.inmemory.VariableNotSetException;
import org.datanucleus.util.Localiser;

/**
 * Evaluator for the method "{listExpr}.indexOf(elemExpr)".
 */
public class ListIndexOfMethod implements InvocationEvaluator
{
    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.memory.InvocationEvaluator#evaluate(org.datanucleus.query.expression.InvokeExpression, org.datanucleus.query.evaluator.memory.InMemoryExpressionEvaluator)
     */
    public Object evaluate(InvokeExpression expr, Object invokedValue, InMemoryExpressionEvaluator eval)
    {
        String method = expr.getOperation();

        if (invokedValue == null)
        {
            return null;
        }
        if (!(invokedValue instanceof List))
        {
            throw new NucleusException(Localiser.msg("021011", method, invokedValue.getClass().getName()));
        }

        Object param = expr.getArguments().get(0);
        Object paramValue = null;
        if (param instanceof Literal)
        {
            paramValue = ((Literal)param).getLiteral();
        }
        else if (param instanceof PrimaryExpression)
        {
            PrimaryExpression primExpr = (PrimaryExpression)param;
            paramValue = eval.getValueForPrimaryExpression(primExpr);
        }
        else if (param instanceof ParameterExpression)
        {
            ParameterExpression paramExpr = (ParameterExpression)param;
            paramValue = QueryUtils.getValueForParameterExpression(eval.getParameterValues(), paramExpr);
        }
        else if (param instanceof VariableExpression)
        {
            VariableExpression varExpr = (VariableExpression)param;
            try
            {
                paramValue = eval.getValueForVariableExpression(varExpr);
            }
            catch (VariableNotSetException vnse)
            {
                // Throw an exception with the possible values of elements
                throw new VariableNotSetException(varExpr, ((List)invokedValue).toArray());
            }
        }
        else
        {
            throw new NucleusException("Dont currently support use of indexOf(" + param.getClass().getName() + ")");
        }

        return ((List)invokedValue).indexOf(paramValue);
    }
}