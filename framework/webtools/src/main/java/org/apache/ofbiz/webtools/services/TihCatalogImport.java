package org.apache.ofbiz.webtools.services;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.ofbiz.base.util.*;
import org.apache.ofbiz.datafile.DataFile;
import org.apache.ofbiz.datafile.DataFile2EntityXml;
import org.apache.ofbiz.datafile.ModelDataFile;
import org.apache.ofbiz.datafile.ModelDataFileReader;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.webtools.services.helpers.DataRowsBuilder;
import org.apache.ofbiz.webtools.services.helpers.DataXmlGenerator;
import org.apache.ofbiz.webtools.services.model.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import java.io.*;
import java.net.URL;
import java.util.*;

import static org.apache.ofbiz.webtools.services.helpers.DataRowsBuilder.buildProductCategoryXMLDataRows;
import static org.apache.ofbiz.webtools.services.helpers.DataRowsBuilder.buildProductXMLDataRows;
import static org.apache.ofbiz.webtools.services.helpers.DataXmlGenerator.*;

public class TihCatalogImport {

    public static final String module = TihCatalogImport.class.getName();
    private static final ResourceBundle OSAFE_PROP = UtilProperties.getResourceBundle("OsafeProperties.xml", Locale.getDefault());
    private static Delegator _delegator = DelegatorFactory.getDelegator(null);
    private static String localeString = "";

