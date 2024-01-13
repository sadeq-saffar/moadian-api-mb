import com.google.gson.Gson;
import com.rabbitmq.client.*;
import ir.gov.tax.tpis.sdk.content.dto.InquiryResultModel;
import moadian.InquiryRequest;
import moadian.InquiryTimeRangeRequest;
import moadian.Moadian;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class RPCInquiryTimeRangeServer {

    private static final String RPC_QUEUE_NAME = "inquiry_time_range_queue";
    Logger logger;
    public RPCInquiryTimeRangeServer(String baseUrl, Logger logger) throws Exception {
        this.logger = logger;
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");


        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);
            channel.queuePurge(RPC_QUEUE_NAME);

            channel.basicQos(1);

//            System.out.println(" [x] Awaiting RPC inquiry time range requests from " + RPC_QUEUE_NAME);
            logger.info(" [x] Awaiting RPC inquiry time range requests from " + RPC_QUEUE_NAME);

            Object monitor = new Object();
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(delivery.getProperties().getCorrelationId())
                        .build();
//                Moadian moadian = new Moadian("");

                List<InquiryResultModel> response = new ArrayList<>();
                Gson gson = new Gson();

                try {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);

                    InquiryTimeRangeRequest request  = gson.fromJson(message, InquiryTimeRangeRequest.class);

                    Moadian moadian = new Moadian(request.getClientId(), request.getCompanyName(), baseUrl, logger);

                    List<InquiryResultModel> rsp = moadian.getInquiryByTimeRange(request.getStartDate(), request.getEndDate());
//                    System.out.println(rsp);
                    logger.info(rsp.toString());
//                    int n = Integer.parseInt(message);

//                    System.out.println(" [.] Time Range Resp (" + message + ")");
                    logger.info(" [.] Time Range Resp (" + message + ")");
                    response = rsp;
                } catch (RuntimeException e) {
//                    System.out.println(" [.] " + e);
                    logger.severe(" [.] Time Range Resp " + e.getLocalizedMessage());
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