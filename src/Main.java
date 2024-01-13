import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.*;

public class Main {
    private static String baseUrl = "https://sandboxrc.tax.gov.ir/req/api/self-tsp";
    public static void main(String[] args) throws IOException {
        Logger logger = Logger.getLogger(Main.class.getName());
        FileHandler fileHandler = new FileHandler("myLog.log", false);
        fileHandler.setEncoding("UTF-8");
        fileHandler.setFormatter(new CustomLogFormatter());

        logger.addHandler(fileHandler);


//        System.out.println("Connected to : "+args[0]);
        logger.info("Connected to : "+args[0]);
        if(args.length>0)
            baseUrl = args[0];

        Runnable invoiceServerRunnable = new Runnable() {
            public void run() {
                try {
                    RPCInvoiceServer invoiceServer = new RPCInvoiceServer(baseUrl, logger);
                } catch (IOException e) {
                    logger.severe(e.getMessage());
                    throw new RuntimeException(e);
                } catch (TimeoutException e) {
                    logger.severe(e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        };

        Runnable inquiryServerRunnable = new Runnable() {
            public void run() {
                try {
                    RPCInquiryServer inquiryServer = new RPCInquiryServer(baseUrl, logger);
                } catch (Exception e) {
                    logger.severe(e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        };

        Runnable inquiryTimeRangeServerRunnable = new Runnable() {
            public void run() {
                try {
                    RPCInquiryTimeRangeServer inquiryServer = new RPCInquiryTimeRangeServer(baseUrl, logger);
                } catch (Exception e) {
                    logger.severe(e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        };

        Thread invoiceThread = new Thread(invoiceServerRunnable);
        Thread inquiryThread = new Thread(inquiryServerRunnable);
        Thread inquiryTimeRangeThread = new Thread(inquiryTimeRangeServerRunnable);

        invoiceThread.start();
        inquiryThread.start();
        inquiryTimeRangeThread.start();


    }

    static class CustomLogFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            // Customize the log message format here
            return "[" + record.getLevel() + "] " + record.getMessage() + "\n";
        }
    }
}