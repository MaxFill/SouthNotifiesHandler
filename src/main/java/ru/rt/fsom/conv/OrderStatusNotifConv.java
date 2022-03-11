package ru.rt.fsom.conv;

import ru.rt.fsom.dict.Params;
import ru.rt.oms.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.xerces.dom.ElementNSImpl;
import org.apache.xerces.dom.TextImpl;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ru.rt.fsom.utils.Utils;

/**
 * Конвертер данных OrderStatusNotification в список параметров
 * @author Maksim.Filatov
 */
public class OrderStatusNotifConv {

    public OrderStatusNotifConv() {
    }
	
    public ConcurrentHashMap<String, String> orderStatusNotifToMap(OrderStatusNotification notification) {
        ConcurrentHashMap<String, String> argMap = new ConcurrentHashMap();
        setArg(argMap, Params.ORIGINATOR, notification.getOriginator());
        setArg(argMap, Params.RECEIVER, notification.getReceiver());
        setArg(argMap, Params.REQUEST_ID, notification.getRequestId());
        OrderStatus orderStatus = notification.getOrder();
        setArg(argMap, Params.ORDER_OMS_ID, orderStatus.getOrderOMSId());
        setArg(argMap, Params.ORDER_ID, orderStatus.getOrderId());
	setArg(argMap, Params.ORDER_STATE, orderStatus.getOrderState());
	loadOrderResult(argMap, notification.getOrderResult());
        setArgDate(argMap, Params.ORDER + "_" + Params.COMPLETION_DATE, orderStatus.getOrderCompletionDate());
	setArgDate(argMap, Params.ORDER + "_" + Params.EXPECTED_COMPLETION_DATE, orderStatus.getOrderExpectedCompletionDate());
	if (orderStatus.getOrderStartDate() != null){
	    setArgDate(argMap, Params.ORDER + "_" + Params.START_DATE, orderStatus.getOrderStartDate());
	}

	CompletableFuture future0 = CompletableFuture.runAsync(()->loadAttributes(argMap, Params.ORDER, orderStatus.getOrderAttributes()));
        CompletableFuture future1 = CompletableFuture.runAsync(()->loadComments(argMap, Params.ORDER, orderStatus.getOrderComments()));
        CompletableFuture future2 = CompletableFuture.runAsync(()->loadOrderItems(argMap, orderStatus.getOrderItems()));
        CompletableFuture future3 = CompletableFuture.runAsync(()->loadOrderNotifications(argMap, Params.ORDER, orderStatus.getOrderNotifications()));
	CompletableFuture future4 = CompletableFuture.runAsync(()->loadOrderParties(argMap, Params.ORDER, orderStatus.getOrderParties()));

	CompletableFuture completableFuture = CompletableFuture.allOf(future0, future1, future2, future3, future4);
	try {
	    completableFuture.get();
	} catch (InterruptedException | ExecutionException ex) {
	    Params.LOGGER.log(Level.SEVERE, null, ex);
	}

        return argMap;
    }

    /* *** privates *** */

    private void loadOrderResult(ConcurrentHashMap<String, String> argMap, OrderResult result){
	if (result == null) return;
	setArg(argMap, Params.ORDER_RESULT_CODE, result.getOrderResultCode());
	setArg(argMap, Params.ORDER_RESULT_TEXT, result.getOrderResultText());
    }
    
    private void loadOrderNotifications(ConcurrentHashMap<String, String> argMap, final String prefix, OrderNotifications notifications) {
        if (notifications == null) return;
        List<OrderNotification> items = notifications.getOrderNotification();
	getListSize(items).ifPresent(listSize -> {
	    final String key = prefix + "_" + Params.NOTIFICATIONS + "_";
	    setArg(argMap, key + Params.LISTSIZE, String.valueOf(listSize));
	    IntStream.range(0, listSize)
		.forEach(index -> {
		    final String itemKey = key + (index + 1);
		    setArg(argMap, itemKey + "_" + Params.STATUS, items.get(index).getNotificationStatus());
		    setArg(argMap, itemKey + "_" + Params.TEXT, items.get(index).getNotificationText());
		    getStringDate(items.get(index).getNotificationTimestamp())
			.ifPresent(value-> setArg(argMap, itemKey + "_" + Params.TIMES_TAMP, value));
		    loadAttributes(argMap, itemKey, items.get(index).getNotificationAttributes());
		});
	});
    }

