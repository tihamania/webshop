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
 * <p>Java class for BigFishProductFeedType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="BigFishProductFeedType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="ProductCategory" type="{}ProductCategoryType" minOccurs="0"/>
 *         &lt;element name="Products" type="{}ProductsType" minOccurs="0"/>
 *         &lt;element name="ProductAssociation" type="{}ProductAssociationType" minOccurs="0"/>
 *         &lt;element name="ProductFacetGroup" type="{}ProductFacetCatGroupType" minOccurs="0"/>
 *         &lt;element name="ProductFacetValue" type="{}ProductFacetValueType" minOccurs="0"/>
 *         &lt;element name="ProductManufacturer" type="{}ProductManufacturerType" minOccurs="0"/>
 *         &lt;element name="ProductAttribute" type="{}ProductAttributesType" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BigFishProductFeedType", propOrder = {

})
public class BigFishProductFeedType {

    @XmlElement(name = "ProductCategory")
    private ProductCategoryType productCategory;
    @XmlElement(name = "Products")
    private ProductsType products;


    /**
     * Gets the value of the productCategory property.
     * 
     * @return
     *     possible object is
     *     {@link ProductCategoryType }
     *     
     */
    public ProductCategoryType getProductCategory() {
        return productCategory;
    }

    /**
     * Sets the value of the productCategory property.
     * 
     * @param value
     *     allowed object is
     *     {@link ProductCategoryType }
     *     
     */
    public void setProductCategory(ProductCategoryType value) {
        this.productCategory = value;
    }

    /**
     * Gets the value of the products property.
     * 
     * @return
     *     possible object is
     *     {@link ProductsType }
     *     
     */
    public ProductsType getProducts() {
        return products;
    }

    /**
     * Sets the value of the products property.
     * 
     * @param value
     *     allowed object is
     *     {@link ProductsType }
     *     
     */
    public void setProducts(ProductsType value) {
        this.products = value;
    }


}