    public static Map<String, Object> importProductXls(DispatchContext ctx, Map<String, ?> context)
    {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        _delegator = ctx.getDelegator();
        List<String> messages = new ArrayList();

        String xlsDataFilePath = (String)context.get("xlsDataFile");
        String xmlDataDirPath = (String)context.get("xmlDataDir");
        GenericValue userLogin = (GenericValue)context.get("userLogin");
        String loadImagesDirPath=(String)context.get("productLoadImagesDir");
        String imageUrl = (String)context.get("imageUrl");
        Boolean removeAll = (Boolean) context.get("removeAll");
        Boolean autoLoad = (Boolean) context.get("autoLoad");
        String productStoreId = (String) context.get("productStoreId");

        if (removeAll == null) removeAll = Boolean.FALSE;
        if (autoLoad == null) autoLoad = Boolean.FALSE;

        File inputWorkbook = null;
        File baseDataDir = null;
        BufferedWriter fOutProduct=null;
        if (UtilValidate.isNotEmpty(xlsDataFilePath) && UtilValidate.isNotEmpty(xmlDataDirPath))
        {
            try
            {
                URL xlsDataFileUrl = UtilURL.fromFilename(xlsDataFilePath);
                InputStream ins = xlsDataFileUrl.openStream();

                if (ins != null && (xlsDataFilePath.toUpperCase().endsWith("XLS")))
                {
                    baseDataDir = new File(xmlDataDirPath);
                    if (baseDataDir.isDirectory() && baseDataDir.canWrite())
                    {

                        // ######################################
                        //save the temp xls data file on server
                        // ######################################
                        try
                        {
                            inputWorkbook = new File(baseDataDir,  UtilDateTime.nowAsString()+"."+FilenameUtils.getExtension(xlsDataFilePath));
                            if (inputWorkbook.createNewFile())
                            {
                                Streams.copy(ins, new FileOutputStream(inputWorkbook), true, new byte[1]);
                            }
                        }
                        catch (IOException ioe)
                        {
                            Debug.logError(ioe, module);
                        } catch (Exception exc)
                        {
                            Debug.logError(exc, module);
                        }
                    }
                    else
                    {
                        messages.add("xml data dir path not found or can't be write");
                    }
                }
                else
                {
                    messages.add(" path specified for Excel sheet file is wrong , doing nothing.");
                }

            }
            catch (IOException ioe)
            {
                Debug.logError(ioe, module);
            }
            catch (Exception exc)
            {
                Debug.logError(exc, module);
            }
        }
        else
        {
            messages.add("No path specified for Excel sheet file or xml data direcotry, doing nothing.");
        }

        String bigfishXmlFile = UtilDateTime.nowAsString()+".xml";

        String importDataPath = System.getProperty("ofbiz.home") + "/runtime/tmp/upload/bigfishXmlFile/";

        if (!new File(importDataPath).exists())
        {
            new File(importDataPath).mkdirs();
        }

        File tempFile = new File(importDataPath, "temp" + bigfishXmlFile);


        // ######################################
        //read the temp xls file and generate Bigfish xml
        // ######################################
        if (inputWorkbook != null && baseDataDir  != null)
        {
            try
            {

                WorkbookSettings ws = new WorkbookSettings();
                ws.setLocale(new Locale("en", "EN"));
                Workbook wb = Workbook.getWorkbook(inputWorkbook,ws);

                ObjectFactory factory = new ObjectFactory();

                BigFishProductFeedType bfProductFeedType = factory.createBigFishProductFeedType();

                // Gets the sheets from workbook
                for (int sheet = 0; sheet < wb.getNumberOfSheets(); sheet++)
                {
                    BufferedWriter bw = null;
                    try
                    {
                        Sheet s = wb.getSheet(sheet);
                        String sTabName=s.getName();

                        if (sheet == 1)
                        {
                            ProductCategoryType productCategoryType = factory.createProductCategoryType();
                            List productCategoryList =  productCategoryType.getCategory();
                            List<Map<String, Object>> dataRows = DataRowsBuilder.buildProductCategoryDataRows(s);
                            DataXmlGenerator.generateProductCategoryXML(factory, productCategoryList,  dataRows);
                            bfProductFeedType.setProductCategory(productCategoryType);
                        }
                        if (sheet == 2)
                        {
                            ProductsType productsType = factory.createProductsType();
                            List productList = productsType.getProduct();
                            List<Map<String, Object>>  dataRows = DataRowsBuilder.buildProductDataRows(s);
                            DataXmlGenerator.generateProductXML(factory, productList, dataRows);
                            bfProductFeedType.setProducts(productsType);
                        }

                        //File to store data in form of CSV
                    } catch (Exception exc) {
                        Debug.logError(exc, module);
                    }
                    finally
                    {
                        try
                        {
                            if (fOutProduct != null)
                            {
                                fOutProduct.close();
                            }
                        }
                        catch (IOException ioe)
                        {
                            Debug.logError(ioe, module);
                        }
                    }
                }

                FeedsUtil.marshalObject(new JAXBElement<BigFishProductFeedType>(new QName("", "BigFishProductFeed"), BigFishProductFeedType.class, null, bfProductFeedType), tempFile);

                new File(importDataPath, bigfishXmlFile).delete();
                File renameFile =new File(importDataPath, bigfishXmlFile);
                RandomAccessFile out = new RandomAccessFile(renameFile, "rw");
                InputStream inputStr = new FileInputStream(tempFile);
                byte[] bytes = new byte[102400];
                int bytesRead;
                while ((bytesRead = inputStr.read(bytes)) != -1)
                {
                    out.write(bytes, 0, bytesRead);
                }
                out.close();
                inputStr.close();

                Map<String, Object> importClientProductTemplateCtx = null;
                Map result  = new HashMap();
                importClientProductTemplateCtx = UtilMisc.toMap("xmlDataFile", renameFile.toString(), "xmlDataDir", xmlDataDirPath,"productLoadImagesDir", loadImagesDirPath, "imageUrl", imageUrl, "removeAll",removeAll,"autoLoad",autoLoad,"userLogin",userLogin,"productStoreId",productStoreId);
                result = dispatcher.runSync("importClientProductXMLTemplate", importClientProductTemplateCtx);
                if(UtilValidate.isNotEmpty(result.get("responseMessage")) && result.get("responseMessage").equals("error"))
                {
                    return ServiceUtil.returnError(result.get("errorMessage").toString());
                }
                messages = (List)result.get("messages");

            }
            catch (BiffException be)
            {
                Debug.logError(be, module);
            }
            catch (Exception exc)
            {
                Debug.logError(exc, module);
            }
            finally
            {
                inputWorkbook.delete();
            }
        }
        Map<String, Object> resp = UtilMisc.toMap("messages", (Object) messages);
        return resp;

    }

