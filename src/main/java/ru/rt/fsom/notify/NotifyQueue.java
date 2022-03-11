package ru.rt.fsom.notify;

import java.util.Objects;

public class NotifyQueue {
    private final String omsId;
    private final String orderId;

    public NotifyQueue(String orderId, String omsId) {
	this.omsId = omsId;
	this.orderId = orderId;
    }

    public String getOmsId() {
	return omsId;
    }

    public String getOrderId() {
	return orderId;
    }

    @Override
    public int hashCode() {
	int hash = 5;
	hash = 37 * hash + Objects.hashCode(this.omsId);
	hash = 37 * hash + Objects.hashCode(this.orderId);
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
	final NotifyQueue other = (NotifyQueue) obj;
	if (!Objects.equals(this.omsId, other.omsId)) {
	    return false;
	}
	if (!Objects.equals(this.orderId, other.orderId)) {
	    return false;
	}
	return true;
    }
    
    
}
