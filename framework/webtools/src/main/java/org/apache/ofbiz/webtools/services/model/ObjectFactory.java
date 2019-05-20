//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.05.21 at 10:11:32 AM EDT 
//


package org.apache.ofbiz.webtools.services.model;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


@XmlRegistry
public class ObjectFactory {

    private final static QName _BigFishStoreFeed_QNAME = new QName("", "BigFishStoreFeed");
    private final static QName _BigFishOrderFeed_QNAME = new QName("", "BigFishOrderFeed");
    private final static QName _ThruDate_QNAME = new QName("", "ThruDate");
    private final static QName _BigFishRequestCatalogFeed_QNAME = new QName("", "BigFishRequestCatalogFeed");
    private final static QName _Url_QNAME = new QName("", "Url");
    private final static QName _BigFishContactUsFeed_QNAME = new QName("", "BigFishContactUsFeed");
    private final static QName _BigFishOrderStatusUpdateFeed_QNAME = new QName("", "BigFishOrderStatusUpdateFeed");
    private final static QName _BigFishCustomerFeed_QNAME = new QName("", "BigFishCustomerFeed");
    private final static QName _BigFishProductFeed_QNAME = new QName("", "BigFishProductFeed");
    private final static QName _BigFishProductRatingFeed_QNAME = new QName("", "BigFishProductRatingFeed");


    public ObjectFactory() {
    }


    public CategoryType createCategoryType() {
        return new CategoryType();
    }

    /**
     * Create an instance of {@link BigFishProductFeedType }
     * 
     */
    public BigFishProductFeedType createBigFishProductFeedType() {
        return new BigFishProductFeedType();
    }


    /**
     * Create an instance of {@link ProductImageType }
     * 
     */
    public ProductImageType createProductImageType() {
        return new ProductImageType();
    }

    /**
     * Create an instance of {@link ProductCategoryType }
     * 
     */
    public ProductCategoryType createProductCategoryType() {
        return new ProductCategoryType();
    }

    /**
     * Create an instance of {@link ProductType }
     * 
     */
    public ProductType createProductType() {
        return new ProductType();
    }

    /**
     * Create an instance of {@link ProductSelectableFeatureType }
     * 
     */
    public ProductSelectableFeatureType createProductSelectableFeatureType() {
        return new ProductSelectableFeatureType();
    }


    /**
     * Create an instance of {@link PdpLargeImageType }
     * 
     */
    public PdpLargeImageType createPdpLargeImageType() {
        return new PdpLargeImageType();
    }




    /**
     * Create an instance of {@link ProductCategoryMemberType }
     * 
     */
    public ProductCategoryMemberType createProductCategoryMemberType() {
        return new ProductCategoryMemberType();
    }


    /**
     * Create an instance of {@link ListPriceType }
     * 
     */
    public ListPriceType createListPriceType() {
        return new ListPriceType();
    }

    /**

    /**
     * Create an instance of {@link PlpSmallImageType }
     * 
     */
    public PlpSmallImageType createPlpSmallImageType() {
        return new PlpSmallImageType();
    }

    /**
     * Create an instance of {@link PdpDetailImageType }
     * 
     */
    public PdpDetailImageType createPdpDetailImageType() {
        return new PdpDetailImageType();
    }


    /**
     * Create an instance of {@link ProductDescriptiveFeatureType }
     * 
     */
    public ProductDescriptiveFeatureType createProductDescriptiveFeatureType() {
        return new ProductDescriptiveFeatureType();
    }

    /**
     * Create an instance of {@link ProductsType }
     * 
     */
    public ProductsType createProductsType() {
        return new ProductsType();
    }


    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "ThruDate", defaultValue = "")
    public JAXBElement<String> createThruDate(String value) {
        return new JAXBElement<String>(_ThruDate_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "Url", defaultValue = "")
    public JAXBElement<String> createUrl(String value) {
        return new JAXBElement<String>(_Url_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigFishProductFeedType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "BigFishProductFeed")
    public JAXBElement<BigFishProductFeedType> createBigFishProductFeed(BigFishProductFeedType value) {
        return new JAXBElement<BigFishProductFeedType>(_BigFishProductFeed_QNAME, BigFishProductFeedType.class, null, value);
    }


    public PlpImageType createPlpImageType() {
        return new PlpImageType();
    }

    public CategoryMemberType createCategoryMemberType() {
        return new CategoryMemberType();
    }

    public ProductPriceType createProductPriceType() {
        return new ProductPriceType();
    }

    public FeatureType createFeatureType() {
        return new FeatureType();
    }
}
