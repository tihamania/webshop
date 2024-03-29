//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.05.21 at 10:11:32 AM EDT 
//


package org.apache.ofbiz.webtools.services.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ProductPriceType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ProductPriceType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="ListPrice" type="{}ListPriceType"/>
 *         &lt;element name="SalesPrice" type="{}SalesPriceType"/>
 *         &lt;element name="RecurringPrice" type="{}RecurringPriceType"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProductPriceType", propOrder = {

})
public class ProductPriceType {

    @XmlElement(name = "ListPrice", required = true)
    private ListPriceType listPrice;
    @XmlElement(name = "SalesPrice", required = true)
    private ListPriceType salesPrice;
    /**
     * Gets the value of the listPrice property.
     * 
     * @return
     *     possible object is
     *     {@link ListPriceType }
     *     
     */
    public ListPriceType getListPrice() {
        return listPrice;
    }

    /**
     * Sets the value of the listPrice property.
     * 
     * @param value
     *     allowed object is
     *     {@link ListPriceType }
     *     
     */
    public void setListPrice(ListPriceType value) {
        this.listPrice = value;
    }


    public ListPriceType getSalesPrice() {
        return salesPrice;
    }

    public void setSalesPrice(ListPriceType salesPrice) {
        this.salesPrice = salesPrice;
    }
}
