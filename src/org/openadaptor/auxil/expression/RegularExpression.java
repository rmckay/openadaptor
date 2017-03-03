/*
 Copyright (C) 2001 - 2010 The Software Conservancy as Trustee. All rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy of
 this software and associated documentation files (the "Software"), to deal in the
 Software without restriction, including without limitation the rights to use, copy,
 modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 and to permit persons to whom the Software is furnished to do so, subject to the
 following conditions:

 The above copyright notice and this permission notice shall be included in all 
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 Nothing in this notice shall be deemed to grant any rights to trademarks, copyrights,
 patents, trade secrets or any other intellectual property of the licensor or any
 contributor except as expressly stated herein. No patent license is granted separate
 from the Software, for code that you delete from the Software, or for combinations
 of the Software with other software or hardware.
*/

package org.openadaptor.auxil.expression;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openadaptor.auxil.simplerecord.ISimpleRecord;
import org.openadaptor.core.exception.RecordException;

/**
 * This class builds upon the Expression class by allowing regular expressions with substitution groups to be applied to
 * the value generated by the Expression class.
 * 
 * Use this class when Expression allows you to select a superset of the data that you want, but you need to select a
 * subset of the data or to change it in some way.
 * 
 * For example the following is extracting a date string from within a value:
 * 
 * <bean class="org.openadaptor.auxil.expression.RegularExpression"> <property name="expression" value="{somefield}"/> <property
 * name="match" value="^.*;date=([0-9]{4})([0-1][0-9])([0-3][0-9])([0-9]{6});.*$"/> <property name="value"
 * value="$1$2$3$4"/> </bean>
 * @deprecated ScriptProcessor or ScriptFilterProcessor may be used in place of Expressions
 * 
 */
public class RegularExpression extends Expression implements IExpression {
  private static final Log log = LogFactory.getLog(RegularExpression.class);

  // INTERNAL STATE:
  protected Matcher matcher = null; // derived from match property

  // bean properties:
  protected String value;

  protected String match;

  protected boolean modifyMatches = true;

  protected boolean replaceAll = false;

  // BEGIN Properties

  /**
   * The new value to set the attribute to.
   * 
   * This can be a substitution group.
   * 
   * @return the new value that the attribute is to be set to.
   */
  public String getValue() {
    return value;
  }

  /**
   * Required: the new value to set the attribute to. <p/> Note that this may make use of capture groups in the
   * <code>match</code>, so it is not a literal expression.
   * 
   * @param value
   *          the new value that the attribute is to be set to.
   */
  public void setValue(String value) {
    this.value = value;
  }

  /**
   * The regular expression pattern to match.
   */
  public String getMatch() {
    return match;
  }

  /**
   * Required: the regular expression pattern to match. <p/> This is not a literal expression (regular expression
   * pattern matching rules will be applied).
   * 
   * @param match
   */
  public void setMatch(String match) {
    // Compile the pattern into a reusable matcher just once for efficiency:
    // reset() it with each value to be tested.
    matcher = Pattern.compile(match).matcher("");

    this.match = match;
  }

  /**
   * Controls the behaviour of the optional <code>match</code> property.
   * 
   * @return whether the optional conditional matching should have normal (or inverted) logic applied.
   */
  public boolean isModifyMatches() {
    return modifyMatches;
  }

  /**
   * Optional: controls whether we try to modify attributes that match or do not match the value specified in the
   * <code>match</code> property. <p/> By default it is <code>true</code> which means modify if attribute value
   * matches. By setting it to <code>false</code> you invert the logic (so it modifies all except those that match).
   * 
   * @param modifyMatches
   */
  public void setModifyMatches(boolean modifyMatches) {
    this.modifyMatches = modifyMatches;
  }

  /**
   * Whether all occurrences of <code>match</code> in the attribute value are to be replaced by <code>value</code>.
   * 
   * @return true if all occurrences are to be replaced; false if just the first one is to be replaced.
   */
  public boolean isReplaceAll() {
    return replaceAll;
  }

  /**
   * Optional: controls whether first or all matches in an attribute value are replaced (defaults to <code>false</code>
   * and so just replaces first occurrence). <p/> It is more computationally efficient to default to just matching the
   * first one. So that is the default chosen, and it should be overridden whenever multiple replacement is required.
   * 
   * @param replaceAll
   */
  public void setReplaceAll(boolean replaceAll) {
    this.replaceAll = replaceAll;
  }

  /**
   * TODO: validate() is not currently called on Expression instances, but I think it should be.
   * 
   * @return array of Exceptions explaining why it fails to validate.
   */
  public Exception[] validate() {
    ArrayList exceptions = new ArrayList();

    if (getValue() == null)
      exceptions.add(new ExpressionException("Required property called \"value\" is missing."));

    if (getMatch() == null)
      exceptions.add(new ExpressionException("Required property called \"match\" is missing."));

    if (!isModifyMatches() && isReplaceAll())
      log.info("Property \"replaceAll\" will be IGNORED because property \"modifyMatches\" is false.");

    if (!isModifyMatches()) {
      if (getValue().indexOf('\\') >= 0 || getValue().indexOf('$') >= 0) {
        log
            .info("Replacement \"value\" contains meta-characters that will be ignored because \"modifyMatches\" is false.");
      }
    }

    return (Exception[]) exceptions.toArray(new Exception[] {});
  }

  // BEGIN Implementation of IExpression
  public Object evaluate(ISimpleRecord record) throws RecordException {
    Object extractedValue = super.evaluate(record);

    if (isModifyMatches()) {
      // Can only match if attribute exists and has a non-null value:
      if (extractedValue != null) {
        String replacement;

        matcher.reset(extractedValue.toString());

        if (matcher.matches()) {
          matcher.reset();

          if (isReplaceAll())
            replacement = matcher.replaceAll(getValue());
          else
            replacement = matcher.replaceFirst(getValue());

          extractedValue = replacement;
        }
      }
      // TODO Need to tidy this up a bit for all of those special cases...
    } else // inverted logic (note that this ignores the replaceAll setting as no explicit match)
    {
      if (extractedValue == null || (!matcher.reset(extractedValue.toString()).matches())) {
        // Attribute does not exist or has null value or does not match,
        // so set it to supplied value.
        extractedValue = getValue();
      }
    }
    // TODO Need to tidy this up a bit for all of those special cases...
    return extractedValue;
  }

  // END Implementation of IExpression

  public RegularExpression() {
    super();
  }

  public String toString() {
    return "{expression=[" + super.toString() + "]; match=[" + match + "]; value=[" + value + "]}";
  }

  /**
   * Convenience method to create an expression from a String. This is not supported for RegularExpression...
   * 
   * @param expressionString
   * @return IExpression which had been generated from the supplied Expression <code>String</code>
   * @throws ExpressionException
   */
  public static IExpression createExpressionFromString(String expressionString) throws ExpressionException {
    throw new ExpressionException("Ignoring attempt to set expressionFromString on RegularExpression");
  }
}
