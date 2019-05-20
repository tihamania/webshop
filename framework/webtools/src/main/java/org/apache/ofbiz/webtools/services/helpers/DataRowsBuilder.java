package org.apache.ofbiz.webtools.services.helpers;

import jxl.Cell;
import jxl.Sheet;
import org.apache.ofbiz.base.util.*;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.webtools.services.Constants;
import org.apache.ofbiz.webtools.services.model.ListPriceType;
import org.apache.ofbiz.webtools.services.TihCatalogImport;
import org.apache.ofbiz.webtools.services.model.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataRowsBuilder {


    public static List buildProductCategoryXMLDataRows(List<CategoryType> productCategories) {
        List dataRows = new ArrayList();

        try {

            for (int rowCount = 0; rowCount < productCategories.size(); rowCount++) {
                CategoryType productCategory = (CategoryType) productCategories.get(rowCount);

                Map mRows = new HashMap();

                mRows.put("productCategoryId", productCategory.getCategoryId());
                mRows.put("parentCategoryId", productCategory.getParentCategoryId());
                mRows.put("categoryName", productCategory.getCategoryName());
                mRows.put("description", productCategory.getDescription());
                mRows.put("longDescription", productCategory.getLongDescription());
                mRows.put("plpText", productCategory.getAdditionalPlpText());
                mRows.put("pdpText", productCategory.getAdditionalPdpText());
                mRows.put("fromDate", productCategory.getFromDate());
                mRows.put("thruDate", productCategory.getThruDate());

                PlpImageType plpImage = productCategory.getPlpImage();
                if (UtilValidate.isNotEmpty(plpImage)) {
                    mRows.put("plpImageName", plpImage.getUrl());
                }

                mRows = formatProductXLSData(mRows);
                dataRows.add(mRows);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dataRows;
    }

    /**
     * process the XLS sheet and build the product data rows
     *
     * @param s XLS sheet object
     * @return a List of Map.
     */
    public static List<Map<String, Object>> buildProductDataRows(Sheet s) {
        List<Map<String, Object>> dataRows = new ArrayList();

        try {
            List xlsDataRows = buildDataRows(buildProductHeader(), s);
            for (int i = 0; i < xlsDataRows.size(); i++) {
                Map mRow = (Map) xlsDataRows.get(i);
                if (mRow.get("masterProductId").equals(mRow.get("productId")) || UtilValidate.isEmpty(mRow.get("productId"))) {
                    //CREATE VIRTUAL/FINISHED GOOD PRODUCT ROW
                    String productId = (String) mRow.get("productId");
                    Map<String, Object> dataRow = buildProductDataRow(mRow, productId, null);
                    if (UtilValidate.isNotEmpty(dataRow)) {
                        dataRows.add(dataRow);
                    }

                } else {
                    List<List> selectableFeatureList = new ArrayList();
                    int totSelectableFeatures = 5;
                    for (int j = 1; j <= totSelectableFeatures; j++) {
                        selectableFeatureList = createFeatureVariantProductId(selectableFeatureList, (String) mRow.get("selectabeFeature_" + j));
                    }
                    if (selectableFeatureList.size() == 1) {
                        //CREATE ONE VARIANT PRODUCT ROW
                        String productId = (String) mRow.get("productId");
                        List<String> selectableFeature = (List) selectableFeatureList.get(0);
                        Map<String, Object> dataRow = buildProductDataRow(mRow, productId, selectableFeature);
                        if (UtilValidate.isNotEmpty(dataRow)) {
                            dataRows.add(dataRow);
                        }
                    } else if (selectableFeatureList.size() > 1) {
                        //CREATE MULTIPLE VARIANT PRODUCT ROW
                        int variantProductIdNo = 1;
                        for (List selectableFeature : selectableFeatureList) {
                            String variantProductId = (String) mRow.get("productId");
                            if (variantProductIdNo < 10) {
                                variantProductId = variantProductId + "-0" + variantProductIdNo;
                            } else {
                                variantProductId = variantProductId + "-" + variantProductIdNo;
                            }
                            Map<String, Object> dataRow = buildProductDataRow(mRow, variantProductId, selectableFeature);
                            if (UtilValidate.isNotEmpty(dataRow)) {
                                dataRows.add(dataRow);
                            }
                            variantProductIdNo++;
                        }
                    }

                }
            }
        } catch (Exception e) {
            Debug.logError(e, TihCatalogImport.module);
        }
        return dataRows;
    }

    /**
     * process the XLS sheet and build the category data rows
     *
     * @param s XLS sheet object
     * @return a List of Map.
     */
    public static List<Map<String, Object>> buildProductCategoryDataRows(Sheet s) {
        List<Map<String, Object>> dataRows = new ArrayList();

        try {
            List xlsDataRows = buildDataRows(buildCategoryHeader(), s);
            for (int i = 0; i < xlsDataRows.size(); i++) {
                Map<String, Object> dataRow = new HashMap();
                Map mRow = (Map) xlsDataRows.get(i);
                dataRow.put(Constants.CATEGORY_ID_DATA_KEY, mRow.get("productCategoryId"));
                dataRow.put(Constants.CATEGORY_PARENT_DATA_KEY, mRow.get("parentCategoryId"));
                dataRow.put(Constants.CATEGORY_NAME_DATA_KEY, mRow.get("categoryName"));
                dataRow.put(Constants.CATEGORY_DESC_DATA_KEY, mRow.get("description"));
                dataRow.put(Constants.CATEGORY_LONG_DESC_DATA_KEY, mRow.get("longDescription"));
                dataRow.put(Constants.CATEGORY_PLP_TEXT_DATA_KEY, mRow.get("plpText"));
                dataRow.put(Constants.CATEGORY_PDP_TEXT_DATA_KEY, mRow.get("pdpText"));
                dataRow.put(Constants.CATEGORY_PLP_IMG_NAME_DATA_KEY, mRow.get("plpImageName"));
                dataRow.put(Constants.CATEGORY_FROM_DATE_DATA_KEY, new Timestamp(System.currentTimeMillis()));

                if (UtilValidate.isNotEmpty(dataRow)) {
                    dataRows.add(dataRow);
                }
            }
        } catch (Exception e) {
            Debug.logError(e, TihCatalogImport.module);
        }
        return dataRows;
    }

    public static Map<String, Object> buildProductDataRow(Map mRow, String productId, List<String> finalSelectableFeature) {
        Map<String, Object> dataRow = new HashMap<>();
        try {
            String currencyUomId = UtilProperties.getPropertyValue("general.properties", "currency.uom.id.default", "USD");
            dataRow.put(Constants.PRODUCT_MASTER_ID_DATA_KEY, mRow.get("masterProductId"));
            dataRow.put(Constants.PRODUCT_ID_DATA_KEY, productId);

            int cnt = 1;
            String productCategory = (String) mRow.get("productCategoryId");
            String sequenceNum = (String) mRow.get("sequenceNum");
            List<String> productCategoryIds = null;
            if (UtilValidate.isNotEmpty(productCategory)) {
                productCategoryIds = StringUtil.split(productCategory, ",");
            }
            if (UtilValidate.isNotEmpty(productCategoryIds)) {
                StringBuffer catMembers = new StringBuffer();
                for (String productCategoryId : productCategoryIds) {
                    dataRow.put(FlexibleStringExpander.expandString(Constants.PRODUCT_CAT_ID_DATA_KEY, UtilMisc.toMap("count", cnt)), productCategoryId);
                    dataRow.put(FlexibleStringExpander.expandString(Constants.PRODUCT_CAT_SEQ_NUM_DATA_KEY, UtilMisc.toMap("count", cnt)), sequenceNum);
                    dataRow.put(FlexibleStringExpander.expandString(Constants.PRODUCT_CAT_FROM_DATE_DATA_KEY, UtilMisc.toMap("count", cnt)), "");
                    dataRow.put(FlexibleStringExpander.expandString(Constants.PRODUCT_CAT_THRU_DATE_DATA_KEY, UtilMisc.toMap("count", cnt)), "");
                    cnt++;
                }
                dataRow.put(Constants.PRODUCT_CAT_COUNT_DATA_KEY, cnt - 1);
            }

            dataRow.put(Constants.PRODUCT_INTERNAL_NAME_DATA_KEY, mRow.get("internalName"));
            if (UtilValidate.isNotEmpty(mRow.get("productWidth"))) {
                dataRow.put(Constants.PRODUCT_WIDTH_DATA_KEY, BigDecimal.valueOf(UtilMisc.toDouble(mRow.get("productWidth"))));
            }
            if (UtilValidate.isNotEmpty(mRow.get("productHeight"))) {
                dataRow.put(Constants.PRODUCT_HEIGHT_DATA_KEY, BigDecimal.valueOf(UtilMisc.toDouble(mRow.get("productHeight"))));
            }
            if (UtilValidate.isNotEmpty(mRow.get("productDepth"))) {
                dataRow.put(Constants.PRODUCT_DEPTH_DATA_KEY, BigDecimal.valueOf(UtilMisc.toDouble(mRow.get("productDepth"))));
            }
            if (UtilValidate.isNotEmpty(mRow.get("weight"))) {
                dataRow.put(Constants.PRODUCT_WEIGHT_DATA_KEY, BigDecimal.valueOf(UtilMisc.toDouble(mRow.get("weight"))));
            }
            dataRow.put(Constants.PRODUCT_RETURN_ABLE_DATA_KEY, mRow.get("returnable"));
            dataRow.put(Constants.PRODUCT_TAX_ABLE_DATA_KEY, mRow.get("taxable"));
            dataRow.put(Constants.PRODUCT_CHARGE_SHIP_DATA_KEY, mRow.get("chargeShipping"));
            dataRow.put(Constants.PRODUCT_INTRO_DATE_DATA_KEY, new Timestamp(System.currentTimeMillis()));

            dataRow.put(Constants.PRODUCT_MANUFACT_PARTY_ID_DATA_KEY, mRow.get("manufacturerId"));

            dataRow.put(Constants.PRODUCT_NAME_DATA_KEY, mRow.get("productName"));
            dataRow.put(Constants.PRODUCT_SALES_PITCH_DATA_KEY, mRow.get("salesPitch"));
            dataRow.put(Constants.PRODUCT_LONG_DESC_DATA_KEY, mRow.get("longDescription"));
            dataRow.put(Constants.PRODUCT_SPCL_INS_DATA_KEY, mRow.get("specialInstructions"));
            dataRow.put(Constants.PRODUCT_DELIVERY_INFO_DATA_KEY, mRow.get("deliveryInfo"));
            dataRow.put(Constants.PRODUCT_DIRECTIONS_DATA_KEY, mRow.get("directions"));
            dataRow.put(Constants.PRODUCT_TERMS_COND_DATA_KEY, mRow.get("termsConditions"));
            dataRow.put(Constants.PRODUCT_INGREDIENTS_DATA_KEY, mRow.get("ingredients"));
            dataRow.put(Constants.PRODUCT_WARNING_DATA_KEY, mRow.get("warnings"));
            dataRow.put(Constants.PRODUCT_PLP_LABEL_DATA_KEY, mRow.get("plpLabel"));
            dataRow.put(Constants.PRODUCT_PDP_LABEL_DATA_KEY, mRow.get("pdpLabel"));

            if (UtilValidate.isNotEmpty(mRow.get("listPrice"))) {
                dataRow.put(Constants.PRODUCT_LIST_PRICE_DATA_KEY, BigDecimal.valueOf(UtilMisc.toDouble(mRow.get("listPrice"))));
                dataRow.put(Constants.PRODUCT_LIST_PRICE_CUR_DATA_KEY, currencyUomId);
                dataRow.put(Constants.PRODUCT_LIST_PRICE_FROM_DATA_KEY, "");
                dataRow.put(Constants.PRODUCT_LIST_PRICE_THRU_DATA_KEY, "");
            }

            if (UtilValidate.isNotEmpty(mRow.get("defaultPrice"))) {
                dataRow.put(Constants.PRODUCT_DEFAULT_PRICE_DATA_KEY, BigDecimal.valueOf(UtilMisc.toDouble(mRow.get("defaultPrice"))));
                dataRow.put(Constants.PRODUCT_DEFAULT_PRICE_CUR_DATA_KEY, currencyUomId);
                dataRow.put(Constants.PRODUCT_DEFAULT_PRICE_FROM_DATA_KEY, "");
                dataRow.put(Constants.PRODUCT_DEFAULT_PRICE_THRU_DATA_KEY, "");
            }

            if (UtilValidate.isNotEmpty(mRow.get("recurringPrice"))) {
                dataRow.put(Constants.PRODUCT_RECURRING_PRICE_DATA_KEY, BigDecimal.valueOf(UtilMisc.toDouble(mRow.get("recurringPrice"))));
                dataRow.put(Constants.PRODUCT_RECURRING_PRICE_CUR_DATA_KEY, currencyUomId);
                dataRow.put(Constants.PRODUCT_RECURRING_PRICE_FROM_DATA_KEY, "");
                dataRow.put(Constants.PRODUCT_RECURRING_PRICE_THRU_DATA_KEY, "");
            }

            if (UtilValidate.isNotEmpty(finalSelectableFeature)) {
                cnt = 1;
                for (String featureValue : finalSelectableFeature) {
                    featureValue = featureValue.trim();
                    String[] featureValueArr = featureValue.split("~");
                    if (featureValueArr.length > 0) {
                        dataRow.put(FlexibleStringExpander.expandString(Constants.PRODUCT_SLCT_FEAT_TYPE_ID_DATA_KEY, UtilMisc.toMap("count", cnt)), featureValueArr[0].trim());
                    }
                    if (featureValueArr.length > 1) {
                        dataRow.put(FlexibleStringExpander.expandString(Constants.PRODUCT_SLCT_FEAT_DESC_DATA_KEY, UtilMisc.toMap("count", cnt)), featureValueArr[1].trim());
                    }
                    dataRow.put(FlexibleStringExpander.expandString(Constants.PRODUCT_SLCT_FEAT_TYPE_DESC_DATA_KEY, UtilMisc.toMap("count", cnt)), "");
                    dataRow.put(FlexibleStringExpander.expandString(Constants.PRODUCT_SLCT_FEAT_SEQ_NUM_DATA_KEY, UtilMisc.toMap("count", cnt)), "");
                    dataRow.put(FlexibleStringExpander.expandString(Constants.PRODUCT_SLCT_FEAT_FROM_DATA_KEY, UtilMisc.toMap("count", cnt)), "");
                    dataRow.put(FlexibleStringExpander.expandString(Constants.PRODUCT_SLCT_FEAT_THRU_DATA_KEY, UtilMisc.toMap("count", cnt)), "");
                    cnt++;
                }
            }

            cnt = 1;
            int totDescriptiveFeatures = 5;
            for (int j = 1; j <= totDescriptiveFeatures; j++) {

                String parseFeatureType = (String) mRow.get("descriptiveFeature_" + j);
                if (UtilValidate.isNotEmpty(parseFeatureType)) {
                    int iFeatIdx = parseFeatureType.indexOf(':');
                    if (iFeatIdx > -1) {
                        String featureType = parseFeatureType.substring(0, iFeatIdx).trim();
                        dataRow.put(FlexibleStringExpander.expandString(Constants.PRODUCT_DESC_FEAT_TYPE_ID_DATA_KEY, UtilMisc.toMap("count", cnt)), featureType);

                        String sFeatures = parseFeatureType.substring(iFeatIdx + 1);
                        String[] featureTokens = sFeatures.split(",");
                        Map mFeatureMap = new HashMap();
                        for (int f = 0; f < featureTokens.length; f++) {
                            dataRow.put(FlexibleStringExpander.expandString(Constants.PRODUCT_DESC_FEAT_DESC_DATA_KEY, UtilMisc.toMap("count", cnt)), featureTokens[f].trim());
                        }
                        dataRow.put(FlexibleStringExpander.expandString(Constants.PRODUCT_DESC_FEAT_TYPE_DESC_DATA_KEY, UtilMisc.toMap("count", cnt)), "");
                        dataRow.put(FlexibleStringExpander.expandString(Constants.PRODUCT_DESC_FEAT_SEQ_NUM_DATA_KEY, UtilMisc.toMap("count", cnt)), "");
                        dataRow.put(FlexibleStringExpander.expandString(Constants.PRODUCT_DESC_FEAT_FROM_DATA_KEY, UtilMisc.toMap("count", cnt)), "");
                        dataRow.put(FlexibleStringExpander.expandString(Constants.PRODUCT_DESC_FEAT_THRU_DATA_KEY, UtilMisc.toMap("count", cnt)), "");
                        cnt++;
                    }
                }
            }

            dataRow.put(Constants.PRODUCT_SKU_DATA_KEY, mRow.get("goodIdentificationSkuId"));
            dataRow.put(Constants.PRODUCT_GOOGLE_ID_DATA_KEY, mRow.get("goodIdentificationGoogleId"));
            dataRow.put(Constants.PRODUCT_ISBN_DATA_KEY, mRow.get("goodIdentificationIsbnId"));
            dataRow.put(Constants.PRODUCT_MANUFACTURER_ID_NO_DATA_KEY, mRow.get("goodIdentificationManufacturerId"));
            dataRow.put(Constants.PRODUCT_BF_INVENTORY_TOT_DATA_KEY, mRow.get("bfInventoryTot"));
            dataRow.put(Constants.PRODUCT_BF_INVENTORY_WHS_DATA_KEY, mRow.get("bfInventoryWhs"));
            dataRow.put(Constants.PRODUCT_MULTI_VARIANT_DATA_KEY, mRow.get("multiVariant"));
            dataRow.put(Constants.PRODUCT_GIFT_MESSAGE_DATA_KEY, mRow.get("giftMessage"));
            dataRow.put(Constants.PRODUCT_QTY_MIN_DATA_KEY, mRow.get("pdpQtyMin"));
            dataRow.put(Constants.PRODUCT_QTY_MAX_DATA_KEY, mRow.get("pdpQtyMax"));
            dataRow.put(Constants.PRODUCT_QTY_DEFAULT_DATA_KEY, mRow.get("pdpQtyDefault"));
            dataRow.put(Constants.PRODUCT_IN_STORE_ONLY_DATA_KEY, mRow.get("pdpInStoreOnly"));

            if (UtilValidate.isNotEmpty(mRow.get("plpSwatchImage"))) {
                dataRow.put(Constants.PRODUCT_PLP_SWATCH_IMG_DATA_KEY, mRow.get("plpSwatchImage"));
                dataRow.put(Constants.PRODUCT_PLP_SWATCH_IMG_THRU_DATA_KEY, "");
            }

            if (UtilValidate.isNotEmpty(mRow.get("pdpSwatchImage"))) {
                dataRow.put(Constants.PRODUCT_PDP_SWATCH_IMG_DATA_KEY, mRow.get("pdpSwatchImage"));
                dataRow.put(Constants.PRODUCT_PDP_SWATCH_IMG_THRU_DATA_KEY, "");
            }

            if (UtilValidate.isNotEmpty(mRow.get("smallImage"))) {
                dataRow.put(Constants.PRODUCT_SMALL_IMG_DATA_KEY, mRow.get("smallImage"));
                dataRow.put(Constants.PRODUCT_SMALL_IMG_THRU_DATA_KEY, "");
            }

            if (UtilValidate.isNotEmpty(mRow.get("smallImageAlt"))) {
                dataRow.put(Constants.PRODUCT_SMALL_IMG_ALT_DATA_KEY, mRow.get("smallImageAlt"));
                dataRow.put(Constants.PRODUCT_SMALL_IMG_ALT_THRU_DATA_KEY, "");
            }

            if (UtilValidate.isNotEmpty(mRow.get("thumbImage"))) {
                dataRow.put(Constants.PRODUCT_THUMB_IMG_DATA_KEY, mRow.get("thumbImage"));
                dataRow.put(Constants.PRODUCT_THUMB_IMG_THRU_DATA_KEY, "");
            }

            if (UtilValidate.isNotEmpty(mRow.get("largeImage"))) {
                dataRow.put(Constants.PRODUCT_LARGE_IMG_DATA_KEY, mRow.get("largeImage"));
                dataRow.put(Constants.PRODUCT_LARGE_IMG_THRU_DATA_KEY, "");
            }

            if (UtilValidate.isNotEmpty(mRow.get("detailImage"))) {
                dataRow.put(Constants.PRODUCT_DETAIL_IMG_DATA_KEY, mRow.get("detailImage"));
                dataRow.put(Constants.PRODUCT_DETAIL_IMG_THRU_DATA_KEY, "");
            }

            if (UtilValidate.isNotEmpty(mRow.get("pdpVideoUrl"))) {
                dataRow.put(Constants.PRODUCT_VIDEO_URL_DATA_KEY, mRow.get("pdpVideoUrl"));
                dataRow.put(Constants.PRODUCT_VIDEO_URL_THRU_DATA_KEY, "");
            }

            if (UtilValidate.isNotEmpty(mRow.get("pdpVideo360Url"))) {
                dataRow.put(Constants.PRODUCT_VIDEO_360_URL_DATA_KEY, mRow.get("pdpVideo360Url"));
                dataRow.put(Constants.PRODUCT_VIDEO_360_URL_THRU_DATA_KEY, "");
            }
            for (cnt = 1; cnt <= 10; cnt++) {
                if (UtilValidate.isNotEmpty(mRow.get("addImage" + cnt))) {
                    dataRow.put(FlexibleStringExpander.expandString(Constants.PRODUCT_ADDNL_IMG_DATA_KEY, UtilMisc.toMap("count", cnt)), mRow.get("addImage" + cnt));
                    dataRow.put(FlexibleStringExpander.expandString(Constants.PRODUCT_ADDNL_IMG_THRU_DATA_KEY, UtilMisc.toMap("count", cnt)), "");
                }
                if (UtilValidate.isNotEmpty(mRow.get("xtraLargeImage" + cnt))) {
                    dataRow.put(FlexibleStringExpander.expandString(Constants.PRODUCT_XTRA_LARGE_IMG_DATA_KEY, UtilMisc.toMap("count", cnt)), mRow.get("xtraLargeImage" + cnt));
                    dataRow.put(FlexibleStringExpander.expandString(Constants.PRODUCT_XTRA_LARGE_IMG_THRU_DATA_KEY, UtilMisc.toMap("count", cnt)), "");
                }
                if (UtilValidate.isNotEmpty(mRow.get("xtraDetailImage" + cnt))) {
                    dataRow.put(FlexibleStringExpander.expandString(Constants.PRODUCT_XTRA_DETAIL_IMG_DATA_KEY, UtilMisc.toMap("count", cnt)), mRow.get("xtraDetailImage" + cnt));
                    dataRow.put(FlexibleStringExpander.expandString(Constants.PRODUCT_XTRA_DETAIL_IMG_THRU_DATA_KEY, UtilMisc.toMap("count", cnt)), "");
                }
            }
            for (cnt = 1; cnt <= 3; cnt++) {
                if (UtilValidate.isNotEmpty(mRow.get("productAttachment" + cnt))) {
                    dataRow.put(FlexibleStringExpander.expandString(Constants.PRODUCT_ATTACH_URL_DATA_KEY, UtilMisc.toMap("count", cnt)), mRow.get("productAttachment" + cnt));
                    dataRow.put(FlexibleStringExpander.expandString(Constants.PRODUCT_ATTACH_URL_THRU_DATA_KEY, UtilMisc.toMap("count", cnt)), "");
                }
            }

        } catch (Exception e) {
            Debug.logError(e, TihCatalogImport.module);
        }
        return dataRow;
    }

    public static List buildDataRows(List headerCols, Sheet s) {
        List dataRows = new ArrayList();

        try {

            for (int rowCount = 1; rowCount < s.getRows(); rowCount++) {
                Cell[] row = s.getRow(rowCount);
                if (row.length > 0) {
                    Map mRows = new HashMap();
                    for (int colCount = 0; colCount < headerCols.size(); colCount++) {
                        String colContent = null;

                        try {
                            colContent = row[colCount].getContents().toString();
                        } catch (Exception e) {
                            colContent = "";

                        }
                        mRows.put(headerCols.get(colCount), colContent);
                    }
                    //mRows = formatProductXLSData(mRows);
                    dataRows.add(mRows);
                }
            }


        } catch (Exception e) {
        }
        return dataRows;
    }

    public static List buildProductXMLDataRows(List<ProductType> products) {
        List dataRows = new ArrayList();

        try {

            for (int rowCount = 0; rowCount < products.size(); rowCount++) {
                ProductType product = (ProductType) products.get(rowCount);

                Map mRows = new HashMap();

                mRows.put("masterProductId", product.getMasterProductId());
                mRows.put("productId", product.getProductId());
                mRows.put("internalName", product.getInternalName());
                mRows.put("productName", product.getProductName());
                mRows.put("salesPitch", product.getSalesPitch());
                mRows.put("longDescription", product.getLongDescription());
                mRows.put("specialInstructions", product.getSpecialInstructions());
                mRows.put("deliveryInfo", product.getDeliveryInfo());
                mRows.put("directions", product.getDirections());
                mRows.put("termsConditions", product.getTermsAndConds());
                mRows.put("ingredients", product.getIngredients());
                mRows.put("warnings", product.getWarnings());
                mRows.put("plpLabel", product.getPlpLabel());
                mRows.put("pdpLabel", product.getPdpLabel());
                mRows.put("productHeight", product.getProductHeight());
                mRows.put("productWidth", product.getProductWidth());
                mRows.put("productDepth", product.getProductDepth());
                mRows.put("returnable", product.getReturnable());
                mRows.put("taxable", product.getTaxable());
                mRows.put("chargeShipping", product.getChargeShipping());
                mRows.put("introDate", product.getIntroDate());
                mRows.put("discoDate", product.getDiscoDate());
                mRows.put("manufacturerId", product.getManufacturerId());

                ProductPriceType productPrice = product.getProductPrice();
                if (UtilValidate.isNotEmpty(productPrice)) {
                    ListPriceType listPrice = productPrice.getListPrice();
                    if (UtilValidate.isNotEmpty(listPrice)) {
                        mRows.put("listPrice", listPrice.getPrice());
                        mRows.put("listPriceCurrency", listPrice.getCurrency());
                        mRows.put("listPriceFromDate", listPrice.getFromDate());
                        mRows.put("listPriceThruDate", listPrice.getThruDate());
                    }

                }


                ProductCategoryMemberType productCategory = product.getProductCategoryMember();
                if (UtilValidate.isNotEmpty(productCategory)) {
                    List<CategoryMemberType> categoryList = productCategory.getCategory();

                    StringBuffer categoryId = new StringBuffer("");
                    if (UtilValidate.isNotEmpty(categoryList)) {

                        for (int i = 0; i < categoryList.size(); i++) {
                            CategoryMemberType category = (CategoryMemberType) categoryList.get(i);
                            if (!category.getCategoryId().equals("")) {
                                categoryId.append(category.getCategoryId() + ",");
                                mRows.put(category.getCategoryId() + "_sequenceNum", category.getSequenceNum());
                                mRows.put(category.getCategoryId() + "_fromDate", category.getFromDate());
                                mRows.put(category.getCategoryId() + "_thruDate", category.getThruDate());
                            }
                        }
                        if (categoryId.length() > 1) {
                            categoryId.setLength(categoryId.length() - 1);
                        }
                    }
                    mRows.put("productCategoryId", categoryId.toString());
                    mRows.put("manufacturerId", product.getManufacturerId());
                }


                ProductSelectableFeatureType selectableFeature = product.getProductSelectableFeature();
                if (UtilValidate.isNotEmpty(selectableFeature)) {
                    List<FeatureType> selectableFeatureList = selectableFeature.getFeature();
                    if (UtilValidate.isNotEmpty(selectableFeatureList)) {
                        for (int i = 0; i < selectableFeatureList.size(); i++) {
                            String featureId = new String("");
                            FeatureType feature = (FeatureType) selectableFeatureList.get(i);
                            if (UtilValidate.isNotEmpty(feature.getFeatureId())) {
                                StringBuffer featureValue = new StringBuffer("");
                                List featureValues = feature.getValue();
                                if (UtilValidate.isNotEmpty(featureValues)) {

                                    for (int value = 0; value < featureValues.size(); value++) {
                                        if (!featureValues.get(value).equals("")) {
                                            featureValue.append(featureValues.get(value) + ",");
                                        }
                                    }
                                    if (featureValue.length() > 1) {
                                        featureValue.setLength(featureValue.length() - 1);
                                    }
                                }
                                if (featureValue.length() > 0) {
                                    featureId = feature.getFeatureId() + ":" + featureValue.toString();
                                    mRows.put(feature.getFeatureId() + "_sequenceNum", feature.getSequenceNum());
                                    mRows.put(feature.getFeatureId() + "_fromDate", feature.getFromDate());
                                    mRows.put(feature.getFeatureId() + "_thruDate", feature.getThruDate());
                                    mRows.put(feature.getFeatureId() + "_description", feature.getDescription());
                                }
                            }
                            mRows.put("selectabeFeature_" + (i + 1), featureId);
                        }
                        mRows.put("totSelectableFeatures", new Integer(selectableFeatureList.size()).toString());
                    }
                } else {
                    mRows.put("totSelectableFeatures", new Integer(0).toString());
                }


                ProductDescriptiveFeatureType descriptiveFeature = product.getProductDescriptiveFeature();
                if (UtilValidate.isNotEmpty(descriptiveFeature)) {
                    List<FeatureType> descriptiveFeatureList = descriptiveFeature.getFeature();
                    if (UtilValidate.isNotEmpty(descriptiveFeatureList)) {
                        for (int i = 0; i < descriptiveFeatureList.size(); i++) {
                            String featureId = new String("");
                            FeatureType feature = (FeatureType) descriptiveFeatureList.get(i);
                            if (UtilValidate.isNotEmpty(feature.getFeatureId())) {
                                StringBuffer featureValue = new StringBuffer("");
                                List featureValues = feature.getValue();
                                if (UtilValidate.isNotEmpty(featureValues)) {

                                    for (int value = 0; value < featureValues.size(); value++) {
                                        if (!featureValues.get(value).equals("")) {
                                            featureValue.append(featureValues.get(value) + ",");
                                        }
                                    }
                                    if (featureValue.length() > 1) {
                                        featureValue.setLength(featureValue.length() - 1);
                                    }
                                }
                                if (featureValue.length() > 0) {
                                    featureId = feature.getFeatureId() + ":" + featureValue.toString();
                                    mRows.put(feature.getFeatureId() + "_sequenceNum", feature.getSequenceNum());
                                    mRows.put(feature.getFeatureId() + "_fromDate", feature.getFromDate());
                                    mRows.put(feature.getFeatureId() + "_thruDate", feature.getThruDate());
                                    mRows.put(feature.getFeatureId() + "_description", feature.getDescription());
                                }
                            }
                            mRows.put("descriptiveFeature_" + (i + 1), featureId);
                        }
                        mRows.put("totDescriptiveFeatures", new Integer(descriptiveFeatureList.size()).toString());
                    }
                } else {
                    mRows.put("totDescriptiveFeatures", new Integer(0).toString());
                }


                ProductImageType productImage = product.getProductImage();
                if (UtilValidate.isNotEmpty(productImage)) {

                    PlpSmallImageType plpSmallImage = productImage.getPlpSmallImage();
                    if (UtilValidate.isNotEmpty(plpSmallImage)) {
                        mRows.put("smallImage", plpSmallImage.getUrl());
                        mRows.put("smallImageThruDate", plpSmallImage.getThruDate());
                    }

                    PdpLargeImageType plpLargeImage = productImage.getPdpLargeImage();
                    if (UtilValidate.isNotEmpty(plpLargeImage)) {
                        mRows.put("largeImage", plpLargeImage.getUrl());
                        mRows.put("largeImageThruDate", plpLargeImage.getThruDate());
                    }

                    PdpDetailImageType pdpDetailImage = productImage.getPdpDetailImage();
                    if (UtilValidate.isNotEmpty(pdpDetailImage)) {
                        mRows.put("detailImage", pdpDetailImage.getUrl());
                        mRows.put("detailImageThruDate", pdpDetailImage.getThruDate());
                    }

                }

                mRows.put("weight", product.getProductWeight());
                mRows = formatProductXLSData(mRows);
                dataRows.add(mRows);
            }
        } catch (Exception e) {
            Debug.logError(e, TihCatalogImport.module);
            e.printStackTrace();
        }
        return dataRows;
    }


    private static Map<String, String> formatProductXLSData(Map<String, String> dataMap) {
        Map<String, String> formattedDataMap = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : dataMap.entrySet()) {
            String value = entry.getValue();
            if (UtilValidate.isNotEmpty(value)) {
                value = StringUtil.replaceString(value, "&", "&amp");
                value = StringUtil.replaceString(value, ";", "&#59;");
                value = StringUtil.replaceString(value, "&amp", "&amp;");
                value = StringUtil.replaceString(value, "\"", "&quot;");
            }
            formattedDataMap.put(entry.getKey(), value);
        }
        return formattedDataMap;
    }

    private static List buildCategoryHeader() {
        List headerCols = new ArrayList();
        headerCols.add("productCategoryId");
        headerCols.add("parentCategoryId");
        headerCols.add("categoryName");
        headerCols.add("description");
        headerCols.add("longDescription");
        headerCols.add("plpImageName");
        headerCols.add("fromDate");
        headerCols.add("thruDate");

        return headerCols;

    }

    private static List buildProductHeader() {
        List headerCols = new ArrayList();
        headerCols.add("masterProductId");
        headerCols.add("productId");
        headerCols.add("productCategoryId");
        headerCols.add("internalName");
        headerCols.add("productName");
        headerCols.add("longDescription");
        headerCols.add("warnings");
        headerCols.add("listPrice");
        headerCols.add("defaultPrice");
        headerCols.add("selectabeFeature_1");
        headerCols.add("selectabeFeature_2");
        headerCols.add("selectabeFeature_3");
        headerCols.add("selectabeFeature_4");
        headerCols.add("selectabeFeature_5");
        headerCols.add("descriptiveFeature_1");
        headerCols.add("descriptiveFeature_2");
        headerCols.add("descriptiveFeature_3");
        headerCols.add("descriptiveFeature_4");
        headerCols.add("descriptiveFeature_5");
        headerCols.add("smallImage");
        headerCols.add("largeImage");
        headerCols.add("detailImage");
        headerCols.add("productHeight");
        headerCols.add("productWidth");
        headerCols.add("productDepth");
        headerCols.add("returnable");
        headerCols.add("taxable");
        headerCols.add("chargeShipping");
        headerCols.add("introDate");
        headerCols.add("discoDate");
        headerCols.add("sequenceNum");
        headerCols.add("weight");

        return headerCols;
    }

    private static List createFeatureVariantProductId(List selectableFeatureList, String selectableFeature) {
        if (UtilValidate.isNotEmpty(selectableFeature)) {
            List tempSelectableFeatureList = new ArrayList();
            int iFeatIdx = selectableFeature.indexOf(':');
            if (iFeatIdx > -1) {
                String rawFeature = selectableFeature.substring(0, iFeatIdx).trim().toUpperCase();
                String featureType = "FEATURE_" + rawFeature.toUpperCase();
                String featureDescription = rawFeature + "_" + selectableFeature.substring(iFeatIdx + 1).trim().toUpperCase();

                if (selectableFeatureList.size() > 0) {
                    for (int i = 0; i < selectableFeatureList.size(); i++) {
                        ArrayList tempList = (ArrayList) selectableFeatureList.get(i);
                        ArrayList featureList = new ArrayList(tempList);
                        featureList.add(featureType + "~" + featureDescription);
                        tempSelectableFeatureList.add(featureList);
                    }
                } else {

                    ArrayList featureList = new ArrayList();
                    featureList.add(featureType + "~" + featureDescription);
                    selectableFeatureList.add(featureList);
                }
            }
            if (tempSelectableFeatureList.size() > 0) {
                selectableFeatureList = tempSelectableFeatureList;
            }
        }

        return selectableFeatureList;
    }
}
