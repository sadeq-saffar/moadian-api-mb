package moadian;

import ir.gov.tax.tpis.sdk.clients.TaxApi;
import ir.gov.tax.tpis.sdk.clients.TaxPublicApi;
import ir.gov.tax.tpis.sdk.content.api.DefaultTaxApiClient;
import ir.gov.tax.tpis.sdk.content.dto.InquiryResultModel;
import ir.gov.tax.tpis.sdk.content.dto.InvoiceDto;
import ir.gov.tax.tpis.sdk.cryptography.Encryptor;
import ir.gov.tax.tpis.sdk.cryptography.Signatory;
import ir.gov.tax.tpis.sdk.dto.InquiryDto;
import ir.gov.tax.tpis.sdk.dto.Pageable;
import ir.gov.tax.tpis.sdk.enums.RequestStatus;
import ir.gov.tax.tpis.sdk.factories.EncryptorFactory;
import ir.gov.tax.tpis.sdk.factories.Pkcs8SignatoryFactory;
import ir.gov.tax.tpis.sdk.factories.TaxApiFactory;
import ir.gov.tax.tpis.sdk.properties.TaxProperties;
import ir.gov.tax.tpis.sdk.transfer.api.ObjectTransferApiImpl;
import ir.gov.tax.tpis.sdk.transfer.api.TransferApi;
import ir.gov.tax.tpis.sdk.transfer.config.ApiConfig;
import ir.gov.tax.tpis.sdk.transfer.dto.AsyncResponseModel;
import ir.gov.tax.tpis.sdk.transfer.impl.signatory.SignatoryFactory;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

public class Moadian {
    private String clientId = "";
    private String companyName = "";
    ApiConfig apiConfig;
    DefaultTaxApiClient taxApi;

    public Moadian(String clientId, String companyName) {
        this.clientId = clientId;
        this.companyName = companyName;
        try {
            init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void init() throws IOException {
        //https://tp.tax.gov.ir/
        apiConfig = new ApiConfig().baseUrl("https://sandboxrc.tax.gov.ir/req/api/self-tsp").transferSignatory(
                SignatoryFactory.getInstance().createPKCS8Signatory(
                        new File("D:\\keys\\"+companyName+"private.txt"), null));
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

    public AsyncResponseModel sendInvoice(InvoiceDto invoice){
        taxApi.requestToken();
        this.taxApi.getServerInformation();
        AsyncResponseModel responseModel;
        responseModel = taxApi.sendMyInvoices(Collections.singletonList(invoice));
        System.out.println(responseModel.getResult().get(0).getReferenceNumber());
        return responseModel;
    }


}
