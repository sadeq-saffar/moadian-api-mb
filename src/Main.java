import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Main {
    public static void main(String[] args) {

        Runnable invoiceServerRunnable = new Runnable() {
            public void run() {
                try {
                    RPCInvoiceServer invoiceServer = new RPCInvoiceServer();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (TimeoutException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        Runnable inquiryServerRunnable = new Runnable() {
            public void run() {
                try {
                    RPCInquiryServer inquiryServer = new RPCInquiryServer();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        Thread invoiceThread = new Thread(invoiceServerRunnable);
        Thread inquiryThread = new Thread(inquiryServerRunnable);

        invoiceThread.start();
        inquiryThread.start();


    }
}