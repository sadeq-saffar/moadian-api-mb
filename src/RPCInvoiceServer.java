import com.google.gson.Gson;
import com.rabbitmq.client.*;
import ir.gov.tax.tpis.sdk.transfer.dto.AsyncResponseModel;
import moadian.DataRequest;
import moadian.Moadian;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class RPCInvoiceServer {

    private static final String RPC_QUEUE_NAME = "invoice_queue";

    public RPCInvoiceServer() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);
            channel.queuePurge(RPC_QUEUE_NAME);

            channel.basicQos(1);

            System.out.println(" [x] Awaiting RPC Invoice requests from " + RPC_QUEUE_NAME);

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

                    DataRequest dataRequest  = gson.fromJson(message, DataRequest.class);
                    Moadian moadian = new Moadian(dataRequest.getData().getUsername(), dataRequest.getData().getVendorName());
                    AsyncResponseModel rsp = moadian.sendInvoice(dataRequest.getData().getInvoice());
                    System.out.println(rsp);
                    System.out.println(" [.] Invoice Resp (" + message + ")");
                    response = rsp;
                } catch (RuntimeException e) {
                    System.out.println(" [.] " + e);
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
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}