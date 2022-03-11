package ru.rt.fsom.utils;

import ru.rt.fsom.dict.Params;
import ru.rt.oms.OrderStatusNotification;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import ru.rt.oms.NotificationResponse;

/**
 * @author dimedrol on 23/09/2019
 * @project FSOM-InstantLink
 * @package ru.rt.utils
 *
 * Запись в файл содержимого асинхронной нотификации
 */
@Stateless
public class SOAPLogger {
    private final static String LOG_DIR = "/opt/wildfly/standalone/log/hermes/";    

    public String makeLogInfo(OrderStatusNotification notification, String orderId, String requestId){
	final String info = orderId + " " + requestId;
	Params.LOGGER.log(Level.INFO, "{0} Start sending OrderStatusNotification to IL", new Object[]{info});
	saveNotificationToFile(notification, orderId, requestId);
	return info;
    }
    
    @Asynchronous
    public void saveNotificationToFile(OrderStatusNotification notification, final String orderId, final String requestId){
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(OrderStatusNotification.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            JAXBElement<OrderStatusNotification> jaxbElement = new JAXBElement<>(new QName("", "OrderStatusNotification"), OrderStatusNotification.class, notification);
	    StringWriter sw = new StringWriter();
	    marshaller.marshal(jaxbElement, sw);
	    String omsId = notification.getOrder().getOrderOMSId();
	    saveLogFile("request", notification.getOrder().getOrderId(), notification.getRequestId(), omsId, sw);
        } catch (JAXBException | IOException ex) {
            Params.LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Asynchronous
    public void saveResponseToFile(NotificationResponse response, String orderId, String requestId, String omsId){
	try {
	    JAXBContext jaxbContext = JAXBContext.newInstance(NotificationResponse.class);
	    Marshaller marshaller = jaxbContext.createMarshaller();
	    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
	    JAXBElement<NotificationResponse> jaxbElement = new JAXBElement<>(new QName("", "NotificationResponse"), NotificationResponse.class, response);
	    StringWriter sw = new StringWriter();
	    marshaller.marshal(jaxbElement, sw);	    
	    saveLogFile("response", orderId, requestId, omsId, sw);
	} catch (JAXBException | IOException ex) {
            Params.LOGGER.log(Level.SEVERE, null, ex);
        }
    }
    
    /* *** privates *** */

    private void saveLogFile(final String prefix, final String orderId, final String requestId, final String omsId, StringWriter sw) throws UnsupportedEncodingException, IOException {
        File dir = new File(LOG_DIR);
        if (!dir.exists()){
            dir.mkdirs();
        }
        StringBuilder fileName = new StringBuilder();
        fileName.append(LOG_DIR).append(orderId).append("_").append(omsId).append("_").append(requestId).append(".").append(prefix) ;
        StringBuilder content = new StringBuilder();
	content.append(Utils.getCurrentDateAsString())
	    .append(" ").append(Params.LOGGER_NAME)
	    .append(" ").append(orderId)
	    .append(" ").append(requestId)
	    .append(" ").append(sw.toString());	
	Files.write(Paths.get(fileName.toString()), content.toString().getBytes("utf-8"), 
	    StandardOpenOption.CREATE, 
	    StandardOpenOption.TRUNCATE_EXISTING);	
    }
}