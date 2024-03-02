import com.google.gson.Gson;
import com.rabbitmq.client.*;
import ir.gov.tax.tpis.sdk.content.dto.CreateInvoiceResponse;
import ir.gov.tax.tpis.sdk.content.dto.InquiryResultModel;
import ir.gov.tax.tpis.sdk.content.dto.InvoiceErrorModel;
import ir.gov.tax.tpis.sdk.transfer.dto.AsyncResponseModel;
import moadian.InquiryRequest;
import moadian.Moadian;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class RPCInquiryServer {

    private static final String RPC_QUEUE_NAME = "inquiry_queue";

    Logger logger;
    public RPCInquiryServer(String baseUrl, Logger logger) throws Exception {
        this.logger = logger;
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);
            channel.queuePurge(RPC_QUEUE_NAME);

            channel.basicQos(1);

//            System.out.println(" [x] Awaiting RPC inquiry requests from " + RPC_QUEUE_NAME);
            logger.info(" [x] Awaiting RPC inquiry requests from " + RPC_QUEUE_NAME);


            Object monitor = new Object();
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(delivery.getProperties().getCorrelationId())
                        .build();
//                Moadian moadian = new Moadian("");

                List<InquiryResultModel> response = new ArrayList<>();
                Gson gson = new Gson();
                InquiryRequest dataRequest = null;

                try {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);

                    dataRequest = gson.fromJson(message, InquiryRequest.class);

                    Moadian moadian = new Moadian(dataRequest.getClientId(), dataRequest.getCompanyName(), baseUrl, logger);

                    logger.info("before InquiryByRef by =" + dataRequest.getRef());
                    List<InquiryResultModel> rsp = moadian.getInquiryByRef(dataRequest.getRef());
                    logger.info("rsp=" + rsp);
//                    logger.info("1:" + rsp.get(0).toString());
//                    logger.info("2:" + rsp.get(0).getData().toString());
//                    logger.info("3:" + rsp.get(0).getData().getError().toString());
//                    logger.info("4:" + rsp.toString());

//                    int n = Integer.parseInt(message);

//                    System.out.println(" [.] Invoice Resp (" + message + ")");
                    logger.info(" [.] Inquiry Resp (" + message + ")");
                    response = rsp;
                } catch (RuntimeException e) {
                    e.printStackTrace();
//                    System.out.println(" [.] " + );
                    System.out.println("e.getMessage()");
                    System.out.println(e.getMessage());
                    if (response.isEmpty()) {
                        InquiryResultModel e1 = new InquiryResultModel();
                        if (dataRequest != null) {
                            e1.setReferenceNumber(dataRequest.getRef());
//                            e1.setUid(dataRequest.getClientId());//@todo che karesh konam ?
                            e1.setPacketType(dataRequest.getClientId());
                            e1.setFiscalId(dataRequest.getClientId());


                            CreateInvoiceResponse data = getCreateInvoiceResponse(e);
//                            data.setError(new AbstractList<InvoiceErrorModel>() {
//                                @Override
//                                public InvoiceErrorModel get(int index) {
//                                    InvoiceErrorModel invoiceErrorModel = new InvoiceErrorModel();
//                                    invoiceErrorModel.setCode("-1");
//                                    ArrayList<Object> detail = new ArrayList<>();
//                                    detail.add(e.getMessage());
//                                    invoiceErrorModel.setDetail(detail);
//                                    invoiceErrorModel.setMessage(e.getMessage());
//                                    return invoiceErrorModel;
//                                }
//
//                                @Override
//                                public int size() {
//                                    return 1;
//                                }
//                            });
                            System.out.println("data.getError().size");
                            System.out.println(data.getError().size());
                            System.out.println("data.getError().getMessage()");
                            System.out.println(data.getError().get(0).getMessage());
                            e1.setData(data);

                        }
                        e1.setStatus("Moadian Error");
                        response.add(e1);
                    }
                    logger.severe(" [.] Inquiry Error is : " + e.getLocalizedMessage());
                } finally {
                    channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, gson.toJson(response).getBytes(StandardCharsets.UTF_8));
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    // RabbitMq consumer worker thread notifies the RPC server owner thread
                    synchronized (monitor) {
                        monitor.notify();
                    }
                }
            };

            channel.basicConsume(RPC_QUEUE_NAME, false, deliverCallback, (consumerTag -> { }));
            // Wait and be prepared to consume the message from RPC client.
            while (true) {
                synchronized (monitor) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                        logger.severe(e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @NotNull
    private static CreateInvoiceResponse getCreateInvoiceResponse(RuntimeException e) {
        CreateInvoiceResponse data = new CreateInvoiceResponse();
        ArrayList<Object> detail = new ArrayList<>();
        detail.add(e.getMessage());
        data.setSuccess(false);
        InvoiceErrorModel errorModel = new InvoiceErrorModel();
        errorModel.setCode("-1");
        errorModel.setMessage(e.getMessage());
        ArrayList<InvoiceErrorModel> error = new ArrayList<>();
        error.add(errorModel);
        data.setError(error);
        data.setWarning(new ArrayList<>());
        return data;
    }
}