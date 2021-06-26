/*
 * Copyright  2003-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.onedev.commons.utils.command;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public abstract class LineConsumer extends OutputStream {
	
    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    
    private boolean reset = false;
    
    private String encoding;
    
    public LineConsumer(String encoding) {
    	this.encoding = encoding;
    }
    
    public LineConsumer() {
    	this(StandardCharsets.UTF_8.name());
    }
    
    public String getEncoding() {
    	return encoding;
    }
    
    @Override
	public void write(int b) throws IOException {
		byte c = (byte) b;
		if (c == '\n') {
			processBuffer();
			reset = false;
		} else if (c == '\r') {
			reset = true;
		} else {
			if (reset) {
				buffer.reset();
				reset = false;
			}
			buffer.write(b);
		}
    }

    protected void processBuffer() {
		try {
			consume(encoding!=null?buffer.toString(encoding):buffer.toString());
			buffer.reset();
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
    }

	public abstract void consume(String line);
	
	@Override
	public void flush() throws IOException {
        if (buffer.size() > 0) 
            processBuffer();
        super.flush();
	}

    @Override
	public void close() throws IOException {
    	flush();
    	super.close();
    }    
    
}