    private void loadOrderItems(ConcurrentHashMap<String, String> argMap, NotificationOrderItems orderItems) {
        if (orderItems == null) return;
        List<NotificationOrderItem> items = orderItems.getOrderItem();        
	getListSize(items).ifPresent(listSize->{	
	    final String key = Params.ORDER_ITEM + "_";
	    setArg(argMap, key + Params.LISTSIZE, String.valueOf(listSize));
	    IntStream.range(0, listSize)
		.forEach(index -> {		
		    final String itemKey = key + (index + 1);
		    NotificationOrderItem item = items.get(index);
		    setArg(argMap, itemKey + "_" + Params.ID, item.getOrderItemId());
		    setArg(argMap, itemKey + "_" + Params.INSTANCE_ID, item.getOrderItemInstanceId());
		    getOptString(item.getOrderItemState()).ifPresent(value->setArg(argMap, itemKey + "_" + Params.STATE, value));
		    getOptString(item.getOrderItemAction()).ifPresent(value->setArg(argMap, itemKey + "_" + Params.ACTION, value));
		    if (item.getOrderItemResult() != null){
			setArg(argMap, itemKey + "_" + Params.RESULT_CODE, item.getOrderItemResult().getOrderItemResultCode());
			setArg(argMap, itemKey + "_" + Params.RESULT_TEXT, item.getOrderItemResult().getOrderItemResultText());
		    }
		    setArg(argMap, itemKey + "_" + Params.APPOINTMENT_ID, item.getOrderItemAppointmentId());
		    loadOrderParties(argMap, itemKey, item.getOrderItemParties());
		    loadInheritableAttributes(argMap, itemKey, item.getOrderItemAttributes());
		});
	});
    }
    
    private void loadInheritableAttributes(ConcurrentHashMap<String, String> argMap, final String prefix, InheritableAttributes attributes) {
        if (attributes == null) return;
        List<InheritableAttribute> items = attributes.getAttribute();
	getListSize(items).ifPresent(listSize->{
	    final String key = prefix + "_" + Params.ATTR + "_"; 
	    setArg(argMap, key + Params.LISTSIZE, String.valueOf(listSize));
	    IntStream.range(0, listSize)
		.forEach(index -> {
		    InheritableAttribute item = items.get(index);
		    final String itemKey = key + String.valueOf(index + 1) + "_";
		    setArg(argMap, itemKey + Params.NAME, item.getName());
		    if (item.getStatus() != null){
			setArg(argMap, itemKey + Params.STATUS, String.valueOf(item.getStatus().value()));
		    }
		    setArg(argMap, itemKey + Params.ISUPDATE, String.valueOf(item.isIsUpdated()));
		    setArg(argMap, itemKey + Params.ISCHANGED, String.valueOf(item.isIsChanged()));
		    setArg(argMap, itemKey + Params.ISINHERITABLE, item.getIsInheritable());
		    setArg(argMap, itemKey + Params.VALUE, String.valueOf(item.getContent()
			    .stream()
			    .filter(Objects::nonNull)
			    .map(Object::toString)
			    .collect(Collectors.joining(", "))));
		    if (item.getRestriction() != null){
			setArg(argMap, itemKey + Params.RESTRICTION, items.get(index).getRestriction().value());
		    }
		});
	    });
    }

