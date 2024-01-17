import com.google.gson.Gson;
import com.rabbitmq.client.*;
import ir.gov.tax.tpis.sdk.transfer.dto.AsyncResponseModel;
import moadian.DataRequest;
import moadian.Moadian;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class RPCInvoiceServer {

    private static final String RPC_QUEUE_NAME = "invoice_queue";
    Logger logger;
    public RPCInvoiceServer(String baseUrl, Logger logger) throws IOException, TimeoutException {
        this.logger = logger;
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);
            channel.queuePurge(RPC_QUEUE_NAME);

            channel.basicQos(1);

//            System.out.println(" [x] Awaiting RPC Invoice requests from " + RPC_QUEUE_NAME);
            logger.info(" [x] Awaiting RPC Invoice requests from " + RPC_QUEUE_NAME);

            Object monitor = new Object();
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(delivery.getProperties().getCorrelationId())
                        .build();
                AsyncResponseModel response = new AsyncResponseModel();
                Gson gson = new Gson();

                try {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);

                    logger.info("Input is :  " + message);

                    DataRequest dataRequest  = gson.fromJson(message, DataRequest.class);
                    logger.info("dataRequest is :  " + dataRequest.toString());
                    logger.info("Moadian Username is :  " + dataRequest.getData().getUsername() );
                    logger.info("Moadian VendorName is :  " + dataRequest.getData().getVendorName() );
                    logger.info("Moadian baseUrl is :  " + baseUrl );

                    Moadian moadian = new Moadian(dataRequest.getData().getUsername(), dataRequest.getData().getVendorName(),baseUrl, logger);
                    logger.info("moadian apiConfig is :  " + moadian.getApiConfig() );
                    AsyncResponseModel rsp = moadian.sendInvoice(dataRequest.getData().getInvoice());
//                    System.out.println(rsp);
                    logger.info("rsp " + rsp.toString());

//                    System.out.println(" [.] Invoice Resp (" + message + ")");
                    logger.info(" [.] Invoice Resp (" + message + ")");
                    response = rsp;
                } catch (RuntimeException e) {
//                    System.out.println(" [.] " + e);
                    logger.severe(" [.] Invoice Resp " + e.getMessage());
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
}