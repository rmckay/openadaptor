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

package org.oa3.node;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oa3.IMessageProcessor;
import org.oa3.LifecycleComponent;
import org.oa3.Message;
import org.oa3.MessageException;
import org.oa3.Response;
import org.oa3.processor.IDataProcessor;

public class Node extends LifecycleComponent implements IMessageProcessor, NodeMBean {

	private static final Log log = LogFactory.getLog(Node.class);
	
	private static final IDataProcessor NULL_PROCESSOR = new IDataProcessor.NullProcessor();

	private IMessageProcessor next;

	private IDataProcessor processor;

	public Node(final String id, final IDataProcessor processor, final IMessageProcessor next) {
		super(id);
		this.processor = processor != null ? processor : NULL_PROCESSOR;
		this.next = next;
	}

	public Node(final String id, final IDataProcessor processor) {
		this(id, processor, null);
	}

	public Node(String id) {
		this(id, null, null);
	}
	
	public Node() {
		this(null, null, null);
	}
	
	public void setNext(IMessageProcessor next) {
		this.next = next;
	}

	public void setProcessor(IDataProcessor processor) {
		this.processor = processor;
	}

	public Response process(Message msg) {
		
		Response response = new Response();
		
		Object[] inputs = msg.getData();
		
		// call processor for each element in the batch
		// collate discarded data and exceptions
		
		for (int i = 0; i < inputs.length; i++) {
			try {
				Object[] outputs = processor.process(inputs[i]);
				if (outputs != null) {
					for (int j = 0; j < outputs.length; j++) {
						response.addOutput(outputs[j]);
					}
				} else {
					response.addDiscardedInput(inputs[i]);
				}
			} catch (Exception e) {
				response.addException(new MessageException(inputs[i], e));
			}
		}
			
		if (log.isTraceEnabled()) {
			log.trace(getId() + " processed " + inputs.length + " input(s) = [" + response.toString() + "]");
		}
		
		// if node is chained and there are no exceptions then
		// delegate to next IMessageProcessor in the chain
		
		if (next != null) {
			if (!response.containsExceptions()) {
				response = next.process(new Message(response.getCollatedOutput(), this));
			} else {
				MessageException[] exceptions = response.getCollatedExceptions();
				throw new RuntimeException(exceptions[0].getException());
			}
		}
		
		return response;
	}

	public void validate(List exceptions) {
		super.validate(exceptions);
		processor.validate(exceptions);
	}

}
