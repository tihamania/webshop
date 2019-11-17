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
 * <p>Java class for ProductImageType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ProductImageType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="PlpSwatch" type="{}PlpSwatchType" minOccurs="0"/>
 *         &lt;element name="PdpSwatch" type="{}PdpSwatchType" minOccurs="0"/>
 *         &lt;element name="PlpSmallImage" type="{}PlpSmallImageType" minOccurs="0"/>
 *         &lt;element name="PlpSmallAltImage" type="{}PlpSmallAltImageType" minOccurs="0"/>
 *         &lt;element name="PdpThumbnailImage" type="{}PdpThumbnailImageType" minOccurs="0"/>
 *         &lt;element name="PdpLargeImage" type="{}PdpLargeImageType" minOccurs="0"/>
 *         &lt;element name="PdpDetailImage" type="{}PdpDetailImageType" minOccurs="0"/>
 *         &lt;element name="PdpVideoImage" type="{}PdpVideoType" minOccurs="0"/>
 *         &lt;element name="PdpVideo360Image" type="{}PdpVideo360Type" minOccurs="0"/>
 *         &lt;element name="PdpAlternateImage" type="{}PdpAlternateImageType" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProductImageType", propOrder = {

})
public class ProductImageType {

    @XmlElement(name = "PlpSmallImage")
    private PlpSmallImageType plpSmallImage;
    @XmlElement(name = "PdpLargeImage")
    private PdpLargeImageType pdpLargeImage;
    @XmlElement(name = "PdpDetailImage")
    private PdpDetailImageType pdpDetailImage;

    /**
     * Gets the value of the plpSmallImage property.
     * 
     * @return
     *     possible object is
     *     {@link PlpSmallImageType }
     *     
     */
    public PlpSmallImageType getPlpSmallImage() {
        return plpSmallImage;
    }

    /**
     * Sets the value of the plpSmallImage property.
     * 
     * @param value
     *     allowed object is
     *     {@link PlpSmallImageType }
     *     
     */
    public void setPlpSmallImage(PlpSmallImageType value) {
        this.plpSmallImage = value;
    }

    /**
     * Gets the value of the pdpLargeImage property.
     * 
     * @return
     *     possible object is
     *     {@link PdpLargeImageType }
     *     
     */
    public PdpLargeImageType getPdpLargeImage() {
        return pdpLargeImage;
    }

    /**
     * Sets the value of the pdpLargeImage property.
     * 
     * @param value
     *     allowed object is
     *     {@link PdpLargeImageType }
     *     
     */
    public void setPdpLargeImage(PdpLargeImageType value) {
        this.pdpLargeImage = value;
    }

    /**
     * Gets the value of the pdpDetailImage property.
     * 
     * @return
     *     possible object is
     *     {@link PdpDetailImageType }
     *     
     */
    public PdpDetailImageType getPdpDetailImage() {
        return pdpDetailImage;
    }

    /**
     * Sets the value of the pdpDetailImage property.
     * 
     * @param value
     *     allowed object is
     *     {@link PdpDetailImageType }
     *     
     */
    public void setPdpDetailImage(PdpDetailImageType value) {
        this.pdpDetailImage = value;
    }




}