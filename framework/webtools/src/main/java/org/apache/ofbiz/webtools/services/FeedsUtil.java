package org.apache.ofbiz.webtools.services;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtil;



public class FeedsUtil {
	public static final String module = FeedsUtil.class.getName();
	
	
	public static void marshalObject(Object obj, File file) {
	    try {
	        JAXBContext jaxbContext = JAXBContext.newInstance("org.apache.ofbiz.webtools.services.model");
	  	    Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
	  	    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
	  	    //jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "Unicode");
	  	    jaxbMarshaller.marshal(obj, file);
            
	    } catch (JAXBException e) {
	        e.printStackTrace();
	    }
	}


}
