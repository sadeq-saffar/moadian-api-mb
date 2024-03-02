package moadian;

import ir.gov.tax.tpis.sdk.content.api.DefaultTaxApiClient;
import ir.gov.tax.tpis.sdk.content.dto.InquiryResultModel;
import ir.gov.tax.tpis.sdk.content.dto.InvoiceDto;
import ir.gov.tax.tpis.sdk.transfer.api.ObjectTransferApiImpl;
import ir.gov.tax.tpis.sdk.transfer.api.TransferApi;
import ir.gov.tax.tpis.sdk.transfer.config.ApiConfig;
import ir.gov.tax.tpis.sdk.transfer.dto.AsyncResponseModel;
import ir.gov.tax.tpis.sdk.transfer.impl.signatory.SignatoryFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class Moadian {
    private String clientId = "";
    private String companyName = "";
    ApiConfig apiConfig;
    DefaultTaxApiClient taxApi;
    private String baseUrl;

    private Logger logger;
    public Moadian(String clientId, String companyName, String baseUrl, Logger logger) {
        this.clientId = clientId;
        this.companyName = companyName;
        this.baseUrl = baseUrl;
        this.logger =logger;
        try {
            init();
        } catch (IOException e) {
            logger.severe("init()" + e.getMessage() );
            throw new RuntimeException(e);
        }

    }

    private void init() throws IOException {
        //https://tp.tax.gov.ir/
        File pkcs8PrivateKey = new File("C:\\keys\\" + companyName + "private.txt");
//        try {w
//            // Read the file content
//            String fileContent = new String(Files.readAllBytes(pkcs8PrivateKey.toPath()), StandardCharsets.UTF_8);
//
//            // Print or process the file content as needed
////            System.out.println("File Content:\n" + fileContent);
////            logger.info("File Content:\n" + fileContent);
//        } catch (Exception e) {
//            logger.info("Reading File Content error is:\n" + e.getLocalizedMessage());
//            e.printStackTrace();
//        }
        apiConfig = new ApiConfig().baseUrl(baseUrl).transferSignatory(
                SignatoryFactory.getInstance().createPKCS8Signatory(
                        pkcs8PrivateKey, null));
        logger.info("init - apiConfig" + apiConfig);
        TransferApi transferApi = new ObjectTransferApiImpl(apiConfig);
        taxApi = new DefaultTaxApiClient(transferApi, clientId);
    }

    public List<InquiryResultModel> getInquiryTime(String time){
        taxApi.requestToken();
        this.taxApi.getServerInformation();

        return taxApi.inquiryByTime(time);
    }
    public List<InquiryResultModel> getInquiryByRef(String ref){
        this.taxApi.requestToken();
        this.taxApi.getServerInformation();

        return this.taxApi.inquiryByReferenceId(Collections.singletonList(ref));
    }
    public List<InquiryResultModel> getInquiryByTimeRange(String startDate, String toDate){
        taxApi.requestToken();
        this.taxApi.getServerInformation();
        return taxApi.inquiryByTimeRange(startDate, toDate);
    }

    public AsyncResponseModel sendInvoice(InvoiceDto invoice){
        taxApi.requestToken();
        this.taxApi.getServerInformation();
        AsyncResponseModel responseModel;
        responseModel = taxApi.sendMyInvoices(Collections.singletonList(invoice));
//        System.out.println(responseModel.getResult().get(0).getReferenceNumber());
        logger.info("ReferenceNumber" + responseModel.getResult().get(0).getReferenceNumber());

        return responseModel;
    }

    public ApiConfig getApiConfig() {
        return apiConfig;
    }
}