    private void loadOrderParties(ConcurrentHashMap<String, String> argMap, final String prefix, OrderParties orderParties) {
        if (orderParties == null) return;
	AtomicInteger partyCount = new AtomicInteger();
	AtomicInteger attacheCount = new AtomicInteger();
        final String partyKey = prefix + "_" + Params.PARTY;	
	orderParties.getOrderPartyOrOrderAttachment()
	    .stream()
	    .forEach(item -> {
		if(item instanceof Party){
		    Party orderParty = (Party) item;
		    final String itemKey = partyKey + "_" + partyCount.incrementAndGet();
		    setArg(argMap, itemKey + "_" + Params.ROLE, String.valueOf(orderParty.getPartyRole()));
		    setArg(argMap, itemKey + "_" + Params.ID, orderParty.getPartyId());
		    setArg(argMap, itemKey + "_" + Params.NAME, orderParty.getPartyName());
		    loadAttributes(argMap, itemKey, orderParty.getPartyAttributes());
		} else
		    if(item instanceof Attachment) {
			final String itemKey = partyKey + "_" + Params.ATTACHMENT + "_" + attacheCount.incrementAndGet();
			Attachment attachment = (Attachment) item;
			setArg(argMap, itemKey + "_" + Params.TYPE, String.valueOf(attachment.getAttachmentType()) );
			setArg(argMap, itemKey + "_" + Params.CREATIONDATE, String.valueOf(attachment.getCreationDate()));
			setArg(argMap, itemKey + "_" + Params.AUTHOR, attachment.getAuthor());
			setArg(argMap, itemKey + "_" + Params.URL, attachment.getURL());
			setArg(argMap, itemKey + "_" + Params.HEADER, attachment.getHeader());
			setArg(argMap, itemKey + "_" + Params.FILENAME, attachment.getFileName());
			setArg(argMap, itemKey + "_" + Params.FILEEXTENSION, attachment.getFileExtension());
		    }
	    });
	if (partyCount.get() > 0){
	    setArg(argMap, partyKey + "_" + Params.LISTSIZE, String.valueOf(partyCount.get()));
	}
	if (attacheCount.get() >0){
	    setArg(argMap, partyKey + "_" + Params.ATTACHMENT + "_" + Params.LISTSIZE, String.valueOf(attacheCount.get()));
	}
    }

    private void loadComments(ConcurrentHashMap<String, String> argList, final String prefix, Comments comments) {
        if (comments == null) return;
        List<Object> items = comments.getAny();
        getListSize(items).ifPresent(listSize -> {
	    final String key = prefix + "_" + Params.COMMENT + "_";
	    setArg(argList, key + Params.LISTSIZE, String.valueOf(listSize));
	    //Params.LOGGER.log(Level.INFO, "Start loadComments listSize= {0}", listSize);
	    IntStream.range(0, listSize)
		.forEach(index -> {
		    //Params.LOGGER.log(Level.INFO, "LoadComments: load index {0}", index);
		    Object item = items.get(index);
		    if (item instanceof String) {
			String value = (String) item;
			//Params.LOGGER.log(Level.INFO, "LoadComments: comment is string= {0}", value);
			setArg(argList, prefix + "_" + Params.COMMENT + "_" + (index + 1) + "_" + Params.TEXT, value);
			//Params.LOGGER.log(Level.INFO, "LoadComments: comment as string loaded");
		    } else {
			final String itemKey = prefix + "_" + Params.COMMENT + "_" + (index + 1) + "_";			
			if (item instanceof Comment){
			    //Params.LOGGER.log(Level.INFO, "LoadComments: comment is Object!");
			    Comment comment = (Comment) item;
			    setArg(argList, itemKey + Params.TEXT, comment.getText());
			    setArg(argList, itemKey + Params.DATE, Utils.xmlDateTimeToString(comment.getDate()));
			    setArg(argList, itemKey + Params.COMMENTER, comment.getCommenter());
			    setArg(argList, itemKey + Params.TYPE, comment.getType());
			    //Params.LOGGER.log(Level.INFO, "LoadComments: comment as Object loaded!");
			} else {
			    //Params.LOGGER.log(Level.INFO, "LoadComments: comment as ElementNSImpl {0}", index);
			    ElementNSImpl element = (ElementNSImpl) items.get(index);
			    getTegValue(Params.TEXT.toLowerCase(), element).ifPresent(value->setArg(argList, itemKey + Params.TEXT, value));
			    getTegValue(Params.DATE.toLowerCase(), element).ifPresent(value->setArg(argList, itemKey + Params.DATE, value));
			    getTegValue(Params.COMMENTER.toLowerCase(), element).ifPresent(value->setArg(argList, itemKey + Params.COMMENTER, value));
			    getTegValue(Params.TYPE.toLowerCase(), element).ifPresent(value->setArg(argList, itemKey + Params.TYPE, value));
			    //Params.LOGGER.log(Level.INFO, "LoadComments: comment {0} as ElementNSImpl finish loaded", index);
			}
		    }
		});
	});
    }
    
