package ru.rt.fsom.notify;

import java.io.StringReader;
import ru.rt.fsom.conf.Conf;
import ru.rt.fsom.dict.Params;
import ru.rt.oms.OrderStatus;
import ru.rt.oms.OrderStatusNotification;
import ru.rt.fsom.utils.Utils;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import javax.xml.bind.Unmarshaller;
import static ru.rt.fsom.dict.Params.LOGGER;

@Stateless
public class NotifyPersist {
    @EJB private Conf conf;

    public int makeNotifyAsCompleted(OrderStatusNotification notification, final String taskId, final String logInfo){
	Params.LOGGER.log(Level.INFO, "{0} Обновление нотификации ...", new Object[]{logInfo});

	OrderStatus orderStatus = notification.getOrder();
	final String sql = 
	    "UPDATE ilink.notify_persist " +
	    "SET \"IsCompleted\" = true, \"DateCompleted\" = ?, \"TaskId\" = ?" +
	    "WHERE \"OrderId\"= ? AND \"OmsId\"= ? AND \"RequestId\"= ?";	
	try (Connection conn = conf.getJdbcConnection();
	    PreparedStatement ps = conn.prepareStatement(sql)
	){
	    ps.setTimestamp(1, Utils.getCurrentTimeStamp());
	    ps.setString(2, taskId);
	    ps.setString(3, orderStatus.getOrderId());
	    ps.setString(4, orderStatus.getOrderOMSId());
	    ps.setString(5, notification.getRequestId());
	    return ps.executeUpdate();
        } catch (SQLException ex) {
            Params.LOGGER.log(Level.SEVERE, "SQL State {0} {1}", new Object[]{ex.getSQLState(), ex.getMessage()});
        } catch (Exception ex) {
	    Params.LOGGER.log(Level.SEVERE, null, ex);
        }
	return 0;
    }
	
    public Set<NotifyQueue> makeNotifyQueue(){	
	Set<NotifyQueue> notifyQueues = new HashSet<>();
	final String sql = "SELECT \"OrderId\", \"OmsId\" FROM ilink.notify_persist WHERE \"IsCompleted\" = false AND \"ServiceGuid\" = ? GROUP BY \"OrderId\", \"OmsId\"";
	try(Connection jdbcConnection = conf.getJdbcConnection();
	    PreparedStatement ps = jdbcConnection.prepareStatement(sql)) 
	{
	    if (jdbcConnection == null) {
		Params.LOGGER.log(Level.SEVERE, "Failed to make JDBC connection!");
		return notifyQueues;
	    }
	    ps.setString(1, conf.getServiceGuid());
	    ResultSet resultSet = ps.executeQuery();
	    while(resultSet.next()){		    
		notifyQueues.add(new NotifyQueue(resultSet.getString(1), resultSet.getString(2)));		    
	    }
	    //LOGGER.log(Level.INFO, "Размер очереди = {0}", notifyQueues.size());
	} catch (SQLException e) {
            Params.LOGGER.log(Level.SEVERE, "SQL State: {0} error: {1}", new Object[]{e.getSQLState(), e.getMessage() });
        } catch (Exception ex) {
	    Params.LOGGER.log(Level.SEVERE, null, ex.getMessage());	    
        }
	return notifyQueues;
    }
    
    public Set<OrderStatusNotification> loadNotCompletedNotifies(String orderId, String omsId){	
	Set<OrderStatusNotification> notifies = new HashSet<>();
	final String sql = "SELECT * FROM ilink.notify_persist WHERE \"OrderId\"= ? AND \"OmsId\"= ? AND \"IsCompleted\" = false AND \"ServiceGuid\" = ? ORDER BY \"DateCreate\"";
	try(Connection jdbcConnection = conf.getJdbcConnection();
	    PreparedStatement ps = jdbcConnection.prepareStatement(sql)) 
	{
	    if (jdbcConnection == null) {
		Params.LOGGER.log(Level.SEVERE, "Failed to make JDBC connection!");
		return notifies;
	    }
	    ps.setString(1, orderId);
	    ps.setString(2, omsId);
	    ps.setString(3, conf.getServiceGuid());
	    ResultSet resultSet = ps.executeQuery();
	    while(resultSet.next()){
		OrderStatusNotification notification = loadNotifyData(resultSet.getString(4));
		if (notification != null){		    
		    notifies.add(notification);		    
		}
	    }
	//LOGGER.log(Level.INFO, "Для очереди: {0} {1} имеются {2} нотификац(я)ий ", new Object[]{orderId, omsId, notifies.size()});	
	} catch (SQLException e) {
            Params.LOGGER.log(Level.SEVERE, "SQL State: {0} error: {1}", new Object[]{e.getSQLState(), e.getMessage() });
        } catch (Exception ex) {
	    Params.LOGGER.log(Level.SEVERE, null, ex.getMessage());
        }
	return notifies;
    }
    
    /* *** privates *** */
    
     private OrderStatusNotification loadNotifyData(String xml){	
	OrderStatusNotification notify = null;
	try {		
	    JAXBContext jaxbContext = JAXBContext.newInstance(OrderStatusNotification.class);
	    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
	    notify = (OrderStatusNotification) unmarshaller.unmarshal(new StringReader(xml));
	} catch (JAXBException ex) {
	    Params.LOGGER.log(Level.SEVERE, null, ex.getMessage());
	} 
	return notify;
    }
}