    public static Map<String, Object> importProductXML(DispatchContext ctx, Map<String, ?> context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        _delegator = ctx.getDelegator();
        List<String> messages = new ArrayList();
        List<String> errorMessages = new ArrayList();

        String xmlDataFilePath = (String)context.get("xmlDataFile");
        String xmlDataDirPath = (String)context.get("xmlDataDir");
        String loadImagesDirPath=(String)context.get("productLoadImagesDir");
        String imageUrl = (String)context.get("imageUrl");
        Boolean removeAll = (Boolean) context.get("removeAll");
        Boolean autoLoad = (Boolean) context.get("autoLoad");
        String productStoreId = (String)context.get("productStoreId");

        if (removeAll == null) removeAll = Boolean.FALSE;
        if (autoLoad == null) autoLoad = Boolean.FALSE;

        File inputWorkbook = null;
        String tempDataFile = null;
        File baseDataDir = null;
        File baseFilePath = null;
        BufferedWriter fOutProduct=null;
        if (UtilValidate.isNotEmpty(xmlDataFilePath) && UtilValidate.isNotEmpty(xmlDataDirPath))
        {
            baseFilePath = new File(xmlDataFilePath);
            try
            {
                URL xlsDataFileUrl = UtilURL.fromFilename(xmlDataFilePath);
                InputStream ins = xlsDataFileUrl.openStream();

                if (ins != null && (xmlDataFilePath.toUpperCase().endsWith("XML")))
                {
                    baseDataDir = new File(xmlDataDirPath);
                    if (baseDataDir.isDirectory() && baseDataDir.canWrite()) {

                        // ############################################
                        // move the existing xml files in dump directory
                        // ############################################
                        File dumpXmlDir = null;
                        File[] fileArray = baseDataDir.listFiles();
                        for (File file: fileArray)
                        {
                            try
                            {
                                if (file.getName().toUpperCase().endsWith("XML"))
                                {
                                    if (dumpXmlDir == null)
                                    {
                                        dumpXmlDir = new File(baseDataDir, "dumpxml_"+UtilDateTime.nowDateString());
                                    }
                                    FileUtils.copyFileToDirectory(file, dumpXmlDir);
                                    file.delete();
                                }
                            }
                            catch (IOException ioe)
                            {
                                Debug.logError(ioe, module);
                            }
                            catch (Exception exc)
                            {
                                Debug.logError(exc, module);
                            }
                        }
                        // ######################################
                        //save the temp xml data file on server
                        // ######################################
                        try
                        {
                            tempDataFile = UtilDateTime.nowAsString()+"."+FilenameUtils.getExtension(xmlDataFilePath);
                            inputWorkbook = new File(baseDataDir,  tempDataFile);
                            if (inputWorkbook.createNewFile())
                            {
                                Streams.copy(ins, new FileOutputStream(inputWorkbook), true, new byte[1]);
                            }
                        }
                        catch (IOException ioe)
                        {
                            Debug.logError(ioe, module);
                        }
                        catch (Exception exc)
                        {
                            Debug.logError(exc, module);
                        }
                    }
                    else {
                        messages.add("xml data dir path not found or can't be write");
                    }
                }
                else
                {
                    messages.add(" path specified for XML file is wrong , doing nothing.");
                }

            }
            catch (IOException ioe)
            {
                Debug.logError(ioe, module);
            }
            catch (Exception exc)
            {
                Debug.logError(exc, module);
            }
        }
        else
        {
            messages.add("No path specified for XML file or xml data direcotry, doing nothing.");
        }

        // ######################################
        //read the temp xls file and generate xml
        // ######################################
        try
        {
            if (inputWorkbook != null && baseDataDir  != null)
            {
                try
                {
                    JAXBContext jaxbContext = JAXBContext.newInstance("org.apache.ofbiz.webtools.services.model");
                    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                    JAXBElement<BigFishProductFeedType> bfProductFeedType = (JAXBElement<BigFishProductFeedType>)unmarshaller.unmarshal(inputWorkbook);

                    if(UtilValidate.isNotEmpty(productStoreId))
                    {
                        try
                        {
                            GenericValue productStore = _delegator.findOne("ProductStore", UtilMisc.toMap("productStoreId", productStoreId), false);
                            if(UtilValidate.isNotEmpty(productStore))
                            {
                                localeString = productStore.getString("defaultLocaleString");
                            }
                        }
                        catch(GenericEntityException gee)
                        {
                            Debug.log("No Product Store Found For ProductStoreId "+productStoreId, gee.toString());
                        }
                    }

                    List<ProductType> products = new ArrayList();
                    List<CategoryType> productCategories = new ArrayList();

                    ProductsType productsType = bfProductFeedType.getValue().getProducts();
                    if(UtilValidate.isNotEmpty(productsType))
                    {
                        products = productsType.getProduct();
                    }

                    ProductCategoryType productCategoryType = bfProductFeedType.getValue().getProductCategory();
                    if(UtilValidate.isNotEmpty(productCategoryType))
                    {
                        productCategories = productCategoryType.getCategory();
                    }

                    if(productCategories.size() > 0)
                    {
                        List dataRows = buildProductCategoryXMLDataRows(productCategories);
                        buildProductCategory(dataRows, xmlDataDirPath,loadImagesDirPath, imageUrl, _delegator, localeString);
                    }

                    if(products.size() > 0)
                    {
                        List dataRows = buildProductXMLDataRows(products);
                        buildProduct(dataRows, xmlDataDirPath, _delegator);
                        buildProductVariant(dataRows, xmlDataDirPath,loadImagesDirPath,imageUrl, removeAll, _delegator);
                        buildProductSelectableFeatures(dataRows, xmlDataDirPath, _delegator);
                        buildProductCategoryFeatures(dataRows, xmlDataDirPath, removeAll, _delegator);
                        buildProductDistinguishingFeatures(dataRows, xmlDataDirPath, _delegator);
                        buildProductContent(dataRows, xmlDataDirPath,loadImagesDirPath,imageUrl, localeString, _delegator);
                        buildProductVariantContent(dataRows, xmlDataDirPath,loadImagesDirPath,imageUrl, localeString, _delegator);
                    }
                }
                catch (Exception e)
                {
                    Debug.logError(e, module);
                }
                finally
                {
                    try {
                        if (fOutProduct != null)
                        {
                            fOutProduct.close();
                        }
                    } catch (IOException ioe)
                    {
                        Debug.logError(ioe, module);
                    }
                }
            }
            // ############################################
            // clear static fields for next run
            // ############################################
            clearMaps();
            // ############################################
            // call the service for remove entity data
            // if removeAll and autoLoad parameter are true
            // ############################################
            if (removeAll)
            {
                Map importRemoveEntityDataParams = UtilMisc.toMap();
                try {

                    Map result = dispatcher.runSync("importRemoveEntityData", importRemoveEntityDataParams);

                    List<String> serviceMsg = (List)result.get("messages");
                    for (String msg: serviceMsg)
                    {
                        messages.add(msg);
                    }
                }
                catch (Exception exc)
                {
                    Debug.logError(exc, module);
                    autoLoad = Boolean.FALSE;
                }
            }

            // ##############################################
            // move the generated xml files in done directory
            // ##############################################
            File doneXmlDir = new File(baseDataDir, Constants.DONE_XML_DIRECTORY_PREFIX+UtilDateTime.nowDateString());
            File[] fileArray = baseDataDir.listFiles();
            for (File file: fileArray)
            {
                try
                {
                    if (FilenameUtils.getExtension(file.getName()).equalsIgnoreCase("XML"))
                    {
                        if(!(file.getName().equals(tempDataFile)) && (!file.getName().equals(baseFilePath.getName()))){
                            FileUtils.copyFileToDirectory(file, doneXmlDir);
                            file.delete();
                        }
                    }
                }
                catch (IOException ioe)
                {
                    Debug.logError(ioe, module);
                }
                catch (Exception exc)
                {
                    Debug.logError(exc, module);
                }
            }

            // ######################################################################
            // call service for insert row in database  from generated xml data files
            // by calling service entityImportDir if autoLoad parameter is true
            // ######################################################################

            if (autoLoad)
            {
                Map entityImportDirParams = UtilMisc.toMap("path", doneXmlDir.getPath(),
                        "userLogin", context.get("userLogin"));
                Map result = dispatcher.runSync("entityImportDir", entityImportDirParams);
                if(UtilValidate.isNotEmpty(result.get("responseMessage")) && result.get("responseMessage").equals("error"))
                {
                    return ServiceUtil.returnError(result.get("errorMessage").toString());
                }
                else
                {
                    List<String> serviceMsg = (List)result.get("messages");
                    for (String msg: serviceMsg)
                    {
                        messages.add(msg);
                    }
                }
            }
        }
        catch (Exception exc)
        {
            Debug.logError(exc, module);
        }
        finally
        {
            inputWorkbook.delete();
        }

        Map<String, Object> resp = UtilMisc.toMap("messages", (Object) messages);
        return resp;

    }


