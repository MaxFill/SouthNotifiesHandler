package ru.rt.fsom;

import ru.rt.fsom.conv.OrderStatusNotifConv;
import com.comptel.mds.sas5.taskengine.rmi_fifo.RMIFifoClient;
import com.comptel.mds.sas5.taskengine.rmi_fifo.RMIResponseObject;
import com.comptel.mds.sas5.taskengine.rmi_fifo.exceptions.ClientNotRegisteredException;
import com.comptel.mds.sas5.taskengine.rmi_fifo.exceptions.InvalidModeException;
import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import ru.rt.fsom.conf.Conf;
import ru.rt.fsom.dict.Params;
import ru.rt.fsom.notify.NotifyPersist;
import ru.rt.oms.Attribute;
import ru.rt.oms.Attributes;
import ru.rt.oms.NotificationOrderItems;
import ru.rt.oms.OrderParties;
import ru.rt.oms.OrderResult;
import ru.rt.oms.OrderStatus;
import ru.rt.oms.OrderStatusNotification;
import ru.rt.oms.Party;
import ru.rt.fsom.tasks.TaskData;
import ru.rt.fsom.tasks.TaskPersist;

/**
 * Клиент RMI FIFO InstantLink
 * @author Maksim.Filatov
 */
@Stateless
public class RmiFifoClient {      
        
    private final static String RMIFIFO_ERR = "RMI_FIFO Error:";
    private final static String REMOTE_EXCEPTION = RMIFIFO_ERR + "RemoteException: there is a communication problem. Most probably something wrong with the physical connection OR TE has been shutdown";
    private final static String CLIENT_NOTREGISTERED_EXCEPTION = RMIFIFO_ERR + "ClientNotRegisteredException: TE does not know us ' must re-register";
    private final static String INVALID_MODE_EXCEPTION = RMIFIFO_ERR + "InvalidModeException: TE is going down... do something with the response at";
    
    public static final BigInteger WRONG_REQUEST = BigInteger.valueOf(-1L);    
    
    @EJB private Conf conf;
    @EJB private TaskPersist taskPersist;
    @EJB private NotifyPersist notifyPersist;
    
    public synchronized void sendNotifToIL(String orderId, String omsId, OrderStatusNotification notification, RMIFifoClient client, ConcurrentHashMap<String, String> sended, ConcurrentHashMap<String, String> noSended) {
	final String logInfo = orderId + " " + omsId;
	TaskData task = taskPersist.loadTask(orderId, omsId, logInfo);
	if (task == null){
	    noSended.put(logInfo, logInfo);
	    //Params.LOGGER.log(Level.INFO, "{0} Отправка нотификации (requestId={1}) в IL не может быть выполнена, т.к. task не найден!", new Object[]{logInfo, notification.getRequestId()});
	    return;
	}	
	RMIResponseObject response = makeRMIResponse(omsId, task, notification);	
	final String taskId = response.requestId + "_" + response.taskId;
	if (sended.containsKey(taskId)){
	    Params.LOGGER.log(Level.INFO, "{0} Отклонена попытка повторной отправки нотификации taskId={1} requestId={2} !", new Object[]{logInfo, taskId, notification.getRequestId()});
	    return;
	}
	sended.put(taskId, taskId);
	noSended.remove(taskId);
	try {	    
	    final String requestId = notification.getRequestId();
	    Params.LOGGER.log(Level.INFO, "{0} Старт отправки нотификации в IL. taskId={1}, requestId={2}", new Object[]{logInfo, taskId, requestId});
	    client.sendResponse(response); //отправка в IL
	    Params.LOGGER.log(Level.INFO, "{0} Нотификация успешно отправлена в IL. taskId={1}, requestId={2}", new Object[]{logInfo, taskId, requestId});
	    int countTaskUpdate = taskPersist.makeTaskAsCompleted(taskId, logInfo);
	    if (countTaskUpdate >0){
		Params.LOGGER.log(Level.INFO, "{0} Обновлено задач = {1}. Записан статус IsComplete = TRUE",  new Object[]{logInfo, countTaskUpdate});
		int countNotifyUpdate = notifyPersist.makeNotifyAsCompleted(notification, taskId, logInfo);
		if (countNotifyUpdate >0){
		    Params.LOGGER.log(Level.INFO, "{0} Обновлено нотификаций = {1}. Записан статус COMPLETED",  new Object[]{logInfo, countNotifyUpdate});
		} else {
		    Params.LOGGER.log(Level.INFO, "{0} Не удалось обновить нотификацию!",  new Object[]{logInfo});
		}
	    } else {
		Params.LOGGER.log(Level.INFO, "{0} Не удалось обновить задачу!",  new Object[]{logInfo});
	    }
	} catch(RemoteException ex){
	    Params.LOGGER.log(Level.SEVERE, REMOTE_EXCEPTION, "");
	    conf.resetRMIFifoClient();
	} catch(ClientNotRegisteredException ex){
	    Params.LOGGER.log(Level.SEVERE, CLIENT_NOTREGISTERED_EXCEPTION, "");
	    conf.resetRMIFifoClient();
	} catch(InvalidModeException ime){
	    Params.LOGGER.log(Level.SEVERE, INVALID_MODE_EXCEPTION, ime.getMessage());	    
	} 
    }