    private Optional<String> getTegValue(final String tagName, ElementNSImpl element){	
	if (element == null || tagName == null) return Optional.empty();	
	NodeList listNode = element.getElementsByTagName(tagName);	
	if (listNode == null || listNode.getLength() == 0) return Optional.empty();
	TextImpl textImpl = (TextImpl)listNode.item(0).getFirstChild();
	if (textImpl == null) return Optional.empty();
	return Optional.of(textImpl.getData());
    }
	
    private void loadAttributes(ConcurrentHashMap<String, String> argMap, final String prefix, Attributes attributes) {
        if (attributes == null) return;
        List<Attribute> items = attributes.getAttribute();        
        getListSize(items).ifPresent(listSize->{
	    final String key = prefix + "_" + Params.ATTR + "_";
	    setArg(argMap, key + Params.LISTSIZE, String.valueOf(listSize));
	    IntStream.range(0, listSize)
		.forEach(index -> {		
		    String itemKey = key + String.valueOf(index + 1) + "_";
		    setArg(argMap, itemKey + Params.NAME, items.get(index).getName());
		    setArg(argMap, itemKey + Params.STATUS, String.valueOf(items.get(index).getStatus()));
		    setArg(argMap, itemKey + Params.ISCHANGED, String.valueOf(items.get(index).isIsChanged()));
		    if ("equipmentList".equalsIgnoreCase(items.get(index).getName())){
			loadEquipment(argMap, itemKey, items.get(index).getContent());
		    } 
		    setArg(argMap, itemKey + Params.VALUE, 
			String.valueOf(items.get(index).getContent().stream().filter(Objects::nonNull)
			    .map(Object::toString)
			    .collect(Collectors.joining(", "))));
	    });
	});
    }

