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
            logger.severe(e.getMessage());
            throw new RuntimeException(e);
        }

    }

    private void init() throws IOException {
        //https://tp.tax.gov.ir/
        apiConfig = new ApiConfig().baseUrl(baseUrl).transferSignatory(
                SignatoryFactory.getInstance().createPKCS8Signatory(
                        new File("C:\\keys\\"+companyName+"private.txt"), null));
        TransferApi transferApi = new ObjectTransferApiImpl(apiConfig);
        taxApi = new DefaultTaxApiClient(transferApi, clientId);
    }

    public List<InquiryResultModel> getInquiryTime(String time){
        taxApi.requestToken();
//        this.taxApi.getServerInformation();

        return taxApi.inquiryByTime(time);
    }
    public List<InquiryResultModel> getInquiryByRef(String ref){
        taxApi.requestToken();
//        this.taxApi.getServerInformation();

        return taxApi.inquiryByReferenceId(Collections.singletonList(ref));
    }
    public List<InquiryResultModel> getInquiryByTimeRange(String startDate, String toDate){
        taxApi.requestToken();
//        this.taxApi.getServerInformation();
        return taxApi.inquiryByTimeRange(startDate, toDate);
    }

    public AsyncResponseModel sendInvoice(InvoiceDto invoice){
        taxApi.requestToken();
        this.taxApi.getServerInformation();
        AsyncResponseModel responseModel;
        responseModel = taxApi.sendMyInvoices(Collections.singletonList(invoice));
//        System.out.println(responseModel.getResult().get(0).getReferenceNumber());
        logger.info(responseModel.getResult().get(0).getReferenceNumber());

        return responseModel;
    }


}