    /* *** privates *** */    
      
    private RMIResponseObject makeRMIResponse(String omsId, TaskData task, OrderStatusNotification notification){	
	//final String logInfo = orderId + " " + omsId;
		
	OrderStatus orderStatus = notification.getOrder();
	checkOrderStatus(orderStatus); //очистка в checkOrderStatus объектов equipment от null элементов	
	
	OrderResult orderResult = notification.getOrderResult();
	
	String smessageId = "";
	String smessage = "";
	if (orderResult != null){
	    smessageId = orderResult.getOrderResultCode() != null ? orderResult.getOrderResultCode() : "";
	    smessage = orderResult.getOrderResultText() != null ? orderResult.getOrderResultText() : "";
	}
	
	List phaseMessages = new ArrayList();
	Map oldParams = new HashMap<>();
	OrderStatusNotifConv conv = new OrderStatusNotifConv();
	Map newParams = conv.orderStatusNotifToMap(notification);
	
	newParams.put(Params.ORDER_OMS_ID, omsId);
	newParams.put(Params.ORDER_STATE, orderStatus.getOrderState());	

	String neType = task.getNeType();
	String taskType = task.getTaskType();
	String taskNumber = task.getTaskId();
	String[] params = taskNumber.split("_");
	long reqId = Long.valueOf(params[0]);
	int taskId = Integer.valueOf(params[1]);
	int status = 0; //the task is always assigned a status READY		
	//Params.LOGGER.log(Level.INFO, "{0} Подготовлен RMIResponseObject: reqId={1}, taskId={2}, neType={3}, taskType={4}, smessageId={5}, smessage={6}", new Object[]{logInfo, reqId, taskId, neType, taskType, smessageId, smessage});	
	return new RMIResponseObject(reqId, taskId, neType, taskType, status, newParams, oldParams, phaseMessages, smessageId, smessage);
    }
    
    private void checkOrderStatus(OrderStatus orderStatus){
	checkAttributes(orderStatus.getOrderAttributes());
	checkNotificationOrderItems(orderStatus.getOrderItems());
	checkParties(orderStatus.getOrderParties());
    }
    
    private void checkNotificationOrderItems(NotificationOrderItems noi){
	if (noi == null) return;
	noi.getOrderItem().forEach(oi->checkParties(oi.getOrderItemParties()));
    }
    
    private void checkParties(OrderParties orderParties){
	if (orderParties == null) return;
	orderParties.getOrderPartyOrOrderAttachment().forEach(ob->{
	    if (ob instanceof Party){
		Party party = (Party) ob;		
		checkAttributes(party.getPartyAttributes());
	    }
	});	
    }
    
    private void checkAttributes(Attributes attributes){
	if (attributes == null) return;
	attributes.getAttribute().forEach(atr->checkAttribute(atr));
    }
    
    private void checkAttribute(Attribute attribute){
	if (attribute == null) return;
	if ("equipmentList".equalsIgnoreCase(attribute.getName())){
	    List<Object> clearEquip = clearNullEquipment(attribute.getContent());
	    attribute.getContent().clear();
	    attribute.getContent().addAll(clearEquip);
	}
    }
    
    private List<Object> clearNullEquipment(List<Object> obj){
	obj.removeIf(Objects::isNull);
	return obj.stream().filter(o -> {
	    if (o instanceof String){		
		String equipment = ((String) o).trim();		
		if (equipment.contains("[equipment: null]")){		    
		    return false;
		}
	    }
	    return true;
	})
	.collect(Collectors.toList());
    }
}