    private void loadEquipment(ConcurrentHashMap<String, String> argMap, final String prefix_, List<Object> content){
	//Params.LOGGER.log(Level.INFO, "Start loadEquipment for itemKey: {0}", new Object[]{prefix_});	
	final String key = prefix_ + Params.EQUIPMENT + "_";
	AtomicInteger counter = new AtomicInteger();
	content.stream()
	    .forEach(contentItem -> {
		if (contentItem instanceof ElementNSImpl){
		    ElementNSImpl element = (ElementNSImpl) contentItem;
		    if (getTegValue("id", element).isPresent()){
			int index = counter.incrementAndGet();
			final String itemKey = key + String.valueOf(index) + "_";		    
			getTegValue("id", element).ifPresent(v->setArg(argMap, itemKey + Params.ID, v));
			getTegValue("name", element).ifPresent(v->setArg(argMap, itemKey + Params.NAME, v));
			getTegValue("status", element).ifPresent(v->setArg(argMap, itemKey + Params.STATUS, v));
			getTegValue("description", element).ifPresent(v->setArg(argMap, itemKey + Params.DESCRIPTION, v));
			getTegValue("resolution", element).ifPresent(v->setArg(argMap, itemKey + Params.RESOLUTION, v));
			getTegValue("availableCapacity", element).ifPresent(v->setArg(argMap, itemKey + Params.AVAILABLE_CAPACIY, v));
			getTegValue("typeName", element).ifPresent(v->setArg(argMap, itemKey + Params.TYPE, v));			
			getTegValue("exclusiveUse", element).ifPresent(v->setArg(argMap, itemKey + Params.EXCLUSIVEUSE, v));
			getTegValue("centralOfficeId", element).ifPresent(v->setArg(argMap, itemKey + Params.CENTRALOFFICE_ID, v));
			getTegValue("commissioningDate", element).ifPresent(v->setArg(argMap, itemKey + Params.COMMISSIONING_DATE, v));
			getTegValue("category", element).ifPresent(v->setArg(argMap, itemKey + Params.CATEGORY, v));
			getTegValue("extraCapacity", element).ifPresent(v->setArg(argMap, itemKey + Params.EXTRACAPACITY, v));
			getTegValue("hasProjectFiberLink", element).ifPresent(v->setArg(argMap, itemKey + Params.HAS_PROJECT_FIBERLINK, v));
			loadAttrsTeg(argMap, itemKey, element);
		    }
		}
	    });
	setArg(argMap, key + Params.LISTSIZE, String.valueOf(counter.get()));
    }        

    private void loadAttrsTeg(ConcurrentHashMap<String, String> argMap, final String prefix, ElementNSImpl element){
	final String atrKey = prefix + Params.ATTR + "_";
	//Params.LOGGER.log(Level.INFO, "start load equipment attrs = {0}", atrKey);
	NodeList listNode = element.getElementsByTagName("attribute");
	if (listNode == null) return;
	int listSize = listNode.getLength();
	if (listSize > 0){
	    //Params.LOGGER.log(Level.INFO, "listNode size = {0}", listSize);
	    setArg(argMap, atrKey + Params.LISTSIZE, String.valueOf(listSize));
	    for(int i = 0; i < listSize; i++){
		int nodeItemSize = listNode.item(i).getAttributes().getLength();		
		if (nodeItemSize == 0) break;   
		TextImpl textImpl = (TextImpl)listNode.item(i).getFirstChild();
		if (textImpl != null){
		    final String itemKey = atrKey + String.valueOf(i + 1) + "_";
		    setArg(argMap, itemKey + Params.ISCHANGED, listNode.item(i).getAttributes().item(0).getNodeValue());
		    if (nodeItemSize > 1){		    
			setArg(argMap, itemKey + Params.NAME, listNode.item(i).getAttributes().item(1).getNodeValue());
		    }
		    if (nodeItemSize > 2){		    
			setArg(argMap, itemKey + Params.STATUS, listNode.item(i).getAttributes().item(2).getNodeValue());		    
		    }
		    //Params.LOGGER.log(Level.INFO, "data = {0}", new Object[]{textImpl.getData()});
		    setArg(argMap, itemKey + Params.VALUE, textImpl.getData());
		}
	    }	
	}
    }
    
    private Optional<Integer> getListSize(List items){
	int size = items.size();
	if (size > 0) return Optional.of(size);
	return Optional.empty();
    }
	
    private void setArg(ConcurrentHashMap<String, String> argMap, String param, String value) {
        if (value != null && !value.isEmpty()){
	    //Params.LOGGER.log(Level.INFO, "param: {0} = {1}", new Object[]{param,value});
	    argMap.put(param, value);	    
	}
    }
	
    private void setArgDate(ConcurrentHashMap<String, String> argMap, String param, XMLGregorianCalendar value) {
        if (value == null) return;	
        setArg(argMap, param, value.toString());
    }
    
    private Optional<String> getStringDate(XMLGregorianCalendar date){
	return Optional.ofNullable(Utils.xmlDateTimeToString(date));
    }
    
    private Optional<String> getOptString(String value){
	if (value == null) return Optional.empty();
	return Optional.of(value);
    }
}