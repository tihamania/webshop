package org.apache.ofbiz.webtools.services.helpers;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.ofbiz.base.util.*;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.webtools.services.Constants;
import org.apache.ofbiz.webtools.services.model.ListPriceType;
import org.apache.ofbiz.webtools.services.TihCatalogImport;
import org.apache.ofbiz.webtools.services.model.*;

import java.io.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DataXmlGenerator {

    private static final SimpleDateFormat _sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final DecimalFormat _df = new DecimalFormat("##.00");
    static Map<String, String> featureTypeIdMap = new HashMap<>();
    private static Set sFeatureGroupExists = new HashSet<>();
    private static Map mFeatureValueExists = new HashMap<>();
    private static Map mProductFeatureCatGrpApplFromDateExists = new HashMap<>();
    private static Map mProductFeatureCategoryApplFromDateExists = new HashMap<>();
    private static Map mProductFeatureGroupApplFromDateExists = new HashMap<>();
    public static final String DEFAULT_IMAGE_DIRECTORY = "/tiha_theme/images/catalog/";

    public static void buildProductCategory(List dataRows, String xmlDataDirPath, String loadImagesDirPath, String imageUrl, Delegator _delegator, String localeString) {

        File fOutFile =null;
        BufferedWriter bwOutFile=null;
        String categoryImageName=null;
        try {

            fOutFile = new File(xmlDataDirPath, "000-ProductCategory.xml");
            if (fOutFile.createNewFile())
            {
                bwOutFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fOutFile), "UTF-8"));

                writeXmlHeader(bwOutFile);

                for (int i=0 ; i < dataRows.size() ; i++)
                {
                    StringBuilder  rowString = new StringBuilder();
                    rowString.append("<" + "ProductCategory" + " ");
                    Map mRow = (Map)dataRows.get(i);
                    rowString.append("productCategoryId" + "=\"" + mRow.get("productCategoryId") + "\" ");
                    rowString.append("productCategoryTypeId" + "=\"" + "CATALOG_CATEGORY" + "\" ");
                    rowString.append("primaryParentCategoryId" + "=\"" + mRow.get("parentCategoryId") + "\" ");
                    rowString.append("categoryName" + "=\"" + (String)mRow.get("categoryName") + "\" ");
                    if(mRow.get("description") != null)
                    {
                        rowString.append("description" + "=\"" + (String)mRow.get("description") + "\" ");
                    }
                    if(mRow.get("longDescription") != null)
                    {
                        rowString.append("longDescription" + "=\"" + (String)mRow.get("longDescription") + "\" ");
                    }

                    categoryImageName=(String)mRow.get("plpImageName");

                    if (UtilValidate.isNotEmpty(categoryImageName))
                    {
                        if (!UtilValidate.isUrl(categoryImageName))
                        {
                             categoryImageName = DEFAULT_IMAGE_DIRECTORY + categoryImageName;
                        }

                        rowString.append("categoryImageUrl" + "=\"" + categoryImageName + "\" ");
                    }
                    else
                    {
                        rowString.append("categoryImageUrl" + "=\"" + "" + "\" ");
                    }
                    rowString.append("linkOneImageUrl" + "=\"" + "" + "\" ");
                    rowString.append("linkTwoImageUrl" + "=\"" + "" + "\" ");
                    rowString.append("detailScreen" + "=\"" + "" + "\" ");
                    rowString.append("/>");
                    bwOutFile.write(rowString.toString());
                    bwOutFile.newLine();
                    try
                    {
                        String fromDate = _sdf.format(UtilDateTime.nowTimestamp());

                        List<GenericValue> productCategoryRollups = _delegator.findByAnd("ProductCategoryRollup", UtilMisc.toMap("productCategoryId",mRow.get("productCategoryId"),"parentProductCategoryId",mRow.get("parentCategoryId")),UtilMisc.toList("-fromDate"), false);
                        if(UtilValidate.isNotEmpty(productCategoryRollups))
                        {
                            productCategoryRollups = EntityUtil.filterByDate(productCategoryRollups);
                            if(UtilValidate.isNotEmpty(productCategoryRollups))
                            {
                                GenericValue productCategoryRollup = EntityUtil.getFirst(productCategoryRollups);
                                fromDate = _sdf.format(new Date(productCategoryRollup.getTimestamp("fromDate").getTime()));
                            }
                        }

                        rowString.setLength(0);
                        rowString.append("<" + "ProductCategoryRollup" + " ");
                        rowString.append("productCategoryId" + "=\"" + mRow.get("productCategoryId") + "\" ");
                        rowString.append("parentProductCategoryId" + "=\"" + mRow.get("parentCategoryId") + "\" ");
                        rowString.append("fromDate" + "=\"" + fromDate + "\" ");
                        rowString.append("sequenceNum" + "=\"" + ((i +1) *10) + "\" ");
                        rowString.append("/>");
                        bwOutFile.write(rowString.toString());
                        bwOutFile.newLine();
                    }
                    catch(Exception ex)
                    {
                        Debug.logError(ex, TihCatalogImport.module);
                    }
                    addCategoryContentRow(rowString, mRow, bwOutFile, "text", "PLP_ESPOT_CONTENT", "plpText", _delegator, localeString);
                    addCategoryContentRow(rowString, mRow, bwOutFile, "text", "PDP_ADDITIONAL", "pdpText", _delegator, localeString);

                }
                bwOutFile.flush();
                writeXmlFooter(bwOutFile);
            }
        }
        catch (Exception e) {
            Debug.logError(e, TihCatalogImport.module);
        }
        finally {
            try {
                if (bwOutFile != null) {
                    bwOutFile.close();
                }
            } catch (IOException ioe) {
                Debug.logError(ioe, TihCatalogImport.module);
            }
        }

    }

    private static void addCategoryContentRow(StringBuilder rowString, Map mRow, BufferedWriter bwOutFile, String contentType, String categoryContentType, String colName, Delegator _delegator, String localeString) {

        String contentId=null;
        String productCategoryId=null;
        String dataResourceId=null;
        try {

            String contentValue=(String)mRow.get(colName);
            if (UtilValidate.isEmpty(contentValue) && UtilValidate.isEmpty(contentValue.trim()))
            {
                return;
            }
            productCategoryId=(String)mRow.get("productCategoryId");

            List<GenericValue> lCategoryContent = _delegator.findByAnd("ProductCategoryContent", UtilMisc.toMap("productCategoryId",productCategoryId,"prodCatContentTypeId",categoryContentType),UtilMisc.toList("-fromDate"), false);
            if (UtilValidate.isNotEmpty(lCategoryContent))
            {
                GenericValue categoryContent = EntityUtil.getFirst(lCategoryContent);
                GenericValue content=categoryContent.getRelatedOne("Content");
                contentId=content.getString("contentId");
                dataResourceId=content.getString("dataResourceId");
            }
            else
            {
                contentId=_delegator.getNextSeqId("Content");
                dataResourceId=_delegator.getNextSeqId("DataResource");

            }


            if ("text".equals(contentType))
            {
                rowString.setLength(0);
                rowString.append("<" + "DataResource" + " ");
                rowString.append("dataResourceId" + "=\"" + dataResourceId + "\" ");
                rowString.append("dataResourceTypeId" + "=\"" + "ELECTRONIC_TEXT" + "\" ");
                rowString.append("dataTemplateTypeId" + "=\"" + "FTL" + "\" ");
                rowString.append("statusId" + "=\"" + "CTNT_PUBLISHED" + "\" ");
                rowString.append("dataResourceName" + "=\"" + colName + "\" ");
                if(UtilValidate.isNotEmpty(localeString))
                {
                    rowString.append("localeString" + "=\"" + localeString + "\" ");
                }
                rowString.append("mimeTypeId" + "=\"" + "application/octet-stream" + "\" ");
                rowString.append("objectInfo" + "=\"" + "" + "\" ");
                rowString.append("isPublic" + "=\"" + "Y" + "\" ");
                rowString.append("/>");
                bwOutFile.write(rowString.toString());
                bwOutFile.newLine();

                rowString.setLength(0);
                rowString.append("<" + "ElectronicText" + " ");
                rowString.append("dataResourceId" + "=\"" + dataResourceId + "\"> ");
                rowString.append("<textData><![CDATA[" + "=\"" +contentValue + "\" ");
                rowString.append("]]></textData></ElectronicText>");
                bwOutFile.write(rowString.toString());
                bwOutFile.newLine();


            }
            else
            {
                rowString.setLength(0);
                rowString.append("<" + "DataResource" + " ");
                rowString.append("dataResourceId" + "=\"" + dataResourceId + "\" ");
                rowString.append("dataResourceTypeId" + "=\"" + "SHORT_TEXT" + "\" ");
                rowString.append("dataTemplateTypeId" + "=\"" + "FTL" + "\" ");
                rowString.append("statusId" + "=\"" + "CTNT_PUBLISHED" + "\" ");
                rowString.append("dataResourceName" + "=\"" + contentValue + "\" ");
                if(UtilValidate.isNotEmpty(localeString))
                {
                    rowString.append("localeString" + "=\"" + localeString + "\" ");
                }
                rowString.append("mimeTypeId" + "=\"" + "text/html" + "\" ");

                if (!UtilValidate.isUrl(contentValue))
                {
                    contentValue = DEFAULT_IMAGE_DIRECTORY + contentValue;
                }

                rowString.append("objectInfo" + "=\"" + contentValue.trim() + "\" ");
                rowString.append("isPublic" + "=\"" + "Y" + "\" ");
                rowString.append("/>");
                bwOutFile.write(rowString.toString());
                bwOutFile.newLine();
            }

            rowString.setLength(0);
            rowString.append("<" + "Content" + " ");
            rowString.append("contentId" + "=\"" + contentId + "\" ");
            rowString.append("contentTypeId" + "=\"" + "DOCUMENT" + "\" ");
            rowString.append("dataResourceId" + "=\"" + dataResourceId + "\" ");
            rowString.append("statusId" + "=\"" + "CTNT_PUBLISHED" + "\" ");
            rowString.append("contentName" + "=\"" + colName + "\" ");
            if(UtilValidate.isNotEmpty(localeString))
            {
                rowString.append("localeString" + "=\"" + localeString + "\" ");
            }
            rowString.append("/>");
            bwOutFile.write(rowString.toString());
            bwOutFile.newLine();
            String sFromDate = (String)mRow.get("fromDate");
            if (UtilValidate.isEmpty(sFromDate))
            {
                sFromDate=_sdf.format(UtilDateTime.nowTimestamp());
            }
            rowString.setLength(0);
            rowString.append("<" + "ProductCategoryContent" + " ");
            rowString.append("productCategoryId" + "=\"" + productCategoryId + "\" ");
            rowString.append("contentId" + "=\"" + contentId + "\" ");
            rowString.append("prodCatContentTypeId" + "=\"" + categoryContentType + "\" ");
            rowString.append("fromDate" + "=\"" + sFromDate + "\" ");
            rowString.append("/>");
            bwOutFile.write(rowString.toString());
            bwOutFile.newLine();

        }
        catch (Exception e) {
            Debug.logError(e, TihCatalogImport.module);
        }

        return;

    }

    /**
     * Generate the category XML
     *
     * @param factory             JAXB object createtion factory object
     * @param productCategoryList JAXB object list
     * @param dataRows            data rows
     */
    public static void generateProductCategoryXML(ObjectFactory factory, List productCategoryList, List<Map<String, Object>> dataRows) {
        for (Map<String, Object> dataRow : dataRows) {
            CategoryType category = factory.createCategoryType();
            category.setCategoryId(getString(dataRow.get(Constants.CATEGORY_ID_DATA_KEY)));
            category.setParentCategoryId(getString(dataRow.get(Constants.CATEGORY_PARENT_DATA_KEY)));
            category.setCategoryName(getString(dataRow.get(Constants.CATEGORY_NAME_DATA_KEY)));
            category.setDescription(getString(dataRow.get(Constants.CATEGORY_DESC_DATA_KEY)));
            category.setLongDescription(getString(dataRow.get(Constants.CATEGORY_LONG_DESC_DATA_KEY)));
            PlpImageType plpImage = factory.createPlpImageType();
            plpImage.setUrl(getString(dataRow.get(Constants.CATEGORY_PLP_IMG_NAME_DATA_KEY)));
            category.setPlpImage(plpImage);
            category.setAdditionalPlpText(getString(dataRow.get(Constants.CATEGORY_PLP_TEXT_DATA_KEY)));
            category.setAdditionalPdpText(getString(dataRow.get(Constants.CATEGORY_PDP_TEXT_DATA_KEY)));
            category.setFromDate(formatDate(dataRow.get(Constants.CATEGORY_FROM_DATE_DATA_KEY)));
            category.setThruDate(formatDate(dataRow.get(Constants.CATEGORY_THRU_DATE_DATA_KEY)));
            productCategoryList.add(category);
        }
    }

    /**
     * Generate the product XML
     *
     * @param factory     JAXB object createtion factory object
     * @param productList JAXB object list
     * @param dataRows    data rows
     */
    public static void generateProductXML(ObjectFactory factory, List productList, List<Map<String, Object>> dataRows) {
        for (Map<String, Object> dataRow : dataRows) {
            ProductType productType = factory.createProductType();
            productType.setMasterProductId(getString(dataRow.get(Constants.PRODUCT_MASTER_ID_DATA_KEY)));
            productType.setProductId(getString(dataRow.get(Constants.PRODUCT_ID_DATA_KEY)));

            ProductCategoryMemberType productCategoryMemberType = factory.createProductCategoryMemberType();
            if (UtilValidate.isNotEmpty(dataRow.get(Constants.PRODUCT_CAT_COUNT_DATA_KEY))) {
                List categoryMemberList = productCategoryMemberType.getCategory();
                int cnt = (Integer) dataRow.get(Constants.PRODUCT_CAT_COUNT_DATA_KEY);
                StringBuffer categories = new StringBuffer();
                for (int i = 1; i <= cnt; i++) {
                    CategoryMemberType categoryMemberType = factory.createCategoryMemberType();
                    categoryMemberType.setCategoryId(getString(dataRow.get(FlexibleStringExpander.expandString(Constants.PRODUCT_CAT_ID_DATA_KEY, UtilMisc.toMap("count", i)))));
                    categoryMemberType.setSequenceNum(getString(dataRow.get(FlexibleStringExpander.expandString(Constants.PRODUCT_CAT_SEQ_NUM_DATA_KEY, UtilMisc.toMap("count", i)))));
                    categoryMemberType.setFromDate(formatDate(dataRow.get(FlexibleStringExpander.expandString(Constants.PRODUCT_CAT_FROM_DATE_DATA_KEY, UtilMisc.toMap("count", i)))));
                    categoryMemberType.setThruDate(formatDate(dataRow.get(FlexibleStringExpander.expandString(Constants.PRODUCT_CAT_THRU_DATE_DATA_KEY, UtilMisc.toMap("count", i)))));
                    categoryMemberList.add(categoryMemberType);
                }
            }
            productType.setProductCategoryMember(productCategoryMemberType);

            productType.setInternalName(getString(dataRow.get(Constants.PRODUCT_INTERNAL_NAME_DATA_KEY)));
            productType.setProductName(getString(dataRow.get(Constants.PRODUCT_NAME_DATA_KEY)));
            productType.setSalesPitch(getString(dataRow.get(Constants.PRODUCT_SALES_PITCH_DATA_KEY)));
            productType.setLongDescription(getString(dataRow.get(Constants.PRODUCT_LONG_DESC_DATA_KEY)));
            productType.setSpecialInstructions(getString(dataRow.get(Constants.PRODUCT_SPCL_INS_DATA_KEY)));
            productType.setDeliveryInfo(getString(dataRow.get(Constants.PRODUCT_DELIVERY_INFO_DATA_KEY)));
            productType.setDirections(getString(dataRow.get(Constants.PRODUCT_DIRECTIONS_DATA_KEY)));
            productType.setTermsAndConds(getString(dataRow.get(Constants.PRODUCT_TERMS_COND_DATA_KEY)));
            productType.setIngredients(getString(dataRow.get(Constants.PRODUCT_INGREDIENTS_DATA_KEY)));
            productType.setWarnings(getString(dataRow.get(Constants.PRODUCT_WARNING_DATA_KEY)));
            productType.setPlpLabel(getString(dataRow.get(Constants.PRODUCT_PLP_LABEL_DATA_KEY)));
            productType.setPdpLabel(getString(dataRow.get(Constants.PRODUCT_PDP_LABEL_DATA_KEY)));
            productType.setProductHeight(formatBigDecimal(dataRow.get(Constants.PRODUCT_HEIGHT_DATA_KEY)));
            productType.setProductWidth(formatBigDecimal(dataRow.get(Constants.PRODUCT_WIDTH_DATA_KEY)));
            productType.setProductDepth(formatBigDecimal(dataRow.get(Constants.PRODUCT_DEPTH_DATA_KEY)));
            productType.setProductWeight(formatBigDecimal(dataRow.get(Constants.PRODUCT_WEIGHT_DATA_KEY)));
            productType.setReturnable(getString(dataRow.get(Constants.PRODUCT_RETURN_ABLE_DATA_KEY)));
            productType.setTaxable(getString(dataRow.get(Constants.PRODUCT_TAX_ABLE_DATA_KEY)));
            productType.setChargeShipping(getString(dataRow.get(Constants.PRODUCT_CHARGE_SHIP_DATA_KEY)));
            productType.setIntroDate(formatDate(dataRow.get(Constants.PRODUCT_INTRO_DATE_DATA_KEY)));
            productType.setDiscoDate(formatDate(dataRow.get(Constants.PRODUCT_DISCO_DATE_DATA_KEY)));
            productType.setManufacturerId(getString(dataRow.get(Constants.PRODUCT_MANUFACT_PARTY_ID_DATA_KEY)));

            ProductPriceType productPriceType = factory.createProductPriceType();
            ListPriceType listPrice = factory.createListPriceType();
            listPrice.setPrice(formatBigDecimal(dataRow.get(Constants.PRODUCT_LIST_PRICE_DATA_KEY)));
            listPrice.setCurrency(getString(dataRow.get(Constants.PRODUCT_LIST_PRICE_CUR_DATA_KEY)));
            listPrice.setFromDate(formatDate(dataRow.get(Constants.PRODUCT_LIST_PRICE_FROM_DATA_KEY)));
            listPrice.setThruDate(formatDate(dataRow.get(Constants.PRODUCT_LIST_PRICE_THRU_DATA_KEY)));
            productPriceType.setListPrice(listPrice);


            ProductSelectableFeatureType productSelectableFeatureType = factory.createProductSelectableFeatureType();
            List selectableFeaturesList = productSelectableFeatureType.getFeature();
            for (int i = 1; i <= 5; i++) {
                if (UtilValidate.isNotEmpty(dataRow.get(FlexibleStringExpander.expandString(Constants.PRODUCT_SLCT_FEAT_TYPE_ID_DATA_KEY, UtilMisc.toMap("count", i))))) {
                    FeatureType selectableFeature = factory.createFeatureType();
                    selectableFeature.setFeatureId(getString(dataRow.get(FlexibleStringExpander.expandString(Constants.PRODUCT_SLCT_FEAT_TYPE_ID_DATA_KEY, UtilMisc.toMap("count", i)))));
                    List valueList = selectableFeature.getValue();
                    valueList.add(getString(dataRow.get(FlexibleStringExpander.expandString(Constants.PRODUCT_SLCT_FEAT_DESC_DATA_KEY, UtilMisc.toMap("count", i)))));
                    selectableFeature.setDescription(getString(dataRow.get(FlexibleStringExpander.expandString(Constants.PRODUCT_SLCT_FEAT_TYPE_DESC_DATA_KEY, UtilMisc.toMap("count", i)))));
                    selectableFeature.setSequenceNum(getString(dataRow.get(FlexibleStringExpander.expandString(Constants.PRODUCT_SLCT_FEAT_SEQ_NUM_DATA_KEY, UtilMisc.toMap("count", i)))));
                    selectableFeature.setFromDate(formatDate(dataRow.get(FlexibleStringExpander.expandString(Constants.PRODUCT_SLCT_FEAT_FROM_DATA_KEY, UtilMisc.toMap("count", i)))));
                    selectableFeature.setThruDate(formatDate(dataRow.get(FlexibleStringExpander.expandString(Constants.PRODUCT_SLCT_FEAT_THRU_DATA_KEY, UtilMisc.toMap("count", i)))));
                    selectableFeaturesList.add(selectableFeature);
                }
            }
            productType.setProductSelectableFeature(productSelectableFeatureType);

            ProductDescriptiveFeatureType productDescriptiveFeatureType = factory.createProductDescriptiveFeatureType();
            List descriptiveFeaturesList = productDescriptiveFeatureType.getFeature();
            for (int i = 1; i <= 5; i++) {
                if (UtilValidate.isNotEmpty(dataRow.get(FlexibleStringExpander.expandString(Constants.PRODUCT_DESC_FEAT_TYPE_ID_DATA_KEY, UtilMisc.toMap("count", i))))) {
                    FeatureType descriptiveFeature = factory.createFeatureType();
                    String featurePrefix = getString(dataRow.get(FlexibleStringExpander.expandString(Constants.PRODUCT_DESC_FEAT_TYPE_ID_DATA_KEY, UtilMisc.toMap("count", i))));
                    descriptiveFeature.setFeatureId("FEATURE_" + featurePrefix.toUpperCase());
                    List valueList = descriptiveFeature.getValue();
                    valueList.add(getString(dataRow.get(FlexibleStringExpander.expandString(Constants.PRODUCT_DESC_FEAT_DESC_DATA_KEY, UtilMisc.toMap("count", i)))));
                    descriptiveFeature.setDescription(featurePrefix.toUpperCase() + getString(dataRow.get(FlexibleStringExpander.expandString(Constants.PRODUCT_DESC_FEAT_TYPE_DESC_DATA_KEY, UtilMisc.toMap("count", i)))));
                    descriptiveFeature.setSequenceNum(getString(dataRow.get(FlexibleStringExpander.expandString(Constants.PRODUCT_DESC_FEAT_SEQ_NUM_DATA_KEY, UtilMisc.toMap("count", i)))));
                    descriptiveFeature.setFromDate(formatDate(dataRow.get(FlexibleStringExpander.expandString(Constants.PRODUCT_DESC_FEAT_FROM_DATA_KEY, UtilMisc.toMap("count", i)))));
                    descriptiveFeature.setThruDate(formatDate(dataRow.get(FlexibleStringExpander.expandString(Constants.PRODUCT_DESC_FEAT_THRU_DATA_KEY, UtilMisc.toMap("count", i)))));
                    descriptiveFeaturesList.add(descriptiveFeature);
                }
            }
            productType.setProductDescriptiveFeature(productDescriptiveFeatureType);

            ProductImageType productImage = factory.createProductImageType();


            PlpSmallImageType plpSmallImage = factory.createPlpSmallImageType();
            plpSmallImage.setUrl(getString(dataRow.get(Constants.PRODUCT_SMALL_IMG_DATA_KEY)));
            plpSmallImage.setThruDate(formatDate(dataRow.get(Constants.PRODUCT_SMALL_IMG_THRU_DATA_KEY)));
            productImage.setPlpSmallImage(plpSmallImage);

            PdpLargeImageType pdpLargeImage = factory.createPdpLargeImageType();
            pdpLargeImage.setUrl(getString(dataRow.get(Constants.PRODUCT_LARGE_IMG_DATA_KEY)));
            pdpLargeImage.setThruDate(formatDate(dataRow.get(Constants.PRODUCT_LARGE_IMG_THRU_DATA_KEY)));
            productImage.setPdpLargeImage(pdpLargeImage);

            PdpDetailImageType pdpDetailImage = factory.createPdpDetailImageType();
            pdpDetailImage.setUrl(getString(dataRow.get(Constants.PRODUCT_DETAIL_IMG_DATA_KEY)));
            pdpDetailImage.setThruDate(formatDate(dataRow.get(Constants.PRODUCT_DETAIL_IMG_THRU_DATA_KEY)));
            productImage.setPdpDetailImage(pdpDetailImage);


            productType.setProductImage(productImage);


            productList.add(productType);
        }
    }

    public static void buildProduct(List dataRows, String xmlDataDirPath, Delegator _delegator) {

        File fOutFile = null;
        BufferedWriter bwOutFile = null;
        StringBuilder rowString = new StringBuilder();
        String masterProductId = null;
        String productId = null;

        try {

            fOutFile = new File(xmlDataDirPath, "030-Product.xml");
            if (fOutFile.createNewFile()) {
                bwOutFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fOutFile), "UTF-8"));
                String currencyUomId = UtilProperties.getPropertyValue("general.properties", "currency.uom.id.default", "USD");
                String priceFromDate = _sdf.format(UtilDateTime.nowTimestamp());
                writeXmlHeader(bwOutFile);

                for (int i = 0; i < dataRows.size(); i++) {
                    Map mRow = (Map) dataRows.get(i);
                    masterProductId = (String) mRow.get("masterProductId");
                    productId = (String) mRow.get("productId");
                    String[] productCategoryIds = null;
                    if ((UtilValidate.isEmpty(productId)) || (UtilValidate.isNotEmpty(productId) && masterProductId.equals(productId))) {
                        rowString.setLength(0);
                        rowString.append("<" + "Product" + " ");
                        rowString.append("productId" + "=\"" + masterProductId + "\" ");
                        rowString.append("productTypeId" + "=\"" + "FINISHED_GOOD" + "\" ");
                        String productCategoryId = (String) mRow.get("productCategoryId");
                        if (UtilValidate.isNotEmpty(productCategoryId)) {
                            productCategoryIds = productCategoryId.split(",");
                            String primaryProductCategoryId = productCategoryIds[0].trim();
                            rowString.append("primaryProductCategoryId" + "=\"" + primaryProductCategoryId + "\" ");
                        }
                        if (mRow.get("manufacturerId") != null) {
                            rowString.append("manufacturerPartyId" + "=\"" + mRow.get("manufacturerId") + "\" ");
                        }
                        if (mRow.get("internalName") != null) {
                            rowString.append("internalName" + "=\"" + (String) mRow.get("internalName") + "\" ");
                        }
                        rowString.append("brandName" + "=\"" + "" + "\" ");

                        try {
                            String fromDate = (String) mRow.get("introDate");
                            if (UtilValidate.isNotEmpty(fromDate)) {
                                rowString.append("introductionDate" + "=\"" + fromDate + "\" ");
                            } else {
                                rowString.append("introductionDate" + "=\"" + "" + "\" ");
                            }
                            if (mRow.get("productName") != null) {
                                rowString.append("productName" + "=\"" + (String) mRow.get("productName") + "\" ");
                            }
                            String thruDate = (String) mRow.get("discoDate");
                            rowString.append("salesDiscontinuationDate" + "=\"" + "" + "\" ");
                        } catch (Exception ex) {
                            Debug.logError(ex, TihCatalogImport.module);
                        }
                        rowString.append("requireInventory" + "=\"" + "N" + "\" ");
                        if (mRow.get("returnable") != null) {
                            rowString.append("returnable" + "=\"" + mRow.get("returnable") + "\" ");
                        }
                        if (mRow.get("taxable") != null) {
                            rowString.append("taxable" + "=\"" + mRow.get("taxable") + "\" ");
                        }
                        if (mRow.get("chargeShipping") != null) {
                            rowString.append("chargeShipping" + "=\"" + mRow.get("chargeShipping") + "\" ");
                        }
                        if (mRow.get("productHeight") != null) {
                            rowString.append("productHeight" + "=\"" + mRow.get("productHeight") + "\" ");
                        }
                        if (mRow.get("productWidth") != null) {
                            rowString.append("productWidth" + "=\"" + mRow.get("productWidth") + "\" ");
                        }
                        if (mRow.get("productDepth") != null) {
                            rowString.append("productDepth" + "=\"" + mRow.get("productDepth") + "\" ");
                        }
                        if (mRow.get("weight") != null) {
                            rowString.append("weight" + "=\"" + mRow.get("weight") + "\" ");
                        }
                        String isVirtual = "N";

                        if (UtilValidate.isNotEmpty(productId) && masterProductId.equals(productId)) {
                            isVirtual = "Y";
                        }

                        rowString.append("isVirtual" + "=\"" + isVirtual + "\" ");
                        rowString.append("isVariant" + "=\"" + "N" + "\" ");
                        rowString.append("/>");
                        bwOutFile.write(rowString.toString());
                        bwOutFile.newLine();
                        if (UtilValidate.isNotEmpty(productCategoryIds)) {
                            for (int j = 0; j < productCategoryIds.length; j++) {
                                String sequenceNum = (String) mRow.get("sequenceNum");
                                String productCategoryFromDate = _sdf.format(UtilDateTime.nowTimestamp());

                                if (UtilValidate.isEmpty(sequenceNum)) {
                                    sequenceNum = "10";
                                }
                                if (UtilValidate.isNotEmpty(productCategoryIds[j].trim())) {

                                    if (UtilValidate.isNotEmpty(mRow.get(productCategoryIds[j].trim() + "_sequenceNum"))) {
                                        sequenceNum = (String) mRow.get(productCategoryIds[j].trim() + "_sequenceNum");
                                    }
                                    if (UtilValidate.isNotEmpty(sequenceNum)) {
                                        sequenceNum = sequenceNum.trim();
                                        if (!UtilValidate.isInteger(sequenceNum)) {
                                            sequenceNum = "0";
                                        }
                                    }
                                    productCategoryFromDate = _sdf.format(new Date());


                                    rowString.setLength(0);
                                    rowString.append("<" + "ProductCategoryMember" + " ");
                                    rowString.append("productCategoryId" + "=\"" + productCategoryIds[j].trim() + "\" ");
                                    rowString.append("productId" + "=\"" + masterProductId + "\" ");
                                    rowString.append("fromDate" + "=\"" + productCategoryFromDate + "\" ");
                                    rowString.append("thruDate" + "=\"\" ");
                                    rowString.append("comments" + "=\"" + "" + "\" ");
                                    rowString.append("sequenceNum" + "=\"" + sequenceNum + "\" ");
                                    rowString.append("quantity" + "=\"" + "" + "\" ");
                                    rowString.append("/>");
                                    bwOutFile.write(rowString.toString());
                                    bwOutFile.newLine();
                                }

                            }
                        }

                        if (UtilValidate.isNotEmpty(mRow.get("listPriceCurrency"))) {
                            currencyUomId = (String) mRow.get("listPriceCurrency");
                        }


                        priceFromDate = _sdf.format(new Date());
                        if (mRow.get("listPrice") != null) {
                            rowString.setLength(0);
                            rowString.append("<" + "ProductPrice" + " ");
                            rowString.append("productId" + "=\"" + masterProductId + "\" ");
                            rowString.append("productPriceTypeId" + "=\"" + "LIST_PRICE" + "\" ");
                            rowString.append("productPricePurposeId" + "=\"" + "PURCHASE" + "\" ");
                            rowString.append("currencyUomId" + "=\"" + currencyUomId + "\" ");
                            rowString.append("productStoreGroupId" + "=\"" + "_NA_" + "\" ");
                            rowString.append("price" + "=\"" + mRow.get("listPrice") + "\" ");
                            rowString.append("fromDate" + "=\"" + priceFromDate + "\" ");
                            rowString.append("thruDate" + "=\"\" ");
                            rowString.append("/>");
                            bwOutFile.write(rowString.toString());
                            bwOutFile.newLine();
                        }
                        if (UtilValidate.isNotEmpty(mRow.get("defaultPriceCurrency"))) {
                            currencyUomId = (String) mRow.get("defaultPriceCurrency");
                        }
                        priceFromDate = _sdf.format(new Date());
                        if (mRow.get("defaultPrice") != null) {
                            rowString.setLength(0);
                            rowString.append("<" + "ProductPrice" + " ");
                            rowString.append("productId" + "=\"" + masterProductId + "\" ");
                            rowString.append("productPriceTypeId" + "=\"" + "DEFAULT_PRICE" + "\" ");
                            rowString.append("productPricePurposeId" + "=\"" + "PURCHASE" + "\" ");
                            rowString.append("currencyUomId" + "=\"" + currencyUomId + "\" ");
                            rowString.append("productStoreGroupId" + "=\"" + "_NA_" + "\" ");
                            rowString.append("price" + "=\"" + mRow.get("defaultPrice") + "\" ");
                            rowString.append("fromDate" + "=\"\" ");
                            rowString.append("/>");
                            bwOutFile.write(rowString.toString());
                            bwOutFile.newLine();
                        }

                    }

                }
                bwOutFile.flush();
                writeXmlFooter(bwOutFile);
            }


        } catch (Exception e) {
            Debug.logError(e, TihCatalogImport.module);
            e.printStackTrace();
        } finally {
            try {
                if (bwOutFile != null) {
                    bwOutFile.close();
                }
            } catch (IOException ioe) {
                Debug.logError(ioe, TihCatalogImport.module);
            }
        }

    }


    public static void buildProductVariant(List dataRows, String xmlDataDirPath, String loadImagesDirPath, String imageUrl, Boolean removeAll, Delegator _delegator) {

        File fOutFile = null;
        BufferedWriter bwOutFile = null;
        Map mFeatureTypeMap = new HashMap();

        StringBuilder rowString = new StringBuilder();

        try {

            fOutFile = new File(xmlDataDirPath, "040-ProductVariant.xml");
            if (fOutFile.createNewFile()) {
                bwOutFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fOutFile), "UTF-8"));
                writeXmlHeader(bwOutFile);

                for (int i = 0; i < dataRows.size(); i++) {
                    Map mRow = (Map) dataRows.get(i);
                    String productId = (String) mRow.get("masterProductId");
                    String featureProductId = (String) mRow.get("productId");
                    String fromDate = (String) mRow.get("introDate");
                    String thruDate = (String) mRow.get("discoDate");

                    mFeatureTypeMap.clear();
                    int iSeq = 0;

                    //not a variant product
                    if (UtilValidate.isEmpty(featureProductId) || productId.equals(featureProductId)) {
                        continue;
                    }

                    addProductVariantRow(rowString, bwOutFile, mRow, loadImagesDirPath, imageUrl, productId, featureProductId, fromDate, thruDate, iSeq, removeAll, _delegator);

                }
                bwOutFile.flush();
                writeXmlFooter(bwOutFile);
            }
        } catch (Exception e) {
            Debug.logError(e, TihCatalogImport.module);
        } finally {
            try {
                if (bwOutFile != null) {
                    bwOutFile.close();
                }
            } catch (IOException ioe) {
                Debug.logError(ioe, TihCatalogImport.module);
            }
        }

    }

    private static void addProductVariantRow(StringBuilder rowString, BufferedWriter bwOutFile, Map mRow, String loadImagesDirPath, String imageUrl, String masterProductId, String featureProductId, String sFromDate, String sThruDate, int iSeq, Boolean removeAll, Delegator delegator) {
        String currencyUomId = UtilProperties.getPropertyValue("general.properties", "currency.uom.id.default", "USD");
        String priceFromDate = _sdf.format(UtilDateTime.nowTimestamp());
        try {

            rowString.setLength(0);
            rowString.append("<" + "Product" + " ");
            rowString.append("productId" + "=\"" + featureProductId + "\" ");
            rowString.append("productTypeId" + "=\"" + "FINISHED_GOOD" + "\" ");
            rowString.append("isVirtual" + "=\"" + "N" + "\" ");
            rowString.append("isVariant" + "=\"" + "Y" + "\" ");
            if (UtilValidate.isNotEmpty(sFromDate)) {
                rowString.append("introductionDate" + "=\"" + sFromDate + "\" ");
            } else {
                rowString.append("introductionDate" + "=\"" + "" + "\" ");
            }
            if (UtilValidate.isNotEmpty(sThruDate)) {
                rowString.append("salesDiscontinuationDate" + "=\"" + sThruDate + "\" ");
            } else {
                rowString.append("salesDiscontinuationDate" + "=\"" + "" + "\" ");
            }

            if (mRow.get("manufacturerId") != null) {
                rowString.append("manufacturerPartyId" + "=\"" + mRow.get("manufacturerId") + "\" ");
            }
            if (mRow.get("internalName") != null) {
                rowString.append("internalName" + "=\"" + (String) mRow.get("internalName") + "\" ");
            }
            rowString.append("brandName" + "=\"" + "" + "\" ");
            if (mRow.get("productName") != null) {
                rowString.append("productName" + "=\"" + (String) mRow.get("productName") + "\" ");
            } else {
                rowString.append("productName" + "=\"" + "" + "\" ");
            }
            if (mRow.get("returnable") != null) {
                rowString.append("returnable" + "=\"" + mRow.get("returnable") + "\" ");
            }
            if (mRow.get("taxable") != null) {
                rowString.append("taxable" + "=\"" + mRow.get("taxable") + "\" ");
            }
            if (mRow.get("chargeShipping") != null) {
                rowString.append("chargeShipping" + "=\"" + mRow.get("chargeShipping") + "\" ");
            }
            if (mRow.get("productHeight") != null) {
                rowString.append("productHeight" + "=\"" + mRow.get("productHeight") + "\" ");
            }
            if (mRow.get("productWidth") != null) {
                rowString.append("productWidth" + "=\"" + mRow.get("productWidth") + "\" ");
            }
            if (mRow.get("productDepth") != null) {
                rowString.append("productDepth" + "=\"" + mRow.get("productDepth") + "\" ");
            }
            if (mRow.get("weight") != null) {
                rowString.append("weight" + "=\"" + mRow.get("weight") + "\" ");
            }

            rowString.append("/>");
            bwOutFile.write(rowString.toString());
            bwOutFile.newLine();

            List<GenericValue> productAssocList = delegator.findByAnd("ProductAssoc", UtilMisc.toMap("productId", masterProductId, "productIdTo", featureProductId, "productAssocTypeId", "PRODUCT_VARIANT"), null, false);
            if(UtilValidate.isNotEmpty(productAssocList))
            {
                productAssocList = EntityUtil.filterByDate(productAssocList, true);
            }
            if(UtilValidate.isEmpty(productAssocList) || removeAll)
            {
                rowString.setLength(0);
                rowString.append("<" + "ProductAssoc" + " ");
                rowString.append("productId" + "=\"" + masterProductId+ "\" ");
                rowString.append("productIdTo" + "=\"" + featureProductId + "\" ");
                rowString.append("productAssocTypeId" + "=\"" + "PRODUCT_VARIANT" + "\" ");
                rowString.append("fromDate" + "=\"" + _sdf.format(UtilDateTime.nowTimestamp()) + "\" ");
                rowString.append("sequenceNum" + "=\"" + ((iSeq +1) *10) + "\" ");
                rowString.append("/>");
                bwOutFile.write(rowString.toString());
                bwOutFile.newLine();
            }


            String sPrice = (String) mRow.get("listPrice");

            if (UtilValidate.isNotEmpty(sPrice)) {
                if (UtilValidate.isNotEmpty(mRow.get("listPriceCurrency"))) {
                    currencyUomId = (String) mRow.get("listPriceCurrency");
                }
                if (UtilValidate.isEmpty(mRow.get("listPriceFromDate"))) {
                    List<GenericValue> productListPrices = delegator.findByAnd("ProductPrice", UtilMisc.toMap("productId", featureProductId, "productPriceTypeId", "LIST_PRICE", "productPricePurposeId", "PURCHASE", "currencyUomId", currencyUomId, "productStoreGroupId", "_NA_"), UtilMisc.toList("-fromDate"), false);
                    if (UtilValidate.isNotEmpty(productListPrices)) {
                        productListPrices = EntityUtil.filterByDate(productListPrices);
                        if (UtilValidate.isNotEmpty(productListPrices)) {
                            GenericValue productListPrice = EntityUtil.getFirst(productListPrices);
                            priceFromDate = _sdf.format(new Date(productListPrice.getTimestamp("fromDate").getTime()));
                        }
                    }
                } else {
                    priceFromDate = (String) mRow.get("listPriceFromDate");
                    priceFromDate = _sdf.format(priceFromDate);
                }

                rowString.setLength(0);
                rowString.append("<" + "ProductPrice" + " ");
                rowString.append("productId" + "=\"" + featureProductId + "\" ");
                rowString.append("productPriceTypeId" + "=\"" + "LIST_PRICE" + "\" ");
                rowString.append("productPricePurposeId" + "=\"" + "PURCHASE" + "\" ");
                rowString.append("currencyUomId" + "=\"" + currencyUomId + "\" ");
                rowString.append("productStoreGroupId" + "=\"" + "_NA_" + "\" ");
                rowString.append("price" + "=\"" + sPrice + "\" ");
                rowString.append("fromDate" + "=\"" + priceFromDate + "\" ");
                if (UtilValidate.isNotEmpty(mRow.get("listPriceThruDate"))) {
                    String priceThruDate = (String) mRow.get("listPriceThruDate");
                    priceThruDate = _sdf.format(priceThruDate);
                    rowString.append("thruDate" + "=\"" + priceThruDate + "\" ");
                }
                rowString.append("/>");
                bwOutFile.write(rowString.toString());
                bwOutFile.newLine();

            }

            sPrice = (String) mRow.get("defaultPrice");
            if (UtilValidate.isNotEmpty(sPrice)) {
                if (UtilValidate.isNotEmpty(mRow.get("defaultPriceCurrency"))) {
                    currencyUomId = (String) mRow.get("defaultPriceCurrency");
                }
                if (UtilValidate.isEmpty(mRow.get("defaultPriceFromDate"))) {
                    List<GenericValue> productDefaultPrices = delegator.findByAnd("ProductPrice", UtilMisc.toMap("productId", featureProductId, "productPriceTypeId", "DEFAULT_PRICE", "productPricePurposeId", "PURCHASE", "currencyUomId", currencyUomId, "productStoreGroupId", "_NA_"), UtilMisc.toList("-fromDate"), false);
                    if (UtilValidate.isNotEmpty(productDefaultPrices)) {
                        productDefaultPrices = EntityUtil.filterByDate(productDefaultPrices);
                        if (UtilValidate.isNotEmpty(productDefaultPrices)) {
                            GenericValue productDefaultPrice = EntityUtil.getFirst(productDefaultPrices);
                            priceFromDate = _sdf.format(new Date(productDefaultPrice.getTimestamp("fromDate").getTime()));
                        }
                    }
                } else {
                    priceFromDate = (String) mRow.get("defaultPriceFromDate");
                    priceFromDate = _sdf.format(priceFromDate);
                }
                rowString.setLength(0);
                rowString.append("<" + "ProductPrice" + " ");
                rowString.append("productId" + "=\"" + featureProductId + "\" ");
                rowString.append("productPriceTypeId" + "=\"" + "DEFAULT_PRICE" + "\" ");
                rowString.append("productPricePurposeId" + "=\"" + "PURCHASE" + "\" ");
                rowString.append("currencyUomId" + "=\"" + currencyUomId + "\" ");
                rowString.append("productStoreGroupId" + "=\"" + "_NA_" + "\" ");
                rowString.append("price" + "=\"" + sPrice + "\" ");
                rowString.append("fromDate" + "=\"" + priceFromDate + "\" ");
                if (UtilValidate.isNotEmpty(mRow.get("defaultPriceThruDate"))) {
                    String priceThruDate = (String) mRow.get("defaultPriceThruDate");
                    priceThruDate = _sdf.format(priceThruDate);
                    rowString.append("thruDate" + "=\"" + priceThruDate + "\" ");
                }
                rowString.append("/>");
                bwOutFile.write(rowString.toString());
                bwOutFile.newLine();

            }

        } catch (Exception e) {
            Debug.logError(e, TihCatalogImport.module);
        }
    }


    public static void buildProductSelectableFeatures(List dataRows, String xmlDataDirPath, Delegator _delegator) {

        File fOutFile = null;
        BufferedWriter bwOutFile = null;
        Map mFeatureTypeMap = new HashMap();
        StringBuilder rowString = new StringBuilder();
        String masterProductId = null;
        String productId = null;

        try {

            fOutFile = new File(xmlDataDirPath, "043-ProductSelectableFeature.xml");
            if (fOutFile.createNewFile()) {
                bwOutFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fOutFile), "UTF-8"));
                writeXmlHeader(bwOutFile);

                Map productFeatureSequenceMap = new HashMap();

                for (int i = 0; i < dataRows.size(); i++) {
                    Map mRow = (Map) dataRows.get(i);
                    masterProductId = (String) mRow.get("masterProductId");
                    productId = (String) mRow.get("productId");

                    mFeatureTypeMap.clear();

                    int totSelectableFeatures = 5;
                    if (UtilValidate.isNotEmpty(mRow.get("totSelectableFeatures"))) {
                        totSelectableFeatures = Integer.parseInt((String) mRow.get("totSelectableFeatures"));
                    }

                    for (int j = 1; j <= totSelectableFeatures; j++) {
                        buildFeatureMap(mFeatureTypeMap, (String) mRow.get("selectabeFeature_" + j), _delegator);
                    }

                    int iSeq = 0;

                    if (mFeatureTypeMap.size() > 0) {
                        Set featureTypeSet = mFeatureTypeMap.keySet();
                        Iterator iterFeatureType = featureTypeSet.iterator();
                        while (iterFeatureType.hasNext()) {
                            String featureType = (String) iterFeatureType.next();
                            String featureTypeId = StringUtil.removeSpaces(featureType).toUpperCase();

                            if (featureTypeId.length() > 20) {
                                featureTypeId = featureTypeId.substring(0, 20);
                            }
                            Map mFeatureMap = (Map) mFeatureTypeMap.get(featureType);
                            Set featureSet = mFeatureMap.keySet();
                            Iterator iterFeature = featureSet.iterator();

                            while (iterFeature.hasNext()) {
                                String featureId = (String) iterFeature.next();
                                String featureValue = (String) mFeatureMap.get(featureId);

                                String featureFromDate = _sdf.format(UtilDateTime.nowTimestamp());
                                if (UtilValidate.isEmpty((String) mRow.get(featureType.trim() + "_fromDate"))) {
                                    List<GenericValue> productFeatureAppls = _delegator.findByAnd("ProductFeatureAppl", UtilMisc.toMap("productId", productId, "productFeatureId", featureId, "productFeatureApplTypeId", "STANDARD_FEATURE"), UtilMisc.toList("-fromDate"), false);
                                    if (UtilValidate.isNotEmpty(productFeatureAppls)) {
                                        productFeatureAppls = EntityUtil.filterByDate(productFeatureAppls);
                                        if (UtilValidate.isNotEmpty(productFeatureAppls)) {
                                            GenericValue productFeatureAppl = EntityUtil.getFirst(productFeatureAppls);
                                            featureFromDate = _sdf.format(new Date(productFeatureAppl.getTimestamp("fromDate").getTime()));
                                        }
                                    }
                                } else {
                                    featureFromDate = (String) mRow.get(featureType.trim() + "_fromDate");
                                }

                                String sequenceNum = String.valueOf((iSeq + 1) * 10);
                                if (UtilValidate.isNotEmpty((String) mRow.get(featureType.trim() + "_sequenceNum"))) {
                                    sequenceNum = (String) mRow.get(featureType.trim() + "_sequenceNum");
                                    if (UtilValidate.isNotEmpty(sequenceNum)) {
                                        sequenceNum = sequenceNum.trim();
                                        if (!UtilValidate.isInteger(sequenceNum)) {
                                            sequenceNum = "0";
                                        }
                                    }
                                }

                                Map entityFieldMap = new HashMap();
                                rowString.setLength(0);
                                rowString.append("<" + "ProductFeatureAppl" + " ");
                                rowString.append("productId" + "=\"" + productId + "\" ");
                                rowString.append("productFeatureId" + "=\"" + featureId + "\" ");
                                rowString.append("productFeatureApplTypeId" + "=\"" + "STANDARD_FEATURE" + "\" ");
                                rowString.append("fromDate" + "=\"" + featureFromDate + "\" ");
                                if (UtilValidate.isNotEmpty((String) mRow.get(featureType.trim() + "_thruDate"))) {
                                    String featureThruDate = (String) mRow.get(featureType.trim() + "_thruDate");
                                    featureThruDate = _sdf.format(featureThruDate);
                                    rowString.append("thruDate" + "=\"" + featureThruDate + "\" ");
                                }
                                rowString.append("sequenceNum" + "=\"" + sequenceNum + "\" ");
                                rowString.append("/>");

                                if (UtilValidate.isEmpty((String) mRow.get(featureType.trim() + "_sequenceNum"))) {
                                    entityFieldMap.put("productId", productId);
                                    entityFieldMap.put("productFeatureId", featureId);
                                    entityFieldMap.put("fromDate", featureFromDate);
                                    productFeatureSequenceMap.put(entityFieldMap, featureValue);
                                }

                                bwOutFile.write(rowString.toString());
                                bwOutFile.newLine();


                                if (UtilValidate.isEmpty((String) mRow.get(featureType.trim() + "_fromDate"))) {
                                    List<GenericValue> productFeatureAppls = _delegator.findByAnd("ProductFeatureAppl", UtilMisc.toMap("productId", masterProductId, "productFeatureId", featureId, "productFeatureApplTypeId", "SELECTABLE_FEATURE"), UtilMisc.toList("-fromDate"), false);
                                    if (UtilValidate.isNotEmpty(productFeatureAppls)) {
                                        productFeatureAppls = EntityUtil.filterByDate(productFeatureAppls);
                                        if (UtilValidate.isNotEmpty(productFeatureAppls)) {
                                            GenericValue productFeatureAppl = EntityUtil.getFirst(productFeatureAppls);
                                            featureFromDate = _sdf.format(new Date(productFeatureAppl.getTimestamp("fromDate").getTime()));
                                        }
                                    }
                                } else {
                                    featureFromDate = (String) mRow.get(featureType.trim() + "_fromDate");
                                }
                                entityFieldMap = new HashMap();
                                rowString.setLength(0);
                                rowString.append("<" + "ProductFeatureAppl" + " ");
                                rowString.append("productId" + "=\"" + masterProductId + "\" ");
                                rowString.append("productFeatureId" + "=\"" + featureId + "\" ");
                                rowString.append("productFeatureApplTypeId" + "=\"" + "SELECTABLE_FEATURE" + "\" ");
                                rowString.append("fromDate" + "=\"" + featureFromDate + "\" ");
                                if (UtilValidate.isNotEmpty((String) mRow.get(featureType.trim() + "_thruDate"))) {
                                    String featureThruDate = (String) mRow.get(featureType.trim() + "_thruDate");
                                    featureThruDate = _sdf.format(featureThruDate);
                                    rowString.append("thruDate" + "=\"" + featureThruDate + "\" ");
                                }
                                rowString.append("sequenceNum" + "=\"" + sequenceNum + "\" ");
                                rowString.append("/>");
                                if (UtilValidate.isEmpty((String) mRow.get(featureType.trim() + "_sequenceNum"))) {
                                    entityFieldMap.put("productId", masterProductId);
                                    entityFieldMap.put("productFeatureId", featureId);
                                    entityFieldMap.put("fromDate", featureFromDate);
                                    productFeatureSequenceMap.put(entityFieldMap, featureValue);
                                }

                                bwOutFile.write(rowString.toString());
                                bwOutFile.newLine();

                            }
                        }
                    }
                }
                if (UtilValidate.isNotEmpty(productFeatureSequenceMap)) {
                    buildFeatureSequence(rowString, bwOutFile, productFeatureSequenceMap, "ProductFeatureAppl");
                }

                bwOutFile.flush();
                writeXmlFooter(bwOutFile);
            }
        } catch (Exception e) {
            Debug.logError(e, TihCatalogImport.module);
        } finally {
            try {
                if (bwOutFile != null) {
                    bwOutFile.close();
                }
            } catch (IOException ioe) {
                Debug.logError(ioe, TihCatalogImport.module);
            }
        }

    }

    public static void buildProductCategoryFeatures(List dataRows, String xmlDataDirPath, Boolean removeAll, Delegator delegator) {

        File fOutFile = null;
        BufferedWriter bwOutFile = null;
        Map mFeatureTypeMap = new HashMap();
        Map mFeatureExists = new HashMap();
        Map mFeatureTypeExists = new HashMap();
        Map mFeatureCategoryGroupApplExists = new HashMap();
        Map mFeatureGroupApplExists = new HashMap();
        StringBuilder rowString = new StringBuilder();
        String masterProductId = null;
        String productId = null;
        String productCategoryId = null;
        String[] productCategoryIds = null;
        Map mProductCategoryIds = new HashMap();
        try {

            fOutFile = new File(xmlDataDirPath, "010-ProductCategoryFeature.xml");
            if (fOutFile.createNewFile()) {
                bwOutFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fOutFile), "UTF-8"));
                writeXmlHeader(bwOutFile);

                Map productFeatureSequenceMap = new HashMap();

                for (int i = 0; i < dataRows.size(); i++) {
                    Map mRow = (Map) dataRows.get(i);

                    int totSelectableFeatures = 5;
                    if (UtilValidate.isNotEmpty(mRow.get("totSelectableFeatures"))) {
                        totSelectableFeatures = Integer.parseInt((String) mRow.get("totSelectableFeatures"));
                    }

                    for (int j = 1; j <= totSelectableFeatures; j++) {
                        buildFeatureMap(mFeatureTypeMap, (String) mRow.get("selectabeFeature_" + j), delegator);
                    }
                    int totDescriptiveFeatures = 5;
                    if (UtilValidate.isNotEmpty(mRow.get("totDescriptiveFeatures"))) {
                        totDescriptiveFeatures = Integer.parseInt((String) mRow.get("totDescriptiveFeatures"));
                    }

                    for (int j = 1; j <= totDescriptiveFeatures; j++) {
                        buildFeatureMap(mFeatureTypeMap, (String) mRow.get("descriptiveFeature_" + j), delegator);
                    }

                    masterProductId = (String) mRow.get("masterProductId");
                    productId = (String) mRow.get("productId");
                    if ((UtilValidate.isEmpty(productId)) || (UtilValidate.isNotEmpty(productId) && masterProductId.equals(productId))) {
                        productCategoryId = (String) mRow.get("productCategoryId");
                        if (UtilValidate.isNotEmpty(productCategoryId)) {
                            productCategoryIds = productCategoryId.split(",");
                            mProductCategoryIds.put(masterProductId, productCategoryIds);
                        }
                    } else {
                        if (mProductCategoryIds.containsKey(masterProductId)) {
                            productCategoryIds = (String[]) mProductCategoryIds.get(masterProductId);
                        }
                    }
                    if (mFeatureTypeMap.size() > 0) {
                        Set featureTypeSet = mFeatureTypeMap.keySet();
                        Iterator iterFeatureType = featureTypeSet.iterator();
                        int seqNumber = 0;
                        while (iterFeatureType.hasNext()) {

                            String featureType = (String) iterFeatureType.next();
                            String featureTypeId = StringUtil.removeSpaces(featureType).toUpperCase();
                            if (featureTypeId.length() > 20) {
                                featureTypeId = featureTypeId.substring(0, 20);
                            }
                            if (!mFeatureTypeExists.containsKey(featureType)) {
                                mFeatureTypeExists.put(featureType, featureType);
                                rowString.setLength(0);
                                rowString.append("<" + "ProductFeatureType" + " ");
                                rowString.append("productFeatureTypeId" + "=\"" + featureTypeId + "\" ");
                                rowString.append("parentTypeId" + "=\"" + "" + "\" ");
                                rowString.append("hasTable" + "=\"" + "N" + "\" ");
                                if (UtilValidate.isNotEmpty((String) mRow.get(featureType.trim() + "_description"))) {
                                    rowString.append("description" + "=\"" + mRow.get(featureType.trim() + "_description") + "\" ");
                                } else {
                                    rowString.append("description" + "=\"" + featureType + "\" ");
                                }
                                rowString.append("/>");
                                bwOutFile.write(rowString.toString());
                                bwOutFile.newLine();

                                rowString.setLength(0);
                                rowString.append("<" + "ProductFeatureCategory" + " ");
                                rowString.append("productFeatureCategoryId" + "=\"" + featureTypeId + "\" ");
                                rowString.append("parentCategoryId" + "=\"" + "" + "\" ");
                                if (UtilValidate.isNotEmpty((String) mRow.get(featureType.trim() + "_description"))) {
                                    rowString.append("description" + "=\"" + mRow.get(featureType.trim() + "_description") + "\" ");
                                } else {
                                    rowString.append("description" + "=\"" + featureType + "\" ");
                                }
                                rowString.append("/>");
                                bwOutFile.write(rowString.toString());
                                bwOutFile.newLine();


                                rowString.setLength(0);
                                rowString.append("<" + "ProductFeatureGroup" + " ");
                                rowString.append("productFeatureGroupId" + "=\"" + featureTypeId + "\" ");
                                if (UtilValidate.isNotEmpty((String) mRow.get(featureType.trim() + "_description"))) {
                                    rowString.append("description" + "=\"" + mRow.get(featureType.trim() + "_description") + "\" ");
                                } else {
                                    rowString.append("description" + "=\"" + featureType + "\" ");
                                }
                                rowString.append("/>");
                                bwOutFile.write(rowString.toString());
                                bwOutFile.newLine();

                                sFeatureGroupExists.add(featureTypeId);
                            }

                            String sNowTimestamp = _sdf.format(UtilDateTime.nowTimestamp());
                            if (UtilValidate.isNotEmpty(productCategoryIds)) {
                                for (int j = 0; j < productCategoryIds.length; j++) {
                                    String sProductCategoryId = productCategoryIds[j].trim();
                                    if (UtilValidate.isNotEmpty(sProductCategoryId) && !mFeatureCategoryGroupApplExists.containsKey(sProductCategoryId + "_" + featureTypeId)) {
                                        mFeatureCategoryGroupApplExists.put(sProductCategoryId + "_" + featureTypeId, sProductCategoryId + "_" + featureTypeId);

                                        if (!mProductFeatureCatGrpApplFromDateExists.containsKey(sProductCategoryId + "~" + featureTypeId)) {
                                            String productFeatureCatGrpApplFromDate = sNowTimestamp;
                                            String productFeatureCatGrpApplThruDate = "";
                                            String productFeatureCatGrpApplSeqNum = "";
                                            List<GenericValue> productFeatureCatGrpApplList = delegator.findByAnd("ProductFeatureCatGrpAppl", UtilMisc.toMap("productCategoryId", sProductCategoryId, "productFeatureGroupId", featureTypeId), UtilMisc.toList("-fromDate"), false);
                                            //Issue 39047 - Hidden Filters are shown after product load.
                                            //If the filter by date is applied to the list, then all expired feature cats are not found.
                                            //Removing the filter then no duplicates are generated.
                                            //productFeatureCatGrpApplList = EntityUtil.filterByDate(productFeatureCatGrpApplList);
                                            if (UtilValidate.isNotEmpty(productFeatureCatGrpApplList)) {
                                                GenericValue productFeatureCatGrpAppl = EntityUtil.getFirst(productFeatureCatGrpApplList);
                                                productFeatureCatGrpApplFromDate = _sdf.format(new Date(productFeatureCatGrpAppl.getTimestamp("fromDate").getTime()));
                                                if (UtilValidate.isNotEmpty(productFeatureCatGrpAppl.getTimestamp("thruDate"))) {
                                                    productFeatureCatGrpApplThruDate = _sdf.format(new Date(productFeatureCatGrpAppl.getTimestamp("thruDate").getTime()));
                                                }
                                                if (UtilValidate.isNotEmpty(productFeatureCatGrpAppl.getLong("sequenceNum"))) {
                                                    productFeatureCatGrpApplSeqNum = "" + productFeatureCatGrpAppl.getLong("sequenceNum");
                                                }
                                            }
                                            rowString.setLength(0);
                                            rowString.append("<" + "ProductFeatureCatGrpAppl" + " ");
                                            rowString.append("productCategoryId" + "=\"" + sProductCategoryId + "\" ");
                                            rowString.append("productFeatureGroupId" + "=\"" + featureTypeId + "\" ");
                                            rowString.append("fromDate" + "=\"" + productFeatureCatGrpApplFromDate + "\" ");
                                            if (UtilValidate.isNotEmpty(productFeatureCatGrpApplThruDate)) {
                                                rowString.append("thruDate" + "=\"" + productFeatureCatGrpApplThruDate + "\" ");

                                            }
                                            if (UtilValidate.isNotEmpty(productFeatureCatGrpApplSeqNum)) {
                                                rowString.append("sequenceNum" + "=\"" + productFeatureCatGrpApplSeqNum + "\" ");

                                            } else {
                                                rowString.append("sequenceNum" + "=\"" + ((seqNumber + 1) * 10) + "\" ");

                                            }
                                            rowString.append("/>");
                                            bwOutFile.write(rowString.toString());
                                            bwOutFile.newLine();

                                            mProductFeatureCatGrpApplFromDateExists.put(sProductCategoryId + "~" + featureTypeId, productFeatureCatGrpApplFromDate);
                                        }

                                        if (!mProductFeatureCategoryApplFromDateExists.containsKey(sProductCategoryId + "~" + featureTypeId)) {
                                            String productFeatureCategoryApplFromDate = sNowTimestamp;
                                            String productFeatureCategoryApplThruDate = "";
                                            String productFeatureCategoryApplSeqNum = "";
                                            List<GenericValue> productFeatureCategoryApplList = delegator.findByAnd("ProductFeatureCategoryAppl", UtilMisc.toMap("productCategoryId", sProductCategoryId, "productFeatureCategoryId", featureTypeId), UtilMisc.toList("-fromDate"), false);
                                            //Issue 39047 - Hidden Filters are shown after product load.
                                            //If the filter by date is applied to the list, then all expired feature cats are not found.
                                            //Removing the filter then no duplicates are generated.
                                            //productFeatureCategoryApplList = EntityUtil.filterByDate(productFeatureCategoryApplList);
                                            if (UtilValidate.isNotEmpty(productFeatureCategoryApplList)) {
                                                GenericValue productFeatureCategoryAppl = EntityUtil.getFirst(productFeatureCategoryApplList);
                                                productFeatureCategoryApplFromDate = _sdf.format(new Date(productFeatureCategoryAppl.getTimestamp("fromDate").getTime()));
                                                if (UtilValidate.isNotEmpty(productFeatureCategoryAppl.getTimestamp("thruDate"))) {
                                                    productFeatureCategoryApplThruDate = _sdf.format(new Date(productFeatureCategoryAppl.getTimestamp("thruDate").getTime()));
                                                }
                                                if (UtilValidate.isNotEmpty(productFeatureCategoryAppl.getLong("sequenceNum"))) {
                                                    productFeatureCategoryApplSeqNum = "" + productFeatureCategoryAppl.getLong("sequenceNum");
                                                }
                                            }

                                            rowString.setLength(0);
                                            rowString.append("<" + "ProductFeatureCategoryAppl" + " ");
                                            rowString.append("productCategoryId" + "=\"" + sProductCategoryId + "\" ");
                                            rowString.append("productFeatureCategoryId" + "=\"" + featureTypeId + "\" ");
                                            rowString.append("fromDate" + "=\"" + productFeatureCategoryApplFromDate + "\" ");
                                            if (UtilValidate.isNotEmpty(productFeatureCategoryApplThruDate)) {
                                                rowString.append("thruDate" + "=\"" + productFeatureCategoryApplThruDate + "\" ");

                                            }
                                            if (UtilValidate.isNotEmpty(productFeatureCategoryApplSeqNum)) {
                                                rowString.append("sequenceNum" + "=\"" + productFeatureCategoryApplSeqNum + "\" ");

                                            }
                                            rowString.append("/>");
                                            bwOutFile.write(rowString.toString());
                                            bwOutFile.newLine();

                                            mProductFeatureCategoryApplFromDateExists.put(sProductCategoryId + "~" + featureTypeId, productFeatureCategoryApplFromDate);
                                        }
                                    }

                                }
                            }


                            Map mFeatureMap = (Map) mFeatureTypeMap.get(featureType);
                            Set featureSet = mFeatureMap.keySet();
                            Iterator iterFeature = featureSet.iterator();
                            int iSeq = 0;

                            while (iterFeature.hasNext()) {
                                String featureId = (String) iterFeature.next();
                                String featureDescription = (String) mFeatureMap.get(featureId);
	                			/*String featureId =StringUtil.removeSpaces(feature).toUpperCase();
	                			featureId =StringUtil.replaceString(featureId, "&", "");
	                			featureId=featureTypeId+"_"+featureId;
	                			if (featureId.length() > 20)
	                			{
	                				featureId=featureId.substring(0,20);
	                			}*/
                                if (!mFeatureExists.containsKey(featureId)) {
                                    mFeatureExists.put(featureId, featureId);

                                    Map<String, String> prodFeatExistsResults = productFeatureExistsInDB(featureTypeId, featureDescription, delegator);
                                    if (UtilValidate.isEmpty(prodFeatExistsResults) || !"Y".equalsIgnoreCase(prodFeatExistsResults.get("existsInDB"))) {
                                        rowString.setLength(0);
                                        rowString.append("<" + "ProductFeature" + " ");
                                        rowString.append("productFeatureId" + "=\"" + featureId + "\" ");
                                        rowString.append("productFeatureTypeId" + "=\"" + featureTypeId + "\" ");
                                        rowString.append("productFeatureCategoryId" + "=\"" + featureTypeId + "\" ");
                                        rowString.append("description" + "=\"" + featureDescription + "\" ");
                                        rowString.append("/>");
                                        bwOutFile.write(rowString.toString());
                                        bwOutFile.newLine();
                                    } else {
                                        featureId = prodFeatExistsResults.get("featureId");
                                    }
                                    mFeatureValueExists.put(featureTypeId + "~" + featureDescription.toUpperCase(), featureId);
                                }

                                if (!mFeatureGroupApplExists.containsKey(featureId)) {
                                    mFeatureGroupApplExists.put(featureId, featureId);

                                    String productFeatureGroupApplFromDate = _sdf.format(UtilDateTime.nowTimestamp());
                                    List<GenericValue> productFeatureGroupApplList = delegator.findByAnd("ProductFeatureGroupAppl", UtilMisc.toMap("productFeatureGroupId", featureTypeId, "productFeatureId", featureId), UtilMisc.toList("-fromDate"), false);
                                    productFeatureGroupApplList = EntityUtil.filterByDate(productFeatureGroupApplList);

                                    if (UtilValidate.isNotEmpty(productFeatureGroupApplList)) {
                                        GenericValue productFeatureGroupAppl = EntityUtil.getFirst(productFeatureGroupApplList);
                                        productFeatureGroupApplFromDate = _sdf.format(new Date(productFeatureGroupAppl.getTimestamp("fromDate").getTime()));
                                    }
                                    String sequenceNum = String.valueOf((iSeq + 1) * 10);
                                    if (UtilValidate.isNotEmpty((String) mRow.get(featureType.trim() + "_sequenceNum"))) {
                                        sequenceNum = (String) mRow.get(featureType.trim() + "_sequenceNum");
                                        if (UtilValidate.isNotEmpty(sequenceNum)) {
                                            sequenceNum = sequenceNum.trim();
                                            if (!UtilValidate.isInteger(sequenceNum)) {
                                                sequenceNum = "0";
                                            }
                                        }
                                    }

                                    Map entityFieldMap = new HashMap<>();
                                    rowString.setLength(0);
                                    rowString.append("<" + "ProductFeatureGroupAppl" + " ");
                                    rowString.append("productFeatureGroupId" + "=\"" + featureTypeId + "\" ");
                                    rowString.append("productFeatureId" + "=\"" + featureId + "\" ");
                                    rowString.append("fromDate" + "=\"" + productFeatureGroupApplFromDate + "\" ");
                                    rowString.append("sequenceNum" + "=\"" + sequenceNum + "\" ");
                                    rowString.append("/>");

                                    if (UtilValidate.isEmpty((String) mRow.get(featureType.trim() + "_sequenceNum"))) {
                                        entityFieldMap.put("productFeatureGroupId", featureTypeId);
                                        entityFieldMap.put("productFeatureId", featureId);
                                        entityFieldMap.put("fromDate", productFeatureGroupApplFromDate);
                                        productFeatureSequenceMap.put(entityFieldMap, featureDescription);
                                    }
                                    bwOutFile.write(rowString.toString());
                                    bwOutFile.newLine();

                                    mProductFeatureGroupApplFromDateExists.put(featureTypeId + "~" + featureDescription, productFeatureGroupApplFromDate);
                                }
                                iSeq++;

                            }
                            seqNumber++;
                        }
                    }
                }
                if (UtilValidate.isNotEmpty(productFeatureSequenceMap)) {
                    buildFeatureSequence(rowString, bwOutFile, productFeatureSequenceMap, "ProductFeatureGroupAppl");
                }
                bwOutFile.flush();
                writeXmlFooter(bwOutFile);
            }
        } catch (Exception e) {
            Debug.logError(e, TihCatalogImport.module);
        } finally {
            try {
                if (bwOutFile != null) {
                    bwOutFile.close();
                }
            } catch (IOException ioe) {
                Debug.logError(ioe, TihCatalogImport.module);
            }
        }
    }

    private static Map buildFeatureMap(Map featureTypeMap, String parseFeatureType, Delegator _delegator) {
        if (UtilValidate.isNotEmpty(parseFeatureType)) {
            int iFeatIdx = parseFeatureType.indexOf(':');
            if (iFeatIdx > -1) {
                String rawFeature = parseFeatureType.substring(0, iFeatIdx).trim();
                String featureType = rawFeature;
                String sFeatures = parseFeatureType.substring(iFeatIdx + 1);
                String[] featureTokens = sFeatures.split(",");
                Map mFeatureMap = new HashMap();
                for (int f = 0; f < featureTokens.length; f++) {
                    String featureId = "";
                    String description = featureTokens[f].trim();
                    String featureTypeKey = StringUtil.removeSpaces(featureType).toUpperCase() + "~" + description;

                    //first check if feature Id was already added to the existing map
                    for (Map.Entry<String, String> entry : featureTypeIdMap.entrySet()) {
                        if (StringEscapeUtils.unescapeHtml(entry.getKey().toUpperCase()).equals(StringEscapeUtils.unescapeHtml(featureTypeKey.toUpperCase()))) {
                            featureId = (String) entry.getValue();
                            featureTypeKey = entry.getKey();
                            String[] featureTypeKeyArr = featureTypeKey.split("~");
                            if (UtilValidate.isNotEmpty(featureTypeKeyArr[1])) {
                                description = featureTypeKeyArr[1];
                            }
                        }
                    }

                    //if it was not added, then check the DB.  If it does not exist in the DB, then create a new one.
                    if (UtilValidate.isEmpty(featureId)) {
                        Map<String, String> prodFeatExistsResults = productFeatureExistsInDB(featureType, featureTokens[f], _delegator);
                        if (UtilValidate.isNotEmpty(prodFeatExistsResults) && "Y".equalsIgnoreCase(prodFeatExistsResults.get("existsInDB"))) {
                            featureId = prodFeatExistsResults.get("featureId");
                            featureTypeKey = prodFeatExistsResults.get("featureTypeKey");
                            ;
                            description = prodFeatExistsResults.get("description");
                            ;
                        }

                        if (UtilValidate.isEmpty(featureId)) {
                            //if the we do not have this feature in the map or in the DB, then create a new one
                            featureId = _delegator.getNextSeqId("ProductFeature");
                        }
                    }
                    featureTypeIdMap.put(featureTypeKey, featureId);
                    mFeatureMap.put("" + featureId, "" + description);
                }
                featureTypeMap.put(featureType, mFeatureMap);
            }

        }
        return featureTypeMap;

    }

    public static void buildProductDistinguishingFeatures(List dataRows, String xmlDataDirPath, Delegator _delegator) {
        File fOutFile = null;
        BufferedWriter bwOutFile = null;
        Map mFeatureTypeMap = new HashMap();
        StringBuilder rowString = new StringBuilder();
        String masterProductId = null;
        String variantProductId = null;
        Map mMasterProductId = new HashMap();

        try {

            fOutFile = new File(xmlDataDirPath, "060-ProductDistinguishingFeature.xml");
            if (fOutFile.createNewFile()) {
                bwOutFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fOutFile), "UTF-8"));
                writeXmlHeader(bwOutFile);
                Map productFeatureSequenceMap = new HashMap();
                for (int i = 0; i < dataRows.size(); i++) {
                    Map mRow = (Map) dataRows.get(i);
                    masterProductId = (String) mRow.get("masterProductId");
                    variantProductId = (String) mRow.get("productId");
                    mFeatureTypeMap.clear();
                    int totDescriptiveFeatures = 5;
                    if (UtilValidate.isNotEmpty(mRow.get("totDescriptiveFeatures"))) {
                        totDescriptiveFeatures = Integer.parseInt((String) mRow.get("totDescriptiveFeatures"));
                    }

                    for (int j = 1; j <= totDescriptiveFeatures; j++) {
                        buildFeatureMap(mFeatureTypeMap, (String) mRow.get("descriptiveFeature_" + j), _delegator);
                    }

                    if (mFeatureTypeMap.size() > 0) {
                        Set featureTypeSet = mFeatureTypeMap.keySet();
                        Iterator iterFeatureType = featureTypeSet.iterator();
                        while (iterFeatureType.hasNext()) {
                            String featureType = (String) iterFeatureType.next();
                            String featureTypeId = StringUtil.removeSpaces(featureType).toUpperCase();


                            if (featureTypeId.length() > 20) {
                                featureTypeId = featureTypeId.substring(0, 20);
                            }
                            Map mFeatureMap = (Map) mFeatureTypeMap.get(featureType);
                            Set featureSet = mFeatureMap.keySet();
                            Iterator iterFeature = featureSet.iterator();
                            int iSeq = 0;
                            while (iterFeature.hasNext()) {
                                String featureId = (String) iterFeature.next();
                                String featureValue = (String) mFeatureMap.get(featureId);
	                			/*String featureId =StringUtil.removeSpaces(feature).toUpperCase();
	                			featureId =StringUtil.replaceString(featureId, "&", "");
	                			featureId=featureTypeId+"_"+featureId;
	                			if (featureId.length() > 20)
	                			{
	                				featureId=featureId.substring(0,20);
	                			}*/

                                String featureFromDate = _sdf.format(UtilDateTime.nowTimestamp());
                                if (UtilValidate.isEmpty((String) mRow.get(featureType.trim() + "_fromDate"))) {
                                    List<GenericValue> productFeatureAppls = _delegator.findByAnd("ProductFeatureAppl", UtilMisc.toMap("productId", masterProductId, "productFeatureId", featureId, "productFeatureApplTypeId", "DISTINGUISHING_FEAT"), UtilMisc.toList("-fromDate"), false);
                                    if (UtilValidate.isNotEmpty(productFeatureAppls)) {
                                        productFeatureAppls = EntityUtil.filterByDate(productFeatureAppls);
                                        if (UtilValidate.isNotEmpty(productFeatureAppls)) {
                                            GenericValue productFeatureAppl = EntityUtil.getFirst(productFeatureAppls);
                                            featureFromDate = _sdf.format(new Date(productFeatureAppl.getTimestamp("fromDate").getTime()));
                                        }
                                    }
                                } else {
                                    featureFromDate = (String) mRow.get(featureType.trim() + "_fromDate");
                                    java.util.Date formattedFromDate = validDate(featureFromDate);
                                    featureFromDate = _sdf.format(formattedFromDate);
                                }

                                String sequenceNum = String.valueOf((iSeq + 1) * 10);
                                if (UtilValidate.isNotEmpty((String) mRow.get(featureType.trim() + "_sequenceNum"))) {
                                    sequenceNum = (String) mRow.get(featureType.trim() + "_sequenceNum");
                                    if (UtilValidate.isNotEmpty(sequenceNum)) {
                                        sequenceNum = sequenceNum.trim();
                                        if (!UtilValidate.isInteger(sequenceNum)) {
                                            sequenceNum = "0";
                                        }
                                    }
                                }

                                Map entityFieldMap = new HashMap();
                                rowString.setLength(0);
                                rowString.append("<" + "ProductFeatureAppl" + " ");
                                rowString.append("productId" + "=\"" + masterProductId + "\" ");
                                rowString.append("productFeatureId" + "=\"" + featureId + "\" ");
                                rowString.append("productFeatureApplTypeId" + "=\"" + "DISTINGUISHING_FEAT" + "\" ");
                                rowString.append("fromDate" + "=\"" + featureFromDate + "\" ");
                                if (UtilValidate.isNotEmpty((String) mRow.get(featureType.trim() + "_thruDate"))) {
                                    String featureThruDate = (String) mRow.get(featureType.trim() + "_thruDate");
                                    java.util.Date formattedFromDate = validDate(featureFromDate);
                                    featureThruDate = _sdf.format(formattedFromDate);
                                    rowString.append("thruDate" + "=\"" + featureThruDate + "\" ");
                                }
                                rowString.append("sequenceNum" + "=\"" + sequenceNum + "\" ");
                                rowString.append("/>");

                                entityFieldMap.put("productId", masterProductId);
                                entityFieldMap.put("productFeatureId", featureId);
                                entityFieldMap.put("fromDate", featureFromDate);
                                productFeatureSequenceMap.put(entityFieldMap, featureValue);

                                bwOutFile.write(rowString.toString());
                                bwOutFile.newLine();

                                if (UtilValidate.isNotEmpty(variantProductId) && !(masterProductId.equals(variantProductId))) {
                                    featureFromDate = _sdf.format(UtilDateTime.nowTimestamp());

                                    if (UtilValidate.isEmpty((String) mRow.get(featureType.trim() + "_fromDate"))) {
                                        List<GenericValue> productFeatureAppls = _delegator.findByAnd("ProductFeatureAppl", UtilMisc.toMap("productId", variantProductId, "productFeatureId", featureId, "productFeatureApplTypeId", "DISTINGUISHING_FEAT"), UtilMisc.toList("-fromDate"), false);
                                        if (UtilValidate.isNotEmpty(productFeatureAppls)) {
                                            productFeatureAppls = EntityUtil.filterByDate(productFeatureAppls);
                                            if (UtilValidate.isNotEmpty(productFeatureAppls)) {
                                                GenericValue productFeatureAppl = EntityUtil.getFirst(productFeatureAppls);
                                                featureFromDate = _sdf.format(new Date(productFeatureAppl.getTimestamp("fromDate").getTime()));
                                            }
                                        }
                                    } else {
                                        featureFromDate = (String) mRow.get(featureType.trim() + "_fromDate");
                                        java.util.Date formattedFromDate = validDate(featureFromDate);
                                        featureFromDate = _sdf.format(formattedFromDate);
                                    }

                                    entityFieldMap = new HashMap();
                                    rowString.setLength(0);
                                    rowString.append("<" + "ProductFeatureAppl" + " ");
                                    rowString.append("productId" + "=\"" + variantProductId + "\" ");
                                    rowString.append("productFeatureId" + "=\"" + featureId + "\" ");
                                    rowString.append("productFeatureApplTypeId" + "=\"" + "DISTINGUISHING_FEAT" + "\" ");
                                    rowString.append("fromDate" + "=\"" + featureFromDate + "\" ");
                                    if (UtilValidate.isNotEmpty((String) mRow.get(featureType.trim() + "_thruDate"))) {
                                        String featureThruDate = (String) mRow.get(featureType.trim() + "_thruDate");
                                        java.util.Date formattedFromDate = validDate(featureThruDate);
                                        featureThruDate = _sdf.format(formattedFromDate);
                                        rowString.append("thruDate" + "=\"" + featureThruDate + "\" ");
                                    }
                                    rowString.append("sequenceNum" + "=\"" + sequenceNum + "\" ");
                                    rowString.append("/>");

                                    entityFieldMap.put("productId", variantProductId);
                                    entityFieldMap.put("productFeatureId", featureId);
                                    entityFieldMap.put("fromDate", featureFromDate);
                                    productFeatureSequenceMap.put(entityFieldMap, featureValue);
                                    bwOutFile.write(rowString.toString());
                                    bwOutFile.newLine();
                                    iSeq++;
                                }
                            }
                        }
                    }
                }
                if (UtilValidate.isNotEmpty(productFeatureSequenceMap)) {
                    buildFeatureSequence(rowString, bwOutFile, productFeatureSequenceMap, "ProductFeatureAppl");
                }
                bwOutFile.flush();
                writeXmlFooter(bwOutFile);
            }

        } catch (Exception e) {
            Debug.logError(e, TihCatalogImport.module);
        } finally {
            try {
                if (bwOutFile != null) {
                    bwOutFile.close();
                }
            } catch (IOException ioe) {
                Debug.logError(ioe, TihCatalogImport.module);
            }
        }
    }

    public static void buildProductContent(List dataRows, String xmlDataDirPath, String loadImagesDirPath, String imageUrl, String localeString, Delegator _delegator) {
        File fOutFile = null;
        BufferedWriter bwOutFile = null;
        String masterProductId = null;
        String productId = null;
        try {

            fOutFile = new File(xmlDataDirPath, "050-ProductContent.xml");
            if (fOutFile.createNewFile()) {
                bwOutFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fOutFile), "UTF-8"));

                writeXmlHeader(bwOutFile);

                for (int i = 0; i < dataRows.size(); i++) {
                    StringBuilder rowString = new StringBuilder();
                    Map mRow = (Map) dataRows.get(i);
                    masterProductId = (String) mRow.get("masterProductId");
                    productId = (String) mRow.get("productId");
                    if ((UtilValidate.isEmpty(productId)) || (UtilValidate.isNotEmpty(productId) && masterProductId.equals(productId))) {
                        addProductContent(rowString, mRow, bwOutFile, masterProductId, loadImagesDirPath, imageUrl, localeString, _delegator);
                    }
                }
                bwOutFile.flush();
                writeXmlFooter(bwOutFile);
            }
        } catch (Exception e) {
            Debug.logError(e, TihCatalogImport.module);
        } finally {
            try {
                if (bwOutFile != null) {
                    bwOutFile.close();
                }
            } catch (IOException ioe) {
                Debug.logError(ioe, TihCatalogImport.module);
            }
        }
    }

    private static void addProductContent(StringBuilder rowString, Map mRow, BufferedWriter bwOutFile, String productId, String loadImagesDirPath, String imageUrl, String localeString, Delegator _delegator) {

        try {
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "SMALL_IMAGE_URL", "smallImage", loadImagesDirPath, imageUrl, "smallImageThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "SMALL_IMAGE_ALT_URL", "smallImageAlt", loadImagesDirPath, imageUrl, "smallImageAltThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "PLP_SWATCH_IMAGE_URL", "plpSwatchImage", loadImagesDirPath, imageUrl, "plpSwatchImageThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "PDP_SWATCH_IMAGE_URL", "pdpSwatchImage", loadImagesDirPath, imageUrl, "pdpSwatchImageThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "THUMBNAIL_IMAGE_URL", "thumbImage", loadImagesDirPath, imageUrl, "thumbImageThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "LARGE_IMAGE_URL", "largeImage", loadImagesDirPath, imageUrl, "largeImageThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "DETAIL_IMAGE_URL", "detailImage", loadImagesDirPath, imageUrl, "detailImageThruDate", localeString, _delegator);

            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "ADDITIONAL_IMAGE_1", "addImage1", loadImagesDirPath, imageUrl, "addImage1ThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "XTRA_IMG_1_LARGE", "xtraLargeImage1", loadImagesDirPath, imageUrl, "xtraLargeImage1ThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "XTRA_IMG_1_DETAIL", "xtraDetailImage1", loadImagesDirPath, imageUrl, "xtraDetailImage1ThruDate", localeString, _delegator);

            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "ADDITIONAL_IMAGE_2", "addImage2", loadImagesDirPath, imageUrl, "addImage2ThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "XTRA_IMG_2_LARGE", "xtraLargeImage2", loadImagesDirPath, imageUrl, "xtraLargeImage2ThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "XTRA_IMG_2_DETAIL", "xtraDetailImage2", loadImagesDirPath, imageUrl, "xtraDetailImage2ThruDate", localeString, _delegator);

            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "ADDITIONAL_IMAGE_3", "addImage3", loadImagesDirPath, imageUrl, "addImage3ThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "XTRA_IMG_3_LARGE", "xtraLargeImage3", loadImagesDirPath, imageUrl, "xtraLargeImage3ThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "XTRA_IMG_3_DETAIL", "xtraDetailImage3", loadImagesDirPath, imageUrl, "xtraDetailImage3ThruDate", localeString, _delegator);

            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "ADDITIONAL_IMAGE_4", "addImage4", loadImagesDirPath, imageUrl, "addImage4ThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "XTRA_IMG_4_LARGE", "xtraLargeImage4", loadImagesDirPath, imageUrl, "xtraLargeImage4ThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "XTRA_IMG_4_DETAIL", "xtraDetailImage4", loadImagesDirPath, imageUrl, "xtraDetailImage4ThruDate", localeString, _delegator);

            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "ADDITIONAL_IMAGE_5", "addImage5", loadImagesDirPath, imageUrl, "addImage5ThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "XTRA_IMG_5_LARGE", "xtraLargeImage5", loadImagesDirPath, imageUrl, "xtraLargeImage5ThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "XTRA_IMG_5_DETAIL", "xtraDetailImage5", loadImagesDirPath, imageUrl, "xtraDetailImage5ThruDate", localeString, _delegator);

            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "ADDITIONAL_IMAGE_6", "addImage6", loadImagesDirPath, imageUrl, "addImage6ThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "XTRA_IMG_6_LARGE", "xtraLargeImage6", loadImagesDirPath, imageUrl, "xtraLargeImage6ThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "XTRA_IMG_6_DETAIL", "xtraDetailImage6", loadImagesDirPath, imageUrl, "xtraDetailImage6ThruDate", localeString, _delegator);

            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "ADDITIONAL_IMAGE_7", "addImage7", loadImagesDirPath, imageUrl, "addImage7ThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "XTRA_IMG_7_LARGE", "xtraLargeImage7", loadImagesDirPath, imageUrl, "xtraLargeImage7ThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "XTRA_IMG_7_DETAIL", "xtraDetailImage7", loadImagesDirPath, imageUrl, "xtraDetailImage7ThruDate", localeString, _delegator);

            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "ADDITIONAL_IMAGE_8", "addImage8", loadImagesDirPath, imageUrl, "addImage8ThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "XTRA_IMG_8_LARGE", "xtraLargeImage8", loadImagesDirPath, imageUrl, "xtraLargeImage8ThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "XTRA_IMG_8_DETAIL", "xtraDetailImage8", loadImagesDirPath, imageUrl, "xtraDetailImage8ThruDate", localeString, _delegator);

            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "ADDITIONAL_IMAGE_9", "addImage9", loadImagesDirPath, imageUrl, "addImage9ThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "XTRA_IMG_9_LARGE", "xtraLargeImage9", loadImagesDirPath, imageUrl, "xtraLargeImage9ThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "XTRA_IMG_9_DETAIL", "xtraDetailImage9", loadImagesDirPath, imageUrl, "xtraDetailImage9ThruDate", localeString, _delegator);

            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "ADDITIONAL_IMAGE_10", "addImage10", loadImagesDirPath, imageUrl, "addImage10ThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "XTRA_IMG_10_LARGE", "xtraLargeImage10", loadImagesDirPath, imageUrl, "xtraLargeImag10ThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "XTRA_IMG_10_DETAIL", "xtraDetailImage10", loadImagesDirPath, imageUrl, "xtraDetailImage10ThruDate", localeString, _delegator);

            addProductContentRow(rowString, mRow, bwOutFile, productId, "text", "PRODUCT_NAME", "productName", loadImagesDirPath, imageUrl, "productNameThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "text", "SHORT_SALES_PITCH", "salesPitch", loadImagesDirPath, imageUrl, "salesPitchThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "text", "LONG_DESCRIPTION", "longDescription", loadImagesDirPath, imageUrl, "longDescriptionThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "text", "SPECIALINSTRUCTIONS", "specialInstructions", loadImagesDirPath, imageUrl, "specialInstructionsThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "text", "DELIVERY_INFO", "deliveryInfo", loadImagesDirPath, imageUrl, "deliveryInfoThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "text", "DIRECTIONS", "directions", loadImagesDirPath, imageUrl, "smallImageAltThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "text", "TERMS_AND_CONDS", "termsConditions", loadImagesDirPath, imageUrl, "smallImageAltThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "text", "INGREDIENTS", "ingredients", loadImagesDirPath, imageUrl, "termsConditionsThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "text", "WARNINGS", "warnings", loadImagesDirPath, imageUrl, "warningsThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "text", "PLP_LABEL", "plpLabel", loadImagesDirPath, imageUrl, "plpLabelThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "text", "PDP_LABEL", "pdpLabel", loadImagesDirPath, imageUrl, "pdpLabelThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "PDP_VIDEO_URL", "pdpVideoUrl", loadImagesDirPath, imageUrl, "pdpVideoUrlThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "PDP_VIDEO_360_URL", "pdpVideo360Url", loadImagesDirPath, imageUrl, "pdpVideo360UrlThruDate", localeString, _delegator);

            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "ATTACH_URL_01", "productAttachment1", loadImagesDirPath, imageUrl, "productAttachment1ThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "ATTACH_URL_02", "productAttachment2", loadImagesDirPath, imageUrl, "productAttachment2ThruDate", localeString, _delegator);
            addProductContentRow(rowString, mRow, bwOutFile, productId, "image", "ATTACH_URL_03", "productAttachment3", loadImagesDirPath, imageUrl, "productAttachment3ThruDate", localeString, _delegator);

        } catch (Exception e) {
            Debug.logError(e, TihCatalogImport.module);
        }
    }

    private static void addProductContentRow(StringBuilder rowString, Map mRow, BufferedWriter bwOutFile, String productId, String contentType, String productContentTypeId, String colName, String productImagesDirPath, String imageUrl, String colNameThruDate, String localeString, Delegator _delegator) {

        String contentId = null;
        String dataResourceId = null;
        Timestamp contentTimestamp = null;
        try {

            String contentValue = mRow.get(colName) != null ? (String) mRow.get(colName) : "";
            if (UtilValidate.isEmpty(contentValue) && UtilValidate.isEmpty(contentValue.trim())) {
                return;
            }
            String contentValueThruDate = (String) mRow.get(colNameThruDate);
            List<GenericValue> lProductContent = _delegator.findByAnd("ProductContent", UtilMisc.toMap("productId", productId, "productContentTypeId", productContentTypeId), UtilMisc.toList("-fromDate"), false);
            if (UtilValidate.isNotEmpty(lProductContent)) {
                GenericValue productContent = EntityUtil.getFirst(lProductContent);
                GenericValue content = productContent.getRelatedOne("Content");
                contentId = content.getString("contentId");
                dataResourceId = content.getString("dataResourceId");
                contentTimestamp = productContent.getTimestamp("fromDate");
            } else {
                contentId = _delegator.getNextSeqId("Content");
                dataResourceId = _delegator.getNextSeqId("DataResource");
                contentTimestamp = UtilDateTime.nowTimestamp();
            }

            if ("text".equals(contentType)) {
                rowString.setLength(0);
                rowString.append("<" + "DataResource" + " ");
                rowString.append("dataResourceId" + "=\"" + dataResourceId + "\" ");
                rowString.append("dataResourceTypeId" + "=\"" + "ELECTRONIC_TEXT" + "\" ");
                rowString.append("dataTemplateTypeId" + "=\"" + "FTL" + "\" ");
                rowString.append("statusId" + "=\"" + "CTNT_PUBLISHED" + "\" ");
                rowString.append("dataResourceName" + "=\"" + colName + "\" ");
                if (UtilValidate.isNotEmpty(localeString)) {
                    rowString.append("localeString" + "=\"" + localeString + "\" ");
                }
                rowString.append("mimeTypeId" + "=\"" + "application/octet-stream" + "\" ");
                rowString.append("objectInfo" + "=\"" + "" + "\" ");
                rowString.append("isPublic" + "=\"" + "Y" + "\" ");
                rowString.append("/>");
                bwOutFile.write(rowString.toString());
                bwOutFile.newLine();

                rowString.setLength(0);
                rowString.append("<" + "ElectronicText" + " ");
                rowString.append("dataResourceId" + "=\"" + dataResourceId + "\" ");

                rowString.append("textData" + "=\"" + contentValue + "\" ");
                rowString.append("/>");
                bwOutFile.write(rowString.toString());
                bwOutFile.newLine();


            } else {
                rowString.setLength(0);
                rowString.append("<" + "DataResource" + " ");
                rowString.append("dataResourceId" + "=\"" + dataResourceId + "\" ");
                rowString.append("dataResourceTypeId" + "=\"" + "SHORT_TEXT" + "\" ");
                rowString.append("dataTemplateTypeId" + "=\"" + "FTL" + "\" ");
                rowString.append("statusId" + "=\"" + "CTNT_PUBLISHED" + "\" ");
                rowString.append("dataResourceName" + "=\"" + contentValue + "\" ");
                if (UtilValidate.isNotEmpty(localeString)) {
                    rowString.append("localeString" + "=\"" + localeString + "\" ");
                }
                rowString.append("mimeTypeId" + "=\"" + "text/html" + "\" ");

                if (!UtilValidate.isUrl(contentValue)) {

                    if (UtilValidate.isNotEmpty(DEFAULT_IMAGE_DIRECTORY)) {
                        contentValue = DEFAULT_IMAGE_DIRECTORY + contentValue;
                    }
                }

                rowString.append("objectInfo" + "=\"" + contentValue.trim() + "\" ");
                rowString.append("isPublic" + "=\"" + "Y" + "\" ");
                rowString.append("/>");
                bwOutFile.write(rowString.toString());
                bwOutFile.newLine();
            }

            rowString.setLength(0);
            rowString.append("<" + "Content" + " ");
            rowString.append("contentId" + "=\"" + contentId + "\" ");
            rowString.append("contentTypeId" + "=\"" + "DOCUMENT" + "\" ");
            rowString.append("dataResourceId" + "=\"" + dataResourceId + "\" ");
            rowString.append("statusId" + "=\"" + "CTNT_PUBLISHED" + "\" ");
            rowString.append("contentName" + "=\"" + colName + "\" ");
            if (UtilValidate.isNotEmpty(localeString)) {
                rowString.append("localeString" + "=\"" + localeString + "\" ");
            }
            rowString.append("/>");
            bwOutFile.write(rowString.toString());
            bwOutFile.newLine();

            rowString.setLength(0);
            rowString.append("<" + "ProductContent" + " ");
            rowString.append("productId" + "=\"" + productId + "\" ");
            rowString.append("contentId" + "=\"" + contentId + "\" ");
            rowString.append("productContentTypeId" + "=\"" + productContentTypeId + "\" ");
            rowString.append("fromDate" + "=\"" + _sdf.format(contentTimestamp) + "\" ");
            if (UtilValidate.isNotEmpty(contentValueThruDate)) {
                java.util.Date formattedThuDate = validDate(contentValueThruDate);
                contentValueThruDate = _sdf.format(formattedThuDate);
                rowString.append("thruDate" + "=\"" + contentValueThruDate + "\" ");
            } else {
                rowString.append("thruDate" + "=\"" + null + "\" ");
            }
            rowString.append("/>");
            bwOutFile.write(rowString.toString());
            bwOutFile.newLine();

        } catch (Exception e) {
            Debug.logError(e, TihCatalogImport.module);
        }

        return;

    }

    private static void buildFeatureSequence(StringBuilder rowString, BufferedWriter bwOutFile, Map productFeatureSequenceMap, String entityName) {
        List<Map.Entry> productFeatureSequenceMapSort = new ArrayList<Map.Entry>(productFeatureSequenceMap.entrySet());
        Collections.sort(productFeatureSequenceMapSort,
                new Comparator() {
                    public int compare(Object firstObjToCompare, Object secondObjToCompare) {
                        Map.Entry e1 = (Map.Entry) firstObjToCompare;
                        Map.Entry e2 = (Map.Entry) secondObjToCompare;

                        return alphaNumericSort(e1.getValue().toString(), e2.getValue().toString());
                    }
                });

        int iSeq = 1;
        for (Map.Entry entry : productFeatureSequenceMapSort) {
            try {
                Map<String, String> entityFieldMap = (Map) entry.getKey();

                rowString.setLength(0);
                rowString.append("<" + entityName + " ");
                for (Map.Entry<String, String> entityMap : entityFieldMap.entrySet()) {
                    String entityFieldName = entityMap.getKey();
                    String entityFieldValue = entityFieldMap.get(entityFieldName);
                    rowString.append(entityFieldName + "=\"" + entityFieldValue + "\" ");
                }
                rowString.append("sequenceNum" + "=\"" + (iSeq * 10) + "\" ");
                rowString.append("/>");
                bwOutFile.write(rowString.toString());
                bwOutFile.newLine();
            } catch (Exception e) {
                Debug.logError(e, TihCatalogImport.module);
            }
            iSeq++;
        }
    }

    public static void buildProductVariantContent(List dataRows, String xmlDataDirPath, String loadImagesDirPath, String imageUrl, String localeString, Delegator _delegator) {
        File fOutFile = null;
        BufferedWriter bwOutFile = null;
        try {

            fOutFile = new File(xmlDataDirPath, "055-ProductVariantContent.xml");
            if (fOutFile.createNewFile()) {
                bwOutFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fOutFile), "UTF-8"));

                writeXmlHeader(bwOutFile);

                for (int i = 0; i < dataRows.size(); i++) {
                    StringBuilder rowString = new StringBuilder();
                    Map mRow = (Map) dataRows.get(i);

                    String masterProductId = (String) mRow.get("masterProductId");
                    String productId = (String) mRow.get("productId");
                    if (UtilValidate.isNotEmpty(productId) && !productId.equals(masterProductId)) {
                        addProductContent(rowString, mRow, bwOutFile, productId, loadImagesDirPath, imageUrl, localeString, _delegator);
                    }
                }
                bwOutFile.flush();
                writeXmlFooter(bwOutFile);
            }


        } catch (Exception e) {
            Debug.logError(e, TihCatalogImport.module);
        } finally {
            try {
                if (bwOutFile != null) {
                    bwOutFile.close();
                }
            } catch (IOException ioe) {
                Debug.logError(ioe, TihCatalogImport.module);
            }
        }
    }

    public static void clearMaps() {
        featureTypeIdMap.clear();
        sFeatureGroupExists.clear();
        mFeatureValueExists.clear();
        mProductFeatureCatGrpApplFromDateExists.clear();
        mProductFeatureCategoryApplFromDateExists.clear();
        mProductFeatureGroupApplFromDateExists.clear();
    }

    private static Map<String, String> productFeatureExistsInDB(String featureType, String featDesc, Delegator _delegator) {
        Map<String, String> resultsMap = new HashMap<>();
        try {
            String productFeatureTypeId = StringUtil.removeSpaces(featureType).toUpperCase();
            if (productFeatureTypeId.length() > 20) {
                productFeatureTypeId = productFeatureTypeId.substring(0, 20);
            }
            List<GenericValue> productFeatureList = _delegator.findByAnd("ProductFeature", UtilMisc.toMap("productFeatureTypeId", productFeatureTypeId, "productFeatureCategoryId", productFeatureTypeId), null, false);
            if (UtilValidate.isNotEmpty(productFeatureList)) {
                for (GenericValue productFeatureValue : productFeatureList) {
                    if (UtilValidate.isNotEmpty(productFeatureValue.getString("description"))) {
                        if (StringEscapeUtils.unescapeHtml(productFeatureValue.getString("description").toUpperCase()).equals(StringEscapeUtils.unescapeHtml(featDesc.trim().toUpperCase()))) {
                            resultsMap.put("existsInDB", "Y");
                            resultsMap.put("featureId", productFeatureValue.getString("productFeatureId"));
                            resultsMap.put("featureTypeKey", StringUtil.removeSpaces(featureType).toUpperCase() + "~" + productFeatureValue.getString("description"));
                            resultsMap.put("description", productFeatureValue.getString("description"));
                            break;
                        }
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, TihCatalogImport.module);
            e.printStackTrace();
        }
        return resultsMap;
    }


    private static void writeXmlHeader(BufferedWriter bfOutFile) {
        try {
            bfOutFile.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            bfOutFile.newLine();
            bfOutFile.write("<entity-engine-xml>");
            bfOutFile.newLine();
            bfOutFile.flush();


        } catch (Exception e) {
        }
    }

    private static void writeXmlFooter(BufferedWriter bfOutFile) {
        try {
            bfOutFile.write("</entity-engine-xml>");
            bfOutFile.flush();
            bfOutFile.close();

        } catch (Exception e) {
        }
    }

    /**
     * process the object and rtuen the string.
     *
     * @param tsObj object
     * @return a String.
     */
    public static String getString(Object tsObj) {
        if (UtilValidate.isNotEmpty(tsObj)) {
            return tsObj.toString();
        } else {
            return "";
        }
    }

    /**
     * process the object and return the formatted time.
     *
     * @param tsObj object
     * @return a formatted time String.
     */
    private static String formatDate(Object tsObj) {
        if (UtilValidate.isNotEmpty(tsObj)) {
            return _sdf.format(new Date(((Timestamp) tsObj).getTime()));
        } else {
            return "";
        }
    }

    private static String formatBigDecimal(Object bdObj) {
        if (UtilValidate.isNotEmpty(bdObj)) {
            return _df.format((BigDecimal) bdObj);
        } else {
            return "";
        }
    }

    /*Returns the value 0 if the first string is equal to second string;
  a value less than 0 if first string is alphanumerically less than the second string ;
  and a value greater than 0 if first string is alphanumerically greater than the second string. */
    public static int alphaNumericSort(String firstStrToCompare, String secondStrToCompare) {
        String firstString = firstStrToCompare;
        String secondString = secondStrToCompare;

        if (UtilValidate.isEmpty(secondString) || UtilValidate.isEmpty(firstString)) {
            return 0;
        }

        int lengthFirstStr = firstString.length();
        int lengthSecondStr = secondString.length();

        int index1 = 0;
        int index2 = 0;

        while (index1 < lengthFirstStr && index2 < lengthSecondStr) {
            char ch1 = firstString.charAt(index1);
            char ch2 = secondString.charAt(index2);

            char[] space1 = new char[lengthFirstStr];
            char[] space2 = new char[lengthSecondStr];

            int loc1 = 0;
            int loc2 = 0;

            //Create Two Character Sequence 'space1' and 'space2' of Same Type either Alphabetic or Numeric
            space1[0] = ch1;
            while (index1 < lengthFirstStr) {
                if (Character.isDigit(ch1) == Character.isDigit(space1[0])) {
                    space1[loc1++] = ch1;
                    index1++;
                    if (index1 < lengthFirstStr) {
                        ch1 = firstString.charAt(index1);
                    }
                } else {
                    break;
                }
            }
            space2[0] = ch2;
            while (index2 < lengthSecondStr) {
                if (Character.isDigit(ch2) == Character.isDigit(space2[0])) {
                    space2[loc2++] = ch2;
                    index2++;
                    if (index2 < lengthSecondStr) {
                        ch2 = secondString.charAt(index2);
                    }
                } else {
                    break;
                }
            }

            //Build Two Strings 'str1' and 'str2' of Same Type either Alphabetic or Numeric from Character Sequence
            String str1 = new String(space1);
            String str2 = new String(space2);

            int result;

            //If both Character Sequences starts with numeric value then convert the same type of String to the Integer value and then Compare
            if (Character.isDigit(space1[0]) && Character.isDigit(space2[0])) {
                Integer firstNumberToCompare = new Integer(Integer.parseInt(str1.trim()));
                Integer secondNumberToCompare = new Integer(Integer.parseInt(str2.trim()));
                result = firstNumberToCompare.compareTo(secondNumberToCompare);
            } else {
                //Compare the Same Type of String 'str1' and 'str2'
                result = str1.trim().compareTo(str2.trim());
            }
            /*If both Same type of String 'str1' and 'str2' are equal then continue to the outer loop with the next index position.
            else return the positive or negative integer value */
            if (result != 0) {
                return result;
            }
        }
        return lengthFirstStr - lengthSecondStr;
    }

    public static java.util.Date validDate(String date) {
        java.util.Date formattedDate = new java.util.Date();

        try {
            _sdf.setLenient(false);
            formattedDate = _sdf.parse(date);
            return formattedDate;
        } catch (ParseException pe) {
        }

        return formattedDate;
    }
}
