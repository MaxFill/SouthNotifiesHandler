package ru.rt.fsom.utils;

import org.w3c.dom.Document;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import ru.rt.oms.TFault;

/**
 * Утилиты
 * @author Maksim.Filatov
 */
public final class Utils {
    public static final BigInteger UNKNOWN_MODE_VALUE = BigInteger.valueOf(-2L);
    public static final BigInteger WRONG_REQUEST = BigInteger.valueOf(-1L);
    
    public static final Long STD_PRIORITY = 1L;    
    
    public static TFault createFault(String msg){	
	TFault faultData = new TFault();
	faultData.setResultCode(WRONG_REQUEST);
	faultData.setMessage(msg);
	return faultData;
    }

    public static java.sql.Timestamp getCurrentTimeStamp() {
	java.util.Date today = new java.util.Date();
	return new java.sql.Timestamp(today.getTime());
    }

    public static String getCurrentDateAsString(){
	Date date = Calendar.getInstance().getTime();
	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	return dateFormat.format(date);
    }
	
    public static XMLGregorianCalendar stringToxmlDateTime( String date) {
        try {
           return DatatypeFactory.newInstance().newXMLGregorianCalendar(date);
        } catch (DatatypeConfigurationException | NullPointerException | IllegalArgumentException ex){
            return null;
        }
    }
    
    public static String xmlDateTimeToString( XMLGregorianCalendar date) throws IllegalArgumentException {
        if (null == date) {return null;}        
        return date.toString();
    }
            
    public static String documentToString(final Document doc) {
        // outputs a DOM structure to plain String
        try {
            StringWriter sw = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error converting to String", ex);
        }
    }
}
