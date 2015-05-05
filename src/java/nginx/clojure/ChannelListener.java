/**
 *  Copyright (C) Zhang,Yuexiang (xfeep)
 *
 */
package nginx.clojure;

public interface ChannelListener<T> {
	
	public void onClose(T data);
	public void onConnect(long status, T data);
	public void onRead(long status, T data);
	public void onWrite(long status, T data);
	
}
