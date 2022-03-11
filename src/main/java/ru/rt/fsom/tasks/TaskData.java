package ru.rt.fsom.tasks;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "TaskData")
public class TaskData implements Serializable{    
    private String taskId;    
    private String taskType;    
    private String neType;    
    private String orderId;
    private String requestId;

    public TaskData(String taskId, String taskType, String neType, String orderId, String requestId) {
	this.taskId = taskId;
	this.taskType = taskType;
	this.neType = neType;
	this.orderId = orderId;
	this.requestId = requestId;
    }
    
    public TaskData() {
    }    
    
    public String getTaskId() {
	return taskId;
    }

    public String getTaskType() {
	return taskType;
    }

    public String getNeType() {
	return neType;
    }

    public String getOrderId() {
	    return orderId;
	}

    public String getRequestId() {
	return requestId;
    }
    
    @XmlElement
    public void setRequestId(String requestId) {
	this.requestId = requestId;
    }
        
    @XmlElement
    public void setTaskId(String taskId) {
	this.taskId = taskId;
    }

    @XmlElement
    public void setTaskType(String taskType) {
	this.taskType = taskType;
    }

    @XmlElement
    public void setNeType(String neType) {
	this.neType = neType;
    }

    @XmlElement
    public void setOrderId(String orderId) {
	this.orderId = orderId;
    }
        
}