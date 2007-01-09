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
package org.oa3.spring.processor.factory;

import org.oa3.auxil.expression.ExpressionException;
import org.oa3.auxil.processor.simplerecord.AttributeSetProcessor;
import org.oa3.auxil.processor.simplerecord.ConditionProcessor;
import org.oa3.auxil.simplerecord.ISimpleRecordAccessor;
import org.oa3.core.Component;
import org.oa3.core.IDataProcessor;
import org.oa3.core.exception.ComponentException;
import org.springframework.beans.factory.FactoryBean;
/*
 * File: $Header: $
 * Rev:  $Revision: $
 * Created Jan 3, 2007 by oa3 Core Team
 */

/**
 * Spring FactoryBean designed to simplify configuring a CondtionProcessor to conditionally set a named attribute.
 * The Factory bean uses expression strings to configure the if/then/else processors. An ISimpleRecordAccessor can be
 * configured which will apply to all expressions.
 */
public class ConditionProcessorFactoryBean extends Component implements FactoryBean {

  /**
   * Processor class that is primary element of group generated by this bean.
   */
  protected static final String CLASSNAME = "org.oa3.auxil.processor.simplerecord.ConditionProcessor";

  /**
   * Cache the generated ConditionProcessor so that the same one is always returned.
   */
  protected IDataProcessor cachedProcessor = null;

  protected String ifExpression = null;
  protected String thenExpression = null;
  protected String elseExpression = null;

  protected String attributeName;

  protected ISimpleRecordAccessor simpleRecordAccessor = null;


  /**
   * Get the generated Processor creating it if necessary.
   *
   * @return created Processor.
   * @throws Exception
   */
  public Object getObject() throws Exception {
    if (cachedProcessor == null) {
      cachedProcessor = createObject();
    }
    return cachedProcessor;
  }

  public Class getObjectType() {
    return getObjectType(CLASSNAME);
  }

  /**
   * Ensure we always return the same singleton created object.
   * @return true
   */
  public boolean isSingleton() {
    return true;
  }

// Factory support methods

  /**
   * generate class from typename.
   *
   * @param typeName String representing type.
   * @return Class of created object.
   */
  protected Class getObjectType(String typeName) {
    try {
      return Class.forName(typeName);
    } catch (ClassNotFoundException e) {
      throw new ComponentException("Cannot find Class [" + typeName + "] in classpath.", this);
    }
  }

  /**
   * Instantiate the ConditionProcessor. This is the Object constructed by this factory. This Object
   * will be cached by the implementation of getObject()
   *
   * @return ProcessorGroup The instantiated object.
   */
  protected IDataProcessor createObject() throws IllegalAccessException, InstantiationException,
      ExpressionException {
    ConditionProcessor conditionProcessor = (ConditionProcessor) getObjectType().newInstance();

    conditionProcessor.setId(getId());
    if (getIfExpression() != null) {
      conditionProcessor.setifExpressionString(getIfExpression());
    }
    conditionProcessor.setSimpleRecordAccessor(getSimpleRecordAccessor());
    if (thenExpression != null) {
      AttributeSetProcessor thenProcessor = new AttributeSetProcessor();
      thenProcessor.setAttributeName(getAttributeName());
      thenProcessor.setExpressionString(getThenExpression());
      thenProcessor.setSimpleRecordAccessor(getSimpleRecordAccessor());
      conditionProcessor.setThenProcessor(thenProcessor);
    }
    if (elseExpression != null) {
      AttributeSetProcessor elseProcessor = new AttributeSetProcessor();
      elseProcessor.setAttributeName(getAttributeName());
      elseProcessor.setExpressionString(getElseExpression());
      elseProcessor.setSimpleRecordAccessor(getSimpleRecordAccessor());
      conditionProcessor.setElseProcessor(elseProcessor);
    }
    return conditionProcessor;
  }

  // End Factory support methods

  // Bean Accessors

  /**
   * The expression representing thr conditional if expression.
   * @return
   */
  public String getIfExpression() {
    return ifExpression;
  }

  public void setIfExpression(String ifExpression) {
    this.ifExpression = ifExpression;
  }

  public String getThenExpression() {
    return thenExpression;
  }

  public void setThenExpression(String thenExpression) {
    this.thenExpression = thenExpression;
  }

  public String getElseExpression() {
    return elseExpression;
  }

  public void setElseExpression(String elseExpression) {
    this.elseExpression = elseExpression;
  }

  public String getAttributeName() {
    return attributeName;
  }

  public void setAttributeName(String attributeName) {
    this.attributeName = attributeName;
  }

  public ISimpleRecordAccessor getSimpleRecordAccessor() {
    return simpleRecordAccessor;
  }

  public void setSimpleRecordAccessor(ISimpleRecordAccessor simpleRecordAccessor) {
    this.simpleRecordAccessor = simpleRecordAccessor;
  }

  // End Bean Accessors
}
