package ru.rt.fsom.notify;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class NotifyHelper implements Serializable{  
    private static final long serialVersionUID = 1L;
    
    private final String omsId;
    private final String orderId;    
	
    private final AtomicBoolean isWork = new AtomicBoolean(false); //признак что обработчик работает
    
    public NotifyHelper(String omsId, String orderId) {
	this.omsId = omsId;
	this.orderId = orderId;
    }

    public boolean isWork() {	
	return isWork.get();
    }  
	
    public void setRun(){
	isWork.set(true);
    }
    
    public void setStop(){
	isWork.set(false);
    }
    
    public String getOmsId() {
	return omsId;
    }

    public String getOrderId() {
	return orderId;
    }
      
    /* *** *** */

    @Override
    public int hashCode() {
	int hash = 5;
	hash = 61 * hash + Objects.hashCode(this.omsId);
	hash = 61 * hash + Objects.hashCode(this.orderId);
	return hash;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj) {
	    return true;
	}
	if (obj == null) {
	    return false;
	}
	if (getClass() != obj.getClass()) {
	    return false;
	}
	final NotifyHelper other = (NotifyHelper) obj;
	if (this.isWork != other.isWork) {
	    return false;
	}
	if (!Objects.equals(this.omsId, other.omsId)) {
	    return false;
	}
	if (!Objects.equals(this.orderId, other.orderId)) {
	    return false;
	}
	return true;
    }
    
    
}
