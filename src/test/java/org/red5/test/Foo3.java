package org.red5.test;

import org.red5.io.amf3.IDataInput;
import org.red5.io.amf3.IDataOutput;
import org.red5.io.amf3.IExternalizable;

/**
 * Used for testing AMF3 Vectors
 * 
 * @author Paul
 */
public class Foo3 implements IExternalizable {

	private int foo;

	public void setFoo3(int foo) {
		this.foo = foo;
	}

	public int getFoo() {
		return foo;
	}

	public void readExternal(IDataInput input) {
		this.foo = input.readInt();
	}

	public void writeExternal(IDataOutput output) {
		output.writeInt(foo);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Foo3 [foo=" + foo + "]";
	}

}