    /**
     * service for generating the xml data files from xls data file using import entity defination
     * take the xls file location path, output xml data files directory path and import entity defination xml file location
     * working first upload the xls data file ,generate csv files from xls data file
     * using service importCsvToXml generate xml data files.
     * this service support only 2003 Excel sheet format
     */
    public static Map<String, Object> importXLSFileAndGenerateXML(DispatchContext ctx, Map<String, ?> context) {

        LocalDispatcher dispatcher = ctx.getDispatcher();
        List<String> messages = new ArrayList<>();
        InputStream ins = null;
        File inputWorkbook = null, baseDataDir = null, csvDir = null;

        String definitionFileLoc = (String)context.get("definitionFileLoc");
        String xlsDataFilePath = (String)context.get("xlsDataFile");
        String xmlDataDirPath = (String)context.get("xmlDataDir");

        if (UtilValidate.isNotEmpty(xlsDataFilePath) && UtilValidate.isNotEmpty(xmlDataDirPath)) {
            try {
                // ######################################
                // make the input stram for xls data file
                // ######################################
                URL xlsDataFileUrl = UtilURL.fromFilename(xlsDataFilePath);
                ins = xlsDataFileUrl.openStream();

                if (ins != null && (xlsDataFilePath.toUpperCase().endsWith("XLS"))) {
                    baseDataDir = new File(xmlDataDirPath);
                    if (baseDataDir.isDirectory() && baseDataDir.canWrite()) {

                        // ############################################
                        // move the existing xml files in dump directory
                        // ############################################
                        File dumpXmlDir = null;
                        File[] fileArray = baseDataDir.listFiles();
                        for (File file: fileArray) {
                            try {
                                if (file.getName().toUpperCase().endsWith("XML")) {
                                    if (dumpXmlDir == null) {
                                        dumpXmlDir = new File(baseDataDir, Constants.DUMP_XML_DIRECTORY_PREFIX+ UtilDateTime.nowDateString());
                                    }
                                    FileUtils.copyFileToDirectory(file, dumpXmlDir);
                                    file.delete();
                                }
                            } catch (IOException ioe) {
                                Debug.logError(ioe, module);
                            } catch (Exception exc) {
                                Debug.logError(exc, module);
                            }
                        }
                        // ######################################
                        //save the temp xls data file on server
                        // ######################################
                        try {
                            inputWorkbook = new File(baseDataDir,  UtilDateTime.nowAsString()+"."+ FilenameUtils.getExtension(xlsDataFilePath));
                            if (inputWorkbook.createNewFile()) {
                                Streams.copy(ins, new FileOutputStream(inputWorkbook), true, new byte[1]);
                            }
                        } catch (IOException ioe) {
                            Debug.logError(ioe, module);
                        } catch (Exception exc) {
                            Debug.logError(exc, module);
                        }
                    }
                    else {
                        messages.add("xml data dir path not found or can't be write");
                    }
                }
                else {
                    messages.add(" path specified for Excel sheet file is wrong , doing nothing.");
                }

            } catch (IOException ioe) {
                Debug.logError(ioe, module);
            } catch (Exception exc) {
                Debug.logError(exc, module);
            }
        }
        else {
            messages.add("No path specified for Excel sheet file or xml data direcotry, doing nothing.");
        }

        // ######################################
        //read the temp xls file and generate csv
        // ######################################
        if (inputWorkbook != null && baseDataDir  != null) {
            try {
                csvDir = new File(baseDataDir,  UtilDateTime.nowDateString()+"_CSV_"+UtilDateTime.nowAsString());
                if (!csvDir.exists() ) {
                    csvDir.mkdirs();
                }

                WorkbookSettings ws = new WorkbookSettings();
                ws.setLocale(new Locale("en", "EN"));
                Workbook wb = Workbook.getWorkbook(inputWorkbook,ws);
                // Gets the sheets from workbook
                for (int sheet = 0; sheet < wb.getNumberOfSheets(); sheet++) {
                    BufferedWriter bw = null;
                    try {
                        Sheet s = wb.getSheet(sheet);

                        //File to store data in form of CSV
                        File csvFile = new File(csvDir, s.getName().trim()+".csv");
                        if (csvFile.createNewFile()) {
                            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile), "UTF-8"));

                            Cell[] row = null;
                            //loop start from 1 because of discard the header row
                            for (int rowCount = 1 ; rowCount < s.getRows() ; rowCount++) {
                                StringBuilder  rowString = new StringBuilder();
                                row = s.getRow(rowCount);
                                if (row.length > 0) {
                                    rowString.append(row[0].getContents());
                                    for (int colCount = 1; colCount < row.length; colCount++) {
                                        rowString.append(",");
                                        rowString.append(row[colCount].getContents());
                                    }
                                    if(UtilValidate.isNotEmpty(StringUtil.replaceString(rowString.toString(), ",", "").trim())) {
                                        bw.write(rowString.toString());
                                        bw.newLine();
                                    }
                                }
                            }
                        }
                    } catch (IOException ioe) {
                        Debug.logError(ioe, module);
                    } catch (Exception exc) {
                        Debug.logError(exc, module);
                    }
                    finally {
                        try {
                            if (bw != null) {
                                bw.flush();
                                bw.close();
                            }
                        } catch (IOException ioe) {
                            Debug.logError(ioe, module);
                        }
                        bw = null;
                    }
                }

            } catch (BiffException be) {
                Debug.logError(be, module);
            } catch (Exception exc) {
                Debug.logError(exc, module);
            }
        }

        // ####################################################################################################################
        //Generate xml files from csv directory using importCsvToXml service
        //Delete temp xls file and csv directory
        // ####################################################################################################################
        if(csvDir != null) {

            // call service for generate xml files from csv directory
            Map importCsvToXmlParams = UtilMisc.toMap("sourceCsvFileLoc", csvDir.getPath(),
                    "definitionFileLoc", definitionFileLoc,
                    "targetXmlFileLoc", baseDataDir.getPath());
            try {
                Map result = dispatcher.runSync("importCsvToXml", importCsvToXmlParams);

            } catch(Exception exc) {
                Debug.logError(exc, module);
            }

            //Delete temp xls file and csv directory
            try {
                inputWorkbook.delete();
                FileUtils.deleteDirectory(csvDir);

            } catch (IOException ioe) {
                Debug.logError(ioe, module);
            } catch (Exception exc) {
                Debug.logError(exc, module);
            }

            messages.add("file saved in xml base dir.");
        }
        else {
            messages.add("input parameter is wrong , doing nothing.");
        }

        // send the notification
        Map<String, Object> resp = UtilMisc.toMap("messages", (Object) messages);
        return resp;
    }

    public static Map<String, Object> importCsvToXml(DispatchContext ctx, Map context){
        /*
         * This Service is used to generate XML file from CSV file using
         * entity definition
         */
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Map<String, Object> result = ServiceUtil.returnSuccess();

        String sourceCsvFileLoc = (String)context.get("sourceCsvFileLoc");
        String definitionFileLoc = (String)context.get("definitionFileLoc");
        String targetXmlFileLoc = (String)context.get("targetXmlFileLoc");

        Collection definitionFileNames = null;
        File outXmlDir = null;
        URL definitionUrl= null;
        definitionUrl = UtilURL.fromFilename(definitionFileLoc);

        if (definitionUrl != null) {
            try {
                ModelDataFileReader reader = ModelDataFileReader.getModelDataFileReader(definitionUrl);
                if (reader != null) {
                    definitionFileNames = (Collection)reader.getDataFileNames();
                    context.put("definitionFileNames", definitionFileNames);
                }
            } catch(Exception ex) {
                Debug.logError(ex.getMessage(), module);
            }
        }
        if (targetXmlFileLoc != null) {
            outXmlDir = new File(targetXmlFileLoc);
            if (!outXmlDir.exists()) {
                outXmlDir.mkdir();
            }
        }
        if (sourceCsvFileLoc != null) {
            File inCsvDir = new File(sourceCsvFileLoc);
            if (!inCsvDir.exists()) {
                inCsvDir.mkdir();
            }
            if (inCsvDir.isDirectory() && inCsvDir.canRead() && outXmlDir.isDirectory() && outXmlDir.canWrite()) {
                File[] fileArray = inCsvDir.listFiles();
                URL dataFileUrl = null;
                for (File file: fileArray) {
                    if(file.getName().toUpperCase().endsWith("CSV")) {
                        String fileNameWithoutExt = FilenameUtils.removeExtension(file.getName());
                        String definationName = OSAFE_PROP.getString("importXls" +UtilValidate.stripWhitespace(fileNameWithoutExt));
                        if(definitionFileNames.contains(definationName)) {
                            dataFileUrl = UtilURL.fromFilename(file.getPath());
                            DataFile dataFile = null;
                            if(dataFileUrl != null && definitionUrl != null && definitionFileNames != null) {
                                try {
                                    dataFile = DataFile.readFile(dataFileUrl, definitionUrl, definationName);
                                    context.put("dataFile", dataFile);
                                }catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            if(dataFile != null) {
                                ModelDataFile modelDataFile = (ModelDataFile)dataFile.getModelDataFile();
                                context.put("modelDataFile", modelDataFile);
                            }
                            if (dataFile != null && definationName != null) {
                                try {
                                    DataFile2EntityXml.writeToEntityXml(new File(outXmlDir, definationName +".xml").getPath(), dataFile);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        else {
                            Debug.log("======csv file name which not according to import defination file================="+file.getName()+"====");
                        }
                    }
                }
            }
        }
        return result;
    }
}
